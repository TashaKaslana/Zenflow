# Adaptive Cost-Aware Caching for Context/Resource Resolution

A concise, implementation-ready spec for switching from fixed TTL to **variable TTL based on observed cost × factor**, with safety rails. Drop this in as a `design.md` for the agent/impl to follow.

---

## Why

- **Fixed TTL** (e.g., 1 minute) keeps cheap entries too long and expires expensive ones too soon.
- **Cost-aware TTL** aligns retention with the actual compute/IO cost saved per hit.
- Uses **Caffeine** with a custom `Expiry` to compute TTL per entry.

---
## Apply range
Only apply cache for template resolution with initial ref > 5 or (ref >=2 and cost(after resolution) reach a limit as an expensive item) 

## High-Level Design

- Wrap each cached value in a `Tracked<T>` that carries:
  - `refs`: current reference count (pinning)
  - `emaCostNanos`: exponential moving average of compute time
  - `lastUsedNanos` (optional; useful for diagnostics)
- Compute TTL at **create/update/read** via `Expiry`:
  - If `refs > 0`: **pinned** but not immortal → longer TTL with a hard cap
  - If `refs == 0`: **idle** → shorter TTL, still proportional to cost
- **Clamp** TTL to `[MIN_TTL, MAX_TTL_{PINNED/IDLE}]`
- Use `Scheduler.systemScheduler()` so expiration happens on time, not only on access.
- Keep a **hard size/weight cap** to bound memory usage.

---

## Data Structures

```java
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

record Tracked<T>(
    T resource,
    AtomicInteger refs,       // current consumers (pinning)
    AtomicLong emaCostNanos,  // exponential moving average of compute time
    AtomicLong lastUsedNanos  // diagnostics/observability
) {
    static final double ALPHA = 0.2; // smoothing factor

    void recordCost(long nanos) {
        long prev = emaCostNanos.get();
        long next = (prev == 0L) ? nanos : (long) (ALPHA * nanos + (1 - ALPHA) * prev);
        emaCostNanos.set(next);
        lastUsedNanos.set(System.nanoTime());
    }
}
```

---

## Cache Construction

```java
import com.github.benmanes.caffeine.cache.*;

final Duration MIN_TTL        = Duration.ofSeconds(1);
final Duration MAX_TTL_IDLE   = Duration.ofMinutes(5);
final Duration MAX_TTL_PINNED = Duration.ofMinutes(15);
final double   TTL_FACTOR     = 20.0; // K: “how many times the cost” to retain

private static long clampNanos(long v, long min, long max) {
    return Math.max(min, Math.min(max, v));
}

private final Cache<String, Tracked<T>> resourceCache =
    Caffeine.newBuilder()
        .maximumSize(500)                   // or maximumWeight + weigher
        .scheduler(Scheduler.systemScheduler()) // timely expiry
        .expireAfter(new Expiry<String, Tracked<T>>() {
            @Override
            public long expireAfterCreate(String k, Tracked<T> v, long now) {
                return ttlNanos(v);
            }
            @Override
            public long expireAfterUpdate(String k, Tracked<T> v, long now, long cur) {
                return ttlNanos(v);
            }
            @Override
            public long expireAfterRead(String k, Tracked<T> v, long now, long cur) {
                return ttlNanos(v);
            }

            private long ttlNanos(Tracked<T> v) {
                int refs = v.refs().get();
                long cost = v.emaCostNanos().get();
                long base = (cost == 0L)
                    ? MIN_TTL.toNanos()
                    : (long) (cost * TTL_FACTOR);

                if (refs > 0) { // pinned
                    return clampNanos(base, MIN_TTL.toNanos(), MAX_TTL_PINNED.toNanos());
                }
                // idle
                return clampNanos(base, MIN_TTL.toNanos(), MAX_TTL_IDLE.toNanos());
            }
        })
        .removalListener((String k, Tracked<T> v, RemovalCause c) -> onResourceRemoval(k, v.resource(), c))
        .recordStats()
        .build();
```
> **Do not** return `Long.MAX_VALUE` from `Expiry`. If `refs` gets stuck > 0, the entry becomes immortal. Always clamp.

---

## Admission & Cost Tracking

Wrap expensive creation/resolution with timing and update EMA:

```java
public Tracked<T> computeTracked(String key, Supplier<T> factory) {
    long t0 = System.nanoTime();
    T res = factory.get(); // create/resolve
    long elapsed = System.nanoTime() - t0;

    Tracked<T> tracked = new Tracked<>(
        res, new AtomicInteger(0), new AtomicLong(0L), new AtomicLong(System.nanoTime())
    );
    tracked.recordCost(elapsed);
    return tracked;
}

// Usage:
Tracked<T> tracked = resourceCache.get(key, k -> computeTracked(k, this::createResource));
```

