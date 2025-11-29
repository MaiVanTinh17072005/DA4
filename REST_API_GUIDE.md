# REST API Client - Hướng Dẫn Sử Dụng

## Tổng Quan Cấu Trúc

Đây là cấu trúc REST API client cho ứng dụng Discord Mini. Tất cả các file đã được tạo để gửi request đăng ký và đăng nhập từ client lên server.

### Cấu Trúc Thư Mục

```
src/main/java/com/example/
├── api/
│   ├── dto/                    # Data Transfer Objects
│   │   ├── RegisterRequest.java    # DTO cho request đăng ký
│   │   ├── LoginRequest.java       # DTO cho request đăng nhập
│   │   ├── AuthResponse.java       # DTO cho response xác thực
│   │   └── UserDTO.java            # DTO cho thông tin user
│   └── ApiClient.java          # HTTP Client để gọi API
├── config/
│   └── ApiConfig.java          # Cấu hình API (URL, endpoints)
├── service/
│   └── AuthService.java        # Service xử lý authentication
└── util/
    └── SessionManager.java     # Quản lý session người dùng
```

## Chi Tiết Các Component

### 1. DTOs (Data Transfer Objects)

#### RegisterRequest.java
```java
// Dữ liệu gửi lên server khi đăng ký
{
    "email": "user@example.com",
    "username": "username123",
    "password": "hashed_password_sha256"
}
```

#### LoginRequest.java
```java
// Dữ liệu gửi lên server khi đăng nhập
{
    "email": "user@example.com",
    "password": "hashed_password_sha256"
}
```

#### AuthResponse.java
```java
// Dữ liệu nhận về từ server
{
    "success": true,
    "message": "Login successful",
    "token": "jwt_token_here",
    "user": {
        "id": 1,
        "email": "user@example.com",
        "username": "username123",
        "avatarUrl": "https://...",
        "status": "online",
        "createdAt": "2024-01-01T00:00:00Z"
    }
}
```

### 2. ApiClient.java

HTTP Client sử dụng `HttpURLConnection` để gửi request.

**Tính năng:**
- Gửi POST request với JSON body
- Gửi GET request
- Tự động serialize/deserialize JSON bằng Gson
- Hỗ trợ authentication token (Bearer token)
- Xử lý lỗi HTTP

**Ví dụ sử dụng:**
```java
ApiClient client = new ApiClient();

// POST request
RegisterRequest request = new RegisterRequest("email", "username", "password");
AuthResponse response = client.post("/api/v1/auth/register", request, AuthResponse.class);

// GET request với token
client.setAuthToken("your_jwt_token");
UserDTO user = client.get("/api/v1/users/profile", UserDTO.class);
```

### 3. ApiConfig.java

Chứa tất cả cấu hình API:

```java
// Base URL
BASE_URL = "http://localhost:8080"

// Endpoints
LOGIN_ENDPOINT = "/api/v1/auth/login"
REGISTER_ENDPOINT = "/api/v1/auth/register"
LOGOUT_ENDPOINT = "/api/v1/auth/logout"
USER_PROFILE_ENDPOINT = "/api/v1/users/profile"

// Timeout
CONNECTION_TIMEOUT = 10000 ms (10 giây)
READ_TIMEOUT = 10000 ms (10 giây)
```

**Thay đổi URL server:**
```java
// Trong ApiConfig.java, thay đổi BASE_URL
public static final String BASE_URL = "http://your-server-ip:port";
```

### 4. AuthService.java

Service chính để xử lý authentication. Sử dụng Singleton pattern.

**Tính năng:**
- Tự động hash password bằng SHA-256 trước khi gửi lên server
- Quản lý authentication token
- Gọi API login/register

**Ví dụ sử dụng:**
```java
// Đăng ký
try {
    AuthResponse response = AuthService.getInstance()
        .register("email@example.com", "username", "password123");
    
    if (response.isSuccess()) {
        System.out.println("Đăng ký thành công!");
        System.out.println("Token: " + response.getToken());
    }
} catch (Exception e) {
    System.err.println("Lỗi: " + e.getMessage());
}

// Đăng nhập
try {
    AuthResponse response = AuthService.getInstance()
        .login("email@example.com", "password123");
    
    if (response.isSuccess()) {
        System.out.println("Đăng nhập thành công!");
    }
} catch (Exception e) {
    System.err.println("Lỗi: " + e.getMessage());
}

// Đăng xuất
AuthService.getInstance().logout();
```

### 5. SessionManager.java

Quản lý session người dùng hiện tại.

**Tính năng:**
- Lưu thông tin user hiện tại
- Lưu authentication token
- Kiểm tra trạng thái đăng nhập

**Ví dụ sử dụng:**
```java
// Lưu session sau khi đăng nhập thành công
SessionManager.setCurrentUser(response.getUser());
SessionManager.setAuthToken(response.getToken());

// Kiểm tra đăng nhập
if (SessionManager.isLoggedIn()) {
    System.out.println("User đã đăng nhập: " + SessionManager.getCurrentUsername());
}

// Lấy thông tin user
Long userId = SessionManager.getCurrentUserId();
String username = SessionManager.getCurrentUsername();
String email = SessionManager.getCurrentUserEmail();

// Xóa session (logout)
SessionManager.clearSession();
```

## Cách Hoạt Động

### Flow Đăng Ký (Registration)

