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
