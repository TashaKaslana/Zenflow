# Google Drive - Update File Node

## Overview
Updates metadata or content of an existing file in Google Drive. Use this node to rename a file, change its description or MIME type, or replace the file's contents.

## Node Information
- **Key**: `google-drive:files.update`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:google-drive-logo`
- **Tags**: `google`, `drive`, `storage`, `update`

## Input/Output
### Input
- `profile` (string, required): Secret profile group containing OAuth credentials.
- `fileId` (string, required): ID of the file to update.
- `name` (string, optional): New name for the file.
- `description` (string, optional): Description for the file.
- `mimeType` (string, optional): MIME type of the file.
- `contentBase64` (string, optional): Base64-encoded file content to replace the existing content.

### Output
- `file` (object): Updated file resource returned from the API.

## Credentials
Use the same credential profile structure as the List Files node.
