package com.lcs.subscription.infrastructure;

import com.lcs.subscription.application.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 만료 임박 구독권 자동 재발급 배치 스케줄러.
 *
 * <p>매일 0시에 {@code SubscriptionService.reissueExpiringSubscriptions()}를 호출하여
 * 만료 임박(2일 이내) 구독권을 자동으로 재발급하는 위임 책임만 갖는다.
 */
@Component
public class SubscriptionReissueScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionReissueScheduler.class);

    private final SubscriptionService subscriptionService;

    public SubscriptionReissueScheduler(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void reissueExpiringSubscriptions() {
        LOGGER.info("Starting scheduled task: reissueExpiringSubscriptions");

        subscriptionService.reissueExpiringSubscriptions();

        LOGGER.info("Finished scheduled task: reissueExpiringSubscriptions");
    }
}
