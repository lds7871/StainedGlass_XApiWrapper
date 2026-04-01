package LDS.Person.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 趋势响应 DTO
 */
@Data
@NoArgsConstructor
@Schema(name = "TrendResponse", description = "趋势响应")
public class TrendResponse {

    @Schema(description = "业务状态码")
    private int code;

    @Schema(description = "响应信息")
    private String message;

    @Schema(description = "趋势列表数据")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<TrendData> data;

    public static TrendResponse success(List<TrendData> data) {
        TrendResponse response = new TrendResponse();
        response.setCode(200);
        response.setMessage("成功获取趋势");
        response.setData(data);
        return response;
    }

    public static TrendResponse error(String message) {
        TrendResponse response = new TrendResponse();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }

    public static TrendResponse badRequest(String message) {
        TrendResponse response = new TrendResponse();
        response.setCode(400);
        response.setMessage(message);
        return response;
    }

    @Data
    @NoArgsConstructor
    @Schema(name = "TrendData", description = "单条趋势信息")
    public static class TrendData {

        @JsonProperty("trend_name")
        @Schema(description = "趋势名称")
        private String trendName;

        @JsonProperty("post_count")
        @Schema(description = "推文数量")
        private Integer postCount;

        @JsonProperty("category")
        @Schema(description = "分类")
        private String category;

        @JsonProperty("trending_since")
        @Schema(description = "开始流行时间")
        private String trendingSince;
    }
}
