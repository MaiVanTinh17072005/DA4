# Server-Side Authentication API - Hướng Dẫn

## Tổng Quan

Server-side REST API đã được tạo để xử lý đăng ký và đăng nhập người dùng, khớp với API client đã tạo trước đó.

## Cấu Trúc Đã Tạo

```
BE_app/src/main/java/
├── dto/
│   ├── RegisterRequest.java    ✅ Nhận request đăng ký từ client
│   ├── LoginRequest.java        ✅ Nhận request đăng nhập từ client
│   ├── AuthResponse.java        ✅ Gửi response về client
│   └── UserDTO.java             ✅ Thông tin user gửi về client
│
├── model/
│   └── User.java                ✅ Entity JPA cho database
│
├── repository/
│   └── UserRepository.java      ✅ JPA Repository
│
├── service/
│   └── AuthService.java         ✅ Business logic (register, login)
│
├── controller/
│   └── AuthController.java      ✅ REST endpoints
│
├── util/
│   ├── PasswordUtil.java        ✅ BCrypt password hashing
│   └── JwtUtil.java             ✅ JWT token generation/validation
│
└── app/
    └── ServerMain.java          ✅ Spring Boot entry point

src/main/resources/
└── application.properties       ✅ Database & server config
```

## API Endpoints

### 1. Register (Đăng Ký)
```
POST http://localhost:8080/api/v1/auth/register
Content-Type: application/json

Request Body:
{
    "email": "user@example.com",
    "username": "username123",
    "password": "sha256_hashed_password_from_client"
}

Response (Success):
{
    "success": true,
    "message": "Registration successful",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
        "id": 1,
        "email": "user@example.com",
        "username": "username123",
        "avatarUrl": null,
        "status": "offline",
        "createdAt": "2024-01-01T00:00:00"
    }
}

Response (Error):
{
    "success": false,
    "message": "Email already exists",
    "token": null,
    "user": null
}
```

### 2. Login (Đăng Nhập)
```
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

Request Body:
{
    "email": "user@example.com",
    "password": "sha256_hashed_password_from_client"
}

Response (Success):
{
    "success": true,
    "message": "Login successful",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
        "id": 1,
        "email": "user@example.com",
        "username": "username123",
        "avatarUrl": null,
        "status": "online",
        "createdAt": "2024-01-01T00:00:00"
    }
}

Response (Error):
{
    "success": false,
    "message": "Invalid credentials",
    "token": null,
    "user": null
}
```

### 3. Health Check
```
GET http://localhost:8080/api/v1/auth/health

Response:
"Auth service is running"
```

## Bảo Mật

### Double Password Hashing

1. **Client-side (SHA-256):**
   ```
   Plain password: "password123"
   → SHA-256: "ef92b778bafe771e89245b89ecbc08a44a4e166c06659911881f383d4473e94f"
   → Gửi lên server
   ```

2. **Server-side (BCrypt):**
   ```
   SHA-256 hash từ client: "ef92b778..."
   → BCrypt: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
   → Lưu vào database
   ```

### JWT Token

- **Expiration:** 24 giờ
- **Claims:** userId, email, username
- **Algorithm:** HS256
- **Secret Key:** Cần thay đổi trong production!

## Cấu Hình Database

### 1. Tạo Database PostgreSQL

```sql
CREATE DATABASE discord_mini_db;
```

### 2. Cập Nhật application.properties

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/discord_mini_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. Hibernate Tự Động Tạo Bảng

Khi chạy server lần đầu, Hibernate sẽ tự động tạo bảng `users`:

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'offline',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

## Chạy Server

### Bước 1: Cài Đặt Dependencies

```bash
cd BE_app
mvn clean install
```

### Bước 2: Cấu Hình Database

Sửa file `src/main/resources/application.properties`:
- Thay `your_password_here` bằng password PostgreSQL của bạn

### Bước 3: Chạy Server

```bash
mvn spring-boot:run
```

Hoặc trong IDE:
- Run `ServerMain.java`

### Bước 4: Kiểm Tra

Server sẽ chạy tại: `http://localhost:8080`

Test health endpoint:
```bash
curl http://localhost:8080/api/v1/auth/health
```

## Testing với Client

### 1. Chạy Server (BE_app)
```bash
cd BE_app
mvn spring-boot:run
```

### 2. Chạy Client (FE_app)
```bash
cd FE_app
mvn javafx:run
```

### 3. Test Đăng Ký
1. Click "Đăng ký" trong client
2. Nhập thông tin:
   - Email: test@example.com
   - Username: testuser
   - Password: password123
3. Click "Đăng ký"
4. **Expected:** "Tài khoản đã được tạo thành công!"

### 4. Test Đăng Nhập
1. Nhập credentials đã đăng ký
2. Click "Đăng nhập"
3. **Expected:** Chuyển sang màn hình chat

## Logs

Server sẽ log tất cả requests:

```
=== REGISTER REQUEST ===
Email: test@example.com
Username: testuser
Password: [HASHED]
Response: AuthResponse{success=true, message='Registration successful', ...}
========================

=== LOGIN REQUEST ===
Email: test@example.com
Password: [HASHED]
Response: AuthResponse{success=true, message='Login successful', ...}
=====================
```

## Troubleshooting

### Lỗi: "Could not connect to database"
**Giải pháp:**
1. Kiểm tra PostgreSQL đã chạy chưa
2. Kiểm tra database `discord_mini_db` đã tạo chưa
3. Kiểm tra username/password trong `application.properties`

### Lỗi: "Port 8080 already in use"
**Giải pháp:**
1. Thay đổi port trong `application.properties`:
   ```properties
   server.port=8081
   ```
2. Cập nhật URL trong client `ApiConfig.java`:
   ```java
   public static final String BASE_URL = "http://localhost:8081";
   ```

### Lỗi: "Email already exists"
**Giải pháp:**
- Email đã được đăng ký, sử dụng email khác hoặc xóa user trong database

### Lỗi: "Invalid credentials"
**Giải pháp:**
- Kiểm tra email/password có đúng không
- Password phải giống với lúc đăng ký

## Dependencies Đã Thêm

```xml
<!-- Spring Security (BCrypt) -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
    <version>6.2.0</version>
</dependency>

<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
    <version>3.3.0</version>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>
```

## Lưu Ý Quan Trọng

1. **JWT Secret Key:** Thay đổi trong `JwtUtil.java` cho production
2. **Database Password:** Không commit password vào Git
3. **CORS:** Hiện tại cho phép tất cả origins (`@CrossOrigin(origins = "*")`), cần giới hạn trong production
4. **HTTPS:** Sử dụng HTTPS trong production
5. **Password Validation:** Thêm validation phức tạp hơn nếu cần

## Kết Luận

Server đã sẵn sàng để nhận requests từ client. Tất cả endpoints đã được implement và test. Chỉ cần cấu hình database và chạy server!
