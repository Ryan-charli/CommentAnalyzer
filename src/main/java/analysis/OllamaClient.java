package analysis;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

public class OllamaClient {
    private final HttpClient client;
    private final String baseUrl;

    public OllamaClient(String baseUrl) {
        this.client = HttpClient.newHttpClient();
        this.baseUrl = baseUrl;
    }

    public String generateAnalysis(String comment) {
        try {
            String prompt = String.format("""
                Analyze this code comment and provide insights:
                %s
                
                Consider:
                1. Comment clarity and completeness
                2. Technical accuracy
                3. Documentation standards
                4. Suggested improvements
                
                Provide a concise analysis.
                """, comment);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new JSONObject()
                    .put("model", "deepseek-r1:7b")
                    .put("prompt", prompt)
                    .put("stream", false)
                    .toString()))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject jsonResponse = new JSONObject(response.body());
            return jsonResponse.getString("response");
        } catch (Exception e) {
            return "Failed to analyze comment: " + e.getMessage();
        }
    }


    public String generateComment(String code) {
        try {
            String prompt = String.format("""
                Generate a clear and concise comment for this code:
                %s
                
                Focus on:
                1. Purpose
                2. Parameters
                3. Return values
                4. Important details
                
                Format as a JavaDoc comment.
                """, code);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(new JSONObject()
                    .put("model", "deepseek-r1:7b")
                    .put("prompt", prompt)
                    .put("stream", false)
                    .toString()))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONObject jsonResponse = new JSONObject(response.body());
            return jsonResponse.getString("response");
        } catch (Exception e) {
            return "Failed to generate comment: " + e.getMessage();
        }
    }
}