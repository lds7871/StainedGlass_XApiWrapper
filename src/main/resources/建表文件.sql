#用于创建数据单表
###########################

CREATE TABLE media_library (
        id         INT AUTO_INCREMENT COMMENT '自增主键'
        PRIMARY KEY,
        media_id   VARCHAR(64)  NOT NULL COMMENT '媒体ID（API返回的media_id）',
        media_key  VARCHAR(128) NOT NULL COMMENT '媒体Key（API返回的media_key）',
        createtime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间，默认当前时间',
        endtime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '过期时间，需在插入时指定为一天后',
        status     TINYINT DEFAULT 0 COMMENT '状态，默认0'
) COMMENT='媒体上传记录表';

##########################

create table twitter_tokens
(
    id              bigint auto_increment
        primary key,
    twitter_user_id varchar(255) not null comment '用户ID',
    access_token    longtext     not null comment '通行token',
    refresh_token   longtext     null comment '刷新token',
    token_type      varchar(50)  null comment 'token类型',
    scope           varchar(255) null,
    expires_at      datetime(6)  null comment '过期于',
    created_at      datetime(6)  not null comment '创建于',
    updated_at      datetime(6)  not null comment '更新于',
    constraint twitter_user_id
        unique (twitter_user_id)
)
    comment 'Xtoken表';

create index idx_expires_at
    on twitter_tokens (expires_at);

create index idx_user_id
    on twitter_tokens (twitter_user_id);

##########################


CREATE TABLE get_tweets (
    -- 原生自增主键，唯一标识数据库中的每条记录
       id INT AUTO_INCREMENT PRIMARY KEY,
    -- 推文的唯一ID，使用字符串存储，避免大整数溢出
       tweet_id VARCHAR(50) NOT NULL comment '推文ID',
    -- 推文创建时间，使用时间戳类型
       created_at TIMESTAMP NOT NULL comment '创建时间',
    -- 作者ID，使用字符串存储，避免大整数溢出
       author_id VARCHAR(50) NOT NULL comment '作者ID',
    -- 推文文本内容，使用TEXT存储
       text VARCHAR(2550) comment '推文内容'
)comment '推文获取表';