# Build
`gradle fatJar`
# Exec
`java -jar SSH2SOCKS5-1.0-SNAPSHOT.jar <PORT|2223> <THREAD_POOL_SIZE|100>`
# API
`POST /connect`
```json
{
    "host": "113.172.209.151",
    "username": "admin",
    "password": "admin"
}
```
```json
{
    "address": "socks5://127.0.0.1:2494",
    "port": 2494,
    "ip": "113.172.209.151",
    "host": "127.0.0.1",
    "id": "912a8f6f-db7b-4d37-a395-4d08f4808b32",
    "delta_time": 6004,
    "status": "OK"
}
```
```json
{
    "status": "ERROR"
}
```
`POST /disconnect`
```json
{
	"id": "912a8f6f-db7b-4d37-a395-4d08f4808b32"
}
```
```json
{
    "status": "OK"
}
```
```json
{
    "status": "ERROR"
}
```
`GET/POST /clear`
```json
{
    "status": "OK"
}
```