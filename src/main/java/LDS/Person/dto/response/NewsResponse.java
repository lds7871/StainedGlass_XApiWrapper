package LDS.Person.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 新闻搜索响应 DTO
 */
@Data
@NoArgsConstructor
@Schema(name = "NewsResponse", description = "新闻搜索响应")
public class NewsResponse {

    @Schema(description = "业务状态码")
    private int code;

    @Schema(description = "响应信息")
    private String message;

    @Schema(description = "新闻列表数据")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<NewsData> data;

    @Schema(description = "结果总数")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer resultCount;

    @Schema(description = "时间戳（毫秒）")
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
    @Schema(name = "NewsData", description = "单条新闻信息")
    public static class NewsData {

        @JsonProperty("tweet_id")
        @Schema(description = "推文 ID")
        private String tweetId;

        @JsonProperty("text")
        @Schema(description = "新闻文本")
        private String text;

        @JsonProperty("author_id")
        @Schema(description = "作者 ID")
        private String authorId;

        @JsonProperty("author_name")
        @Schema(description = "作者名称")
        private String authorName;

        @JsonProperty("created_at")
        @Schema(description = "创建时间")
        private String createdAt;

        @JsonProperty("public_metrics")
        @Schema(description = "公开指标")
        private PublicMetrics publicMetrics;

        @JsonProperty("lang")
        @Schema(description = "语言")
        private String lang;

        @JsonProperty("source")
        @Schema(description = "来源")
        private String source;

        /**
         * 公开指标
         */
        @Data
        @NoArgsConstructor
        @Schema(name = "PublicMetrics", description = "推文公开指标")
        public static class PublicMetrics {

            @Schema(description = "点赞数量")
            @JsonProperty("like_count")
            private Integer likeCount;

            @Schema(description = "转推数量")
            @JsonProperty("retweet_count")
            private Integer retweetCount;

            @Schema(description = "引用数量")
            @JsonProperty("quote_count")
            private Integer quoteCount;

            @Schema(description = "回复数量")
            @JsonProperty("reply_count")
            private Integer replyCount;

            @Schema(description = "浏览数量")
            @JsonProperty("impression_count")
            private Integer impressionCount;
        }
    }
}
