package com.friendinneed.token;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenBudgetService {
    private final int defaultMaxTokens;

    public TokenBudgetService(@Value("${token-budget:6000}") int defaultMaxTokens) {
        this.defaultMaxTokens = defaultMaxTokens;
    }

    public String trimToFit(String systemPrompt, String history, String memories, int maxTokens) {
        String retainedHistory = history == null ? "" : history;
        String retainedMemories = memories == null ? "" : memories;
        int budget = maxTokens > 0 ? maxTokens : defaultMaxTokens;
        while (estimateTokens(systemPrompt, retainedHistory, retainedMemories) > budget && !retainedHistory.isBlank()) {
            retainedHistory = removeOldestLine(retainedHistory);
        }
        while (estimateTokens(systemPrompt, retainedHistory, retainedMemories) > budget && !retainedMemories.isBlank()) {
            retainedMemories = removeOldestLine(retainedMemories);
        }
        String prompt = systemPrompt + "\n" + retainedHistory + "\n" + retainedMemories;
        int maximumCharacters = budget * 4;
        return prompt.length() <= maximumCharacters ? prompt : prompt.substring(0, maximumCharacters);
    }

    private int estimateTokens(String systemPrompt, String history, String memories) {
        return (systemPrompt.length() + history.length() + memories.length() + 2 + 3) / 4;
    }

    private String removeOldestLine(String value) {
        int newline = value.indexOf('\n');
        return newline < 0 ? "" : value.substring(newline + 1);
    }
}
