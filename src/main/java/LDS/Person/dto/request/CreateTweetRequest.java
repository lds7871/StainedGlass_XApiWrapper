package LDS.Person.dto.request;

import lombok.Data;
import java.util.Map;

/**
 * 创建推文请求
 */
@Data
public class CreateTweetRequest {
    /**
     * 推文文本内容
     */
    private String text;
    
    /**
     * 媒体信息（可选）
     * 包含 media_ids 列表
     */
    private Map<String, Object> media;
}
