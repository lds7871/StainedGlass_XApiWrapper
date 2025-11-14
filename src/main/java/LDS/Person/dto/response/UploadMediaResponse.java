package LDS.Person.dto.response;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 媒体上传响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(value = "UploadMediaResponse", description = "媒体上传响应")
public class UploadMediaResponse {

    @ApiModelProperty(value = "HTTP 状态码", example = "200")
    private Integer code;

    @ApiModelProperty(value = "响应消息", example = "✅ 媒体上传成功")
    private String message;

    @ApiModelProperty(value = "响应数据（包含 media_id, media_key 等）")
    private Object data;

    @ApiModelProperty(value = "时间戳（毫秒）", example = "1704067200000")
    private Long timestamp;

    public UploadMediaResponse(Integer code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static UploadMediaResponse success(Object data) {
        return new UploadMediaResponse(200, "✅ 媒体上传成功", data);
    }

    public static UploadMediaResponse error(Integer code, String message) {
        return new UploadMediaResponse(code, message, null);
    }

    public static UploadMediaResponse badRequest(String message) {
        return new UploadMediaResponse(400, message, null);
    }

    public static UploadMediaResponse unauthorized(String message) {
        return new UploadMediaResponse(401, message, null);
    }

    public static UploadMediaResponse forbidden(String message) {
        return new UploadMediaResponse(403, message, null);
    }

    public static UploadMediaResponse serverError(String message) {
        return new UploadMediaResponse(500, message, null);
    }

}
