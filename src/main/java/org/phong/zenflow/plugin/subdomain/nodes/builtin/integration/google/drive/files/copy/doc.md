# Google Drive - Copy File Node

## Overview
Creates a copy of an existing file in Google Drive. You can optionally specify a new name and destination folder for the copy.

## Node Information
- **Key**: `google-drive:files.copy`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:google-drive-logo`
- **Tags**: `google`, `drive`, `storage`, `copy`

## Input/Output
### Input
- `profile` (string, required): Secret profile group containing OAuth credentials.
- `fileId` (string, required): ID of the file to copy.
- `destinationFolderId` (string, optional): Folder ID where the copy should be placed.
- `name` (string, optional): New name for the copied file.

### Output
- `id` (string): ID of the copied file.
- `name` (string): Name of the copied file.
- `mimeType` (string): MIME type of the copied file.
- `size` (number): Size of the file in bytes.
- `parents` (array of strings): Parent folder IDs for the copied file.
- `webViewLink` (string): Link to view the file in Google Drive.

## Credentials
Use the same credential profile structure as the List Files node.
