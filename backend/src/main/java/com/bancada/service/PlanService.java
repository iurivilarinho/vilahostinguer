package com.bancada.service;

import com.bancada.enums.AuditAction;
import com.bancada.enums.MachineDistribution;
import com.bancada.enums.MachineStatus;
import com.bancada.enums.SubscriptionStatus;
import com.bancada.filter.PlanFilter;
import com.bancada.records.HyperVStatus;
import com.bancada.models.Machine;
import com.bancada.models.Plan;
import com.bancada.models.Subscription;
import com.bancada.records.PlanSnapshot;
import com.bancada.repository.MachineRepository;
import com.bancada.repository.PlanRepository;
import com.bancada.repository.SubscriptionRepository;
import com.bancada.request.PlanRequest;
import com.bancada.response.DistributionResponse;
import com.bancada.response.PlanResponse;
import com.bancada.response.PortalPlanResponse;
import com.bancada.specification.PlanSpecification;
import jakarta.persistence.EntityNotFoundException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plans for sale. Every server is a virtual machine on this PC; a plan is available while the PC
 * still has memory for one more.
 */
@Service
public class PlanService {

    private static final String ENTITY = "Plan";
    private static final List<SubscriptionStatus> WAITING_FOR_MACHINE = List.of(SubscriptionStatus.PENDING_PAYMENT,
        SubscriptionStatus.PROVISIONING);

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MachineRepository machineRepository;
    private final HyperVService hyperVService;
    private final AuditService auditService;
    private final long reservedMemoryMb;

    public PlanService(PlanRepository planRepository, SubscriptionRepository subscriptionRepository, MachineRepository machineRepository,
                       HyperVService hyperVService, AuditService auditService,
                       @Value("${bancada.vms.reserved-memory-mb}") long reservedMemoryMb) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.machineRepository = machineRepository;
        this.hyperVService = hyperVService;
        this.auditService = auditService;
        this.reservedMemoryMb = reservedMemoryMb;
    }

    @Transactional(readOnly = true)
    public Page<Plan> search(PlanFilter filter, Pageable pageable) {
        Specification<Plan> specification = Specification.where(PlanSpecification.search(filter.getSearch()))
            .and(PlanSpecification.active(filter.getActive()));
        return planRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Plan findById(Long id) {
        return planRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Plano não encontrado para ID: " + id));
    }

    @Transactional
    public Plan create(PlanRequest request) {
        Plan plan = planRepository.save(new Plan(request));
        auditService.record(AuditAction.CREATE, ENTITY, plan.getId(), null, new PlanSnapshot(plan), null);
        return plan;
    }

    /** A new price is for new contracts: subscriptions keep the price they were sold with. */
    @Transactional
    public Plan update(Long id, PlanRequest request) {
        Plan plan = findById(id);
        PlanSnapshot before = new PlanSnapshot(plan);
        plan.update(request);
        Plan saved = planRepository.save(plan);
        auditService.record(AuditAction.UPDATE, ENTITY, id, before, new PlanSnapshot(saved), null);
        return saved;
    }

    @Transactional(readOnly = true)
    public PlanResponse toResponse(Plan plan) {
        return new PlanResponse(plan, isAvailable(plan), subscriptionRepository.countByPlanId(plan.getId()));
    }

    /** Storefront: active plans with price per period, operating systems and stock. */
    @Transactional(readOnly = true)
    public List<PortalPlanResponse> storefront() {
        return planRepository.findByActiveTrueOrderByOrderNumberAscPriceMonthlyAsc().stream()
            .map(plan -> new PortalPlanResponse(plan, isAvailable(plan), distributions(plan)))
            .toList();
    }

    @Transactional(readOnly = true)
    public Plan requireForSale(Long id) {
        Plan plan = findById(id);
        if (!plan.isActive()) {
            throw new IllegalStateException("Este plano não está mais à venda.");
        }
        if (!isAvailable(plan)) {
            throw new IllegalStateException("Este plano está esgotado no momento. Escolha outro ou tente mais tarde.");
        }
        return plan;
    }

    public List<DistributionResponse> distributions(Plan plan) {
        return Arrays.stream(MachineDistribution.values())
            .map(distribution -> new DistributionResponse(distribution, distribution.getDisplayName(), distribution.getVersions(), true))
            .toList();
    }

    /**
     * One more server fits while this PC has memory for it: machines already there plus orders
     * waiting to be created, against the PC memory minus what stays for Windows.
     */
    public boolean isAvailable(Plan plan) {
        HyperVStatus status;
        try {
            status = hyperVService.cachedStatus();
        } catch (RuntimeException exception) {
            return false;
        }
        if (!status.ready()) {
            return false;
        }
        long committedMb = 0;
        for (Machine machine : machineRepository.findByStatusNot(MachineStatus.REMOVED)) {
            committedMb += machine.getMemoryMb();
        }
        for (Subscription subscription : subscriptionRepository.findByStatusInAndMachineIsNull(WAITING_FOR_MACHINE)) {
            committedMb += subscription.getPlan().getMemoryMb();
        }
        return committedMb + plan.getMemoryMb() <= status.memoryMb() - reservedMemoryMb;
    }
}
