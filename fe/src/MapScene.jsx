import { useEffect, useMemo, useRef } from 'react';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';

const MAP_SCALE = 0.048;
const HEIGHT_SCALE = 0.38;
const FLOOR_STACK_GAP = 6.2;
const SLAB_THICKNESS = 0.18;
const ROUTE_LIFT = 0.32;
const TILE_UNIT = 80;

const SPACE_COLORS = {
  CORRIDOR: 0xdff7ff,
  STORE: 0xffd5df,
  ROOM: 0xe9ddff,
  RESTROOM: 0xc8f7d5,
  CONNECTOR: 0xffefad,
  SERVICE: 0xe2e8f0,
  KIOSK_AREA: 0xfbd4eb,
};

const SPACE_ACCENTS = {
  CORRIDOR: 0x38bdf8,
  STORE: 0xe11d48,
  ROOM: 0x7c3aed,
  RESTROOM: 0x16a34a,
  CONNECTOR: 0x2563eb,
  SERVICE: 0x64748b,
  KIOSK_AREA: 0xdb2777,
};

function createLayout(sceneData) {
  const boundsList = sceneData
    .map(({ summary, map }) => map?.bounds || summary?.bounds)
    .filter(Boolean);
  const extent = boundsList.reduce((acc, bounds) => ({
    minX: Math.min(acc.minX, bounds.minX),
    minY: Math.min(acc.minY, bounds.minY),
    maxX: Math.max(acc.maxX, bounds.maxX),
    maxY: Math.max(acc.maxY, bounds.maxY),
  }), {
    minX: Number.POSITIVE_INFINITY,
    minY: Number.POSITIVE_INFINITY,
    maxX: Number.NEGATIVE_INFINITY,
    maxY: Number.NEGATIVE_INFINITY,
  });

  const safeExtent = Number.isFinite(extent.minX)
    ? extent
    : { minX: 0, minY: 0, maxX: 1000, maxY: 600 };
  const yByFloorId = new Map();
  sceneData.forEach(({ summary }, index) => {
    yByFloorId.set(summary.id, index * FLOOR_STACK_GAP);
  });

  return {
    centerX: (safeExtent.minX + safeExtent.maxX) / 2,
    centerZ: (safeExtent.minY + safeExtent.maxY) / 2,
    width: (safeExtent.maxX - safeExtent.minX) * MAP_SCALE,
    depth: (safeExtent.maxY - safeExtent.minY) * MAP_SCALE,
    yByFloorId,
    maxY: Math.max(0, (sceneData.length - 1) * FLOOR_STACK_GAP),
  };
}

function floorY(summary, layout) {
  return layout.yByFloorId.get(summary.id) ?? 0;
}

function mapPoint(point, layout, floorId, lift = 0) {
  return new THREE.Vector3(
    (point[0] - layout.centerX) * MAP_SCALE,
    (layout.yByFloorId.get(floorId) ?? 0) + lift,
    (point[1] - layout.centerZ) * MAP_SCALE,
  );
}

function worldPointFromPlan(point, layout, floorId, lift = 0) {
  return {
    x: (point[0] - layout.centerX) * MAP_SCALE,
    y: (layout.yByFloorId.get(floorId) ?? 0) + lift,
    z: (point[1] - layout.centerZ) * MAP_SCALE,
  };
}

function addBox(scene, size, position, color, options = {}) {
  const geometry = new THREE.BoxGeometry(size.x, size.y, size.z);
  const material = new THREE.MeshStandardMaterial({
    color,
    transparent: options.opacity != null,
    opacity: options.opacity ?? 1,
    roughness: options.roughness ?? 0.58,
    metalness: options.metalness ?? 0,
    emissive: options.emissive ?? 0x000000,
    emissiveIntensity: options.emissiveIntensity ?? 0,
  });
  const mesh = new THREE.Mesh(geometry, material);
  mesh.position.set(position.x, position.y, position.z);
  mesh.rotation.y = options.rotationY || 0;
  mesh.castShadow = Boolean(options.castShadow);
  mesh.receiveShadow = Boolean(options.receiveShadow);
  scene.add(mesh);
  return mesh;
}

function makeShape(polygon, layout) {
  const shape = new THREE.Shape();
  polygon.forEach((point, index) => {
    const x = (point[0] - layout.centerX) * MAP_SCALE;
    const y = (point[1] - layout.centerZ) * MAP_SCALE;
    if (index === 0) {
      shape.moveTo(x, y);
    } else {
      shape.lineTo(x, y);
    }
  });
  shape.closePath();
  return shape;
}

function polygonBounds(polygon) {
  return polygon.reduce((bounds, point) => ({
    minX: Math.min(bounds.minX, point[0]),
    minY: Math.min(bounds.minY, point[1]),
    maxX: Math.max(bounds.maxX, point[0]),
    maxY: Math.max(bounds.maxY, point[1]),
  }), {
    minX: Number.POSITIVE_INFINITY,
    minY: Number.POSITIVE_INFINITY,
    maxX: Number.NEGATIVE_INFINITY,
    maxY: Number.NEGATIVE_INFINITY,
  });
}

