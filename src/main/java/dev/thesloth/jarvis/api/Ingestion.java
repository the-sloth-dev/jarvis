package dev.thesloth.jarvis.api;

import dev.thesloth.jarvis.etl.Pipeline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ingest")
public class Ingestion {

    private final Pipeline pipeline;

    @Autowired
    public Ingestion(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    @PostMapping("/run")
    public ResponseEntity<?> run(){
        pipeline.ingest();
        return ResponseEntity.accepted().build();
    }
}
