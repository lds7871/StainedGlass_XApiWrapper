package LDS.Person.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传本地媒体请求 DTO
 * 从 config.properties 的 saveimgdir 目录中随机选择一个 PNG 文件并上传
 */
@Data
@NoArgsConstructor
@ApiModel(value = "UploadLocalMediaRequest", description = "上传本地媒体请求")
public class UploadLocalMediaRequest {

    @ApiModelProperty(
        value = "媒体类别",
        required = false,
        example = "tweet_image",
        notes = "可选，默认值为 tweet_image。用于告知 Twitter API 媒体的用途"
    )
    private String mediaCategory = "tweet_image";

    @ApiModelProperty(
        value = "媒体类型",
        required = false,
        example = "image/png",
        notes = "可选，默认值为 image/png。当前上传的媒体总是 PNG 格式"
    )
    private String mediaType = "image/png";

}
