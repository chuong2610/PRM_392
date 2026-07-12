package com.wayflo.runner;

import com.wayflo.service.FloorplanImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
@RequiredArgsConstructor
public class MapSeedRunner implements ApplicationRunner {

    private final FloorplanImportService floorplanImportService;

    @Value("${wayflo.seed.enabled:false}")
    private boolean enabled;

    @Value("${wayflo.seed.location-pattern}")
    private String locationPattern;

    @Override
    public void run(ApplicationArguments args) {
        if (enabled) {
            floorplanImportService.importSeedResources(locationPattern);
        }
    }
}
