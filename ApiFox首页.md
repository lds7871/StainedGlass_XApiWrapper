# 00FF00HEAD_XAPI封装接口请求示例

首页仅提供请求和响应示例，详细解释还需查看接口请求参数的介绍
# [查看接口目录点这里](https://f8slqhf5qu.apifox.cn/376735311e0)

## Twitter OAuth 回调
### 生成 Twitter OAuth 授权 URL
GET /callback/twitter/authorize

请求
```json
无
```
响应
```json
{
    "code": 200,
    "authorizationUrl": "https://twitter.com/i/oauth2/authorize?client_id=UEhHYTA1V*******Th4MWpwbjM6MTpjaQ&redirect_uri=http%3A%2F%2F*****0.56%2Fcallback%2Ftwitter%2Foauth&response_type=code&state=d63fb8a0-1089-4588-936d-ad6704a02fec&scope=tweet.read+tweet.write+users.read+offline.access+media.write+like.write&code_challenge=eCDfbxpzmUZV*******OgKkcyUtTzNxVC_6Cl4jQ&code_challenge_method=S256",
    "codeChallenge": "eCDfbxpzmUZ**********cyUtTzNxVC_6Cl4jQ",
    "codeChallengeMethod": "S256",
    "state": "d63fb8a0-108********ad6704a02fec",
    "message": "授权 URL 生成成功"
}
```
### 处理 Twitter OAuth 回调 (GET)
GET /callback/twitter/oauth

请求
```json
上一个接口连接点击授权自动触发
```
响应
```json
{
  "code": 200,
  "message": "认证成功",
  "userId": "1640**********",
  "username": "********ad",
  "displayName": "******AD",
  "accessToken": "aXpZN**********************TQ1Njg6MToxOmF0OjE"
}
```
## 转发
### 转发一条推文
POST /api/twitter/tweet/repost/create

请求
```json
{
    "tweetId": "1990302869522969080"
}
```
响应
```json
{
    "code": 200,
    "message": "转发成功",
    "data": {
        "retweeted": true
    },
    "timestamp": 1766114770042
}
```
## 媒体上传（比如图片/视频/GIF需要先上传至X才能二次调用，过期时间24H）
### 上传本地媒体
POST /api/x/media/upload-local（从config内的配置路径读取.png）

请求
```json
可以为空，默认：
{
    "mediaCategory": "tweet_image",
    "mediaType": "image/png"
}
```
响应
```json
{
    "code": 200,
    "message": "✅ 媒体上传成功",
    "data": {
        "id": "2001858209141919746",
        "media_key": "3_2001858209141919746",
        "size": 110864,
        "expires_after_secs": 86400,
        "image": {
            "image_type": "image/png",
            "w": 380,
            "h": 384
        }
    },
    "timestamp": 1766115165017
}
```
### 上传指定路径的媒体
  POST /api/x/media/upload-file

请求
```json
{
    "filePath": "string",
    "mediaCategory": "tweet_image",
    "mediaType": "image/png"
}
```
响应
```json
{
    "code": 200,
    "message": "✅ 媒体上传成功",
    "data": {
        "id": "2001858209141919746",
        "media_key": "3_2001858209141919746",
        "size": 110864,
        "expires_after_secs": 86400,
        "image": {
            "image_type": "image/png",
            "w": 380,
            "h": 384
        }
    },
    "timestamp": 1766115165017
}
```
### 查询媒体记录列表
  GET /api/x/media/list

请求
```json
无
```
响应
```json
{
    "total": 19,
    "code": 200,
    "data": [
        {
            "id": 19,
            "mediaId": "2001858209141919746",
            "mediaKey": "3_2001858209141919746",
            "createTime": "2025-12-19T11:32:45",
            "endTime": "2025-12-20T11:32:45",
            "status": 0
        },
        ...略
        {
            "id": 1,
            "mediaId": "1983745756323516416",
            "mediaKey": "3_1983745756323516416",
            "createTime": "2025-10-30T13:50:45",
            "endTime": "2025-10-31T13:50:45",
            "status": 1
        }
    ],
    "limit": 20,
    "message": "✅ 查询成功",
    "returned": 19
}
```
## 推文获取
### 获取最近的推文列表（5是平台最低限制）
GET /api/twitter/tweet/get/latest

