package com.example.ragApp.service;

import com.example.ragApp.dto.LlmRuntimeSelectionResponse;
import io.netty.channel.ChannelOption;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Locale;

@Service
public class LlmChatClientFactory {

    private final LlmModelConfigService llmModelConfigService;
    private final ProviderConfig openAiConfig;
    private final ProviderConfig geminiConfig;
    private final ProviderConfig grokConfig;
    private final ProviderConfig claudeConfig;
    private final boolean geminiEnabled;
    private final boolean grokEnabled;
    private final boolean claudeEnabled;
    private final String geminiRoutingMode;
    private final String grokRoutingMode;
    private final String claudeRoutingMode;
    private final int claudeMaxTokens;

    private volatile String cachedProvider;
    private volatile String cachedModel;
    private volatile ChatClient cachedClient;

    public LlmChatClientFactory(LlmModelConfigService llmModelConfigService,
                                @Value("${app.llm.provider.openai.enabled:true}") boolean openAiEnabled,
                                @Value("${app.llm.provider.openai.api-key:${spring.ai.openai.api-key:}}") String openAiApiKey,
                                @Value("${app.llm.provider.openai.base-url:${spring.ai.openai.base-url:https://api.openai.com}}") String openAiBaseUrl,
                                @Value("${app.llm.provider.openai.completions-path:/v1/chat/completions}") String openAiCompletionsPath,
                                @Value("${app.llm.provider.openai.connect-timeout-seconds:10}") long openAiConnectTimeoutSeconds,
                                @Value("${app.llm.provider.openai.read-timeout-seconds:60}") long openAiReadTimeoutSeconds,
                                @Value("${app.llm.provider.gemini.enabled:false}") boolean geminiEnabled,
                                @Value("${app.llm.provider.gemini.api-key:${GEMINI_API_KEY:}}") String geminiApiKey,
                                @Value("${app.llm.provider.gemini.base-url:https://generativelanguage.googleapis.com}") String geminiBaseUrl,
                                @Value("${app.llm.provider.gemini.completions-path:/v1beta/openai/chat/completions}") String geminiCompletionsPath,
                                @Value("${app.llm.provider.gemini.connect-timeout-seconds:15}") long geminiConnectTimeoutSeconds,
                                @Value("${app.llm.provider.gemini.read-timeout-seconds:120}") long geminiReadTimeoutSeconds,
                                @Value("${app.llm.provider.grok.enabled:false}") boolean grokEnabled,
                                @Value("${app.llm.provider.grok.api-key:${GROK_API_KEY:}}") String grokApiKey,
                                @Value("${app.llm.provider.grok.base-url:https://api.x.ai}") String grokBaseUrl,
                                @Value("${app.llm.provider.grok.completions-path:/v1/chat/completions}") String grokCompletionsPath,
                                @Value("${app.llm.provider.grok.connect-timeout-seconds:10}") long grokConnectTimeoutSeconds,
                                @Value("${app.llm.provider.grok.read-timeout-seconds:90}") long grokReadTimeoutSeconds,
                                @Value("${app.llm.provider.claude.enabled:false}") boolean claudeEnabled,
                                @Value("${app.llm.provider.claude.api-key:${CLAUDE_API_KEY:}}") String claudeApiKey,
                                @Value("${app.llm.provider.claude.base-url:https://api.anthropic.com}") String claudeBaseUrl,
                                @Value("${app.llm.provider.claude.completions-path:/v1/messages}") String claudeCompletionsPath,
                                @Value("${app.llm.provider.claude.connect-timeout-seconds:10}") long claudeConnectTimeoutSeconds,
                                @Value("${app.llm.provider.claude.read-timeout-seconds:90}") long claudeReadTimeoutSeconds,
                                 @Value("${app.llm.provider.claude.max-tokens:1024}") int claudeMaxTokens,
                                @Value("${app.llm.provider.gemini.routing-mode:DISABLED}") String geminiRoutingMode,
                                @Value("${app.llm.provider.grok.routing-mode:DISABLED}") String grokRoutingMode,
                                @Value("${app.llm.provider.claude.routing-mode:DISABLED}") String claudeRoutingMode) {
        this.llmModelConfigService = llmModelConfigService;
        this.openAiConfig = new ProviderConfig(
                openAiEnabled,
                normalizeTrim(openAiApiKey),
                normalizeTrim(openAiBaseUrl),
                normalizeTrim(openAiCompletionsPath),
                positiveTimeout(openAiConnectTimeoutSeconds, 10),
                positiveTimeout(openAiReadTimeoutSeconds, 60)
        );
        this.geminiConfig = new ProviderConfig(
                geminiEnabled,
                normalizeTrim(geminiApiKey),
                normalizeTrim(geminiBaseUrl),
                normalizeTrim(geminiCompletionsPath),
                positiveTimeout(geminiConnectTimeoutSeconds, 15),
                positiveTimeout(geminiReadTimeoutSeconds, 120)
        );
        this.grokConfig = new ProviderConfig(
                grokEnabled,
                normalizeTrim(grokApiKey),
                normalizeTrim(grokBaseUrl),
                normalizeTrim(grokCompletionsPath),
                positiveTimeout(grokConnectTimeoutSeconds, 10),
                positiveTimeout(grokReadTimeoutSeconds, 90)
        );
        this.claudeConfig = new ProviderConfig(
                claudeEnabled,
                normalizeTrim(claudeApiKey),
                normalizeTrim(claudeBaseUrl),
                normalizeTrim(claudeCompletionsPath),
                positiveTimeout(claudeConnectTimeoutSeconds, 10),
                positiveTimeout(claudeReadTimeoutSeconds, 90)
        );
        this.geminiEnabled = geminiEnabled;
        this.grokEnabled = grokEnabled;
        this.claudeEnabled = claudeEnabled;
        this.geminiRoutingMode = normalizeUpper(geminiRoutingMode);
        this.grokRoutingMode = normalizeUpper(grokRoutingMode);
        this.claudeRoutingMode = normalizeUpper(claudeRoutingMode);
        this.claudeMaxTokens = positiveInt(claudeMaxTokens, 1024);
    }

