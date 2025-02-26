package guru.springframework.mercedesmanual.controller;

import com.theokanning.openai.service.OpenAiService; // Fixed import
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
public class ManualController {
    private final OpenAiService openAiService;
    private final OkHttpClient pineconeClient;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public ManualController(@Value("${OPENAI_API_KEY}") String openAiKey,
                            @Value("${PINECONE_API_KEY}") String pineconeKey) {
        this.openAiService = new OpenAiService(openAiKey);
        this.pineconeClient = new OkHttpClient();
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/")
    public String query(@RequestParam("query") String query, Model model) throws Exception {
        // Embed query
        EmbeddingRequest embeddingRequest = EmbeddingRequest.builder()
                .input(Collections.singletonList(query))
                .model("text-embedding-ada-002")
                .build();
        List<Double> embedding = openAiService.createEmbeddings(embeddingRequest)
                .getData().get(0).getEmbedding();

        // Query Pinecone via REST API
        String pineconeJson = String.format(
                "{\"vector\": %s, \"topK\": 1, \"includeMetadata\": true}",
                embedding.toString()
        );
        RequestBody body = RequestBody.create(pineconeJson, JSON);
        Request pineconeRequest = new Request.Builder()
                .url("https://novel-data-xyz.pinecone.io/query") // Replace with your Pinecone index URL
                .header("Api-Key", System.getenv("PINECONE_API_KEY"))
                .post(body)
                .build();
        String retrievedText = "";
        String imagePath = "";
        try (Response response = pineconeClient.newCall(pineconeRequest).execute()) {
            String jsonResponse = response.body().string();
            if (jsonResponse.contains("\"matches\":[")) {
                retrievedText = extractValue(jsonResponse, "\"text\":", ",");
                imagePath = extractValue(jsonResponse, "\"image\":", "\"");
            }
        }

        // OpenAI chat completion
        ChatCompletionRequest chatRequest = ChatCompletionRequest.builder()
                .model("gpt-3.5-turbo")
                .messages(List.of(
                        new ChatMessage("system", "You are a helpful assistant summarizing a Mercedes-Benz user manual."),
                        new ChatMessage("user", "Based on this: " + retrievedText + ", answer: " + query)
                ))
                .maxTokens(150)
                .build();
        String answer = openAiService.createChatCompletion(chatRequest)
                .getChoices().get(0).getMessage().getContent();

        model.addAttribute("query", query);
        model.addAttribute("text", retrievedText.isEmpty() ? "No match found" : retrievedText);
        model.addAttribute("answer", answer);
        model.addAttribute("image", imagePath.isEmpty() ? "" : imagePath);
        return "result";
    }

    private String extractValue(String json, String key, String end) {
        int start = json.indexOf(key) + key.length() + 1;
        int finish = json.indexOf(end, start);
        return start > key.length() && finish > start ? json.substring(start, finish) : "";
    }
}