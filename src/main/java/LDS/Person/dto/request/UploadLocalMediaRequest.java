package LDS.Person.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传本地媒体请求 DTO
 * 从 config.properties 的 saveimgdir 目录中随机选择一个 PNG 文件并上传
 */
@Data
@NoArgsConstructor
@Schema(name = "UploadLocalMediaRequest", description = "上传本地媒体请求")
public class UploadLocalMediaRequest {

    @Schema(description = "媒体类别", example = "tweet_image")
    private String mediaCategory = "tweet_image";

    @Schema(description = "媒体类型", example = "image/png")
    private String mediaType = "image/png";

}
