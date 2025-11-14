package LDS.Person.dto.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传指定文件媒体请求 DTO
 */
@Data
@NoArgsConstructor
@ApiModel(value = "UploadFileMediaRequest", description = "上传指定文件媒体请求")
public class UploadFileMediaRequest {

    @ApiModelProperty(
        value = "文件路径",
        required = true,
        example = "C:\\path\\to\\image.png",
        notes = "支持本地文件绝对路径（如 C:\\Users\\Administrator\\Desktop\\image.png）或网络 URL（如 https://example.com/image.png）"
    )
    private String filePath;

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
        notes = "可选，默认值为 image/png。支持 image/png, image/jpeg, image/gif, image/webp, video/mp4"
    )
    private String mediaType = "image/png";

}
