# Google Docs - Delete Text Node

## Overview
Deletes a range of text from an existing Google Docs document using the Google Docs API v1.

## Node Information
- **Key**: `google-docs:documents.delete_text`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:file-text`
- **Tags**: `google`, `docs`, `document`, `integration`

## Description
Removes the specified text range from a document using OAuth2 credentials. Docs client instances are cached through the global node resource manager to maximize efficiency in high concurrency environments.

## Input/Output
### Input
- `profile` (string, required): Secret profile group containing OAuth credentials.
- `documentId` (string, required): ID of the document to modify.
- `startIndex` (integer, required): Start index of the range to delete.
- `endIndex` (integer, required): End index of the range to delete.

### Output
- `documentId` (string): ID of the modified document.
- `deleted` (boolean): Indicates the delete operation succeeded.

## Usage Example
```json
{
  "input": {
    "profile": "my-google-docs-profile",
    "documentId": "1A2B3C4D",
    "startIndex": 1,
    "endIndex": 5
  }
}
```

## Credentials
Configure a plugin-level OAuth profile (e.g., `my-google-docs-profile`) with the following keys:

- `CLIENT_ID`
- `CLIENT_SECRET`
- `REFRESH_TOKEN`

Reference the profile name via the `profile` input field. The system retrieves these keys from the plugin-level profile before execution.
