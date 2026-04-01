package LDS.Person.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新闻搜索请求 DTO
 */
@Data
@NoArgsConstructor
@Schema(name = "NewsSearchRequest", description = "新闻搜索请求")
public class NewsSearchRequest {

    @Schema(description = "搜索关键词", requiredMode = Schema.RequiredMode.REQUIRED, example = "cryptocurrency")
    private String query;

    @Schema(description = "搜索范围（last_hour, last_24h, last_7d, last_30d）", example = "last_24h")
    private String timeRange;

    @Schema(description = "排序方式（recency, relevancy）", example = "recency")
    private String sortBy;

    @Schema(description = "返回结果数量（10-100）", example = "10")
    private Integer maxResults;

    @Schema(description = "语言代码（en）", example = "en")
    private String lang;
}
