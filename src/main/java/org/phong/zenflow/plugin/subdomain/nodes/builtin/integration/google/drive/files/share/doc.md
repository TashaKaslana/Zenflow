# Google Drive - Share File Node

## Overview
Adds a permission to a file in Google Drive, allowing it to be shared with a user, group, domain, or anyone.

## Node Information
- **Key**: `google-drive:files.share`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:google-drive-logo`
- **Tags**: `google`, `drive`, `storage`, `share`

## Input/Output
### Input
- `profile` (string, required): Secret profile group containing OAuth credentials.
- `fileId` (string, required): ID of the file to share.
- `role` (string, required): Role to assign (`owner`, `organizer`, `fileOrganizer`, `writer`, `commenter`, `reader`).
- `type` (string, required): Grantee type (`user`, `group`, `domain`, `anyone`).
- `emailAddress` (string, optional): Email of the user or group if applicable.

### Output
- `permission` (object): Permission resource created for the file.

## Credentials
Use the same credential profile structure as the List Files node.
