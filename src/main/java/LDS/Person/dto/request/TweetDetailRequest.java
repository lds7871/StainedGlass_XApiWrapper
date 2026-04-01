package LDS.Person.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取推文详情请求
 */
@Data
@NoArgsConstructor
@Schema(name = "TweetDetailRequest", description = "获取单条推文详情请求")
public class TweetDetailRequest {

    @JsonProperty("tweet_id")
    @Schema(description = "推文唯一 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1990302869522969080")
    private String tweetId;

    @JsonProperty("user_id")
    @Schema(description = "可选的用户 ID，用于指定哪位用户的推文（默认读取 config）")
    private String userId;
}