# Google Drive - Trash File Node

## Overview
Moves a file to the trash in Google Drive.

## Node Information
- **Key**: `google-drive:files.trash`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:google-drive-logo`
- **Tags**: `google`, `drive`, `storage`, `trash`

## Input/Output
### Input
- `profile` (string, required): Secret profile group containing OAuth credentials.
- `fileId` (string, required): ID of the file to move to trash.

### Output
- `file` (object): Updated file metadata including the `trashed` state.

## Credentials
Use the same credential profile structure as the List Files node.
