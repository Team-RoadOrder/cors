package dev.gmpark.cors.validators;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class BadWordValidator {

    private final Set<String> forbiddenWords = new HashSet<>();
    private final String placeHolder = "***";

    @PostConstruct
    public void init() {
        loadBadWords();
    }

    private void loadBadWords() {
        try {
            ClassPathResource resource = new ClassPathResource("filter/badwords.txt");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        forbiddenWords.add(line);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("금지어 로딩 실패: " + e.getMessage());
        }
    }

    public boolean isBad(String text) {
        if (text == null || text.isBlank()) return false;

        // 1단계: 원본 검사
        if (hasForbidden(text)) return true;

        // 2단계: 정규화 검사 (공백/특수문자/숫자 제거)
        String normalized = text.replaceAll("[^ㄱ-ㅎ가-힣a-zA-Z]", "");
        if (hasForbidden(normalized)) return true;

        // 3단계: 초성 검사
        String consonantOnly = text.replaceAll("[^ㄱ-ㅎ]", "");
        return checkConsonants(consonantOnly);
    }

    private boolean hasForbidden(String target) {
        return forbiddenWords.stream().anyMatch(target::contains);
    }

    private boolean checkConsonants(String text) {
        List<String> badConsonants = Arrays.asList("ㅅㅂ", "ㅄ", "ㄲㅈ", "ㅗ", "ㄷㅊ", "ㅈㄴ", "ㅈㄱ", "ㅁㅊ");
        return badConsonants.stream().anyMatch(text::contains);
    }

    public String clean(String text) {
        if (text == null || text.isBlank()) return text;
        String result = text;
        for (String word : forbiddenWords) {
            result = result.replaceAll(Pattern.quote(word), placeHolder);
        }
        return result;
    }

    public void addWords(String... newWords) {
        this.forbiddenWords.addAll(Arrays.asList(newWords));
    }

    public void removeWords(String... targetWords) {
        this.forbiddenWords.removeAll(Arrays.asList(targetWords));
    }
}