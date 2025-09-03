# Node Resource Pool Monitoring

Zenflow exposes runtime information about pooled node resources through Spring Boot Actuator.

## Health

The `/actuator/health/nodeResources` endpoint reports active resource usage and whether each
resource is healthy.

## Metrics

Micrometer gauges provide usage metrics under `/actuator/metrics`:

- `node.resources.total` – number of active resources, tagged with `pool`
- `node.resource.nodes` – active node count per resource key, tagged with `pool` and `resource`
