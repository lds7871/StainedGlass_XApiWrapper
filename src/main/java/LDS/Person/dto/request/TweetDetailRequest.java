package LDS.Person.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取推文详情请求
 */
@Data
@NoArgsConstructor
@ApiModel(value = "TweetDetailRequest", description = "获取单条推文详情请求")
public class TweetDetailRequest {

    @JsonProperty("tweet_id")
    @ApiModelProperty(value = "推文唯一 ID", required = true, example = "1990302869522969080")
    private String tweetId;

    @JsonProperty("user_id")
    @ApiModelProperty(value = "可选的用户 ID，用于指定哪位用户的推文（默认读取 config）")
    private String userId;
}