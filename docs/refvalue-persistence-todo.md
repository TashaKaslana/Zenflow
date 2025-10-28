# RefValue Workflow Persistence - TODO

## Status: Not Implemented (Future Feature)

**Last Updated:** October 27, 2025  
**Priority:** Low (only needed for workflow pause/resume functionality)  
**Current Branch:** `refactor/context-storage`

---

## Overview

The RefValue system is **fully functional** for in-memory workflow execution with the following capabilities:
- ✅ Automatic storage selection (Memory/JSON/File)
- ✅ Memory optimization for large payloads
- ✅ Consumer tracking and auto-cleanup
- ✅ Metrics and monitoring
- ✅ All executors using new write pattern

However, **workflow persistence** (pause/resume across application restarts) is **not yet implemented**. This document tracks what needs to be done when that feature is required.

---

## What Works Today

### ✅ Short-Lived Workflows
- Workflows that complete in one run
- No application restarts mid-execution
- Context exists only in RuntimeContext (memory)
- RefValue cleanup happens automatically when consumers reach zero

### ✅ Current Descriptor Support
Basic `toDescriptor()` and `fromDescriptor()` methods exist but are **placeholders**:
- `MemoryRefValue.toDescriptor()` - stores inline value
- `JsonRefValue.toDescriptor()` - stores serialized bytes inline
- `FileRefValue.toDescriptor()` - stores file path (⚠️ file may be deleted!)

---

## What Doesn't Work

### ❌ Workflow Pause/Resume
- Cannot pause workflow and resume after application restart
- RuntimeContext is not persisted to database
- RefValue descriptors are not serialized to WorkflowRun entity

### ❌ File-Backed Persistence
- `FileRefValue` stores temp file path in descriptor
- Temp files are deleted when RefValue is released
- File no longer exists if trying to restore from descriptor

### ❌ Complex Object Serialization
- `MemoryRefValue` stores raw object inline
- May not deserialize correctly for complex object graphs
- No custom serialization logic

---

## TODO List for Future Implementation

### Phase 1: Database Schema (1-2 days)

#### 1.1 Update WorkflowRun Entity
**File:** `src/main/java/org/phong/zenflow/workflow/subdomain/workflow_run/infrastructure/persistence/entity/WorkflowRun.java`

**Current:**
```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "context")
private Map<String, Object> context;
```

**Option A - Store Descriptors (Recommended):**
```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "context")
private Map<String, RefValueDescriptor> contextDescriptors;
```

**Option B - Keep Object Map (Simpler but less efficient):**
- Keep current schema
- Serialize RefValue contents inline before save
- Lose file optimization benefits

#### 1.2 Create Database Migration
**Task:** Write Flyway/Liquibase migration script
- Rename column if needed
- Convert existing data format
- Test with production-like data volume

**Files to create:**
- `src/main/resources/db/migration/V{next}_refvalue_context_schema.sql`

---

### Phase 2: RuntimeContext Persistence (2-3 days)

#### 2.1 Implement Context Serialization
**File:** `src/main/java/org/phong/zenflow/workflow/subdomain/context/RuntimeContext.java`

**Add method:**
```java
/**
 * Converts current context to descriptors for database persistence.
 * Call this before saving WorkflowRun.
 */
public Map<String, RefValueDescriptor> toDescriptors() {
    Map<String, RefValueDescriptor> descriptors = new HashMap<>();
    for (Map.Entry<String, RefValue> entry : context.entrySet()) {
        try {
            descriptors.put(entry.getKey(), entry.getValue().toDescriptor());
        } catch (UnsupportedOperationException e) {
            log.warn("Cannot persist context key '{}': {}", entry.getKey(), e.getMessage());
            // Skip non-persistable values or handle differently
        }
    }
    return descriptors;
}
```

#### 2.2 Implement Context Restoration
**Add method:**
```java
/**
 * Restores context from persisted descriptors.
 * Call this when resuming a paused workflow.
 */
public void fromDescriptors(Map<String, RefValueDescriptor> descriptors) {
    for (Map.Entry<String, RefValueDescriptor> entry : descriptors.entrySet()) {
        try {
            RefValue refValue = refValueSupport.getFactory().fromDescriptor(entry.getValue());
            context.put(entry.getKey(), refValue);
        } catch (IOException e) {
            log.error("Failed to restore context key '{}': {}", entry.getKey(), e.getMessage());
            // Handle restoration failure (skip, use default, fail workflow?)
        }
    }
}
```

