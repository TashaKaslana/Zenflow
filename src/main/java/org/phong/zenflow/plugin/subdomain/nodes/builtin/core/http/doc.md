# HTTP Request Node

## Overview

Executes an HTTP request to a specified URL using the chosen method.

## Node Information

- **Key**: `core:http.request`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:globe`
- **Tags**: `http`, `request`, `network`

## Description

Sends an HTTP request to a specified URL with the given method and optional body and headers.

## Input/Output

### Input
- `url` (string, required): The destination URL.
- `method` (string, required): One of `GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `HEAD`, or `OPTIONS`.
- `body` (object, optional): Request payload for methods like POST or PUT.
- `headers` (object, optional): Key-value pairs of HTTP headers.

### Output
- `status_code` (integer): HTTP status code from the response.
- `headers` (object): Response headers.
- `body` (object): Response body parsed as JSON when possible.

## Secrets

Accepts a list of secret references, each containing a `key` field.
