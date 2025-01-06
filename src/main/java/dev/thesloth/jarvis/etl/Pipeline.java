package dev.thesloth.jarvis.etl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.KeywordMetadataEnricher;
import org.springframework.ai.transformer.SummaryMetadataEnricher;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Stream;

@Component
public class Pipeline {
    private static final Logger logger = LoggerFactory.getLogger(Pipeline.class);

    private final VectorStore store;
    private final TextSplitter splitter;
    private final SummaryMetadataEnricher summaryMetadata;
    private final KeywordMetadataEnricher keywordMetadata;
    private final DirectoryReader reader;

    @Autowired
    public Pipeline(VectorStore vectorStore,
                    TextSplitter splitter,
                    SummaryMetadataEnricher summaryMetadata,
                    KeywordMetadataEnricher keywordMetadata,
                    DirectoryReader reader) {
        this.store = vectorStore;
        this.splitter = splitter;
        this.summaryMetadata = summaryMetadata;
        this.keywordMetadata = keywordMetadata;
        this.reader = reader;
    }

    public void ingest() {
        logger.info("Started documents ingestion");

        reader.stream(document -> {
            Stream.of(new TikaDocumentReader(document).read())
                    .peek(documents -> logger.info("Started splitting document text into chunks"))
                    .map(splitter)
                    .peek(documents -> logger.info("Completed splitting document text into chunks"))
                    .peek(documents -> logger.info("Started summarizing document"))
                    .map(summaryMetadata)
                    .peek(documents -> logger.info("Completed summarizing document"))
                    .peek(documents -> logger.info("Started extracting document keywords"))
                    .map(keywordMetadata)
                    .peek(documents -> logger.info("Completed extracting document keywords"))
                    .peek(documents -> logger.info("Storing {} documents", documents.size()))
                    .forEach(store);
        });

        logger.info("Completed documents ingestion");
    }
}
