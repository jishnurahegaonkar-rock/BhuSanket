package com.bhusanket.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import org.json.JSONObject
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

private val Ink = Color(0xFF10191D)
private val Paper = Color(0xFFF3F5F1)
private val Orange = Color(0xFFC87422)
private val Red = Color(0xFFD34438)
private val Green = Color(0xFF4D9D69)
private val Amber = Color(0xFFD5A03B)
private val Muted = Color(0xFF6F7D7A)
internal data class Zone(val id: String, val name: String, val district: String, val score: Int, val rain: Double, val moisture: Int, val temp: Double, val totalRain: Int, val exposure: Int, val latitude: Double, val longitude: Double) { val level get() = when { score >= 75 -> "Critical"; score >= 55 -> "High"; score >= 35 -> "Advisory"; else -> "Monitoring" } }
internal val zoneData = listOf(Zone("tawang", "Tawang Corridor", "Tawang, Arunachal Pradesh", 92, 42.6, 76, 19.4, 184, 86, 27.4728, 94.9120), Zone("siang", "East Siang Valley", "Pasighat, Arunachal Pradesh", 52, 29.8, 64, 22.1, 138, 72, 28.0667, 95.3267), Zone("chura", "Churachandpur Ridge", "Churachandpur, Manipur", 41, 18.4, 51, 24.7, 96, 63, 24.3333, 93.6833), Zone("garo", "South Garo Hills", "Baghmara, Meghalaya", 28, 11.2, 39, 25.8, 67, 51, 25.4969, 90.6036), Zone("bomdila", "Bomdila Pass", "West Kameng, Arunachal Pradesh", 19, 7.4, 29, 14.8, 42, 32, 27.2648, 92.4246), Zone("ziro", "Ziro Valley", "Lower Subansiri, Arunachal Pradesh", 34, 14.6, 46, 20.6, 81, 58, 27.5444, 93.8197), Zone("roing", "Roing Foothills", "Lower Dibang Valley, Arunachal Pradesh", 47, 22.7, 58, 21.2, 119, 61, 28.1397, 95.8400), Zone("ukhrul", "Ukhrul Ridge", "Ukhrul, Manipur", 31, 13.1, 43, 22.8, 74, 44, 25.0968, 94.3614))
private enum class Screen { Situation, Map, Intelligence, Alerts, Notifications, Reports }

