package LDS.Person.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Properties;

/**
 * SpringDoc OpenAPI 配置类（原 Springfox Swagger 配置，升级为 OpenAPI 3）
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("StainedGlass XApiWrapper API")
                        .description("Twitter / X API 包装器的 REST API 文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("LDS 开发团队")
                                .url("")
                                .email("")));
    }

    /**
     * 定义 RestTemplate Bean 用于发送 HTTP 请求
     * 用于调用 Twitter API、内部 API 等（支持可配置的代理）
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        Properties props = new Properties();
        boolean proxyIsOpen = false;
        String proxyHost = "127.0.0.1";
        int proxyPort = 33210;

        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                props.load(input);
                proxyIsOpen = Boolean.parseBoolean(props.getProperty("proxy.is.open", "false").trim());
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
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            if (proxyIsOpen) {
                factory.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
            }
            factory.setConnectTimeout(30000);
            factory.setReadTimeout(30000);
            ClientHttpRequestFactory bufferingFactory = new BufferingClientHttpRequestFactory(factory);
            return builder
                    .requestFactory(() -> bufferingFactory)
                    .setConnectTimeout(java.time.Duration.ofSeconds(30))
                    .setReadTimeout(java.time.Duration.ofSeconds(30))
                    .build();
        } catch (Exception e) {
            return builder
                    .setConnectTimeout(java.time.Duration.ofSeconds(30))
                    .setReadTimeout(java.time.Duration.ofSeconds(30))
                    .build();
        }
    }

}
