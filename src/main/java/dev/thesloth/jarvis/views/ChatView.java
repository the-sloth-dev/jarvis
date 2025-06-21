package dev.thesloth.jarvis.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import dev.thesloth.jarvis.services.ChatService;
import org.vaadin.firitin.components.messagelist.MarkdownMessage;

@PageTitle("Chat")
@Route(value = "", layout = MainLayout.class)
public class ChatView extends VerticalLayout {

    private static final String TEXT_AREA_FOCUS = "requestAnimationFrame(() => this.querySelector('vaadin-text-area').focus() )";

    private final ChatService chatService;

    private MessageInput messageInput = new MessageInput();

    public ChatView(ChatService chatService) {
        this.chatService = chatService;

        setPadding(false);
        setSpacing(false);

        var newChatButton = new Button("New Chat");
        newChatButton.addClassName("new-chat-button");

        var messageList = new VerticalLayout();
        messageList.setSpacing(true);
        messageList.addClassNames(
                LumoUtility.Padding.Horizontal.SMALL,
                LumoUtility.Margin.Horizontal.AUTO,
                LumoUtility.MaxWidth.SCREEN_MEDIUM
        );

        newChatButton.addClickListener(e -> {
            messageList.removeAll();
            focusMessageInput();
        });

        messageInput.setWidthFull();
        messageInput.addClassNames(
                LumoUtility.Padding.Horizontal.LARGE,
                LumoUtility.Padding.Vertical.MEDIUM,
                LumoUtility.Margin.Horizontal.AUTO,
                LumoUtility.MaxWidth.SCREEN_MEDIUM
        );

        messageInput.addSubmitListener(e -> {
            var userQuestion = e.getValue();
            var question = new MarkdownMessage(userQuestion, "You");
            question.addClassName("you");
            var answer = new MarkdownMessage("Jarvis");
            answer.getElement().executeJs("this.scrollIntoView()");

            messageList.add(question);
            messageList.add(answer);

            this.chatService.chat(userQuestion, answer::appendMarkdownAsync);
        });

        var scroller = new Scroller(messageList);
        scroller.addClassName("chat-view-scroller-1");
        scroller.setWidthFull();
        scroller.addClassName(LumoUtility.AlignContent.END);
        addAndExpand(scroller);

        add(newChatButton, messageInput);

        focusMessageInput();
    }

    private void focusMessageInput() {
        messageInput.getElement().executeJs(TEXT_AREA_FOCUS);
    }

}
