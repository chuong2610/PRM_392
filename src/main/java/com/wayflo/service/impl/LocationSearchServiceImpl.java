package com.wayflo.service.impl;

import com.wayflo.dto.SearchResponse;
import com.wayflo.dto.SearchResultResponse;
import com.wayflo.dto.SearchResultType;
import com.wayflo.dto.SearchTargetType;
import com.wayflo.entity.Building;
import com.wayflo.entity.MapVersion;
import com.wayflo.entity.MapVersionStatus;
import com.wayflo.entity.Poi;
import com.wayflo.entity.PoiStatus;
import com.wayflo.entity.Space;
import com.wayflo.exception.BusinessException;
import com.wayflo.exception.ErrorCode;
import com.wayflo.mapper.MapMapper;
import com.wayflo.repository.BuildingRepository;
import com.wayflo.repository.MapVersionRepository;
import com.wayflo.repository.PoiRepository;
import com.wayflo.repository.SpaceRepository;
import com.wayflo.service.LocationSearchService;
import com.wayflo.util.TextNormalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocationSearchServiceImpl implements LocationSearchService {

    private final BuildingRepository buildingRepository;
    private final MapVersionRepository mapVersionRepository;
    private final PoiRepository poiRepository;
    private final SpaceRepository spaceRepository;
    private final TextNormalizer textNormalizer;
    private final MapMapper mapMapper;

    @Override
    @Transactional(readOnly = true)
    public SearchResponse search(UUID buildingId, String query, UUID floorId, SearchTargetType type, int limit) {
        MapVersion mapVersion = getPublishedMap(buildingId).mapVersion();
        String normalizedQuery = textNormalizer.normalize(query);
        SearchTargetType targetType = type == null ? SearchTargetType.ALL : type;
        int safeLimit = Math.max(1, Math.min(limit, 50));

        List<SearchResultResponse> results = deduplicateByDisplayLocation(
            buildResults(mapVersion, normalizedQuery, floorId, targetType)
        ).stream()
            .sorted(Comparator.comparing(SearchResultResponse::score).reversed())
            .limit(safeLimit)
            .toList();

        return new SearchResponse(query, results);
    }

    private List<SearchResultResponse> buildResults(
        MapVersion mapVersion,
        String normalizedQuery,
        UUID floorId,
        SearchTargetType targetType
    ) {
        List<SearchResultResponse> poiResults = targetType == SearchTargetType.SPACE
            ? List.of()
            : poiRepository.findByMapVersionId(mapVersion.getId()).stream()
                .filter(poi -> floorId == null || poi.getFloor().getId().equals(floorId))
                .filter(poi -> poi.getStatus() != PoiStatus.HIDDEN)
                .map(poi -> toPoiResult(poi, normalizedQuery))
                .filter(result -> result.score() > 0)
                .toList();

        List<SearchResultResponse> spaceResults = targetType == SearchTargetType.POI
            ? List.of()
            : spaceRepository.findByMapVersionId(mapVersion.getId()).stream()
                .filter(space -> floorId == null || space.getFloor().getId().equals(floorId))
                .map(space -> toSpaceResult(space, normalizedQuery))
                .filter(result -> result.score() > 0)
                .toList();

        return java.util.stream.Stream.concat(poiResults.stream(), spaceResults.stream()).toList();
    }

    private List<SearchResultResponse> deduplicateByDisplayLocation(List<SearchResultResponse> results) {
        Map<String, SearchResultResponse> bestByLocation = new HashMap<>();
        for (SearchResultResponse result : results) {
            String key = result.floorId() + ":" + textNormalizer.normalize(result.name());
            SearchResultResponse existing = bestByLocation.get(key);
            if (existing == null
                || result.score() > existing.score()
                || (result.score() == existing.score()
                    && existing.type() == SearchResultType.SPACE
                    && result.type() == SearchResultType.POI)) {
                bestByLocation.put(key, result);
            }
        }
        return new ArrayList<>(bestByLocation.values());
    }

    private SearchResultResponse toPoiResult(Poi poi, String normalizedQuery) {
        double score = score(poi.getNormalizedName(), poi.getCategory(), poi.getNormalizedAliases(), normalizedQuery);
        return new SearchResultResponse(
            poi.getId(),
            poi.getExternalId(),
            poi.getName(),
            SearchResultType.POI,
            poi.getCategory(),
            poi.getFloor().getId(),
            poi.getFloor().getName(),
            score,
            mapMapper.pointToList(poi.getDisplayAnchor())
        );
    }

    private SearchResultResponse toSpaceResult(Space space, String normalizedQuery) {
        double score = score(space.getNormalizedName(), space.getSpaceType().name(), List.of(), normalizedQuery);
        return new SearchResultResponse(
            space.getId(),
            space.getExternalId(),
            space.getName(),
            SearchResultType.SPACE,
            space.getSpaceType().name(),
            space.getFloor().getId(),
            space.getFloor().getName(),
            score,
            mapMapper.pointToList(space.getCentroid())
        );
    }

    private double score(String normalizedName, String category, List<String> aliases, String normalizedQuery) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            return 0;
        }
        String normalizedCategory = textNormalizer.normalize(category);
        if (normalizedName.equals(normalizedQuery)) {
            return 100;
        }
        if (normalizedName.startsWith(normalizedQuery)) {
            return 60;
        }
        if (aliases.stream().anyMatch(alias -> alias.equals(normalizedQuery))) {
            return 50;
        }
        if (aliases.stream().anyMatch(alias -> alias.contains(normalizedQuery))) {
            return 40;
        }
        if (normalizedName.contains(normalizedQuery)) {
            return 35;
        }
        if (normalizedCategory.contains(normalizedQuery)) {
            return 20;
        }
        return tokenOverlap(normalizedName + " " + normalizedCategory + " " + String.join(" ", aliases), normalizedQuery);
    }

    private double tokenOverlap(String haystack, String needle) {
        List<String> haystackTokens = java.util.Arrays.stream(haystack.split("\\s+"))
            .filter(token -> !token.isBlank())
            .toList();
        String[] tokens = needle.split(" ");
        int matched = 0;
        int required = 0;
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            required++;
            if (haystackTokens.contains(token)) {
                matched++;
            }
        }
        if (required > 1 && matched < required) {
            return 0;
        }
        return matched == 0 || required == 0 ? 0 : 10.0 * matched / required;
    }

    private PublishedMap getPublishedMap(UUID buildingId) {
        Building building = buildingRepository.findById(buildingId)
            .orElseThrow(() -> new BusinessException(ErrorCode.BUILDING_NOT_FOUND));

        MapVersion mapVersion = null;
        if (building.getPublishedMapVersionId() != null) {
            mapVersion = mapVersionRepository.findById(building.getPublishedMapVersionId()).orElse(null);
        }
        if (mapVersion == null) {
            mapVersion = mapVersionRepository
                .findFirstByBuildingIdAndStatusOrderByVersionNumberDesc(buildingId, MapVersionStatus.PUBLISHED)
                .orElseThrow(() -> new BusinessException(ErrorCode.PUBLISHED_MAP_NOT_FOUND));
        }
        return new PublishedMap(building, mapVersion);
    }

    private record PublishedMap(Building building, MapVersion mapVersion) {
    }
}
