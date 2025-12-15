package LDS.Person.config;

import java.lang.annotation.*;

/**
 * 绕过IP白名单检查的注解
 * 
 * 用法：在需要绕过IP白名单限制的Controller方法上标注此注解
 * 
 * 示例：
 * @GetMapping("/public-api")
 * @BypassIpWhitelist(reason = "公开API，允许任意IP访问")
 * public Map<String, Object> publicApi() {
 *     ...
 * }
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BypassIpWhitelist {
    
    /**
     * 说明为什么此接口需要绕过IP白名单限制
     * @return 原因说明
     */
    String reason() default "此接口允许任意IP访问";
}
