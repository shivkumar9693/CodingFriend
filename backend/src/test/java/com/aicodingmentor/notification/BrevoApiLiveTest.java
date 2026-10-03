package com.aicodingmentor.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

class BrevoApiLiveTest {

    @Test
    @org.junit.jupiter.api.Disabled("Verified live with Brevo HTTP API")
    @DisplayName("Verify Brevo HTTPS REST API with verified sender")
    void testBrevoApi() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        String payload = """
            {
              "sender": {
                "name": "CodingFriend",
                "email": "ironman9162682@gmail.com"
              },
              "to": [
                {
                  "email": "shivkumarthakur9693@gmail.com"
                }
              ],
              "subject": "🚀 CodingFriend Brevo HTTPS Live Verification",
              "htmlContent": "<h2>Brevo HTTPS API is working!</h2><p>This email was sent from verified sender ironman9162682@gmail.com to shivkumarthakur9693@gmail.com.</p>"
            }
            """;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("api-key", "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")
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Brevo Status Code: " + response.statusCode());
        System.out.println("Brevo Response Body: " + response.body());
        org.junit.jupiter.api.Assertions.assertTrue(response.statusCode() >= 200 && response.statusCode() < 300);
    }
}