#### 2.3 Update WorkflowRunService
**File:** `src/main/java/org/phong/zenflow/workflow/subdomain/workflow_run/service/WorkflowRunService.java`

**Update `saveContext()` method:**
```java
@Transactional
public void saveContext(UUID workflowRunId, RuntimeContext runtimeContext) {
    WorkflowRun workflowRun = workflowRunRepository.findById(workflowRunId)
            .orElseThrow(() -> new WorkflowRunException("WorkflowRun not found"));
    
    // Convert RefValues to descriptors
    Map<String, RefValueDescriptor> descriptors = runtimeContext.toDescriptors();
    workflowRun.setContextDescriptors(descriptors);
    
    workflowRunRepository.save(workflowRun);
}
```

**Add `restoreContext()` method:**
```java
@Transactional(readOnly = true)
public RuntimeContext restoreContext(UUID workflowRunId) {
    WorkflowRun workflowRun = workflowRunRepository.findById(workflowRunId)
            .orElseThrow(() -> new WorkflowRunException("WorkflowRun not found"));
    
    RuntimeContext context = new RuntimeContext();
    if (workflowRun.getContextDescriptors() != null) {
        context.fromDescriptors(workflowRun.getContextDescriptors());
    }
    
    return context;
}
```

---

### Phase 3: File-Backed Value Persistence (3-4 days)

#### 3.1 Choose File Persistence Strategy

**Option A: Inline File Contents in Descriptor**
- Store file bytes in `inlineValue` field
- Works for files < few MB
- Simple but may bloat database

**Option B: Copy to Persistent Storage**
- Copy temp file to permanent location on descriptor creation
- Store permanent path in `locator`
- Need file retention and cleanup policy

**Option C: Hybrid Approach**
- Small files (< 1MB): inline in descriptor
- Large files: copy to persistent storage
- Best of both worlds but more complex

#### 3.2 Implement Chosen Strategy

**Update FileRefValue.toDescriptor():**
```java
@Override
public RefValueDescriptor toDescriptor() {
    // Option A - Inline small files
    if (size <= 1_048_576) { // 1MB threshold
        try {
            byte[] fileBytes = Files.readAllBytes(filePath);
            return RefValueDescriptor.builder()
                    .type(RefValueType.FILE)
                    .mediaType(mediaType)
                    .size(size)
                    .inlineValue(fileBytes) // Store inline
                    .build();
        } catch (IOException e) {
            log.error("Failed to inline file for persistence", e);
            throw new RuntimeException(e);
        }
    }
    
    // Option B - Copy to persistent storage
    Path persistentPath = copyToPersistentStorage(filePath);
    return RefValueDescriptor.builder()
            .type(RefValueType.FILE)
            .locator(persistentPath.toString())
            .mediaType(mediaType)
            .size(size)
            .build();
}

private Path copyToPersistentStorage(Path tempFile) {
    // TODO: Implement copy to persistent directory
    // Maybe: ./data/context-files/persistent/{workflowRunId}/{key}.dat
    throw new UnsupportedOperationException("Not yet implemented");
}
```

**Update RefValueFactory.fromDescriptor():**
```java
private Map<String, RefValue> fromDescriptor() {
    case FILE -> {
        // Try to restore from inline value first
        if (descriptor.getInlineValue() instanceof byte[] bytes) {
            yield FileRefValue.fromBytes(bytes, descriptor.getMediaType(),
                    config.getFileStorageDir(), config.getTempFilePrefix());
        }

        // Otherwise use locator
        if (descriptor.getLocator() == null) {
            throw new IOException("Invalid FILE descriptor: no inline value or locator");
        }

        Path path = Path.of(descriptor.getLocator());
        if (!Files.exists(path)) {
            throw new IOException("Persistent file not found: " + path);
        }

        yield new FileRefValue(path, descriptor.getMediaType());
    }
}
```

#### 3.3 Implement File Retention Policy

**Create cleanup job:**
- Delete persistent files when workflow is completed/deleted
- Delete orphaned files (workflows deleted but files remain)
- Configurable retention period

