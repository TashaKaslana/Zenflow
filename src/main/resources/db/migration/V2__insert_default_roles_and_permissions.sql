-- Insert default roles
INSERT INTO roles (id, name, description, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'USER', 'Standard user with basic permissions', NOW(), NOW()),
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

-- Assign permissions to roles
-- Owner gets all permissions
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
AND p.feature IN ('user', 'role', 'permission')
AND NOT (p.feature = 'system' AND p.action = 'admin')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- User gets basic read permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'USER'
AND p.feature = 'user'
AND p.action = 'read'
ON CONFLICT (role_id, permission_id) DO NOTHING;
