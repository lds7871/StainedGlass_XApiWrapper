package LDS.Person.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传指定文件媒体请求 DTO
 */
@Data
@NoArgsConstructor
@Schema(name = "UploadFileMediaRequest", description = "上传指定文件媒体请求")
public class UploadFileMediaRequest {

    @Schema(description = "文件路径", requiredMode = Schema.RequiredMode.REQUIRED, example = "C:\\path\\to\\image.png")
    private String filePath;

    @Schema(description = "媒体类别", example = "tweet_image")
    private String mediaCategory = "tweet_image";

    @Schema(description = "媒体类型", example = "image/png")
    private String mediaType = "image/png";

}
