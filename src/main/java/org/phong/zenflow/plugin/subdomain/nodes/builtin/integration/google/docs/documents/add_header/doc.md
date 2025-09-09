# Google Docs - Add Header Node

## Overview
Creates a header in an existing Google Docs document and optionally inserts text into it.

## Node Information
- **Key**: `google-docs:documents.add_header`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:header`
- **Tags**: `google`, `docs`, `document`, `integration`

## Description
Uses the Google Docs API to create a new header in the specified document. If text is provided, it is inserted at the beginning of the header. Docs client instances are cached through the global node resource manager to maximize efficiency in high concurrency environments.

## Input/Output
### Input
- `profile` (string, required): Secret profile group containing OAuth credentials.
- `documentId` (string, required): ID of the document to modify.
- `text` (string, optional): Text to insert into the new header.

### Output
- `documentId` (string): ID of the modified document.
- `headerId` (string): ID of the created header.

## Usage Example
```json
{
  "input": {
    "profile": "my-google-docs-profile",
    "documentId": "1A2B3C4D",
    "text": "Confidential"
  }
}
```

## Credentials
Configure a plugin-level OAuth profile (e.g., `my-google-docs-profile`) with the following keys:

- `CLIENT_ID`
- `CLIENT_SECRET`
- `REFRESH_TOKEN`

Reference the profile name via the `profile` input field. The system retrieves these keys from the plugin-level profile before execution.
