package LDS.Person.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 媒体库实体 - 存储上传到 Twitter 的媒体信息
 */
@Entity
@Table(name = "media_library")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaLibrary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 媒体ID（API返回的media_id）
     */
    @Column(name = "media_id", length = 64, nullable = false)
    private String mediaId;

    /**
     * 媒体Key（API返回的media_key）
     */
    @Column(name = "media_key", length = 128, nullable = false)
    private String mediaKey;

    /**
     * 创建时间
     */
    @Column(name = "createtime", nullable = false)
    private LocalDateTime createTime;

    /**
     * 过期时间（24小时后）
     */
    @Column(name = "endtime", nullable = false)
    private LocalDateTime endTime;

    /**
     * 状态（默认0：可用）
     */
    @Column(name = "status")
    private Integer status;

    @PrePersist
    public void prePersist() {
        if (this.createTime == null) {
            this.createTime = LocalDateTime.now();
        }
        if (this.endTime == null) {
            this.endTime = this.createTime.plusHours(24);
        }
        if (this.status == null) {
            this.status = 0;
        }
    }
}