function addTileLines(scene, bounds, summary, layout, isActive) {
  if (!bounds) {
    return;
  }
  const points = [];
  for (let x = Math.ceil(bounds.minX / TILE_UNIT) * TILE_UNIT; x < bounds.maxX; x += TILE_UNIT) {
    points.push(mapPoint([x, bounds.minY], layout, summary.id, 0.025));
    points.push(mapPoint([x, bounds.maxY], layout, summary.id, 0.025));
  }
  for (let y = Math.ceil(bounds.minY / TILE_UNIT) * TILE_UNIT; y < bounds.maxY; y += TILE_UNIT) {
    points.push(mapPoint([bounds.minX, y], layout, summary.id, 0.025));
    points.push(mapPoint([bounds.maxX, y], layout, summary.id, 0.025));
  }
  const geometry = new THREE.BufferGeometry().setFromPoints(points);
  const material = new THREE.LineBasicMaterial({
    color: isActive ? 0xd8e2ef : 0xe2e8f0,
    transparent: true,
    opacity: isActive ? 0.72 : 0.18,
  });
  scene.add(new THREE.LineSegments(geometry, material));
}

function ellipsize(ctx, text, maxWidth) {
  if (!text || ctx.measureText(text).width <= maxWidth) {
    return text || '';
  }
  let value = text;
  while (value.length > 3 && ctx.measureText(`${value}...`).width > maxWidth) {
    value = value.slice(0, -1);
  }
  return `${value.trim()}...`;
}

function addLabel(scene, text, position, color = '#172033') {
  const canvas = document.createElement('canvas');
  canvas.width = 320;
  canvas.height = 76;
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  ctx.fillStyle = 'rgba(255,255,255,0.94)';
  roundRect(ctx, 8, 10, 304, 50, 12);
  ctx.fill();
  ctx.strokeStyle = 'rgba(148,163,184,0.55)';
  ctx.lineWidth = 2;
  ctx.stroke();
  ctx.font = '700 20px Inter, system-ui, sans-serif';
  ctx.fillStyle = color;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(ellipsize(ctx, text, 266), 160, 36);

  const texture = new THREE.CanvasTexture(canvas);
  const material = new THREE.SpriteMaterial({ map: texture, transparent: true });
  const sprite = new THREE.Sprite(material);
  sprite.position.copy(position);
  sprite.scale.set(6.9, 1.65, 1);
  scene.add(sprite);
}

function roundRect(ctx, x, y, width, height, radius) {
  ctx.beginPath();
  ctx.moveTo(x + radius, y);
  ctx.lineTo(x + width - radius, y);
  ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
  ctx.lineTo(x + width, y + height - radius);
  ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
  ctx.lineTo(x + radius, y + height);
  ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
  ctx.lineTo(x, y + radius);
  ctx.quadraticCurveTo(x, y, x + radius, y);
  ctx.closePath();
}

function addIconSprite(scene, text, position, options = {}) {
  const canvas = document.createElement('canvas');
  canvas.width = 160;
  canvas.height = 160;
  const ctx = canvas.getContext('2d');
  ctx.clearRect(0, 0, canvas.width, canvas.height);
  ctx.beginPath();
  ctx.arc(80, 80, 54, 0, Math.PI * 2);
  ctx.fillStyle = options.fill || '#ffffff';
  ctx.fill();
  ctx.lineWidth = 8;
  ctx.strokeStyle = options.stroke || '#172033';
  ctx.stroke();
  ctx.font = `${options.fontWeight || 800} ${options.fontSize || 42}px Inter, system-ui, sans-serif`;
  ctx.fillStyle = options.textColor || '#172033';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(text, 80, 83);

  const texture = new THREE.CanvasTexture(canvas);
  const material = new THREE.SpriteMaterial({ map: texture, transparent: true });
  const sprite = new THREE.Sprite(material);
  sprite.position.copy(position);
  const scale = options.scale || 1.8;
  sprite.scale.set(scale, scale, 1);
  scene.add(sprite);
  return sprite;
}

function addConnectorFloorBadge(scene, position, summary, layout, name) {
  const lowerName = String(name || '').toLowerCase();
  const isEscalator = lowerName.includes('cuon') || lowerName.includes('cuốn');
  const isStair = lowerName.includes('stair') || lowerName.includes('thang thoat') || lowerName.includes('thoat hiem');
  const label = isEscalator ? 'ES' : isStair ? 'ST' : 'E';
  const color = isEscalator ? 0xf59e0b : isStair ? 0x64748b : 0x2563eb;
  const center = mapPoint(position, layout, summary.id, 0.18);
  const disk = new THREE.Mesh(
    new THREE.CylinderGeometry(0.58, 0.58, 0.08, 40),
    new THREE.MeshStandardMaterial({ color, transparent: true, opacity: 0.16, roughness: 0.45 }),
  );
  disk.position.copy(center);
  scene.add(disk);
  addIconSprite(scene, label, mapPoint(position, layout, summary.id, 1.05), {
    fill: '#ffffff',
    stroke: isEscalator ? '#d97706' : isStair ? '#475569' : '#2563eb',
    textColor: isEscalator ? '#92400e' : isStair ? '#334155' : '#1d4ed8',
    fontSize: label.length > 1 ? 36 : 46,
    scale: 1.25,
  });
}

