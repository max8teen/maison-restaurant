-- MAISON Fine Dining - initial data
INSERT INTO restaurant_tables (id, table_number, capacity, location, status) VALUES
(1, 'T-01', 2, 'window', 'available'),
(2, 'T-02', 2, 'window', 'available'),
(3, 'T-03', 4, 'indoor', 'available'),
(4, 'T-04', 4, 'indoor', 'available'),
(5, 'T-05', 6, 'indoor', 'available'),
(6, 'T-06', 2, 'outdoor', 'available'),
(7, 'T-07', 4, 'outdoor', 'available'),
(8, 'T-08', 8, 'private', 'available')
ON CONFLICT (id) DO NOTHING;

-- Default admin: admin@maison.com / admin
INSERT INTO users (id, full_name, email, phone, password, role) VALUES
(1, 'Admin', 'admin@maison.com', '9876543210', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'admin')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('restaurant_tables','id'), GREATEST((SELECT COALESCE(MAX(id),1) FROM restaurant_tables),1), true);
SELECT setval(pg_get_serial_sequence('users','id'), GREATEST((SELECT COALESCE(MAX(id),1) FROM users),1), true);
