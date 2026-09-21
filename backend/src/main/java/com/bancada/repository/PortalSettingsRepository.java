package com.bancada.repository;

import com.bancada.models.PortalSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortalSettingsRepository extends JpaRepository<PortalSettings, Long> {
}
