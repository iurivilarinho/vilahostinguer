package com.bancada.repository;

import com.bancada.enums.InvoiceStatus;
import com.bancada.models.Invoice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long>, JpaSpecificationExecutor<Invoice> {

    List<Invoice> findBySubscriptionIdAndStatus(Long subscriptionId, InvoiceStatus status);

    List<Invoice> findByStatusAndProviderPaymentIdIsNotNull(InvoiceStatus status);

    boolean existsBySubscriptionIdAndStatus(Long subscriptionId, InvoiceStatus status);
}
