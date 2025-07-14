package com.nextdoor.nextdoor.command;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vertexai.api.Content;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.Part;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.PartMaker;
import com.nextdoor.nextdoor.common.Adapter;
import com.nextdoor.nextdoor.domain.aianalysis.controller.dto.response.ProductConditionAnalysisResponseDto;
import com.nextdoor.nextdoor.domain.aianalysis.exception.ExternalApiException;
import com.nextdoor.nextdoor.domain.post.controller.dto.response.AnalyzeProductImageResponse;
import com.nextdoor.nextdoor.domain.post.domain.Category;
import com.nextdoor.nextdoor.domain.post.exception.HttpFileReadException;
import com.nextdoor.nextdoor.domain.post.port.ProductImageAnalysisPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.MimeType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Adapter
public class GeminiProductImageAnalysisAdapter implements ProductImageAnalysisPort {

    private final ChatClient chatClient;
    private final String productAnalyzerPrompt;
    private final ObjectMapper objectMapper;

    public GeminiProductImageAnalysisAdapter(
            @Qualifier("geminiFlashHighChatClient")
            ChatClient chatClient,
            @Qualifier("productAnalyzerPrompt")
            String  productAnalyzerPrompt,
            ObjectMapper objectMapper
    ) {
        this.chatClient = chatClient;
        this.productAnalyzerPrompt = productAnalyzerPrompt;
        this.objectMapper = objectMapper;
    }

    @Override
    public AnalyzeProductImageResponse analyzeProductImage(MultipartFile productImage) {
        MimeType mimeType = MimeType.valueOf(productImage.getContentType());
        byte[] bytes;
        try {
            bytes = productImage.getBytes();
        } catch (IOException e) {
            throw new HttpFileReadException();
        }
        Media media = Media.builder()
                .mimeType(mimeType)
                .data(bytes)
                .build();
        String response = chatClient.prompt(productAnalyzerPrompt)
                .user(u -> u.media(media))
                .call()
                .content();
        Map<String, String> resultMap;
        try {
            resultMap = objectMapper.readValue(cleanMarkdownCodeBlocks(response), new TypeReference<>() {});
        } catch (IOException e) {
            throw new ExternalApiException();
        }
        return AnalyzeProductImageResponse.builder()
                .title(resultMap.get("title"))
                .content(resultMap.get("content"))
                .category(Category.from(resultMap.get("category")))
                .condition(resultMap.get("condition"))
                .build();
    }

    private String cleanMarkdownCodeBlocks(String text) {
        Pattern pattern = Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return text.trim();
    }
}
