package ai;

import okhttp3.*;
import java.io.IOException;

// Link to api
public class ChatGPTClient {
    private static final String API_URL = "https://api.openai.com/v1/completions";
    private static final String API_KEY = System.getenv("OPENAI_API_KEY");

    // Generating Code Analysis with OpenAI
    public String generateSummary(String codeContent) {
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        // Define jsonPayload
        String jsonPayload = "{\"key\": \"value\"}";  // Replace with actual JSON payload
        String prompt = "Analyze the following code and describe its purpose:\n\n" + codeContent;
        String json = "{\"model\": \"text-davinci-003\", \"prompt\": \"" + prompt + "\", \"max_tokens\": 100}";

        RequestBody body = RequestBody.create(jsonPayload, mediaType);
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                System.err.println("OpenAI API Error: " + response.code());
                return "AI generation failed";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Network Error";
        }
    }
}