function addAtriumFeature(scene, space, summary, layout) {
  if (!space.centroid?.length) {
    return;
  }
  const center = mapPoint(space.centroid, layout, summary.id, 0.22);
  const basin = new THREE.Mesh(
    new THREE.CylinderGeometry(0.62, 0.72, 0.12, 40),
    new THREE.MeshStandardMaterial({ color: 0xc7d2fe, transparent: true, opacity: 0.55, roughness: 0.25 }),
  );
  basin.position.copy(center);
  scene.add(basin);

  const water = new THREE.Mesh(
    new THREE.CylinderGeometry(0.42, 0.42, 0.18, 40),
    new THREE.MeshStandardMaterial({
      color: 0x38bdf8,
      transparent: true,
      opacity: 0.42,
      roughness: 0.18,
      metalness: 0.08,
    }),
  );
  water.position.copy(mapPoint(space.centroid, layout, summary.id, 0.36));
  scene.add(water);

  const ring = new THREE.Mesh(
    new THREE.TorusGeometry(0.92, 0.025, 8, 64),
    new THREE.MeshBasicMaterial({ color: 0x60a5fa, transparent: true, opacity: 0.5 }),
  );
  ring.rotation.x = Math.PI / 2;
  ring.position.copy(mapPoint(space.centroid, layout, summary.id, 0.43));
  scene.add(ring);
}

function addSpace(scene, space, summary, layout, isActive) {
  if (!space.polygon?.length) {
    return;
  }
  const shape = makeShape(space.polygon, layout);
  const type = space.type || 'ROOM';
  const accent = SPACE_ACCENTS[type] || SPACE_ACCENTS.ROOM;
  const isTenant = ['STORE', 'RESTROOM', 'SERVICE', 'ROOM', 'KIOSK_AREA'].includes(type);
  const isConnector = type === 'CONNECTOR';

  const geometry = new THREE.ShapeGeometry(shape);
  const material = new THREE.MeshStandardMaterial({
    color: isActive ? (SPACE_COLORS[type] || SPACE_COLORS.ROOM) : 0xcbd5e1,
    transparent: true,
    opacity: isActive ? (isTenant ? 0.58 : 0.24) : 0.08,
    side: THREE.DoubleSide,
    roughness: 0.62,
  });
  const mesh = new THREE.Mesh(geometry, material);
  mesh.rotation.x = -Math.PI / 2;
  mesh.position.y = floorY(summary, layout) + 0.075;
  scene.add(mesh);

  if (isActive && isTenant) {
    const raisedGeometry = new THREE.ExtrudeGeometry(shape, {
      depth: type === 'RESTROOM' ? 0.3 : 0.22,
      bevelEnabled: true,
      bevelSize: 0.025,
      bevelThickness: 0.018,
      bevelSegments: 1,
    });
    const raised = new THREE.Mesh(
      raisedGeometry,
      new THREE.MeshStandardMaterial({
        color: SPACE_COLORS[type] || SPACE_COLORS.ROOM,
        transparent: true,
        opacity: type === 'SERVICE' ? 0.42 : 0.58,
        roughness: 0.52,
      }),
    );
    raised.rotation.x = -Math.PI / 2;
    raised.position.y = floorY(summary, layout) + 0.11;
    raised.receiveShadow = true;
    scene.add(raised);
    addSpaceTrim(scene, space, summary, layout, accent, type === 'RESTROOM' ? 0.45 : 0.36);
  }

  const outlineGeometry = new THREE.BufferGeometry().setFromPoints(
    space.polygon.map((point) => mapPoint(point, layout, summary.id, 0.1)),
  );
  const outline = new THREE.LineLoop(outlineGeometry, new THREE.LineBasicMaterial({
    color: isActive ? accent : 0xcbd5e1,
    transparent: true,
    opacity: isActive ? (isTenant ? 0.7 : 0.42) : 0.18,
  }));
  scene.add(outline);

  if (isActive && space.centroid?.length) {
    if (isConnector) {
      addConnectorFloorBadge(scene, space.centroid, summary, layout, space.name || 'Connector');
    } else if (String(space.name || '').toLowerCase().includes('thong tang')
      || String(space.name || '').toLowerCase().includes('thông tầng')
      || String(space.name || '').toLowerCase().includes('atrium')) {
      addAtriumFeature(scene, space, summary, layout);
    }
  }
}

function addSpaceTrim(scene, space, summary, layout, color, lift) {
  const polygon = space.polygon || [];
  for (let index = 0; index < polygon.length; index++) {
    const start = mapPoint(polygon[index], layout, summary.id, lift);
    const end = mapPoint(polygon[(index + 1) % polygon.length], layout, summary.id, lift);
    const length = start.distanceTo(end);
    if (length < 0.35) {
      continue;
    }
    const geometry = new THREE.TubeGeometry(new THREE.LineCurve3(start, end), 1, 0.035, 8, false);
    const material = new THREE.MeshStandardMaterial({
      color,
      transparent: true,
      opacity: 0.82,
      roughness: 0.42,
    });
    scene.add(new THREE.Mesh(geometry, material));
  }
}

function projectRatioOnWall(wall, point) {
  const start = wall.start;
  const end = wall.end;
  const dx = end[0] - start[0];
  const dy = end[1] - start[1];
  const lengthSq = dx * dx + dy * dy;
  if (!lengthSq || !point?.length) {
    return 0;
  }
  return ((point[0] - start[0]) * dx + (point[1] - start[1]) * dy) / lengthSq;
}

