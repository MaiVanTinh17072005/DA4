# Database Setup Guide - Discord Mini Server

## Tổng Quan

Hướng dẫn cấu hình và kết nối PostgreSQL database cho Discord Mini Server theo đúng schema đã định nghĩa.

## Database Schema

### Tên Database: `discordminidb`

### Các Bảng Chính

| Bảng | Mô tả |
|------|-------|
| **users** | Thông tin tài khoản người dùng |
| **friend** | Quan hệ bạn bè giữa các user |
| **groups** | Thông tin nhóm chat |
| **group_member** | Thành viên trong nhóm |
| **message** | Tin nhắn (cá nhân và nhóm) |
| **call_history** | Lịch sử cuộc gọi |
| **livestream** | Thông tin livestream |
| **ai_config** | Cấu hình AI cho từng user |

## Bước 1: Cài Đặt PostgreSQL

### Windows
```bash
# Download từ: https://www.postgresql.org/download/windows/
# Hoặc dùng chocolatey:
choco install postgresql
```

### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
```

### macOS
```bash
brew install postgresql
```

## Bước 2: Tạo Database

### Khởi động PostgreSQL
```bash
# Windows
pg_ctl -D "C:\Program Files\PostgreSQL\15\data" start

# Linux/macOS
sudo service postgresql start
```

### Tạo Database
```bash
# Đăng nhập PostgreSQL
psql -U postgres

# Trong PostgreSQL shell:
CREATE DATABASE discordminidb;

# Kiểm tra database đã tạo
\l

# Kết nối đến database
\c discordminidb

# Thoát
\q
```

## Bước 3: Cấu Hình Server

### File: `application.properties`

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/discordminidb
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD_HERE  # Thay bằng password của bạn
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update  # Tự động tạo/cập nhật bảng
spring.jpa.show-sql=true              # Hiển thị SQL queries
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
```

### Thay Đổi Password

Mở file `application.properties` và thay:
```properties
spring.datasource.password=your_password_here
```
thành password PostgreSQL của bạn.

## Bước 4: Chạy Server

### Cài Dependencies
```bash
cd BE_app
mvn clean install
```

### Chạy Server
```bash
mvn spring-boot:run
```

### Kiểm Tra Logs

Server sẽ tự động tạo các bảng. Bạn sẽ thấy logs như:
```
Hibernate: create table users (...)
Hibernate: create table friend (...)
Hibernate: create table groups (...)
...
```

## Bước 5: Kiểm Tra Database

### Kết nối PostgreSQL
```bash
psql -U postgres -d discordminidb
```

### Kiểm tra các bảng đã tạo
```sql
-- Liệt kê tất cả bảng
\dt

-- Xem cấu trúc bảng users
\d users

-- Xem cấu trúc bảng friend
\d friend

-- Xem cấu trúc bảng message
\d message
```

### Kết quả mong đợi
```
                List of relations
 Schema |     Name      | Type  |  Owner   
--------+---------------+-------+----------
 public | ai_config     | table | postgres
 public | call_history  | table | postgres
 public | friend        | table | postgres
 public | group_member  | table | postgres
 public | groups        | table | postgres
 public | livestream    | table | postgres
 public | message       | table | postgres
 public | users         | table | postgres
```

## Chi Tiết Cấu Trúc Bảng

### 1. Table: `users`
```sql
Column       | Type                     | Nullable | Default
-------------+--------------------------+----------+---------
id           | bigint                   | not null | nextval(...)
email        | varchar(255)             | not null | 
username     | varchar(50)              | not null | 
password     | varchar(255)             | not null | 
avatar_url   | varchar(500)             |          | 
status       | varchar(20)              |          | 'offline'
created_at   | timestamp                | not null | 
updated_at   | timestamp                | not null | 

Indexes:
    "users_pkey" PRIMARY KEY (id)
    "uk_email" UNIQUE (email)
    "uk_username" UNIQUE (username)
```

