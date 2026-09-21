package com.bancada.repository;

import com.bancada.models.Plan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long>, JpaSpecificationExecutor<Plan> {

    List<Plan> findByActiveTrueOrderByOrderNumberAscPriceMonthlyAsc();
}
