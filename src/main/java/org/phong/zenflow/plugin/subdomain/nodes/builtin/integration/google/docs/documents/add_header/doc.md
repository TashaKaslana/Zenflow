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
