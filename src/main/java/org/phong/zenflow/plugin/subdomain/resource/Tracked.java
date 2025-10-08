package org.phong.zenflow.plugin.subdomain.resource;

import java.util.concurrent.atomic.AtomicInteger;

final class Tracked<T> {
  final T resource;
  final AtomicInteger refs = new AtomicInteger(0);
  Tracked(T r) { this.resource = r; }
}