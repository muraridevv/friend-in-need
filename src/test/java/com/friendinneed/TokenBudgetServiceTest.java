package com.friendinneed;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.friendinneed.token.TokenBudgetService;
import org.junit.jupiter.api.Test;

class TokenBudgetServiceTest {
    private final TokenBudgetService budgets = new TokenBudgetService(6_000);
    @Test void removesOldestHistoryBeforeMemories() {
        String prompt = budgets.trimToFit("system", "old\nnew", "memory", 4);
        assertFalse(prompt.contains("old")); assertTrue(prompt.contains("new"));
    }
    @Test void removesMemoriesWhenHistoryIsGone() {
        assertFalse(budgets.trimToFit("sys", "", "old-memory\nnew-memory", 4).contains("old-memory"));
    }
}
