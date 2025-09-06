# Google Drive - Delete File Node

## Overview
Permanently deletes a file from Google Drive.

## Node Information
- **Key**: `google-drive:files.delete`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:google-drive-logo`
- **Tags**: `google`, `drive`, `storage`, `delete`

## Input/Output
### Input
- `profile` (string, required): Secret profile group containing OAuth credentials.
- `fileId` (string, required): ID of the file to delete.

### Output
- `deleted` (boolean): Indicates whether the file was deleted.

## Credentials
Use the same credential profile structure as the List Files node.