function mergeOpeningRanges(ranges) {
  const merged = [];
  ranges
    .filter((range) => range.end > 0 && range.start < 1)
    .map((range) => ({
      start: Math.max(0, range.start),
      end: Math.min(1, range.end),
    }))
    .sort((a, b) => a.start - b.start)
    .forEach((range) => {
      const previous = merged[merged.length - 1];
      if (previous && range.start <= previous.end) {
        previous.end = Math.max(previous.end, range.end);
      } else {
        merged.push(range);
      }
    });
  return merged;
}

function pointAlongWall(wall, ratio) {
  return [
    wall.start[0] + (wall.end[0] - wall.start[0]) * ratio,
    wall.start[1] + (wall.end[1] - wall.start[1]) * ratio,
  ];
}

function addWallSegment(scene, wall, start, end, summary, layout, isActive) {
  if (!start?.length || !end?.length) {
    return;
  }
  const dx = (end[0] - start[0]) * MAP_SCALE;
  const dz = (end[1] - start[1]) * MAP_SCALE;
  const length = Math.sqrt(dx * dx + dz * dz);
  const thickness = Math.max((wall.thickness || 10) * MAP_SCALE, 0.12);
  const height = isActive ? Math.max((wall.height || 4) * HEIGHT_SCALE, 0.95) : 0.18;
  const geometry = new THREE.BoxGeometry(length, height, thickness);
  const material = new THREE.MeshStandardMaterial({
    color: isActive ? 0x334155 : 0x94a3b8,
    transparent: true,
    opacity: isActive ? 0.82 : 0.13,
    roughness: 0.66,
    depthWrite: true,
  });
  const mesh = new THREE.Mesh(geometry, material);
  mesh.position.set(
    (((start[0] + end[0]) / 2) - layout.centerX) * MAP_SCALE,
    floorY(summary, layout) + height / 2,
    (((start[1] + end[1]) / 2) - layout.centerZ) * MAP_SCALE,
  );
  mesh.rotation.y = -Math.atan2(dz, dx);
  scene.add(mesh);
}

function addWall(scene, wall, openings, summary, layout, isActive) {
  const length = Math.hypot(wall.end[0] - wall.start[0], wall.end[1] - wall.start[1]);
  const gapRanges = mergeOpeningRanges((openings || []).map((opening) => {
    const center = projectRatioOnWall(wall, opening.center);
    const halfGap = ((opening.width || 70) + 22) / (2 * Math.max(length, 1));
    return { start: center - halfGap, end: center + halfGap };
  }));

  let cursor = 0;
  gapRanges.forEach((range) => {
    if (range.start - cursor > 0.015) {
      addWallSegment(scene, wall, pointAlongWall(wall, cursor), pointAlongWall(wall, range.start), summary, layout, isActive);
    }
    cursor = Math.max(cursor, range.end);
  });
  if (1 - cursor > 0.015) {
    addWallSegment(scene, wall, pointAlongWall(wall, cursor), pointAlongWall(wall, 1), summary, layout, isActive);
  }
}

function addOpeningVisual(scene, opening, wall, summary, layout, isActive) {
  if (!opening?.center?.length || !wall?.start?.length || !wall?.end?.length) {
    return;
  }
  const wallDx = wall.end[0] - wall.start[0];
  const wallDz = wall.end[1] - wall.start[1];
  const wallLength = Math.hypot(wallDx, wallDz);
  if (!wallLength) {
    return;
  }
  const width = Math.max((opening.width || 72) * MAP_SCALE, 1.15);
  const angle = -Math.atan2(wallDz * MAP_SCALE, wallDx * MAP_SCALE);
  const dir = new THREE.Vector3(wallDx / wallLength, 0, wallDz / wallLength);
  const normal = new THREE.Vector3(-dir.z, 0, dir.x);
  const center = mapPoint(opening.center, layout, summary.id, 0);
  const isDoor = opening.type === 'DOOR';
  const accent = isDoor ? 0xf59e0b : 0x22c55e;
  const opacity = isActive ? 1 : 0.28;

  addBox(scene, { x: width, y: 0.075, z: 0.58 }, {
    x: center.x + normal.x * 0.04,
    y: floorY(summary, layout) + 0.18,
    z: center.z + normal.z * 0.04,
  }, accent, {
    opacity: isActive ? 0.92 : 0.22,
    rotationY: angle,
    roughness: 0.34,
  });

  const jambOffset = width / 2 + 0.05;
  [1, -1].forEach((side) => {
    addBox(scene, { x: 0.08, y: isDoor ? 1.0 : 0.52, z: 0.15 }, {
      x: center.x + dir.x * jambOffset * side,
      y: floorY(summary, layout) + (isDoor ? 0.68 : 0.42),
      z: center.z + dir.z * jambOffset * side,
    }, isDoor ? 0x92400e : 0x15803d, {
      opacity,
      rotationY: angle,
      roughness: 0.48,
    });
  });

  if (isDoor) {
    addBox(scene, { x: width * 0.86, y: 0.78, z: 0.04 }, {
      x: center.x - normal.x * 0.035,
      y: floorY(summary, layout) + 0.75,
      z: center.z - normal.z * 0.035,
    }, 0xbae6fd, {
      opacity: isActive ? 0.42 : 0.12,
      rotationY: angle,
      roughness: 0.18,
      metalness: 0.04,
    });
    if (isActive) {
      addIconSprite(scene, 'IN', mapPoint(opening.center, layout, summary.id, 1.35), {
        fill: '#fffbeb',
        stroke: '#f59e0b',
        textColor: '#92400e',
        fontSize: 34,
        scale: 0.9,
      });
    }
  }
}

