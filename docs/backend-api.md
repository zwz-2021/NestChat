# NestChat 后端对接接口文档

## 1. 目标说明

本文档只覆盖当前 Android 项目里已经存在、并且真正需要后端支撑的功能。

当前目标不是重做业务，而是让现有页面从本地演示逻辑平滑切到真实后端。

已在客户端预留的接口契约代码位置：

- `app/src/main/java/com/example/nestchat/api/ApiCallback.java`
- `app/src/main/java/com/example/nestchat/api/ApiError.java`
- `app/src/main/java/com/example/nestchat/api/AuthApi.java`
- `app/src/main/java/com/example/nestchat/api/UserApi.java`
- `app/src/main/java/com/example/nestchat/api/RelationApi.java`
- `app/src/main/java/com/example/nestchat/api/FileApi.java`
- `app/src/main/java/com/example/nestchat/api/DiaryApi.java`
- `app/src/main/java/com/example/nestchat/api/ChatApi.java`

这些文件只是“接口预留”，没有接入真实网络库，也没有接入真实请求实现。

## 2. 当前前端已有功能与后端需求映射

### 2.1 登录 / 注册 / 找回密码

对应页面：

- `LoginActivity`
- `RegisterActivity`
- `ForgetPasswordActivity`

需要后端的能力：

- 图形验证码
- 登录
- 注册
- 发送找回密码验证码
- 重置密码
- 退出登录

### 2.2 首页 Mine

对应页面：

- `MineFragment`
- `EditProfileActivity`
- `AccountSecurityActivity`

需要后端的能力：

- 获取当前用户资料
- 修改昵称
- 修改头像
- 更新情绪
- 退出登录

### 2.3 关系管理

对应页面：

- `RelationManageActivity`
- `MineFragment` 中关系卡片

需要后端的能力：

- 获取当前关系状态
- 输入手机号发起绑定
- 查询申请状态
- 预留接受/拒绝绑定申请
- 修改备注
- 解除绑定

### 2.4 日记

对应页面：

- `DiaryFragment`
- `WriteDiaryActivity`
- `DiaryDetailActivity`

需要后端的能力：

- 拉取日记列表
- 拉取单篇日记详情
- 发布日记
- 上传日记图片
- 获取 TA 最近 7 天情绪趋势

### 2.5 聊天

对应页面：

- `ChatFragment`
- `ChatImagePreviewActivity`

需要后端的能力：

- 获取会话信息
- 拉取消息列表
- 发送文本消息
- 发送图片消息
- 发送语音消息
- 上传聊天图片
- 上传语音文件

备注：

- 当前项目没有接 WebSocket。
- 如果后端暂时没有长连接能力，第一阶段可以先用“轮询拉消息 + 发送消息接口”跑通。

## 3. 统一约定

## 3.1 Base URL

示例：

```text
https://api.nestchat.com/api/v1
```

## 3.2 统一请求头

未登录接口：

```http
Content-Type: application/json
```

已登录接口：

```http
Content-Type: application/json
Authorization: Bearer {accessToken}
```

文件上传接口可使用：

```http
Content-Type: multipart/form-data
Authorization: Bearer {accessToken}
```

## 3.3 统一返回结构

建议统一：

```json
{
  "code": 0,
  "message": "success",
  "traceId": "20260418-abc123",
  "data": {}
}
```

约定：

- `code = 0` 表示成功
- 非 `0` 表示业务失败
- `traceId` 便于排查线上问题

## 3.4 常见错误码建议

```text
0       成功
40001   参数错误
40002   验证码错误
40003   账号或密码错误
40004   验证码已过期
40005   两次密码不一致
40101   未登录或 token 失效
40301   无权限操作
40401   资源不存在
40901   重复注册
40902   已存在绑定关系
40903   绑定申请已存在
50000   服务端异常
```

## 4. 认证模块

## 4.1 获取登录验证码

`GET /auth/captcha/login`

用途：

- `LoginActivity` 图形验证码

响应 `data`：

```json
{
  "captchaId": "c_login_001",
  "imageBase64": "data:image/png;base64,...",
  "expireAt": 1776500000000
}
```

## 4.2 获取注册验证码

`GET /auth/captcha/register`

用途：

- `RegisterActivity` 图形验证码

响应结构同上。

## 4.3 登录

`POST /auth/login`

请求体：

```json
{
  "account": "13800000000",
  "password": "123456",
  "captchaId": "c_login_001",
  "captchaCode": "ABCD",
  "rememberMe": true
}
```

响应 `data`：

```json
{
  "accessToken": "access_xxx",
  "refreshToken": "refresh_xxx",
  "expireAt": 1776500000000,
  "user": {
    "userId": "u_1001",
    "account": "13800000000",
    "nickname": "小明",
    "avatarUrl": "https://cdn.xxx/avatar/u_1001.jpg"
  }
}
```

