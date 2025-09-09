# Google Drive - Get File Metadata Node

## Overview
Retrieves metadata for a single Google Drive file using the Google Drive API v3.

## Node Information
- **Key**: `google-drive:files.getMetadata`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:google-drive-logo`
- **Tags**: `google`, `drive`, `storage`, `metadata`

## Input/Output
### Input
- `profile` (string, required): Secret profile group containing OAuth credentials.
- `fileId` (string, required): ID of the file to retrieve metadata for.

### Output
- `file` (object): Metadata of the requested file.

## Credentials
Use the plugin-level OAuth profile referenced by the `profile` input field. Configure this profile once with `CLIENT_ID`, `CLIENT_SECRET`, and `REFRESH_TOKEN`.
