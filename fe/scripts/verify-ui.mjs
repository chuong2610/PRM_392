import { mkdir } from 'node:fs/promises';
import { chromium } from 'playwright';

const url = process.env.UI_URL || 'http://127.0.0.1:5174';
const buildingId = '11111111-1111-4111-8111-111111111111';
const mapVersionId = '22222222-2222-4222-8222-222222222222';

const floors = [
  {
    id: '33333333-3333-4333-8333-333333333331',
    externalId: 'floor-l1',
    name: 'Tầng 1 - Sảnh đón và mua sắm',
    floorNumber: 1,
    elevation: 0,
    defaultCeilingHeight: 4.5,
    metersPerUnit: 0.01,
    bounds: { minX: 0, minY: 0, maxX: 1000, maxY: 600 },
  },
  {
    id: '33333333-3333-4333-8333-333333333332',
    externalId: 'floor-l2',
    name: 'Tầng 2 - Thời trang và dịch vụ',
    floorNumber: 2,
    elevation: 4.8,
    defaultCeilingHeight: 4.5,
    metersPerUnit: 0.01,
    bounds: { minX: 0, minY: 0, maxX: 1000, maxY: 600 },
  },
  {
    id: '33333333-3333-4333-8333-333333333333',
    externalId: 'floor-l3',
    name: 'Tầng 3 - Ẩm thực, rạp phim và giải trí',
    floorNumber: 3,
    elevation: 9.6,
    defaultCeilingHeight: 4.5,
    metersPerUnit: 0.01,
    bounds: { minX: 0, minY: 0, maxX: 1000, maxY: 600 },
  },
];

const floorMaps = Object.fromEntries(floors.map((floor) => [floor.id, makeFloorMap(floor)]));

function makeFloorMap(floor) {
  const suffix = floor.floorNumber;
  return {
    buildingId,
    mapVersionId,
    floorId: floor.id,
    floorName: floor.name,
    floorNumber: floor.floorNumber,
    bounds: floor.bounds,
    walls: [
      { id: `${floor.id}-w1`, externalId: `l${suffix}-north`, start: [0, 0], end: [1000, 0], thickness: 12, height: 4.5 },
      { id: `${floor.id}-w2`, externalId: `l${suffix}-east`, start: [1000, 0], end: [1000, 600], thickness: 12, height: 4.5 },
      { id: `${floor.id}-w3`, externalId: `l${suffix}-south`, start: [1000, 600], end: [0, 600], thickness: 12, height: 4.5 },
      { id: `${floor.id}-w4`, externalId: `l${suffix}-west`, start: [0, 600], end: [0, 0], thickness: 12, height: 4.5 },
    ],
    openings: [
      { id: `${floor.id}-door`, externalId: `door-l${suffix}`, wallExternalId: `l${suffix}-north`, type: 'DOOR', center: [260, 220], width: 80 },
    ],
    spaces: [
      {
        id: `${floor.id}-corridor`,
        externalId: `room-l${suffix}-corridor`,
        name: `Hành lang chính tầng ${suffix}`,
        type: 'CORRIDOR',
        walkable: true,
        publicAccess: true,
        status: 'OPEN',
        polygon: [[80, 250], [740, 250], [740, 350], [80, 350]],
        centroid: [410, 300],
      },
      {
        id: `${floor.id}-connector`,
        externalId: `room-l${suffix}-elevator`,
        name: `Thang máy A tầng ${suffix}`,
        type: 'CONNECTOR',
        walkable: true,
        publicAccess: true,
        status: 'OPEN',
        polygon: [[760, 250], [860, 250], [860, 350], [760, 350]],
        centroid: [810, 300],
      },
      {
        id: `${floor.id}-store`,
        externalId: `room-l${suffix}-store`,
        name: suffix === 3 ? 'Cửa hàng Công nghệ B' : 'Bách hóa An Khang',
        type: suffix === 2 ? 'RESTROOM' : 'STORE',
        walkable: false,
        publicAccess: true,
        status: 'OPEN',
        polygon: [[120, 60], [340, 60], [340, 220], [120, 220]],
        centroid: [230, 140],
      },
    ],
    pois: [
      {
        id: `${floor.id}-poi`,
        externalId: suffix === 3 ? 'poi-store-b' : `poi-store-${suffix}`,
        name: suffix === 3 ? 'Cửa hàng Công nghệ B' : suffix === 2 ? 'Nhà vệ sinh tầng 2' : 'Bách hóa An Khang',
        category: suffix === 2 ? 'RESTROOM' : 'STORE',
        status: 'OPEN',
        floorId: floor.id,
        spaceExternalId: `room-l${suffix}-store`,
        displayAnchor: [230, 170],
        aliases: ['store', 'nha ve sinh'],
      },
    ],
    connectors: [
      {
        id: `${floor.id}-elevator`,
        externalId: `elevator-a-l${suffix}`,
        groupId: 'elevator-a',
        type: 'ELEVATOR',
        servedFloors: [1, 2, 3],
        accessible: true,
        status: 'OPEN',
        anchor: [810, 300],
      },
    ],
    kiosks: suffix === 1
      ? [{ id: `${floor.id}-kiosk`, externalId: 'kiosk-l1-01', name: 'Máy tra cứu lối vào chính', floorId: floor.id, position: [90, 310] }]
      : [],
  };
}

