package com.friendinneed.routing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelRouterTest {
    @Test
    void usesCloudOnlyWhenRecentPingIsFastAndHealthy() {
        HealthPinger chat = new HealthPinger();
        chat.recordSuccess(4_999);
        ModelRouter router = new ModelRouter(chat, new HealthPinger(), new HealthPinger(), "https://chat", "https://voice", "https://embed", false, false);
        assertEquals(ModelChoice.CLOUD, router.routeChat());
        HealthPinger slow = new HealthPinger();
        slow.recordSuccess(5_000);
        ModelRouter slowRouter = new ModelRouter(slow, new HealthPinger(), new HealthPinger(), "https://chat", "https://voice", "https://embed", false, false);
        assertEquals(ModelChoice.LOCAL, slowRouter.routeChat());
    }

    @Test
    void preferLocalOverridesHealthyCloud() {
        HealthPinger chat = new HealthPinger();
        chat.recordSuccess(10);
        ModelRouter router = new ModelRouter(chat, new HealthPinger(), new HealthPinger(), "https://chat", "https://voice", "https://embed", true, false);
        assertEquals(ModelChoice.LOCAL, router.routeChat());
    }
}
