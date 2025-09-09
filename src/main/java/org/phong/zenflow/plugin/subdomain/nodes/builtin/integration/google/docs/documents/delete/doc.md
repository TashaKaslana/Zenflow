# Google Docs - Delete Document Node

## Overview
Permanently deletes a Google Docs document using the Google Drive API.

## Node Information
- **Key**: `google-docs:documents.delete`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:file-text`
- **Tags**: `google`, `docs`, `document`, `integration`, `delete`

## Description
Removes the specified document by ID using OAuth2 credentials. Docs client instances are cached through the global node resource manager to maximize efficiency in high concurrency environments.

## Input/Output
### Input
- `profile` (string, required): Secret profile group containing OAuth credentials.
- `documentId` (string, required): ID of the document to delete.

### Output
- `documentId` (string): ID of the deleted document.
- `deleted` (boolean): Whether the document was deleted.

## Usage Example
```json
{
  "input": {
    "profile": "my-google-docs-profile",
    "documentId": "1A2B3C4D"
  }
}
```

## Credentials
Configure a plugin-level OAuth profile (e.g., `my-google-docs-profile`) with the following keys:

- `CLIENT_ID`
- `CLIENT_SECRET`
- `REFRESH_TOKEN`

Reference the profile name via the `profile` input field. The system retrieves these keys from the plugin-level profile before execution.