@Composable fun BhuSanketApp(forceWarning: Boolean = false) {
    var screen by remember { mutableStateOf(Screen.Situation) }; var selectedId by remember { mutableStateOf("tawang") }; var scenario by remember { mutableStateOf("Normal") }
    var historyStep by remember { mutableStateOf(3) }; var historyPlaying by remember { mutableStateOf(false) }
    var warningAcknowledged by remember { mutableStateOf(false) }; var selectionNonce by remember { mutableStateOf(0) }
    var liveData by remember { mutableStateOf(zoneData) }
    var notificationPayload by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { BhuSanketApi.loadZones()?.let { liveData = it } }
    LaunchedEffect(Unit) { notificationPayload = BhuSanketApi.loadNotifications() }
    LaunchedEffect(historyPlaying) { while (historyPlaying) { kotlinx.coroutines.delay(1800); historyStep = (historyStep + 1) % 4 } }
    val scenarioBoost = when (scenario) { "Heavy Rain" -> 12; "Extreme Rain" -> 24; "Recovery" -> -8; else -> 0 }
    val historyOffset = when (historyStep) { 0 -> -12; 1 -> -7; 2 -> -3; else -> 0 }
    val zones = liveData.map { base ->
        val score = ((if (base.id == "tawang") maxOf(base.score, 92) else base.score) + scenarioBoost + historyOffset).coerceIn(0, 100)
        base.copy(score = score, rain = (base.rain + scenarioBoost * .65 + historyOffset * .2).coerceAtLeast(0.0), moisture = (base.moisture + scenarioBoost / 3 + historyOffset / 3).coerceIn(0, 100), totalRain = (base.totalRain + scenarioBoost * 2 + historyOffset).coerceAtLeast(0))
    }
    val selected = zones.firstOrNull { it.id == selectedId } ?: zones.first()
    val context = LocalContext.current
    LaunchedEffect(selectionNonce) { if (selectionNonce > 0) { warningAcknowledged = false; if (selected.score >= 90) CriticalAlerts.notify(context, selected.name, selected.score) } }
    val selectZone: (String) -> Unit = { id -> selectedId = id; selectionNonce++ }
    MaterialTheme(colorScheme = androidx.compose.material3.lightColorScheme(primary = Orange, background = Paper)) { Scaffold(containerColor = Paper, topBar = { TopDeck { scenario = "Normal"; historyStep = 3; historyPlaying = false } }, bottomBar = { Navigation(screen) { screen = it } }) { inset -> Box(Modifier.fillMaxSize().padding(inset)) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) { when (screen) { Screen.Situation -> Dashboard(zones, selected, selected.id, scenario, selectZone, { scenario = it }, { screen = Screen.Map }); Screen.Map -> DedicatedMapScreen(zones, selected.id, scenario, selectZone, { scenario = it }, historyStep, { historyStep = it }, historyPlaying, { historyPlaying = !historyPlaying }); else -> Directory(screen, zones, selected, notificationPayload, selectZone) } }; if ((forceWarning || (selectionNonce > 0 && selected.score >= 90)) && !warningAcknowledged) CriticalWarning(selected) { warningAcknowledged = true } } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TopDeck(reset: () -> Unit) { TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink, titleContentColor = Color.White), title = { Column { Text("BHUSANKET", fontWeight = FontWeight.Bold, letterSpacing = 2.sp); Text("LANDSLIDE INTELLIGENCE NETWORK", fontSize = 9.sp, color = Color(0xFFB5C2BF)) } }, actions = { Column(horizontalAlignment = Alignment.End) { Text("● REGIONAL WATCH", color = Color(0xFF72D3B1), fontSize = 10.sp); Text("OPERATIONAL", color = Color(0xFFB5C2BF), fontSize = 9.sp) }; IconButton(onClick = reset) { Icon(Icons.Default.Refresh, "Reset simulation", tint = Color.White) } }) }
@Composable private fun Navigation(current: Screen, select: (Screen) -> Unit) { NavigationBar(containerColor = Ink) { listOf(Screen.Situation to Icons.Default.Dashboard, Screen.Map to Icons.Default.Map, Screen.Alerts to Icons.Default.Campaign, Screen.Notifications to Icons.Default.Notifications, Screen.Reports to Icons.Default.Report).forEach { (item, icon) -> NavigationBarItem(selected = current == item, onClick = { select(item) }, icon = { Icon(icon, item.name) }, label = { Text(item.name, fontSize = 8.sp) }) } } }
@Composable private fun Dashboard(zones: List<Zone>, selected: Zone, selectedId: String, scenario: String, select: (String) -> Unit, simulate: (String) -> Unit, openMap: () -> Unit) { Text("SITUATION ROOM / LIVE OPERATIONS", color = Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text("What is happening right now?", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Bold); Text("BhuSanket watches environmental conditions across priority landslide zones and turns change into action.", color = Muted, fontSize = 13.sp); Spacer(Modifier.height(14.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Stat("REGIONAL RISK", zones.map { it.score }.average().toInt().toString(), "/ 100", Modifier.weight(1f)); Stat("ACTIVE ALERTS", zones.count { it.score >= 55 }.toString().padStart(2, '0'), "needs action", Modifier.weight(1f)); Stat("PRIORITY", zones.maxBy { it.score }.name.substringBefore(' '), "verify now", Modifier.weight(1f)) }; Spacer(Modifier.height(12.dp)); MapSummary(selected, openMap); Spacer(Modifier.height(12.dp)); SimulationDashboard(scenario, simulate); Spacer(Modifier.height(12.dp)); Incident(selected); Spacer(Modifier.height(12.dp)); Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Telemetry(selected); Exposure(selected); RiskOutlook(selected); SystemStatus(); RiskChart(zones) }; Spacer(Modifier.height(12.dp)); Priority(zones) }

@Composable private fun MapSummary(selected: Zone, openMap: () -> Unit) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE1EAE3)), shape = RoundedCornerShape(6.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(54.dp).clip(CircleShape).background(color(selected.level)), contentAlignment = Alignment.Center) { Text(selected.score.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }; Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text("GEOSPATIAL SITUATION", color = Orange, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text("${selected.name} monitoring map", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp); Text("Open the dedicated map for layers, shelters, and impact zones.", color = Muted, fontSize = 11.sp); OutlinedButton(onClick = openMap, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)) { Icon(Icons.Default.Map, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("View map", fontSize = 11.sp) } } } } }

