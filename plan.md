# WAYFLO BACKEND IMPLEMENTATION PLAN
## Spring Boot Backend for 3D Indoor Map Rendering and Indoor Routing

---

## 1. Mục tiêu tài liệu

Tài liệu này mô tả kế hoạch triển khai backend cho hệ thống WayFlo bằng Spring Boot.

Phạm vi chỉ tập trung vào các chức năng:

- Backend đọc dữ liệu bản đồ đã được seed sẵn theo định dạng FloorplanVLM.
- Chuẩn hóa dữ liệu FloorplanVLM thành mô hình dữ liệu nội bộ.
- Lưu trữ dữ liệu không gian của tòa nhà.
- Trả dữ liệu bản đồ để frontend dựng mô hình 3D.
- Tìm kiếm phòng, cửa hàng và POI.
- Tìm đường giữa hai vị trí được người dùng lựa chọn.
- Tìm đường giữa nhiều tầng.
- Hỗ trợ tuyến đường tránh cầu thang.
- Hỗ trợ khu vực bị chặn tạm thời.
- Trả polyline và hướng dẫn từng bước cho frontend.

Hệ thống không triển khai định vị người dùng.

Frontend hoặc kiosk phải truyền điểm bắt đầu vào request tìm đường.

Ví dụ:

- Kiosk cố định.
- Cửa ra vào.
- POI.
- Phòng.
- Điểm được người dùng chọn trên bản đồ.
- Navigation node đã biết trước.

---

# 2. Phạm vi hệ thống

## 2.1 Chức năng được triển khai

### Map Import

- Đọc JSON đã được tạo bởi FloorplanVLM.
- Validate JSON.
- Chuẩn hóa hệ tọa độ.
- Lưu dữ liệu tường, cửa, phòng và khu vực.
- Kết hợp dữ liệu geometry với metadata nghiệp vụ.
- Tạo phiên bản bản đồ.
- Sinh navigation graph.

### 3D Map API

- Trả thông tin tòa nhà.
- Trả danh sách tầng.
- Trả bounds của bản đồ.
- Trả wall segments.
- Trả wall thickness và wall height.
- Trả doors và openings.
- Trả polygon của room/space.
- Trả POI.
- Trả elevator, escalator và stair.
- Trả coordinate convention cho frontend.

### Search

- Tìm phòng theo tên.
- Tìm cửa hàng theo tên.
- Tìm theo alias.
- Tìm theo category.
- Tìm gần đúng khi người dùng nhập sai chính tả.
- Lọc kết quả theo tầng.

### Indoor Routing

- Route cùng tầng.
- Route khác tầng.
- Route từ kiosk đến POI.
- Route từ POI đến POI.
- Route từ map point đến POI.
- Tránh cầu thang.
- Chỉ sử dụng tuyến accessible.
- Tránh node hoặc edge đang bị block.
- Kiểm tra destination đang đóng.
- Trả route polyline.
- Trả step-by-step instructions.
- Tính khoảng cách và thời gian dự kiến.

---

## 2.2 Chức năng không triển khai

- Không gọi trực tiếp FloorplanVLM.
- Không xử lý upload PDF hoặc PNG.
- Không xử lý OCR.
- Không có trình chỉnh sửa bản đồ.
- Không có localization bằng GPS.
- Không có localization bằng BLE.
- Không có localization bằng Wi-Fi.
- Không có camera positioning.
- Không có indoor positioning tự động.
- Không đồng bộ Google Maps.
- Không xử lý analytics.
- Không xử lý heatmap.
- Không xử lý subscription.
- Không xử lý payment.
- Không xử lý tenant management đầy đủ.
- Không xử lý AI Business Insights.

---

# 3. Giả định dữ liệu đầu vào

## 3.1 Dữ liệu từ FloorplanVLM

FloorplanVLM sinh ra dữ liệu geometry cơ bản.

Ví dụ:

```json
{
  "walls": [
    {
      "id": "wall-001",
      "start": [100, 200],
      "end": [500, 200],
      "thickness": 12,
      "curvature": 0,
      "openings": [
        {
          "id": "door-001",
          "type": "door",
          "center": 0.4,
          "width": 80
        }
      ]
    }
  ],
  "rooms": [
    {
      "id": "room-001",
      "label": "corridor",
      "walls": [
        "wall-001",
        "wall-002",
        "wall-003",
        "wall-004"
      ]
    }
  ]
}
```

Dữ liệu này chưa đủ để tìm đường.

Backend vẫn cần metadata bổ sung như:

- Tên phòng.
- Loại phòng.
- Không gian có thể đi bộ hay không.
- Cửa chính của cửa hàng.
- POI.
- Alias tìm kiếm.
- Số tầng.
- Cao độ tầng.
- Tỉ lệ pixel sang mét.
- Elevator.
- Stair.
- Escalator.
- Accessibility.
- Kiosk.
- Trạng thái hoạt động.

---

## 3.2 Seed manifest đề xuất

Mỗi tầng nên sử dụng một seed manifest bao quanh dữ liệu FloorplanVLM.

```json
{
  "schemaVersion": "1.0",
  "building": {
    "externalId": "mall-hcm-01",
    "name": "WayFlo Mall"
  },
  "floor": {
    "externalId": "floor-l1",
    "name": "Ground Floor",
    "floorNumber": 1,
    "elevation": 0,
    "defaultCeilingHeight": 4.5,
    "coordinateUnit": "pixel",
    "metersPerUnit": 0.01
  },
  "floorplanVlm": {
    "walls": [],
    "rooms": []
  },
  "spaceMetadata": [
    {
      "roomId": "room-001",
      "name": "Main Corridor",
      "spaceType": "CORRIDOR",
      "walkable": true,
      "publicAccess": true,
      "status": "OPEN"
    }
  ],
  "pois": [
    {
      "externalId": "poi-store-a",
      "name": "Store A",
      "category": "FASHION_STORE",
      "roomId": "room-store-a",
      "entranceDoorId": "door-store-a",
      "searchAliases": [
        "store a",
        "fashion store a"
      ]
    }
  ],
  "connectors": [
    {
      "externalId": "elevator-a-l1",
      "groupId": "elevator-a",
      "type": "ELEVATOR",
      "roomId": "room-elevator-a",
      "servedFloors": [1, 2, 3],
      "accessible": true
    }
  ],
  "kiosks": [
    {
      "externalId": "kiosk-l1-01",
      "name": "Main Entrance Kiosk",
      "position": [250, 420]
    }
  ]
}
```

---

# 4. Kiến trúc tổng thể

