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
Configure a plugin-level OAuth profile (e.g., `my-google-docs-profile`) with the following keys:

- `CLIENT_ID`
- `CLIENT_SECRET`
- `REFRESH_TOKEN`

Reference the profile name via the `profile` input field. The system retrieves these keys from the plugin-level profile before execution.
