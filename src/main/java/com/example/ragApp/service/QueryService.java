package com.example.ragApp.service;

import com.example.ragApp.data.Conversation;
import com.example.ragApp.data.Message;
import com.example.ragApp.data.PromptTemplate;
import com.example.ragApp.data.SubscriptionPlan;
import com.example.ragApp.data.User;
import com.example.ragApp.data.UserProfile;
import com.example.ragApp.dto.ChatRequest;
import com.example.ragApp.dto.ChatResponse;
import com.example.ragApp.dto.LlmRuntimeSelectionResponse;
import com.example.ragApp.helper.questionMapper.QuestionTextMapper;
import com.example.ragApp.repository.ConversationRepository;
import com.example.ragApp.repository.MessageRepository;
import com.example.ragApp.repository.PromptTemplateRepository;
import com.example.ragApp.repository.SubscriptionPlanRepository;
import com.example.ragApp.repository.UserRepository;
import com.example.ragApp.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.util.Pair;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class QueryService {

    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);

    /**
     * Appended to every user-facing prompt so the LLM always returns structured JSON.
     * Stripping markdown fences before parsing handles models that wrap JSON in ```json blocks.
     */
    private static final String JSON_INSTRUCTION = """

            IMPORTANT:
            Return output strictly in JSON format:
            {
              "response": "complete answer for the user",
              "followUp": "one short helpful follow-up question"
            }
            Do not output anything outside JSON.
            """;

    private static final String ASSISTANT_BEHAVIOR_INSTRUCTION = """
            Additional behavior rules:
            1) If user asks who made/built/created you, answer that you were made by CeDISI.
            2) Whenever user ask to respond/reply/explain in any different language reply in the requested language in its font.
            3) If the user ask any related to thread questions ask the user to use the general chat option in the app""";

    private final LlmChatClientFactory llmChatClientFactory;
    private final UserProfileRepository userRepo;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepo;
    private final MessageRepository messageRepo;
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final PromptTemplateRepository promptTemp;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public QueryService(LlmChatClientFactory llmChatClientFactory,
                       UserProfileRepository userRepo,
                       UserRepository userRepository,
                       ConversationRepository conversationRepo,
                       MessageRepository messageRepo,
                       VectorStore vectorStore,
                       EmbeddingModel embeddingModel,
                       PromptTemplateRepository promptTemp,
                       SubscriptionPlanRepository subscriptionPlanRepository) {
        this.llmChatClientFactory = llmChatClientFactory;
        this.userRepo         = userRepo;
        this.userRepository   = userRepository;
        this.conversationRepo = conversationRepo;
        this.messageRepo      = messageRepo;
        this.vectorStore      = vectorStore;
        this.embeddingModel   = embeddingModel;
        this.promptTemp       = promptTemp;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    // =========================================================================
    // PUBLIC: Plain-text chat
    // =========================================================================

    /**
     * Main text-based chat entry point.
     *
     * Flow:
     *  1. Load and validate user profile
     *  2. Load use-case prompt template from DB
     *  3. Resolve or create the conversation
     *  4. RAG retrieval
     *  5. Scope gate — block only brand-new conversations with zero RAG hits
     *  6. Save user message to DB and vector store
     *  7. Build prompt and call LLM
     *  8. Save assistant message, refresh index, update summary
     */
    @Transactional
    public ChatResponse streamChat(ChatRequest request) {

        log("=================================================");
        log("🚀 streamChat | user={} | conv={} | msg={}",
                request.userId(), request.conversationId(), request.message());
        log("=================================================");

        ensureUserHasTokenQuota(request.userId(), "streamChat");

        // ── 1. Load user profile ─────────────────────────────────────────────
        UserProfile profile = userRepo.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.userId()));
        log("📄 UserProfile loaded: {}", profile);



        // ── 3. Load prompt template ──────────────────────────────────────────
        PromptTemplate template = promptTemp.findByUseCase(request.usecase())
                .orElseThrow(() -> new RuntimeException(
                        "No prompt template configured for usecase: " + request.usecase()));
        log("📜 Prompt template loaded for usecase={}", request.usecase());

        // ── 4. Resolve / create conversation ─────────────────────────────────
        boolean isNew = isNewConversation(request.conversationId());
        Conversation conversation = resolveConversation(request);
        log("🆔 ConversationId={} | isNew={}", conversation.getConversationId(), isNew);

        // ── 5. RAG retrieval ─────────────────────────────────────────────────
        List<Document> ragDocs = retrieveDocuments(request.message(), conversation.getUserId());
        log("📥 RAG docs retrieved: {}", ragDocs.size());

        // ── 6. Scope behavior ───────────────────────────────────────────────
        // Do not block by domain. If RAG has no hits, continue with conversation
        // context and base model reasoning.
        if (ragDocs.isEmpty()) {
            log("⚠ No RAG docs matched. Continuing without retrieval context.");
        }

        // ── 7. Persist user message and index it ─────────────────────────────
        saveUserMessage(conversation, request);
        uploadLatestMessageToVectorStore(conversation);
        log("💾 User message saved and indexed");

        // ── 8. Build prompt and call LLM ─────────────────────────────────────
        LlmCallResult llmResult = buildAndCallLLM(
                isNew, template, profile, conversation,
                ragDocs, request.message(), request.language());
        incrementUserTokenUsage(request.userId(), llmResult.totalTokens(), "streamChat");
        String raw = llmResult.content();
        log("📦 RAW LLM response:\n{}", raw);

        // ── 9. Parse structured response ─────────────────────────────────────
        ParsedResponse parsed = parseJsonResponse(raw);

        // ── 10. Persist assistant message, refresh index, update summary ─────
        saveAssistantMessage(conversation, parsed.response(),request);
        uploadLatestMessageToVectorStore(conversation);
        updateConversationSummary(conversation, request.language());
        uploadSummaryToVectorStore(conversation);

        log("✅ streamChat completed | conv={}", conversation.getConversationId());
        log("=================================================");

        return new ChatResponse(
                conversation.getConversationId().toString(),
                parsed.response(),
                parsed.followUp());
    }

    // =========================================================================
    // PUBLIC: File (image / PDF / Excel) chat
    // =========================================================================

    /**
     * Entry point when the user uploads a file alongside a text message.
     *
     * Key difference from streamChat:
     *   Extracted PDF/Excel text is BOTH sent to the LLM AND chunked into the
     *   vector store. This ensures that subsequent plain-text follow-up messages
     *   (routed through streamChat) can find the file content via RAG and will
     *   not be blocked as "out of scope".
     */

    // ...existing code...
    @Transactional
    public ChatResponse chatWithMedia(MultipartFile[] files, ChatRequest request) {

        try {
            log("=================================================");
            log("📂 chatWithMedia | user={} | files={} | msg={}",
                    request.userId(), files != null ? files.length : 0, request.message());
            log("=================================================");

            ensureUserHasTokenQuota(request.userId(), "chatWithMedia");

            if (files == null || files.length == 0) {
                throw new RuntimeException("No files uploaded");
            }

            // ── Load profile and template ────────────────────────────────────
            UserProfile profile = userRepo.findById(request.userId())
                    .orElseThrow(() -> new RuntimeException("User not found: " + request.userId()));

            PromptTemplate template = promptTemp.findByUseCase(request.usecase())
                    .orElseThrow(() -> new RuntimeException(
                            "No prompt template configured for usecase: " + request.usecase()));

            boolean isNew = isNewConversation(request.conversationId());
            Conversation conversation = resolveConversation(request);

            log("🆔 ConversationId={} | isNew={}", conversation.getConversationId(), isNew);

            // Persist user message first so it appears correctly in history
            saveUserMessage(conversation, request);

            // Reload history after the user message is saved
            List<Message> recentMessages = getRecentMessages(conversation);

            // ── Build prompt ─────────────────────────────────────────────────
            ChatClient.ChatClientRequestSpec prompt = currentChatClient().prompt();

            prompt.system(template.getSystemPrompt() + "\n\n" + ASSISTANT_BEHAVIOR_INSTRUCTION);
            log("🧠 System prompt injected");

            // Inject conversation history
            if (!isNew) {
                List<org.springframework.ai.chat.messages.Message> history =
                        toSpringMessages(recentMessages);

                prompt.messages(history);

                log("🧠 Injected {} memory messages", history.size());
            }

            // ── Detect file types ────────────────────────────────────────────
            StringBuilder combinedText = new StringBuilder();
            List<Pair<MimeType, ByteArrayResource>> images = new ArrayList<>();

            for (MultipartFile file : files) {

                log("📄 Processing file: {}", file.getOriginalFilename());

                String contentType = file.getContentType();

                boolean isImage = contentType != null && contentType.startsWith("image/");
                boolean isPdf   = "application/pdf".equals(contentType);
                boolean isExcel = contentType != null &&
                        (contentType.contains("spreadsheet")
                                || (file.getOriginalFilename() != null &&
                                (file.getOriginalFilename().endsWith(".xlsx")
                                        || file.getOriginalFilename().endsWith(".xls"))));

                if (isImage) {

                    log("🖼 Image detected: {}", file.getOriginalFilename());

                    ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                        @Override
                        public String getFilename() {
                            return file.getOriginalFilename();
                        }
                    };

                    MimeType mimeType = MimeTypeUtils.parseMimeType(contentType);

                    images.add(Pair.of(mimeType, resource));

                } else if (isPdf || isExcel) {

                    log("📄 Extracting text from {}", file.getOriginalFilename());

                    String extractedText = isPdf ? extractPdf(file) : extractExcel(file);

                    log("📄 Extracted {} characters", extractedText.length());

                    combinedText.append("\n\nFILE: ")
                            .append(file.getOriginalFilename())
                            .append("\n")
                            .append(extractedText);

                    uploadFileContentToVectorStore(
                            conversation,
                            file.getOriginalFilename(),
                            extractedText
                    );

                } else {
                    log("⚠ Unsupported file type: {}", contentType);
                    throw new RuntimeException("Unsupported file type: " + contentType);
                }
            }

            // ── Build final user prompt ──────────────────────────────────────
            String userText = buildMediaUserPrompt(
                    isNew,
                    template,
                    profile,
                    conversation.getSummary(),
                    request.message(),
                    combinedText.toString(),
                    request.language()
            );

            prompt.user(u -> {

                u.text(userText);

                for (Pair<MimeType, ByteArrayResource> img : images) {
                    u.media(img.getFirst(), img.getSecond());
                }
            });

            log("📨 User prompt injected | textLength={} | images={}",
                    combinedText.length(),
                    images.size());

            // ── Call LLM ─────────────────────────────────────────────────────
            logLlmSelection("chatWithMedia");
            log("🔵 Calling LLM");

            org.springframework.ai.chat.model.ChatResponse chatResponse = prompt.call().chatResponse();
            LlmCallResult llmResult = toLlmCallResult(chatResponse);
            incrementUserTokenUsage(request.userId(), llmResult.totalTokens(), "chatWithMedia");
            String raw = llmResult.content();

            log("📦 RAW media response:\n{}", raw);

            ParsedResponse parsed = parseJsonResponse(raw);

            // ── Persist, index, summarise ────────────────────────────────────
            saveAssistantMessage(conversation, parsed.response(),request);

            uploadLatestMessageToVectorStore(conversation);

            updateConversationSummary(conversation, request.language());

            uploadSummaryToVectorStore(conversation);

            log("✅ chatWithMedia completed | conv={}", conversation.getConversationId());
            log("=================================================");

            return new ChatResponse(
                    conversation.getConversationId().toString(),
                    parsed.response(),
                    parsed.followUp());

        } catch (Exception e) {

            logger.error("❌ chatWithMedia failed", e);

            return new ChatResponse(
                    request.conversationId(),
                    "Error processing file.",
                    null
            );
        }
    }

    // =========================================================================
    // PRIVATE: Core LLM prompt builder and caller
    // =========================================================================

    /**
     * Assembles the full prompt and invokes the LLM.
     *
     * ┌──────────────────────┬────────────────────────────────────────────────┐
     * │ FIRST message        │ FOLLOW-UP message                              │
     * ├──────────────────────┼────────────────────────────────────────────────┤
     * │ System prompt from DB│ System prompt from DB  (always — see note)     │
     * │ No chat history      │ Last N messages as memory                      │
     * │ RAG context (if any) │ RAG context (if any)                           │
     * │ Template + profile   │ Raw user query only (profile already in history│
     * └──────────────────────┴────────────────────────────────────────────────┘
     *
     * NOTE — why system prompt is always re-sent:
     *   Spring AI ChatClient is stateless. Each .call() is an independent HTTP
     *   request. If system prompt is skipped on follow-ups, the LLM has no role
     *   or instructions and will ignore the injected conversation history.
     *   This was the primary reason "memory didn't work on follow-ups".
     *
     * NOTE — why profile is NOT re-sent on follow-ups:
     *   The profile was part of the first USER message which now lives in
     *   recentMessages (chat history). Re-sending it wastes tokens and can
     *   cause the model to over-weight it versus the current question.
     */
    private LlmCallResult buildAndCallLLM(boolean isNew,
                                   PromptTemplate template,
                                   UserProfile profile,
                                   Conversation conversation,
                                   List<Document> ragDocs,
                                   String userMessage,
                                   String language) {

        List<Message> recentMessages = getRecentMessages(conversation);
        ChatClient.ChatClientRequestSpec prompt = currentChatClient().prompt();

        // ── System prompt — always, on every call ────────────────────────────
        String systemPrompt = template.getSystemPrompt() + "\n\n" + ASSISTANT_BEHAVIOR_INSTRUCTION;
        prompt.system(systemPrompt);
        log("🧠 System prompt set ({} chars)", systemPrompt.length());

        String languageDirective = languageDirective(language);

        if (isNew) {
            // ── FIRST MESSAGE ────────────────────────────────────────────────

            // RAG context injected before the user prompt
            if (!ragDocs.isEmpty()) {
                String ragContext = buildRagContext(ragDocs);
                prompt.user(ragContext);
                log("📚 RAG context injected ({} chars)", ragContext.length());
            }

            // User prompt = DB template + full profile block (only time profile is sent)
            String userPrompt = template.getUserPromptTemplate()
                    + "\n\nUser Business Profile:\n"
                    + buildProfileBlock(profile, conversation.getSummary())
                    + "\n\nUse this info: " + userMessage
                    + languageDirective
                    + JSON_INSTRUCTION;

            log("👤 FIRST user pro" +
                    "mpt ({} chars):\n{}", userPrompt.length(), userPrompt);
            prompt.user(userPrompt);

        } else {
            // ── FOLLOW-UP MESSAGE ────────────────────────────────────────────

            // Inject recent messages as memory (profile embedded in first turn)
            if (!recentMessages.isEmpty()) {
                List<org.springframework.ai.chat.messages.Message> history =
                        toSpringMessages(recentMessages);
                prompt.messages(history);
                log("🧠 Injected {} memory messages", history.size());
            }

            // RAG context injected after history so it's the freshest input
            if (!ragDocs.isEmpty()) {
                String ragContext = buildRagContext(ragDocs);
                prompt.user(ragContext);
                log("📚 RAG context injected ({} chars)", ragContext.length());
            }

            // User prompt = raw query only
            String userPrompt = userMessage + languageDirective + JSON_INSTRUCTION;
            log("👤 FOLLOW-UP user prompt ({} chars):\n{}", userPrompt.length(), userPrompt);
            prompt.user(userPrompt);
        }

        logLlmSelection("streamChat");
        log("🔵 Calling LLM...");
        return toLlmCallResult(prompt.call().chatResponse());
    }

    private LlmCallResult toLlmCallResult(org.springframework.ai.chat.model.ChatResponse chatResponse) {
        String content = chatResponse == null || chatResponse.getResult() == null
                || chatResponse.getResult().getOutput() == null
                ? ""
                : chatResponse.getResult().getOutput().getText();

        int totalTokens = 0;
        if (chatResponse != null && chatResponse.getMetadata() != null
                && chatResponse.getMetadata().getUsage() != null
                && chatResponse.getMetadata().getUsage().getTotalTokens() != null) {
            totalTokens = Math.max(0, chatResponse.getMetadata().getUsage().getTotalTokens());
        }
        return new LlmCallResult(content, totalTokens);
    }

    private void incrementUserTokenUsage(String userId, int tokens, String operation) {
        if (userId == null || userId.isBlank() || tokens <= 0) {
            return;
        }
        try {
            int rows = userRepository.incrementTokensConsumed(userId, tokens);
            if (rows > 0) {
                log("🧮 Token usage updated | op={} | userId={} | tokensAdded={}", operation, userId, tokens);
            } else {
                logger.warn("⚠ Token usage not updated, user not found | op={} | userId={} | tokensAdded={}",
                        operation, userId, tokens);
            }
        } catch (Exception e) {
            logger.warn("⚠ Failed to update token usage | op={} | userId={} | tokensAdded={} | reason={}",
                    operation, userId, tokens, e.getMessage());
        }
    }

    private void ensureUserHasTokenQuota(String userId, String operation) {
        if (userId == null || userId.isBlank()) {
            throw new RuntimeException("userId is required");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        String planCode = user.getSubscriptionPlanId();
        if (planCode == null || planCode.isBlank()) {
            return;
        }

        SubscriptionPlan plan = subscriptionPlanRepository.findByPlanCodeIgnoreCase(planCode.trim())
                .orElse(null);
        if (plan == null || plan.getTokenLimit() == null) {
            return;
        }

        int consumed = user.getNoOfTokensConsumed() == null ? 0 : user.getNoOfTokensConsumed();
        int tokenLimit = Math.max(0, plan.getTokenLimit());
        log("🧾 Token quota check | op={} | userId={} | planCode={} | consumed={} | limit={}",
                operation, userId, plan.getPlanCode(), consumed, tokenLimit);

        if (consumed >= tokenLimit) {
            throw new RuntimeException("Token limit reached for current subscription plan");
        }
    }

    // =========================================================================
    // PRIVATE: Conversation helpers
    // =========================================================================

    private boolean isNewConversation(String conversationId) {
        return conversationId == null || conversationId.isBlank();
    }

    private Conversation resolveConversation(ChatRequest request) {
        if (isNewConversation(request.conversationId())) {
            Conversation conv = new Conversation();
            conv.setUserId(request.userId());
            conv.setSummary(null);
            conv.setCategory(request.category());
            conv.setUsecase(request.usecase());
            Conversation saved = conversationRepo.save(conv);
            log("🆕 New conversation created: {}", saved.getConversationId());
            return saved;
        }
        try {
            UUID uuid = UUID.fromString(request.conversationId());
            Conversation conversation = conversationRepo.findById(uuid)
                    .orElseThrow(() -> new RuntimeException(
                            "Conversation not found: " + uuid));
            boolean changed = false;
            if (hasText(request.category()) && !Objects.equals(conversation.getCategory(), request.category())) {
                conversation.setCategory(request.category());
                changed = true;
            }
            if (hasText(request.usecase()) && !Objects.equals(conversation.getUsecase(), request.usecase())) {
                conversation.setUsecase(request.usecase());
                changed = true;
            }
            if (changed) {
                conversation = conversationRepo.save(conversation);
            }
            return conversation;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Invalid conversationId — must be a valid UUID: "
                            + request.conversationId());
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /** Returns the last 6 messages in chronological order (oldest → newest). */
    private List<Message> getRecentMessages(Conversation conversation) {
        List<Message> msgs = messageRepo
                .findTop6ByConversationIdOrderByTimestampDesc(
                        conversation.getConversationId());
        Collections.reverse(msgs);
        return msgs;
    }

    private List<org.springframework.ai.chat.messages.Message> toSpringMessages(
            List<Message> messages) {
        List<org.springframework.ai.chat.messages.Message> result = new ArrayList<>();
        for (Message msg : messages) {
            if ("USER".equals(msg.getRole())) {
                result.add(new UserMessage(msg.getContent()));
            } else {
                result.add(new AssistantMessage(msg.getContent()));
            }
        }
        return result;
    }

    // =========================================================================
    // PRIVATE: Message persistence
    // =========================================================================

    private void saveUserMessage(Conversation conversation, ChatRequest request) {
        Message msg = new Message();
        msg.setConversationId(conversation.getConversationId());
        msg.setRole("USER");
        msg.setContent(request.message());
        msg.setCategory(request.category());
        msg.setUsecase(request.usecase());
        msg.setTimestamp(LocalDateTime.now());
        messageRepo.save(msg);
        log("💾 USER message saved");
    }

    private void saveAssistantMessage(Conversation conversation, String content,ChatRequest request) {
        Message msg = new Message();
        msg.setConversationId(conversation.getConversationId());
        msg.setRole("ASSISTANT");
        msg.setContent(content);
        msg.setCategory(request.category());
        msg.setUsecase(request.usecase());
        msg.setTimestamp(LocalDateTime.now());
        messageRepo.save(msg);
        log("💾 ASSISTANT message saved");
    }

    // =========================================================================
    // PRIVATE: Vector store uploads
    // =========================================================================

    /**
     * Fetches the most recently saved message and uploads it to the vector store.
     * Eliminates the repeated findTop6...get(0) pattern from the old code.
     */
    private void uploadLatestMessageToVectorStore(Conversation conversation) {
        List<Message> recent = messageRepo
                .findTop6ByConversationIdOrderByTimestampDesc(
                        conversation.getConversationId());
        if (!recent.isEmpty()) {
            uploadMessageToVectorStore(conversation, recent.get(0));
        }
    }

    private void uploadMessageToVectorStore(Conversation conversation, Message message) {
        try {
            String content = message.getContent();
            if (content == null || content.isBlank()) {
                logger.warn("⚠ Skipping vector upload — message content is empty");
                return;
            }
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userId",         String.valueOf(conversation.getUserId()));
            metadata.put("conversationId", String.valueOf(conversation.getConversationId()));
            metadata.put("role",           message.getRole());
            metadata.put("timestamp",      message.getTimestamp().toString());
            metadata.put("type",           "chat");

            vectorStore.add(List.of(new Document(content, metadata)));
            log("✅ Uploaded {} message to vector store ({} chars)",
                    message.getRole(), content.length());
        } catch (Exception e) {
            logger.error("❌ Failed to upload message to vector store", e);
        }
    }

    /**
     * Chunks and stores extracted file text (PDF/Excel) in the vector store.
     *
     * WHY THIS EXISTS:
     *   chatWithMedia sends file text to the LLM but previously discarded it
     *   afterwards. When the user then sent a plain-text follow-up via streamChat,
     *   retrieveDocuments() returned 0 hits and blocked the message as "out of
     *   scope" — the exact bug visible in the production logs at 11:37.
     *
     *   Indexing the file here means all future follow-up queries in this
     *   conversation can match the file content via RAG similarity search.
     *
     * Chunking: 800-char chunks, 100-char overlap.
     *   - Keeps each chunk within embedding-model token limits.
     *   - Overlap prevents losing context at chunk boundaries.
     */
    private void uploadFileContentToVectorStore(Conversation conversation,
                                                String filename,
                                                String content) {
        try {
            if (content == null || content.isBlank()) {
                logger.warn("⚠ Skipping file vector upload — content is empty");
                return;
            }
            int chunkSize = 800;
            int overlap   = 100;
            List<Document> chunks = new ArrayList<>();
            int start = 0;

            while (start < content.length()) {
                int end   = Math.min(start + chunkSize, content.length());
                String chunk = content.substring(start, end);

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("userId",         String.valueOf(conversation.getUserId()));
                metadata.put("conversationId", String.valueOf(conversation.getConversationId()));
                metadata.put("type",           "file");
                metadata.put("filename",       filename != null ? filename : "unknown");
                metadata.put("chunkStart",     String.valueOf(start));

                chunks.add(new Document(chunk, metadata));
                start += (chunkSize - overlap);
            }

            vectorStore.add(chunks);
            log("✅ Uploaded {} chunks from '{}' to vector store", chunks.size(), filename);

        } catch (Exception e) {
            logger.error("❌ Failed to upload file content to vector store: {}", filename, e);
        }
    }

    private void uploadSummaryToVectorStore(Conversation conversation) {
        try {
            if (conversation.getSummary() == null) return;

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("userId",         String.valueOf(conversation.getUserId()));
            metadata.put("conversationId", conversation.getConversationId().toString());
            metadata.put("type",           "summary");

            vectorStore.add(List.of(new Document(conversation.getSummary(), metadata)));
            log("✅ Summary uploaded to vector store");
        } catch (Exception e) {
            logger.error("❌ Failed to upload summary to vector store", e);
        }
    }

    // =========================================================================
    // PRIVATE: RAG retrieval
    // =========================================================================

    private List<Document> retrieveDocuments(String query, String userId) {
        log("=================================================");
        log("🔎 RAG search | query='{}' | userId={}", query, userId);

        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(5)
                    .similarityThreshold(0.6)
                    .filterExpression("userId == \"" + userId + "\"")
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log("📥 {} docs matched (threshold=0.6, topK=5)", results.size());

            int index = 1;
            for (Document doc : results) {
                String preview = doc.getText() != null && doc.getText().length() > 200
                        ? doc.getText().substring(0, 200) + "..."
                        : doc.getText();
                log("  📄 Doc #{} | score={} | type={} | preview={}",
                        index++,
                        doc.getScore(),
                        doc.getMetadata().get("type"),
                        preview);
            }

            log("=================================================");
            return results;

        } catch (Exception e) {
            logger.error("❌ RAG retrieval failed", e);
            return List.of();
        }
    }

    // =========================================================================
    // PRIVATE: Conversation summary
    // =========================================================================

    private void updateConversationSummary(Conversation conversation, String language) {
        try {
            List<Message> allMessages = messageRepo
                    .findByConversationIdOrderByTimestampAsc(conversation.getConversationId());

            StringBuilder transcript = new StringBuilder();
            for (Message msg : allMessages) {
                transcript.append(msg.getRole())
                        .append(": ")
                        .append(msg.getContent())
                        .append("\n");
            }

            String summaryPrompt = """
                    Summarize this conversation crisply in under 8 lines.
                    Keep only key business goals, decisions and pending questions.
                    Respond in %s language.
                    """.formatted(language);

            logLlmSelection("conversationSummary");
            String summary = currentChatClient().prompt()
                    .system(summaryPrompt)
                    .user(transcript.toString())
                    .call()
                    .content();

            conversation.setSummary(summary);
            conversationRepo.save(conversation);
            log("📝 Conversation summary updated ({} chars)", summary.length());

        } catch (Exception e) {
            logger.error("❌ Failed to update conversation summary", e);
        }
    }

    // =========================================================================
    // PRIVATE: Prompt string builders
    // =========================================================================

    /**
     * Builds the profile block injected into the FIRST message only.
     * Includes the current conversation summary for carryover context.
     */
    private String buildProfileBlock(UserProfile profile, String summary) {
        return """
                Business Name:               %s
                Legal Structure:             %s
                Primary Business Category:   %s
                Industry Sector:             %s
                Product Category:            %s
                Registered Office City:      %s
                Registered Office State:     %s
                Annual Turnover:             %s
                Employees:                   %s
                Running Loan:                %s
                Biggest Business Challenge:  %s
                Top Goal (Next 6 Months):    %s
                Manufacturing Type:          %s
                Trading Type:                %s
                Primary Customer Type:       %s
                Sales Geography:             %s
                Primary Sales Channel:       %s
                Total Loan Taken:            %s
                Outstanding Loan:            %s
                Interest Rate:               %s
                CIBIL Score Range:           %s
                Growth Plan:                 %s
                Prior Conversation Summary:  %s
                """.formatted(
                profile.getName(),
                profile.getLegalStructure(),
                profile.getPrimaryBusinessCategory(),
                nullSafe(profile.getIndustrySector()),
                profile.getProductCategory(),
                profile.getRegisteredOfficeCity(),
                profile.getRegisteredOfficeState(),
                profile.getAnnualTurnover(),
                profile.getEmployees(),
                profile.getRunningLoan(),
                profile.getBiggestBusinessChallenge(),
                profile.getTopGoal6Months(),
                nvl(profile.getManufacturingType()),
                nvl(profile.getTradingType()),
                nullSafe(profile.getPrimaryCustomerType()),
                nullSafe(profile.getSalesGeography()),
                profile.getPrimarySalesChannel(),
                profile.getTotalLoanTakenRange(),
                profile.getOutstandingLoanRange(),
                profile.getInterestRateRange(),
                profile.getCibilScoreRange(),
                nullSafe(profile.getGrowthPlan()),
                summary != null ? summary : "None"
        );
    }

    private String buildRagContext(List<Document> docs) {
        StringBuilder sb = new StringBuilder("Relevant Context:\n");
        for (Document doc : docs) {
            sb.append("- ").append(doc.getText()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Builds the user-facing prompt for media (file) messages.
     *
     * First message  → DB template + profile + optional file content
     * Follow-up      → raw user message + optional file content
     * Images         → extractedText is empty string; file is sent as binary media
     */
    private String buildMediaUserPrompt(boolean isNew,
                                        PromptTemplate template,
                                        UserProfile profile,
                                        String summary,
                                        String userMessage,
                                        String extractedText,
                                        String language) {
        String docBlock = (extractedText == null || extractedText.isBlank())
                ? ""
                : "\n\nUploaded Document Content:\n" + extractedText;
        String languageDirective = languageDirective(language);

        if (isNew) {
            return template.getUserPromptTemplate()
                    + " Answer using the userProfile below.\n"
                    + buildProfileBlock(profile, summary)
                    + docBlock
                    + languageDirective
                    + JSON_INSTRUCTION;
        } else {
            return userMessage + docBlock + languageDirective + JSON_INSTRUCTION;
        }
    }

    private String languageDirective(String language) {
        if (language == null || language.isBlank()) {
            return "";
        }
        return "\n\nUser explicitly requested language: " + language
                + ". Respond using that language and script.";
    }

    // =========================================================================
    // PRIVATE: JSON response parsing
    // =========================================================================

    /**
     * Parses the LLM response into (response, followUp).
     * Strips markdown fences first — some models wrap JSON in ```json blocks.
     * Falls back to returning the raw string if JSON parsing fails.
     */
    private ParsedResponse parseJsonResponse(String raw) {
        try {
            String cleaned = raw.replaceAll("(?s)```json|```", "").trim();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(cleaned);

            String response = json.get("response").asText();
            String followUp = json.has("followUp") ? json.get("followUp").asText() : null;

            // Prevent the follow-up question appearing twice inside the response
            if (followUp != null && !followUp.isBlank() && response.contains(followUp)) {
                response = response.replace(followUp, "").trim();
            }

            return new ParsedResponse(response, followUp);

        } catch (Exception e) {
            logger.warn("⚠ JSON parsing failed — returning raw response. Error: {}",
                    e.getMessage());
            return new ParsedResponse(raw, null);
        }
    }

    // =========================================================================
    // PRIVATE: Standard gate responses
    // =========================================================================

    private ChatResponse missingFieldsResponse(String conversationId,
                                               List<Integer> missing) {
        List<String> questions = missing.stream()
                .map(QuestionTextMapper.QUESTION_TEXT::get)
                .toList();
        StringBuilder sb = new StringBuilder(
                "To proceed, please answer the following questions:\n\n");
        for (int i = 0; i < questions.size(); i++) {
            sb.append(i + 1).append(". ").append(questions.get(i)).append("\n");
        }
        logger.warn("⚠ Returning missing-fields prompt. Questions: {}", questions);
        return new ChatResponse(conversationId, sb.toString(), null);
    }


    // =========================================================================
    // PRIVATE: File extraction
    // =========================================================================

    private String extractPdf(MultipartFile file) throws Exception {
        try (PDDocument doc = PDDocument.load(file.getInputStream())) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String extractExcel(MultipartFile file) throws Exception {
        StringBuilder text = new StringBuilder();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            for (Sheet sheet : workbook) {
                text.append("Sheet: ").append(sheet.getSheetName()).append("\n");
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        text.append(cell.toString()).append(" | ");
                    }
                    text.append("\n");
                }
                text.append("\n");
            }
        }
        return text.toString();
    }

    // =========================================================================
    // PRIVATE: Utilities
    // =========================================================================

    private String nvl(String value) {
        return value != null ? value : "Not specified";
    }

    private String nullSafe(List<String> list) {
        return (list == null || list.isEmpty())
                ? "Not specified"
                : String.join(", ", list);
    }

    private void log(String fmt, Object... args) {
        logger.info(fmt, args);
    }

    private void logLlmSelection(String operation) {
        try {
            LlmRuntimeSelectionResponse runtime = llmChatClientFactory.currentRuntimeSelection();
            log("🤖 LLM selection | op={} | provider={} | model={} | source={} | fallback={}",
                    operation,
                    runtime.provider(),
                    runtime.modelName(),
                    runtime.source(),
                    runtime.fallback());
        } catch (Exception e) {
            logger.warn("⚠ Could not resolve LLM runtime selection for op={}: {}", operation, e.getMessage());
        }
    }

    private ChatClient currentChatClient() {
        return llmChatClientFactory.getChatClient();
    }

    // =========================================================================
    // PRIVATE: Inner types
    // =========================================================================

    private record ParsedResponse(String response, String followUp) {}

    private record LlmCallResult(String content, int totalTokens) {}
}
