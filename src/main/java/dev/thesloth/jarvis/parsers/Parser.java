package dev.thesloth.jarvis.parsers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.ModelOptionsUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Parser {
    private static final Logger logger = LoggerFactory.getLogger(Parser.class);

    private static final String LINE_BREAK = "\n";
    private static final String EMPTY_ARRAY = "[]";
    private static final String NONE = "None";
    private static final String KEY_VALUE_SEPARATOR = ": ";

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final StringBuilder stringBuilder;

        private Builder() {
            this.stringBuilder = new StringBuilder();
        }

        public Builder append(Map<?, ?> data) {
            if (data != null && !data.isEmpty()) {

                for (Entry<?, ?> entry : data.entrySet()) {
                    if (entry.getKey() == null) continue;
                    if (entry.getValue() == null) continue;

                    switch (entry.getValue()) {
                        case String value:
                            appendKey(entry.getKey());
                            append(value);
                            break;
                        case Number value:
                            appendKey(entry.getKey());
                            append(value);
                            break;
                        case Boolean value:
                            appendKey(entry.getKey());
                            append(value);
                            break;
                        case MessageType value:
                            appendKey(entry.getKey());
                            append(value.getValue());
                            break;
                        case List<?> value:
                            appendKey(entry.getKey());
                            lineBreak();
                            append(value);
                            break;
                        case Map<?, ?> value:
                            append(value);
                            break;
                        default:
                            logError(entry.getValue());
                    }
                }
            }
            return this;
        }

        public Builder append(List<?> elements) {
            if (!elements.isEmpty()) {
                for (Object element : elements) {
                    if (element == null) continue;

                    switch (element) {
                        case String value:
                            append(value);
                            break;
                        case Number value:
                            append(value);
                            break;
                        case Boolean value:
                            append(value);
                            break;
                        case Document value:
                            append(value.getId());
                            break;
                        case Generation value:
                            append(value);
                            break;
                        case List<?> value:
                            append(value);
                            break;
                        case Map<?, ?> value:
                            append(value);
                            break;
                        default:
                            logError(element);
                    }
                }
            } else {
                this.stringBuilder.append(EMPTY_ARRAY);
                lineBreak();
            }

            return this;
        }

        public Builder append(String value) {
            if (!value.isBlank()) {
                this.stringBuilder.append(value);
            } else {
                this.stringBuilder.append(NONE);
            }
            lineBreak();
            return this;
        }

        public Builder append(Boolean value) {
            this.stringBuilder.append(value);
            lineBreak();
            return this;
        }

        public Builder append(Number value) {
            this.stringBuilder.append(value);
            lineBreak();
            return this;
        }

        public Builder lineBreak() {
            this.stringBuilder.append(LINE_BREAK);
            return this;
        }

        public Builder append(ChatResponseMetadata metadata) {
            Map<String, Object> data = ModelOptionsUtils.objectToMap(metadata);
            if (!data.isEmpty()) {
                append(data);
            }
            return this;
        }

        public Builder append(ChatModel chatModel) {
            Map<String, Object> data = ModelOptionsUtils.objectToMap(chatModel);
            if (!data.isEmpty()) {
                append(data);
            }
            return this;
        }

        public Builder append(String key, @Nullable String value) {
            if (value != null && !value.isBlank()) {
                appendKey(key);
                append(value);
            }
            return this;
        }

        public Builder append(String key, List<?> values) {
            if (!values.isEmpty()) {
                appendKey(key);
                lineBreak();
                append(values);
            }
            return this;
        }

        public Builder append(String key, Map<String, Object> values) {
            if (values != null && !values.isEmpty()) {
                appendKey(key);
                lineBreak();
                append(values);
            }
            return this;
        }

        public String build() {
            return this.stringBuilder.toString();
        }

        private void append(Generation value) {
            if (value != null) {
                AssistantMessage output = value.getOutput();
                if (output != null) {
                    append("Answer", output.getContent());
                    append(output.getMetadata());
                    append("Total calls", output.getToolCalls());
                }
                append(ModelOptionsUtils.objectToMap(value.getMetadata()));
            }
        }

        private void appendKey(Object key) {
            this.stringBuilder.append(key).append(KEY_VALUE_SEPARATOR);
        }

        private void logError(Object value) {
            logger.error("Could not parse value", new Throwable("Unexpected type: " + value.getClass()));
        }
    }
}