```text
Seed JSON
   │
   ▼
Spring Boot Map Import Module
   │
   ├── JSON Schema Validation
   ├── FloorplanVLM Adapter
   ├── Coordinate Normalization
   ├── Geometry Validation
   ├── Metadata Enrichment
   └── Map Version Creation
   │
   ▼
PostgreSQL + PostGIS
   │
   ├── Buildings
   ├── Floors
   ├── Walls
   ├── Openings
   ├── Spaces
   ├── POIs
   ├── Connectors
   ├── Navigation Nodes
   └── Navigation Edges
   │
   ▼
Navigation Graph Builder
   │
   ├── Walkable Polygon Builder
   ├── Door-to-Corridor Linker
   ├── POI Anchor Builder
   ├── Vertical Connector Builder
   └── Graph Validator
   │
   ├─────────────────────────────┐
   ▼                             ▼
Map Query Service           Routing Service
   │                             │
   ▼                             ▼
3D Map REST API             Search/Route REST API
   │                             │
   └──────────────┬──────────────┘
                  ▼
          Web / Kiosk Frontend
```

---

# 5. Công nghệ đề xuất

## Backend

- Java 21.
- Spring Boot 3.
- Spring Web.
- Spring Data JPA.
- Hibernate Spatial.
- Bean Validation.
- Jackson.
- Flyway.
- Spring Cache.
- Spring Data Redis.
- Spring Boot Actuator.
- Springdoc OpenAPI.
- MapStruct.
- Lombok.

## Database

- PostgreSQL.
- PostGIS.
- pg_trgm.
- Full Text Search.

## Cache

- Redis.

## Testing

- JUnit 5.
- Mockito.
- AssertJ.
- Spring Boot Test.
- Testcontainers PostgreSQL/PostGIS.
- REST Assured hoặc MockMvc.

## Build

- Gradle Kotlin DSL hoặc Maven.

Khuyến nghị dùng Gradle Kotlin DSL cho dự án mới.

---

# 6. Dependency đề xuất

Ví dụ Gradle:

```kotlin
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation("org.hibernate.orm:hibernate-spatial")
    implementation("org.locationtech.jts:jts-core")

    implementation("org.mapstruct:mapstruct")
    annotationProcessor("org.mapstruct:mapstruct-processor")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}
```

---

# 7. Kiến trúc source code

Khuyến nghị sử dụng kiến trúc module theo business domain.

```text
src/main/java/com/wayflo/
├── WayFloApplication.java
│
├── common/
│   ├── config/
│   │   ├── CacheConfig.java
│   │   ├── JacksonConfig.java
│   │   ├── OpenApiConfig.java
│   │   └── PostGisConfig.java
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   ├── BusinessException.java
│   │   └── ErrorCode.java
│   ├── geometry/
│   │   ├── GeometryUtils.java
│   │   ├── CoordinateTransformer.java
│   │   ├── PolygonBuilder.java
│   │   └── GeometryValidator.java
│   ├── response/
│   └── util/
│
├── building/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── mapper/
│
├── floor/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── mapversion/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── mapimport/
│   ├── controller/
│   ├── service/
│   │   ├── FloorplanImportService.java
│   │   ├── FloorplanAdapterService.java
│   │   ├── FloorplanValidationService.java
│   │   ├── GeometryProcessingService.java
│   │   └── MetadataEnrichmentService.java
│   ├── parser/
│   ├── schema/
│   ├── dto/
│   │   ├── FloorplanSeedDto.java
│   │   ├── FloorplanVlmDto.java
│   │   ├── WallSeedDto.java
│   │   ├── RoomSeedDto.java
│   │   └── PoiSeedDto.java
│   └── exception/
│
├── mapquery/
│   ├── controller/
│   ├── service/
│   ├── dto/
│   └── mapper/
│
├── space/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── poi/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── search/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   └── dto/
│
├── navigation/
│   ├── graph/
│   │   ├── NavigationGraphBuilder.java
│   │   ├── NavigationGraphLoader.java
│   │   ├── NavigationGraphValidator.java
│   │   └── NavigationGraphCache.java
│   ├── pathfinding/
│   │   ├── AStarPathFinder.java
│   │   ├── CostCalculator.java
│   │   └── RouteOptionFilter.java
│   ├── instruction/
│   │   ├── RouteInstructionBuilder.java
│   │   ├── TurnAngleCalculator.java
│   │   └── RouteInstructionFormatter.java
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── connector/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
├── kiosk/
│   ├── repository/
│   ├── entity/
│   └── dto/
│
└── blocking/
    ├── controller/
    ├── service/
    ├── repository/
    ├── entity/
    └── dto/
```

---

# 8. Mô hình dữ liệu

## 8.1 Building

```java
@Entity
@Table(name = "buildings")
public class Building {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String externalId;

    @Column(nullable = false)
    private String name;

    private String address;

    @Enumerated(EnumType.STRING)
    private BuildingStatus status;

    private UUID publishedMapVersionId;

    private Instant createdAt;

    private Instant updatedAt;
}
```

---

## 8.2 Floor

```java
@Entity
@Table(
    name = "floors",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_floor_building_external_id",
            columnNames = {"building_id", "external_id"}
        )
    }
)
public class Floor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Building building;

    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer floorNumber;

    @Column(nullable = false)
    private Double elevation;

    @Column(nullable = false)
    private Double defaultCeilingHeight;

    @Column(nullable = false)
    private Double metersPerUnit;

    @Column(columnDefinition = "geometry(Polygon, 0)")
    private Polygon boundsGeometry;
}
```

---

## 8.3 MapVersion

```java
@Entity
@Table(name = "map_versions")
public class MapVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Building building;

    @Column(nullable = false)
    private Integer versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MapVersionStatus status;

    @Column(nullable = false)
    private String schemaVersion;

    @Enumerated(EnumType.STRING)
    private MapSourceType sourceType;

    private Instant createdAt;

    private Instant publishedAt;
}
```

Status:

```java
public enum MapVersionStatus {
    PROCESSING,
    READY,
    FAILED,
    PUBLISHED,
    ARCHIVED
}
```

---

## 8.4 FloorplanSource

Lưu JSON gốc để debug và import lại.

```java
@Entity
@Table(name = "floorplan_sources")
public class FloorplanSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private MapVersion mapVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Floor floor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private JsonNode rawJson;

    @Column(nullable = false)
    private String checksum;

    @Enumerated(EnumType.STRING)
    private ImportStatus importStatus;

    @Column(columnDefinition = "text")
    private String importError;
}
```

---

## 8.5 Wall

```java
@Entity
@Table(name = "walls")
public class Wall {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private MapVersion mapVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Floor floor;

    @Column(nullable = false)
    private String externalId;

    @Column(columnDefinition = "geometry(LineString, 0)", nullable = false)
    private LineString geometry;

    @Column(nullable = false)
    private Double thickness;

    @Column(nullable = false)
    private Double height;

    private Double curvature;
}
```

---

## 8.6 Opening

```java
@Entity
@Table(name = "openings")
public class Opening {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Wall wall;

    @Column(nullable = false)
    private String externalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpeningType openingType;

    private Double centerRatio;

    private Double width;

    private Double height;

    @Column(columnDefinition = "geometry(LineString, 0)")
    private LineString geometry;

    private UUID connectedSpaceFromId;

    private UUID connectedSpaceToId;
}
```

---

## 8.7 Space

