# Google Drive - List Files Node

## Overview
Lists files from a Google Drive account using the Google Drive API v3.

## Node Information
- **Key**: `google-drive:files.list`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:google-drive-logo`
- **Tags**: `google`, `drive`, `storage`, `integration`

## Description
Uses OAuth2 credentials to access Google Drive and retrieve basic file information. Drive client instances are cached through the global node resource manager to maximize efficiency in high concurrency environments.

## Input/Output
### Input
- `profile` (string, required): Secret profile group containing OAuth credentials.
- `query` (string): Search query. Defaults to `trashed=false`.

### Output
- `files` (array): List of files including `id` and `name`.

## Usage Example
```json
{
  "input": {
    "profile": "my-google-drive-profile",
    "query": "mimeType='application/pdf'"
  }
}
```

## Credentials
Create a secret profile group (e.g., `my-google-drive-profile`) that includes the following keys:

```json
{
  "profiles": [
    {
      "name": "my-google-drive-profile",
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
