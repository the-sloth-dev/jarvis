package dev.thesloth.jarvis;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import dev.thesloth.jarvis.advisors.LogAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

@Push
@SpringBootApplication
@Theme(value = "jarvis")
public class JarvisApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(JarvisApplication.class, args);
    }

    @Value("classpath:prompts/system.st")
    private Resource systemPrompt;


    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.defaultSystem(systemPrompt).defaultAdvisors(new LogAdvisor(3)).build();
    }

}
