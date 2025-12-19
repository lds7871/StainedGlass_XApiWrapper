package LDS.Person.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新闻搜索请求 DTO
 */
@Data
@NoArgsConstructor
@ApiModel(value = "NewsSearchRequest", description = "新闻搜索请求")
public class NewsSearchRequest {

    @ApiModelProperty(value = "搜索关键词", required = true, example = "cryptocurrency")
    private String query;

    @ApiModelProperty(value = "搜索范围（last_hour, last_24h, last_7d, last_30d）", example = "last_24h")
    private String timeRange;

    @ApiModelProperty(value = "排序方式（recency, relevancy）", example = "recency")
    private String sortBy;

    @ApiModelProperty(value = "返回结果数量（10-100）", example = "10")
    private Integer maxResults;

    @ApiModelProperty(value = "语言代码（en）", example = "en")
    private String lang;
}