```java
@Entity
@Table(name = "spaces")
public class Space {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private MapVersion mapVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Floor floor;

    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String normalizedName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceType spaceType;

    @Column(columnDefinition = "geometry(Polygon, 0)", nullable = false)
    private Polygon geometry;

    @Column(columnDefinition = "geometry(Point, 0)", nullable = false)
    private Point centroid;

    private Double ceilingHeight;

    @Column(nullable = false)
    private Boolean walkable;

    @Column(nullable = false)
    private Boolean publicAccess;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpaceStatus status;
}
```

---

## 8.8 POI

```java
@Entity
@Table(name = "pois")
public class Poi {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private MapVersion mapVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Building building;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Floor floor;

    @ManyToOne(fetch = FetchType.LAZY)
    private Space space;

    @Column(nullable = false)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String normalizedName;

    @Enumerated(EnumType.STRING)
    private PoiCategory category;

    @Column(columnDefinition = "geometry(Point, 0)")
    private Point displayAnchor;

    @Column(columnDefinition = "geometry(Point, 0)")
    private Point routingAnchor;

    private UUID entranceOpeningId;

    @Enumerated(EnumType.STRING)
    private PoiStatus status;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] searchAliases;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode metadata;
}
```

---

## 8.9 VerticalConnector

```java
@Entity
@Table(name = "vertical_connectors")
public class VerticalConnector {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Building building;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private MapVersion mapVersion;

    @Column(nullable = false)
    private String externalGroupId;

    private String name;

    @Enumerated(EnumType.STRING)
    private ConnectorType connectorType;

    private Boolean accessible;

    @Enumerated(EnumType.STRING)
    private ConnectorStatus status;
}
```

---

## 8.10 ConnectorStop

```java
@Entity
@Table(name = "connector_stops")
public class ConnectorStop {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private VerticalConnector connector;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Floor floor;

    @ManyToOne(fetch = FetchType.LAZY)
    private Space space;

    private UUID navigationNodeId;

    @Column(columnDefinition = "geometry(Point, 0)")
    private Point position;
}
```

---

## 8.11 NavigationNode

```java
@Entity
@Table(name = "navigation_nodes")
public class NavigationNode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private MapVersion mapVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Floor floor;

    @Enumerated(EnumType.STRING)
    private NavigationNodeType nodeType;

    @Column(columnDefinition = "geometry(Point, 0)", nullable = false)
    private Point geometry;

    private UUID spaceId;

    private UUID referenceId;

    private Boolean accessible;

    @Enumerated(EnumType.STRING)
    private NavigationStatus status;
}
```

Node types:

```java
public enum NavigationNodeType {
    CORRIDOR_POINT,
    JUNCTION,
    DOOR,
    POI_ANCHOR,
    CONNECTOR_STOP,
    KIOSK,
    MAP_POINT
}
```

---

## 8.12 NavigationEdge

```java
@Entity
@Table(name = "navigation_edges")
public class NavigationEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private MapVersion mapVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private NavigationNode fromNode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private NavigationNode toNode;

    @Enumerated(EnumType.STRING)
    private NavigationEdgeType edgeType;

    @Column(columnDefinition = "geometry(LineString, 0)", nullable = false)
    private LineString geometry;

    private Double distanceMeters;

    private Double baseCost;

    private Boolean bidirectional;

    private Boolean accessible;

    @Enumerated(EnumType.STRING)
    private NavigationStatus status;
}
```

Edge types:

```java
public enum NavigationEdgeType {
    WALK,
    ENTER_SPACE,
    EXIT_SPACE,
    ELEVATOR,
    STAIR,
    ESCALATOR,
    RAMP
}
```

---

## 8.13 RouteBlock

```java
@Entity
@Table(name = "route_blocks")
public class RouteBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Building building;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private MapVersion mapVersion;

    private UUID floorId;

    private UUID nodeId;

    private UUID edgeId;

    private String reason;

    private Instant startTime;

    private Instant endTime;

    @Enumerated(EnumType.STRING)
    private RouteBlockStatus status;
}
```

---

# 9. Import pipeline

## 9.1 Import flow

```text
Load seed JSON
    ↓
Validate schema
    ↓
Create map version PROCESSING
    ↓
Store raw JSON
    ↓
Parse FloorplanVLM geometry
    ↓
Normalize coordinates
    ↓
Create walls
    ↓
Create openings
    ↓
Build room polygons
    ↓
Merge business metadata
    ↓
Create POIs
    ↓
Create connectors
    ↓
Generate navigation graph
    ↓
Validate graph
    ↓
Set map version READY
```

Nếu bất kỳ bước nào lỗi:

```text
Rollback transaction
    ↓
Map version FAILED
    ↓
Store error report
```

---

## 9.2 Transaction strategy

Import một floorplan nên chạy trong transaction riêng.

```java
@Transactional
public MapVersionImportResult importFloorplan(FloorplanSeedDto seed) {
    // validate
    // persist raw source
    // persist geometry
    // build graph
    // validate result
}
```

Nếu import nhiều tầng cùng lúc, có hai lựa chọn:

### Cách 1: Một transaction cho toàn bộ building

Ưu điểm:

- Hoặc thành công toàn bộ.
- Hoặc rollback toàn bộ.

Nhược điểm:

- Transaction lớn.
- Khó xử lý building rất lớn.

### Cách 2: Transaction từng tầng và finalization transaction

Khuyến nghị cho production.

```text
Import floor 1
Import floor 2
Import floor 3
Build cross-floor connectors
Validate whole building
Mark map version READY
```

---

## 9.3 JSON validation

Dùng:

- Bean Validation cho DTO.
- JSON Schema validator nếu cần validate schema chặt.
- Business validation service cho quan hệ tham chiếu.

Ví dụ:

```java
public record WallSeedDto(
    @NotBlank String id,
    @NotNull CoordinateDto start,
    @NotNull CoordinateDto end,
    @Positive double thickness,
    List<OpeningSeedDto> openings
) {
}
```

Business validations:

- Wall ID không trùng.
- Room ID không trùng.
- Room chỉ tham chiếu wall tồn tại.
- POI chỉ tham chiếu room tồn tại.
- Door ID phải tồn tại.
- `metersPerUnit` phải lớn hơn 0.
- Connector floor phải tồn tại.
- `floorNumber` không trùng trong building.

---

# 10. Coordinate normalization

FloorplanVLM thường trả tọa độ theo pixel hoặc normalized coordinate.

Backend phải chuyển tất cả về mét.

```text
Input coordinate
      ↓
Multiply by metersPerUnit
      ↓
Canonical coordinate in meters
```

Ví dụ:

```text
Input: [500, 300]
metersPerUnit: 0.01

Canonical:
x = 5.0 meters
z = 3.0 meters
```

Coordinate convention đề xuất:

```text
Map 2D:
X = horizontal coordinate
Y = vertical coordinate in floor plan

3D frontend:
X = map X
Y = floor elevation
Z = map Y
```

Backend phải trả rõ convention:

```json
{
  "unit": "meter",
  "horizontalPlane": "XZ",
  "verticalAxis": "Y",
  "origin": [0, 0, 0]
}
```

---

# 11. Geometry processing

## 11.1 Build wall geometry

