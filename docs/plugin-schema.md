# Plugin Schemas

Plugins may declare configuration requirements through a JSON schema file. Each plugin definition can point to a `plugin.schema.json` using the `schemaPath` attribute of the `@Plugin` annotation. The schema is loaded at startup and stored with the plugin record.

The attribute is optional. Plugins without additional configuration can omit `schemaPath`, in which case no schema is persisted or returned by the API.

The stored schema may contain multiple sections such as `profile` credentials or other `settings`. The entire schema is exposed via the REST API so the frontend can render forms dynamically.

```
GET /plugins/{key}/schema
```

Example: requesting `GET /plugins/google-drive/schema` returns the shared Google OAuth profile schema used by both Google Drive and Google Docs plugins.

Clients should use the returned JSON schema to build forms for gathering configuration information from users.
