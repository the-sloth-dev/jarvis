package dev.thesloth.jarvis.advisors;

import dev.thesloth.jarvis.parsers.RequestParser;
import dev.thesloth.jarvis.parsers.ResponseParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;

import org.springframework.ai.chat.model.MessageAggregator;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;

public class LogAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private static final Logger logger = LoggerFactory.getLogger(LogAdvisor.class);

    private final RequestParser requestParser;
    private final ResponseParser responseParser;

    private int order;

    public LogAdvisor(int order) {
        this(new RequestParser(), new ResponseParser(), order);
    }

    public LogAdvisor(RequestParser requestParser,
                      ResponseParser responseParser,
                      int order) {
        this.requestParser = requestParser;
        this.responseParser = responseParser;
        this.order = order;
    }

    private AdvisedRequest before(AdvisedRequest request) {
        if (logger.isDebugEnabled()) {
            var id = request.advisorParams().get(CHAT_MEMORY_CONVERSATION_ID_KEY);

            if (id == null) {
                logger.debug("Chat request: \n{}", this.requestParser.apply(request));
            } else {
                logger.debug("[{}] Chat request: \n{}", id, this.requestParser.apply(request));
            }
        }

        return request;
    }

    private void observeAfter(AdvisedResponse advisedResponse) {
        if (logger.isDebugEnabled()) {
            var id = advisedResponse.adviseContext().get(CHAT_MEMORY_CONVERSATION_ID_KEY);

            if (id == null) {
                logger.debug("Chat response: \n{}", this.responseParser.apply(advisedResponse.response()));
            } else {
                logger.debug("[{}] Chat response: \n{}", id, this.responseParser.apply(advisedResponse.response()));
            }
        }
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        advisedRequest = before(advisedRequest);

        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);

        observeAfter(advisedResponse);

        return advisedResponse;
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        advisedRequest = before(advisedRequest);

        Flux<AdvisedResponse> advisedResponses = chain.nextAroundStream(advisedRequest);

        return new MessageAggregator().aggregateAdvisedResponse(advisedResponses, this::observeAfter);
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int getOrder() {
        return this.order;
    }
}
