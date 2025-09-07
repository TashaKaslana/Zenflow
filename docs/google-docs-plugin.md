# Google Docs Plugin

The Google Docs plugin provides workflow nodes that interact with Google Docs documents. It allows workflows to create and manage documents within a user's Google account.

## Categories

| Category | Description |
|----------|-------------|
| Document Management | Operations on the document as a whole, including creating, getting, and deleting documents. |
| Content Manipulation | Operations on the content inside a document, such as inserting and deleting text, or adding images and tables. |
| Formatting | Operations that apply styling to text, paragraphs, lists, and tables, such as changing font size or applying bold formatting. |
| Structural Elements | Operations that manage structural parts of a document, including headers, footers, and named ranges. |
| Collaboration | Operations related to collaborative features like comments and suggestions. |

## Available Nodes

| Category | Node | Description |
|---------|------|-------------|
| Document Management | `google-docs:documents.create` | Create a blank Google Docs document |
| Document Management | `google-docs:documents.get` | Retrieve a Google Docs document |
| Document Management | `google-docs:documents.delete` | Delete a Google Docs document |
| Content Manipulation | `google-docs:documents.append_text` | Append text to a Google Docs document |
| Content Manipulation | `google-docs:documents.delete_text` | Delete a range of text from a document |
| Formatting | `google-docs:documents.format_text` | Apply text styling to a range in a document |
| Structural Elements | `google-docs:documents.add_header` | Create a header and insert text |
| Collaboration | `google-docs:documents.add_comment` | Add a comment to a document |

## Credentials

All nodes require an OAuth2 credential profile containing the following keys:

- `CLIENT_ID`
- `CLIENT_SECRET`
- `REFRESH_TOKEN`

Define profiles as secret groups and reference them through the `profile` property in node inputs.

## Notes

This plugin relies on the [Google Docs API](https://developers.google.com/docs/api) and uses the Docs v1 Java client.
