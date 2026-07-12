const DEFAULT_HEADERS = {
  'Content-Type': 'application/json',
};

function joinUrl(baseUrl, path) {
  if (!baseUrl) {
    return path;
  }
  return `${baseUrl.replace(/\/$/, '')}${path}`;
}

async function request(baseUrl, path, options = {}) {
  const response = await fetchWithLocalFallback(baseUrl, path, options);

  const contentType = response.headers.get('content-type') || '';
  const payload = contentType.includes('application/json') ? await response.json() : await response.text();

  if (!response.ok) {
    const message = payload?.message || payload?.error || response.statusText;
    throw new Error(`${response.status} ${message}`);
  }

  return payload;
}

async function fetchWithLocalFallback(baseUrl, path, options) {
  const response = await fetch(joinUrl(baseUrl, path), {
    ...options,
    headers: {
      ...DEFAULT_HEADERS,
      ...options.headers,
    },
  });

  if (!baseUrl && [403, 404, 405].includes(response.status)) {
    return fetch(joinUrl('http://localhost:8080', path), {
      ...options,
      headers: {
        ...DEFAULT_HEADERS,
        ...options.headers,
      },
    });
  }

  return response;
}

export function checkHealth(baseUrl) {
  return request(baseUrl, '/actuator/health', { method: 'GET' });
}

export function importSeedResources(baseUrl) {
  return request(baseUrl, '/api/v1/admin/map-imports/seed-resources', { method: 'POST' });
}

export function getManifest(baseUrl, buildingId) {
  return request(baseUrl, `/api/v1/buildings/${buildingId}/maps/current`, { method: 'GET' });
}

export function getFloorMap(baseUrl, buildingId, floorId) {
  return request(baseUrl, `/api/v1/buildings/${buildingId}/maps/current/floors/${floorId}`, { method: 'GET' });
}

export function searchLocations(baseUrl, buildingId, params) {
  const searchParams = new URLSearchParams({
    q: params.query,
    type: params.type,
    limit: String(params.limit || 20),
  });
  if (params.floorId) {
    searchParams.set('floorId', params.floorId);
  }
  return request(baseUrl, `/api/v1/buildings/${buildingId}/search?${searchParams}`, { method: 'GET' });
}

export function createRoute(baseUrl, buildingId, body) {
  return request(baseUrl, `/api/v1/buildings/${buildingId}/routes`, {
    method: 'POST',
    body: JSON.stringify(body),
  });
}
