import { useEffect, useMemo, useState } from 'react';
import {
  Building2,
  CheckCircle2,
  DownloadCloud,
  Eye,
  Layers3,
  Loader2,
  MapPinned,
  Play,
  RefreshCw,
  Route,
  Search,
  Server,
  SlidersHorizontal,
} from 'lucide-react';
import MapScene from './MapScene.jsx';
import {
  checkHealth,
  createRoute,
  getFloorMap,
  getManifest,
  importSeedResources,
  searchLocations,
} from './api.js';

const STORAGE_KEYS = {
  baseUrl: 'wayflo.baseUrl',
  buildingId: 'wayflo.buildingId',
};

const CATEGORY_LABELS = {
  BOOK_STORE: 'Nhà sách',
  CAFE: 'Cà phê',
  COSMETICS: 'Mỹ phẩm',
  DEPARTMENT_STORE: 'Bách hóa',
  ELECTRONICS_STORE: 'Điện máy',
  FOOD_COURT: 'Ẩm thực',
  LANDMARK: 'Điểm mốc',
  PHARMACY: 'Nhà thuốc',
  RESTROOM: 'Nhà vệ sinh',
  SUPERMARKET: 'Siêu thị',
};

function initialBaseUrl() {
  const params = new URLSearchParams(window.location.search);
  return params.get('baseUrl') || localStorage.getItem(STORAGE_KEYS.baseUrl) || import.meta.env.VITE_API_BASE_URL || '';
}

function initialBuildingId() {
  const params = new URLSearchParams(window.location.search);
  return params.get('buildingId') || localStorage.getItem(STORAGE_KEYS.buildingId) || '';
}

