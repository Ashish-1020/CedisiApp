# Push Token Integration (FCM)

This backend now supports user-device push token lifecycle for frontend integration.

## Endpoints

### 1) Register token
- Method: `POST`
- Path: `/api/push-tokens/register`

Request:
```json
{
  "userId": "<user-id>",
  "fcmToken": "<firebase-token>",
  "deviceId": "<stable-device-id>",
  "platform": "android",
  "appVersion": "1.0.0"
}
```

Response:
```json
{
  "code": "PUSH_TOKEN_REGISTERED",
  "status": 200,
  "message": "Push token registered",
  "token": {
    "id": 12,
    "userId": "<user-id>",
    "fcmToken": "<firebase-token>",
    "deviceId": "<stable-device-id>",
    "platform": "ANDROID",
    "appVersion": "1.0.0",
    "active": true,
    "lastSeenAt": "2026-04-13T16:30:10",
    "updatedAt": "2026-04-13T16:30:10"
  }
}
```

### 2) Unregister token
- Method: `POST`
- Path: `/api/push-tokens/unregister`

Request by token:
```json
{
  "userId": "<user-id>",
  "fcmToken": "<firebase-token>"
}
```

Request by deviceId:
```json
{
  "userId": "<user-id>",
  "deviceId": "<stable-device-id>"
}
```

### 3) List user tokens (for debug)
- Method: `GET`
- Path: `/api/push-tokens/user/{userId}`

## Optional Login Shortcut
`POST /api/auth/login` now also accepts these optional fields:
- `fcmToken`
- `deviceId`
- `platform`
- `appVersion`

If provided, token registration is attempted during login itself.

## Frontend Trigger Points
1. After login success: call register (or send token in login payload).
2. On app start with stored session: call register again.
3. On `onTokenRefresh`: call register with new token.
4. Before logout: call unregister, then clear local session.

