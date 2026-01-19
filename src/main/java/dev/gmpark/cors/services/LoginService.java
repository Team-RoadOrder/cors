package dev.gmpark.cors.services;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.gmpark.cors.entities.RegisterEntity;
import dev.gmpark.cors.mappers.RegisterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final RegisterMapper registerMapper;
    private final ObjectMapper objectMapper;

    @Value("${custom.property.kakao-client-id}")
    private String kakaoClientId;

    @Value("${custom.property.kakao-redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${custom.property.naver-client-id}")
    private String naverClientId;

    @Value("${custom.property.naver-client-secret}")
    private String naverClientSecret;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    public RegisterEntity CheckLogin(String email, String password) {
        if(email.isEmpty() || password.isEmpty()) {
            return null;
        }
        RegisterEntity dbUser = registerMapper.selectByEmail(email);
        if(dbUser == null) {
            return null;
        }
        try {
            if( !BCrypt.checkpw(password, dbUser.getPassword())) {
                return null;
            }
        } catch (IllegalArgumentException e) {
            // 비밀번호 형식이 잘못된 경우 (예: 평문 저장됨) 로그인 실패 처리
            return null;
        }
        return dbUser;
    }
    public RegisterEntity getUserByEmail(String email) {
        // 비밀번호 확인 없이 이메일만으로 유저 정보를 가져옵니다.
        // (직원이 로그인했을 때, 사장님의 정보를 가져오기 위해 사용됨)
        return this.registerMapper.selectByEmail(email);
    }
    // LoginService 내부에 추가
    public RegisterEntity checkSocialUser(String socialId, String socialTypeCode) {
        // DB에서 socialId와 socialTypeCode가 일치하는 회원을 찾습니다.
        // 예: select * from users where social_id = ? and social_type_code = ?
        // Mapper나 Repository를 호출하여 구현
        return this.registerMapper.selectUserBySocial(socialId, socialTypeCode);
    }
    public String getKakaoSocialId(String code) {
        if (code == null) return null;

        try {
            HttpClient client = HttpClient.newHttpClient();

            // 1. 토큰 발급 요청
            String params = "grant_type=authorization_code" +
                    "&client_id=" + kakaoClientId +
                    "&redirect_uri=" + URLEncoder.encode(kakaoRedirectUri, StandardCharsets.UTF_8) +
                    "&code=" + code;

            HttpRequest tokenRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://kauth.kakao.com/oauth/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(params))
                    .build();

            HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

            if (tokenResponse.statusCode() != 200) {
                System.out.println("Kakao Token Error: " + tokenResponse.body());
                return null;
            }

            JsonNode tokenRoot = objectMapper.readTree(tokenResponse.body());
            String accessToken = tokenRoot.get("access_token").asText();

            // 2. 사용자 정보 요청
            HttpRequest userRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://kapi.kakao.com/v2/user/me"))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.noBody()) // 카카오는 POST 권장
                    .build();

            HttpResponse<String> userResponse = client.send(userRequest, HttpResponse.BodyHandlers.ofString());

            if (userResponse.statusCode() != 200) {
                return null;
            }

            JsonNode userRoot = objectMapper.readTree(userResponse.body());
            return String.valueOf(userRoot.get("id").asLong()); // 소셜 ID 반환

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getNaverSocialId(String code) {
        if (code == null) return null;

        try {
            HttpClient client = HttpClient.newHttpClient();

            // 1. 토큰 발급 요청
            String uriStr = String.format("https://nid.naver.com/oauth2.0/token?grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&state=study",
                    naverClientId, naverClientSecret, code);

            HttpRequest tokenRequest = HttpRequest.newBuilder()
                    .uri(URI.create(uriStr))
                    .GET()
                    .build();

            HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());

            if (tokenResponse.statusCode() != 200) {
                System.out.println("Naver Token Error: " + tokenResponse.body());
                return null;
            }

            JsonNode tokenRoot = objectMapper.readTree(tokenResponse.body());
            if (!tokenRoot.has("access_token")) {
                return null;
            }
            String accessToken = tokenRoot.get("access_token").asText();

            // 2. 사용자 정보 요청
            HttpRequest userRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://openapi.naver.com/v1/nid/me"))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> userResponse = client.send(userRequest, HttpResponse.BodyHandlers.ofString());

            if (userResponse.statusCode() != 200) {
                return null;
            }

            JsonNode userRoot = objectMapper.readTree(userResponse.body());
            // 네이버는 response 객체 안에 id가 있음
            return userRoot.get("response").get("id").asText();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
