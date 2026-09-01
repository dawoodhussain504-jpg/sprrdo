package com.speedo.admin.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.speedo.admin.viewmodel.AdminViewModel
import com.speedo.core.components.SpeedoTopBar
import com.speedo.core.model.PopularDestination
import com.speedo.core.theme.*

@Composable
fun ManagePopularDestinationsScreen(
    viewModel: AdminViewModel,
    onMenuClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val destinations = uiState.popularDestinations

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var destinationToEdit by remember { mutableStateOf<PopularDestination?>(null) }
    var destinationToDelete by remember { mutableStateOf<PopularDestination?>(null) }

    LaunchedEffect(Unit) {
        viewModel.fetchPopularDestinations()
    }

    val categories = listOf("All", "Airport", "Metro", "Shopping", "Tech Park", "Dining", "Park", "Transit")

    val filteredDestinations = remember(destinations, searchQuery, selectedCategory) {
        destinations.filter { dest ->
            val matchesCategory = selectedCategory == "All" || dest.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    dest.title.contains(searchQuery, ignoreCase = true) ||
                    dest.subtitle.contains(searchQuery, ignoreCase = true) ||
                    dest.fullAddress.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            SpeedoTopBar(
                title = "Popular Destinations",
                onMenuClick = onMenuClick,
                actions = {
                    IconButton(onClick = { viewModel.fetchPopularDestinations() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = SpeedoTextPrimary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = SpeedoOrange,
                contentColor = SpeedoWhite,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Destination")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header & Real-Time Sync Indicator
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFF8E1),
                border = BorderStroke(1.dp, Color(0xFFFFE082))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = SpeedoOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Real-Time Sync Active • Changes broadcast instantly to Rider & Captain apps via WebSocket",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB78103),
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by landmark, area, or terminal...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpeedoOrange,
                    unfocusedBorderColor = SpeedoCardBorder
                )
            )

            // Category Filter Pills
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpeedoOrange,
                            selectedLabelColor = SpeedoWhite
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Destinations List
            if (filteredDestinations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = SpeedoTextSecondary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No destinations match your search",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SpeedoTextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredDestinations, key = { it.id }) { dest ->
                        AdminDestinationCard(
                            destination = dest,
                            onEdit = { destinationToEdit = dest },
                            onDelete = { destinationToDelete = dest }
                        )
                    }
                }
            }
        }
    }

    // Create Destination Dialog
    if (showCreateDialog) {
        DestinationFormDialog(
            title = "Add Popular Destination",
            destination = null,
            isSubmitting = uiState.isSubmittingAction,
            onDismiss = { showCreateDialog = false },
            onSubmit = { title, sub, cat, badge, img, lat, lng, addr, active, order ->
                viewModel.createPopularDestination(title, sub, cat, badge, img, lat, lng, addr, order)
                showCreateDialog = false
            }
        )
    }

    // Edit Destination Dialog
    destinationToEdit?.let { dest ->
        DestinationFormDialog(
            title = "Edit Destination: ${dest.title}",
            destination = dest,
            isSubmitting = uiState.isSubmittingAction,
            onDismiss = { destinationToEdit = null },
            onSubmit = { title, sub, cat, badge, img, lat, lng, addr, active, order ->
                viewModel.updatePopularDestination(dest.id, title, sub, cat, badge, img, lat, lng, addr, active, order)
                destinationToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    destinationToDelete?.let { dest ->
        AlertDialog(
            onDismissRequest = { destinationToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = SpeedoError) },
            title = { Text("Delete Popular Destination?") },
            text = {
                Text("Are you sure you want to permanently delete '${dest.title}'? This will remove it in real-time from all Rider and Captain apps.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePopularDestination(dest.id, dest.title)
                        destinationToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpeedoError)
                ) {
                    Text("Delete Instantly")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { destinationToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminDestinationCard(
    destination: PopularDestination,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SpeedoWhite,
        border = BorderStroke(1.dp, SpeedoCardBorder),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header Image with Badges
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(SpeedoSurfaceVariant)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(destination.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = destination.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Dark Scrim Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                startY = 50f
                            )
                        )
                )

                // Category & Badge Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SpeedoOrange,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = destination.badge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = SpeedoWhite
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Coordinates pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Lat: ${destination.lat} • Lng: ${destination.lng}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SpeedoWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Body Info
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = destination.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SpeedoTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = destination.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = SpeedoOrange
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (destination.isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        border = BorderStroke(1.dp, if (destination.isActive) SpeedoSuccess else SpeedoError)
                    ) {
                        Text(
                            text = if (destination.isActive) "ACTIVE" else "INACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (destination.isActive) SpeedoSuccess else SpeedoError,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = SpeedoTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = destination.fullAddress,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = SpeedoTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SpeedoCardBorder)
                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons: Edit & Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, SpeedoOrange)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = SpeedoOrange)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit", color = SpeedoOrange, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        border = BorderStroke(1.dp, SpeedoError)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = SpeedoError)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", color = SpeedoError, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DestinationFormDialog(
    title: String,
    destination: PopularDestination?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        subtitle: String,
        category: String,
        badge: String,
        imageUrl: String,
        lat: Double,
        lng: Double,
        address: String,
        isActive: Boolean,
        sortOrder: Int
    ) -> Unit
) {
    var titleInput by remember { mutableStateOf(destination?.title ?: "") }
    var subtitleInput by remember { mutableStateOf(destination?.subtitle ?: "") }
    var categoryInput by remember { mutableStateOf(destination?.category ?: "Airport") }
    var badgeInput by remember { mutableStateOf(destination?.badge ?: "✈️ Airport Terminal") }
    var imageUrlInput by remember { mutableStateOf(destination?.imageUrl ?: "https://images.unsplash.com/photo-1542296332-2e4473faf563?w=600&auto=format&fit=crop&q=80") }
    var latInput by remember { mutableStateOf(destination?.lat?.toString() ?: "12.9716") }
    var lngInput by remember { mutableStateOf(destination?.lng?.toString() ?: "77.5946") }
    var addressInput by remember { mutableStateOf(destination?.fullAddress ?: "") }
    var isActiveInput by remember { mutableStateOf(destination?.isActive ?: true) }
    var sortOrderInput by remember { mutableStateOf(destination?.sortOrder?.toString() ?: "0") }

    val categories = listOf("Airport", "Metro", "Shopping", "Tech Park", "Dining", "Park", "Transit", "General")

    val sampleImages = listOf(
        "Airport" to "https://images.unsplash.com/photo-1542296332-2e4473faf563?w=600&auto=format&fit=crop&q=80",
        "Metro" to "https://images.unsplash.com/photo-1519501025264-65ba15a82390?w=600&auto=format&fit=crop&q=80",
        "Mall" to "https://images.unsplash.com/photo-1567449303078-57ad995bd301?w=600&auto=format&fit=crop&q=80",
        "Tech Park" to "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?w=600&auto=format&fit=crop&q=80",
        "Cafe" to "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=600&auto=format&fit=crop&q=80",
        "Park" to "https://images.unsplash.com/photo-1519331379826-f10be5486c6f?w=600&auto=format&fit=crop&q=80",
        "Train" to "https://images.unsplash.com/photo-1474487548417-781cb71495f3?w=600&auto=format&fit=crop&q=80"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SpeedoWhite,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = SpeedoTextPrimary
                )
                Text(
                    text = "Live thumbnail landmark visible to all riders and captains",
                    style = MaterialTheme.typography.bodySmall,
                    color = SpeedoTextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Image Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpeedoSurfaceVariant)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrlInput)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick image picker chips
                Text("Quick Preset Photos:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(sampleImages) { (label, url) ->
                        SuggestionChip(
                            onClick = { imageUrlInput = url },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Landmark Title *") },
                    placeholder = { Text("e.g. Kempegowda Int'l Airport (BLR)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = subtitleInput,
                    onValueChange = { subtitleInput = it },
                    label = { Text("Subtitle / Neighborhood *") },
                    placeholder = { Text("e.g. Devanahalli, Terminal 1 & 2") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category selector
                Text("Category:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(categories) { cat ->
                        val isSel = categoryInput == cat
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                categoryInput = cat
                                badgeInput = when (cat) {
                                    "Airport" -> "✈️ Airport Terminal"
                                    "Metro" -> "🚇 Direct Metro"
                                    "Shopping" -> "🛍️ Shopping & Cinema"
                                    "Tech Park" -> "💼 IT Hub"
                                    "Dining" -> "☕ Food & Nightlife"
                                    "Park" -> "🌳 Sightseeing"
                                    "Transit" -> "🚆 Trains & Metro"
                                    else -> "📍 Popular Spot"
                                }
                            },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = badgeInput,
                    onValueChange = { badgeInput = it },
                    label = { Text("Badge Label") },
                    placeholder = { Text("e.g. ✈️ Airport Terminal") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = imageUrlInput,
                    onValueChange = { imageUrlInput = it },
                    label = { Text("High-Res Photo URL *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latInput,
                        onValueChange = { latInput = it },
                        label = { Text("Latitude *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = lngInput,
                        onValueChange = { lngInput = it },
                        label = { Text("Longitude *") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = addressInput,
                    onValueChange = { addressInput = it },
                    label = { Text("Full Street Address *") },
                    placeholder = { Text("e.g. KIAL Rd, Devanahalli, Bengaluru") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Destination Active on Maps", fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = isActiveInput,
                        onCheckedChange = { isActiveInput = it }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = {
                            val lat = latInput.toDoubleOrNull() ?: 12.9716
                            val lng = lngInput.toDoubleOrNull() ?: 77.5946
                            val order = sortOrderInput.toIntOrNull() ?: 0
                            onSubmit(
                                titleInput.trim(),
                                subtitleInput.trim(),
                                categoryInput.trim(),
                                badgeInput.trim(),
                                imageUrlInput.trim(),
                                lat,
                                lng,
                                addressInput.trim(),
                                isActiveInput,
                                order
                            )
                        },
                        enabled = !isSubmitting && titleInput.isNotBlank() && imageUrlInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = SpeedoOrange)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SpeedoWhite)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text("Save & Broadcast Live")
                    }
                }
            }
        }
    }
}