## 4.4 注册

`POST /auth/register`

请求体：

```json
{
  "account": "13800000000",
  "password": "123456",
  "confirmPassword": "123456",
  "captchaId": "c_register_001",
  "captchaCode": "PQRS"
}
```

成功后前端行为：

- Toast 成功
- 返回登录页

## 4.5 发送找回密码验证码

`POST /auth/password/code/send`

请求体：

```json
{
  "account": "13800000000"
}
```

用途：

- `ForgetPasswordActivity` 中“获取验证码”

## 4.6 重置密码

`POST /auth/password/reset`

请求体：

```json
{
  "account": "13800000000",
  "verifyCode": "123456",
  "newPassword": "654321",
  "confirmPassword": "654321"
}
```

成功后前端行为：

- Toast 成功
- 返回登录页

## 4.7 退出登录

`POST /auth/logout`

说明：

- 如果后端不做 token 黑名单，这个接口可先返回成功占位
- 前端仍然会回到登录页并清空任务栈

## 5. 用户资料模块

## 5.1 获取我的资料

`GET /users/me`

响应 `data`：

```json
{
  "userId": "u_1001",
  "account": "13800000000",
  "nickname": "小明",
  "avatarUrl": "https://cdn.xxx/avatar/u_1001.jpg",
  "moodCode": "happy",
  "moodText": "开心"
}
```

用途：

- `MineFragment` 顶部名片
- `EditProfileActivity` 初始化展示

## 5.2 修改个人资料

`PUT /users/me/profile`

请求体：

```json
{
  "nickname": "新的昵称",
  "avatarUrl": "https://cdn.xxx/avatar/u_1001_new.jpg"
}
```

说明：

- 当前前端编辑页只允许修改 `昵称` 和 `头像`

## 5.3 修改当前情绪

`PUT /users/me/mood`

请求体：

```json
{
  "moodCode": "happy"
}
```

建议情绪枚举：

```text
happy
sad
tired
```

## 6. 文件上传模块

## 6.1 上传图片

`POST /files/upload/image`

表单字段建议：

- `file`
- `bizType`

`bizType` 建议值：

```text
avatar
diary
chat
```

响应 `data`：

```json
{
  "fileId": "img_10001",
  "fileUrl": "https://cdn.xxx/image/10001.jpg",
  "thumbnailUrl": "https://cdn.xxx/image/10001_thumb.jpg",
  "mimeType": "image/jpeg",
  "fileSize": 245678
}
```

## 6.2 上传语音

`POST /files/upload/voice`

表单字段建议：

- `file`
- `bizType=chat`

响应 `data`：

```json
{
  "fileId": "voice_9001",
  "fileUrl": "https://cdn.xxx/voice/9001.m4a",
  "mimeType": "audio/mp4",
  "fileSize": 34567,
  "durationSeconds": 4
}
```

## 7. 关系管理模块

## 7.1 获取当前关系状态

`GET /relations/current`

响应 `data`：

```json
{
  "relationId": "r_1001",
  "status": "bound",
  "partnerUserId": "u_2001",
  "partnerPhone": "13900000000",
  "partnerNickname": "小龙",
  "partnerAvatarUrl": "https://cdn.xxx/avatar/u_2001.jpg",
  "partnerRemark": "小龙",
  "boundAt": "2024-03-27 10:20:00",
  "companionDays": 32
}
```

状态建议：

```text
none
pending
bound
```

## 7.2 发起绑定申请

`POST /relations/applications`

请求体：

```json
{
  "targetPhone": "13900000000"
}
```

说明：

- 对应 `RelationManageActivity` 输入手机号发起绑定

## 7.3 获取待处理申请

`GET /relations/applications`

用途：

- 预留给后续“对方确认绑定”功能

响应 `data`：

```json
{
  "items": [
    {
      "applicationId": "ra_1001",
      "status": "pending",
      "initiatorUserId": "u_1001",
      "initiatorPhone": "13800000000",
      "targetUserId": "u_2001",
      "targetPhone": "13900000000",
      "createdAt": "2026-04-18 14:00:00"
    }
  ]
}
```

## 7.4 接受绑定申请

`POST /relations/applications/{applicationId}/accept`

说明：

- 当前前端还没有完整确认页面，但你的产品结构已经明确需要，建议后端先出

## 7.5 拒绝绑定申请

`POST /relations/applications/{applicationId}/reject`

## 7.6 修改备注

`PUT /relations/current/remark`

请求体：

```json
{
  "relationId": "r_1001",
  "remark": "新的备注"
}
```

## 7.7 解除绑定

`DELETE /relations/current`

成功后前端状态恢复为无关系。

## 8. 日记模块

## 8.1 获取日记列表

`GET /diaries?pageNo=1&pageSize=20`

响应 `data`：

