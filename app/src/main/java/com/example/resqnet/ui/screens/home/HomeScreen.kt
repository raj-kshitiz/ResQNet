package com.example.resqnet.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.resqnet.domain.model.SosRequest
import com.example.resqnet.domain.model.SosStatus
import com.example.resqnet.ui.components.SOSButton
import com.example.resqnet.ui.theme.ResQNetTheme
import com.example.resqnet.util.LatLng
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTriggerSos: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToVolunteerHome: () -> Unit,
    sosViewModel: SosViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by sosViewModel.uiState.collectAsState()

    // ── Location permission launcher ──────────────────────────────────────
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) sosViewModel.initMap(context)
    }

    // On first composition: check / request permission then load map data
    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (fineGranted) {
            sosViewModel.initMap(context)
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ResQNet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Stay safe today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (uiState.userIsVolunteer) {
                        IconButton(onClick = onNavigateToVolunteerHome) {
                            Icon(
                                Icons.Default.SwapHoriz,
                                contentDescription = "Switch to Volunteer",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { }) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = true,
                    onClick = { }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    selected = false,
                    onClick = onNavigateToHistory
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = false,
                    onClick = onNavigateToProfile
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── OpenStreetMap — always visible, pans to GPS when ready ──────
            // Volunteers see SOS markers only when volunteer mode is on
            val visibleSosRequests = if (!uiState.userIsVolunteer || uiState.volunteerModeOn) {
                uiState.nearbyActiveSos
            } else {
                emptyList()
            }
            OsmMapView(
                userLocation = uiState.userLocation,
                activeSosRequests = visibleSosRequests,
                modifier = Modifier.fillMaxSize()
            )

            // ── Quick-info card (top overlay) ─────────────────────────────
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickStat(
                        label = "Active\nAlerts",
                        value = visibleSosRequests.size.toString()
                    )
                    QuickStat(label = "Avg Response\nTime", value = "~2 min")
                    QuickStat(
                        label = "My\nRequests",
                        value = uiState.myRequests.size.toString()
                    )
                }

                // Volunteer mode toggle — only visible for registered volunteers
                if (uiState.userIsVolunteer) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Volunteer Mode",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (uiState.volunteerModeOn) "Showing active SOS alerts"
                                       else "SOS alerts hidden",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.volunteerModeOn,
                            onCheckedChange = { sosViewModel.toggleVolunteerMode() },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }
                }
            }

            // ── FAB: re-center map ────────────────────────────────────────
            FloatingActionButton(
                onClick = { sosViewModel.initMap(context) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 120.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // ── SOS button (bottom centre) ────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Tap for emergency help",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                SOSButton(onClick = onTriggerSos)
            }
        }
    }
}

// ── OSMDroid Map Composable ──────────────────────────────────────────────────

@Composable
fun OsmMapView(
    userLocation: LatLng?,
    activeSosRequests: List<SosRequest>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val mapView = remember {
        Configuration.getInstance().userAgentValue = context.packageName
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(5.0)
            controller.setCenter(GeoPoint(20.5937, 78.9629))
        }
    }

    // Pan + zoom to user as soon as GPS is ready
    LaunchedEffect(userLocation) {
        if (userLocation != null) {
            mapView.controller.setZoom(15.0)
            mapView.controller.animateTo(GeoPoint(userLocation.latitude, userLocation.longitude))
        }
    }

    // Rebuild markers whenever location or SOS list changes
    LaunchedEffect(userLocation, activeSosRequests) {
        mapView.overlays.clear()

        // ── User location: blue pulsing dot ───────────────────────────────
        if (userLocation != null) {
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = GeoPoint(userLocation.latitude, userLocation.longitude)
                    title = "You are here"
                    icon = createUserDotDrawable(context)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
            )
        }

        // ── SOS markers: color-coded by status ────────────────────────────
        activeSosRequests.forEach { sos ->
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = GeoPoint(sos.latitude, sos.longitude)
                    title = "${sos.emergencyType.label} — ${sos.requesterName}"
                    snippet = sos.description ?: sos.addressHint ?: ""
                    icon = createPinDrawable(context, sosPinColor(sos.status))
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
            )
        }

        mapView.invalidate()
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause() }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

// ── Marker color by SOS status ────────────────────────────────────────────────

private fun sosPinColor(status: SosStatus): Int = when (status) {
    SosStatus.PENDING,
    SosStatus.NOTIFIED     -> android.graphics.Color.rgb(229, 57, 53)   // Red — active
    SosStatus.ACCEPTED,
    SosStatus.IN_PROGRESS  -> android.graphics.Color.rgb(255, 160, 0)   // Amber — help on the way
    SosStatus.RESOLVED     -> android.graphics.Color.rgb(56, 142, 60)   // Green — resolved
    else                   -> android.graphics.Color.rgb(97, 97, 97)    // Gray — other
}

// ── Custom drawable helpers ───────────────────────────────────────────────────

/**
 * Classic teardrop map pin. Circle at top, pointed tail below.
 * White stroke border + white center dot for a clean, minimalist look.
 */
private fun createPinDrawable(context: android.content.Context, fillColor: Int, sizeDp: Float = 32f): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val r = sizeDp / 2f * d          // circle radius
    val pad = 2.5f * d               // bitmap padding
    val bmpW = (r * 2 + pad * 2).toInt()
    val tipY = r * 2 + pad + r * 0.55f
    val bmpH = (tipY + pad).toInt()

    val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
    val cv = android.graphics.Canvas(bmp)

    val cx = bmpW / 2f
    val cy = r + pad                  // circle centre

    // Teardrop path: tip → lower-left → arc over top → lower-right → tip
    val path = Path().apply {
        moveTo(cx, bmpH - pad / 2f)
        lineTo(cx - r * 0.50f, cy + r * 0.866f)
        arcTo(RectF(cx - r, cy - r, cx + r, cy + r), 120f, 300f, false)
        lineTo(cx, bmpH - pad / 2f)
        close()
    }

    // Fill
    cv.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fillColor
        style = Paint.Style.FILL
    })

    // White border
    cv.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * d
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    })

    // White center dot
    cv.drawCircle(cx, cy, r * 0.27f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    })

    return BitmapDrawable(context.resources, bmp)
}

/**
 * Blue location dot with a translucent outer ring — standard "you are here" indicator.
 */
private fun createUserDotDrawable(context: android.content.Context, sizeDp: Float = 28f): BitmapDrawable {
    val d = context.resources.displayMetrics.density
    val outerR = sizeDp / 2f * d
    val bmpSize = (outerR * 2 + 1f).toInt()
    val bmp = Bitmap.createBitmap(bmpSize, bmpSize, Bitmap.Config.ARGB_8888)
    val cv = android.graphics.Canvas(bmp)
    val cx = bmpSize / 2f

    // Outer translucent ring
    cv.drawCircle(cx, cx, outerR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(55, 25, 118, 210)
    })

    // Inner solid blue circle
    val innerR = outerR * 0.64f
    cv.drawCircle(cx, cx, innerR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(25, 118, 210)
    })

    // White border
    cv.drawCircle(cx, cx, innerR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * d
    })

    return BitmapDrawable(context.resources, bmp)
}

@Composable
private fun QuickStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