```java
Coordinate start = transformer.toMeters(seed.start());
Coordinate end = transformer.toMeters(seed.end());

LineString wallLine = geometryFactory.createLineString(
    new Coordinate[]{start, end}
);
```

---

## 11.2 Build opening geometry

FloorplanVLM có thể mô tả cửa bằng `centerRatio`.

```text
openingCenter =
wallStart + (wallEnd - wallStart) × centerRatio
```

Sau đó backend tính:

```text
halfWidth = openingWidth / 2
openingStart = point before center
openingEnd = point after center
```

Opening geometry phải nằm trên wall segment.

Nếu opening vượt quá wall:

```text
INVALID_OPENING_POSITION
```

---

## 11.3 Build room polygon

Quy trình:

1. Lấy danh sách wall theo room.
2. Lấy start và end của từng wall.
3. Snap endpoint gần nhau.
4. Tạo graph tạm từ endpoints.
5. Sắp xếp các wall thành closed ring.
6. Tạo `LinearRing`.
7. Tạo `Polygon`.
8. Validate polygon.
9. Tính centroid.
10. Tính bounds.

Pseudo-code:

```java
public Polygon buildRoomPolygon(
    RoomSeedDto room,
    Map<String, WallGeometry> walls
) {
    List<LineString> segments = resolveSegments(room.wallIds(), walls);

    List<Coordinate> orderedCoordinates =
        wallOrderingService.orderAsClosedRing(segments);

    LinearRing shell =
        geometryFactory.createLinearRing(
            orderedCoordinates.toArray(Coordinate[]::new)
        );

    Polygon polygon = geometryFactory.createPolygon(shell);

    geometryValidator.validatePolygon(polygon);

    return polygon;
}
```

---

## 11.4 Snap tolerance

FloorplanVLM có thể tạo endpoint lệch nhau một khoảng nhỏ.

Ví dụ:

```text
Wall A end:   [100.000, 200.000]
Wall B start: [100.004, 200.003]
```

Có thể snap nếu khoảng cách nhỏ hơn:

```text
SNAP_TOLERANCE = 0.02 meter
```

Không nên snap khi chênh lệch quá lớn.

---

## 11.5 Geometry validation

Kiểm tra:

- Polygon phải đóng.
- Polygon không self-intersection.
- Polygon có diện tích lớn hơn minimum.
- Opening nằm trên wall.
- Room không nằm ngoài floor bounds.
- Walkable areas không xuyên qua wall.
- Door phải kết nối được các space phù hợp.

Error codes:

```text
ROOM_POLYGON_NOT_CLOSED
ROOM_POLYGON_SELF_INTERSECTION
ROOM_AREA_TOO_SMALL
OPENING_OUTSIDE_WALL
WALL_ZERO_LENGTH
INVALID_SCALE
SPACE_OUTSIDE_FLOOR_BOUNDS
```

---

# 12. Navigation graph generation

## 12.1 Walkable area

Chỉ các space có:

```text
walkable = true
publicAccess = true
status = OPEN
```

được dùng để sinh graph.

Ví dụ:

- Corridor.
- Lobby.
- Public elevator area.
- Public stair area.
- Public escalator area.
- Ramp.

Không tạo graph đi xuyên qua:

- Store interior.
- Warehouse.
- Staff-only area.
- Electrical room.
- Locked room.
- Restricted area.

---

## 12.2 MVP graph strategy

Giai đoạn đầu nên sử dụng grid graph.

Flow:

```text
Walkable polygon
    ↓
Generate fixed-size grid
    ↓
Keep points inside polygon
    ↓
Connect adjacent points
    ↓
Remove edges crossing walls
    ↓
Attach doors and POIs
    ↓
Simplify graph
```

Ví dụ grid spacing:

```text
0.5 meter hoặc 1 meter
```

Ưu điểm:

- Dễ triển khai.
- Dễ debug.
- A* hoạt động ổn định.
- Phù hợp capstone hoặc MVP.

Nhược điểm:

- Graph lớn.
- Route có thể zigzag.
- Cần smoothing.

---

## 12.3 Production graph strategy

Có thể nâng cấp sang:

- Navigation mesh.
- Medial axis.
- Corridor centerline graph.
- Triangulation + funnel algorithm.

Ưu điểm:

- Graph ít node.
- Đường đẹp hơn.
- Route tự nhiên hơn.
- Performance tốt hơn cho building lớn.

---

## 12.4 Door connection

Cửa hàng không được nối trực tiếp từ centroid.

Flow đúng:

```text
Store polygon
    ↓
Store entrance door
    ↓
Door navigation node
    ↓
Nearest valid corridor node
```

Nếu route tới centroid, polyline có thể xuyên tường.

POI phải có:

- `displayAnchor`: vị trí icon/label.
- `routingAnchor`: vị trí dùng cho navigation.

---

## 12.5 Multi-floor graph

Mỗi connector vật lý có một `groupId`.

Ví dụ:

```text
Elevator A
├── stop floor 1
├── stop floor 2
└── stop floor 3
```

Graph:

```text
Floor 1 elevator node
          │
          │ ELEVATOR edge
          ▼
Floor 2 elevator node
          │
          │ ELEVATOR edge
          ▼
Floor 3 elevator node
```

Mỗi vertical edge cần:

- From floor.
- To floor.
- Connector type.
- Accessibility.
- Direction.
- Base cost.
- Estimated travel time.

---

# 13. Routing algorithm

## 13.1 A*

Khuyến nghị dùng A*.

```text
f(node) = g(node) + h(node)
```

Trong đó:

- `g(node)` là chi phí từ origin đến node.
- `h(node)` là heuristic tới destination.

Heuristic:

```text
Euclidean distance
+
floor difference penalty
```

---

## 13.2 Edge cost

Công thức cơ bản:

```text
cost = distanceMeters
```

Công thức tốt hơn:

```text
cost =
walkingTime
+ connectorPenalty
+ accessibilityPenalty
+ blockingPenalty
```

Ví dụ:

```text
walkingTime = distance / walkingSpeed
```

Connector penalties:

```text
ELEVATOR: waitingTime + verticalTravelTime
STAIR: numberOfFloors × stairPenalty
ESCALATOR: directionPenalty + verticalTravelTime
RAMP: distance-based cost
```

---

## 13.3 Accessible route

Khi request:

```json
{
  "accessible": true
}
```

Backend loại bỏ:

- Stair edge.
- Non-accessible escalator.
- Narrow corridor edge.
- Closed elevator.
- Edge có slope không phù hợp.
- Restricted edge.

Chỉ giữ:

- Elevator.
- Ramp.
- Accessible corridor.
- Accessible entrance.

---

## 13.4 Blocked route

Backend lấy các block đang active:

```sql
SELECT node_id, edge_id
FROM route_blocks
WHERE start_time <= now()
  AND end_time >= now()
  AND status = 'ACTIVE';
```

Sau đó loại node hoặc edge khỏi graph trước khi chạy A*.

---

## 13.5 Destination status

Trước khi route:

```text
Check POI status
Check space status
Check floor status
Check map version status
```

Nếu destination đóng:

