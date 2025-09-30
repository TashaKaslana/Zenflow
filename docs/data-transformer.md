# Data Transformer Subplugins

Zenflow exposes common data transformations as standalone plugin nodes. These nodes internally use the core `DataTransformerExecutor` and can also be chained through the original `data.transformer` node for pipeline style processing.

## Direct usage

Use a specific transformer node when you only need a single operation. The executor automatically routes to the correct transformer.

```json
{
  "plugin": "core",
  "node": "data.transformer.filter",
  "version": "1.0.0",
  "config": {
    "input": {
      "data": [ {"value": 1}, {"value": 2} ],
      "params": {
        "expression": "value > 1"
      }
    }
  }
}
```

## Composing a pipeline

Multiple transformers can be combined using the `data.transformer` node. Each step references one of the subplugins.

```json
{
  "plugin": "core",
  "node": "data.transformer",
  "version": "1.0.0",
  "config": {
    "input": {
      "data": [ {"amount": 5, "active": true}, {"amount": 7, "active": true} ],
      "isPipeline": true,
      "steps": [
        { "transformer": "filter", "params": { "expression": "active == true" } },
        { "transformer": "aggregate", "params": { "aggregations": [ {"field": "amount", "function": "sum", "alias": "total"} ] } }
      ]
    }
  }
}
```

This example filters active items and then aggregates the remaining amounts into a single total.

## Element-wise Transformation

In addition to single-transform and pipeline modes, you can apply a transformer to each item in a list using the `forEach` flag. This is useful when you want the same operation performed on every element of an array.

```json
{
  "plugin": "core",
  "node": "data.transformer",
  "version": "1.0.0",
  "config": {
    "input": {
      "data": [ {"value": 1}, {"value": 2}, {"value": 3} ],
      "transformer": "increment",
      "params": { "amount": 1 },
      "forEach": true
    }
  }
}
```

This applies the `increment` transformer to each item in the input list, returning an array of transformed values.