-- Insert 3 accounts with each role
INSERT INTO users (id, username, email, password_hash, role_id)
SELECT gen_random_uuid(), u.username, u.email, u.password_hash, r.id
FROM (
         VALUES
             ('owner_user', 'owner@example.com', 'fakehash_owner'),
             ('admin_user', 'admin@example.com', 'fakehash_admin'),
             ('normal_user', 'user@example.com', 'fakehash_user')
     ) AS u(username, email, password_hash)
         JOIN roles r ON LOWER(r.name) =
                         CASE u.username
                             WHEN 'owner_user' THEN 'USER'
                             WHEN 'admin_user' THEN 'ADMIN'
                             ELSE 'USER'
                             END
    ON CONFLICT (email) DO NOTHING;
