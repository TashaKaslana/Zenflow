# Google Drive - Upload File Node

## Overview
Uploads a file to Google Drive from Base64 encoded data.

## Node Information
- **Key**: `google-drive:files.upload`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:google-drive-logo`
- **Tags**: `google`, `drive`, `storage`, `upload`

## Input/Output
### Input
- `profile` (string, required): Secret profile group containing OAuth credentials.
- `name` (string, required): Name of the file to create.
- `mimeType` (string): MIME type of the file.
- `data` (string, required): Base64 encoded file content.

### Output
- `file` (object): Metadata of the uploaded file.

## Credentials
Use the plugin-level OAuth profile referenced by the `profile` input field. Configure this profile once with `CLIENT_ID`, `CLIENT_SECRET`, and `REFRESH_TOKEN`.