On subsequent **refreshes** (if you recompute), call `tracked.recordCost(elapsed)` again to keep the EMA current.

---

## Reference Management

- Call `tracked.refs.incrementAndGet()` when a consumer starts using the resource.
- Call `tracked.refs.decrementAndGet()` on release.
- The `Expiry` will automatically give pinned entries a longer TTL, and idle entries a shorter TTL.

> Even when pinned, entries **still expire** due to the pinned max backstop (`MAX_TTL_PINNED`) to avoid leaks.

---

## Optional: Memory Pressure Guardrail

If you already have a `SystemLoadMonitor`:

```java
// inside ttlNanos(...)
double mem = systemLoadMonitor.readMemoryPressure(); // 0.0..1.0
if (mem >= 0.90) {
    // Constrain retention under stress
    return MIN_TTL.toNanos();
}
```

You can also adjust `TTL_FACTOR` dynamically (cache less when memory tight; cache more when CPU high).

---

## Weighing (if resource sizes vary)

Prefer `maximumWeight` with a `weigher`:

```java
Caffeine.newBuilder()
    .maximumWeight(128L * 1024 * 1024) // 128 MB
    .weigher((String k, Tracked<T> v) -> estimateBytes(v.resource()))
```

---

## Stampede Protection (Single-Flight)

If multiple threads may build the same key:

- Use `resourceCache.get(key, ...)` which is already per-key atomic **per mapping function invocation**.
- For multi-step builds, consider a separate `ConcurrentHashMap<String, CompletableFuture<Tracked<T>>>>` to coalesce work.

---

## Safety & Edge Cases

- **No refs drop:** Pinned entries still expire due to `MAX_TTL_PINNED`.
- **Zero cost:** Entries start with `MIN_TTL`; they’ll get longer only after recording real cost.
- **Hot but cheap values:** They won’t balloon the cache because TTL scales with *cost*, not *frequency*.
- **Global cap:** `maximumSize/Weight` ensures bounded memory even if many expensive entries appear.

---

## Configuration Knobs

- `TTL_FACTOR` (default `20.0`): higher → retain longer per unit cost.
- `ALPHA` for EMA (default `0.2`): higher → react faster to new costs.
- `MIN_TTL`, `MAX_TTL_IDLE`, `MAX_TTL_PINNED`: clamp bounds.
- `maximumSize/Weight`: memory ceiling.

**Suggested defaults**:
- `MIN_TTL = 1s`
- `MAX_TTL_IDLE = 5m`
- `MAX_TTL_PINNED = 15m`
- `TTL_FACTOR = 20`

Tune with `cache.stats()` (hit rate, evictions) and your latency goals.

---

## Minimal Integration Steps

1. Add `Tracked<T>` and wrap resource creation to **record cost**.
2. Switch cache builder to `expireAfter(Expiry)` + `Scheduler.systemScheduler()`.
3. Replace fixed TTL with **dynamic TTL** logic above.
4. Wire **refs** inc/dec where your consumers acquire/release.
5. (Optional) Add memory pressure modifier & weight-based limit.
6. Add metrics: hit ratio, average TTL, evictions, P95 resolve time.

---

## Example Acquire/Release Helpers

```java
public T acquire(String key, Supplier<T> factory) {
    Tracked<T> tracked = resourceCache.get(key, k -> computeTracked(k, factory));
    tracked.refs().incrementAndGet();
    return tracked.resource();
}

public void release(String key) {
    Tracked<T> tracked = resourceCache.getIfPresent(key);
    if (tracked != null) {
        tracked.refs().decrementAndGet();
        tracked.lastUsedNanos().set(System.nanoTime());
        // Expiry will handle TTL; no direct invalidate needed
    }
}
```

---

## Testing Checklist

- [ ] Entries with **high recorded cost** get longer TTLs than cheap ones.
- [ ] `refs > 0` → longer TTL but still bounded by `MAX_TTL_PINNED`.
- [ ] Under **memory pressure**, TTL shrinks (if guardrail enabled).
- [ ] **Hit ratio** improves vs fixed 1-minute TTL for mixed workloads.
- [ ] **No leak** when clients forget to release: expiration still happens.
- [ ] Concurrent acquires for same key do not duplicate resource creation.

---

## TL;DR

Swap fixed TTL for **`Expiry` + EMA(cost) × factor**, clamp with min/max, keep a hard size/weight cap, and (optionally) modulate by system load. This stays simple while giving you materially better hit rates and memory discipline.
