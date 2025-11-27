package LDS.Person.dto.request;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 引用推文请求 DTO
 * 参考: https://docs.x.com/x-api/posts/create-post
 */

@ApiModel(value = "QuoteTweetRequest", description = "引用推文请求")
@Getter
@Setter
public class QuoteTweetRequest {

    @ApiModelProperty(value = "推文文本内容", required = true, example = "这是一条引用推文")
    private String text;

    @ApiModelProperty(value = "要引用的推文 ID", required = true, example = "1234567890123456789")
    private String quote_tweet_id;

//    public QuoteTweetRequest(String text, String quote_tweet_id) {
//        this.text = text;
//        this.quote_tweet_id = quote_tweet_id;
//    }
}
