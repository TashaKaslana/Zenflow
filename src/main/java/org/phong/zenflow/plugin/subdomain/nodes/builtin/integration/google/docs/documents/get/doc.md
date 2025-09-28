# Google Docs - Get Document Node

## Overview
Retrieves a Google Docs document including metadata and content using the Google Docs API v1.

## Node Information
- **Key**: `google-docs:documents.get`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:file-text`
- **Tags**: `google`, `docs`, `document`, `integration`

## Description
Fetches the specified document by ID using OAuth2 credentials. Docs client instances are cached through the global node resource manager to maximize efficiency in high concurrency environments.

## Input/Output
### Input
- `profile` (string, required): Secret profile group containing OAuth credentials.
- `documentId` (string, required): ID of the document to retrieve.

### Output
- `documentId` (string): ID of the retrieved document.
- `title` (string): Title of the document.
- `document` (object): Full `Document` resource returned by the Google Docs API.

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