1. User nhập email, username, password vào UI (`LoginScene.java`)
2. `LoginScene` gọi `performRegister()` trong background thread
3. `performRegister()` gọi `AuthService.getInstance().register()`
4. `AuthService` hash password bằng SHA-256
5. `AuthService` tạo `RegisterRequest` với password đã hash
6. `AuthService` gọi `ApiClient.post()` để gửi request lên server
7. `ApiClient` serialize request thành JSON và gửi HTTP POST
8. Server xử lý và trả về `AuthResponse`
9. `ApiClient` deserialize JSON response thành object
10. `AuthService` lưu token nếu thành công
11. `LoginScene` hiển thị kết quả cho user

### Flow Đăng Nhập (Login)

1. User nhập email, password vào UI
2. `LoginScene` gọi `performLogin()` trong background thread
3. `performLogin()` gọi `AuthService.getInstance().login()`
4. `AuthService` hash password bằng SHA-256
5. `AuthService` tạo `LoginRequest` với password đã hash
6. `AuthService` gọi `ApiClient.post()` để gửi request lên server
7. Server xác thực và trả về `AuthResponse` với token và user info
8. `AuthService` lưu token
9. `LoginScene` lưu user vào `SessionManager`
10. `LoginScene` chuyển sang màn hình chính

## Bảo Mật

### Password Hashing

Password được hash bằng SHA-256 **TRƯỚC KHI** gửi lên server:

```java
// Trong AuthService.java
private String hashPassword(String password) {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
    
    // Convert to hex string
    StringBuilder hexString = new StringBuilder();
    for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
    }
    
    return hexString.toString();
}
```

**Lưu ý:** Password gốc KHÔNG BAO GIỜ được gửi lên server!

### Authentication Token

- Token được lưu trong `ApiClient` và `SessionManager`
- Mọi request sau khi login sẽ tự động thêm header:
  ```
  Authorization: Bearer <token>
  ```

## Cấu Hình Server

### Thay Đổi URL Server

Mở file `ApiConfig.java` và thay đổi:

```java
// Development (local)
public static final String BASE_URL = "http://localhost:8080";

// Production
public static final String BASE_URL = "https://your-domain.com";

// Local network
public static final String BASE_URL = "http://192.168.1.100:8080";
```

### Thay Đổi Endpoints

Nếu server của bạn sử dụng endpoints khác:

```java
// Trong ApiConfig.java
public static final String LOGIN_ENDPOINT = "/your/custom/login";
public static final String REGISTER_ENDPOINT = "/your/custom/register";
```

## Testing

### Test với Mock Server

Bạn có thể test với mock server hoặc Postman:

**Request đăng ký:**
```bash
POST http://localhost:8080/api/v1/auth/register
Content-Type: application/json

{
    "email": "test@example.com",
    "username": "testuser",
    "password": "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"
}
```

**Request đăng nhập:**
```bash
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
    "email": "test@example.com",
    "password": "5e884898da28047151d0e56f8dc6292773603d0d6aabbdd62a11ef721d1542d8"
}
```

### Debug Mode

Tất cả request/response được log ra console:

```
POST Request to: http://localhost:8080/api/v1/auth/register
Request Body: {"email":"test@example.com","username":"testuser","password":"..."}
Response Code: 200
Response Body: {"success":true,"message":"Registration successful",...}
```

## Dependencies

Đã thêm vào `pom.xml`:

```xml
<!-- Gson for JSON serialization -->
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

## Lưu Ý Quan Trọng

1. **Server chưa cần viết:** Code này chỉ là phần CLIENT. Server cần implement các endpoints tương ứng.

2. **Password Security:** Password được hash SHA-256 ở client, nhưng server NÊN hash lại một lần nữa (bcrypt, argon2) trước khi lưu vào database.

3. **HTTPS:** Trong production, BẮT BUỘC sử dụng HTTPS để bảo mật.

4. **Error Handling:** Tất cả exceptions đã được handle và hiển thị cho user.

5. **Thread Safety:** `AuthService` sử dụng singleton pattern, `SessionManager` sử dụng static fields.

## Troubleshooting

### Lỗi Connection Refused
```
Lỗi kết nối: Connection refused
```
**Giải pháp:** Kiểm tra server đã chạy chưa và URL trong `ApiConfig` đúng chưa.

### Lỗi Timeout
```
Lỗi kết nối: Read timed out
```
**Giải pháp:** Tăng timeout trong `ApiConfig`:
```java
public static final int CONNECTION_TIMEOUT = 30000; // 30 seconds
public static final int READ_TIMEOUT = 30000;
```

### Lỗi JSON Parse
```
Lỗi: com.google.gson.JsonSyntaxException
```
**Giải pháp:** Kiểm tra format JSON response từ server có đúng với DTO không.

## Tích Hợp Với LoginScene

File `LoginScene.java` đã được update để sử dụng API:

- `performLogin()`: Gọi `AuthService.login()`
- `performRegister()`: Gọi `AuthService.register()`
- Tự động lưu session khi thành công
- Hiển thị lỗi khi thất bại

## Kết Luận

Bạn đã có đầy đủ code để gửi request đăng ký/đăng nhập từ client lên server. 

**Các bước tiếp theo:**
1. Chạy Maven để download Gson dependency
2. Implement server endpoints tương ứng
3. Test kết nối giữa client và server
4. Deploy và sử dụng!
