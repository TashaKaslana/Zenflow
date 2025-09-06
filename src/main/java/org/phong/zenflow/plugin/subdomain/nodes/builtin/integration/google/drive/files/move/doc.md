# Google Drive - Move File Node

## Overview
Moves an existing file to a different folder in Google Drive.

## Node Information
- **Key**: `google-drive:files.move`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:google-drive-logo`
- **Tags**: `google`, `drive`, `storage`, `move`

## Input/Output
### Input
- `profile` (string, required): Secret profile group containing OAuth credentials.
- `fileId` (string, required): ID of the file to move.
- `destinationFolderId` (string, required): ID of the folder to move the file to.
- `sourceFolderId` (string, optional): ID of the current parent folder. If not provided, the current parent is fetched automatically.

### Output
- `id` (string): ID of the moved file.
- `parents` (array of strings): New parent folder IDs of the file.

## Credentials
Use the same credential profile structure as the List Files node.
