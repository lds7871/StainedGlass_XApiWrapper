package LDS.Person.dto.response;

import lombok.Data;

/**
 * 创建推文响应
 */
@Data
public class CreateTweetResponse {
    /**
     * 推文 ID
     */
    private String tweetId;
    
    /**
     * 推文文本
     */
    private String text;
    
    /**
     * 创建时间
     */
    private String createdAt;
}
