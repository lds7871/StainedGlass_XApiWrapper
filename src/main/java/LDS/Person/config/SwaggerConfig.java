package LDS.Person.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Properties;

/**
 * Swagger 配置类
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("LDS.Person.controller"))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * 定义 RestTemplate Bean 用于发送 HTTP 请求
     * 用于调用 Twitter API、内部 API 等（支持可配置的代理）
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // 从 config.properties 读取代理配置
        Properties props = new Properties();
        boolean proxyIsOpen = false;
        String proxyHost = "127.0.0.1";
        int proxyPort = 33210;
        
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                props.load(input);
                
                // 读取代理开关配置
                String proxyOpenStr = props.getProperty("proxy.is.open", "false");
                proxyIsOpen = Boolean.parseBoolean(proxyOpenStr.trim());
                
                // 读取代理主机和端口
                proxyHost = props.getProperty("proxy.host", "127.0.0.1").trim();
                String proxyPortStr = props.getProperty("proxy.port", "33210");
                try {
                    proxyPort = Integer.parseInt(proxyPortStr.trim());
                } catch (NumberFormatException e) {
                    proxyPort = 33210;
                }
            }
        } catch (Exception e) {
            // 配置文件读取失败，使用默认值
        }
        
        try {
            // 创建 RestTemplate
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            
            // 如果启用了代理，则配置代理
            if (proxyIsOpen) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
                factory.setProxy(proxy);
            }
            
            factory.setConnectTimeout(30000);
            factory.setReadTimeout(30000);
            
            // 使用 BufferingClientHttpRequestFactory 包装以支持重复读取
            ClientHttpRequestFactory bufferingFactory = new BufferingClientHttpRequestFactory(factory);
            
            return builder
                    .requestFactory(() -> bufferingFactory)
                    .setConnectTimeout(java.time.Duration.ofSeconds(30))
                    .setReadTimeout(java.time.Duration.ofSeconds(30))
                    .build();
        } catch (Exception e) {
            // 配置失败，降级到直连
            return builder
                    .setConnectTimeout(java.time.Duration.ofSeconds(30))
                    .setReadTimeout(java.time.Duration.ofSeconds(30))
                    .build();
        }
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("PersonLog 个人日志系统 API")
                .description("PersonLog 个人日志管理系统的 REST API 文档")
                .version("1.0.0")
                .contact(new springfox.documentation.service.Contact(
                        "PersonLog 开发团队",
                        "",
                        ""
                ))
                .build();
    }

}
