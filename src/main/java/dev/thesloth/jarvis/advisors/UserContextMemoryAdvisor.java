package dev.thesloth.jarvis.advisors;

import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.MessageAggregator;
import org.springframework.ai.model.Content;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UserContextMemoryAdvisor extends AbstractChatMemoryAdvisor<ChatMemory> {

    private static final String DEFAULT_USER_TEXT_ADVISE = """
            
            Use the conversation memory from the MEMORY section to provide accurate answers.
            
            ---------------------
            MEMORY:
            {memory}
            ---------------------
            
            """;

    private final String userTextAdvise;

    public UserContextMemoryAdvisor(ChatMemory chatMemory, String defaultConversationId, int chatHistoryWindowSize,
                                    String systemTextAdvise, int order) {
        super(chatMemory, defaultConversationId, chatHistoryWindowSize, true, order);
        this.userTextAdvise = systemTextAdvise;
    }

    public static UserContextMemoryAdvisor.Builder builder(ChatMemory chatMemory) {
        return new UserContextMemoryAdvisor.Builder(chatMemory);
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {

        advisedRequest = this.before(advisedRequest);

        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);

        this.observeAfter(advisedResponse);

        return advisedResponse;
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {

        Flux<AdvisedResponse> advisedResponses = this.doNextWithProtectFromBlockingBefore(advisedRequest, chain,
                this::before);

        return new MessageAggregator().aggregateAdvisedResponse(advisedResponses, this::observeAfter);
    }

    private AdvisedRequest before(AdvisedRequest request) {

        // 1. Advise user parameters.
        List<Message> memoryMessages = this.getChatMemoryStore()
                .get(this.doGetConversationId(request.adviseContext()),
                        this.doGetChatMemoryRetrieveSize(request.adviseContext()));

        String memory = (memoryMessages != null) ? memoryMessages.stream()
                .filter(m -> m.getMessageType() == MessageType.USER || m.getMessageType() == MessageType.ASSISTANT)
                .map(m -> m.getMessageType() + ":" + ((Content) m).getContent())
                .collect(Collectors.joining(System.lineSeparator())) : "";

        Map<String, Object> advisedUserParams = new HashMap<>(request.userParams());
        advisedUserParams.put("memory", memory);

        // 2. Advise the user text.
        String advisedUserText = request.userText() + System.lineSeparator() + this.userTextAdvise;

        // 3. Create a new request with the advised user text and parameters.
        AdvisedRequest advisedRequest = AdvisedRequest.from(request)
                .withUserText(advisedUserText)
                .withUserParams(advisedUserParams)
                .build();

        // 4. Add the new user input to the conversation memory.
        UserMessage userMessage = new UserMessage(request.userText(), request.media());
        this.getChatMemoryStore().add(this.doGetConversationId(request.adviseContext()), userMessage);

        return advisedRequest;
    }

    private void observeAfter(AdvisedResponse advisedResponse) {

        List<Message> assistantMessages = advisedResponse.response()
                .getResults()
                .stream()
                .map(g -> (Message) g.getOutput())
                .toList();

        this.getChatMemoryStore().add(this.doGetConversationId(advisedResponse.adviseContext()), assistantMessages);
    }

    public static class Builder extends AbstractChatMemoryAdvisor.AbstractBuilder<ChatMemory> {

        private String userTextAdvise = DEFAULT_USER_TEXT_ADVISE;

        protected Builder(ChatMemory chatMemory) {
            super(chatMemory);
        }

        public UserContextMemoryAdvisor.Builder withUserTextAdvise(String systemTextAdvise) {
            this.userTextAdvise = systemTextAdvise;
            return this;
        }

        public UserContextMemoryAdvisor build() {
            return new UserContextMemoryAdvisor(this.chatMemory, this.conversationId, this.chatMemoryRetrieveSize,
                    this.userTextAdvise, this.order);
        }

    }
}
