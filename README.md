# MAISON — Hosting Ready

Clean deployment package for **Spring Boot + Supabase PostgreSQL + Railway**.

## Contents
- `backend/` — the complete Java Spring Boot application, with the customer/admin frontend bundled into `src/main/resources/static/`.
- `database/schema.sql` — PostgreSQL/Supabase schema.
- `database/seed.sql` — initial restaurant tables and default admin.

## Deliberately removed
- PHP API files and PHPMailer
- MySQL driver/configuration
- H2 database/configuration
- Old PHP `.htaccess` files
- Duplicate unused frontend JS files
- Old MySQL/H2 SQL setup files
- Separate duplicate frontend copy

## Supabase
Run `database/schema.sql` first, then `database/seed.sql` in Supabase SQL Editor.

## Railway
Deploy the `backend` folder. Set:
- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `APP_URL`

The application listens on Railway's `PORT` automatically.

## Admin
Default account: `admin@maison.com` / `admin`.
Change the password immediately after first login.
