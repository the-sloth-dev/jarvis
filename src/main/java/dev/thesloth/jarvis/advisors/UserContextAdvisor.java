package dev.thesloth.jarvis.advisors;

import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.Content;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class UserContextAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {
    public static final String RETRIEVED_DOCUMENTS = "question_retrieved_documents";

    public static final String FILTER_EXPRESSION = "question_filter_expression";

    public static final String USER_QUESTION = "user_question";
    public static final String QUESTION_ANSWER_CONTEXT = "question_answer_context";

    private static final String DEFAULT_USER_TEXT_ADVISE = """

			Context information is below, surrounded by ---------------------

			---------------------
			{question_answer_context}
			---------------------

			Given the context and provided history information and not prior knowledge,
			reply to the user comment. If the answer is not in the context, inform
			the user that you can't answer the question.

            User question: {user_question}
            """;

    private static final int DEFAULT_ORDER = 0;

    private final VectorStore vectorStore;

    private final String userTextAdvise;

    private final SearchRequest searchRequest;

    private final boolean protectFromBlocking;

    private final int order;

    /**
     * The UserContextAdvisor retrieves context information from a Vector Store and
     * combines it with the user's text.
     *
     * @param vectorStore The vector store to use
     */
    public UserContextAdvisor(VectorStore vectorStore) {
        this(vectorStore, SearchRequest.defaults(), DEFAULT_USER_TEXT_ADVISE);
    }

    /**
     * The UserContextAdvisor retrieves context information from a Vector Store and
     * combines it with the user's text.
     *
     * @param vectorStore   The vector store to use
     * @param searchRequest The search request defined using the portable filter
     *                      expression syntax
     */
    public UserContextAdvisor(VectorStore vectorStore, SearchRequest searchRequest) {
        this(vectorStore, searchRequest, DEFAULT_USER_TEXT_ADVISE);
    }

    /**
     * The UserContextAdvisor retrieves context information from a Vector Store and
     * combines it with the user's text.
     *
     * @param vectorStore    The vector store to use
     * @param searchRequest  The search request defined using the portable filter
     *                       expression syntax
     * @param userTextAdvise The user text to append to the existing user prompt. The text
     *                       should contain a placeholder named "question_answer_context".
     */
    public UserContextAdvisor(VectorStore vectorStore, SearchRequest searchRequest, String userTextAdvise) {
        this(vectorStore, searchRequest, userTextAdvise, true);
    }

    /**
     * The UserContextAdvisor retrieves context information from a Vector Store and
     * combines it with the user's text.
     *
     * @param vectorStore         The vector store to use
     * @param searchRequest       The search request defined using the portable filter
     *                            expression syntax
     * @param userTextAdvise      The user text to append to the existing user prompt. The text
     *                            should contain a placeholder named "question_answer_context".
     * @param protectFromBlocking If true the advisor will protect the execution from
     *                            blocking threads. If false the advisor will not protect the execution from blocking
     *                            threads. This is useful when the advisor is used in a non-blocking environment. It
     *                            is true by default.
     */
    public UserContextAdvisor(VectorStore vectorStore, SearchRequest searchRequest, String userTextAdvise,
                                 boolean protectFromBlocking) {
        this(vectorStore, searchRequest, userTextAdvise, protectFromBlocking, DEFAULT_ORDER);
    }

    /**
     * The UserContextAdvisor retrieves context information from a Vector Store and
     * combines it with the user's text.
     *
     * @param vectorStore         The vector store to use
     * @param searchRequest       The search request defined using the portable filter
     *                            expression syntax
     * @param userTextAdvise      The user text to append to the existing user prompt. The text
     *                            should contain a placeholder named "question_answer_context".
     * @param protectFromBlocking If true the advisor will protect the execution from
     *                            blocking threads. If false the advisor will not protect the execution from blocking
     *                            threads. This is useful when the advisor is used in a non-blocking environment. It
     *                            is true by default.
     * @param order               The order of the advisor.
     */
    public UserContextAdvisor(VectorStore vectorStore, SearchRequest searchRequest, String userTextAdvise,
                                 boolean protectFromBlocking, int order) {

        Assert.notNull(vectorStore, "The vectorStore must not be null!");
        Assert.notNull(searchRequest, "The searchRequest must not be null!");
        Assert.hasText(userTextAdvise, "The userTextAdvise must not be empty!");

        this.vectorStore = vectorStore;
        this.searchRequest = searchRequest;
        this.userTextAdvise = userTextAdvise;
        this.protectFromBlocking = protectFromBlocking;
        this.order = order;
    }

    public static Builder builder(VectorStore vectorStore) {
        return new Builder(vectorStore);
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {

        AdvisedRequest advisedRequest2 = before(advisedRequest);

        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest2);

        return after(advisedResponse);
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {

        // This can be executed by both blocking and non-blocking Threads
        // E.g. a command line or Tomcat blocking Thread implementation
        // or by a WebFlux dispatch in a non-blocking manner.
        Flux<AdvisedResponse> advisedResponses = (this.protectFromBlocking) ?
                // @formatter:off
                Mono.just(advisedRequest)
                        .publishOn(Schedulers.boundedElastic())
                        .map(this::before)
                        .flatMapMany(request -> chain.nextAroundStream(request))
                : chain.nextAroundStream(before(advisedRequest));
        // @formatter:on

        return advisedResponses.map(ar -> {
            if (onFinishReason().test(ar)) {
                ar = after(ar);
            }
            return ar;
        });
    }

    private AdvisedRequest before(AdvisedRequest request) {

        var context = new HashMap<>(request.adviseContext());

        // 1. Search for similar documents in the vector store.
        String query = new PromptTemplate(request.userText(), request.userParams()).render();
        var searchRequestToUse = SearchRequest.from(this.searchRequest)
                .withQuery(query)
                .withFilterExpression(doGetFilterExpression(context));

        List<Document> documents = this.vectorStore.similaritySearch(searchRequestToUse);

        // 2. Create the context from the documents.
        context.put(RETRIEVED_DOCUMENTS, documents);

        String documentContext = documents.stream()
                .map(Content::getContent)
                .collect(Collectors.joining(System.lineSeparator()));

        // 3. Advise the user parameters.
        Map<String, Object> advisedUserParams = new HashMap<>(request.userParams());
        advisedUserParams.put(QUESTION_ANSWER_CONTEXT, documentContext);
        advisedUserParams.put(USER_QUESTION, request.userText());

        AdvisedRequest advisedRequest = AdvisedRequest.from(request)
                .withUserText(this.userTextAdvise)
                .withUserParams(advisedUserParams)
                .withAdviseContext(context)
                .build();

        return advisedRequest;
    }

    private AdvisedResponse after(AdvisedResponse advisedResponse) {
        ChatResponse.Builder chatResponseBuilder = ChatResponse.builder().from(advisedResponse.response());
        chatResponseBuilder.withMetadata(RETRIEVED_DOCUMENTS, advisedResponse.adviseContext().get(RETRIEVED_DOCUMENTS));
        return new AdvisedResponse(chatResponseBuilder.build(), advisedResponse.adviseContext());
    }

    protected Filter.Expression doGetFilterExpression(Map<String, Object> context) {

        if (!context.containsKey(FILTER_EXPRESSION)
                || !StringUtils.hasText(context.get(FILTER_EXPRESSION).toString())) {
            return this.searchRequest.getFilterExpression();
        }
        return new FilterExpressionTextParser().parse(context.get(FILTER_EXPRESSION).toString());

    }

    private Predicate<AdvisedResponse> onFinishReason() {
        return advisedResponse -> advisedResponse.response()
                .getResults()
                .stream()
                .filter(result -> result != null && result.getMetadata() != null
                        && StringUtils.hasText(result.getMetadata().getFinishReason()))
                .findFirst()
                .isPresent();
    }

    public static final class Builder {

        private final VectorStore vectorStore;

        private SearchRequest searchRequest = SearchRequest.defaults();

        private String userTextAdvise = DEFAULT_USER_TEXT_ADVISE;

        private boolean protectFromBlocking = true;

        private int order = DEFAULT_ORDER;

        private Builder(VectorStore vectorStore) {
            Assert.notNull(vectorStore, "The vectorStore must not be null!");
            this.vectorStore = vectorStore;
        }

        public UserContextAdvisor.Builder withSearchRequest(SearchRequest searchRequest) {
            Assert.notNull(searchRequest, "The searchRequest must not be null!");
            this.searchRequest = searchRequest;
            return this;
        }

        public UserContextAdvisor.Builder withUserTextAdvise(Resource userTextAdvise) {
            Assert.notNull(userTextAdvise, "The userTextAdvise must not be null!");
            Charset charset = Charset.defaultCharset();
            Assert.notNull(charset, "charset cannot be null");

            try {
                String contentAsString = userTextAdvise.getContentAsString(charset);
                Assert.hasText(contentAsString, "The userTextAdvise must not be empty!");
                this.userTextAdvise = contentAsString;
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public UserContextAdvisor.Builder withUserTextAdvise(String userTextAdvise) {
            Assert.hasText(userTextAdvise, "The userTextAdvise must not be empty!");
            this.userTextAdvise = userTextAdvise;
            return this;
        }

        public UserContextAdvisor.Builder withProtectFromBlocking(boolean protectFromBlocking) {
            this.protectFromBlocking = protectFromBlocking;
            return this;
        }

        public UserContextAdvisor.Builder withOrder(int order) {
            this.order = order;
            return this;
        }

        public UserContextAdvisor build() {
            return new UserContextAdvisor(this.vectorStore, this.searchRequest, this.userTextAdvise,
                    this.protectFromBlocking, this.order);
        }

    }
}
