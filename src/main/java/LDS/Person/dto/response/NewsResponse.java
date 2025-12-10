package LDS.Person.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 新闻搜索响应 DTO
 */
@Data
@NoArgsConstructor
@ApiModel(value = "NewsResponse", description = "新闻搜索响应")
public class NewsResponse {

    @ApiModelProperty(value = "业务状态码")
    private int code;

    @ApiModelProperty(value = "响应信息")
    private String message;

    @ApiModelProperty(value = "新闻列表数据")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<NewsData> data;

    @ApiModelProperty(value = "结果总数")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer resultCount;

    @ApiModelProperty(value = "时间戳（毫秒）")
    private Long timestamp;

    public NewsResponse(Integer code, String message, List<NewsData> data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static NewsResponse success(List<NewsData> data, Integer count) {
        NewsResponse response = new NewsResponse();
        response.setCode(200);
        response.setMessage("成功获取新闻");
        response.setData(data);
        response.setResultCount(count);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    public static NewsResponse error(String message) {
        NewsResponse response = new NewsResponse();
        response.setCode(500);
        response.setMessage(message);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    public static NewsResponse badRequest(String message) {
        NewsResponse response = new NewsResponse();
        response.setCode(400);
        response.setMessage(message);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    public static NewsResponse unauthorized(String message) {
        NewsResponse response = new NewsResponse();
        response.setCode(401);
        response.setMessage(message);
        response.setTimestamp(System.currentTimeMillis());
        return response;
    }

    /**
     * 新闻数据内部类
     */
    @Data
    @NoArgsConstructor
    @ApiModel(value = "NewsData", description = "单条新闻信息")
    public static class NewsData {

        @JsonProperty("tweet_id")
        @ApiModelProperty(value = "推文 ID")
        private String tweetId;

        @JsonProperty("text")
        @ApiModelProperty(value = "新闻文本")
        private String text;

        @JsonProperty("author_id")
        @ApiModelProperty(value = "作者 ID")
        private String authorId;

        @JsonProperty("author_name")
        @ApiModelProperty(value = "作者名称")
        private String authorName;

        @JsonProperty("created_at")
        @ApiModelProperty(value = "创建时间")
        private String createdAt;

        @JsonProperty("public_metrics")
        @ApiModelProperty(value = "公开指标")
        private PublicMetrics publicMetrics;

        @JsonProperty("lang")
        @ApiModelProperty(value = "语言")
        private String lang;

        @JsonProperty("source")
        @ApiModelProperty(value = "来源")
        private String source;

        /**
         * 公开指标
         */
        @Data
        @NoArgsConstructor
        @ApiModel(value = "PublicMetrics", description = "推文公开指标")
        public static class PublicMetrics {

            @ApiModelProperty(value = "点赞数量")
            @JsonProperty("like_count")
            private Integer likeCount;

            @ApiModelProperty(value = "转推数量")
            @JsonProperty("retweet_count")
            private Integer retweetCount;

            @ApiModelProperty(value = "引用数量")
            @JsonProperty("quote_count")
            private Integer quoteCount;

            @ApiModelProperty(value = "回复数量")
            @JsonProperty("reply_count")
            private Integer replyCount;

            @ApiModelProperty(value = "浏览数量")
            @JsonProperty("impression_count")
            private Integer impressionCount;
        }
    }
}
