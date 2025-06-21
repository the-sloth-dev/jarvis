package dev.thesloth.jarvis;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import dev.thesloth.jarvis.advisors.LogAdvisor;
import dev.thesloth.jarvis.advisors.UserContextAdvisor;
import dev.thesloth.jarvis.advisors.UserContextMemoryAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.transformer.KeywordMetadataEnricher;
import org.springframework.ai.transformer.SummaryMetadataEnricher;
import org.springframework.ai.transformer.SummaryMetadataEnricher.SummaryType;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import java.util.List;

import static org.apache.logging.log4j.util.Strings.EMPTY;

@Push
@SpringBootApplication
@Theme(value = "jarvis")
public class JarvisApplication implements AppShellConfigurator {

	public static void main(String[] args) {
		SpringApplication.run(JarvisApplication.class, args);
	}

	@Value("classpath:prompts/system.st")
	private Resource systemPrompt;

	@Value("classpath:prompts/user.st")
	private Resource userPrompt;

	@Value("${spring.ai.rag.similarity-threshold}")
	private double similarityThreshold = 0.5;

	@Value("${spring.ai.rag.top-k}")
	private int topK = 4;

	@Bean
	TextSplitter splitter() {
		return new TokenTextSplitter();
	}

	@Bean
	public SummaryMetadataEnricher summaryMetadata(ChatModel chatModel) {
		return new SummaryMetadataEnricher(
				chatModel,
				List.of(SummaryType.PREVIOUS, SummaryType.CURRENT, SummaryType.NEXT)
		);
	}

	@Bean
	public KeywordMetadataEnricher keywordMetadata(ChatModel chatModel) {
		return new KeywordMetadataEnricher(
				chatModel,
				5
		);
	}

	@Bean
	public ChatMemory chatMemory() {
		return new InMemoryChatMemory();
	}

	@Bean
	public ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, VectorStore vectorStore) {
		return chatClientBuilder
				.defaultSystem(systemPrompt)
				.defaultAdvisors(
						UserContextMemoryAdvisor // Chat Memory
								.builder(chatMemory)
								.withUserTextAdvise(EMPTY)
								.withOrder(1)
								.build(),
						UserContextAdvisor // RAG
								.builder(vectorStore)
								.withUserTextAdvise(userPrompt)
								.withOrder(2)
								.withSearchRequest(SearchRequest.defaults()
										.withSimilarityThreshold(similarityThreshold)
										.withTopK(topK)
								)
								.build(),
						new LogAdvisor(3)
				)
				.build();
	}

}
