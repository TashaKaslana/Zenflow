# Google Drive Plugin

The Google Drive plugin provides a collection of workflow nodes that interact with Google Drive files. These nodes allow workflows to list and inspect files, transfer content, manage permissions and organize files.

## Available Nodes

| Category | Node                              | Description                           |
|----------|-----------------------------------|---------------------------------------|
| Files    | `google-drive:files.list`         | List files in Google Drive            |
| Files    | `google-drive:files.get_metadata` | Retrieve file metadata                |
| Files    | `google-drive:files.download`     | Download file content as Base64       |
| Files    | `google-drive:files.upload`       | Upload a new file from Base64 content |
| Files    | `google-drive:files.copy`         | Create a copy of a file               |
| Files    | `google-drive:files.move`         | Move a file to another folder         |
| Files    | `google-drive:files.delete`       | Permanently delete a file             |
| Files    | `google-drive:files.trash`        | Move a file to the trash              |
| Files    | `google-drive:files.share`        | Share a file by creating a permission |
| Files    | `google-drive:files.update`       | Update file metadata or content       |

## Credentials

All nodes require an OAuth2 credential profile containing the following keys:

- `CLIENT_ID`
- `CLIENT_SECRET`
- `REFRESH_TOKEN`

Define profiles as secret groups and reference them through the `profile` property in node inputs.

## Notes

This plugin relies on the [Google Drive API](https://developers.google.com/drive/api) and uses the Drive v3 Java client.