    public ChatClient getChatClient() {
        LlmRuntimeSelectionResponse runtime = currentRuntimeSelection();
        String provider = runtime.provider();
        String model = runtime.modelName();

        ChatClient localClient = cachedClient;
        if (localClient != null
                && provider.equalsIgnoreCase(cachedProvider)
                && model.equalsIgnoreCase(cachedModel)) {
            return localClient;
        }

        synchronized (this) {
            localClient = cachedClient;
            if (localClient != null
                    && provider.equalsIgnoreCase(cachedProvider)
                    && model.equalsIgnoreCase(cachedModel)) {
                return localClient;
            }

            ChatClient rebuilt = switch (normalizeUpper(provider)) {
                case "OPENAI" -> buildOpenAiClient(model);
                case "GEMINI" -> buildGeminiClient(model);
                case "GROK" -> buildGrokClient(model);
                case "CLAUDE" -> buildClaudeClient(model);
                default -> throw new RuntimeException("Unsupported provider: " + provider);
            };

            cachedProvider = provider;
            cachedModel = model;
            cachedClient = rebuilt;
            return rebuilt;
        }
    }

    public LlmRuntimeSelectionResponse currentRuntimeSelection() {
        return llmModelConfigService.resolveRuntimeSelection();
    }

    private ChatClient buildOpenAiClient(String model) {
        return buildOpenAiCompatibleClient("OPENAI", model, openAiConfig);
    }

    private ChatClient buildGeminiClient(String model) {
        validateProviderBranch("GEMINI", geminiEnabled, geminiRoutingMode);
        return buildOpenAiCompatibleClient("GEMINI", model, geminiConfig);
    }

    private ChatClient buildGrokClient(String model) {
        validateProviderBranch("GROK", grokEnabled, grokRoutingMode);
        return buildOpenAiCompatibleClient("GROK", model, grokConfig);
    }

