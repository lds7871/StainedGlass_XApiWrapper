package LDS.Person.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条推文详情响应
 */
@Data
@NoArgsConstructor
@ApiModel(value = "TweetDetailResponse", description = "单条推文详情")
public class TweetDetailResponse {

    @ApiModelProperty(value = "业务状态码")
    private int code;

    @ApiModelProperty(value = "响应信息")
    private String message;

    @ApiModelProperty(value = "推文数据")
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
    @ApiModel(value = "TweetDetailData", description = "推文详细信息")
    public static class TweetDetailData {

        @ApiModelProperty(value = "推文 ID")
        private String id;

        @ApiModelProperty(value = "推文文本")
        private String text;

        @ApiModelProperty(value = "作者 ID")
        private String authorId;

        @ApiModelProperty(value = "创建时间")
        private String createdAt;

        @ApiModelProperty(value = "公开指标")
        private TweetPublicMetrics publicMetrics;
    }

    @Data
    @NoArgsConstructor
    @ApiModel(value = "TweetPublicMetrics", description = "推文公开指标")
    public static class TweetPublicMetrics {

        @ApiModelProperty(value = "点赞数量")
        private Integer likeCount;

        @ApiModelProperty(value = "转推数量")
        private Integer retweetCount;

        @ApiModelProperty(value = "引用数量")
        private Integer quoteCount;

        @ApiModelProperty(value = "回复数量")
        private Integer replyCount;
    }
}