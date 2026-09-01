const MapView = (() => {
    let map;
    let mapElement;
    let zoneLayer;
    let assetLayer;
    let safetyLayer;
    let exposureLayer;
    let selectZone;
    let activeMode = 'risk';
    let timelineStep = 3;
    let timelineTimer;
    let renderedState;
    const layers = {};
    const modeLabels = { risk: 'Risk overlay', rainfall: 'Rainfall overlay', terrain: 'Terrain context', exposure: 'Exposure overlay', roads: 'Road network', satellite: 'Satellite context' };

    function init(selectCallback) {
        selectZone = typeof selectCallback === 'function' ? selectCallback : () => {};
        mapElement = document.getElementById('risk-map');
        if (!mapElement) return;
        if (!window.L) {
            mapElement.classList.add('map-unavailable');
            mapElement.insertAdjacentHTML('afterbegin', '<div class="map-fallback"><strong>Map service unavailable</strong><span>Map controls will return when the Leaflet map service is reachable.</span></div>');
            addTimeline();
            return;
        }
        map = L.map(mapElement, { zoomControl: false, attributionControl: true, minZoom: 5, maxZoom: 18, worldCopyJump: false, scrollWheelZoom: true, touchZoom: true, dragging: true, tap: true, zoomAnimation: true, fadeAnimation: true }).setView([26.85, 93.7], 6);
        L.control.zoom({ position: 'bottomright' }).addTo(map);
        layers.terrain = L.tileLayer('https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png', { maxZoom: 17, attribution: 'OpenTopoMap', crossOrigin: true });
        layers.satellite = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', { maxZoom: 19, attribution: 'Esri World Imagery', crossOrigin: true });
        layers.roads = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, opacity: .88, attribution: 'OpenStreetMap', crossOrigin: true });
        layers.satellite.addTo(map); layers.roads.addTo(map);
        Object.values(layers).forEach(layer => layer.on('tileerror', () => mapElement.classList.add('map-tile-error')));
        zoneLayer = L.layerGroup().addTo(map); assetLayer = L.layerGroup().addTo(map); safetyLayer = L.layerGroup().addTo(map); exposureLayer = L.layerGroup().addTo(map);
        document.querySelectorAll('.map-control').forEach(button => button.addEventListener('click', () => { document.querySelectorAll('.map-control').forEach(item => item.classList.remove('active')); button.classList.add('active'); activeMode = button.dataset.mapMode; renderMode(activeMode); if (renderedState) render(renderedState); }));
        addMapActions();
        addTimeline();
        window.addEventListener('resize', resize);
    }

    function addTimeline() {
        const shell = mapElement.closest('.map-shell');
        if (!shell || shell.querySelector('.map-timeline')) return;
        const timeline = document.createElement('div');
        timeline.className = 'map-timeline';
        timeline.innerHTML = '<div><strong>TIME / HISTORY</strong><small id="timeline-label">Now · live simulation</small></div><button type="button" id="timeline-play" title="Play history">▶</button><input id="timeline-range" type="range" min="0" max="3" step="1" value="3" aria-label="Select map time"><div class="timeline-endpoints"><span>Past 24h</span><span>Now</span></div>';
        shell.append(timeline);
        const range = timeline.querySelector('#timeline-range');
        range.addEventListener('input', event => setTimeline(Number(event.target.value)));
        timeline.querySelector('#timeline-play').addEventListener('click', () => {
            if (timelineTimer) { clearInterval(timelineTimer); timelineTimer = null; timeline.querySelector('#timeline-play').textContent = 'Play'; return; }
            timeline.querySelector('#timeline-play').textContent = 'Pause';
            timelineTimer = setInterval(() => { const next = (timelineStep + 1) % 4; range.value = next; setTimeline(next); }, 1800);
        });
    }

    function setTimeline(step) {
        timelineStep = step;
        const labels = ['24 hours ago', '12 hours ago', '3 hours ago', 'Now'];
        const label = document.getElementById('timeline-label');
        if (label) label.textContent = `${labels[step]} · DEMO HISTORY`;
        if (!renderedState) return;
        const target = [42, 57, 71, 92][step];
        const selected = renderedState.zones.find(zone => zone.id === renderedState.selectedZoneId) || renderedState.zones[0];
        const current = selected.score || 1;
        const historical = renderedState.zones.map(zone => ({ ...zone, score: Math.max(0, Math.min(100, Math.round(zone.score + (target - current) * (zone.id === selected.id ? 1 : .35)))), level: undefined }));
        historical.forEach(zone => { zone.level = zone.score >= 75 ? 'Critical' : zone.score >= 55 ? 'High' : zone.score >= 35 ? 'Advisory' : 'Monitoring'; });
        render({ ...renderedState, zones: historical });
    }

    function addMapActions() {
        const actions = document.createElement('div'); actions.className = 'map-actions'; actions.innerHTML = '<button type="button" data-map-action="recenter" title="Recenter map">Center</button><button type="button" data-map-action="fullscreen" title="Toggle fullscreen">Expand</button>';
        mapElement.append(actions);
        actions.querySelector('[data-map-action="recenter"]').addEventListener('click', () => { const selected = renderedState?.zones.find(zone => zone.id === renderedState.selectedZoneId) || renderedState?.zones[0]; if (selected) map.flyTo(selected.coordinates, Math.max(map.getZoom(), 9), { duration: .7 }); else map.setView([26.85, 93.7], 6); });
        actions.querySelector('[data-map-action="fullscreen"]').addEventListener('click', toggleFullscreen);
    }

    function toggleFullscreen() { const shell = mapElement.closest('.map-shell'); shell?.classList.toggle('map-fullscreen'); document.body.classList.toggle('map-is-fullscreen', shell?.classList.contains('map-fullscreen')); resize(); }
    function resize() { if (map) requestAnimationFrame(() => map.invalidateSize({ animate: false, pan: false })); }
    function color(zone) { return zone.level === 'Critical' ? '#b93432' : zone.level === 'High' ? '#d36c36' : zone.level === 'Advisory' ? '#d5a03b' : '#4d9d69'; }
    function markerIcon(type, zone) { const labels = { critical: '!', high: '▲', advisory: '•', monitoring: '·', sensor: '⌁', settlement: '⌂', road: '━', infrastructure: '◆', mountain: '▲' }; return L.divIcon({ className: `gis-marker gis-${type}`, html: `<span>${labels[type] || '•'}</span><b>${zone?.name || type}</b>`, iconSize: [type === 'critical' || type === 'high' ? 92 : 74, 34], iconAnchor: [14, 17] }); }
    function riskPopup(zone) { const risk = zone.score ?? zone.risk_score ?? 0; return `<div class="gis-popup"><strong>${zone.name}</strong><span class="gis-popup-level" style="color:${color(zone)}">${zone.level || zone.risk_level || 'Monitoring'} · AI score ${risk}/100</span><dl><dt>Current status</dt><dd>${risk >= 75 ? 'Immediate verification recommended' : 'Monitoring active'}</dd><dt>Rainfall</dt><dd>${Number(zone.rainfall ?? zone.rain ?? 0).toFixed(1)} mm/hr</dd><dt>Slope</dt><dd>${zone.slope ?? 'Demo value'}%</dd><dt>Soil moisture</dt><dd>${Math.round(zone.moisture ?? zone.soil_moisture ?? 0)}%</dd><dt>Ground movement</dt><dd>${zone.ground_movement || 'Demo telemetry: stable'}</dd><dt>Population exposed</dt><dd>${zone.population_exposed || `${((zone.exposure || 0) * 150).toLocaleString()} (demo)`}</dd><dt>Road impact</dt><dd>${zone.road_impact || (risk >= 75 ? 'High' : 'Moderate')}</dd><dt>AI confidence</dt><dd>${zone.confidence ?? 'Demo'}%</dd></dl><small>DEMO/MOCK GIS record · Replace with reviewed backend geometry.</small></div>`; }
    function polygon(zone, radiusKm, options) { return L.circle(zone.coordinates, { radius: radiusKm * 1000, ...options }); }
    function hoverText(zone, radiusKm, label = 'Impact area') { const risk = zone.score ?? 0; return `<strong>${zone.name}</strong><br>${label}: ${radiusKm.toFixed(1)} km radius<br>Risk ${risk}/100 · ${zone.level || 'Monitoring'}`; }
    function mountainText(zone) { return `<strong>${zone.name}</strong><br>Mountain terrain / steep slope<br>Susceptibility: ${zone.susceptibility ?? 'Demo'}%`; }

    function render(state) {
        renderedState = state;
        const zones = Array.isArray(state?.zones) ? state.zones.filter(zone => Array.isArray(zone.coordinates) && zone.coordinates.length === 2) : [];
        const selected = zones.find(zone => zone.id === state.selectedZoneId) || zones[0]; if (!selected) return;
        const coordinates = document.getElementById('coordinates'); if (coordinates) coordinates.textContent = `${selected.coordinates[0].toFixed(4)}° N, ${selected.coordinates[1].toFixed(4)}° E`;
        renderLocationSwitcher(state); if (!map) return;
        zoneLayer.clearLayers(); assetLayer.clearLayers(); safetyLayer.clearLayers(); exposureLayer.clearLayers();
        const showRisk = activeMode === 'risk' || activeMode === 'exposure';
        zones.forEach(zone => {
            const isSelected = zone.id === selected.id; const riskRadius = zone.level === 'Critical' ? 18 : zone.level === 'High' ? 13 : zone.level === 'Advisory' ? 9 : 6;
            if (showRisk) { polygon(zone, riskRadius, { color: color(zone), weight: isSelected ? 3 : 1.5, dashArray: isSelected ? '7 6' : '4 7', fillColor: color(zone), fillOpacity: isSelected ? .18 : .08 }).bindPopup(riskPopup(zone)).bindTooltip(hoverText(zone, riskRadius), { sticky:true, className:'map-tooltip' }).addTo(zoneLayer); if (isSelected) polygon(zone, riskRadius * .56, { color: '#b93432', weight: 2, dashArray: '3 7', fillColor: '#b93432', fillOpacity: .16 }).bindPopup(riskPopup(zone)).bindTooltip(hoverText(zone, riskRadius * .56, 'Immediate impact area'), { sticky:true, className:'map-tooltip' }).addTo(safetyLayer); }
            if (activeMode === 'rainfall') { const rainfallRadius = Math.max(2, Number(zone.rainfall || 0) * .26); polygon(zone, rainfallRadius, { color: '#3e82a1', weight: 2, dashArray: '5 6', fillColor: '#4e9ab9', fillOpacity: .16 }).bindTooltip(hoverText(zone, rainfallRadius, 'Rainfall intensity'), { sticky:true, className:'map-tooltip' }).addTo(zoneLayer); }
            L.marker(zone.coordinates, { icon: markerIcon((zone.level || 'monitoring').toLowerCase(), zone), zIndexOffset: isSelected ? 500 : 200 }).bindPopup(riskPopup(zone)).bindTooltip(hoverText(zone, riskRadius, 'Risk zone'), { sticky:true, className:'map-tooltip' }).on('click', () => selectZone(zone.id)).addTo(zoneLayer);
            if (activeMode === 'terrain') L.marker([zone.coordinates[0] + .018, zone.coordinates[1] - .018], { icon: markerIcon('mountain', zone), zIndexOffset: 300 }).bindTooltip(mountainText(zone), { sticky:true, className:'map-tooltip' }).addTo(assetLayer);
        });
        const showAssets = activeMode === 'exposure' || activeMode === 'roads';
        if (showAssets) (state.infrastructure || []).forEach((asset, index) => { const zone = state.zones.find(item => item.id === asset.zone_id); if (!zone) return; const point = [zone.coordinates[0] - .01 * (index % 3 + 1), zone.coordinates[1] + .012 * ((index % 2) ? 1 : -1)]; const type = asset.type === 'road' ? 'road' : asset.type === 'village' ? 'settlement' : 'infrastructure'; const roadRisk = type === 'road' && zone.score >= 55; const roadStatus = asset.status || (roadRisk ? (zone.score >= 75 ? 'Blocked' : 'At Risk') : 'Open'); const roadLine = type === 'road' ? L.polyline([zone.coordinates, point], { color: roadRisk ? '#b93432' : '#6d8587', weight: roadRisk ? 5 : 3, dashArray: roadRisk ? '8 6' : '3 7', opacity: .9 }).addTo(assetLayer) : null; if (roadLine) roadLine.bindPopup(`<div class="gis-popup"><strong>${asset.name || 'Important road'}</strong><span>Road · ${roadRisk ? 'High risk' : 'Monitoring'}</span><dl><dt>Road name</dt><dd>${asset.name || 'Demo road'}</dd><dt>Risk level</dt><dd>${zone.level}</dd><dt>Affected length</dt><dd>${asset.affected_length_km || (roadRisk ? '2.4' : '0.0')} km</dd><dt>Status</dt><dd>${roadStatus}</dd><dt>Nearby incident</dt><dd>${zone.name}</dd></dl><small>DEMO/MOCK road impact record</small></div>`).bindTooltip(`<strong>${asset.name || 'Important road'}</strong><br>${roadStatus} · ${roadRisk ? 'At risk / blocked' : 'Open'}`, { sticky:true, className:'map-tooltip' }); L.marker(point, { icon: markerIcon(type) }).bindTooltip(`<strong>${asset.name || type}</strong><br>${type === 'road' ? `Road status: ${roadStatus}` : 'Exposed asset'}`, { sticky:true, className:'map-tooltip' }).bindPopup(type === 'road' ? `<div class="gis-popup"><strong>${asset.name || 'Important road'}</strong><span>Road · ${zone.level}</span><p>${roadStatus} · ${asset.affected_length_km || (roadRisk ? '2.4' : '0.0')} km affected · Nearby: ${zone.name}</p><small>DEMO/MOCK road impact record</small></div>` : `<div class="gis-popup"><strong>${asset.name || type}</strong><span>${asset.type || 'Infrastructure'} · ${asset.criticality || 'Operational'}</span><p>Status: ${asset.status || 'DEMO/MOCK'}</p></div>`).addTo(assetLayer); });
        if (showAssets) (state.sensors || []).forEach(sensor => { const zone = state.zones.find(item => item.id === sensor.zone_id); if (!zone) return; L.marker([zone.coordinates[0] + .006, zone.coordinates[1] + .006], { icon: markerIcon('sensor') }).bindPopup(`<div class="gis-popup"><strong>${sensor.id || 'Sensor station'}</strong><span>Sensor station · ${sensor.status || 'DEMO/MOCK'}</span><dl><dt>Rainfall</dt><dd>${Number(sensor.rainfall || 0).toFixed(1)} mm/hr</dd><dt>Soil moisture</dt><dd>${Math.round(sensor.soil_moisture || 0)}%</dd><dt>Temperature</dt><dd>${Number(sensor.temperature || 0).toFixed(1)} °C</dd></dl></div>`).addTo(assetLayer); });
        if (activeMode === 'exposure' && state.exposure?.features) L.geoJSON(state.exposure, { style: () => ({ color: '#c87422', weight: 2, dashArray: '4 6', fillColor: '#f0a33c', fillOpacity: .1 }), pointToLayer: (_, latlng) => L.circleMarker(latlng, { radius: 7, color: '#c87422', fillColor: '#f0a33c', fillOpacity: .9, weight: 2 }) }).addTo(exposureLayer);
        if (selected.id !== map._selectedZone) { map.flyTo(selected.coordinates, Math.max(map.getZoom(), 8), { duration: .7 }); map._selectedZone = selected.id; }
        resize();
        renderMode(activeMode);
    }

    function renderLocationSwitcher(state) { const switcher = document.getElementById('map-location-switcher'); if (!switcher) return; switcher.innerHTML = state.zones.map(zone => `<button class="map-place ${zone.id === state.selectedZoneId ? 'active' : ''}" data-map-zone="${zone.id}"><i class="place-dot" style="background:${color(zone)}"></i><span>${zone.name}</span><b>${zone.score}</b></button>`).join(''); switcher.querySelectorAll('[data-map-zone]').forEach(button => button.addEventListener('click', () => selectZone(button.dataset.mapZone))); }
    function renderMode(mode) { const label = document.getElementById('active-layer'); if (label) label.textContent = (modeLabels[mode] || mode).toUpperCase(); const legend = document.querySelector('.map-legend'); if (legend) { legend.dataset.activeLayer = mode; legend.innerHTML = mode === 'rainfall' ? '<span><i class="legend-dot rainfall-low"></i> Low rainfall</span><span><i class="legend-dot rainfall-medium"></i> Moderate rainfall</span><span><i class="legend-dot rainfall-high"></i> High rainfall</span><span><i class="legend-ring"></i> Intensity radius</span>' : mode === 'terrain' ? '<span><i class="legend-dot mountain"></i> Mountain terrain</span><span><i class="legend-dot high"></i> Steep slope</span><span><i class="legend-ring"></i> Terrain context</span>' : mode === 'exposure' ? '<span><i class="legend-dot settlement"></i> Settlement</span><span><i class="legend-dot asset"></i> Critical asset</span><span><i class="legend-dot hotspot"></i> Exposed population</span><span><i class="legend-ring"></i> Impact area</span>' : mode === 'roads' ? '<span><i class="legend-dot road-open"></i> Open</span><span><i class="legend-dot road-monitor"></i> Monitor</span><span><i class="legend-dot critical"></i> At risk / blocked</span><span><i class="legend-line"></i> Affected road</span>' : '<span><i class="legend-dot critical"></i> Critical 80–100</span><span><i class="legend-dot high"></i> High 60–79</span><span><i class="legend-dot advisory"></i> Advisory 40–59</span><span><i class="legend-dot monitoring"></i> Monitoring 0–39</span><span><i class="legend-ring"></i> Risk zone</span>'; } if (!map) return; [layers.terrain, layers.satellite, layers.roads].forEach(layer => { if (map.hasLayer(layer)) map.removeLayer(layer); }); if (mode === 'terrain') layers.terrain.addTo(map); else if (mode !== 'roads') layers.satellite.addTo(map); if (mode === 'roads' || mode === 'risk' || mode === 'rainfall' || mode === 'exposure') layers.roads.addTo(map); [zoneLayer, safetyLayer, exposureLayer, assetLayer].forEach(group => group?.eachLayer(layer => layer.bringToFront?.())); }
    return { init, render };
})();
