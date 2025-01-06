package dev.thesloth.jarvis.parsers;

import org.springframework.ai.chat.model.ChatResponse;

import javax.annotation.Nullable;
import java.util.function.Function;



public class ResponseParser implements Function<ChatResponse, String> {

    @Override
    public String apply(@Nullable ChatResponse response) {
        if (response == null) return "None";

        return Parser.builder()
                .lineBreak()
                .lineBreak()
                .append("-------- Response --------")
                .append(response.getResults())
                .append(response.getMetadata())
                .append("-------- Response --------")
                .lineBreak()
                .build();
    }
}