@Composable private fun RiskChart(zones: List<Zone>) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(6.dp)) { Column(Modifier.padding(14.dp)) { Text("REGIONAL RISK PROFILE", color = Orange, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text("Live zone comparison", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold); Row(Modifier.fillMaxWidth().height(140.dp).padding(top = 12.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceEvenly) { zones.forEach { zone -> Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) { Box(Modifier.height((zone.score.coerceAtLeast(8) * 1.05f).dp).width(12.dp).background(color(zone.level), RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))); Text(zone.name.substringBefore(' '), color = Muted, fontSize = 7.sp, maxLines = 1); Text(zone.score.toString(), color = Ink, fontSize = 8.sp, fontWeight = FontWeight.Bold) } } } } } }

@Composable private fun DedicatedMapScreen(zones: List<Zone>, selectedId: String, scenario: String, select: (String) -> Unit, simulate: (String) -> Unit, historyStep: Int, setHistoryStep: (Int) -> Unit, historyPlaying: Boolean, toggleHistory: () -> Unit) { Text("GEOSPATIAL COMMAND / MAP WORKSPACE", color = Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text("Map workspace", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Bold); Text("A focused map for place selection, risk layers, shelters, and response planning.", color = Muted, fontSize = 13.sp); Spacer(Modifier.height(12.dp)); MapCard(zones, selectedId, scenario, select, dedicated = true); Spacer(Modifier.height(12.dp)); HistoryControls(historyStep, setHistoryStep, historyPlaying, toggleHistory); Spacer(Modifier.height(12.dp)); MapLegend(); Spacer(Modifier.height(12.dp)); SimulationDashboard(scenario, simulate) }

@Composable private fun HistoryControls(step: Int, setStep: (Int) -> Unit, playing: Boolean, toggle: () -> Unit) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(4.dp)) { Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("TIME / HISTORY", color = Orange, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text(listOf("24 hours ago", "12 hours ago", "3 hours ago", "Now")[step], color = Ink, fontWeight = FontWeight.Bold) }; Button(onClick = toggle, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 2.dp)) { Text(if (playing) "Pause" else "Play") } }; Slider(value = step.toFloat(), onValueChange = { setStep(it.toInt()) }, valueRange = 0f..3f, steps = 2, enabled = !playing); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Past 24h", color = Muted, fontSize = 9.sp); Text("Now", color = Muted, fontSize = 9.sp) } } } }

@Composable private fun MapLegend() { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(6.dp)) { Column(Modifier.padding(14.dp)) { Text("MAP KEY / IMPACT SCALE", color = Orange, fontSize = 10.sp, fontWeight = FontWeight.Bold); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("Critical" to Red, "High" to Orange, "Advisory" to Amber, "Monitoring" to Green).forEach { (label, tint) -> Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(10.dp).clip(CircleShape).background(tint)); Spacer(Modifier.width(5.dp)); Text(label, color = Muted, fontSize = 9.sp) } } } } } }

