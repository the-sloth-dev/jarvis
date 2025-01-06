package dev.thesloth.jarvis.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

@Service
public class ChatService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ChatClient chatClient;

    @Autowired
    ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public void chat(String chatId, String userQuestion, Consumer<String> consumer) {
        this.chatClient
                .prompt()
                .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId))
                .user(userQuestion)
                .stream()
                .content()
                .doOnComplete(() -> logger.info("[{}] Chat completed", chatId))
                .onErrorComplete(err -> {
                    String message = String.format("[%s] Chat failed to answer question: %s", chatId, userQuestion);
                    logger.error(message, err);
                    return true;
                })
                .subscribe(consumer);
    }
}
