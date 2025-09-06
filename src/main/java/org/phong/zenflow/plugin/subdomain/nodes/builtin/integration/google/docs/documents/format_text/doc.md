# Google Docs - Format Text Node

## Overview
Applies text formatting to an existing Google Docs document using the Google Docs API v1.

## Node Information
- **Key**: `google-docs:documents.format_text`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:text-b-bold`
- **Tags**: `google`, `docs`, `document`, `integration`

## Description
Updates the style of text within a specified range, such as applying bold formatting or changing the font size. Docs client instances are cached through the global node resource manager to maximize efficiency in high concurrency environments.

## Input/Output
### Input
- `profile` (string, required): Secret profile group containing OAuth credentials.
- `documentId` (string, required): ID of the document to modify.
- `startIndex` (integer, required): Start index of the range to format.
- `endIndex` (integer, required): End index of the range to format.
- `bold` (boolean, optional): Whether to apply bold formatting. Defaults to `false`.
- `fontSize` (number, optional): Font size in points to apply to the range.

### Output
- `documentId` (string): ID of the modified document.
- `formatted` (boolean): Indicates the format operation succeeded.

## Usage Example
```json
{
  "input": {
    "profile": "my-google-docs-profile",
    "documentId": "1A2B3C4D",
    "startIndex": 1,
    "endIndex": 5,
    "bold": true,
    "fontSize": 14
  }
}
```

## Credentials
Create a secret profile group (e.g., `my-google-docs-profile`) that includes the following keys:

```json
{
  "profiles": [
    {
      "name": "my-google-docs-profile",
      "keys": {
        "CLIENT_ID": "<client id>",
        "CLIENT_SECRET": "<client secret>",
        "REFRESH_TOKEN": "<refresh token>"
      }
    }
  ]
}
```

The profile name is supplied via the `profile` input field and the system resolves these keys to their secret values before execution.