```json
{
  "code": "DESTINATION_CLOSED",
  "message": "The selected destination is currently closed.",
  "routingAllowed": false
}
```

Có thể cấu hình:

```text
routeToClosedDestinationEntrance = true
```

Khi đó hệ thống vẫn route tới cửa nhưng thêm warning.

---

# 14. Route request model

```java
public record RouteRequest(
    @NotNull RouteEndpointDto origin,
    @NotNull RouteEndpointDto destination,
    RouteOptionsDto options
) {
}
```

```java
public record RouteEndpointDto(
    @NotNull RouteEndpointType type,
    UUID id,
    MapPointDto point,
    UUID floorId
) {
}
```

```java
public enum RouteEndpointType {
    POI,
    SPACE,
    KIOSK,
    NAVIGATION_NODE,
    MAP_POINT
}
```

```java
public record RouteOptionsDto(
    boolean accessible,
    boolean avoidStairs,
    boolean avoidEscalators
) {
}
```

---

# 15. Route service flow

```java
@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteEndpointResolver endpointResolver;
    private final NavigationGraphLoader graphLoader;
    private final RouteOptionFilter routeOptionFilter;
    private final AStarPathFinder pathFinder;
    private final RoutePathSimplifier pathSimplifier;
    private final RouteInstructionBuilder instructionBuilder;

    public RouteResponse findRoute(
        UUID buildingId,
        RouteRequest request
    ) {
        MapVersion mapVersion =
            loadPublishedMapVersion(buildingId);

        ResolvedRouteEndpoint origin =
            endpointResolver.resolve(
                mapVersion.getId(),
                request.origin()
            );

        ResolvedRouteEndpoint destination =
            endpointResolver.resolve(
                mapVersion.getId(),
                request.destination()
            );

        NavigationGraph graph =
            graphLoader.load(mapVersion.getId());

        NavigationGraph filteredGraph =
            routeOptionFilter.apply(
                graph,
                request.options()
            );

        GraphPath path =
            pathFinder.find(
                filteredGraph,
                origin.nodeId(),
                destination.nodeId()
            );

        RoutePath simplifiedPath =
            pathSimplifier.simplify(path);

        List<RouteInstructionDto> instructions =
            instructionBuilder.build(simplifiedPath);

        return RouteResponse.from(
            mapVersion,
            simplifiedPath,
            instructions
        );
    }
}
```

---

# 16. Step-by-step instruction generation

Input:

```text
Node 1 → Node 2 → Node 3 → ... → Destination
```

Quy trình:

1. Group edges theo floor.
2. Merge edge thẳng liên tiếp.
3. Tính vector mỗi segment.
4. Tính góc giữa hai vector.
5. Xác định hướng rẽ.
6. Phát hiện connector edge.
7. Tính khoảng cách.
8. Sinh instruction.
9. Sinh arrival instruction.

---

## 16.1 Turn angle

```text
angle = atan2(crossProduct, dotProduct)
```

Gợi ý rule:

```text
-20° đến 20°      → CONTINUE_STRAIGHT
20° đến 60°       → SLIGHT_RIGHT
60° đến 120°      → TURN_RIGHT
120° đến 180°     → SHARP_RIGHT

Giá trị âm tương ứng LEFT.
```

---

## 16.2 Instruction types

```java
public enum RouteInstructionType {
    START,
    CONTINUE_STRAIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    TURN_LEFT,
    TURN_RIGHT,
    SHARP_LEFT,
    SHARP_RIGHT,
    TAKE_ELEVATOR,
    TAKE_ESCALATOR,
    TAKE_STAIRS,
    USE_RAMP,
    ENTER_DESTINATION,
    ARRIVE
}
```

---

# 17. API design

## 17.1 Lấy map manifest

```http
GET /api/v1/buildings/{buildingId}/maps/current
```

Response:

```json
{
  "buildingId": "0cd7e629-42b3-4dc0-a33f-400db8464abc",
  "mapVersion": 4,
  "coordinateSystem": {
    "unit": "meter",
    "horizontalPlane": "XZ",
    "verticalAxis": "Y",
    "origin": [0, 0, 0]
  },
  "floors": [
    {
      "id": "floor-l1",
      "name": "Ground Floor",
      "floorNumber": 1,
      "elevation": 0,
      "ceilingHeight": 4.5,
      "dataUrl": "/api/v1/buildings/{buildingId}/maps/current/floors/{floorId}"
    }
  ]
}
```

---

## 17.2 Lấy dữ liệu một tầng

```http
GET /api/v1/buildings/{buildingId}/maps/current/floors/{floorId}
```

Response:

```json
{
  "floor": {
    "id": "floor-l1",
    "floorNumber": 1,
    "elevation": 0,
    "ceilingHeight": 4.5,
    "bounds": {
      "min": [0, 0],
      "max": [90, 65]
    }
  },
  "walls": [
    {
      "id": "wall-1",
      "start": [1.2, 3.4],
      "end": [8.5, 3.4],
      "thickness": 0.2,
      "height": 4.5,
      "curvature": 0,
      "openings": [
        {
          "id": "door-1",
          "type": "DOOR",
          "startOffset": 2.1,
          "width": 1.6,
          "height": 2.4
        }
      ]
    }
  ],
  "spaces": [
    {
      "id": "space-corridor-1",
      "name": "Main Corridor",
      "type": "CORRIDOR",
      "polygon": [
        [1, 1],
        [20, 1],
        [20, 6],
        [1, 6]
      ],
      "centroid": [10.5, 3.5],
      "walkable": true,
      "status": "OPEN"
    }
  ],
  "pois": [
    {
      "id": "poi-store-a",
      "name": "Store A",
      "category": "FASHION_STORE",
      "position": [18.2, 5.1],
      "spaceId": "space-store-a",
      "status": "OPEN"
    }
  ],
  "connectors": [
    {
      "id": "elevator-a",
      "type": "ELEVATOR",
      "position": [30.5, 12.5],
      "servedFloors": [1, 2, 3],
      "accessible": true
    }
  ]
}
```

---

## 17.3 Search

```http
GET /api/v1/buildings/{buildingId}/search?q=store&floorId={floorId}&type=POI
```

Response:

```json
{
  "query": "store",
  "total": 2,
  "items": [
    {
      "targetType": "POI",
      "targetId": "poi-store-a",
      "name": "Store A",
      "category": "FASHION_STORE",
      "floorId": "floor-l1",
      "floorNumber": 1,
      "position": [18.2, 5.1],
      "routingAnchor": {
        "nodeId": "node-door-store-a",
        "position": [17.8, 5.6]
      },
      "status": "OPEN"
    }
  ]
}
```

---

## 17.4 Route

```http
POST /api/v1/buildings/{buildingId}/routes
```

Request:

```json
{
  "origin": {
    "type": "KIOSK",
    "id": "f7438951-3898-41d3-98b4-f83a670e2d91"
  },
  "destination": {
    "type": "POI",
    "id": "8d18fb85-0b99-42bf-a573-2085d79cfe5b"
  },
  "options": {
    "accessible": false,
    "avoidStairs": false,
    "avoidEscalators": false
  }
}
```

