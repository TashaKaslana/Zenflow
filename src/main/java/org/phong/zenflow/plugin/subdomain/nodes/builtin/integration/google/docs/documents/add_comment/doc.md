# Google Docs - Add Comment Node

## Overview
Adds a comment to an existing Google Docs document using the Drive API.

## Node Information
- **Key**: `google-docs:documents.add_comment`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:chat-circle-text`
- **Tags**: `google`, `docs`, `document`, `integration`

## Description
Creates a new comment on the specified document using OAuth2 credentials. Drive client instances are cached through the global node resource manager to maximize efficiency in high concurrency environments.

## Input/Output
### Input
- `profile` (string, required): Secret profile group containing OAuth credentials.
- `documentId` (string, required): ID of the document to comment on.
- `content` (string, required): Comment text.

### Output
- `documentId` (string): ID of the document.
- `commentId` (string): ID of the created comment.

## Usage Example
```json
{
  "input": {
    "profile": "my-google-docs-profile",
    "documentId": "1A2B3C4D",
    "content": "Please review this section"
  }
}
```

## Credentials
Configure a plugin-level OAuth profile (e.g., `my-google-docs-profile`) with the following keys:

- `CLIENT_ID`
- `CLIENT_SECRET`
- `REFRESH_TOKEN`

Reference the profile name via the `profile` input field. The system retrieves these keys from the plugin-level profile before execution.
