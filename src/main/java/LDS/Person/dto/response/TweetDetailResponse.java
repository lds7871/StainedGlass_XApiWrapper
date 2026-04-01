package LDS.Person.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条推文详情响应
 */
@Data
@NoArgsConstructor
@Schema(name = "TweetDetailResponse", description = "单条推文详情")
public class TweetDetailResponse {

    @Schema(description = "业务状态码")
    private int code;

    @Schema(description = "响应信息")
    private String message;

    @Schema(description = "推文数据")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private TweetDetailData data;

    public static TweetDetailResponse success(TweetDetailData data) {
        TweetDetailResponse response = new TweetDetailResponse();
        response.setCode(200);
        response.setMessage("成功获取推文详情");
        response.setData(data);
        return response;
    }

    public static TweetDetailResponse badRequest(String message) {
        TweetDetailResponse response = new TweetDetailResponse();
        response.setCode(400);
        response.setMessage(message);
        return response;
    }

    public static TweetDetailResponse error(String message) {
        TweetDetailResponse response = new TweetDetailResponse();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }

    @Data
    @NoArgsConstructor
    @Schema(name = "TweetDetailData", description = "推文详细信息")
    public static class TweetDetailData {

        @Schema(description = "推文 ID")
        private String id;

        @Schema(description = "推文文本")
        private String text;

        @Schema(description = "作者 ID")
        private String authorId;

        @Schema(description = "创建时间")
        private String createdAt;

        @Schema(description = "公开指标")
        private TweetPublicMetrics publicMetrics;
    }

    @Data
    @NoArgsConstructor
    @Schema(name = "TweetPublicMetrics", description = "推文公开指标")
    public static class TweetPublicMetrics {

        @Schema(description = "点赞数量")
        private Integer likeCount;

        @Schema(description = "转推数量")
        private Integer retweetCount;

        @Schema(description = "引用数量")
        private Integer quoteCount;

        @Schema(description = "回复数量")
        private Integer replyCount;
    }
}