function addMarker(scene, position, summary, layout, color, radius = 0.42, isActive = true) {
  if (!position?.length) {
    return;
  }
  const geometry = new THREE.SphereGeometry(radius, 24, 16);
  const material = new THREE.MeshStandardMaterial({
    color,
    emissive: color,
    emissiveIntensity: isActive ? 0.18 : 0.02,
    transparent: true,
    opacity: isActive ? 1 : 0.32,
  });
  const marker = new THREE.Mesh(geometry, material);
  marker.position.copy(mapPoint(position, layout, summary.id, isActive ? 0.82 : 0.36));
  scene.add(marker);
}

function addCylinderMarker(scene, position, summary, layout, color, isActive = true) {
  if (!position?.length) {
    return;
  }
  const geometry = new THREE.CylinderGeometry(0.28, 0.36, isActive ? 1.3 : 0.48, 24);
  const material = new THREE.MeshStandardMaterial({
    color,
    roughness: 0.42,
    transparent: true,
    opacity: isActive ? 1 : 0.34,
  });
  const marker = new THREE.Mesh(geometry, material);
  marker.position.copy(mapPoint(position, layout, summary.id, isActive ? 0.72 : 0.3));
  scene.add(marker);
}

function poiIcon(poi) {
  const category = poi.category || '';
  if (category === 'RESTROOM') return { text: 'WC', fill: '#dcfce7', stroke: '#16a34a', textColor: '#166534', color: 0x16a34a };
  if (category === 'CAFE') return { text: 'CF', fill: '#fef3c7', stroke: '#d97706', textColor: '#92400e', color: 0xd97706 };
  if (category === 'FOOD_COURT') return { text: 'FO', fill: '#ffedd5', stroke: '#ea580c', textColor: '#9a3412', color: 0xea580c };
  if (category === 'PHARMACY') return { text: '+', fill: '#fee2e2', stroke: '#dc2626', textColor: '#991b1b', color: 0xdc2626 };
  if (category === 'BOOK_STORE') return { text: 'BK', fill: '#e0f2fe', stroke: '#0284c7', textColor: '#075985', color: 0x0284c7 };
  if (category === 'ELECTRONICS_STORE') return { text: 'EL', fill: '#e0e7ff', stroke: '#4f46e5', textColor: '#3730a3', color: 0x4f46e5 };
  if (category === 'SUPERMARKET') return { text: 'MK', fill: '#dcfce7', stroke: '#15803d', textColor: '#14532d', color: 0x15803d };
  if (category === 'LANDMARK') return { text: 'i', fill: '#dbeafe', stroke: '#2563eb', textColor: '#1d4ed8', color: 0x2563eb };
  return { text: 'SH', fill: '#ffe4e6', stroke: '#e11d48', textColor: '#9f1239', color: 0xe11d48 };
}

function addPoiMarker(scene, poi, summary, layout, isActive) {
  if (!poi.displayAnchor?.length) {
    return;
  }
  const icon = poiIcon(poi);
  const base = mapPoint(poi.displayAnchor, layout, summary.id, 0.18);
  const ring = new THREE.Mesh(
    new THREE.CylinderGeometry(0.36, 0.36, 0.055, 32),
    new THREE.MeshStandardMaterial({
      color: icon.color,
      transparent: true,
      opacity: isActive ? 0.34 : 0.12,
      roughness: 0.44,
    }),
  );
  ring.position.copy(base);
  scene.add(ring);

  const pin = new THREE.Mesh(
    new THREE.CylinderGeometry(0.08, 0.13, isActive ? 0.62 : 0.28, 20),
    new THREE.MeshStandardMaterial({
      color: icon.color,
      emissive: icon.color,
      emissiveIntensity: isActive ? 0.08 : 0,
      transparent: true,
      opacity: isActive ? 0.94 : 0.26,
      roughness: 0.38,
    }),
  );
  pin.position.copy(mapPoint(poi.displayAnchor, layout, summary.id, isActive ? 0.52 : 0.28));
  scene.add(pin);

  if (isActive) {
    addIconSprite(scene, icon.text, mapPoint(poi.displayAnchor, layout, summary.id, 1.2), {
      fill: icon.fill,
      stroke: icon.stroke,
      textColor: icon.textColor,
      fontSize: icon.text.length > 1 ? 36 : 50,
      scale: 1.18,
    });
  }
}

