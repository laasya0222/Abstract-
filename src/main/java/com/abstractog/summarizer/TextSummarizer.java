package com.abstractog.summarizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TextSummarizer {

    private static final Set<String> STOP_WORDS = new HashSet<>(Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has", "he", "in", "is",
            "it", "its", "of", "on", "that", "the", "to", "was", "were", "will", "with", "this", "these",
            "those", "or", "if", "then", "than", "you", "your", "we", "our", "they", "their", "them", "i",
            "me", "my", "mine", "can", "could", "should", "would", "about", "into", "over", "under", "also",
            "not", "no", "do", "does", "did", "done", "have", "had", "having", "such", "so", "but", "because"
    ));

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9']+");

    public String summarizeText(String text, int maxSentences) {
        List<String> sentences = splitSentences(text);
        if (sentences.isEmpty()) {
            return "";
        }

        if (sentences.size() <= maxSentences) {
            return String.join(" ", sentences);
        }

        List<String> allWords = tokenize(text).stream()
                .filter(word -> !STOP_WORDS.contains(word) && !word.chars().allMatch(Character::isDigit))
                .collect(Collectors.toList());

        if (allWords.isEmpty()) {
            return String.join(" ", sentences.subList(0, Math.min(maxSentences, sentences.size())));
        }

        Map<String, Integer> frequency = new HashMap<>();
        for (String word : allWords) {
            frequency.put(word, frequency.getOrDefault(word, 0) + 1);
        }

        int maxFrequency = frequency.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        List<SentenceScore> scored = new ArrayList<>();
        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            List<String> sentenceWords = tokenize(sentence).stream()
                    .filter(word -> !STOP_WORDS.contains(word))
                    .collect(Collectors.toList());

            if (sentenceWords.isEmpty()) {
                scored.add(new SentenceScore(i, 0.0));
                continue;
            }

            double score = 0.0;
            for (String word : sentenceWords) {
                score += (double) frequency.getOrDefault(word, 0) / maxFrequency;
            }

            double lengthPenalty = 1 + Math.log(sentenceWords.size() + 1);
            scored.add(new SentenceScore(i, score / lengthPenalty));
        }

        List<Integer> selectedIndices = scored.stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(maxSentences)
                .map(SentenceScore::index)
                .sorted()
                .collect(Collectors.toList());

        List<String> selectedSentences = new ArrayList<>();
        for (Integer index : selectedIndices) {
            selectedSentences.add(sentences.get(index));
        }
        return String.join(" ", selectedSentences);
    }

    public List<PageSummary> summarizePages(String text, int maxSentencesPerPage) {
        String[] pages = text.split("\\f");
        List<String> cleanedPages = new ArrayList<>();
        for (String page : pages) {
            String cleaned = page.trim();
            if (!cleaned.isEmpty()) {
                cleanedPages.add(cleaned);
            }
        }

        if (cleanedPages.isEmpty()) {
            if (text != null && !text.trim().isEmpty()) {
                cleanedPages.add(text.trim());
            }
        }

        List<PageSummary> summaries = new ArrayList<>();
        for (int i = 0; i < cleanedPages.size(); i++) {
            String summary = summarizeText(cleanedPages.get(i), maxSentencesPerPage);
            summaries.add(new PageSummary(i + 1, summary));
        }
        return summaries;
    }

    private List<String> splitSentences(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String normalized = text.replaceAll("\\s+", " ").trim();
        String[] parts = normalized.split("(?<=[.!?])\\s+");
        List<String> sentences = new ArrayList<>();
        for (String part : parts) {
            String sentence = part.trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }
        return sentences;
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }

        Matcher matcher = TOKEN_PATTERN.matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    public record PageSummary(int pageNumber, String summary) {}

    private record SentenceScore(int index, double score) {}
}
