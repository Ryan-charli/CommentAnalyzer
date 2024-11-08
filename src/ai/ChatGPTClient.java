package ai;

import okhttp3.*;
import java.io.IOException;

// ChatGPTClient: 与ChatGPT API通信以生成文件注释和分析
public class ChatGPTClient {
    private static final String API_URL = "https://api.openai.com/v1/completions";
    private static final String API_KEY = System.getenv("OPENAI_API_KEY");

    // 使用OpenAI的API生成代码分析描述
    public String generateSummary(String codeContent) {
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        // Define jsonPayload with your JSON data
        String jsonPayload = "{\"key\": \"value\"}";  // Replace with your actual JSON payload
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
                System.err.println("OpenAI API错误: " + response.code());
                return "AI生成失败";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "网络错误";
        }
    }
}
