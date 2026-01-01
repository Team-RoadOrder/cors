package dev.gmpark.cors.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 규칙: 사용자가 웹 브라우저에서 /images/ 로 시작하는 주소로 접속하면
        // 실제로는 내 컴퓨터(Mac)의 /Users/parkgyumin/Desktop/upload/ 폴더를 뒤져서 보여준다.

        registry.addResourceHandler("/images/**") // 웹 접근 경로
                .addResourceLocations("file:///Users/parkgyumin/Desktop/upload/"); // 실제 파일 경로 (file:/// 3개 주의!)
    }
}