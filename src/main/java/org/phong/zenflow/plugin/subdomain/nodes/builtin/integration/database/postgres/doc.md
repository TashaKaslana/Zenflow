# PostgreSQL Database Node

## Overview

Executes SQL queries against PostgreSQL databases with support for parameterized queries, connection pooling, and transaction management.

## Node Information

- **Key**: `integration:database.postgres`
- **Version**: `1.0.0`
- **Type**: `action`
- **Icon**: `ph:database`
- **Tags**: `database`, `postgresql`, `sql`, `data`

## Description

The PostgreSQL Database node enables workflows to interact with PostgreSQL databases through SQL queries. It supports SELECT, INSERT, UPDATE, DELETE operations with parameterized queries for security, connection pooling for performance, and proper transaction handling.

## Input/Output

### Input
- `host` (string): PostgreSQL server hostname or IP address (default: "localhost")
- `port` (integer): PostgreSQL server port (default: 5432, range: 1-65535)
- `database` (string, required): Database name to connect to
- `username` (string, required): Database username (use secrets injection)
- `password` (string, required): Database password (use secrets injection)
- `query` (string, required): SQL query to execute (supports ? placeholders for parameters)
- `parameters` (array): Indexed parameters for parameterized queries
  - `index` (integer): 1-based parameter index
  - `type` (string): PostgreSQL parameter type (VARCHAR, INTEGER, TIMESTAMP, etc.)
  - `value` (any): Parameter value
- `timeout` (integer): Query timeout in seconds (default: 30)
- `max_rows` (integer): Maximum rows to return for SELECT queries

### Output
- `affected_rows` (integer): Number of rows affected (INSERT/UPDATE/DELETE)
- `result_set` (array): Query results for SELECT operations
- `columns` (array): Column metadata including names and types
- `execution_time` (integer): Query execution time in milliseconds
- `connection_info` (object): Connection details (host, database, etc.)
- `query_type` (string): Type of SQL operation (SELECT, INSERT, UPDATE, DELETE)

## Usage Examples

### Simple SELECT Query
```json
{
  "key": "fetch-users",
  "type": "PLUGIN",
  "pluginNode": {
    "pluginKey": "integration",
    "nodeKey": "database.postgres",
    "version": "1.0.0"
  },
  "config": {
    "input": {
      "host": "db.example.com",
      "port": 5432,
      "database": "myapp",
      "username": "{{secrets.db_username}}",
      "password": "{{secrets.db_password}}",
      "query": "SELECT id, name, email FROM users WHERE active = true"
    }
  }
}
```

### Parameterized Query with User Input
```json
{
  "input": {
    "query": "SELECT * FROM orders WHERE customer_id = ? AND order_date >= ?",
    "parameters": [
      {
        "index": 1,
        "type": "INTEGER",
        "value": "{{webhook.output.payload.customer_id}}"
      },
      {
        "index": 2,
        "type": "TIMESTAMP",
        "value": "{{start_date}}"
      }
    ]
  }
}
```

### INSERT Operation
```json
{
  "input": {
    "query": "INSERT INTO users (name, email, created_at) VALUES (?, ?, NOW())",
    "parameters": [
      {
        "index": 1,
        "type": "VARCHAR",
        "value": "{{user_data.name}}"
      },
      {
        "index": 2,
        "type": "VARCHAR",
        "value": "{{user_data.email}}"
      }
    ]
  }
}
```

### Bulk UPDATE Operation
```json
{
  "input": {
    "query": "UPDATE products SET price = price * ? WHERE category = ?",
    "parameters": [
      {
        "index": 1,
        "type": "DECIMAL",
        "value": 1.1
      },
      {
        "index": 2,
        "type": "VARCHAR",
        "value": "electronics"
      }
    ]
  }
}
```

## Response Examples

### SELECT Query Response
```json
{
  "result_set": [
    {"id": 1, "name": "John Doe", "email": "john@example.com"},
    {"id": 2, "name": "Jane Smith", "email": "jane@example.com"}
  ],
  "columns": [
    {"name": "id", "type": "INTEGER"},
    {"name": "name", "type": "VARCHAR"},
    {"name": "email", "type": "VARCHAR"}
  ],
  "affected_rows": 0,
  "execution_time": 45,
  "query_type": "SELECT"
}
```

### INSERT/UPDATE/DELETE Response
```json
{
  "affected_rows": 3,
  "result_set": [],
  "execution_time": 12,
  "query_type": "UPDATE"
}
```

## Parameter Types

Supported PostgreSQL parameter types:
- `VARCHAR`, `TEXT` - String values
- `INTEGER`, `BIGINT` - Numeric integers
- `DECIMAL`, `NUMERIC` - Decimal numbers
- `BOOLEAN` - Boolean values
- `TIMESTAMP`, `DATE`, `TIME` - Date/time values
- `JSON`, `JSONB` - JSON data
- `UUID` - UUID values
- `BYTEA` - Binary data

## Common Use Cases

- **Data Retrieval**: Fetch user data, reports, and analytics
- **User Management**: Create, update, and manage user accounts
- **Order Processing**: Handle e-commerce transactions and orders
- **Reporting**: Generate business reports and dashboards
- **Data Migration**: Transfer data between systems
- **Audit Logging**: Record workflow activities and changes
- **Configuration Management**: Store and retrieve application settings

## Security Best Practices

- Always use parameterized queries to prevent SQL injection
- Store database credentials securely using secrets management
- Use least-privilege database accounts
- Enable SSL/TLS for database connections
- Validate and sanitize input parameters
- Implement proper error handling without exposing sensitive data
- Use connection pooling for better performance

## Error Handling

Common error scenarios:
- Connection timeouts and failures
- Authentication errors
- SQL syntax errors
- Parameter type mismatches
- Database constraint violations
- Insufficient permissions
- Network connectivity issues