function App() {
  const [baseUrl, setBaseUrl] = useState(initialBaseUrl);
  const [buildingId, setBuildingId] = useState(initialBuildingId);
  const [manifest, setManifest] = useState(null);
  const [floorMaps, setFloorMaps] = useState({});
  const [activeFloorId, setActiveFloorId] = useState('');
  const [query, setQuery] = useState('nhà vệ sinh');
  const [searchType, setSearchType] = useState('ALL');
  const [searchFloorId, setSearchFloorId] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [selectedResult, setSelectedResult] = useState(null);
  const [originType, setOriginType] = useState('KIOSK');
  const [originExternalId, setOriginExternalId] = useState('kiosk-l1-01');
  const [destinationType, setDestinationType] = useState('POI');
  const [destinationExternalId, setDestinationExternalId] = useState('poi-store-b');
  const [accessibleOnly, setAccessibleOnly] = useState(true);
  const [avoidStairs, setAvoidStairs] = useState(true);
  const [showAllFloors, setShowAllFloors] = useState(false);
  const [viewMode, setViewMode] = useState('tilt');
  const [route, setRoute] = useState(null);
  const [status, setStatus] = useState({ tone: 'idle', message: 'Sẵn sàng' });
  const [busy, setBusy] = useState('');

  const floors = manifest?.data?.floors || [];
  const floorEntries = useMemo(() => floorMaps, [floorMaps]);
  const selectedFloor = floors.find((floor) => floor.id === activeFloorId);

  useEffect(() => {
    localStorage.setItem(STORAGE_KEYS.baseUrl, baseUrl);
  }, [baseUrl]);

  useEffect(() => {
    if (buildingId) {
      localStorage.setItem(STORAGE_KEYS.buildingId, buildingId);
      loadManifestAndFloors(buildingId);
    }
  }, [buildingId]);

  // Đồng bộ sang Flutter qua JS Channel khi đổi tầng
  useEffect(() => {
    if (activeFloorId && window.WayFloChannel) {
      window.WayFloChannel.postMessage(JSON.stringify({
        type: 'FLOOR_CHANGED',
        payload: activeFloorId
      }));
    }
  }, [activeFloorId]);

  // Đồng bộ sang Flutter qua JS Channel khi chọn POI
  useEffect(() => {
    if (selectedResult && window.WayFloChannel) {
      window.WayFloChannel.postMessage(JSON.stringify({
        type: 'POI_SELECTED',
        payload: selectedResult
      }));
    }
  }, [selectedResult]);

  // Lắng nghe lệnh gửi từ Flutter xuống Webview
  useEffect(() => {
    window.handleFlutterMessage = (message) => {
      console.log('Received message from Flutter:', message);
      const { type, payload } = message;
      switch (type) {
        case 'CHANGE_FLOOR':
          setActiveFloorId(payload);
          break;
        case 'SELECT_POI_BY_ID':
          if (payload) {
            const found = searchResults.find(r => r.externalId === payload);
            if (found) {
              selectResult(found);
            }
          }
          break;
        default:
          console.warn('Unknown event type from Flutter:', type);
      }
    };
    return () => {
      delete window.handleFlutterMessage;
    };
  }, [searchResults]);

  async function runTask(taskName, action, successMessage) {
    setBusy(taskName);
    setStatus({ tone: 'idle', message: 'Đang xử lý...' });
    try {
      const value = await action();
      setStatus({ tone: 'ok', message: successMessage });
      return value;
    } catch (error) {
      setStatus({ tone: 'error', message: error.message });
      throw error;
    } finally {
      setBusy('');
    }
  }

  async function handleHealth() {
    await runTask('health', async () => {
      const result = await checkHealth(baseUrl);
      setStatus({ tone: 'ok', message: `Backend ${result.status}` });
      return result;
    }, 'Backend hoạt động tốt');
  }

  async function handleImportSeed() {
    const result = await runTask('import', async () => {
      const payload = await importSeedResources(baseUrl);
      const importedBuildingId = payload.data.buildingId;
      setBuildingId(importedBuildingId);
      await loadManifestAndFloors(importedBuildingId);
      return payload;
    }, 'Đã import seed');
    setDestinationExternalId('poi-store-b');
    setOriginExternalId('kiosk-l1-01');
    return result;
  }

  async function loadManifestAndFloors(nextBuildingId = buildingId) {
    if (!nextBuildingId) {
      setStatus({ tone: 'error', message: 'Cần nhập Building ID' });
      return null;
    }
    return runTask('manifest', async () => {
      const loadedManifest = await getManifest(baseUrl, nextBuildingId);
      setManifest(loadedManifest);
      const floorSummaries = loadedManifest.data.floors || [];
      if (floorSummaries.length > 0) {
        setActiveFloorId((current) => current || floorSummaries[0].id);
      }
      const maps = await Promise.all(
        floorSummaries.map(async (summary) => {
          const floorMap = await getFloorMap(baseUrl, nextBuildingId, summary.id);
          return [summary.id, { summary, map: floorMap.data }];
        }),
      );
      setFloorMaps(Object.fromEntries(maps));
      return loadedManifest;
    }, 'Đã tải bản đồ');
  }

  async function handleSearch(event) {
    event?.preventDefault();
    if (!buildingId) {
      setStatus({ tone: 'error', message: 'Hãy import seed hoặc nhập Building ID trước' });
      return;
    }
    await runTask('search', async () => {
      const response = await searchLocations(baseUrl, buildingId, {
        query,
        type: searchType,
        floorId: searchFloorId,
        limit: 12,
      });
      setSearchResults(response.data.results || []);
      return response;
    }, 'Tìm kiếm xong');
  }

  function selectResult(result) {
    setSelectedResult(result);
    setActiveFloorId(result.floorId);
    if (result.type === 'POI') {
      setDestinationType('POI');
      setDestinationExternalId(result.externalId);
    } else {
      setDestinationType('POINT');
      setDestinationExternalId('');
    }
  }

  async function handleRoute() {
    if (!buildingId) {
      setStatus({ tone: 'error', message: 'Hãy import seed hoặc nhập Building ID trước' });
      return;
    }
    await runTask('route', async () => {
      const body = {
        origin: buildEndpoint(originType, originExternalId),
        destination: buildDestinationEndpoint(),
        options: {
          accessibleOnly,
          avoidStairs,
        },
      };
      const response = await createRoute(baseUrl, buildingId, body);
      setRoute(response.data);
      if (response.data.polyline?.length) {
        setActiveFloorId(response.data.polyline[response.data.polyline.length - 1].floorId);
      }
      return response;
    }, 'Đã tạo đường đi');
  }

  function buildDestinationEndpoint() {
    if (selectedResult && destinationType === 'POINT') {
      return {
        type: 'POINT',
        floorId: selectedResult.floorId,
        position: {
          x: selectedResult.anchor?.[0] || 0,
          y: selectedResult.anchor?.[1] || 0,
        },
      };
    }
    return buildEndpoint(destinationType, destinationExternalId);
  }

  function buildEndpoint(type, externalId) {
    if (type === 'POINT') {
      const floorId = activeFloorId || floors[0]?.id;
      return {
        type,
        floorId,
        position: { x: 300, y: 300 },
      };
    }
    return { type, externalId };
  }

  const statItems = [
    ['Tầng', floors.length],
    ['Khu vực', sumFloorItems(floorMaps, 'spaces')],
    ['Địa điểm', sumFloorItems(floorMaps, 'pois')],
    ['Bước', route?.steps?.length || 0],
  ];

  return (
    <div className="app">
      <aside className="sidebar">
        <header className="brand">
          <div className="brand-mark">
            <MapPinned size={22} />
          </div>
          <div>
            <h1>WayFlo Console</h1>
            <span>Kiểm thử bản đồ 3D</span>
          </div>
        </header>

        <section className="panel">
          <div className="panel-title">
            <Server size={17} />
            <h2>Backend</h2>
          </div>
          <label className="field">
            <span>Base URL</span>
            <input
              value={baseUrl}
              onChange={(event) => setBaseUrl(event.target.value)}
              placeholder="Proxy Vite hoặc http://localhost:8080"
            />
          </label>
          <label className="field">
            <span>Building ID</span>
            <input
              value={buildingId}
              onChange={(event) => setBuildingId(event.target.value)}
              placeholder="Import seed để tự điền"
            />
          </label>
          <div className="button-row">
            <button title="Kiểm tra backend" onClick={handleHealth} disabled={busy === 'health'}>
              {busy === 'health' ? <Loader2 className="spin" size={16} /> : <CheckCircle2 size={16} />}
              Kiểm tra
            </button>
            <button title="Import dữ liệu seed" onClick={handleImportSeed} disabled={busy === 'import'}>
              {busy === 'import' ? <Loader2 className="spin" size={16} /> : <DownloadCloud size={16} />}
              Import
            </button>
            <button title="Tải lại bản đồ" onClick={() => loadManifestAndFloors()} disabled={busy === 'manifest'}>
              {busy === 'manifest' ? <Loader2 className="spin" size={16} /> : <RefreshCw size={16} />}
              Tải
            </button>
          </div>
          <div className={`status ${status.tone}`}>{status.message}</div>
        </section>

        <section className="panel">
          <div className="panel-title">
            <Layers3 size={17} />
            <h2>Tầng</h2>
          </div>
          <div className="floor-tabs">
            {floors.map((floor) => (
              <button
                key={floor.id}
                className={activeFloorId === floor.id ? 'active' : ''}
                title={`Xem ${floor.name}`}
                onClick={() => setActiveFloorId(floor.id)}
              >
                {floor.floorNumber}
              </button>
            ))}
          </div>
          <div className="stats">
            {statItems.map(([label, value]) => (
              <div key={label} className="stat">
                <strong>{value}</strong>
                <span>{label}</span>
              </div>
            ))}
          </div>
        </section>

        <section className="panel">
          <div className="panel-title">
            <Search size={17} />
            <h2>Tìm kiếm</h2>
          </div>
          <form onSubmit={handleSearch} className="search-form">
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="cà phê, nhà vệ sinh, rạp phim" />
            <button title="Tìm địa điểm" disabled={busy === 'search'}>
              {busy === 'search' ? <Loader2 className="spin" size={16} /> : <Search size={16} />}
            </button>
          </form>
          <div className="control-grid">
            <select value={searchType} onChange={(event) => setSearchType(event.target.value)}>
              <option value="ALL">Tất cả</option>
              <option value="POI">Địa điểm</option>
              <option value="SPACE">Khu vực</option>
            </select>
            <select value={searchFloorId} onChange={(event) => setSearchFloorId(event.target.value)}>
              <option value="">Mọi tầng</option>
              {floors.map((floor) => (
                <option key={floor.id} value={floor.id}>{floor.name}</option>
              ))}
            </select>
          </div>
          <div className="results">
            {searchResults.map((result) => (
              <button
                key={`${result.type}-${result.id}`}
                className={`result ${selectedResult?.id === result.id ? 'selected' : ''}`}
                title={`Chọn ${result.name}`}
                onClick={() => selectResult(result)}
              >
                <span>
                  <strong>{result.name}</strong>
                  <small>{formatCategory(result.category)} · {result.floorName}</small>
                </span>
                <em>{Math.round(result.score)}</em>
              </button>
            ))}
          </div>
        </section>

        <section className="panel">
          <div className="panel-title">
            <Route size={17} />
            <h2>Đường đi</h2>
          </div>
          <div className="control-grid">
            <select value={originType} onChange={(event) => setOriginType(event.target.value)}>
              <option value="KIOSK">Máy tra cứu</option>
              <option value="POI">Địa điểm</option>
              <option value="NODE">Nút đường</option>
              <option value="POINT">Tọa độ</option>
            </select>
            <input value={originExternalId} onChange={(event) => setOriginExternalId(event.target.value)} placeholder="externalId điểm đi" />
          </div>
          <div className="control-grid">
            <select value={destinationType} onChange={(event) => setDestinationType(event.target.value)}>
              <option value="POI">Địa điểm</option>
              <option value="KIOSK">Máy tra cứu</option>
              <option value="NODE">Nút đường</option>
              <option value="POINT">Tọa độ</option>
            </select>
            <input value={destinationExternalId} onChange={(event) => setDestinationExternalId(event.target.value)} placeholder="externalId điểm đến" />
          </div>
          <div className="toggles">
            <label>
              <input type="checkbox" checked={accessibleOnly} onChange={(event) => setAccessibleOnly(event.target.checked)} />
              Dễ tiếp cận
            </label>
            <label>
              <input type="checkbox" checked={avoidStairs} onChange={(event) => setAvoidStairs(event.target.checked)} />
              Tránh cầu thang
            </label>
          </div>
          <button className="primary" title="Tạo đường đi" onClick={handleRoute} disabled={busy === 'route'}>
            {busy === 'route' ? <Loader2 className="spin" size={16} /> : <Play size={16} />}
            Tạo đường đi
          </button>
          {route && (
            <div className="route-summary">
              <strong>{route.distanceMeters.toFixed(1)} m</strong>
              <span>{Math.round(route.etaSeconds)} giây</span>
            </div>
          )}
        </section>
      </aside>

      <main className="workspace">
        <div className="scene-topbar">
          <div className="scene-chip">
            <Building2 size={18} />
            <span>{manifest?.data?.buildingName || 'Trung tâm WayFlo'}</span>
          </div>
          <div className="scene-chip">
            <SlidersHorizontal size={18} />
            <span>{showAllFloors ? 'Tất cả tầng' : selectedFloor?.name || 'Một tầng'}</span>
          </div>
          <div className="view-controls" aria-label="Chế độ xem bản đồ">
            <button className={!showAllFloors ? 'active' : ''} onClick={() => setShowAllFloors(false)} title="Chỉ xem tầng đang chọn">
              <Layers3 size={15} />
              Một tầng
            </button>
            <button className={showAllFloors ? 'active' : ''} onClick={() => setShowAllFloors(true)} title="Xem chồng tất cả tầng">
              <Layers3 size={15} />
              Tất cả
            </button>
            <button className={viewMode === 'tilt' ? 'active' : ''} onClick={() => setViewMode('tilt')} title="Góc nhìn 3D">
              <Eye size={15} />
              3D
            </button>
            <button className={viewMode === 'top' ? 'active' : ''} onClick={() => setViewMode('top')} title="Góc nhìn từ trên xuống">
              <Eye size={15} />
              2D
            </button>
          </div>
        </div>
        <MapScene
          floors={floorEntries}
          activeFloorId={activeFloorId}
          route={route}
          showAllFloors={showAllFloors}
          viewMode={viewMode}
        />
        {route?.steps?.length > 0 && (
          <div className="steps-dock">
            {route.steps.map((step, index) => (
              <div key={`${step.type}-${index}`} className="step">
                <span>{index + 1}</span>
                <p>{step.instruction}</p>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}

function sumFloorItems(floorMaps, key) {
  return Object.values(floorMaps).reduce((total, entry) => total + (entry.map?.[key]?.length || 0), 0);
}

function formatCategory(category) {
  return CATEGORY_LABELS[category] || category || 'Khu vực';
}

export default App;
