package LDS.Person.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Twitter Token 持久化实体
 * 存储 access_token、refresh_token 和过期时间
 */
@Entity
@Table(name = "twitter_tokens")
@Data
@NoArgsConstructor
public class TwitterToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Twitter 用户 ID
     */
    @Column(name = "twitter_user_id", nullable = false, unique = true)
    private String twitterUserId;

    /**
     * Access Token（2小时有效期）
     */
    @Column(name = "access_token", length = 2048, nullable = false)
    private String accessToken;

    /**
     * Refresh Token（长期有效）
     */
    @Column(name = "refresh_token", length = 2048)
    private String refreshToken;

    /**
     * Token 过期时间（Unix 秒）
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Token 权限范围
     */
    @Column(name = "scope")
    private String scope;

    /**
     * Token 类型（通常为 bearer）
     */
    @Column(name = "token_type")
    private String tokenType;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