```json
{
  "items": [
    {
      "diaryId": "d_1001",
      "date": "2026.04.18",
      "authorType": "me",
      "moodText": "开心 🙂",
      "contentSummary": "今天我们聊了很久，感觉轻松了很多……",
      "imageCount": 2,
      "coverUrl": "https://cdn.xxx/diary/cover_1001.jpg"
    }
  ],
  "hasMore": true
}
```

`authorType` 建议：

```text
me
ta
```

## 8.2 获取日记详情

`GET /diaries/{diaryId}`

响应 `data`：

```json
{
  "diaryId": "d_1001",
  "date": "2026.04.18",
  "authorType": "me",
  "moodCode": "happy",
  "moodText": "开心 🙂",
  "content": "今天我们聊了很久，感觉轻松了很多……",
  "imageUrls": [
    "https://cdn.xxx/diary/1.jpg",
    "https://cdn.xxx/diary/2.jpg"
  ]
}
```

## 8.3 发布日记

`POST /diaries`

请求体：

```json
{
  "authorType": "me",
  "date": "2026.04.18",
  "moodCode": "happy",
  "moodText": "开心 🙂",
  "content": "今天我们聊了很久，感觉轻松了很多……",
  "imageFileIds": [
    "img_10001",
    "img_10002"
  ]
}
```

说明：

- 当前 `WriteDiaryActivity` 是先选本地图片，再保存日记
- 实际接入时建议先调用上传图片，再把 `fileId` 传给创建日记接口

## 8.4 获取 TA 最近 7 天情绪趋势

`GET /diaries/trend/partner?days=7`

响应 `data`：

```json
{
  "points": [
    { "date": "2026-04-12", "score": 3, "moodText": "开心" },
    { "date": "2026-04-13", "score": 2, "moodText": "难过" },
    { "date": "2026-04-14", "score": 5, "moodText": "开心" }
  ]
}
```

## 9. 聊天模块

## 9.1 获取聊天会话信息

`GET /chat/session/current`

响应 `data`：

```json
{
  "conversationId": "c_1001",
  "partnerUserId": "u_2001",
  "partnerNickname": "小龙",
  "partnerAvatarUrl": "https://cdn.xxx/avatar/u_2001.jpg",
  "subtitle": "已绑定 · 在线"
}
```

## 9.2 拉取消息列表

`GET /chat/messages?conversationId=c_1001&cursor=&pageSize=20`

响应 `data`：

```json
{
  "items": [
    {
      "messageId": "m_1001",
      "conversationId": "c_1001",
      "senderType": "ta",
      "messageType": "text",
      "content": "嗨，你在忙什么呀？",
      "imageUrl": "",
      "voiceUrl": "",
      "durationSeconds": 0,
      "createdAt": "2026-04-18 14:32:00",
      "clientMessageId": "",
      "sendStatus": "sent"
    }
  ],
  "nextCursor": "m_0980",
  "hasMore": true
}
```

枚举建议：

`senderType`

```text
me
ta
system
```

`messageType`

```text
text
image
voice
system
```

`sendStatus`

```text
sending
sent
failed
```

## 9.3 发送文本消息

`POST /chat/messages/text`

请求体：

```json
{
  "conversationId": "c_1001",
  "content": "在写代码",
  "clientMessageId": "local_1713439200"
}
```

## 9.4 发送图片消息

`POST /chat/messages/image`

请求体：

```json
{
  "conversationId": "c_1001",
  "imageFileId": "img_10001",
  "clientMessageId": "local_1713439201"
}
```

## 9.5 发送语音消息

`POST /chat/messages/voice`

请求体：

```json
{
  "conversationId": "c_1001",
  "voiceFileId": "voice_9001",
  "durationSeconds": 4,
  "clientMessageId": "local_1713439202"
}
```

## 10. 前端接入顺序建议

建议后端联调顺序：

1. 认证链路
   - 登录验证码
   - 登录
   - 注册
   - 找回密码验证码
   - 重置密码
2. 我的页
   - 获取我的资料
   - 修改昵称/头像
   - 修改情绪
3. 关系管理
   - 获取当前关系
   - 发起绑定
   - 解除绑定
4. 日记
   - 图片上传
   - 发布日记
   - 日记列表
   - 日记详情
   - TA 情绪趋势
5. 聊天
   - 获取会话信息
   - 拉消息
   - 图片上传
   - 语音上传
   - 发文本/图片/语音

## 11. 当前客户端仍是占位实现的部分

以下功能现在仍是本地演示，不是真实业务：

- 登录成功直接进首页
- 注册成功直接返回登录
- 找回密码验证码倒计时
- 关系状态本地切换
- 日记本地保存到当前运行期内
- 聊天消息本地追加
- AI 按钮只是占位弹窗

后端接入时，建议逐个把这些本地占位逻辑替换为真实接口调用，不要一次性全改。
