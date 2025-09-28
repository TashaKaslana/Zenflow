# Plugin Node Sample Data Endpoint

Retrieve example configuration for a plugin node based on its `config_schema`.

## Request

`GET /plugins/nodes/{nodeId}/sample`

## Response

```json
{
  "metadata": {
    "message": "Sample data generated successfully"
  },
  "data": {
    "...": "Sample configuration fields"
  }
}
```

The `data` field contains a map of generated sample values that conforms to the node's configuration schema. This helps the front‑end render default forms or provide example payloads.