请求
```json
Query 参数：默认config内的数据，可以为空
userId 
```
响应
```json
{
    "code": 200,
    "data": {
        "tweet_count": 5,
        "tweets": [
            {
                "like_count": 8,
                "created_at": "2025-12-18T15:37:59.000Z",
                "id": "2001678337597673890",
                "text": "@hsn8086 公司依旧8，我自己还是21为主，Maven也相对兼容",
                "author_id": "16402******5440",
                "reply_count": 0,
                "retweet_count": 0
            },
            ...略
            {
                "like_count": 0,
                "created_at": "2025-11-28T15:49:49.000Z",
                "id": "1994433556375023797",
                "text": "Bot开发笑话三则 https://t.co/q2UmQb4olm",
                "author_id": "16402******5440",
                "reply_count": 0,
                "retweet_count": 0
            }
        ],
        "result_count": 5
    },
    "message": "成功获取推文"
}
```
### 获取最近推文保存数据库
  GET /api/twitter/tweet/get/latestsave

同上

### 获取推文详情
  POST /api/twitter/tweet/get/detail

请求
```json
{
    "tweet_id": "1990302869522969080",//搜索的推文id
    "user_id": "1640**********0"//你的uid
}
```
响应
```json
{
    "code": 200,
    "message": "成功获取推文详情",
    "data": {
        "id": "1985381463718834249",
        "text": "致敬传奇大不列颠留子千早爱音被当成雷火剑女主 https://t.co/jWTicxW5oJ",
        "authorId": "1640********5440",
        "createdAt": "2025-11-03T16:20:02.000Z",
        "publicMetrics": {
            "likeCount": 0,
            "retweetCount": 0,
            "quoteCount": 0,
            "replyCount": 0
        }
    }
}
```

## 创建推文
### 创建推文（格式请从XAPI文档参考）
  POST /api/twitter/tweet/create

请求
```json
格式规范从XAPI文档参考，建议格式：
{
     "text": "API测试发送"
} 
```
响应
```json
{
    "code": 200,
    "data": {
        "tweetId": "2001862717054066936",
        "text": "API测试发送",
        "createdAt": null
    },
    "message": "推文创建成功"
}
```

### 创建带有媒体的推文
  POST /api/twitter/tweet/createformedia

请求
```json
meidia内容从上传上去的且没过期的参数中获取并使用，使用后数据库自动修改状态字段：
{
     "text": "API测试发送"
    //  ,
    //  "media": {
    //              "key": {}
    //           }
} 
```
响应
```json
同上不做展示
```

## 趋势
### 获取个性化趋势（Free层级不可用）
  GET /api/twitter/trends/personalized

请求
```json
无
```
响应
```json
很遗憾我不知道
{
    "code": 0,
    "message": "string",
    "data": [
        {
            "trend_name": "string",
            "post_count": 0,
            "category": "string",
            "trending_since": "string"
        }
    ]
}
```

### 搜索新闻
  POST /api/twitter/trends/search-news

请求
```json
{
    "query": "Trump",
    "timeRange": "last_24h",
    "sortBy": "recency",
    "maxResults": 10,
    "lang": "en"
}
```
响应
```json
{
    "code": 200,
    "message": "成功获取新闻",
    "data": [
        {
            "tweet_id": "2001864620164960391",
            "text": "RT @CheriJacobus: Oh look -- 2015 license agreement for Trump Tower Moscow. https://t.co/4CrNc4hbEu",
            "author_id": "1457120589710233609",
            "author_name": "Chris",
            "created_at": "2025-12-19T03:58:12.000Z",
            "public_metrics": {
                "like_count": 0,
                "retweet_count": 1001,
                "quote_count": 0,
                "reply_count": 0,
                "impression_count": 0
            },
            "lang": "en",
            "source": "Twitter/X"
        },
        ...略
        {
            "tweet_id": "2001864619225477143",
            "text": "@RichardAngwin @RepMcGovern Groceries were already sky high from Biden. There is no denying that fact. It has level off since Trump and some items, such as eggs, have come back down to normal prices. Insurance, you can place that directly on letting 20 million illegals into the country. It's on Dems.",
            "author_id": "161474816",
            "author_name": "Tim",
            "created_at": "2025-12-19T03:58:12.000Z",
            "public_metrics": {
                "like_count": 0,
                "retweet_count": 0,
                "quote_count": 0,
                "reply_count": 0,
                "impression_count": 1
            },
            "lang": "en",
            "source": "Twitter/X"
        }
    ],
    "resultCount": 10,
    "timestamp": 1766116703363
}
```