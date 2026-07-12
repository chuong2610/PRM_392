package com.wayflo.service;

import com.wayflo.dto.FloorplanSeedDto;
import com.wayflo.dto.ImportResultResponse;
import java.util.List;

public interface FloorplanImportService {

    ImportResultResponse importSeed(FloorplanSeedDto seed);

    ImportResultResponse importSeeds(List<FloorplanSeedDto> seeds);

    ImportResultResponse importSeedResources(String locationPattern);
}
