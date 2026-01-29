package dev.gmpark.cors.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class TossApiService {
    private final String SECRET_KEY = "test_sk_yZqmkKeP8gNGqeA05AvprbQRxB9l";
    private final ObjectMapper mapper = new ObjectMapper();

    public JsonNode confirmPayment(String paymentKey, String orderId, Long amount) throws Exception {
        URL url = new URL("https://api.tosspayments.com/v1/payments/confirm");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        String encodedKey = Base64.getEncoder().encodeToString((SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8));
        connection.setRequestProperty("Authorization", "Basic " + encodedKey);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        String jsonBody = mapper.createObjectNode()
                .put("paymentKey", paymentKey)
                .put("orderId", orderId)
                .put("amount", amount)
                .toString();

        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        return getResponse(connection);
    }

    public JsonNode cancelPayment(String paymentKey, String cancelReason, long cancelAmount) throws Exception {
        URL url = new URL("https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        String encodedKey = Base64.getEncoder().encodeToString((SECRET_KEY + ":").getBytes(StandardCharsets.UTF_8));
        connection.setRequestProperty("Authorization", "Basic " + encodedKey);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);

        String jsonBody = mapper.createObjectNode()
                .put("cancelReason", cancelReason)
                .put("cancelAmount", cancelAmount)
                .toString();

        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        return getResponse(connection);
    }

    private JsonNode getResponse(HttpURLConnection connection) throws Exception {
        int code = connection.getResponseCode();
        boolean isSuccess = code == 200;

        InputStream responseStream = isSuccess ? connection.getInputStream() : connection.getErrorStream();
        JsonNode responseNode = mapper.readTree(responseStream);

        if (!isSuccess) {
            throw new RuntimeException(responseNode.path("message").asText());
        }
        return responseNode;
    }
}