function addConnectorObject(scene, connector, summary, layout, isActive = true) {
  if (!connector.anchor?.length) {
    return;
  }
  const type = connector.type || 'ELEVATOR';
  const center = worldPointFromPlan(connector.anchor, layout, summary.id, 0);
  if (type === 'ELEVATOR') {
    addBox(scene, { x: 0.72, y: isActive ? 1.55 : 0.42, z: 0.72 }, {
      x: center.x,
      y: floorY(summary, layout) + (isActive ? 0.85 : 0.3),
      z: center.z,
    }, 0x2563eb, {
      opacity: isActive ? 0.2 : 0.08,
      roughness: 0.2,
      metalness: 0.08,
    });
    addBox(scene, { x: 0.54, y: 0.06, z: 0.54 }, {
      x: center.x,
      y: floorY(summary, layout) + 0.22,
      z: center.z,
    }, 0x1d4ed8, { opacity: isActive ? 0.82 : 0.2 });
    if (isActive) {
      addIconSprite(scene, 'E', mapPoint(connector.anchor, layout, summary.id, 1.78), {
        fill: '#dbeafe',
        stroke: '#2563eb',
        textColor: '#1d4ed8',
        scale: 1.18,
      });
    }
  } else if (type === 'ESCALATOR') {
    addBox(scene, { x: 1.18, y: 0.14, z: 0.42 }, {
      x: center.x,
      y: floorY(summary, layout) + 0.38,
      z: center.z,
    }, 0xf59e0b, {
      opacity: isActive ? 0.8 : 0.24,
      rotationY: Math.PI * 0.2,
      roughness: 0.32,
    });
    addBox(scene, { x: 1.25, y: 0.055, z: 0.08 }, {
      x: center.x,
      y: floorY(summary, layout) + 0.54,
      z: center.z - 0.23,
    }, 0x78350f, {
      opacity: isActive ? 0.72 : 0.2,
      rotationY: Math.PI * 0.2,
    });
    if (isActive) {
      addIconSprite(scene, 'ES', mapPoint(connector.anchor, layout, summary.id, 1.22), {
        fill: '#fef3c7',
        stroke: '#d97706',
        textColor: '#92400e',
        fontSize: 35,
        scale: 1.05,
      });
    }
  } else {
    for (let index = 0; index < 4; index++) {
      addBox(scene, { x: 0.58 - index * 0.06, y: 0.08, z: 0.36 }, {
        x: center.x,
        y: floorY(summary, layout) + 0.22 + index * 0.09,
        z: center.z + index * 0.09,
      }, 0x64748b, { opacity: isActive ? 0.76 : 0.2 });
    }
    if (isActive) {
      addIconSprite(scene, 'ST', mapPoint(connector.anchor, layout, summary.id, 1.08), {
        fill: '#f1f5f9',
        stroke: '#64748b',
        textColor: '#334155',
        fontSize: 35,
        scale: 1.05,
      });
    }
  }
}

function addKioskObject(scene, kiosk, summary, layout, isActive = true) {
  if (!kiosk.position?.length) {
    return;
  }
  const center = worldPointFromPlan(kiosk.position, layout, summary.id, 0);
  addBox(scene, { x: 0.42, y: isActive ? 0.96 : 0.34, z: 0.3 }, {
    x: center.x,
    y: floorY(summary, layout) + (isActive ? 0.55 : 0.28),
    z: center.z,
  }, 0xf59e0b, {
    opacity: isActive ? 0.94 : 0.28,
    roughness: 0.36,
  });
  if (isActive) {
    addBox(scene, { x: 0.5, y: 0.34, z: 0.035 }, {
      x: center.x,
      y: floorY(summary, layout) + 0.9,
      z: center.z - 0.17,
    }, 0x172033, {
      opacity: 0.95,
      roughness: 0.22,
    });
    addIconSprite(scene, 'i', mapPoint(kiosk.position, layout, summary.id, 1.4), {
      fill: '#fef3c7',
      stroke: '#d97706',
      textColor: '#92400e',
      fontSize: 52,
      scale: 1.0,
    });
  }
}

function addSpaceFurniture(scene, space, summary, layout, isActive, index) {
  if (!isActive || !space.centroid?.length || !space.polygon?.length) {
    return;
  }
  const type = space.type || 'ROOM';
  const center = worldPointFromPlan(space.centroid, layout, summary.id, 0);
  const bounds = polygonBounds(space.polygon);
  const width = bounds.maxX - bounds.minX;
  const height = bounds.maxY - bounds.minY;
  const longAlongX = width >= height;
  const rotationY = longAlongX ? 0 : Math.PI / 2;

  if (type === 'CORRIDOR' && index % 2 === 0) {
    addBox(scene, { x: 0.74, y: 0.08, z: 0.2 }, {
      x: center.x,
      y: floorY(summary, layout) + 0.25,
      z: center.z,
    }, 0x94a3b8, {
      opacity: 0.72,
      rotationY,
      roughness: 0.5,
    });
    addBox(scene, { x: 0.08, y: 0.22, z: 0.2 }, {
      x: center.x + (longAlongX ? -0.26 : 0),
      y: floorY(summary, layout) + 0.39,
      z: center.z + (longAlongX ? 0 : -0.26),
    }, 0x64748b, { opacity: 0.58, rotationY });
    addPlanter(scene, [space.centroid[0] + (longAlongX ? 120 : 0), space.centroid[1] + (longAlongX ? 0 : 120)], summary, layout);
  }

  if (type === 'CONNECTOR') {
    const marker = new THREE.Mesh(
      new THREE.RingGeometry(0.52, 0.68, 40),
      new THREE.MeshBasicMaterial({ color: 0x2563eb, transparent: true, opacity: 0.28, side: THREE.DoubleSide }),
    );
    marker.rotation.x = -Math.PI / 2;
    marker.position.copy(mapPoint(space.centroid, layout, summary.id, 0.16));
    scene.add(marker);
  }
}