Response:

```json
{
  "mapVersion": 4,
  "summary": {
    "distanceMeters": 146.8,
    "estimatedSeconds": 132,
    "floorTransitions": 1
  },
  "segments": [
    {
      "type": "WALKING",
      "floorId": "floor-l1",
      "floorNumber": 1,
      "distanceMeters": 62.4,
      "polyline": [
        [5.1, 0, 8.2],
        [12.4, 0, 8.2],
        [18.3, 0, 14.1]
      ]
    },
    {
      "type": "FLOOR_TRANSITION",
      "connector": {
        "id": "elevator-a",
        "type": "ELEVATOR",
        "fromFloor": 1,
        "toFloor": 2
      }
    },
    {
      "type": "WALKING",
      "floorId": "floor-l2",
      "floorNumber": 2,
      "distanceMeters": 84.4,
      "polyline": [
        [18.3, 5, 14.1],
        [30.2, 5, 21.6]
      ]
    }
  ],
  "instructions": [
    {
      "sequence": 1,
      "type": "START",
      "floorNumber": 1,
      "text": "Start from the main entrance kiosk."
    },
    {
      "sequence": 2,
      "type": "TURN_RIGHT",
      "floorNumber": 1,
      "distanceMeters": 20,
      "text": "Continue for 20 meters, then turn right."
    },
    {
      "sequence": 3,
      "type": "TAKE_ELEVATOR",
      "fromFloor": 1,
      "toFloor": 2,
      "text": "Take Elevator A to Floor 2."
    },
    {
      "sequence": 4,
      "type": "ARRIVE",
      "floorNumber": 2,
      "text": "Store A is on your left."
    }
  ],
  "warnings": []
}
```

---

# 18. Map point routing

Người dùng có thể tap vào bản đồ để chọn origin.

Do không có localization, điểm này chỉ là điểm được chọn thủ công.

Request:

```json
{
  "origin": {
    "type": "MAP_POINT",
    "floorId": "fbe6fb55-aab8-4783-abd3-7d87becc95c7",
    "point": {
      "x": 12.5,
      "z": 18.2
    }
  },
  "destination": {
    "type": "POI",
    "id": "8d18fb85-0b99-42bf-a573-2085d79cfe5b"
  }
}
```

Backend phải:

1. Kiểm tra point nằm trong floor bounds.
2. Kiểm tra point nằm trong walkable polygon.
3. Nếu không nằm trong walkable polygon, snap tới nearest walkable point.
4. Tìm nearest navigation node.
5. Tạo virtual node cho request.
6. Chạy route.

Không cần persist virtual node.

---

# 19. Search implementation

## 19.1 Normalize text

Ví dụ:

```text
"Nhà Vệ Sinh"
    ↓ lowercase
"nhà vệ sinh"
    ↓ remove accents
"nha ve sinh"
    ↓ trim spaces
"nha ve sinh"
```

---

## 19.2 Search ranking

```text
score =
exactNameMatch × 100
+ prefixMatch × 60
+ aliasMatch × 40
+ categoryMatch × 20
+ trigramSimilarity × 10
```

Thứ tự:

1. Exact name.
2. Prefix name.
3. Alias.
4. Category.
5. Fuzzy match.
6. Description.

---

## 19.3 Database indexes

```sql
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_poi_normalized_name_trgm
ON pois
USING gin (normalized_name gin_trgm_ops);

CREATE INDEX idx_space_normalized_name_trgm
ON spaces
USING gin (normalized_name gin_trgm_ops);

CREATE INDEX idx_poi_display_anchor
ON pois
USING gist (display_anchor);

CREATE INDEX idx_space_geometry
ON spaces
USING gist (geometry);

CREATE INDEX idx_navigation_node_geometry
ON navigation_nodes
USING gist (geometry);

CREATE INDEX idx_navigation_edge_geometry
ON navigation_edges
USING gist (geometry);
```

---

# 20. Cache strategy

## 20.1 Map cache

```text
map:{buildingId}:{mapVersion}:manifest
map:{buildingId}:{mapVersion}:floor:{floorId}
```

Map ít thay đổi nên có thể cache lâu.

Khi publish map version mới:

```text
Evict old manifest cache
Evict floor cache
Warm up new published version
```

---

## 20.2 Search cache

```text
search:{buildingId}:{mapVersion}:{queryHash}:{filterHash}
```

---

## 20.3 Navigation graph cache

Navigation graph không nên query toàn bộ database ở mỗi request.

Cache key:

```text
graph:{buildingId}:{mapVersion}
```

Có thể cache:

- In-memory bằng Caffeine.
- Redis.
- Hoặc kết hợp hai tầng cache.

Khuyến nghị:

```text
L1: Caffeine in application memory
L2: Redis
L3: PostgreSQL
```

---

# 21. Performance strategy

## Map API

- Lazy-load từng tầng.
- Không trả toàn bộ building map trong một response lớn.
- Enable gzip hoặc Brotli tại gateway.
- Sử dụng ETag.
- Sử dụng `Cache-Control`.
- Chỉ trả published map.
- Có thể pre-generate DTO JSON.

## Routing

- Load graph vào memory.
- Không query từng node/edge riêng lẻ.
- Dùng adjacency list.
- Dùng priority queue cho A*.
- Cache route phổ biến nếu cần.
- Tách graph theo map version.

## Search

- Dùng `pg_trgm`.
- Dùng pagination.
- Giới hạn kết quả mặc định.
- Normalize query trước khi search.

---

# 22. API controller skeleton

## MapController

```java
@RestController
@RequestMapping("/api/v1/buildings/{buildingId}/maps/current")
@RequiredArgsConstructor
public class MapController {

    private final MapQueryService mapQueryService;

    @GetMapping
    public MapManifestResponse getCurrentMap(
        @PathVariable UUID buildingId
    ) {
        return mapQueryService.getPublishedManifest(buildingId);
    }

    @GetMapping("/floors/{floorId}")
    public FloorMapResponse getFloorMap(
        @PathVariable UUID buildingId,
        @PathVariable UUID floorId
    ) {
        return mapQueryService.getPublishedFloorMap(
            buildingId,
            floorId
        );
    }
}
```

---

## SearchController

```java
@RestController
@RequestMapping("/api/v1/buildings/{buildingId}/search")
@RequiredArgsConstructor
public class SearchController {

    private final LocationSearchService locationSearchService;

    @GetMapping
    public SearchResponse search(
        @PathVariable UUID buildingId,
        @RequestParam String q,
        @RequestParam(required = false) UUID floorId,
        @RequestParam(required = false) SearchTargetType type,
        @RequestParam(defaultValue = "20") int limit
    ) {
        return locationSearchService.search(
            buildingId,
            q,
            floorId,
            type,
            limit
        );
    }
}
```

---

## RouteController

