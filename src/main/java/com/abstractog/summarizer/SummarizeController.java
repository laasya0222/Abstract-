package com.abstractog.summarizer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SummarizeController {

    private final TextSummarizer summarizer = new TextSummarizer();

    @PostMapping("/summarize")
    public ResponseEntity<?> summarize(@RequestBody SummaryRequest request) {
        if (request == null || request.text() == null || request.text().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Input text is required"));
        }

        int sentenceCount = request.sentences() != null && request.sentences() > 0 ? request.sentences() : 3;
        String summary = summarizer.summarizeText(request.text(), sentenceCount);
        return ResponseEntity.ok(new SummaryResponse(summary));
    }
}
