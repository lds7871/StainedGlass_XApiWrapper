package LDS.Person.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 转发推文请求 DTO
 */
@Data
@NoArgsConstructor
@ApiModel(value = "RepostRequest", description = "转发推文请求")
public class RepostRequest {

    @ApiModelProperty(value = "要转发的推文 ID", required = true, example = "1234567890")
    private String tweetId;

    public RepostRequest(String tweetId) {
        this.tweetId = tweetId;
    }

}
