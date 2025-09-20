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
Configure a plugin-level OAuth profile (e.g., `my-google-drive-profile`) with the following keys:

- `CLIENT_ID`
- `CLIENT_SECRET`
- `REFRESH_TOKEN`

Reference the profile name via the `profile` input field. The system retrieves these keys from the plugin-level profile before execution.
