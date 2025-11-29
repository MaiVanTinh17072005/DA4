# Hướng Dẫn Sửa Lỗi và Chạy Server

## Các Lỗi Đã Sửa

### 1. ✅ Lỗi JWT `parserBuilder()` not found
**Nguyên nhân:** Phiên bản JWT 0.12.3 có API khác  
**Giải pháp:** Hạ xuống phiên bản 0.11.5 (ổn định hơn)

### 2. ✅ Lỗi SLF4J conflict
**Nguyên nhân:** Conflict giữa slf4j-simple và Logback của Spring Boot  
**Giải pháp:** Xóa slf4j-simple dependency

### 3. ✅ Lỗi Jackson version conflict
**Nguyên nhân:** JWT 0.11.5 kéo theo Jackson 2.12.x, Spring Boot 3.3.0 cần Jackson 2.17.x  
**Giải pháp:** Thêm exclusions để loại bỏ Jackson cũ từ JWT dependencies

## Cách Chạy Server

### Bước 1: Clean và Rebuild
```bash
cd BE_app
mvn clean install -DskipTests
```

### Bước 2: Chạy Server
```bash
mvn spring-boot:run
```

Hoặc trong IntelliJ IDEA:
- Click chuột phải vào `ServerMain.java`
- Chọn "Run 'ServerMain.main()'"

### Bước 3: Kiểm Tra Server Đã Chạy

Bạn sẽ thấy log như sau:
```
=================================
Discord Mini Server Starting...
=================================
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.3.0)

...
Tomcat started on port 8080 (http)
Started ServerMain in X.XXX seconds
=================================
Server Started Successfully!
=================================
```

## Lưu Ý Quan Trọng

### Database Connection
Server sẽ cần kết nối PostgreSQL. Nếu chưa tạo database:

```bash
# 1. Đăng nhập PostgreSQL
psql -U postgres

# 2. Tạo database
CREATE DATABASE discordminidb;

# 3. Thoát
\q
```

### Cập Nhật Password
Sửa file `application.properties`:
```properties
spring.datasource.password=YOUR_ACTUAL_PASSWORD
```

## Nếu Vẫn Gặp Lỗi

### Lỗi: "Cannot connect to database"
- Kiểm tra PostgreSQL đã chạy chưa
- Kiểm tra password trong `application.properties`
- Kiểm tra database `discordminidb` đã tạo chưa

### Lỗi: "Port 8080 already in use"
Thay đổi port trong `application.properties`:
```properties
server.port=8081
```

### Lỗi Maven dependency
```bash
# Xóa cache Maven và tải lại
mvn dependency:purge-local-repository
mvn clean install
```

## Test API

Sau khi server chạy thành công, test health endpoint:

```bash
curl http://localhost:8080/api/v1/auth/health
```

Kết quả mong đợi:
```
Auth service is running
```

## Dependencies Đã Cập Nhật

```xml
<!-- JWT với exclusions để tránh conflict -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
    <exclusions>
        <exclusion>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

## Tóm Tắt

✅ Đã sửa 3 lỗi chính  
✅ Dependencies đã tương thích  
✅ Server sẵn sàng chạy  
⚠️ Cần tạo database `discordminidb` trước khi chạy  
⚠️ Cần cập nhật password PostgreSQL trong `application.properties`
