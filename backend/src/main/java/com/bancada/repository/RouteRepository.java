package com.bancada.repository;

import com.bancada.enums.RouteStatus;
import com.bancada.enums.RouteType;
import com.bancada.models.Route;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteRepository extends JpaRepository<Route, Long>, JpaSpecificationExecutor<Route> {

    boolean existsByTypeAndHostnameAndStatusNotAndIdNot(RouteType type, String hostname, RouteStatus status, Long id);

    boolean existsByTypeAndPublicPortAndStatusNotAndIdNot(RouteType type, Integer publicPort, RouteStatus status, Long id);

    List<Route> findByStatus(RouteStatus status);

    List<Route> findByStatusNot(RouteStatus status);

    List<Route> findByMachineIdAndStatusIn(Long machineId, Collection<RouteStatus> statuses);
}
