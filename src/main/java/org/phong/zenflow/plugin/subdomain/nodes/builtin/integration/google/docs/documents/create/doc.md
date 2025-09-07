# Google Docs - Create Document Node

## Overview
Creates a blank Google Docs document using the Google Docs API v1.

## Node Information
- **Key**: `google-docs:documents.create`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:file-text`
- **Tags**: `google`, `docs`, `document`, `integration`

## Description
Uses OAuth2 credentials to access Google Docs and create a new document. Docs client instances are cached through the global node resource manager to maximize efficiency in high concurrency environments.

## Input/Output
### Input
- `profile` (string, required): Secret profile group containing OAuth credentials.
- `title` (string): Title for the new document. Defaults to `Untitled Document`.

### Output
- `documentId` (string): ID of the created document.
- `title` (string): Title of the created document.

## Usage Example
```json
{
  "input": {
    "profile": "my-google-docs-profile",
    "title": "My Document"
  }
}
```

## Credentials
Create a secret profile group (e.g., `my-google-docs-profile`) that includes the following keys:

```json
{
  "profiles": [
    {
      "name": "my-google-docs-profile",
      "keys": {
        "CLIENT_ID": "<client id>",
        "CLIENT_SECRET": "<client secret>",
        "REFRESH_TOKEN": "<refresh token>"
      }
    }
  ]
}
```

The profile name is supplied via the `profile` input field and the system resolves these keys to their secret values before execution.