### 2. Table: `friend`
```sql
Column       | Type                     | Nullable | Default
-------------+--------------------------+----------+---------
friend_id    | bigint                   | not null | nextval(...)
user_id      | bigint                   | not null | 
target_id    | bigint                   | not null | 
status       | varchar(20)              | not null | 'pending'
created_at   | timestamp                | not null | 

Indexes:
    "friend_pkey" PRIMARY KEY (friend_id)
```

### 3. Table: `groups`
```sql
Column        | Type                     | Nullable | Default
--------------+--------------------------+----------+---------
group_id      | bigint                   | not null | nextval(...)
name          | varchar(100)             | not null | 
owner_id      | bigint                   | not null | 
description   | varchar(500)             |          | 
member_count  | integer                  | not null | 0
created_at    | timestamp                | not null | 

Indexes:
    "groups_pkey" PRIMARY KEY (group_id)
```

### 4. Table: `message`
```sql
Column        | Type                     | Nullable | Default
--------------+--------------------------+----------+---------
msg_id        | bigint                   | not null | nextval(...)
sender_id     | bigint                   | not null | 
receiver_id   | bigint                   |          | 
group_id      | bigint                   |          | 
content       | text                     |          | 
msg_type      | varchar(20)              | not null | 'text'
file_path     | varchar(500)             |          | 
timestamp     | timestamp                | not null | 
is_read       | boolean                  | not null | false
aes_encrypted | boolean                  | not null | false

Indexes:
    "message_pkey" PRIMARY KEY (msg_id)
```

## Troubleshooting

### Lỗi: "Connection refused"
**Nguyên nhân:** PostgreSQL chưa chạy

**Giải pháp:**
```bash
# Windows
pg_ctl -D "C:\Program Files\PostgreSQL\15\data" start

# Linux
sudo service postgresql start
```

### Lỗi: "database does not exist"
**Nguyên nhân:** Chưa tạo database

**Giải pháp:**
```bash
psql -U postgres
CREATE DATABASE discordminidb;
```

### Lỗi: "password authentication failed"
**Nguyên nhân:** Password sai trong `application.properties`

**Giải pháp:**
1. Kiểm tra password PostgreSQL
2. Cập nhật trong `application.properties`

### Lỗi: "Port 5432 already in use"
**Nguyên nhân:** PostgreSQL đã chạy hoặc port bị chiếm

**Giải pháp:**
```bash
# Kiểm tra process đang dùng port 5432
netstat -ano | findstr :5432

# Hoặc thay đổi port trong PostgreSQL config
```

## Testing Database Connection

### Test 1: Đăng ký user mới
1. Chạy server
2. Chạy client và đăng ký user mới
3. Kiểm tra database:
```sql
SELECT * FROM users;
```

### Test 2: Kiểm tra auto-increment
```sql
-- Xem sequence hiện tại
SELECT currval('users_id_seq');

-- Xem user mới nhất
SELECT * FROM users ORDER BY id DESC LIMIT 1;
```

### Test 3: Kiểm tra constraints
```sql
-- Test unique email (sẽ lỗi nếu email đã tồn tại)
INSERT INTO users (email, username, password, created_at, updated_at) 
VALUES ('test@example.com', 'testuser2', 'password', NOW(), NOW());
```

## Backup và Restore

### Backup Database
```bash
pg_dump -U postgres discordminidb > backup.sql
```

### Restore Database
```bash
psql -U postgres discordminidb < backup.sql
```

## Kết Luận

Database đã được cấu hình hoàn chỉnh với:
- ✅ 8 bảng chính theo schema
- ✅ Tất cả constraints và indexes
- ✅ Auto-increment primary keys
- ✅ Timestamps tự động
- ✅ Hibernate auto-create tables

Chỉ cần:
1. Tạo database `discordminidb`
2. Cập nhật password trong `application.properties`
3. Chạy server → Bảng tự động được tạo!
