package com.bancada.service;

import com.bancada.enums.AuditAction;
import com.bancada.enums.MachineDistribution;
import com.bancada.enums.MachineStatus;
import com.bancada.enums.SubscriptionStatus;
import com.bancada.filter.PlanFilter;
import com.bancada.models.Device;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plans for sale. A plan is available while its device still has memory for one more server:
 * machines already there plus orders waiting to be created, against 90% of the device memory.
 */
@Service
public class PlanService {

    private static final String ENTITY = "Plan";
    private static final double MEMORY_SHARE_FOR_SERVERS = 0.9;
    private static final long BYTES_PER_MB = 1024L * 1024L;
    private static final List<SubscriptionStatus> WAITING_FOR_MACHINE = List.of(SubscriptionStatus.PENDING_PAYMENT,
        SubscriptionStatus.PROVISIONING);

    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MachineRepository machineRepository;
    private final DeviceService deviceService;
    private final AuditService auditService;

    public PlanService(PlanRepository planRepository, SubscriptionRepository subscriptionRepository, MachineRepository machineRepository,
                       DeviceService deviceService, AuditService auditService) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.machineRepository = machineRepository;
        this.deviceService = deviceService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<Plan> search(PlanFilter filter, Pageable pageable) {
        Specification<Plan> specification = Specification.where(PlanSpecification.search(filter.getSearch()))
            .and(PlanSpecification.active(filter.getActive()))
            .and(PlanSpecification.device(filter.getDeviceId()));
        return planRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public Plan findById(Long id) {
        return planRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Plano não encontrado para ID: " + id));
    }

    @Transactional
    public Plan create(PlanRequest request) {
        Device device = deviceService.findById(request.deviceId());
        Plan plan = planRepository.save(new Plan(request, device));
        auditService.record(AuditAction.CREATE, ENTITY, plan.getId(), null, new PlanSnapshot(plan), null);
        return plan;
    }

    /** A new price is for new contracts: subscriptions keep the price they were sold with. */
    @Transactional
    public Plan update(Long id, PlanRequest request) {
        Plan plan = findById(id);
        PlanSnapshot before = new PlanSnapshot(plan);
        plan.update(request, deviceService.findById(request.deviceId()));
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
        String architecture = plan.getDevice().getArchitecture();
        return Arrays.stream(MachineDistribution.values())
            .filter(distribution -> distribution.supports(architecture))
            .map(distribution -> new DistributionResponse(distribution, distribution.getDisplayName(), distribution.getVersions(), true))
            .toList();
    }

    public boolean isAvailable(Plan plan) {
        Device device = plan.getDevice();
        if (!device.isActive()) {
            return false;
        }
        if (device.getMemoryTotalBytes() == null) {
            return true;
        }
        long committedMb = 0;
        for (Machine machine : machineRepository.findByDeviceIdAndStatusNot(device.getId(), MachineStatus.REMOVED)) {
            committedMb += machine.getMemoryLimitMb() == null ? 0 : machine.getMemoryLimitMb();
        }
        for (Subscription subscription : subscriptionRepository.findByPlanDeviceIdAndStatusIn(device.getId(), WAITING_FOR_MACHINE)) {
            if (subscription.getMachine() == null) {
                committedMb += subscription.getPlan().getMemoryMb();
            }
        }
        long capacityMb = (long) (device.getMemoryTotalBytes() / BYTES_PER_MB * MEMORY_SHARE_FOR_SERVERS);
        return committedMb + plan.getMemoryMb() <= capacityMb;
    }
}
