package LDS.Person.dto.request;

import lombok.Data;
import java.util.Map;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 创建推文请求
 */
@Data
@ApiModel(value = "CreateTweetRequest", description = "创建推文请求")
public class CreateTweetRequest {
    /**
     * 推文文本内容
     */
    @ApiModelProperty(value = "推文文本内容", example = "Hello world!")
    private String text;
    
    /**
     * 媒体信息（可选）
     * 包含 media_ids 列表
     */
    @ApiModelProperty(value = "媒体信息（可选）", example = "{\"media_ids\": [\"12345\", \"67890\"]}")
    private Map<String, Object> media;
}
