package dev.gmpark.cors.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}") // 설정 파일에서 경로 가져오기
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 경로 끝에 슬래시(/)가 없으면 붙여줌 (안전장치)
        String path = uploadDir.endsWith("/") ? uploadDir : uploadDir + "/";

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + path); // 이제 동적으로 경로를 찾습니다.

        //리뷰 이미지 전용
        registry.addResourceHandler("/review-images/**")
                // upload의하위폴더에 리뷰이미지저장목적
                .addResourceLocations("file:" + path + "review/");
    }
}