```java
@RestController
@RequestMapping("/api/v1/buildings/{buildingId}/routes")
@RequiredArgsConstructor
public class RouteController {

    private final RouteService routeService;

    @PostMapping
    public RouteResponse createRoute(
        @PathVariable UUID buildingId,
        @Valid @RequestBody RouteRequest request
    ) {
        return routeService.findRoute(buildingId, request);
    }
}
```

---

# 23. Error handling

## Error response

```json
{
  "timestamp": "2026-07-10T10:15:00Z",
  "status": 422,
  "code": "ROUTE_NOT_FOUND",
  "message": "No route is available between the selected locations.",
  "path": "/api/v1/buildings/.../routes",
  "details": []
}
```

---

## Import error codes

```text
INVALID_SCHEMA
DUPLICATED_WALL_ID
DUPLICATED_ROOM_ID
ROOM_REFERENCES_UNKNOWN_WALL
POI_REFERENCES_UNKNOWN_ROOM
OPENING_REFERENCES_UNKNOWN_WALL
ROOM_POLYGON_NOT_CLOSED
ROOM_POLYGON_SELF_INTERSECTION
INVALID_GEOMETRY
INVALID_SCALE
UNKNOWN_CONNECTOR_FLOOR
NAVIGATION_GRAPH_DISCONNECTED
POI_WITHOUT_ROUTING_ANCHOR
```

---

## Routing error codes

```text
PUBLISHED_MAP_NOT_FOUND
ORIGIN_NOT_FOUND
DESTINATION_NOT_FOUND
INVALID_MAP_POINT
MAP_POINT_NOT_WALKABLE
DESTINATION_CLOSED
ROUTE_NOT_FOUND
ACCESSIBLE_ROUTE_NOT_FOUND
CONNECTOR_UNAVAILABLE
FLOOR_UNAVAILABLE
```

---

# 24. Database migration plan

```text
V1__enable_extensions.sql
V2__create_building_tables.sql
V3__create_map_version_tables.sql
V4__create_geometry_tables.sql
V5__create_poi_tables.sql
V6__create_connector_tables.sql
V7__create_navigation_tables.sql
V8__create_route_block_tables.sql
V9__create_search_indexes.sql
V10__create_spatial_indexes.sql
```

---

# 25. Seed strategy

Folder structure:

```text
src/main/resources/seed/
└── buildings/
    └── mall-hcm-01/
        ├── building.json
        ├── floor-1.json
        ├── floor-2.json
        └── floor-3.json
```

Có thể tạo Spring profile:

```text
spring.profiles.active=local
wayflo.seed.enabled=true
```

Runner:

```java
@Component
@Profile("local")
@RequiredArgsConstructor
public class MapSeedRunner implements ApplicationRunner {

    private final FloorplanImportService importService;

    @Value("${wayflo.seed.enabled:false}")
    private boolean enabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        importService.importSeedDirectory(
            "classpath:seed/buildings"
        );
    }
}
```

Production không nên tự seed mỗi lần startup.

Production nên dùng:

- Admin import endpoint.
- CLI command.
- Migration job.
- Deployment job.

---

# 26. Logging và monitoring

## Structured logging

Log fields:

```text
requestId
buildingId
mapVersionId
floorId
originType
destinationType
routeDurationMs
routeNodeCount
graphLoadDurationMs
cacheHit
errorCode
```

## Metrics

- Map API latency.
- Search API latency.
- Route API latency.
- Route success rate.
- Route not found count.
- Import duration.
- Import failure count.
- Graph node count.
- Graph edge count.
- Cache hit ratio.

Dùng:

- Spring Boot Actuator.
- Micrometer.
- Prometheus.
- Grafana.

---

# 27. Testing plan

## 27.1 Unit test

### Floorplan adapter

- Parse wall.
- Parse opening.
- Parse room.
- Parse POI.
- Parse connector.
- Convert coordinate.
- Missing reference.
- Duplicate ID.

### Geometry

- Closed polygon.
- Open polygon.
- Self-intersection.
- Zero-length wall.
- Door position.
- Snap endpoint.
- Centroid.
- Bounds.

### Navigation graph

- Straight corridor.
- T-junction.
- Crossroad.
- Store entrance.
- Disconnected corridor.
- Multiple floors.
- Elevator connection.
- Stair connection.
- Accessible graph.

### Routing

- Same-floor route.
- Multi-floor route.
- Avoid stairs.
- Accessible route.
- Active blocked edge.
- Destination closed.
- Route not found.
- Start equals destination.

### Instructions

- Straight.
- Slight left.
- Turn left.
- Turn right.
- Elevator.
- Stair.
- Arrival.

---

## 27.2 Integration test

```text
Load seed JSON
    ↓
Import into Testcontainers PostGIS
    ↓
Build navigation graph
    ↓
Call map API
    ↓
Call search API
    ↓
Call route API
    ↓
Verify geometry and route
```

---

## 27.3 Acceptance criteria

Backend được xem là hoàn thành khi:

- Seed được ít nhất một building có ba tầng.
- Mỗi tầng import được wall, door và room.
- Frontend nhận đủ dữ liệu để dựng 3D.
- Search tìm được POI theo name.
- Search tìm được POI theo alias.
- Route cùng tầng hoạt động.
- Route khác tầng hoạt động.
- Route từ kiosk hoạt động.
- Accessible route không dùng stair.
- Route không đi qua restricted area.
- Blocked edge bị loại khỏi route.
- POI có routing anchor hợp lệ.
- Published map được cache.
- Draft map không được trả cho visitor.
- Route không xuyên tường.
- Import lỗi phải rollback.

---

# 28. Implementation roadmap

## Phase 1 — Project foundation

Công việc:

- Khởi tạo Spring Boot.
- Cấu hình PostgreSQL.
- Cấu hình PostGIS.
- Cấu hình Flyway.
- Cấu hình Redis.
- Cấu hình OpenAPI.
- Tạo global exception handler.
- Tạo base response.
- Tạo Docker Compose local.

Deliverable:

```text
Spring Boot application chạy được với PostgreSQL, PostGIS và Redis.
```

---

## Phase 2 — Canonical data model

Công việc:

- Tạo building entity.
- Tạo floor entity.
- Tạo map version.
- Tạo wall.
- Tạo opening.
- Tạo space.
- Tạo POI.
- Tạo connector.
- Tạo navigation node.
- Tạo navigation edge.
- Tạo migration.

Deliverable:

```text
Database schema hoàn chỉnh.
```

---

## Phase 3 — FloorplanVLM importer

Công việc:

- Tạo seed DTO.
- Tạo FloorplanVLM DTO.
- JSON parsing.
- JSON validation.
- External ID validation.
- Coordinate normalization.
- Raw JSON storage.
- Import transaction.
- Error report.

Deliverable:

```text
FloorplanVLM JSON → canonical database.
```

---

## Phase 4 — Geometry processing

Công việc:

- Build wall geometry.
- Build opening geometry.
- Build room polygon.
- Snap endpoints.
- Validate polygons.
- Tính centroid.
- Tính floor bounds.
- Xác định adjacency.

Deliverable:

```text
Geometry hợp lệ và có thể query bằng PostGIS.
```

---

## Phase 5 — 3D map API

Công việc:

