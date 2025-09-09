# Google Docs - Append Text Node

## Overview
Appends text to the end of an existing Google Docs document using the Google Docs API v1.

## Node Information
- **Key**: `google-docs:documents.append_text`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:file-text`
- **Tags**: `google`, `docs`, `document`, `integration`

## Description
Adds the provided text to the end of the specified document using OAuth2 credentials. Docs client instances are cached through the global node resource manager to maximize efficiency in high concurrency environments.

## Input/Output
### Input
- `profile` (string, required): Secret profile group containing OAuth credentials.
- `documentId` (string, required): ID of the document to modify.
- `text` (string, required): Text to append to the document.

### Output
- `documentId` (string): ID of the modified document.
- `text` (string): The appended text.

## Usage Example
```json
{
  "input": {
    "profile": "my-google-docs-profile",
    "documentId": "1A2B3C4D",
    "text": "Hello world!"
  }
}
```

## Credentials
Configure a plugin-level OAuth profile (e.g., `my-google-docs-profile`) with the following keys:

- `CLIENT_ID`
- `CLIENT_SECRET`
- `REFRESH_TOKEN`

Reference the profile name via the `profile` input field. The system retrieves these keys from the plugin-level profile before execution.
