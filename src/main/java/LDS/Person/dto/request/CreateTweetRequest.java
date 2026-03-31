package LDS.Person.dto.request;

import lombok.Data;
import java.util.Map;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 创建推文请求
 */
@Data
@Schema(name = "CreateTweetRequest", description = "创建推文请求")
public class CreateTweetRequest {
    /**
     * 推文文本内容
     */
    @Schema(description = "推文文本内容", example = "Hello world!")
    private String text;
    
    /**
     * 媒体信息（可选）
     * 包含 media_ids 列表
     */
    @Schema(description = "媒体信息（可选）", example = "{\"media_ids\": [\"12345\", \"67890\"]}")
    private Map<String, Object> media;
}