    private ChatClient buildClaudeClient(String model) {
        validateClaudeBranch();
        return buildAnthropicClient(model, claudeConfig);
    }

    private ChatClient buildOpenAiCompatibleClient(String provider, String model, ProviderConfig config) {
        if (!config.enabled()) {
            throw new RuntimeException(provider + " provider is disabled");
        }
        if (config.apiKey().isBlank()) {
            throw new RuntimeException(provider + " api key is not configured");
        }
        if (config.baseUrl().isBlank()) {
            throw new RuntimeException(provider + " base URL is not configured");
        }

        OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
                .apiKey(config.apiKey())
                .baseUrl(config.baseUrl())
                .restClientBuilder(buildRestClientBuilder(config));

        if (!config.completionsPath().isBlank()) {
            apiBuilder.completionsPath(config.completionsPath());
        }

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(apiBuilder.build())
                .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                .build();

        return ChatClient.builder(chatModel).build();
    }

    private ChatClient buildAnthropicClient(String model, ProviderConfig config) {
        if (!config.enabled()) {
            throw new RuntimeException("CLAUDE provider is disabled");
        }
        if (config.apiKey().isBlank()) {
            throw new RuntimeException("CLAUDE api key is not configured");
        }
        if (config.baseUrl().isBlank()) {
            throw new RuntimeException("CLAUDE base URL is not configured");
        }

        AnthropicApi.Builder apiBuilder = AnthropicApi.builder()
                .apiKey(config.apiKey())
                .baseUrl(config.baseUrl())
                .restClientBuilder(buildRestClientBuilder(config))
                .webClientBuilder(buildWebClientBuilder(config));

        if (!config.completionsPath().isBlank()) {
            apiBuilder.completionsPath(config.completionsPath());
        }

        AnthropicApi anthropicApi = apiBuilder.build();

        AnthropicChatModel chatModel = AnthropicChatModel.builder()
                .anthropicApi(anthropicApi)
                .defaultOptions(AnthropicChatOptions.builder()
                        .model(model)
                        .maxTokens(claudeMaxTokens)
                        .build())
                .build();

        return ChatClient.builder(chatModel).build();
    }

    private RestClient.Builder buildRestClientBuilder(ProviderConfig config) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(config.connectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(config.readTimeoutSeconds()));
        return RestClient.builder().requestFactory(factory);
    }

    private WebClient.Builder buildWebClientBuilder(ProviderConfig config) {
        int connectTimeoutMs = safeConnectTimeoutMs(config.connectTimeoutSeconds());
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofSeconds(config.readTimeoutSeconds()));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    private void validateProviderBranch(String provider, boolean enabled, String routingMode) {
        if (!enabled) {
            throw new RuntimeException(provider + " provider is disabled");
        }
        if (!"OPENAI_COMPATIBLE".equalsIgnoreCase(routingMode)) {
            throw new RuntimeException(provider + " routing mode is unsupported. Use OPENAI_COMPATIBLE");
        }
    }

    private void validateClaudeBranch() {
        if (!claudeEnabled) {
            throw new RuntimeException("CLAUDE provider is disabled");
        }
        if (!"ANTHROPIC_NATIVE".equalsIgnoreCase(claudeRoutingMode)) {
            throw new RuntimeException("CLAUDE routing mode is unsupported. Use ANTHROPIC_NATIVE");
        }
    }

    private String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private long positiveTimeout(long value, long fallback) {
        return value > 0 ? value : fallback;
    }

    private int positiveInt(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    private int safeConnectTimeoutMs(long seconds) {
        long millis = Duration.ofSeconds(seconds).toMillis();
        if (millis <= 0) {
            return 10000;
        }
        return millis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) millis;
    }

    private record ProviderConfig(boolean enabled,
                                  String apiKey,
                                  String baseUrl,
                                  String completionsPath,
                                  long connectTimeoutSeconds,
                                  long readTimeoutSeconds) {
    }
}

