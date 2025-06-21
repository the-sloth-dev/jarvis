package dev.thesloth.jarvis.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
public class ChatService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ChatClient chatClient;

    @Autowired
    ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public void chat(String userQuestion, Consumer<String> consumer) {
        this.chatClient
                .prompt()
                .user(userQuestion)
                .stream()
                .content()
                .doOnComplete(() -> logger.info("chat completed"))
                .onErrorComplete(err -> {
                    String message = String.format("chat failed to answer question: %s", userQuestion);
                    logger.error(message, err);
                    return true;
                })
                .subscribe(consumer);
    }
}