**Configuration:**
```yaml
zenflow:
  context:
    refvalue:
      persistent-file-dir: ./data/context-files/persistent
      persistent-file-retention-days: 30
      orphan-cleanup-enabled: true
      orphan-cleanup-schedule: "0 0 2 * * ?" # 2 AM daily
```

---

### Phase 4: Testing (2-3 days)

#### 4.1 Unit Tests
- [ ] Test MemoryRefValue descriptor serialization/restoration
- [ ] Test JsonRefValue descriptor serialization/restoration
- [ ] Test FileRefValue descriptor serialization/restoration (both inline and persistent)
- [ ] Test RuntimeContext.toDescriptors() / fromDescriptors()
- [ ] Test descriptor with null values, empty maps, complex objects

#### 4.2 Integration Tests
- [ ] Test workflow pause (save context to DB)
- [ ] Test workflow resume (restore context from DB)
- [ ] Test file restoration after app restart
- [ ] Test consumer tracking survives persistence
- [ ] Test loop cleanup with persisted context

#### 4.3 Stress Tests
- [ ] Test with 1000+ context keys
- [ ] Test with 10MB+ file-backed values
- [ ] Test with complex object graphs
- [ ] Test database serialization performance
- [ ] Test restoration performance

---

### Phase 5: Production Readiness (1-2 days)

#### 5.1 Configuration
- [ ] Add feature flag: `zenflow.context.persistence.enabled`
- [ ] Add fail-safe defaults
- [ ] Document all config options

#### 5.2 Monitoring
- [ ] Add metrics for descriptor creation failures
- [ ] Add metrics for restoration failures
- [ ] Add alerts for orphaned files
- [ ] Dashboard for persistent file storage usage

#### 5.3 Documentation
- [ ] Migration guide for existing workflows
- [ ] Operational runbook for file cleanup
- [ ] Troubleshooting guide
- [ ] Performance tuning guide

---

## Estimated Effort

**Total:** 10-14 days (2-3 weeks)

| Phase | Days | Risk Level |
|-------|------|-----------|
| Phase 1: Database Schema | 1-2 | Low |
| Phase 2: RuntimeContext Persistence | 2-3 | Medium |
| Phase 3: File-Backed Persistence | 3-4 | High |
| Phase 4: Testing | 2-3 | Medium |
| Phase 5: Production Readiness | 1-2 | Low |

**High Risk Areas:**
- File persistence strategy (complex, many edge cases)
- Database migration (must handle existing data)
- File cleanup policy (risk of orphaned files or premature deletion)

---

## Decision Points

Before starting implementation, decide:

1. **Do we need workflow pause/resume at all?**
   - If no → Mark this as WONTFIX and close
   - If yes → Continue to next decision

2. **How long should workflows be pausable?**
   - Minutes → Keep simple, inline files in DB
   - Hours/Days → Need persistent storage strategy
   - Weeks+ → Need robust retention and cleanup

3. **What's the expected context size?**
   - < 10MB → Inline everything in DB
   - 10-100MB → Hybrid approach (small inline, large files)
   - > 100MB → Must use persistent file storage

4. **What's the failure mode for lost context?**
   - Fail workflow → Simple, clear errors
   - Retry with empty context → Resilient but complex
   - Partial restore → Best UX but hardest to implement

---

## Alternative: Don't Implement This

**Consider if:**
- Workflows always complete quickly (< 5 minutes)
- No need to survive application restarts
- No planned maintenance windows during workflow execution
- Context is easily reconstructable from inputs

**Then:**
- Mark this TODO as WONTFIX
- Document that RefValue is optimized for in-memory execution only
- Keep existing implementations as-is
- Move on to other features

---

## References

- RefValue Phase 1 Complete: `docs/REFVALUE-COMPLETE.md`
- RuntimeContext Integration: `docs/runtime-context-refvalue-integration.md`
- Executor Write Refactor: `docs/execution-context-write-refactor.md`

---

## Next Steps

When ready to implement:
1. Review this document with team
2. Make architecture decisions (see Decision Points)
3. Create GitHub issues for each phase
4. Estimate sprint capacity
5. Start with Phase 1 (database schema)

**Until then:** This is a **KNOWN LIMITATION** documented in code comments. Current system works perfectly for short-lived workflows.
