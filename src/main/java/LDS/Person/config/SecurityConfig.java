package LDS.Person.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一安全配置类
 * 
 * 功能：
 * 1. 管理IP白名单配置
 * 2. 注册安全拦截器
 * 3. 配置静态资源排除规则
 * 
 * 整合了原有的 WebMvcConfig 功能，统一管理安全相关配置
 */
@Configuration
@ConfigurationProperties(prefix = "security")
public class SecurityConfig implements WebMvcConfigurer {

    private final ObjectProvider<IpWhitelistInterceptor> ipWhitelistInterceptorProvider;

    /**
     * IP白名单是否启用
     */
    private boolean ipWhitelistEnabled = true;

    /**
     * IP白名单列表
     */
    private List<String> ipWhitelist = new ArrayList<>();

    /**
     * Pass Token是否启用（允许非白名单IP通过令牌访问）
     */
    private boolean passTokenEnabled = true;

    /**
     * Pass Token列表（令牌，用于绕过IP白名单）
     */
    private List<String> passTokens = new ArrayList<>();

    /**
     * 构造函数注入，使用ObjectProvider破坏循环依赖
     */
    public SecurityConfig(ObjectProvider<IpWhitelistInterceptor> ipWhitelistInterceptorProvider) {
        this.ipWhitelistInterceptorProvider = ipWhitelistInterceptorProvider;
    }

    /**
     * 注册IP白名单拦截器和请求日志拦截器
     * 拦截器会检查所有controller请求，但可以通过@BypassIpWhitelist注解绕过
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册IP白名单拦截器
        IpWhitelistInterceptor interceptor = ipWhitelistInterceptorProvider.getIfAvailable();
        if (interceptor != null) {
            registry.addInterceptor(interceptor)
                    .addPathPatterns("/**")  // 拦截所有请求
                    .excludePathPatterns(    // 排除静态资源
                            "/static/**",
                            "/css/**",
                            "/js/**",
                            "/img/**",
                            "/images/**",
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/swagger-resources/**",
                            "/webjars/**"
                    );
        }
    }

    // Getters and Setters for ConfigurationProperties

    public boolean isIpWhitelistEnabled() {
        return ipWhitelistEnabled;
    }

    public void setIpWhitelistEnabled(boolean ipWhitelistEnabled) {
        this.ipWhitelistEnabled = ipWhitelistEnabled;
    }

    public List<String> getIpWhitelist() {
        return ipWhitelist;
    }

    public void setIpWhitelist(List<String> ipWhitelist) {
        this.ipWhitelist = ipWhitelist;
    }

    public boolean isPassTokenEnabled() {
        return passTokenEnabled;
    }

    public void setPassTokenEnabled(boolean passTokenEnabled) {
        this.passTokenEnabled = passTokenEnabled;
    }

    public List<String> getPassTokens() {
        return passTokens;
    }

    public void setPassTokens(List<String> passTokens) {
        this.passTokens = passTokens;
    }
}
