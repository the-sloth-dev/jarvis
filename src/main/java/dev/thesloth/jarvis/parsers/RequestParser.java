package dev.thesloth.jarvis.parsers;

import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;

import java.util.function.Function;

public class RequestParser implements Function<AdvisedRequest, String> {

    @Override
    public String apply(AdvisedRequest request) {
        return Parser.builder()
                .append("-------- LLM --------")
                .append(request.chatModel())
                .append("Functions", request.functionNames())
                .append("Callbacks", request.functionCallbacks())
                .append("-------- LLM --------")
                .lineBreak()
                .lineBreak()
                .append("-------- System --------")
                .append("Prompt", request.systemText())
                .append("Parameters", request.systemParams())
                .append("-------- System --------")
                .lineBreak()
                .lineBreak()
                .append("-------- User --------")
                .append("Prompt", request.userText())
                .append(request.userParams())
                .append("History", request.functionNames())
                .append("-------- User --------")
                .lineBreak()
                .lineBreak()
                .append("-------- Request --------")
                .append("Advisors", request.functionNames())
                .append("Media", request.media())
                .append("Context", request.adviseContext())
                .append("Parameters", request.advisorParams())
                .append("-------- Request --------")
                .build();
    }

}
