package com.bancada.repository;

import com.bancada.enums.SubscriptionStatus;
import com.bancada.models.Subscription;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long>, JpaSpecificationExecutor<Subscription> {

    List<Subscription> findByStatusIn(Collection<SubscriptionStatus> statuses);

    List<Subscription> findByPlanDeviceIdAndStatusIn(Long deviceId, Collection<SubscriptionStatus> statuses);

    Optional<Subscription> findByMachineId(Long machineId);

    boolean existsByCustomerIdAndHostnameAndStatusNot(Long customerId, String hostname, SubscriptionStatus status);

    long countByPlanId(Long planId);

    boolean existsByCustomerIdAndStatusIn(Long customerId, Collection<SubscriptionStatus> statuses);
}