function addPlanter(scene, position, summary, layout) {
  const base = mapPoint(position, layout, summary.id, 0.22);
  const pot = new THREE.Mesh(
    new THREE.CylinderGeometry(0.16, 0.2, 0.2, 18),
    new THREE.MeshStandardMaterial({ color: 0x92400e, roughness: 0.62 }),
  );
  pot.position.copy(base);
  scene.add(pot);
  const leaves = new THREE.Mesh(
    new THREE.SphereGeometry(0.24, 16, 10),
    new THREE.MeshStandardMaterial({ color: 0x22c55e, roughness: 0.58 }),
  );
  leaves.position.copy(mapPoint(position, layout, summary.id, 0.48));
  scene.add(leaves);
}

function addRouteGroup(scene, routePoints, layout) {
  if (!routePoints.length) {
    return;
  }
  const points = routePoints.map((point) => new THREE.Vector3(
    (point.x - layout.centerX) * MAP_SCALE,
    (layout.yByFloorId.get(point.floorId) ?? 0) + ROUTE_LIFT,
    (point.y - layout.centerZ) * MAP_SCALE,
  ));

  const material = new THREE.MeshBasicMaterial({
    color: 0xf97316,
    transparent: true,
    opacity: 0.94,
  });
  for (let index = 0; index < points.length - 1; index++) {
    const curve = new THREE.LineCurve3(points[index], points[index + 1]);
    const geometry = new THREE.TubeGeometry(curve, 3, 0.095, 12, false);
    scene.add(new THREE.Mesh(geometry, material));
  }

  points.forEach((point) => {
    const pulse = new THREE.Mesh(
      new THREE.SphereGeometry(0.22, 18, 12),
      new THREE.MeshStandardMaterial({ color: 0xf97316, emissive: 0xf97316, emissiveIntensity: 0.22 }),
    );
    pulse.position.copy(point);
    scene.add(pulse);
  });
}

function addRoute(scene, route, layout, visibleFloorIds) {
  if (!route?.polyline?.length) {
    return;
  }

  const groups = [];
  let current = [];
  route.polyline.forEach((point) => {
    if (visibleFloorIds.has(point.floorId)) {
      current.push(point);
    } else if (current.length) {
      groups.push(current);
      current = [];
    }
  });
  if (current.length) {
    groups.push(current);
  }
  groups.forEach((group) => addRouteGroup(scene, group, layout));
}