- Map manifest API.
- Floor data API.
- DTO cho frontend.
- Coordinate convention.
- ETag.
- Cache.
- OpenAPI docs.

Deliverable:

```text
Frontend dựng được map 3D.
```

---

## Phase 6 — Search

Công việc:

- Normalize text.
- Search name.
- Search alias.
- Search category.
- Search theo tầng.
- Fuzzy search.
- Search ranking.
- Search indexes.

Deliverable:

```text
Người dùng tìm được room và POI.
```

---

## Phase 7 — Navigation graph

Công việc:

- Build walkable area.
- Sinh grid nodes.
- Sinh grid edges.
- Loại edge cắt tường.
- Nối door.
- Nối POI.
- Nối kiosk.
- Nối connector.
- Graph validation.
- Graph cache.

Deliverable:

```text
Navigation graph hoàn chỉnh.
```

---

## Phase 8 — Routing

Công việc:

- Endpoint resolver.
- A*.
- Route option filter.
- Blocked edge filter.
- Accessible route.
- Multi-floor route.
- Path simplification.
- ETA.
- Instruction generation.
- Route API.

Deliverable:

```text
Origin + destination → route polyline + instructions.
```

---

## Phase 9 — Hardening

Công việc:

- Unit test.
- Integration test.
- Performance test.
- Cache tuning.
- Monitoring.
- Structured logging.
- Rate limiting.
- Security.
- Seed documentation.
- API documentation.

Deliverable:

```text
Backend ổn định và sẵn sàng tích hợp frontend.
```

---

# 29. Sprint plan tham khảo

| Sprint | Nội dung | Deliverable |
|---|---|---|
| Sprint 1 | Spring Boot foundation, PostgreSQL, PostGIS, Redis | Project base |
| Sprint 2 | Entity, migration, canonical model | Database schema |
| Sprint 3 | FloorplanVLM adapter và seed importer | Import được map |
| Sprint 4 | Geometry processing | Room polygon hợp lệ |
| Sprint 5 | Map manifest và floor API | FE dựng được 3D |
| Sprint 6 | POI và search | Search destination |
| Sprint 7 | Same-floor navigation graph | Route cùng tầng |
| Sprint 8 | Multi-floor connectors | Route nhiều tầng |
| Sprint 9 | Accessibility, blocking, instruction | Routing đầy đủ |
| Sprint 10 | Test, cache, monitoring, optimization | Production-ready MVP |

---

# 30. Docker Compose local

```yaml
services:
  postgres:
    image: postgis/postgis:16-3.4
    environment:
      POSTGRES_DB: wayflo
      POSTGRES_USER: wayflo
      POSTGRES_PASSWORD: wayflo
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

volumes:
  postgres_data:
```

---

# 31. application.yml

```yaml
spring:
  application:
    name: wayflo-backend

  datasource:
    url: jdbc:postgresql://localhost:5432/wayflo
    username: wayflo
    password: wayflo

  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
        jdbc:
          batch_size: 100

  flyway:
    enabled: true

  data:
    redis:
      host: localhost
      port: 6379

  cache:
    type: redis

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus

wayflo:
  seed:
    enabled: false

  geometry:
    snap-tolerance-meter: 0.02
    minimum-room-area-square-meter: 0.5

  navigation:
    grid-spacing-meter: 0.5
    default-walking-speed-meter-per-second: 1.2
    elevator-base-penalty-second: 25
    stair-floor-penalty-second: 20
```

---

# 32. Security cơ bản

Trong phạm vi map visitor:

Public API:

```text
GET map manifest
GET floor map
GET search
POST route
```

Private API:

```text
POST import map
POST publish map
POST route blocks
DELETE route blocks
```

Có thể dùng:

- Spring Security.
- JWT.
- Role `BUILDING_MANAGER`.
- Role `BUILDING_OWNER`.
- Role `SYSTEM_ADMIN`.

Map version draft chỉ được truy cập bởi manager có quyền.

Visitor chỉ truy cập map đã publish.

---

# 33. Các quyết định kỹ thuật quan trọng

## Không dùng trực tiếp FloorplanVLM JSON cho frontend

Lý do:

- JSON model có thể thay đổi.
- Có thể thiếu business metadata.
- Không có scale chuẩn.
- Không có accessibility.
- Không có POI đầy đủ.
- Không có connector topology.
- Không có routing anchor.

Cần adapter:

```text
FloorplanVLM JSON
      ↓
Canonical WayFlo Map Model
```

---

## Tách rendering model và routing graph

Frontend dựng 3D từ:

```text
Walls
Openings
Spaces
POIs
Connectors
Floor elevation
```

Backend route từ:

```text
Navigation nodes
Navigation edges
Route blocks
Connector costs
Accessibility flags
```

Không nên dùng wall polygon trực tiếp để route ở mỗi request.

---

## Navigation graph phải thuộc map version

Mỗi map version có graph riêng.

```text
Map version 1 → Graph version 1
Map version 2 → Graph version 2
```

Khi publish version mới:

- Switch `publishedMapVersionId`.
- Warm cache graph mới.
- Không phá route đang sử dụng version cũ ngay lập tức.
- Có thể giữ version cũ trong thời gian ngắn.

---

# 34. Final backend flow

## Map rendering flow

```text
Frontend chọn building
        ↓
GET current map manifest
        ↓
Backend trả map version và floor list
        ↓
Frontend gọi floor API
        ↓
Backend trả walls, openings, spaces, POIs
        ↓
Frontend extrude geometry thành 3D
```

---

## Search flow

```text
User nhập tên cửa hàng
        ↓
GET search API
        ↓
Normalize query
        ↓
Search name, alias, category
        ↓
Ranking
        ↓
Return POI + routing anchor
```

---

## Route flow

```text
User chọn origin
        ↓
User chọn destination
        ↓
POST route API
        ↓
Resolve origin node
        ↓
Resolve destination node
        ↓
Load published graph
        ↓
Apply accessibility filters
        ↓
Remove blocked nodes/edges
        ↓
Run A*
        ↓
Simplify route
        ↓
Build instructions
        ↓
Return 3D polyline + ETA + instructions
```

---

# 35. Kết luận

Backend Spring Boot được chia thành ba lớp dữ liệu quan trọng:

```text
FloorplanVLM Raw JSON
        ↓
Canonical Spatial Map
        ↓
Navigation Graph
```

Trong đó:

- `FloorplanVLM Raw JSON` là dữ liệu đầu vào.
- `Canonical Spatial Map` phục vụ lưu trữ và dựng 3D.
- `Navigation Graph` phục vụ tìm đường.

Frontend không nên phụ thuộc trực tiếp vào schema FloorplanVLM.

Routing không nên tính trực tiếp từ geometry trong mỗi request.

Cách triển khai phù hợp nhất cho MVP:

```text
Spring Boot
PostgreSQL
PostGIS
Redis
Grid-based navigation graph
A* pathfinding
REST API
```

Sau khi MVP ổn định, có thể nâng cấp grid graph thành navigation mesh hoặc centerline graph mà không cần thay đổi API frontend.
