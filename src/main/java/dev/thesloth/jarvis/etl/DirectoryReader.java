package dev.thesloth.jarvis.etl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import static java.nio.file.Files.newDirectoryStream;

@Component
public class DirectoryReader {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${spring.ai.etl.reader.directory}")
    private String directory;

    @Value("${spring.ai.etl.reader.supported.extensions}")
    private String supportedExtensions;

    public void stream(Consumer<Resource> consumer) {
        logger.info("Started streaming documents from directory :: {}", directory);
        logger.info("Stream only documents with extension :: {}", supportedExtensions);

        try {
            newDirectoryStream(Path.of(directory), supportedExtensions).forEach(path -> {
                logger.info("Streaming document :: {}", path.getFileName());
                consumer.accept(new FileSystemResource(path));
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed reading from directory :: " + directory, e);
        }

        logger.info("Completed streaming documents from directory :: {}", directory);
    }
}