export default function MapScene({ floors, activeFloorId, route, showAllFloors = false, viewMode = 'tilt' }) {
  const containerRef = useRef(null);
  const rendererRef = useRef(null);
  const animationRef = useRef(null);

  const sceneData = useMemo(() => {
    const entries = Object.values(floors || {}).filter((entry) => entry?.map && entry?.summary);
    const sorted = entries.sort((a, b) => a.summary.floorNumber - b.summary.floorNumber);
    if (showAllFloors || !activeFloorId) {
      return sorted;
    }
    const activeOnly = sorted.filter((entry) => entry.summary.id === activeFloorId);
    return activeOnly.length ? activeOnly : sorted;
  }, [floors, activeFloorId, showAllFloors]);

  const sceneLayout = useMemo(() => createLayout(sceneData), [sceneData]);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) {
      return undefined;
    }

    const scene = new THREE.Scene();
    scene.background = new THREE.Color(0xf7fafc);
    scene.fog = new THREE.Fog(0xf7fafc, 78, 178);

    const camera = new THREE.PerspectiveCamera(viewMode === 'top' ? 42 : 39, container.clientWidth / container.clientHeight, 0.1, 1000);
    const cameraRadius = Math.max(sceneLayout.width, sceneLayout.depth, 24);
    if (viewMode === 'top') {
      camera.position.set(0, Math.max(56, cameraRadius * 1.36 + sceneLayout.maxY), 0.01);
    } else {
      camera.position.set(cameraRadius * 0.45, sceneLayout.maxY + 20, cameraRadius * 0.58);
    }

    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false, preserveDrawingBuffer: true });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setSize(container.clientWidth, container.clientHeight);
    renderer.shadowMap.enabled = true;
    rendererRef.current = renderer;
    container.innerHTML = '';
    container.appendChild(renderer.domElement);

    const controls = new OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.dampingFactor = 0.06;
    controls.target.set(0, sceneLayout.maxY * 0.35, 0);
    controls.enablePan = true;
    controls.enableRotate = viewMode !== 'top';
    controls.panSpeed = 0.75;
    controls.zoomSpeed = 0.72;
    controls.maxPolarAngle = viewMode === 'top' ? Math.PI * 0.08 : Math.PI * 0.49;
    controls.minDistance = 14;
    controls.maxDistance = 120;

    const hemi = new THREE.HemisphereLight(0xffffff, 0xb6c2d4, 2.3);
    scene.add(hemi);
    const key = new THREE.DirectionalLight(0xffffff, 2.4);
    key.position.set(35, 55, 24);
    scene.add(key);
    const fill = new THREE.DirectionalLight(0xaad8ff, 0.8);
    fill.position.set(-24, 28, -18);
    scene.add(fill);

    const gridSize = Math.max(sceneLayout.width, sceneLayout.depth, 32) + 12;
    const grid = new THREE.GridHelper(gridSize, 24, 0xaab4c3, 0xd8dee8);
    grid.position.set(0, -0.08, 0);
    scene.add(grid);

    if (sceneData.length === 0) {
      const empty = new THREE.Mesh(
        new THREE.TorusKnotGeometry(2.8, 0.22, 96, 12),
        new THREE.MeshStandardMaterial({ color: 0x2f7df6, roughness: 0.35 }),
      );
      empty.position.set(0, 3, 0);
      scene.add(empty);
    }

    sceneData.forEach(({ summary, map }) => {
      const isActive = summary.id === activeFloorId || !activeFloorId;
      const y = floorY(summary, sceneLayout);
      const bounds = map.bounds || summary.bounds;
      const width = Math.max((bounds.maxX - bounds.minX) * MAP_SCALE, 4);
      const depth = Math.max((bounds.maxY - bounds.minY) * MAP_SCALE, 4);
      const slab = new THREE.Mesh(
        new THREE.BoxGeometry(width, SLAB_THICKNESS, depth),
        new THREE.MeshStandardMaterial({
          color: isActive ? 0xffffff : 0xe8edf5,
          transparent: true,
          opacity: isActive ? 0.94 : 0.16,
          roughness: 0.7,
        }),
      );
      slab.position.set(
        (((bounds.minX + bounds.maxX) * 0.5) - sceneLayout.centerX) * MAP_SCALE,
        y,
        (((bounds.minY + bounds.maxY) * 0.5) - sceneLayout.centerZ) * MAP_SCALE,
      );
      scene.add(slab);
      addTileLines(scene, bounds, summary, sceneLayout, isActive);

      const outlinePoints = [
        [bounds.minX, bounds.minY],
        [bounds.maxX, bounds.minY],
        [bounds.maxX, bounds.maxY],
        [bounds.minX, bounds.maxY],
      ].map((point) => mapPoint(point, sceneLayout, summary.id, SLAB_THICKNESS / 2 + 0.04));
      scene.add(new THREE.LineLoop(
        new THREE.BufferGeometry().setFromPoints(outlinePoints),
        new THREE.LineBasicMaterial({
          color: isActive ? 0x64748b : 0xcbd5e1,
          transparent: true,
          opacity: isActive ? 0.66 : 0.32,
        }),
      ));

      if (isActive) {
        addLabel(scene, summary.name, mapPoint([bounds.minX + 120, bounds.maxY - 76], sceneLayout, summary.id, 1.05), '#1e293b');
      }

      const openingsByWall = new Map();
      const wallById = new Map();
      map.walls?.forEach((wall) => wallById.set(wall.externalId, wall));
      map.openings?.forEach((opening) => {
        const values = openingsByWall.get(opening.wallExternalId) || [];
        values.push(opening);
        openingsByWall.set(opening.wallExternalId, values);
      });

      map.spaces?.forEach((space) => addSpace(scene, space, summary, sceneLayout, isActive));
      map.spaces?.forEach((space, index) => addSpaceFurniture(scene, space, summary, sceneLayout, isActive, index));
      map.walls?.forEach((wall) => addWall(scene, wall, openingsByWall.get(wall.externalId), summary, sceneLayout, isActive));
      map.openings?.forEach((opening) => addOpeningVisual(scene, opening, wallById.get(opening.wallExternalId), summary, sceneLayout, isActive));
      map.pois?.forEach((poi) => {
        addPoiMarker(scene, poi, summary, sceneLayout, isActive);
        if (isActive) {
          addLabel(scene, poi.name, mapPoint(poi.displayAnchor, sceneLayout, summary.id, 1.78), '#881337');
        }
      });
      map.connectors?.forEach((connector) => addConnectorObject(scene, connector, summary, sceneLayout, isActive));
      map.kiosks?.forEach((kiosk) => {
        addKioskObject(scene, kiosk, summary, sceneLayout, isActive);
        if (isActive) {
          addLabel(scene, kiosk.name, mapPoint(kiosk.position, sceneLayout, summary.id, 1.86), '#713f12');
        }
      });
    });

    addRoute(scene, route, sceneLayout, new Set(sceneData.map((entry) => entry.summary.id)));

    const resizeObserver = new ResizeObserver(() => {
      const width = container.clientWidth || 1;
      const height = container.clientHeight || 1;
      camera.aspect = width / height;
      camera.updateProjectionMatrix();
      renderer.setSize(width, height);
    });
    resizeObserver.observe(container);

    const animate = () => {
      controls.update();
      renderer.render(scene, camera);
      animationRef.current = requestAnimationFrame(animate);
    };
    animate();

    return () => {
      resizeObserver.disconnect();
      cancelAnimationFrame(animationRef.current);
      controls.dispose();
      renderer.dispose();
      scene.traverse((object) => {
        if (object.geometry) {
          object.geometry.dispose();
        }
        if (object.material) {
          const materials = Array.isArray(object.material) ? object.material : [object.material];
          materials.forEach((material) => {
            if (material.map) {
              material.map.dispose();
            }
            material.dispose();
          });
        }
      });
      if (renderer.domElement.parentNode) {
        renderer.domElement.parentNode.removeChild(renderer.domElement);
      }
    };
  }, [sceneData, sceneLayout, activeFloorId, route, viewMode]);

  return (
    <div className="scene-shell" ref={containerRef} aria-label="Khung bản đồ 3D trong nhà" />
  );
}