@Composable private fun PlacePicker(zones: List<Zone>, selectedId: String, select: (String) -> Unit) { var expanded by remember { mutableStateOf(false) }; val selected = zones.first { it.id == selectedId }; Column { Text("JUMP TO MONITORED PLACE", color = Orange, fontSize = 10.sp, fontWeight = FontWeight.Bold); Box { Button(onClick = { expanded = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Ink)) { Text(selected.name); Text("  ▾", color = Orange) }; DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { zones.forEach { zone -> DropdownMenuItem(text = { Text("${zone.name}  ·  ${zone.score}") }, onClick = { select(zone.id); expanded = false }) } } } } }
@Composable private fun Stat(label: String, value: String, detail: String, modifier: Modifier = Modifier) { Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(3.dp)) { Column(Modifier.padding(10.dp)) { Text(label, color = Muted, fontSize = 8.sp, fontWeight = FontWeight.Bold); Text(value, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text(detail, color = Orange, fontSize = 9.sp) } } }
@Composable private fun MapCard(zones: List<Zone>, selectedId: String, scenario: String, select: (String) -> Unit, dedicated: Boolean = false) {
    var fullScreen by remember { mutableStateOf(dedicated) }
    var mapMode by remember { mutableStateOf("Risk") }
    var controlsOpen by remember { mutableStateOf(false) }
    val mapContent: @Composable (Modifier) -> Unit = { mapModifier ->
        AndroidView(
            modifier = mapModifier,
            factory = { context ->
                Configuration.getInstance().userAgentValue = context.packageName
                MapView(context).apply { setTileSource(mapTileSource("Risk")); setHorizontalMapRepetitionEnabled(false); setVerticalMapRepetitionEnabled(false); setScrollableAreaLimitDouble(BoundingBox(29.8, 97.7, 21.4, 88.0)); minZoomLevel = 5.0; maxZoomLevel = 11.0; setMultiTouchControls(false); controller.setZoom(6.0); controller.setCenter(GeoPoint(26.85, 93.7)) }
            },
            update = { map ->
                map.setTileSource(mapTileSource(mapMode))
                map.setMultiTouchControls(dedicated && !fullScreen)
                map.setOnTouchListener { _, event -> fullScreen || (!dedicated && event.actionMasked != android.view.MotionEvent.ACTION_UP) }
                map.overlays.clear()
                zones.forEach { zone ->
                    val point = GeoPoint(zone.latitude, zone.longitude)
                    val zoneColor = color(zone.level).toArgb()
                    val radius = when (mapMode) { "Rainfall" -> (zone.rain * 120.0).coerceIn(1800.0, 7200.0); "Exposure" -> (zone.exposure * 55.0).coerceIn(2200.0, 6200.0); else -> when { zone.score >= 75 -> 5000.0; zone.score >= 55 -> 3500.0; else -> 2200.0 } }
                    if (mapMode == "Risk" || mapMode == "Rainfall" || mapMode == "Exposure") map.overlays.add(Polygon().apply { points = impactRing(point, radius); fillColor = Color(zoneColor).copy(alpha = if (zone.id == selectedId) .25f else .10f).toArgb(); strokeColor = zoneColor; strokeWidth = if (zone.id == selectedId) 4f else 2f })
                    if (zone.id == selectedId && mapMode == "Risk" && zone.score >= 55) map.overlays.add(Polygon().apply { points = impactRing(point, radius * .55); fillColor = Color(Red.toArgb()).copy(alpha = .18f).toArgb(); strokeColor = Red.toArgb(); strokeWidth = 3f })
                    if (mapMode == "Roads") { val roadPoint = GeoPoint(zone.latitude - .018, zone.longitude + .024); map.overlays.add(Polyline().apply { setPoints(listOf(point, roadPoint)); color = if (zone.score >= 55) Red.toArgb() else Muted.toArgb(); width = if (zone.score >= 55) 7f else 4f; isGeodesic = true }) }
                    map.overlays.add(Marker(map).apply { position = point; title = "${zone.name} · ${zone.score}/100 · ${zone.level}"; snippet = "${zone.district}\nRain ${zone.rain} mm/hr · Soil ${zone.moisture}%\nAction: ${if (zone.score >= 55) "VERIFY NOW" else "MONITOR"}"; setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM); setOnMarkerClickListener { clicked, _ -> select(zone.id); clicked.showInfoWindow(); true } })
                    if (zone.id == selectedId) listOf(GeoPoint(zone.latitude + .012, zone.longitude - .012), GeoPoint(zone.latitude - .011, zone.longitude - .010)).forEachIndexed { index, shelter -> map.overlays.add(Marker(map).apply { position = shelter; title = "SAFE SHELTER ${index + 1}"; snippet = "Move here if evacuation is advised"; setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) }) }
                }
                map.invalidate()
                zones.firstOrNull { it.id == selectedId }?.let { focused -> if (map.tag != focused.id) { map.controller.animateTo(GeoPoint(focused.latitude, focused.longitude)); map.tag = focused.id } }
            },
            onRelease = { map -> map.onPause(); map.onDetach() }
        )
    }
    val controls: @Composable () -> Unit = {
        Text("GEOSPATIAL SITUATION", color = Orange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) { Text("Regional terrain view", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold); IconButton(onClick = { fullScreen = !fullScreen }) { Icon(if (fullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen, if (fullScreen) "Exit full screen map" else "Open full screen map", tint = Ink) } }
        Text("${mapMode.uppercase()} OVERLAY  ·  OPEN MAP DATA", color = Muted, fontSize = 9.sp)
        Spacer(Modifier.height(4.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { IconButton(onClick = { controlsOpen = !controlsOpen }) { Icon(if (controlsOpen) Icons.Default.Close else Icons.Default.Menu, if (controlsOpen) "Close map controls" else "Open map controls", tint = Ink) } }
        if (controlsOpen) { PlacePicker(zones, selectedId, select); Spacer(Modifier.height(3.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("Risk", "Rainfall", "Terrain", "Exposure", "Roads", "Satellite").forEach { mode -> Button(onClick = { mapMode = mode }, modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 1.dp, vertical = 0.dp), colors = ButtonDefaults.buttonColors(containerColor = if (mode == mapMode) Orange else Ink)) { Text(mode, fontSize = 8.sp) } } } }
    }
    @Composable fun MapSurface(modifier: Modifier) { Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFD8E1D9)), shape = RoundedCornerShape(3.dp), modifier = modifier) { Box(Modifier.fillMaxSize().padding(10.dp)) { mapContent(Modifier.fillMaxSize()); Text(if (fullScreen) "NER view locked" else "Preview · open View map for full-screen controls", Modifier.align(Alignment.BottomStart).padding(8.dp).background(Color.White.copy(alpha = .88f)).padding(6.dp), color = Muted, fontSize = 10.sp) } } }
    @Composable fun MapWorkspace(modifier: Modifier) { Column(modifier) { controls(); Spacer(Modifier.height(8.dp)); MapSurface(Modifier.fillMaxWidth().weight(1f)) } }
    if (fullScreen) Dialog(onDismissRequest = { fullScreen = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) { MapWorkspace(Modifier.fillMaxSize().padding(4.dp)) } else MapWorkspace(Modifier.fillMaxWidth().height(if (dedicated) 620.dp else 170.dp))
}
@Composable private fun Incident(zone: Zone) { Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(3.dp)) { Column(Modifier.padding(16.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("SELECTED INCIDENT", color = Orange, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(zone.name, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text(zone.district, color = Muted, fontSize = 12.sp) }; Badge(zone.level) }; Spacer(Modifier.height(14.dp)); Text(zone.score.toString(), color = color(zone.level), fontSize = 40.sp, fontWeight = FontWeight.Bold); Text("RISK SCORE  /  100", color = Muted, fontSize = 9.sp); Spacer(Modifier.height(10.dp)); Text("WHY IS IT RISKY?", color = Ink, fontWeight = FontWeight.Bold, fontSize = 11.sp); Text("Heavy rainfall and rising soil saturation are increasing landslide risk on steep, susceptible terrain.", color = Muted, fontSize = 12.sp) } } }
@Composable private fun Badge(level: String) { Text(level.uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.background(color(level), RoundedCornerShape(3.dp)).padding(8.dp)) }
@Composable private fun Scenario(active: String, simulate: (String) -> Unit) { Card(colors = CardDefaults.cardColors(containerColor = Ink), shape = RoundedCornerShape(3.dp)) { Column(Modifier.padding(14.dp)) { Text("SIMULATION / ESCALATION CONTROL", color = Color(0xFF72D3B1), fontSize = 10.sp, fontWeight = FontWeight.Bold); Text("Scenario inputs", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Normal", "Heavy Rain", "Extreme Rain", "Recovery").forEach { Button(onClick = { simulate(it) }, colors = ButtonDefaults.buttonColors(containerColor = if (it == active) Orange else Color(0xFF2A393D))) { Text(it, fontSize = 10.sp) } } } } } }
@Composable private fun SimulationDashboard(active: String, simulate: (String) -> Unit) { Card(colors = CardDefaults.cardColors(containerColor = Ink), shape = RoundedCornerShape(3.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text("SIMULATION / ESCALATION CONTROL", color = Color(0xFF72D3B1), fontSize = 10.sp, fontWeight = FontWeight.Bold); Text("Scenario dashboard", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold); Text("Model a changing weather pattern before response decisions are made.", color = Color(0xFFB5C2BF), fontSize = 12.sp); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("Normal", "Heavy Rain", "Extreme Rain", "Recovery").forEach { item -> Button(onClick = { simulate(item) }, modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp), colors = ButtonDefaults.buttonColors(containerColor = if (item == active) Orange else Color(0xFF2A393D))) { Text(item, fontSize = 9.sp) } } } } } }
@Composable private fun Telemetry(zone: Zone) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(3.dp)) { Column(Modifier.padding(14.dp)) { Text("ENVIRONMENTAL CONDITIONS", color = Orange, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text("Live telemetry", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold); Line("RAINFALL", "${zone.rain} mm/hr"); Line("SOIL MOISTURE", "${zone.moisture}%"); Line("TEMPERATURE", "${zone.temp} °C"); Line("ACCUMULATED", "${zone.totalRain} mm") } } }
@Composable private fun Exposure(zone: Zone) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(3.dp)) { Column(Modifier.padding(14.dp)) { Text("EXPOSURE SNAPSHOT", color = Orange, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text("What is at stake?", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("${(zone.exposure * .15).toInt()}K", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Bold); Text("People exposed", color = Muted, fontSize = 11.sp); Line("ROADS AT RISK", (zone.exposure / 28).coerceAtLeast(1).toString()); Line("CRITICAL ASSETS", (zone.exposure / 22).coerceAtLeast(1).toString()) } } }
@Composable private fun RiskOutlook(zone: Zone) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(3.dp)) { Column(Modifier.padding(14.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("RISK OUTLOOK", color = Orange, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text("Next 6 hours", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold) }; Text("RISING ↑", color = Red, fontWeight = FontWeight.Bold, fontSize = 11.sp) }; Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) { listOf("NOW" to zone.score, "+1H" to (zone.score + 4).coerceAtMost(100), "+3H" to (zone.score + 9).coerceAtMost(100), "+6H" to (zone.score + 15).coerceAtMost(100)).forEach { (time, score) -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(time, color = Muted, fontSize = 9.sp); Text(score.toString(), color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold) } } } } } }
@Composable private fun SystemStatus() { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(3.dp)) { Column(Modifier.padding(14.dp)) { Text("SYSTEM STATUS", color = Orange, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text("Operational health", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold); Line("● Simulation engine", "ONLINE"); Line("● Terrain model", "READY"); Line("● Field reports", "SYNCING") } } }
@Composable private fun Line(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = Muted, fontSize = 10.sp); Text(value, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
@Composable private fun Priority(zones: List<Zone>) { Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(3.dp)) { Column(Modifier.padding(14.dp)) { Text("EMERGENCY RESPONSE", color = Orange, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text("Where to act first", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold); zones.sortedByDescending { it.score * .65 + it.exposure * .35 }.take(3).forEachIndexed { i, zone -> Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Text("0${i + 1}", color = Orange, fontWeight = FontWeight.Bold); Spacer(Modifier.width(14.dp)); Text(zone.name, Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold); Text(if (zone.score >= 55) "VERIFY NOW" else "MONITOR", color = if (zone.score >= 55) Red else Green, fontSize = 10.sp, fontWeight = FontWeight.Bold) } } } } }
@Composable private fun Directory(screen: Screen, zones: List<Zone>, selected: Zone, notificationPayload: String?, onSelect: (String) -> Unit) { Text(screen.name.uppercase(), color = Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text(when (screen) { Screen.Intelligence -> "Zone intelligence"; Screen.Alerts -> "Alerts and response queue"; Screen.Notifications -> "Notification center"; Screen.Reports -> "Field reports"; else -> screen.name }, color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Bold); Text("Prototype operational view. Authentication is intentionally skipped for now.", color = Muted, fontSize = 13.sp); Spacer(Modifier.height(14.dp)); when (screen) { Screen.Intelligence -> zones.forEach { ZoneRow(it) }; Screen.Alerts -> zones.sortedByDescending { it.score }.forEach { AlertRow(it) }; Screen.Notifications -> NotificationList(notificationPayload); Screen.Reports -> ReportForm(zones, selected, onSelect) ; else -> Unit } }
@Composable private fun ZoneRow(zone: Zone) { Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(3.dp)) { Row(Modifier.padding(14.dp)) { Box(Modifier.size(10.dp).clip(CircleShape).background(color(zone.level))); Spacer(Modifier.width(12.dp)); Text(zone.name, Modifier.weight(1f), color = Ink, fontWeight = FontWeight.Bold); Text(zone.score.toString(), color = color(zone.level), fontWeight = FontWeight.Bold) } } }
@Composable private fun AlertRow(zone: Zone) { Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(3.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("${zone.name} risk detected", color = Ink, fontWeight = FontWeight.Bold); Text("Risk ${zone.score} · ${zone.rain} mm/hr · ${zone.moisture}% soil moisture", color = Muted, fontSize = 11.sp) }; Badge(zone.level) } } }
@Composable private fun NotificationList(payload: String?) { val rows = payload?.let { runCatching { org.json.JSONArray(it) }.getOrNull() }; if (rows == null || rows.length() == 0) { Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(3.dp)) { Column(Modifier.padding(20.dp)) { Text("No simulated notifications yet", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("Run Heavy Rain or Extreme Rain to generate an actionable alert.", color = Muted, fontSize = 13.sp) } } } else { for (index in 0 until rows.length()) { val row = rows.getJSONObject(index); Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(3.dp)) { Column(Modifier.padding(14.dp)) { Text(row.optString("channel", "SYSTEM").uppercase(), color = Orange, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(row.optString("message", "Notification received"), color = Ink, fontWeight = FontWeight.Bold); Text("${row.optString("status", "Simulated")} · ${row.optString("created_at", "")}", color = Muted, fontSize = 11.sp) } } } } }
@Composable private fun ReportForm(zones: List<Zone>, selected: Zone, onSelect: (String) -> Unit) { val context = LocalContext.current; var observation by remember { mutableStateOf("") }; var severity by remember { mutableStateOf("Advisory") }; var observedAt by remember { mutableStateOf(java.time.LocalDateTime.now().toString().take(16)) }; var evidenceName by remember { mutableStateOf("") }; var submitted by remember { mutableStateOf(false) }; val evidencePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> evidenceName = uri?.lastPathSegment ?: "Evidence selected" }; Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(3.dp)) { Column(Modifier.padding(16.dp)) { Text("NEW OBSERVATION", color = Orange, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text("Submit field report", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)); Text("LOCATION", color = Muted, fontSize = 10.sp); Row { zones.take(4).forEach { zone -> Button(onClick = { onSelect(zone.id) }, colors = ButtonDefaults.buttonColors(containerColor = if (zone.id == selected.id) Orange else Ink), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp)) { Text(zone.name.substringBefore(' '), fontSize = 9.sp) } } }; Spacer(Modifier.height(10.dp)); OutlinedTextField(value = observation, onValueChange = { observation = it }, modifier = Modifier.fillMaxWidth(), minLines = 4, label = { Text("Observation") }, placeholder = { Text("Describe the ground condition or visible evidence...") }); Spacer(Modifier.height(10.dp)); Text("SEVERITY", color = Muted, fontSize = 10.sp); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Advisory", "High", "Critical").forEach { value -> Button(onClick = { severity = value }, colors = ButtonDefaults.buttonColors(containerColor = if (severity == value) Orange else Ink), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)) { Text(value, fontSize = 11.sp) } } }; Spacer(Modifier.height(10.dp)); OutlinedTextField(value = observedAt, onValueChange = { observedAt = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Observed date and time") }); Spacer(Modifier.height(10.dp)); OutlinedButton(onClick = { evidencePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.CloudUpload, null); Spacer(Modifier.width(8.dp)); Text(if (evidenceName.isBlank()) "Upload evidence photo" else evidenceName) }; Spacer(Modifier.height(12.dp)); Button(onClick = { BhuSanketApi.queueReport(context, JSONObject().apply { put("zone_id", selected.id); put("location", selected.name); put("observation", observation.ifBlank { "Ground observation submitted for review." }); put("severity", severity); put("timestamp", observedAt); put("media_type", if (evidenceName.isBlank()) JSONObject.NULL else "image/*"); put("media_name", if (evidenceName.isBlank()) JSONObject.NULL else evidenceName); put("status", "Under review") }); BhuSanketApi.syncReports(context); submitted = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Report, null); Spacer(Modifier.width(8.dp)); Text("Submit observation") }; if (submitted) Text("Report saved and queued for synchronization.", color = Green, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) } } }
private fun color(level: String) = when (level) { "Critical" -> Red; "High" -> Orange; "Advisory" -> Amber; else -> Green }
private fun mapTileSource(mode: String) = when (mode) {
    "Satellite" -> object : OnlineTileSourceBase("Esri World Imagery", 1, 19, 256, "", arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/") ) {
        override fun getTileURLString(index: Long): String = getBaseUrl() + MapTileIndex.getZoom(index) + "/" + MapTileIndex.getY(index) + "/" + MapTileIndex.getX(index)
    }
    "Roads" -> TileSourceFactory.MAPNIK
    else -> TileSourceFactory.OpenTopo
}
private fun impactRing(center: GeoPoint, radiusMeters: Double): List<GeoPoint> = (0..36).map { index -> val angle = Math.toRadians(index * 10.0); GeoPoint(center.latitude + radiusMeters / 111000.0 * kotlin.math.sin(angle), center.longitude + radiusMeters / (111000.0 * kotlin.math.cos(Math.toRadians(center.latitude))) * kotlin.math.cos(angle)) }