async function installMocks(page) {
  await page.route('**/actuator/health', (route) => json(route, { status: 'UP' }));
  await page.route('**/api/v1/admin/map-imports/seed-resources', (route) => json(route, {
    timestamp: new Date().toISOString(),
    data: { buildingId, mapVersionId, versionNumber: 1, floorCount: 3, nodeCount: 10, edgeCount: 10 },
  }));
  await page.route('**/api/v1/buildings/*/maps/current', (route) => json(route, {
    timestamp: new Date().toISOString(),
    data: {
      buildingId,
      buildingExternalId: 'mall-hcm-01',
      buildingName: 'Trung tâm WayFlo Sài Gòn',
      mapVersionId,
      versionNumber: 1,
      coordinateUnit: 'pixel',
      floors,
    },
  }));
  await page.route('**/api/v1/buildings/*/maps/current/floors/*', (route) => {
    const floorId = route.request().url().split('/').pop();
    return json(route, { timestamp: new Date().toISOString(), data: floorMaps[floorId] });
  });
  await page.route('**/api/v1/buildings/*/search?**', (route) => json(route, {
    timestamp: new Date().toISOString(),
    data: {
      query: 'nhà vệ sinh',
      results: [
        {
          id: `${floors[2].id}-poi`,
          externalId: 'poi-store-b',
          name: 'Nhà vệ sinh tầng 2',
          type: 'POI',
          category: 'RESTROOM',
          floorId: floors[1].id,
          floorName: floors[1].name,
          score: 100,
          anchor: [230, 170],
        },
      ],
    },
  }));
  await page.route('**/api/v1/buildings/*/routes', (route) => json(route, {
    timestamp: new Date().toISOString(),
    data: {
      mapVersionId,
      distanceMeters: 14.2,
      etaSeconds: 48,
      polyline: [
        { floorId: floors[0].id, floorNumber: 1, x: 90, y: 310, z: 0 },
        { floorId: floors[0].id, floorNumber: 1, x: 810, y: 300, z: 0 },
        { floorId: floors[1].id, floorNumber: 2, x: 810, y: 300, z: 4.8 },
        { floorId: floors[2].id, floorNumber: 3, x: 810, y: 300, z: 9.6 },
        { floorId: floors[2].id, floorNumber: 3, x: 230, y: 170, z: 9.6 },
      ],
      steps: [
        { type: 'START', instruction: 'Bắt đầu ở tầng 1.', distanceMeters: 0, floorId: floors[0].id },
        { type: 'WALK', instruction: 'Đi bộ khoảng 7 mét.', distanceMeters: 7, floorId: floors[0].id },
        { type: 'TAKE_ELEVATOR', instruction: 'Đi thang máy đến tầng 3.', distanceMeters: 0, floorId: floors[2].id },
        { type: 'ARRIVE', instruction: 'Đã đến điểm đến.', distanceMeters: 0, floorId: floors[2].id },
      ],
    },
  }));
}

function json(route, payload) {
  return route.fulfill({
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify(payload),
  });
}

async function sampleCanvas(page) {
  return page.locator('canvas').evaluate((canvas) => {
    const gl = canvas.getContext('webgl2') || canvas.getContext('webgl');
    if (!gl) {
      return { ok: false, reason: 'No WebGL context' };
    }
    const width = gl.drawingBufferWidth;
    const height = gl.drawingBufferHeight;
    const samples = [];
    const xs = [0.25, 0.5, 0.75];
    const ys = [0.25, 0.5, 0.75];
    for (const x of xs) {
      for (const y of ys) {
        const pixel = new Uint8Array(4);
        gl.readPixels(Math.floor(width * x), Math.floor(height * y), 1, 1, gl.RGBA, gl.UNSIGNED_BYTE, pixel);
        samples.push(Array.from(pixel).join(','));
      }
    }
    const unique = new Set(samples);
    return { ok: unique.size >= 3, unique: unique.size, samples };
  });
}

await mkdir('artifacts', { recursive: true });
const browser = await chromium.launch();

for (const viewport of [
  { width: 1440, height: 920, name: 'desktop' },
  { width: 390, height: 844, name: 'mobile' },
]) {
  const page = await browser.newPage({ viewport });
  await page.addInitScript(() => localStorage.clear());
  await installMocks(page);
  await page.goto(url, { waitUntil: 'domcontentloaded' });
  await page.getByTitle('Import dữ liệu seed').click();
  await page.waitForSelector('.floor-tabs button.active');
  await page.getByTitle('Tìm địa điểm').click();
  await page.waitForSelector('.result');
  await page.locator('.result').first().click();
  await page.getByTitle('Tạo đường đi').click();
  await page.waitForSelector('.steps-dock');
  await page.waitForTimeout(900);

  const canvasStatus = await sampleCanvas(page);
  if (!canvasStatus.ok) {
    throw new Error(`Canvas pixel check failed on ${viewport.name}: ${JSON.stringify(canvasStatus)}`);
  }
  await page.screenshot({ path: `artifacts/wayflo-${viewport.name}.png`, fullPage: true });
  await page.close();
}

await browser.close();
console.log('UI verification passed for desktop and mobile.');
