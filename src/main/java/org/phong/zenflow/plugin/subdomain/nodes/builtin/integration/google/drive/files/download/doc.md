# Google Drive - Download File Node

## Overview
Downloads a file from Google Drive and returns its content as a Base64 encoded string.

## Node Information
- **Key**: `google-drive:files.download`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:google-drive-logo`
- **Tags**: `google`, `drive`, `storage`, `download`

## Input/Output
### Input
- `profile` (string, required): Secret profile group containing OAuth credentials.
- `fileId` (string, required): ID of the file to download.

### Output
- `fileId` (string): ID of the downloaded file.
- `name` (string): Name of the file.
- `mimeType` (string): MIME type of the file.
- `data` (string): Base64 encoded file content.

## Credentials
Use the same credential profile structure as the List Files node.