@Composable private fun CriticalWarning(zone: Zone, onAcknowledge: () -> Unit) { val context = LocalContext.current; Box(Modifier.fillMaxSize().background(Color(0xF510191D)).padding(8.dp), contentAlignment = Alignment.Center) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(10.dp)) { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("⚠", color = Red, fontSize = 58.sp, fontWeight = FontWeight.Bold); Text("CRITICAL LANDSLIDE WARNING", color = Red, fontWeight = FontWeight.Bold, fontSize = 23.sp); Text("IMMEDIATE ACTION REQUIRED", color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp); Spacer(Modifier.height(16.dp)); Text("${zone.name}", color = Ink, fontWeight = FontWeight.Bold, fontSize = 22.sp); Text("RISK SCORE ${zone.score}/100", color = Red, fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(14.dp)); Text("EVACUATE NOW", color = Red, fontWeight = FontWeight.Bold, fontSize = 34.sp); Text("Move away from steep slopes, river channels, and the red impact area. Go to a green SAFE SHELTER marker immediately.", color = Ink, fontSize = 16.sp); Spacer(Modifier.height(20.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("AMBULANCE", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text("ETA 08 min", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp) }; Column(horizontalAlignment = Alignment.End) { Text("GROUND SUPPORT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text("ETA 12 min", color = Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp) } }; Spacer(Modifier.height(18.dp)); Button(onClick = onAcknowledge, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("I UNDERSTAND · VIEW SAFE ZONES", fontWeight = FontWeight.Bold, fontSize = 14.sp) }; Spacer(Modifier.height(8.dp)); OutlinedButton(onClick = { CriticalAlerts.openSms(context, zone.name, zone.score) }, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("SEND WARNING BY SMS") } } } } }
