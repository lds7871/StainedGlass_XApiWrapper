package LDS.Person.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * get_tweets 表实体
 */
@Entity
@Table(name = "get_tweets")
@Data
@NoArgsConstructor
public class GetTweet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tweet_id", length = 50, nullable = false, unique = true)
    private String tweetId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "author_id", length = 50, nullable = false)
    private String authorId;

    @Column(name = "text", length = 2550)
    private String text;
}