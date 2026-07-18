package com.lcs.subscription.domain.repository;

import com.lcs.subscription.domain.model.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByMemberId(Long memberId);

    List<Subscription> findByMemberId(Long memberId);

    List<Subscription> findByActivatedAtIsNotNullAndSuspendedAtIsNullAndCancelledAtIsNull();
}
