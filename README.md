# Licence server

## Run


```
docker build -t security-app:0.1 .
docker run -p 8080:8080 --name security-app security-app:0.1
```
Địa chỉ: localhost:8080/info

```
curl localhost:8080/info
```

Kết quả trả về:
```
{"hardwareInfo":"bHPc8d1220r8m144f8a16666f","hash":"3cae6df0347105fc25f5ff951004e82c"}
```
Thông tin phần cứng:
```
* bHP - motherboard: hP
* c8 - số  thread cpu: 8
* d1220 - tổng dung lượng ổ đĩa: 1220 MB
* r8 - dung lượng ram: 8GB
* m144f8a16666f - địa chỉ mac: 14:4f:8a:16:66:6f
```

## Hoạt động
- Container chạy định kỳ mỗi ngày một lần kiểm tra thông tin phần cứng
- Nếu thông tin phần cứng chính xác trả về  "Status=200 và chuỗi md5 băm từ thông tin phần cứng", nếu thông tin sai thì exit.