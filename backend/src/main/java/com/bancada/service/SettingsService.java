package com.bancada.service;

import com.bancada.models.AppSettings;
import com.bancada.repository.AppSettingsRepository;
import com.bancada.request.GatewaySettingsRequest;
import com.bancada.request.SettingsRequest;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    private final AppSettingsRepository appSettingsRepository;
    private final String defaultBackupDirectory;

    public SettingsService(AppSettingsRepository appSettingsRepository, @Value("${bancada.data-dir}") String dataDir) {
        this.appSettingsRepository = appSettingsRepository;
        this.defaultBackupDirectory = Paths.get(dataDir, "backups").toString();
    }

    @Transactional
    public AppSettings get() {
        return appSettingsRepository.findById(AppSettings.SINGLETON_ID)
            .orElseGet(() -> appSettingsRepository.save(new AppSettings(defaultBackupDirectory)));
    }

    @Transactional
    public AppSettings updateGateway(GatewaySettingsRequest request) {
        AppSettings settings = get();
        settings.updateGateway(request);
        return appSettingsRepository.save(settings);
    }

    @Transactional
    public AppSettings update(SettingsRequest request) {
        AppSettings settings = get();
        settings.update(request);
        return appSettingsRepository.save(settings);
    }
}
