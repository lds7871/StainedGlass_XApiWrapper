package LDS.Person.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 转发推文请求 DTO
 */
@Data
@NoArgsConstructor
@Schema(name = "RepostRequest", description = "转发推文请求")
public class RepostRequest {

    @Schema(description = "要转发的推文 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1234567890")
    private String tweetId;

    public RepostRequest(String tweetId) {
        this.tweetId = tweetId;
    }

}
