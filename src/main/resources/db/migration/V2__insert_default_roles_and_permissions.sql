-- Insert default roles
INSERT INTO roles (id, name, description, created_at, updated_at)
VALUES (gen_random_uuid(), 'USER', 'Standard user with basic permissions', NOW(), NOW()),
       (gen_random_uuid(), 'ADMIN', 'Administrator with elevated permissions', NOW(), NOW()),
       (gen_random_uuid(), 'OWNER', 'Owner with full system access', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Insert default permissions
INSERT INTO permissions (id, feature, action, description)
VALUES
    -- User management permissions
    (gen_random_uuid(), 'user', 'create', 'Create new users'),
    (gen_random_uuid(), 'user', 'read', 'View user information'),
    (gen_random_uuid(), 'user', 'update', 'Update user information'),
    (gen_random_uuid(), 'user', 'delete', 'Delete users'),

    -- Role management permissions
    (gen_random_uuid(), 'role', 'create', 'Create new roles'),
    (gen_random_uuid(), 'role', 'read', 'View role information'),
    (gen_random_uuid(), 'role', 'update', 'Update role information'),
    (gen_random_uuid(), 'role', 'delete', 'Delete roles'),

    -- Permission management permissions
    (gen_random_uuid(), 'permission', 'create', 'Create new permissions'),
    (gen_random_uuid(), 'permission', 'read', 'View permission information'),
    (gen_random_uuid(), 'permission', 'update', 'Update permission information'),
    (gen_random_uuid(), 'permission', 'delete', 'Delete permissions'),

    -- System administration permissions
    (gen_random_uuid(), 'system', 'admin', 'Full system administration'),
    (gen_random_uuid(), 'system', 'config', 'System configuration')
ON CONFLICT (feature, action) DO NOTHING;

-- Workflow permissions
INSERT INTO permissions (id, feature, action, description)
VALUES (gen_random_uuid(), 'workflow', 'create', 'Create new workflow'),
       (gen_random_uuid(), 'workflow', 'read', 'View workflows'),
       (gen_random_uuid(), 'workflow', 'update', 'Update workflows'),
       (gen_random_uuid(), 'workflow', 'delete', 'Delete workflows'),
       (gen_random_uuid(), 'workflow', 'execute', 'Execute workflows'),
       (gen_random_uuid(), 'workflow', 'version', 'View and rollback workflow versions'),

-- Project permissions
       (gen_random_uuid(), 'project', 'create', 'Create new projects'),
       (gen_random_uuid(), 'project', 'read', 'View project information'),
       (gen_random_uuid(), 'project', 'update', 'Update project information'),
       (gen_random_uuid(), 'project', 'delete', 'Delete projects'),

-- Plugin permissions
       (gen_random_uuid(), 'plugin', 'install', 'Install plugins'),
       (gen_random_uuid(), 'plugin', 'read', 'View plugin list'),
       (gen_random_uuid(), 'plugin', 'update', 'Update plugin'),
       (gen_random_uuid(), 'plugin', 'delete', 'Remove plugin'),

-- Trigger permissions
       (gen_random_uuid(), 'trigger', 'create', 'Create triggers'),
       (gen_random_uuid(), 'trigger', 'read', 'View trigger list'),
       (gen_random_uuid(), 'trigger', 'update', 'Update trigger'),
       (gen_random_uuid(), 'trigger', 'delete', 'Delete trigger'),

-- Secret permissions
       (gen_random_uuid(), 'secret', 'create', 'Create secrets'),
       (gen_random_uuid(), 'secret', 'read', 'View secrets'),
       (gen_random_uuid(), 'secret', 'update', 'Update secrets'),
       (gen_random_uuid(), 'secret', 'delete', 'Delete secrets'),

-- Log permissions
       (gen_random_uuid(), 'log', 'view', 'View execution logs'),
       (gen_random_uuid(), 'log', 'export', 'Export logs'),
       (gen_random_uuid(), 'log', 'delete', 'Delete logs'),

-- Audit log
       (gen_random_uuid(), 'audit_log', 'read', 'View audit logs')
ON CONFLICT (feature, action) DO NOTHING;


-- Assign permissions to roles
-- Owner gets all permissions
-- Owner gets all permissions (giữ nguyên)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.name = 'OWNER'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Admin gets most permissions except system admin
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
  AND NOT (p.feature = 'system' AND p.action = 'admin')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- User basic permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.name = 'USER'
  AND (
    (p.feature = 'workflow') OR
    (p.feature = 'project') OR
    (p.feature = 'plugin' AND p.action IN ('read', 'install')) OR
    (p.feature = 'trigger') OR
    (p.feature = 'secret') OR
    (p.feature = 'log' AND p.action IN ('view', 'export'))
    )
ON CONFLICT (role_id, permission_id) DO NOTHING;
