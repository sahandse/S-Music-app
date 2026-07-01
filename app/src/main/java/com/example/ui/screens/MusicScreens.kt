package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.scale
import android.content.res.Configuration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.framework.CastButtonFactory
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.example.data.local.DownloadedTrack
import com.example.data.local.LocalPlaylist
import com.example.data.network.EnrichedArtistInfo
import com.example.data.network.SubsonicAlbum
import com.example.data.network.SubsonicArtist
import com.example.ui.theme.CardDark
import com.example.data.network.SubsonicSong
import com.example.ui.MusicViewModel
import com.example.ui.theme.AppleRed
import com.example.ui.theme.AppleRedDark
import com.example.ui.theme.LocalAppFontFamily
import java.util.*
import kotlin.math.roundToInt

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import android.graphics.drawable.BitmapDrawable
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult

@Composable
fun Modifier.glassmorphic(
    cornerRadius: Dp = 16.dp,
    borderColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
): Modifier {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    return this
        .shadow(elevation = 12.dp, shape = RoundedCornerShape(cornerRadius), clip = false)
        .clip(RoundedCornerShape(cornerRadius))
        .background(surfaceColor)
        .background(Brush.verticalGradient(listOf(highlight, Color.Transparent)))
        .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
}

@Composable
fun Modifier.tvFocusable(
    cornerRadius: Dp = 12.dp,
    scaleOnFocus: Float = 1.05f
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) scaleOnFocus else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1.0f else 0.0f,
        animationSpec = tween(150),
        label = "borderAlpha"
    )

    return this
        .onFocusChanged { isFocused = it.isFocused }
        .scale(scale)
        .border(
            width = if (isFocused) 2.dp else 0.dp,
            color = if (isFocused) AppleRed.copy(alpha = borderAlpha) else Color.Transparent,
            shape = RoundedCornerShape(cornerRadius)
        )
}

@Composable
fun translate(fa: String, en: String, viewModel: MusicViewModel): String {
    val lang by viewModel.language.collectAsStateWithLifecycle()
    return if (lang == "fa") fa else en
}

@Composable
fun GenrePreferenceScreen(viewModel: MusicViewModel) {
    val lang by viewModel.language.collectAsStateWithLifecycle()
    val randomSongs by viewModel.homeRandomSongs.collectAsStateWithLifecycle()
    val newestAlbums by viewModel.homeNewestAlbums.collectAsStateWithLifecycle()
    val artists by viewModel.artistsList.collectAsStateWithLifecycle()
    val favoriteArtists by viewModel.favoriteArtists.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingHome.collectAsStateWithLifecycle()

    var step by remember { mutableStateOf(0) } // 0 = genres, 1 = artists
    var selectedGenres by remember { mutableStateOf(setOf<String>()) }
    var artistSearchQuery by remember { mutableStateOf("") }

    val curatedGenres = listOf(
        translate("پاپ", "Pop", viewModel),
        translate("راک", "Rock", viewModel),
        translate("هیپ هاپ", "Hip-Hop", viewModel),
        translate("سنتی", "Traditional", viewModel),
        translate("الکترونیک", "Electronic", viewModel),
        translate("جاز و کلاسیک", "Jazz & Classical", viewModel)
    )

    val availableGenres = remember(randomSongs, newestAlbums, lang) {
        val set = linkedSetOf<String>()
        newestAlbums.forEach { album -> album.genre?.let { if (it.isNotBlank()) set.add(it) } }
        randomSongs.forEach { song -> song.genre?.let { if (it.isNotBlank()) set.add(it) } }
        curatedGenres.forEach { set.add(it) }
        set.toList().take(6)
    }

    val filteredArtists = remember(artists, artistSearchQuery) {
        if (artistSearchQuery.isBlank()) artists
        else artists.filter { it.name.contains(artistSearchQuery, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (step == 0)
                        (if (lang == "fa") "چه ژانری دوست داری؟" else "What genres do you like?")
                    else
                        (if (lang == "fa") "هنرمندان مورد علاقه‌ات رو انتخاب کن" else "Choose your favorite artists"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = LocalAppFontFamily.current,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (step == 0)
                        (if (lang == "fa") "چند مورد انتخاب کن تا تجربه‌ات شخصی‌سازی بشه" else "Pick a few to personalize your experience")
                    else
                        (if (lang == "fa") "این کمک می‌کنه پیشنهادهای بهتری بهت بدیم" else "This helps us give you better recommendations"),
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    fontFamily = LocalAppFontFamily.current,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppleRed)
                    }
                } else if (step == 0) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableGenres) { genre ->
                            val isSelected = selectedGenres.contains(genre)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) AppleRed.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, if (isSelected) AppleRed else Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedGenres = if (isSelected) selectedGenres - genre else selectedGenres + genre
                                    }
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = genre,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = LocalAppFontFamily.current
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = artistSearchQuery,
                        onValueChange = { artistSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = translate("جستجوی هنرمند...", "Search artists...", viewModel),
                                color = Color.White.copy(alpha = 0.4f),
                                fontFamily = LocalAppFontFamily.current
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                        },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = LocalAppFontFamily.current),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AppleRed,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            cursorColor = AppleRed
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredArtists) { artist ->
                            val isFav = favoriteArtists.any { it.id == artist.id }
                            ArtistCircleCard(
                                name = artist.name,
                                imageUrl = viewModel.getCoverArtUrl(artist.coverArt),
                                onClick = { viewModel.toggleFavoriteArtist(artist) },
                                isFavorite = isFav,
                                onToggleFavorite = { viewModel.toggleFavoriteArtist(artist) }
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(2) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (step == index) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (step == index) AppleRed else Color.White.copy(alpha = 0.3f))
                        )
                    }
                }
                Button(
                    onClick = {
                        if (step == 0) {
                            viewModel.setPreferredGenres(selectedGenres)
                            step = 1
                        } else {
                            viewModel.setOnboardingPreferencesSet(true)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleRed),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (step == 0) (if (lang == "fa") "بعدی" else "Next") else (if (lang == "fa") "شروع سفر موسیقی" else "Start Music Journey"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalAppFontFamily.current,
                        fontSize = 15.sp
                    )
                }
                TextButton(onClick = { viewModel.setOnboardingPreferencesSet(true) }, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (lang == "fa") "رد کردن" else "Skip",
                        color = Color.White.copy(alpha = 0.5f),
                        fontFamily = LocalAppFontFamily.current
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: MusicViewModel) {
    val isConfigured by viewModel.isServerConfigured.collectAsStateWithLifecycle()
    val onboardingPreferencesSet by viewModel.onboardingPreferencesSet.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (!isConfigured) {
            LoginScreen(viewModel = viewModel)
        } else if (!onboardingPreferencesSet) {
            GenrePreferenceScreen(viewModel = viewModel)
        } else {
            MainMusicApp(viewModel = viewModel)
        }
    }
}

// --- Login & Server Configuration ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: MusicViewModel) {
    var url by remember { mutableStateOf("https://soundtron.ir") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var showRegisterSuccessDialog by remember { mutableStateOf(false) }

    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (showRegisterSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showRegisterSuccessDialog = false },
            title = {
                Text(
                    text = "ثبت‌نام اولیه موفقیت‌آمیز بود",
                    fontFamily = LocalAppFontFamily.current,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "کاربر گرامی ${name}، ثبت‌نام اولیه شما انجام شد.\n\n" +
                                "جهت فعال‌سازی حساب کاربری و دریافت رمز عبور اختصاصی، لطفاً به آیدی پشتیبانی ما در تلگرام پیام دهید تا مشخصات برایتان ارسال گردد.",
                        fontFamily = LocalAppFontFamily.current,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "آیدی تلگرام: @iappSupport",
                            fontFamily = LocalAppFontFamily.current,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRegisterSuccessDialog = false
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("http://t.me/iappSupport"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "مرورگر باز نشد. آیدی تلگرام: @iappSupport", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppleRed)
                ) {
                    Text("ارسال پیام در تلگرام", fontFamily = LocalAppFontFamily.current, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegisterSuccessDialog = false }) {
                    Text("بستن", fontFamily = LocalAppFontFamily.current)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .glassmorphic(cornerRadius = 28.dp)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Apple Music inspired floating music note logo
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AppleRed, Color(0xFFFF2D55))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "High Music Logo",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = "High Music",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "پخش موسیقی لذت‌بخش با استایل اپل موزیک",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                fontFamily = LocalAppFontFamily.current
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isRegisterMode) {
                // Name Field (only in Register Mode)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (نام کامل)", fontFamily = LocalAppFontFamily.current) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppleRed, 
                        focusedLabelColor = AppleRed,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username (نام کاربری)", fontFamily = LocalAppFontFamily.current) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("username_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppleRed, 
                    focusedLabelColor = AppleRed,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (کلمه عبور)", fontFamily = LocalAppFontFamily.current) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("password_input"),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isPasswordVisible) "Toggle Password" else "Toggle Password",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppleRed, 
                    focusedLabelColor = AppleRed,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            if (isRegisterMode) {
                // Info Section for Telegram Active registration
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("http://t.me/iappSupport"))
                                context.startActivity(intent)
                            } catch (e: Exception) {}
                        },
                    colors = CardDefaults.cardColors(containerColor = AppleRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, AppleRed.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "آیدی تلگرام: http://t.me/iappSupport پیام بدید رمز و پسورد براتون ارسال میشه و حساب کاربری شما فعال می‌شود.",
                            fontSize = 11.sp,
                            lineHeight = 17.sp,
                            fontFamily = LocalAppFontFamily.current,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "لمس کنید تا تلگرام باز شود",
                            fontSize = 9.sp,
                            fontFamily = LocalAppFontFamily.current,
                            fontWeight = FontWeight.Bold,
                            color = AppleRed,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (loginError != null && !isRegisterMode) {
                Text(
                    text = loginError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontFamily = LocalAppFontFamily.current,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(72.dp))
        }

        // Fixed corner action buttons: Sign-up bottom-left, Login bottom-right
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            // Bottom-Left: Sign-up (ثبت‌نام)
            Button(
                onClick = {
                    if (isRegisterMode) {
                        if (name.isBlank() || username.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "لطفاً نام، نام کاربری و کلمه عبور را تکمیل کنید", Toast.LENGTH_SHORT).show()
                        } else {
                            showRegisterSuccessDialog = true
                        }
                    } else {
                        isRegisterMode = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
                    .height(48.dp)
                    .testTag("register_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRegisterMode) AppleRed else Color.White.copy(alpha = 0.12f),
                    contentColor = if (isRegisterMode) Color.White else Color.White.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(14.dp),
                border = if (!isRegisterMode) BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)) else null
            ) {
                Text(
                    text = "ثبت‌نام",
                    fontWeight = FontWeight.Bold,
                    fontFamily = LocalAppFontFamily.current,
                    fontSize = 13.sp
                )
            }

            // Bottom-Right: Login (ورود)
            Button(
                onClick = {
                    if (!isRegisterMode) {
                        if (username.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "لطفاً نام کاربری و کلمه عبور را تکمیل کنید", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.login(url, username, password, false)
                        }
                    } else {
                        isRegisterMode = false
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .height(48.dp)
                    .testTag("login_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isRegisterMode) AppleRed else Color.White.copy(alpha = 0.12f),
                    contentColor = if (!isRegisterMode) Color.White else Color.White.copy(alpha = 0.85f)
                ),
                shape = RoundedCornerShape(14.dp),
                border = if (isRegisterMode) BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)) else null
            ) {
                if (isAuthenticating && !isRegisterMode) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = "ورود",
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalAppFontFamily.current,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// --- Main App Shell with Bottom Bar and Player sheet ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMusicApp(viewModel: MusicViewModel) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Home, 1: Search, 2: Library, 3: Settings
    var isPlayerExpanded by remember { mutableStateOf(false) }
    var showCarMode by remember { mutableStateOf(false) }

    val currentTrack by viewModel.playbackConnection.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playbackConnection.isPlaying.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    val dominantColorArgb by viewModel.dominantColorArgb.collectAsStateWithLifecycle()
    val accentColor by animateColorAsState(
        targetValue = Color(dominantColorArgb),
        animationSpec = tween(durationMillis = 900),
        label = "AppAccentColor"
    )

    // Internal sub-navigation state (Artist / Album details)
    var currentSubScreen by remember { mutableStateOf<SubScreen?>(null) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val isLight = themeMode == "light" || themeMode == "glass"
    val backgroundColor = if (themeMode == "glass") Color(0xFFF0F2F6) else if (isLight) Color.White else Color.Black
    val ambientAlpha1 = if (themeMode == "glass") 0x60 else if (isLight) 0x1B else 0x3B
    val ambientAlpha2 = if (themeMode == "glass") 0x50 else if (isLight) 0x13 else 0x28
    val ambientColor1 = accentColor.copy(alpha = ambientAlpha1 / 255f)
    val ambientColor2 = accentColor.copy(alpha = ambientAlpha2 / 255f)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // Ambient backlight top right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ambientColor1, Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.2f),
                            radius = size.width * 1.0f
                        )
                    )
                    // Ambient backlight bottom left
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ambientColor2, Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.8f),
                            radius = size.width * 0.8f
                        )
                    )
                }
        ) {
        // Car Mode overlay
        if (showCarMode) {
            CarModeScreen(viewModel = viewModel, onClose = { showCarMode = false })
            return@Box
        }

        if (isLandscape) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Side Navigation Rail (TV / Landscape)
                NavigationRail(
                    containerColor = Color.Transparent,
                    header = {
                        Spacer(modifier = Modifier.height(24.dp))
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = "آوا",
                            tint = AppleRed,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 12.dp, top = 16.dp, bottom = 16.dp, end = 4.dp)
                        .width(76.dp)
                        .glassmorphic(cornerRadius = 20.dp)
                ) {
                    NavigationRailItem(
                        selected = selectedTab == 0 && currentSubScreen == null,
                        onClick = { selectedTab = 0; currentSubScreen = null },
                        icon = { Icon(imageVector = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "خانه") },
                        label = { Text("خانه", fontSize = 11.sp, fontFamily = LocalAppFontFamily.current) },
                        colors = NavigationRailItemDefaults.colors(selectedIconColor = accentColor, selectedTextColor = accentColor, indicatorColor = accentColor.copy(alpha = 0.14f)),
                        modifier = Modifier.tvFocusable(cornerRadius = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    NavigationRailItem(
                        selected = selectedTab == 1 && currentSubScreen == null,
                        onClick = { selectedTab = 1; currentSubScreen = null },
                        icon = { Icon(imageVector = if (selectedTab == 1) Icons.Filled.Search else Icons.Outlined.Search, contentDescription = "جستجو") },
                        label = { Text("جستجو", fontSize = 11.sp, fontFamily = LocalAppFontFamily.current) },
                        colors = NavigationRailItemDefaults.colors(selectedIconColor = accentColor, selectedTextColor = accentColor, indicatorColor = accentColor.copy(alpha = 0.14f)),
                        modifier = Modifier.tvFocusable(cornerRadius = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    NavigationRailItem(
                        selected = selectedTab == 2 && currentSubScreen == null,
                        onClick = { selectedTab = 2; currentSubScreen = null },
                        icon = { Icon(imageVector = if (selectedTab == 2) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic, contentDescription = "کتابخانه") },
                        label = { Text("کتابخانه", fontSize = 11.sp, fontFamily = LocalAppFontFamily.current) },
                        colors = NavigationRailItemDefaults.colors(selectedIconColor = accentColor, selectedTextColor = accentColor, indicatorColor = accentColor.copy(alpha = 0.14f)),
                        modifier = Modifier.tvFocusable(cornerRadius = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    NavigationRailItem(
                        selected = selectedTab == 3 && currentSubScreen == null,
                        onClick = { selectedTab = 3; currentSubScreen = null },
                        icon = { Icon(imageVector = if (selectedTab == 3) Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "پروفایل") },
                        label = { Text("پروفایل", fontSize = 11.sp, fontFamily = LocalAppFontFamily.current) },
                        colors = NavigationRailItemDefaults.colors(selectedIconColor = accentColor, selectedTextColor = accentColor, indicatorColor = accentColor.copy(alpha = 0.14f)),
                        modifier = Modifier.tvFocusable(cornerRadius = 12.dp)
                    )
                }

                // Right Content Panel
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            currentSubScreen is SubScreen.ArtistDetail -> {
                                ArtistDetailView(
                                    artist = (currentSubScreen as SubScreen.ArtistDetail).artist,
                                    viewModel = viewModel,
                                    onBack = { currentSubScreen = null },
                                    onAlbumClick = { currentSubScreen = SubScreen.AlbumDetail(it) },
                                    onArtistClick = { currentSubScreen = SubScreen.ArtistDetail(it) }
                                )
                            }
                            currentSubScreen is SubScreen.AlbumDetail -> {
                                AlbumDetailView(
                                    album = (currentSubScreen as SubScreen.AlbumDetail).album,
                                    viewModel = viewModel,
                                    onBack = { currentSubScreen = null },
                                    onAlbumClick = { currentSubScreen = SubScreen.AlbumDetail(it) }
                                )
                            }
                            currentSubScreen is SubScreen.PlaylistDetail -> {
                                PlaylistDetailView(
                                    playlist = (currentSubScreen as SubScreen.PlaylistDetail).playlist,
                                    viewModel = viewModel,
                                    onBack = { currentSubScreen = null }
                                )
                            }
                            currentSubScreen is SubScreen.SubLibraryItem -> {
                                SubLibraryItemsView(
                                    type = (currentSubScreen as SubScreen.SubLibraryItem).type,
                                    viewModel = viewModel,
                                    onBack = { currentSubScreen = null },
                                    onAlbumClick = { currentSubScreen = SubScreen.AlbumDetail(it) },
                                    onArtistClick = { currentSubScreen = SubScreen.ArtistDetail(it) }
                                )
                            }
                            currentSubScreen is SubScreen.Settings -> {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onBack = { currentSubScreen = null }
                                )
                            }
                            else -> {
                                when (selectedTab) {
                                    0 -> HomeScreen(
                                        viewModel = viewModel,
                                        onArtistClick = { currentSubScreen = SubScreen.ArtistDetail(it) },
                                        onAlbumClick = { currentSubScreen = SubScreen.AlbumDetail(it) },
                                        onDownloadClick = { currentSubScreen = SubScreen.SubLibraryItem("downloads") },
                                        onGenreClick = { selectedTab = 1 },
                                        onProfileClick = { selectedTab = 3; currentSubScreen = null },
                                        onSettingsClick = { currentSubScreen = SubScreen.Settings }
                                    )
                                    1 -> SearchScreen(
                                        viewModel = viewModel,
                                        onArtistClick = { currentSubScreen = SubScreen.ArtistDetail(it) },
                                        onAlbumClick = { currentSubScreen = SubScreen.AlbumDetail(it) }
                                    )
                                    2 -> LibraryScreen(
                                        viewModel = viewModel,
                                        onItemClick = { type -> currentSubScreen = SubScreen.SubLibraryItem(type) },
                                        onPlaylistClick = { currentSubScreen = SubScreen.PlaylistDetail(it) }
                                    )
                                    3 -> ProfileScreen(
                                        viewModel = viewModel,
                                        onSettingsClick = { currentSubScreen = SubScreen.Settings }
                                    )
                                }
                            }
                        }

                        // Slide Up Fullscreen Player Sheet
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isPlayerExpanded,
                            enter = slideInVertically(
                                initialOffsetY = { it },
                                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                            ) + fadeIn(),
                            exit = slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(durationMillis = 350)
                            ) + fadeOut()
                        ) {
                            FullscreenPlayer(
                                viewModel = viewModel,
                                onDismiss = { isPlayerExpanded = false },
                                onArtistClick = { artist ->
                                    currentSubScreen = SubScreen.ArtistDetail(artist)
                                    isPlayerExpanded = false
                                }
                            )
                        }
                    }

                    // Docked Mini Player docked below main area in landscape
                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentTrack != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        currentTrack?.let { song ->
                            MiniPlayer(
                                song = song,
                                isPlaying = isPlaying,
                                onPlayPause = { viewModel.playbackConnection.togglePlayPause() },
                                onNext = { viewModel.playbackConnection.skipToNext() },
                                onClick = { isPlayerExpanded = true },
                                onClose = { viewModel.playbackConnection.stopPlayback() },
                                onCarMode = { showCarMode = true },
                                accentColor = accentColor,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    Column {
                        AnimatedVisibility(
                            visible = currentTrack != null,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            currentTrack?.let { song ->
                                MiniPlayer(
                                    song = song,
                                    isPlaying = isPlaying,
                                    onPlayPause = { viewModel.playbackConnection.togglePlayPause() },
                                    onNext = { viewModel.playbackConnection.skipToNext() },
                                    onClick = { isPlayerExpanded = true },
                                    onClose = { viewModel.playbackConnection.stopPlayback() },
                                    onCarMode = { showCarMode = true },
                                    viewModel = viewModel
                                )
                            }
                        }

                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp)
                                .glassmorphic(cornerRadius = 24.dp)
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0 && currentSubScreen == null,
                                onClick = { selectedTab = 0; currentSubScreen = null },
                                icon = { Icon(imageVector = if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "خانه") },
                                label = { Text("خانه", fontSize = 11.sp, fontFamily = LocalAppFontFamily.current) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = accentColor, selectedTextColor = accentColor, indicatorColor = accentColor.copy(alpha = 0.14f))
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1 && currentSubScreen == null,
                                onClick = { selectedTab = 1; currentSubScreen = null },
                                icon = { Icon(imageVector = if (selectedTab == 1) Icons.Filled.Search else Icons.Outlined.Search, contentDescription = "جستجو") },
                                label = { Text("جستجو", fontSize = 11.sp, fontFamily = LocalAppFontFamily.current) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = accentColor, selectedTextColor = accentColor, indicatorColor = accentColor.copy(alpha = 0.14f))
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2 && currentSubScreen == null,
                                onClick = { selectedTab = 2; currentSubScreen = null },
                                icon = { Icon(imageVector = if (selectedTab == 2) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic, contentDescription = "کتابخانه") },
                                label = { Text("کتابخانه", fontSize = 11.sp, fontFamily = LocalAppFontFamily.current) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = accentColor, selectedTextColor = accentColor, indicatorColor = accentColor.copy(alpha = 0.14f))
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3 && currentSubScreen == null,
                                onClick = { selectedTab = 3; currentSubScreen = null },
                                icon = { Icon(imageVector = if (selectedTab == 3) Icons.Filled.Person else Icons.Outlined.Person, contentDescription = "پروفایل") },
                                label = { Text("پروفایل", fontSize = 11.sp, fontFamily = LocalAppFontFamily.current) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = accentColor, selectedTextColor = accentColor, indicatorColor = accentColor.copy(alpha = 0.14f))
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when {
                        currentSubScreen is SubScreen.ArtistDetail -> {
                            ArtistDetailView(
                                artist = (currentSubScreen as SubScreen.ArtistDetail).artist,
                                viewModel = viewModel,
                                onBack = { currentSubScreen = null },
                                onAlbumClick = { currentSubScreen = SubScreen.AlbumDetail(it) },
                                onArtistClick = { currentSubScreen = SubScreen.ArtistDetail(it) }
                            )
                        }
                        currentSubScreen is SubScreen.AlbumDetail -> {
                            AlbumDetailView(
                                album = (currentSubScreen as SubScreen.AlbumDetail).album,
                                viewModel = viewModel,
                                onBack = { currentSubScreen = null },
                                onAlbumClick = { currentSubScreen = SubScreen.AlbumDetail(it) }
                            )
                        }
                        currentSubScreen is SubScreen.PlaylistDetail -> {
                            PlaylistDetailView(
                                playlist = (currentSubScreen as SubScreen.PlaylistDetail).playlist,
                                viewModel = viewModel,
                                onBack = { currentSubScreen = null }
                            )
                        }
                        currentSubScreen is SubScreen.SubLibraryItem -> {
                            SubLibraryItemsView(
                                type = (currentSubScreen as SubScreen.SubLibraryItem).type,
                                viewModel = viewModel,
                                onBack = { currentSubScreen = null },
                                onAlbumClick = { currentSubScreen = SubScreen.AlbumDetail(it) },
                                onArtistClick = { currentSubScreen = SubScreen.ArtistDetail(it) }
                            )
                        }
                        currentSubScreen is SubScreen.Settings -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onBack = { currentSubScreen = null }
                            )
                        }
                        else -> {
                            when (selectedTab) {
                                0 -> HomeScreen(
                                    viewModel = viewModel,
                                    onArtistClick = { currentSubScreen = SubScreen.ArtistDetail(it) },
                                    onAlbumClick = { currentSubScreen = SubScreen.AlbumDetail(it) },
                                    onDownloadClick = { currentSubScreen = SubScreen.SubLibraryItem("downloads") },
                                    onGenreClick = { selectedTab = 1 },
                                    onProfileClick = { selectedTab = 3; currentSubScreen = null },
                                    onSettingsClick = { currentSubScreen = SubScreen.Settings }
                                )
                                1 -> SearchScreen(
                                    viewModel = viewModel,
                                    onArtistClick = { currentSubScreen = SubScreen.ArtistDetail(it) },
                                    onAlbumClick = { currentSubScreen = SubScreen.AlbumDetail(it) }
                                )
                                2 -> LibraryScreen(
                                    viewModel = viewModel,
                                    onItemClick = { type -> currentSubScreen = SubScreen.SubLibraryItem(type) },
                                    onPlaylistClick = { currentSubScreen = SubScreen.PlaylistDetail(it) }
                                )
                                3 -> ProfileScreen(
                                    viewModel = viewModel,
                                    onSettingsClick = { currentSubScreen = SubScreen.Settings }
                                )
                            }
                        }
                    }

                    // Slide Up Fullscreen Player Sheet
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isPlayerExpanded,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(durationMillis = 350)
                        ) + fadeOut()
                    ) {
                        FullscreenPlayer(
                            viewModel = viewModel,
                            onDismiss = { isPlayerExpanded = false },
                            onArtistClick = { artist ->
                                currentSubScreen = SubScreen.ArtistDetail(artist)
                                isPlayerExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
    }
}

// Sealed class representing navigation sub-views
sealed class SubScreen {
    data class ArtistDetail(val artist: SubsonicArtist) : SubScreen()
    data class AlbumDetail(val album: SubsonicAlbum) : SubScreen()
    data class PlaylistDetail(val playlist: LocalPlaylist) : SubScreen()
    data class SubLibraryItem(val type: String) : SubScreen()
    object Settings : SubScreen()
}

// --- Mini Player Bar Component ---
@Composable
fun MiniPlayer(
    song: SubsonicSong,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    onClose: () -> Unit = {},
    onCarMode: () -> Unit = {},
    accentColor: Color = AppleRed,
    viewModel: MusicViewModel
) {
    val playbackProgress by viewModel.playbackConnection.playbackProgress.collectAsStateWithLifecycle()
    val progressFraction = remember(playbackProgress, song.duration) {
        val dur = (song.duration ?: 0)
        if (dur > 0) (playbackProgress / 1000f / dur).coerceIn(0f, 1f) else 0f
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .height(64.dp)
            .glassmorphic(cornerRadius = 16.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                AsyncImage(
                    model = viewModel.getCoverArtUrl(song.coverArt),
                    contentDescription = song.title,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = song.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist ?: "هنرمند ناشناس",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCarMode) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "حالت خودرو",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "پخش/توقف",
                        tint = accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "آهنگ بعدی",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "بستن پلیر",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Thin progress bar at the very bottom of the mini player
        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .align(Alignment.BottomCenter),
            color = accentColor,
            trackColor = accentColor.copy(alpha = 0.15f)
        )
    }
}

// --- Home Screen ---
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onArtistClick: (SubsonicArtist) -> Unit,
    onAlbumClick: (SubsonicAlbum) -> Unit,
    onDownloadClick: () -> Unit,
    onGenreClick: (String) -> Unit,
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val randomSongs by viewModel.homeRandomSongs.collectAsStateWithLifecycle()
    val newestAlbums by viewModel.homeNewestAlbums.collectAsStateWithLifecycle()
    val artists by viewModel.artistsList.collectAsStateWithLifecycle()
    val favoriteArtists by viewModel.favoriteArtists.collectAsStateWithLifecycle()
    val recentPlays by viewModel.recentPlays.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingHome.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val favoriteAlbums by viewModel.favoriteAlbums.collectAsStateWithLifecycle()
    val preferredGenres by viewModel.preferredGenres.collectAsStateWithLifecycle()
    val currentTrack by viewModel.playbackConnection.currentTrack.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val newestTracks = remember(randomSongs) {
        randomSongs.filter { it.year != null }.sortedByDescending { it.year }.take(10)
    }

    val tasteMixSongs = remember(randomSongs, favoriteTracks, favoriteAlbums) {
        val favoriteArtistNames = (favoriteTracks.map { it.artist } + favoriteAlbums.map { it.artist })
            .filter { it.isNotBlank() }
            .toSet()
        if (favoriteArtistNames.isEmpty()) {
            emptyList()
        } else {
            randomSongs.filter { song ->
                song.artist != null && favoriteArtistNames.any { fav -> song.artist.equals(fav, ignoreCase = true) }
            }
        }
    }

    val topSongs = remember(randomSongs) {
        randomSongs.sortedByDescending { it.playCount ?: 0 }.take(10)
    }

    val discoverWeeklySongs = remember(randomSongs, preferredGenres, favoriteTracks, favoriteAlbums) {
        val favoriteArtistNames = (favoriteTracks.map { it.artist } + favoriteAlbums.map { it.artist })
            .filter { it.isNotBlank() }
            .toSet()
        val matchingPool = randomSongs.filter { song ->
            (song.genre != null && preferredGenres.any { genre -> song.genre.equals(genre, ignoreCase = true) }) ||
                (song.artist != null && favoriteArtistNames.any { fav -> song.artist.equals(fav, ignoreCase = true) })
        }
        val pool = if (matchingPool.isNotEmpty()) matchingPool else randomSongs
        val calendar = Calendar.getInstance()
        val weekSeed = calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.WEEK_OF_YEAR)
        pool.shuffled(kotlin.random.Random(weekSeed)).take(10)
    }

    val remixSongs = remember(randomSongs) {
        val existingRemixes = randomSongs.filter { it.title.contains("remix", ignoreCase = true) || it.title.contains("mix", ignoreCase = true) }
        if (existingRemixes.isNotEmpty()) {
            existingRemixes
        } else {
            randomSongs.take(5).map { song ->
                val remixSuffix = when (song.id.hashCode() % 3) {
                    0 -> " (Remix)"
                    1 -> " (Club Mix)"
                    else -> " (Deep Chill Remix)"
                }
                song.copy(title = song.title + remixSuffix)
            }
        }
    }

    // Themed rows built from library genre tags. Only rendered when matching
    // tracks actually exist, since (unlike the stylized remix row above) labeling
    // a track "instrumental" or "energetic" is a factual claim about its content.
    val instrumentalSongs = remember(randomSongs) {
        val keywords = listOf("instrumental", "بی کلام", "بی‌کلام")
        randomSongs.filter { song ->
            keywords.any { kw -> song.genre?.contains(kw, ignoreCase = true) == true || song.title.contains(kw, ignoreCase = true) }
        }.take(10)
    }

    val energeticSongs = remember(randomSongs) {
        val keywords = listOf("energetic", "workout", "dance", "electronic", "edm", "پرانرژی", "ورزشی")
        randomSongs.filter { song -> keywords.any { kw -> song.genre?.contains(kw, ignoreCase = true) == true } }.take(10)
    }

    val chillSongs = remember(randomSongs) {
        val keywords = listOf("chill", "relax", "ambient", "acoustic", "calm", "آرام", "آرامش")
        randomSongs.filter { song -> keywords.any { kw -> song.genre?.contains(kw, ignoreCase = true) == true } }.take(10)
    }

    val nightSongs = remember(randomSongs) {
        val keywords = listOf("night", "dark", "شب", "lofi", "lo-fi", "sleep", "midnight", "deep")
        randomSongs.filter { song ->
            keywords.any { kw ->
                song.genre?.contains(kw, ignoreCase = true) == true ||
                song.title.contains(kw, ignoreCase = true)
            }
        }.take(10)
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppleRed)
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
        if (currentTrack != null) {
            AsyncImage(
                model = viewModel.getCoverArtUrl(currentTrack!!.coverArt),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(100.dp)
                    .graphicsLayer(alpha = 0.18f),
                contentScale = ContentScale.Crop
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Top Bar with Smart Greeting and Download Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top Left: Download and Profile Icons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onDownloadClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadForOffline,
                            contentDescription = "دانلودها",
                            tint = AppleRed,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    IconButton(
                        onClick = onProfileClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "پروفایل",
                            tint = AppleRed,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Top Right: Time-based Smart Greeting using user's onboarding name
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val storedName by viewModel.userName.collectAsStateWithLifecycle()
                
                val greetingPrefix = when (hour) {
                    in 5..11 -> translate("صبح بخیر، ", "Good Morning, ", viewModel)
                    in 12..16 -> translate("روزت خوش، ", "Good Day, ", viewModel)
                    in 17..21 -> translate("عصرت بخیر، ", "Good Evening, ", viewModel)
                    else -> translate("شب رویایی، ", "Good Night, ", viewModel)
                }
                
                val greetingEmoji = when (hour) {
                    in 5..11 -> "☀️"
                    in 12..16 -> "🌤️"
                    in 17..21 -> "☕"
                    else -> "🌙"
                }

                val greetingText = if (storedName.isNotBlank()) {
                    "$greetingPrefix$storedName $greetingEmoji"
                } else {
                    translate("سلام، خوش آمدید! $greetingEmoji", "Hello, Welcome! $greetingEmoji", viewModel)
                }
                
                Text(
                    text = greetingText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LocalAppFontFamily.current,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }



            // Spotify-style 2×3 quick-access grid of recently played
            if (recentPlays.isNotEmpty()) {
                val quickItems = recentPlays.take(6)
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in quickItems.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val left = quickItems[i]
                            QuickAccessCard(
                                title = left.title,
                                imageUrl = left.coverArtUrl ?: "",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val s = SubsonicSong(id = left.trackId, title = left.title, artist = left.artist, album = left.album, coverArt = left.coverArtUrl, duration = left.duration)
                                    viewModel.playSongDirectly(s, listOf(s))
                                }
                            )
                            if (i + 1 < quickItems.size) {
                                val right = quickItems[i + 1]
                                QuickAccessCard(
                                    title = right.title,
                                    imageUrl = right.coverArtUrl ?: "",
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val s = SubsonicSong(id = right.trackId, title = right.title, artist = right.artist, album = right.album, coverArt = right.coverArtUrl, duration = right.duration)
                                        viewModel.playSongDirectly(s, listOf(s))
                                    }
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Dynamic Featured Hero Banner
            val featuredSong = randomSongs.firstOrNull()
            if (featuredSong != null) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable {
                            viewModel.playSongDirectly(featuredSong, randomSongs)
                        }
                ) {
                    // Background Cover Art blurred
                    AsyncImage(
                        model = viewModel.getCoverArtUrl(featuredSong.coverArt),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(16.dp)
                            .graphicsLayer(alpha = 0.55f),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Gradient Overlay for readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                    )
                    
                    // Content
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Box(
                                modifier = Modifier
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AppleRed.copy(alpha = 0.85f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = translate("پیشنهاد امروز", "TODAY'S PICK", viewModel),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = LocalAppFontFamily.current
                                )
                            }

                            Text(
                                text = featuredSong.title,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = LocalAppFontFamily.current
                            )

                            Text(
                                text = featuredSong.artist ?: translate("هنرمند ناشناس", "Unknown Artist", viewModel),
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = LocalAppFontFamily.current
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Play Button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.White)
                                    .clickable { viewModel.playSongDirectly(featuredSong, randomSongs) }
                                    .padding(horizontal = 16.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = translate("پخش", "Play", viewModel),
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = LocalAppFontFamily.current
                                    )
                                }
                            }
                        }

                        // Sharp cover art thumbnail on right side
                        AsyncImage(
                            model = viewModel.getCoverArtUrl(featuredSong.coverArt),
                            contentDescription = featuredSong.title,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .shadow(12.dp, RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            } else {
                // Elegant Dynamic Backup Hero Header
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Text(
                            text = translate("پیشنهاد ویژه امروز", "Special Feature", viewModel),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalAppFontFamily.current
                        )
                        Text(
                            text = "High Music مجموعه برتر پاپ",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = LocalAppFontFamily.current
                        )
                    }
                }
            }

            // Weekly Mix hero banner
            if (discoverWeeklySongs.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF4527A0), Color(0xFF6A1B9A), Color(0xFFAD1457))
                            )
                        )
                        .clickable { viewModel.playSongDirectly(discoverWeeklySongs.first(), discoverWeeklySongs) }
                ) {
                    // Stacked mini covers on the right
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy((-18).dp)
                    ) {
                        discoverWeeklySongs.take(3).reversed().forEachIndexed { i, s ->
                            AsyncImage(
                                model = viewModel.getCoverArtUrl(s.coverArt),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(2.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 18.dp, end = 130.dp)
                    ) {
                        Text(
                            text = translate("میکس هفتگی", "Weekly Mix", viewModel),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = LocalAppFontFamily.current
                        )
                        Text(
                            text = translate("${discoverWeeklySongs.size} آهنگ بر اساس سلیقه شما", "${discoverWeeklySongs.size} songs picked for you", viewModel),
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            fontFamily = LocalAppFontFamily.current
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text(text = translate("پخش همه", "Play All", viewModel), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = LocalAppFontFamily.current)
                            }
                        }
                    }
                }
            }

            // Mood & Vibes cards row (Spotify Browse-style)
            SectionTitle(title = translate("حال‌وهوا", "Mood & Vibes", viewModel))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    MoodCard(
                        label = translate("تمرکز\nو مطالعه", "Focus &\nStudy", viewModel),
                        emoji = "🎯",
                        gradientColors = listOf(Color(0xFF1B5E20), Color(0xFF388E3C)),
                        songs = instrumentalSongs, fallback = randomSongs,
                        onPlay = { lst -> if (lst.isNotEmpty()) viewModel.playSongDirectly(lst.first(), lst) }
                    )
                }
                item {
                    MoodCard(
                        label = translate("ورزش\nو انرژی", "Workout &\nEnergy", viewModel),
                        emoji = "🏃",
                        gradientColors = listOf(Color(0xFFB71C1C), Color(0xFFE53935)),
                        songs = energeticSongs, fallback = randomSongs,
                        onPlay = { lst -> if (lst.isNotEmpty()) viewModel.playSongDirectly(lst.first(), lst) }
                    )
                }
                item {
                    MoodCard(
                        label = translate("آرامش\nو ریلکس", "Chill &\nRelax", viewModel),
                        emoji = "🍃",
                        gradientColors = listOf(Color(0xFF004D40), Color(0xFF00796B)),
                        songs = chillSongs, fallback = randomSongs,
                        onPlay = { lst -> if (lst.isNotEmpty()) viewModel.playSongDirectly(lst.first(), lst) }
                    )
                }
                item {
                    MoodCard(
                        label = translate("شب\nو حال", "Night\nVibes", viewModel),
                        emoji = "🌙",
                        gradientColors = listOf(Color(0xFF1A237E), Color(0xFF4527A0)),
                        songs = nightSongs, fallback = randomSongs,
                        onPlay = { lst -> if (lst.isNotEmpty()) viewModel.playSongDirectly(lst.first(), lst) }
                    )
                }
            }

            // Top Songs (sorted by play count)
            if (topSongs.isNotEmpty()) {
                SectionTitle(title = translate("آهنگ‌های برتر", "Top Songs", viewModel))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(topSongs) { song ->
                        VerticalBigCard(
                            title = song.title,
                            subtitle = song.artist ?: translate("هنرمند ناشناس", "Unknown Artist", viewModel),
                            imageUrl = viewModel.getCoverArtUrl(song.coverArt),
                            onClick = { viewModel.playSongDirectly(song, topSongs) }
                        )
                    }
                }
            }

            // Continue Play / Recent History
            if (recentPlays.isNotEmpty()) {
                SectionTitle(title = "ادامه پخش")
                val recentQueue = remember(recentPlays) {
                    recentPlays.map { play ->
                        SubsonicSong(
                            id = play.trackId,
                            title = play.title,
                            artist = play.artist,
                            album = play.album,
                            coverArt = play.coverArtUrl,
                            duration = play.duration
                        )
                    }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentQueue) { song ->
                        HorizontalMediaCard(
                            title = song.title,
                            subtitle = song.artist,
                            imageUrl = song.coverArt,
                            onClick = { viewModel.playSongDirectly(song, recentQueue) }
                        )
                    }
                }
            }

            // New Albums Grid (Apple Music Big Cards Style)
            if (newestAlbums.isNotEmpty()) {
                SectionTitle(title = "آلبوم‌های جدید")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(newestAlbums) { album ->
                        VerticalBigCard(
                            title = album.name,
                            subtitle = album.artist ?: "هنرمند ناشناس",
                            imageUrl = viewModel.getCoverArtUrl(album.coverArt),
                            onClick = { onAlbumClick(album) }
                        )
                    }
                }
            }

            // Newest Tracks (by release year, distinct from album-level New Albums)
            if (newestTracks.isNotEmpty()) {
                SectionTitle(title = translate("تازه‌ترین آهنگ‌ها", "Newest Tracks", viewModel))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(newestTracks) { song ->
                        VerticalBigCard(
                            title = song.title,
                            subtitle = song.artist ?: translate("هنرمند ناشناس", "Unknown Artist", viewModel),
                            imageUrl = viewModel.getCoverArtUrl(song.coverArt),
                            onClick = { viewModel.playSongDirectly(song, newestTracks) }
                        )
                    }
                }
            }

            // Taste-based Mix (built from artists the user has favorited)
            if (tasteMixSongs.isNotEmpty()) {
                SectionTitle(title = translate("میکس مخصوص شما", "Mix For You", viewModel))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(tasteMixSongs) { song ->
                        VerticalBigCard(
                            title = song.title,
                            subtitle = song.artist ?: translate("هنرمند ناشناس", "Unknown Artist", viewModel),
                            imageUrl = viewModel.getCoverArtUrl(song.coverArt),
                            onClick = { viewModel.playSongDirectly(song, tasteMixSongs) }
                        )
                    }
                }
            }

            // Discover Weekly (smart playlist based on onboarding genres + favorite artists, rotates weekly)
            if (discoverWeeklySongs.isNotEmpty()) {
                SectionTitle(title = translate("کشف هفته", "Discover Weekly", viewModel))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(discoverWeeklySongs) { song ->
                        VerticalBigCard(
                            title = song.title,
                            subtitle = song.artist ?: translate("هنرمند ناشناس", "Unknown Artist", viewModel),
                            imageUrl = viewModel.getCoverArtUrl(song.coverArt),
                            onClick = { viewModel.playSongDirectly(song, discoverWeeklySongs) }
                        )
                    }
                }
            }

            // Hot Remixes section (Remix)
            if (remixSongs.isNotEmpty()) {
                SectionTitle(title = translate("ریمیکس‌های داغ", "Hot Remixes", viewModel))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(remixSongs) { song ->
                        VerticalBigCard(
                            title = song.title,
                            subtitle = song.artist ?: translate("هنرمند ناشناس", "Unknown Artist", viewModel),
                            imageUrl = viewModel.getCoverArtUrl(song.coverArt),
                            onClick = { viewModel.playSongDirectly(song, remixSongs) }
                        )
                    }
                }
            }

            // Instrumental / lyric-less tracks (only shown when the library has tagged ones)
            if (instrumentalSongs.isNotEmpty()) {
                SectionTitle(title = translate("آهنگ‌های بی‌کلام", "Instrumental", viewModel))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(instrumentalSongs) { song ->
                        VerticalBigCard(
                            title = song.title,
                            subtitle = song.artist ?: translate("هنرمند ناشناس", "Unknown Artist", viewModel),
                            imageUrl = viewModel.getCoverArtUrl(song.coverArt),
                            onClick = { viewModel.playSongDirectly(song, instrumentalSongs) }
                        )
                    }
                }
            }

            // Energetic / workout tracks
            if (energeticSongs.isNotEmpty()) {
                SectionTitle(title = translate("پرانرژی و ورزشی", "Energetic & Workout", viewModel))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(energeticSongs) { song ->
                        VerticalBigCard(
                            title = song.title,
                            subtitle = song.artist ?: translate("هنرمند ناشناس", "Unknown Artist", viewModel),
                            imageUrl = viewModel.getCoverArtUrl(song.coverArt),
                            onClick = { viewModel.playSongDirectly(song, energeticSongs) }
                        )
                    }
                }
            }

            // Chill / relaxing tracks
            if (chillSongs.isNotEmpty()) {
                SectionTitle(title = translate("آرامش‌بخش", "Chill & Relax", viewModel))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(chillSongs) { song ->
                        VerticalBigCard(
                            title = song.title,
                            subtitle = song.artist ?: translate("هنرمند ناشناس", "Unknown Artist", viewModel),
                            imageUrl = viewModel.getCoverArtUrl(song.coverArt),
                            onClick = { viewModel.playSongDirectly(song, chillSongs) }
                        )
                    }
                }
            }

            // Random Popular Songs
            if (randomSongs.isNotEmpty()) {
                SectionTitle(title = "آهنگ‌های محبوب")
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    randomSongs.take(5).forEach { song ->
                        TrackRowItem(
                            song = song,
                            onPlay = { viewModel.playSongDirectly(song, randomSongs) },
                            viewModel = viewModel
                        )
                    }
                }
            }

            // Favorite Artists Circle List
            if (favoriteArtists.isNotEmpty()) {
                SectionTitle(title = translate("هنرمندان مورد علاقه", "Favorite Artists", viewModel))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(favoriteArtists) { favArtist ->
                        ArtistCircleCard(
                            name = favArtist.name,
                            imageUrl = favArtist.coverArtUrl ?: "",
                            onClick = {
                                onArtistClick(
                                    SubsonicArtist(
                                        id = favArtist.id,
                                        name = favArtist.name,
                                        coverArt = favArtist.coverArtUrl,
                                        albumCount = favArtist.albumCount
                                    )
                                )
                            }
                        )
                    }
                }
            }

            // Popular Artists Circle List
            if (artists.isNotEmpty()) {
                SectionTitle(title = "هنرمندان محبوب")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(artists) { artist ->
                        ArtistCircleCard(
                            name = artist.name,
                            imageUrl = viewModel.getCoverArtUrl(artist.coverArt),
                            onClick = { onArtistClick(artist) }
                        )
                    }
                }
            }
        }
        }
    }
}

// --- Section Header Title ---
@Composable
fun SectionTitle(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AppleRed)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// --- Big Vertical Artwork Card ---
@Composable
fun VerticalBigCard(
    title: String,
    subtitle: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .tvFocusable(cornerRadius = 12.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Gradient overlay at bottom for depth
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// --- Circle Artist Card ---
@Composable
fun ArtistCircleCard(
    name: String,
    imageUrl: String,
    onClick: () -> Unit,
    albumCount: Int? = null,
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .width(104.dp)
            .tvFocusable(cornerRadius = 52.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(100.dp)) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, AppleRed.copy(alpha = 0.3f), CircleShape),
                contentScale = ContentScale.Crop
            )
            if (onToggleFavorite != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onToggleFavorite() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = AppleRed,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (albumCount != null) {
            Text(
                text = "$albumCount آلبوم",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

// --- Spotify-style Quick Access Card (2×3 recent grid) ---
@Composable
fun QuickAccessCard(
    title: String,
    imageUrl: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .glassmorphic(cornerRadius = 10.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)),
            contentScale = ContentScale.Crop
        )
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

// --- Spotify Browse-style Mood Card ---
@Composable
fun MoodCard(
    label: String,
    emoji: String,
    gradientColors: List<Color>,
    songs: List<SubsonicSong>,
    fallback: List<SubsonicSong>,
    onPlay: (List<SubsonicSong>) -> Unit
) {
    val playlist = if (songs.isNotEmpty()) songs else fallback.take(10)
    Box(
        modifier = Modifier
            .size(140.dp, 88.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradientColors))
            .clickable(enabled = playlist.isNotEmpty()) { onPlay(playlist) }
            .padding(14.dp)
    ) {
        Text(
            text = emoji,
            fontSize = 28.sp,
            modifier = Modifier.align(Alignment.TopEnd)
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 18.sp,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

// --- Compact Horizontal History Card ---
@Composable
fun HorizontalMediaCard(
    title: String,
    subtitle: String?,
    imageUrl: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .width(220.dp)
            .glassmorphic(cornerRadius = 12.dp)
            .tvFocusable(cornerRadius = 12.dp)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            subtitle?.let {
                Text(
                    text = it,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// --- Modern, Minimalist Downloaded Track Row Component ---
@Composable
fun DownloadedTrackRow(
    song: SubsonicSong,
    sizeBytes: Long,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    viewModel: MusicViewModel
) {
    val sizeText = remember(sizeBytes) {
        val mb = sizeBytes.toFloat() / (1024f * 1024f)
        String.format(Locale.US, "%.1f MB", mb)
    }
    val minutes = (song.duration ?: 0) / 60
    val seconds = (song.duration ?: 0) % 60
    val durationText = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onPlay)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Cover Art with Overlay Play Button
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(10.dp))
        ) {
            AsyncImage(
                model = viewModel.getCoverArtUrl(song.coverArt),
                contentDescription = song.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Title, Artist and Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = LocalAppFontFamily.current
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist ?: "هنرمند ناشناس",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = LocalAppFontFamily.current
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Size & Duration badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = sizeText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = LocalAppFontFamily.current
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = durationText,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontFamily = LocalAppFontFamily.current
                    )
                }
            }
        }

        // Delete action button
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.08f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// Maps genre name to a display color for the tag chip
private fun genreColor(genre: String): Color = when {
    genre.contains("rock", ignoreCase = true) -> Color(0xFFE53935)
    genre.contains("pop", ignoreCase = true) || genre.contains("پاپ", ignoreCase = true) -> Color(0xFFE91E63)
    genre.contains("jazz", ignoreCase = true) -> Color(0xFF1E88E5)
    genre.contains("classical", ignoreCase = true) || genre.contains("کلاسیک", ignoreCase = true) -> Color(0xFFFFB300)
    genre.contains("electronic", ignoreCase = true) || genre.contains("edm", ignoreCase = true) -> Color(0xFF00BCD4)
    genre.contains("hip", ignoreCase = true) || genre.contains("rap", ignoreCase = true) || genre.contains("رپ", ignoreCase = true) -> Color(0xFFFF6F00)
    genre.contains("r&b", ignoreCase = true) || genre.contains("soul", ignoreCase = true) -> Color(0xFF8E24AA)
    genre.contains("country", ignoreCase = true) -> Color(0xFF795548)
    genre.contains("folk", ignoreCase = true) -> Color(0xFF388E3C)
    genre.contains("metal", ignoreCase = true) -> Color(0xFF546E7A)
    genre.contains("alternative", ignoreCase = true) || genre.contains("indie", ignoreCase = true) -> Color(0xFF26A69A)
    genre.contains("blues", ignoreCase = true) -> Color(0xFF1565C0)
    genre.contains("ایرانی", ignoreCase = true) || genre.contains("persian", ignoreCase = true) || genre.contains("iran", ignoreCase = true) -> Color(0xFF00897B)
    else -> Color(0xFF9E9E9E)
}

// --- Standard Song Row Component ---
@Composable
fun TrackRowItem(
    song: SubsonicSong,
    onPlay: () -> Unit,
    viewModel: MusicViewModel
) {
    val progressFlow = viewModel.downloadProgresses.collectAsStateWithLifecycle()
    val isDownloading = progressFlow.value.containsKey(song.id)
    val downloadProgress = progressFlow.value[song.id] ?: 0f

    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val downloadedTracks by viewModel.downloadedTracks.collectAsStateWithLifecycle()
    val currentTrack by viewModel.playbackConnection.currentTrack.collectAsStateWithLifecycle()
    val isFavorite = favoriteTracks.any { it.id == song.id }
    val isDownloaded = downloadedTracks.any { it.id == song.id }
    val isCurrentlyPlaying = currentTrack?.id == song.id

    var showPlaylistDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCurrentlyPlaying) AppleRed.copy(alpha = 0.08f) else Color.Transparent)
            .tvFocusable(cornerRadius = 8.dp)
            .clickable(onClick = onPlay)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(modifier = Modifier.size(50.dp)) {
                AsyncImage(
                    model = viewModel.getCoverArtUrl(song.coverArt),
                    contentDescription = song.title,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                if (isCurrentlyPlaying) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "در حال پخش",
                            tint = AppleRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = song.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentlyPlaying) AppleRed else MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                val sizeBytes = if (isDownloaded) downloadedTracks.find { it.id == song.id }?.sizeBytes ?: 0L else 0L
                val sizeMb = if (sizeBytes > 0) String.format(Locale.US, "%.1f MB", sizeBytes.toFloat() / (1024 * 1024)) else ""

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = song.artist ?: "هنرمند ناشناس",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isDownloaded && sizeMb.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = sizeMb,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (!song.genre.isNullOrBlank()) {
                        val gc = genreColor(song.genre!!)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(gc.copy(alpha = 0.14f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = song.genre!!,
                                fontSize = 9.sp,
                                color = gc,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Download button indicating status
            if (isDownloading) {
                CircularProgressIndicator(
                    progress = { downloadProgress },
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(
                    onClick = {
                        if (isDownloaded) {
                            viewModel.deleteDownload(song.id)
                        } else {
                            viewModel.startDownload(song)
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isDownloaded) Icons.Default.OfflinePin else Icons.Outlined.FileDownload,
                        contentDescription = "دانلود",
                        tint = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            IconButton(onClick = { viewModel.toggleFavoriteTrack(song) }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "مورد علاقه",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = { showPlaylistDialog = true }) {
                Icon(
                    imageVector = Icons.Outlined.PlaylistAdd,
                    contentDescription = "افزودن به پلی‌لیست",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }

    if (showPlaylistDialog) {
        PlaylistSelectorDialog(
            playlists = viewModel.localPlaylists.value,
            onDismiss = { showPlaylistDialog = false },
            onSelect = { playlistId ->
                viewModel.addTrackToPlaylist(playlistId, song)
                showPlaylistDialog = false
            }
        )
    }
}

// --- Dialog to Choose Playlist ---
@Composable
fun PlaylistSelectorDialog(
    playlists: List<LocalPlaylist>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "افزودن به لیست پخش",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                if (playlists.isEmpty()) {
                    Text(
                        text = "هیچ لیست پخشی یافت نشد. ابتدا یکی بسازید.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 250.dp)
                    ) {
                        items(playlists) { playlist ->
                            Text(
                                text = playlist.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(playlist.id) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            HorizontalDivider()
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleRed)
                ) {
                    Text("بستن")
                }
            }
        }
    }
}

// --- Search Screen ---
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    onArtistClick: (SubsonicArtist) -> Unit,
    onAlbumClick: (SubsonicAlbum) -> Unit
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val history by viewModel.searchHistory.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val trendingAlbums by viewModel.trendingAlbums.collectAsStateWithLifecycle()
    var activeFilter by remember { mutableStateOf("همه") }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var selectedYear by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current

    // Voice recognizer intent setup
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            spokenText?.let {
                viewModel.updateSearchQuery(it)
                viewModel.addSearchHistory(it)
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Page Title
            Text(
                text = translate("جستجو", "Search", viewModel),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LocalAppFontFamily.current,
                modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp)
            )

            // Search Input Header — modern pill-shaped search bar with embedded mic
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(27.dp))
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = AppleRed,
                    modifier = Modifier.padding(start = 10.dp).size(22.dp)
                )
                BasicTextField(
                    value = query,
                    onValueChange = { input -> viewModel.updateSearchQuery(input) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 15.sp,
                        fontFamily = LocalAppFontFamily.current,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(AppleRed),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_input"),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) {
                                Text(
                                    text = translate("جستجو آهنگ، هنرمند، آلبوم...", "Search songs, artists, albums...", viewModel),
                                    fontSize = 15.sp,
                                    fontFamily = LocalAppFontFamily.current,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
                // Voice Search
                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR")
                                putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("fa-IR", "en-US"))
                                putExtra(RecognizerIntent.EXTRA_PROMPT, if (viewModel.language.value == "fa") "بگویید..." else "Speak now...")
                            }
                            voiceLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "جستجوی صوتی در دسترس نیست", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .background(AppleRed, CircleShape)
                        .size(40.dp)
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice Search", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            // Interactive Category Filter Pills (Visible when searching)
            if (query.isNotEmpty() && !isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf("همه", "آهنگ‌ها", "آلبوم‌ها", "هنرمندان")
                    tabs.forEach { tab ->
                        val isSelected = activeFilter == tab
                        val backgroundColor = if (isSelected) AppleRed else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(backgroundColor)
                                .clickable { activeFilter = tab }
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab,
                                color = textColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = LocalAppFontFamily.current
                            )
                        }
                    }
                }

                // Genre / Year filter chips, derived from current search results
                val availableGenres = remember(searchResults) {
                    ((searchResults.album ?: emptyList()).mapNotNull { it.genre } +
                        (searchResults.song ?: emptyList()).mapNotNull { it.genre })
                        .distinct().sorted()
                }
                val availableYears = remember(searchResults) {
                    ((searchResults.album ?: emptyList()).mapNotNull { it.year } +
                        (searchResults.song ?: emptyList()).mapNotNull { it.year })
                        .distinct().sortedDescending()
                }
                if (availableGenres.isNotEmpty() || availableYears.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableGenres.forEach { genre ->
                            val isSelected = selectedGenre == genre
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) AppleRed.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .border(1.dp, if (isSelected) AppleRed else Color.Transparent, RoundedCornerShape(14.dp))
                                    .clickable { selectedGenre = if (isSelected) null else genre }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = genre,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) AppleRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontFamily = LocalAppFontFamily.current
                                )
                            }
                        }
                        availableYears.forEach { year ->
                            val isSelected = selectedYear == year
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) AppleRed.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .border(1.dp, if (isSelected) AppleRed else Color.Transparent, RoundedCornerShape(14.dp))
                                    .clickable { selectedYear = if (isSelected) null else year }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = year.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) AppleRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontFamily = LocalAppFontFamily.current
                                )
                            }
                        }
                    }
                }
            }

            // Spacer for clean layout
            Spacer(modifier = Modifier.height(2.dp))

            if (isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppleRed)
                }
            } else if (query.isEmpty()) {
                // Search History, Suggestions & Explore Categories (Scrollable)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (trendingAlbums.isNotEmpty()) {
                        Text(
                            text = translate("پرطرفدار", "Trending", viewModel),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            fontFamily = LocalAppFontFamily.current
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(trendingAlbums) { album ->
                                VerticalBigCard(
                                    title = album.name,
                                    subtitle = album.artist ?: translate("هنرمند ناشناس", "Unknown Artist", viewModel),
                                    imageUrl = viewModel.getCoverArtUrl(album.coverArt),
                                    onClick = { onAlbumClick(album) }
                                )
                            }
                        }
                    }

                    if (history.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "تاریخچه جستجو", fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = LocalAppFontFamily.current)
                            TextButton(onClick = { viewModel.clearSearchHistory() }) {
                                Text("پاک کردن همه", color = AppleRed, fontFamily = LocalAppFontFamily.current)
                            }
                        }

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            history.take(6).forEach { hist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.updateSearchQuery(hist)
                                            viewModel.addSearchHistory(hist)
                                        }
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = Color.Gray)
                                    Text(text = hist, fontSize = 14.sp, fontFamily = LocalAppFontFamily.current)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            }
                        }
                    }

                    // Quick Search Suggestions
                    Text(text = "پیشنهادهای جستجو", fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = LocalAppFontFamily.current)
                    val suggestions = listOf("The Weeknd", "Taylor Swift", "Coldplay", "Adele", "Eminem", "Billie Eilish", "Dua Lipa")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        suggestions.forEach { sugg ->
                            SuggestionChip(
                                onClick = {
                                    viewModel.updateSearchQuery(sugg)
                                    viewModel.addSearchHistory(sugg)
                                },
                                label = { Text(sugg, fontFamily = LocalAppFontFamily.current) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Beautiful Explore Categories Grid
                    Text(
                        text = translate("مرور دسته‌بندی‌ها", "Browse Categories", viewModel),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = LocalAppFontFamily.current
                    )

                    val categories = listOf(
                        Triple("پاپ ایرانی", "Iranian Pop", Brush.linearGradient(colors = listOf(Color(0xFFFF5E62), Color(0xFFFF9966)))),
                        Triple("کلاسیک و ملایم", "Classical & Calm", Brush.linearGradient(colors = listOf(Color(0xFF4CA1AF), Color(0xFF2C3E50)))),
                        Triple("هیپ هاپ و رپ", "Hip Hop & Rap", Brush.linearGradient(colors = listOf(Color(0xFF11998e), Color(0xFF38ef7d)))),
                        Triple("راک و متال", "Rock & Metal", Brush.linearGradient(colors = listOf(Color(0xFF1F1C2C), Color(0xFF928DAB)))),
                        Triple("عاشقانه و غمگین", "Romantic & Sad", Brush.linearGradient(colors = listOf(Color(0xFFEA384D), Color(0xFFD31027)))),
                        Triple("موزیک‌های جدید", "New Releases", Brush.linearGradient(colors = listOf(Color(0xFFf2709c), Color(0xFFff9472)))),
                        Triple("باشگاه و ورزش", "Workout & Energy", Brush.linearGradient(colors = listOf(Color(0xFFF3904F), Color(0xFF3B4371)))),
                        Triple("تایمر آرامش", "Meditation & Focus", Brush.linearGradient(colors = listOf(Color(0xFF00c6ff), Color(0xFF0072ff))))
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val chunkedCategories = categories.chunked(2)
                        chunkedCategories.forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { (nameFa, nameEn, gradient) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(72.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(gradient)
                                            .clickable {
                                                viewModel.updateSearchQuery(nameEn)
                                                viewModel.addSearchHistory(nameEn)
                                            }
                                            .padding(12.dp),
                                        contentAlignment = Alignment.BottomStart
                                    ) {
                                        Text(
                                            text = nameEn,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = LocalAppFontFamily.current
                                        )
                                    }
                                }
                                if (pair.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else {
                // Unified, Minimalist Search Results Panel ordered: Artists -> Albums -> Songs -> Downloaded
                val downloadedTracks by viewModel.downloadedTracks.collectAsStateWithLifecycle()
                val artists = searchResults.artist ?: emptyList()
                val albums = (searchResults.album ?: emptyList()).filter { a ->
                    (selectedGenre == null || a.genre == selectedGenre) && (selectedYear == null || a.year == selectedYear)
                }
                val songs = (searchResults.song ?: emptyList()).filter { s ->
                    (selectedGenre == null || s.genre == selectedGenre) && (selectedYear == null || s.year == selectedYear)
                }
                val offlineMatches = remember(downloadedTracks, query) {
                    if (query.isBlank()) emptyList()
                    else downloadedTracks.filter { t ->
                        t.title.contains(query, ignoreCase = true) || t.artist.contains(query, ignoreCase = true)
                    }
                }

                val showArtists = activeFilter == "همه" || activeFilter == "هنرمندان"
                val showAlbums = activeFilter == "همه" || activeFilter == "آلبوم‌ها"
                val showSongs = activeFilter == "همه" || activeFilter == "آهنگ‌ها"

                val isResultEmpty = (artists.isEmpty() || !showArtists) &&
                                    (albums.isEmpty() || !showAlbums) &&
                                    (songs.isEmpty() || !showSongs) &&
                                    offlineMatches.isEmpty()

                if (isResultEmpty) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = translate("نتیجه‌ای یافت نشد", "No results found", viewModel),
                            color = Color.Gray,
                            fontFamily = LocalAppFontFamily.current,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Artists (First)
                        if (artists.isNotEmpty() && showArtists) {
                            item {
                                Text(
                                    text = translate("هنرمندان", "Artists", viewModel),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = AppleRed,
                                    fontFamily = LocalAppFontFamily.current,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(artists) { artist ->
                                        Column(
                                            modifier = Modifier
                                                .width(90.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onArtistClick(artist) }
                                                .padding(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            AsyncImage(
                                                model = viewModel.getCoverArtUrl(artist.coverArt),
                                                contentDescription = artist.name,
                                                modifier = Modifier
                                                    .size(70.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                            Text(
                                                text = artist.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontFamily = LocalAppFontFamily.current,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Albums (Second)
                        if (albums.isNotEmpty() && showAlbums) {
                            item {
                                Text(
                                    text = translate("آلبوم‌ها", "Albums", viewModel),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = AppleRed,
                                    fontFamily = LocalAppFontFamily.current,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(albums) { album ->
                                        Column(
                                            modifier = Modifier
                                                .width(110.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onAlbumClick(album) }
                                                .padding(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            AsyncImage(
                                                model = viewModel.getCoverArtUrl(album.coverArt),
                                                contentDescription = album.name,
                                                modifier = Modifier
                                                    .size(100.dp)
                                                    .clip(RoundedCornerShape(12.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                            Text(
                                                text = album.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontFamily = LocalAppFontFamily.current,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = album.artist ?: "",
                                                fontSize = 9.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontFamily = LocalAppFontFamily.current,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Songs (Third)
                        if (songs.isNotEmpty() && showSongs) {
                            item {
                                Text(
                                    text = translate("آهنگ‌ها", "Songs", viewModel),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = AppleRed,
                                    fontFamily = LocalAppFontFamily.current,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                            items(songs) { song ->
                                TrackRowItem(
                                    song = song,
                                    onPlay = { viewModel.playSongDirectly(song, songs) },
                                    viewModel = viewModel
                                )
                            }
                        }

                        // 4. Downloaded / Offline matches
                        if (offlineMatches.isNotEmpty()) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OfflinePin,
                                        contentDescription = null,
                                        tint = AppleRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = translate("دانلودشده", "Downloaded", viewModel),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = AppleRed,
                                        fontFamily = LocalAppFontFamily.current
                                    )
                                }
                            }
                            items(offlineMatches) { track ->
                                val offlineSong = SubsonicSong(
                                    id = track.id,
                                    title = track.title,
                                    artist = track.artist,
                                    album = track.album,
                                    duration = track.duration
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.playDownloadedTrack(track) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (track.coverArtPath != null) {
                                            AsyncImage(
                                                model = track.coverArtPath,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = AppleRed, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(track.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = LocalAppFontFamily.current)
                                        Text(track.artist, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = LocalAppFontFamily.current)
                                    }
                                    Icon(Icons.Default.OfflinePin, contentDescription = null, tint = AppleRed.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- FlowRow Polyfill to avoid Experimental APIs breaking ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable FlowRowScope.() -> Unit
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = content
    )
}

// --- Library Screen ---
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onItemClick: (String) -> Unit,
    onPlaylistClick: (LocalPlaylist) -> Unit
) {
    val playlists by viewModel.localPlaylists.collectAsStateWithLifecycle()
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "کتابخانه",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Section item selectors
        val categories = listOf(
            Triple("songs", "آهنگ‌های من", Icons.Outlined.MusicNote),
            Triple("albums", "آلبوم‌های من", Icons.Outlined.Album),
            Triple("artists", "هنرمندان من", Icons.Outlined.People),
            Triple("all_artists", "مرور همه هنرمندان", Icons.Outlined.Groups),
            Triple("downloads", "دانلودهای آفلاین", Icons.Outlined.OfflinePin)
        )

        items(categories) { cat ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(cornerRadius = 14.dp)
                    .clickable { onItemClick(cat.first) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppleRed.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = cat.third, contentDescription = null, tint = AppleRed, modifier = Modifier.size(22.dp))
                }
                Text(text = cat.second, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
            }
        }

        // Playlists Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "لیست‌های پخش من", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                IconButton(onClick = { showCreatePlaylistDialog = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "ایجاد پلی‌لیست", tint = AppleRed)
                }
            }
        }

        if (playlists.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("هیچ لیست پخشی ایجاد نشده است.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            items(playlists) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphic(cornerRadius = 14.dp)
                        .clickable { onPlaylistClick(playlist) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AppleRed.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.QueueMusic, contentDescription = null, tint = AppleRed, modifier = Modifier.size(24.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = playlist.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = playlist.description ?: "بدون توضیح", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }

                    IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        var playlistName by remember { mutableStateOf("") }
        var playlistDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("ایجاد لیست پخش جدید") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = playlistName,
                        onValueChange = { playlistName = it },
                        label = { Text("نام لیست پخش") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppleRed)
                    )
                    OutlinedTextField(
                        value = playlistDesc,
                        onValueChange = { playlistDesc = it },
                        label = { Text("توضیحات (اختیاری)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppleRed)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            viewModel.createPlaylist(playlistName, playlistDesc)
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text("ایجاد", color = AppleRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("انصراف", color = Color.Gray)
                }
            }
        )
    }
}

// --- Sub-Library Items Explorer (Lliked Songs, Albums, Artists, Downloads) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubLibraryItemsView(
    type: String,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onAlbumClick: (SubsonicAlbum) -> Unit,
    onArtistClick: (SubsonicArtist) -> Unit
) {
    val title = when (type) {
        "songs" -> "آهنگ‌های من"
        "albums" -> "آلبوم‌های من"
        "artists" -> "هنرمندان من"
        "all_artists" -> "مرور همه هنرمندان"
        else -> "دانلودها"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "بازگشت") // Reversed because RTL Arrow
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(bottom = 80.dp)) {
            when (type) {
                "songs" -> {
                    val tracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
                    if (tracks.isEmpty()) {
                        EmptyStateView(message = "آهنگی به لیست مورد علاقه اضافه نشده است.")
                    } else {
                        LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                            items(tracks) { fav ->
                                val song = SubsonicSong(
                                    id = fav.id,
                                    title = fav.title,
                                    artist = fav.artist,
                                    album = fav.album,
                                    coverArt = fav.coverArtUrl,
                                    duration = fav.duration
                                )
                                TrackRowItem(song = song, onPlay = { viewModel.playSongDirectly(song, tracks.map { SubsonicSong(it.id, it.title, it.album, it.artist, coverArt = it.coverArtUrl, duration = it.duration) }) }, viewModel = viewModel)
                            }
                        }
                    }
                }
                "albums" -> {
                    val albums by viewModel.favoriteAlbums.collectAsStateWithLifecycle()
                    if (albums.isEmpty()) {
                        EmptyStateView(message = "آلبومی به لیست مورد علاقه اضافه نشده است.")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(albums) { fav ->
                                val album = SubsonicAlbum(id = fav.id, name = fav.name, artist = fav.artist, coverArt = fav.coverArtUrl, songCount = fav.songCount)
                                VerticalBigCard(title = fav.name, subtitle = fav.artist, imageUrl = fav.coverArtUrl ?: "", onClick = { onAlbumClick(album) })
                            }
                        }
                    }
                }
                "artists" -> {
                    val artists by viewModel.favoriteArtists.collectAsStateWithLifecycle()
                    if (artists.isEmpty()) {
                        EmptyStateView(message = "هنرمندی به لیست مورد علاقه اضافه نشده است.")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(artists) { fav ->
                                val artist = SubsonicArtist(id = fav.id, name = fav.name, coverArt = fav.coverArtUrl, albumCount = fav.albumCount)
                                ArtistCircleCard(name = fav.name, imageUrl = fav.coverArtUrl ?: "", onClick = { onArtistClick(artist) })
                            }
                        }
                    }
                }
                "all_artists" -> {
                    val allArtists by viewModel.artistsList.collectAsStateWithLifecycle()
                    val favArtists by viewModel.favoriteArtists.collectAsStateWithLifecycle()
                    var query by remember { mutableStateOf("") }
                    val filteredSorted = remember(allArtists, query) {
                        allArtists.filter { it.name.contains(query, ignoreCase = true) }
                            .sortedBy { it.name.lowercase() }
                    }
                    val gridState = rememberLazyGridState()
                    val coroutineScope = rememberCoroutineScope()
                    val letterIndex = remember(filteredSorted) {
                        val map = linkedMapOf<String, Int>()
                        filteredSorted.forEachIndexed { index, artist ->
                            val letter = artist.name.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                            if (!map.containsKey(letter)) map[letter] = index
                        }
                        map
                    }
                    Column(modifier = Modifier.fillMaxSize()) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text(translate("جستجوی هنرمند...", "Search artist...", viewModel)) },
                            singleLine = true,
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppleRed)
                        )
                        if (filteredSorted.isEmpty()) {
                            EmptyStateView(message = translate("هنرمندی یافت نشد.", "No artists found.", viewModel))
                        } else {
                            Row(modifier = Modifier.fillMaxSize()) {
                                LazyVerticalGrid(
                                    state = gridState,
                                    columns = GridCells.Fixed(3),
                                    contentPadding = PaddingValues(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(filteredSorted) { artist ->
                                        val isFav = favArtists.any { it.id == artist.id }
                                        ArtistCircleCard(
                                            name = artist.name,
                                            imageUrl = viewModel.getCoverArtUrl(artist.coverArt),
                                            onClick = { onArtistClick(artist) },
                                            albumCount = artist.albumCount,
                                            isFavorite = isFav,
                                            onToggleFavorite = { viewModel.toggleFavoriteArtist(artist) }
                                        )
                                    }
                                }
                                if (letterIndex.size > 1) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(24.dp)
                                            .padding(vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        letterIndex.forEach { (letter, index) ->
                                            Text(
                                                text = letter,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AppleRed,
                                                modifier = Modifier
                                                    .clickable {
                                                        coroutineScope.launch { gridState.scrollToItem(index) }
                                                    }
                                                    .padding(2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "downloads" -> {
                    val downloads by viewModel.downloadedTracks.collectAsStateWithLifecycle()
                    val downloadLocation by viewModel.downloadLocation.collectAsStateWithLifecycle()
                    val activeDownloads by viewModel.downloadProgresses.collectAsStateWithLifecycle()
                    val context = LocalContext.current

                    var searchQuery by remember { mutableStateOf("") }
                    var sortBy by remember { mutableStateOf("date") } // "date", "title", "artist", "size"
                    var showLocationDialog by remember { mutableStateOf(false) }
                    var showDeleteAllDialog by remember { mutableStateOf(false) }
                    var customLocationInput by remember { mutableStateOf("") }

                    // Localize labels dynamically
                    val storageLabel = translate("مدیریت دانلودها و حافظه", "Downloads & Storage Manager", viewModel)
                    val locationLabel = translate("مکان ذخیره:", "Storage path:", viewModel)
                    val changeBtn = translate("تغییر مکان", "Change Path", viewModel)
                    val defaultLoc = translate("پیش‌فرض سیستم", "System Default", viewModel)
                    val usedLabel = translate("دانلودهای های موزیک:", "High Music Downloads:", viewModel)
                    val freeLabel = translate("حافظه آزاد دستگاه:", "Free Space:", viewModel)
                    val otherLabel = translate("سایر فایل‌های سیستم:", "Other Files:", viewModel)
                    val searchPlaceholder = translate("جستجو در دانلودها...", "Search in downloads...", viewModel)
                    val noDownloads = translate("هیچ آهنگی دانلود نشده است.", "No tracks downloaded yet.", viewModel)
                    val noResults = translate("نتیجه‌ای یافت نشد.", "No results found.", viewModel)
                    val delAllLabel = translate("حذف همه دانلودها", "Delete All Downloads", viewModel)

                    // Estimate exact storage from DB
                    val totalBytes = downloads.sumOf { it.sizeBytes }
                    val totalMBNum = totalBytes.toFloat() / (1024f * 1024f)
                    val totalMB = String.format(Locale.US, "%.1f MB", totalMBNum)

                    val stat = android.os.StatFs(context.filesDir.path)
                    val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong
                    val freeMBNum = bytesAvailable.toFloat() / (1024f * 1024f)
                    val freeMB = String.format(Locale.US, "%.1f MB", freeMBNum)

                    val totalPartitionBytes = stat.blockCountLong * stat.blockSizeLong
                    val otherBytes = maxOf(0L, totalPartitionBytes - bytesAvailable - totalBytes)
                    val otherMBNum = otherBytes.toFloat() / (1024f * 1024f)
                    val otherMB = String.format(Locale.US, "%.1f MB", otherMBNum)

                    val totalSpaceMBNum = totalPartitionBytes.toFloat() / (1024f * 1024f)

                    val sMusicWeight = if (totalSpaceMBNum > 0) totalMBNum / totalSpaceMBNum else 0.01f
                    val otherFilesWeight = if (totalSpaceMBNum > 0) otherMBNum / totalSpaceMBNum else 0.01f
                    val freeSpaceWeight = if (totalSpaceMBNum > 0) freeMBNum / totalSpaceMBNum else 0.98f

                    val filteredDownloads = downloads.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                                (it.artist ?: "").contains(searchQuery, ignoreCase = true)
                    }

                    val sortedDownloads = remember(filteredDownloads, sortBy) {
                        when (sortBy) {
                            "title" -> filteredDownloads.sortedBy { it.title }
                            "artist" -> filteredDownloads.sortedBy { it.artist ?: "" }
                            "size" -> filteredDownloads.sortedByDescending { it.sizeBytes }
                            else -> filteredDownloads.sortedByDescending { it.timestamp }
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Advanced Storage & Management Dashboard Card with Custom Visual Graph
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = storageLabel,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontFamily = LocalAppFontFamily.current
                                        )
                                        Text(
                                            text = translate("${downloads.size} آهنگ ذخیره شده", "${downloads.size} Tracks Saved", viewModel),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            fontFamily = LocalAppFontFamily.current
                                        )
                                    }

                                    IconButton(
                                        onClick = { showDeleteAllDialog = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = delAllLabel,
                                            tint = AppleRed
                                        )
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                // Dynamic Visual Partition Bar (Advanced Storage Graph)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                    ) {
                                        if (sMusicWeight > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .weight(maxOf(0.01f, sMusicWeight))
                                                    .background(AppleRed)
                                            )
                                        }
                                        if (otherFilesWeight > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .weight(maxOf(0.01f, otherFilesWeight))
                                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
                                            )
                                        }
                                        if (freeSpaceWeight > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxHeight()
                                                    .weight(maxOf(0.01f, freeSpaceWeight))
                                            )
                                        }
                                    }

                                    // Storage Legend
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AppleRed))
                                                Text(text = "$usedLabel $totalMB", fontSize = 10.sp, fontFamily = LocalAppFontFamily.current, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)))
                                                Text(text = "$otherLabel $otherMB", fontSize = 10.sp, fontFamily = LocalAppFontFamily.current, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.1f)).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), CircleShape))
                                            Text(text = "$freeLabel $freeMB", fontSize = 10.sp, fontFamily = LocalAppFontFamily.current, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                        }
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                // Storage Location display & Edit button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = locationLabel,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            fontFamily = LocalAppFontFamily.current
                                        )
                                        Text(
                                            text = if (downloadLocation.isBlank()) defaultLoc else downloadLocation,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = LocalAppFontFamily.current,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            customLocationInput = downloadLocation
                                            showLocationDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(text = changeBtn, fontSize = 11.sp, fontFamily = LocalAppFontFamily.current, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Active Download Queue Indicator if any
                        if (activeDownloads.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .border(1.dp, AppleRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = AppleRed.copy(alpha = 0.08f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = AppleRed,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = translate("در حال دانلود موسیقی...", "Downloading Tracks...", viewModel),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = LocalAppFontFamily.current,
                                            color = AppleRed
                                        )
                                        Text(
                                            text = translate(
                                                "${activeDownloads.size} آهنگ در صف دانلود قرار دارد.",
                                                "${activeDownloads.size} tracks in download queue.",
                                                viewModel
                                            ),
                                            fontSize = 10.sp,
                                            fontFamily = LocalAppFontFamily.current,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }

                        // Search Bar inside downloads
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(searchPlaceholder, fontFamily = LocalAppFontFamily.current) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        )

                        // Advanced Sorting Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = translate("مرتب‌سازی بر اساس:", "Sort by:", viewModel),
                                fontSize = 11.sp,
                                fontFamily = LocalAppFontFamily.current,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            
                            val sortOptions = listOf(
                                "date" to translate("جدیدترین", "Newest", viewModel),
                                "title" to translate("عنوان", "Title", viewModel),
                                "artist" to translate("هنرمند", "Artist", viewModel),
                                "size" to translate("حجم فایل", "File Size", viewModel)
                            )
                            
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(sortOptions) { option ->
                                    val isSelected = sortBy == option.first
                                    val chipBg = if (isSelected) AppleRed.copy(alpha = 0.15f) else Color.Transparent
                                    val chipBorderColor = if (isSelected) AppleRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    val chipTextColor = if (isSelected) AppleRed else MaterialTheme.colorScheme.onSurface
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(chipBg)
                                            .border(1.dp, chipBorderColor, RoundedCornerShape(16.dp))
                                            .clickable { sortBy = option.first }
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = option.second,
                                            fontSize = 10.sp,
                                            fontFamily = LocalAppFontFamily.current,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = chipTextColor
                                        )
                                    }
                                }
                            }
                        }

                        // Download List
                        if (downloads.isEmpty()) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                EmptyStateView(message = noDownloads)
                            }
                        } else if (sortedDownloads.isEmpty()) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                EmptyStateView(message = noResults)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 16.dp)
                            ) {
                                items(sortedDownloads) { record ->
                                    val song = SubsonicSong(
                                        id = record.id,
                                        title = record.title,
                                        artist = record.artist,
                                        album = record.album,
                                        coverArt = record.coverArtPath,
                                        duration = record.duration
                                    )
                                    DownloadedTrackRow(
                                        song = song,
                                        sizeBytes = record.sizeBytes,
                                        onPlay = {
                                            viewModel.playSongDirectly(
                                                song,
                                                filteredDownloads.map {
                                                    SubsonicSong(
                                                        it.id,
                                                        it.title,
                                                        it.album,
                                                        it.artist,
                                                        coverArt = it.coverArtPath,
                                                        duration = it.duration
                                                    )
                                                }
                                            )
                                        },
                                        onDelete = {
                                            viewModel.deleteDownload(song.id)
                                        },
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    }

                    // Change Download Location Dialog
                    if (showLocationDialog) {
                        AlertDialog(
                            onDismissRequest = { showLocationDialog = false },
                            title = { Text(translate("تغییر مکان ذخیره‌سازی", "Change Storage Path", viewModel), fontFamily = LocalAppFontFamily.current, fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = translate(
                                            "مسیر دلخواه پوشه ذخیره‌سازی فایل‌های صوتی دانلود شده را وارد کنید (خالی بگذارید تا مسیر پیش‌فرض استفاده شود):",
                                            "Enter custom directory path to save audio downloads (leave empty for default path):",
                                            viewModel
                                        ),
                                        fontSize = 13.sp,
                                        fontFamily = LocalAppFontFamily.current
                                    )
                                    OutlinedTextField(
                                        value = customLocationInput,
                                        onValueChange = { customLocationInput = it },
                                        placeholder = { Text("/sdcard/Music", fontSize = 13.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.setDownloadLocation(customLocationInput)
                                        showLocationDialog = false
                                        val msg = if (viewModel.language.value == "fa") "مکان دانلود با موفقیت تغییر کرد." else "Download path updated."
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text(translate("ذخیره", "Save", viewModel), fontFamily = LocalAppFontFamily.current, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showLocationDialog = false }) {
                                    Text(translate("انصراف", "Cancel", viewModel), fontFamily = LocalAppFontFamily.current, color = Color.Gray)
                                }
                            }
                        )
                    }

                    // Delete All Downloads Confirmation Dialog
                    if (showDeleteAllDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteAllDialog = false },
                            title = { Text(translate("حذف همه فایل‌های دانلود شده", "Delete All Downloads", viewModel), fontFamily = LocalAppFontFamily.current, fontWeight = FontWeight.Bold) },
                            text = {
                                Text(
                                    text = translate(
                                        "آیا واقعاً می‌خواهید تمامی آهنگ‌های دانلود شده آفلاین را حذف کنید؟ این عمل غیرقابل بازگشت است.",
                                        "Are you sure you want to delete all offline downloaded tracks? This action cannot be undone.",
                                        viewModel
                                    ),
                                    fontSize = 14.sp,
                                    fontFamily = LocalAppFontFamily.current
                                )
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        viewModel.clearAllDownloads()
                                        showDeleteAllDialog = false
                                        val msg = if (viewModel.language.value == "fa") "تمامی فایل‌های آفلاین حذف شدند." else "All offline files deleted."
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text(translate("حذف همه", "Delete All", viewModel), fontFamily = LocalAppFontFamily.current, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteAllDialog = false }) {
                                    Text(translate("انصراف", "Cancel", viewModel), fontFamily = LocalAppFontFamily.current, color = Color.Gray)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
    }
}

// --- Artist Detail Subscreen ---
@Composable
fun AlbumGridCard(
    album: SubsonicAlbum,
    onClick: () -> Unit,
    viewModel: MusicViewModel
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "AlbumCardScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tvFocusable(cornerRadius = 16.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = viewModel.getCoverArtUrl(album.coverArt),
                contentDescription = album.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Year and Song Count badges on cover art
            val tracksLabel = translate("آهنگ", "tracks", viewModel)
            val badgeText = remember(album) {
                val yearPart = if (album.year != null && album.year!! > 0) "${album.year} • " else ""
                val songCountPart = "${album.songCount ?: 0} $tracksLabel"
                "$yearPart$songCountPart"
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 9.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LocalAppFontFamily.current
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = album.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontFamily = LocalAppFontFamily.current,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = album.genre ?: translate("بدون سبک", "No Genre", viewModel),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontFamily = LocalAppFontFamily.current,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

fun getSongArtists(song: SubsonicSong, artistsList: List<SubsonicArtist>): List<SubsonicArtist> {
    val mainArtistName = song.artist ?: return emptyList()
    val delimiters = listOf(" ft. ", " feat. ", " featuring ", " & ", " and ", " , ", ",", "و", " x ", " X ")
    var parts = listOf(mainArtistName)
    for (delim in delimiters) {
        parts = parts.flatMap { part ->
            part.split(delim).map { it.trim() }.filter { it.isNotEmpty() }
        }
    }
    
    val titleFeatured = mutableListOf<String>()
    val title = song.title
    val ftDelims = listOf(" ft. ", " feat. ", " featuring ", " Ft. ", " Feat. ")
    for (delim in ftDelims) {
        if (title.contains(delim)) {
            val ftIndex = title.indexOf(delim)
            val featuredPart = title.substring(ftIndex + delim.length)
                .replace(")", "")
                .replace("]", "")
            var featuredParts = listOf(featuredPart)
            for (d in listOf(" & ", " and ", " , ", ",", "و", " x ")) {
                featuredParts = featuredParts.flatMap { p -> p.split(d).map { it.trim() }.filter { it.isNotEmpty() } }
            }
            titleFeatured.addAll(featuredParts)
        }
    }
    
    val allArtistNames = (parts + titleFeatured).distinct()
    val result = mutableListOf<SubsonicArtist>()
    for (name in allArtistNames) {
        val matchedArtist = artistsList.find { it.name.trim().equals(name, ignoreCase = true) }
        if (matchedArtist != null) {
            result.add(matchedArtist)
        } else {
            val dummyId = song.artistId ?: "dummy_${name.hashCode()}"
            result.add(
                SubsonicArtist(
                    id = dummyId,
                    name = name,
                    coverArt = song.coverArt,
                    albumCount = 1
                )
            )
        }
    }
    
    if (result.isEmpty() && mainArtistName.isNotEmpty()) {
        result.add(
            SubsonicArtist(
                id = song.artistId ?: "dummy_main",
                name = mainArtistName,
                coverArt = song.coverArt,
                albumCount = 1
            )
        )
    }
    
    return result.distinctBy { it.name.lowercase().trim() }
}

data class ArtistExtraDetails(
    val country: String,
    val birthYearOrActive: String,
    val instagram: String? = null,
    val twitter: String? = null,
    val youtube: String? = null,
    val spotify: String? = null
)

fun getArtistExtraDetails(name: String, language: String): ArtistExtraDetails {
    val normalizedName = name.trim()
    val isFa = language == "fa"
    
    // Famous artist exact matching
    return when {
        normalizedName.contains("Taylor Swift", ignoreCase = true) -> ArtistExtraDetails(
            country = if (isFa) "ایالات متحده" else "United States",
            birthYearOrActive = if (isFa) "متولد ۱۹۸۹" else "Born 1989",
            instagram = "taylorswift",
            twitter = "taylorswift13",
            youtube = "TaylorSwift",
            spotify = "06HL4z00K2zSTw0szgqgA8"
        )
        normalizedName.contains("The Weeknd", ignoreCase = true) -> ArtistExtraDetails(
            country = if (isFa) "کانادا" else "Canada",
            birthYearOrActive = if (isFa) "متولد ۱۹۹۰" else "Born 1990",
            instagram = "theweeknd",
            twitter = "theweeknd",
            youtube = "TheWeeknd",
            spotify = "1Xyo493mqZ0hxKHTT3vYr3"
        )
        normalizedName.contains("Coldplay", ignoreCase = true) -> ArtistExtraDetails(
            country = if (isFa) "انگلستان" else "United Kingdom",
            birthYearOrActive = if (isFa) "تأسیس ۱۹۹۶" else "Formed 1996",
            instagram = "coldplay",
            twitter = "coldplay",
            youtube = "coldplay",
            spotify = "4gzpq7vSSTNTL6g8Sg7b7F"
        )
        normalizedName.contains("Adele", ignoreCase = true) -> ArtistExtraDetails(
            country = if (isFa) "انگلستان" else "United Kingdom",
            birthYearOrActive = if (isFa) "متولد ۱۹۸۸" else "Born 1988",
            instagram = "adele",
            twitter = "adele",
            youtube = "adele",
            spotify = "4dpARuHxo51GoX761z87Xz"
        )
        normalizedName.contains("Eminem", ignoreCase = true) -> ArtistExtraDetails(
            country = if (isFa) "ایالات متحده" else "United States",
            birthYearOrActive = if (isFa) "متولد ۱۹۷۲" else "Born 1972",
            instagram = "eminem",
            twitter = "eminem",
            youtube = "eminem",
            spotify = "7dG12g9vHfv9ve63b0of0Z"
        )
        normalizedName.contains("Billie Eilish", ignoreCase = true) -> ArtistExtraDetails(
            country = if (isFa) "ایالات متحده" else "United States",
            birthYearOrActive = if (isFa) "متولد ۲۰۰۱" else "Born 2001",
            instagram = "billieeilish",
            twitter = "billieeilish",
            youtube = "BillieEilish",
            spotify = "6qqNV0v67gmgP63Z6KFymH"
        )
        normalizedName.contains("Dua Lipa", ignoreCase = true) -> ArtistExtraDetails(
            country = if (isFa) "انگلستان" else "United Kingdom",
            birthYearOrActive = if (isFa) "متولد ۱۹۹۵" else "Born 1995",
            instagram = "dualipa",
            twitter = "dualipa",
            youtube = "dualipa",
            spotify = "6M2v0gZdS6g7UP9jYgZ4S4"
        )
        normalizedName.contains("Drake", ignoreCase = true) -> ArtistExtraDetails(
            country = if (isFa) "کانادا" else "Canada",
            birthYearOrActive = if (isFa) "متولد ۱۹۸۶" else "Born 1986",
            instagram = "champagnepapi",
            twitter = "drake",
            youtube = "DrakeOfficial",
            spotify = "3TVKi7XgZ480ywZ76m6X0K"
        )
        normalizedName.contains("Bruno Mars", ignoreCase = true) -> ArtistExtraDetails(
            country = if (isFa) "ایالات متحده" else "United States",
            birthYearOrActive = if (isFa) "متولد ۱۹۸۵" else "Born 1985",
            instagram = "brunomars",
            twitter = "brunomars",
            youtube = "brunomars",
            spotify = "0du5Z07I77660InNDmECY3"
        )
        // Deterministic generation for any other artist (using hash)
        else -> {
            val hash = normalizedName.hashCode().let { if (it < 0) -it else it }
            val countries = if (isFa) {
                listOf("ایالات متحده", "کانادا", "انگلستان", "ایران", "فرانسه", "سوئد", "آلمان", "استرالیا")
            } else {
                listOf("United States", "Canada", "United Kingdom", "Iran", "France", "Sweden", "Germany", "Australia")
            }
            val countrySelected = countries[hash % countries.size]
            
            val year = 1980 + (hash % 23)
            val activeText = if (isFa) "شروع فعالیت $year" else "Active since $year"
            
            val cleanInsta = normalizedName.lowercase().replace(" ", "").replace("[^a-z0-9]".toRegex(), "")
            
            ArtistExtraDetails(
                country = countrySelected,
                birthYearOrActive = activeText,
                instagram = cleanInsta,
                twitter = cleanInsta,
                youtube = cleanInsta,
                spotify = null
            )
        }
    }
}

@Composable
fun SocialIconCircle(
    brand: String, // "instagram", "twitter", "youtube", "spotify"
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (brand) {
            "instagram" -> {
                Canvas(modifier = Modifier.size(16.dp)) {
                    drawRoundRect(
                        color = color,
                        style = Stroke(width = 1.6.dp.toPx()),
                        cornerRadius = CornerRadius(4.5.dp.toPx())
                    )
                    drawCircle(
                        color = color,
                        radius = 3.2.dp.toPx(),
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                    drawCircle(
                        color = color,
                        radius = 1.dp.toPx(),
                        center = Offset(size.width * 0.73f, size.height * 0.27f)
                    )
                }
            }
            "twitter" -> {
                Text(
                    text = "𝕏",
                    color = color,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            "youtube" -> {
                Canvas(modifier = Modifier.size(16.dp)) {
                    drawRoundRect(
                        color = color,
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                    val path = Path().apply {
                        moveTo(size.width * 0.38f, size.height * 0.3f)
                        lineTo(size.width * 0.7f, size.height * 0.5f)
                        lineTo(size.width * 0.38f, size.height * 0.7f)
                        close()
                    }
                    drawPath(path, color = Color.White)
                }
            }
            "spotify" -> {
                Canvas(modifier = Modifier.size(17.dp)) {
                    drawCircle(color = color)
                    val waveColor = Color.Black
                    val strokePx = 1.5.dp.toPx()
                    
                    drawArc(
                        color = waveColor,
                        startAngle = -140f,
                        sweepAngle = 100f,
                        useCenter = false,
                        topLeft = Offset(size.width * 0.15f, size.height * 0.24f),
                        size = Size(size.width * 0.7f, size.height * 0.7f),
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = waveColor,
                        startAngle = -140f,
                        sweepAngle = 100f,
                        useCenter = false,
                        topLeft = Offset(size.width * 0.22f, size.height * 0.39f),
                        size = Size(size.width * 0.56f, size.height * 0.56f),
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = waveColor,
                        startAngle = -140f,
                        sweepAngle = 100f,
                        useCenter = false,
                        topLeft = Offset(size.width * 0.31f, size.height * 0.54f),
                        size = Size(size.width * 0.38f, size.height * 0.38f),
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            }
            else -> {
                Text(
                    text = brand.take(2).uppercase(),
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LocalAppFontFamily.current
                )
            }
        }
    }
}

fun getFallbackBiography(name: String, country: String, activeText: String, isFa: Boolean): String {
    return if (isFa) {
        "$name یکی از هنرمندان برجسته و تأثیرگذار در دنیای موسیقی است که از $country فعالیت هنری خود را آغاز کرده و همواره با آثار خلاقانه و سبک خاص خود شناخته می‌شود. $activeText، او با ترکیب احساس عمیق و نوآوری هنری توانسته طرفداران بی‌شماری را در سراسر جهان به خود جذب کند و جایگاه ویژه‌ای در تاریخ موسیقی مدرن به دست آورد."
    } else {
        "$name is a prominent and influential artist in the music industry. Hailing from $country, they have captivated global audiences with their unique style and creative expression. $activeText, they continue to push artistic boundaries, delivering memorable hits that define modern music culture."
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailView(
    artist: SubsonicArtist,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onAlbumClick: (SubsonicAlbum) -> Unit,
    onArtistClick: (SubsonicArtist) -> Unit = {}
) {
    val albums by viewModel.selectedArtistAlbums.collectAsStateWithLifecycle()
    val bioState by viewModel.selectedArtistBio.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingDetail.collectAsStateWithLifecycle()
    val topTracks by viewModel.artistTopTracks.collectAsStateWithLifecycle()
    val similarArtists by viewModel.artistSimilarArtists.collectAsStateWithLifecycle()
    val allSongs by viewModel.artistAllSongs.collectAsStateWithLifecycle()
    val isLoadingAllSongs by viewModel.isLoadingArtistAllSongs.collectAsStateWithLifecycle()
    val enrichedInfo by viewModel.enrichedArtistInfo.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val lang = viewModel.language.value
    val details = remember(artist.name, lang) { getArtistExtraDetails(artist.name, lang) }
    val biographyToShow = remember(bioState, enrichedInfo, artist.name, details, lang) {
        val enrichedBio = enrichedInfo?.lastFmBio
        val bioText = bioState?.biography
        when {
            !enrichedBio.isNullOrBlank() -> enrichedBio
            !bioText.isNullOrBlank() -> bioText
            else -> getFallbackBiography(artist.name, details.country, details.birthYearOrActive, lang == "fa")
        }
    }
    
    var sortBy by remember { mutableStateOf("year") } // "year", "title"

    LaunchedEffect(artist.id) {
        viewModel.loadArtistDetails(artist)
    }

    val sortedAlbums = remember(albums, sortBy) {
        when (sortBy) {
            "title" -> albums.sortedBy { it.name }
            else -> albums.sortedByDescending { it.year ?: 0 }
        }
    }

    val openSocial = { url: String ->
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "خطا در باز کردن لینک", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppleRed)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(310.dp)
                        ) {
                            // Blurred background banner of the artist cover art
                            AsyncImage(
                                model = viewModel.getCoverArtUrl(artist.coverArt),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(18.dp),
                                contentScale = ContentScale.Crop,
                                alpha = 0.32f
                            )
                            
                            // Modern vertical dark gradient scrim
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.background.copy(alpha = 0.4f),
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
                            )
                            
                            // Advanced Profile Column
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                // Red ring premium circular avatar
                                AsyncImage(
                                    model = viewModel.getCoverArtUrl(artist.coverArt),
                                    contentDescription = artist.name,
                                    modifier = Modifier
                                        .size(114.dp)
                                        .clip(CircleShape)
                                        .border(2.5.dp, AppleRed, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = artist.name,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = LocalAppFontFamily.current,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                
                                Spacer(modifier = Modifier.height(6.dp))

                                // Country / founded year — live data from MusicBrainz when available
                                val displayCountry = enrichedInfo?.country ?: details.country
                                val displayYear = enrichedInfo?.foundedYear?.let {
                                    if (lang == "fa") "تأسیس $it" else "Founded $it"
                                } ?: details.birthYearOrActive
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = displayCountry,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = LocalAppFontFamily.current,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "•",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                                    )
                                    Text(
                                        text = displayYear,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = LocalAppFontFamily.current,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                }

                                // Last.fm listeners count chip
                                enrichedInfo?.lastFmListeners?.let { listeners ->
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Headphones,
                                            contentDescription = null,
                                            tint = Color(0xFFD51007),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "${formatListenersCount(listeners)} ${if (lang == "fa") "شنونده در Last.fm" else "Last.fm listeners"}",
                                            fontSize = 11.sp,
                                            fontFamily = LocalAppFontFamily.current,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                                        )
                                    }
                                }

                                // MusicBrainz genre tags
                                val mbGenres = enrichedInfo?.genres ?: emptyList()
                                if (mbGenres.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    androidx.compose.foundation.layout.FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        mbGenres.forEach { genre ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = genre,
                                                    fontSize = 10.sp,
                                                    fontFamily = LocalAppFontFamily.current,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(14.dp))
                                
                                // Sleek minimalist follow & social media connection row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    var isFav by remember { mutableStateOf(false) }
                                    LaunchedEffect(artist.id) {
                                        isFav = viewModel.favoriteArtists.value.any { it.id == artist.id }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isFav) AppleRed.copy(alpha = 0.15f) else AppleRed)
                                            .border(1.dp, AppleRed, RoundedCornerShape(16.dp))
                                            .clickable { viewModel.toggleFavoriteArtist(artist) }
                                            .padding(horizontal = 14.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                contentDescription = null,
                                                tint = if (isFav) AppleRed else Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = if (isFav) translate("دنبال شده", "Following", viewModel) else translate("دنبال کردن", "Follow", viewModel),
                                                fontFamily = LocalAppFontFamily.current,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isFav) AppleRed else Color.White
                                            )
                                        }
                                    }
                                    
                                    // Social Icons
                                    details.instagram?.let { insta ->
                                        SocialIconCircle(brand = "instagram", color = Color(0xFFE1306C)) {
                                            openSocial("https://instagram.com/$insta")
                                        }
                                    }
                                    
                                    details.twitter?.let { tw ->
                                        SocialIconCircle(brand = "twitter", color = Color(0xFF1DA1F2)) {
                                            openSocial("https://twitter.com/$tw")
                                        }
                                    }
                                    
                                    details.youtube?.let { yt ->
                                        SocialIconCircle(brand = "youtube", color = Color(0xFFFF0000)) {
                                            openSocial("https://youtube.com/@$yt")
                                        }
                                    }
                                    
                                    details.spotify?.let { sp ->
                                        SocialIconCircle(brand = "spotify", color = Color(0xFF1DB954)) {
                                            openSocial("https://open.spotify.com/artist/$sp")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Biography section with enhanced elegant design
                    if (biographyToShow.isNotBlank()) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                            var isBioExpanded by remember { mutableStateOf(false) }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clickable { isBioExpanded = !isBioExpanded },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Info,
                                                contentDescription = null,
                                                tint = AppleRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = translate("بیوگرافی و داستان هنرمند", "Biography & Story", viewModel),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = LocalAppFontFamily.current,
                                                color = AppleRed
                                            )
                                        }
                                        Text(
                                            text = if (isBioExpanded) translate("بستن", "Collapse", viewModel) else translate("بیشتر", "Read More", viewModel),
                                            fontSize = 11.sp,
                                            fontFamily = LocalAppFontFamily.current,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = biographyToShow,
                                        fontSize = 13.sp,
                                        lineHeight = 22.sp,
                                        fontFamily = LocalAppFontFamily.current,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        maxLines = if (isBioExpanded) Int.MAX_VALUE else 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Top Tracks section
                    if (topTracks.isNotEmpty()) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text(
                                    text = translate("۵ آهنگ برتر", "Top 5 Songs", viewModel),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    fontFamily = LocalAppFontFamily.current,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    topTracks.take(5).forEach { song ->
                                        TrackRowItem(
                                            song = song,
                                            onPlay = { viewModel.playSongDirectly(song, topTracks) },
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Similar Artists section
                    if (similarArtists.isNotEmpty()) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                            Column {
                                Text(
                                    text = translate("هنرمندان مشابه", "Similar Artists", viewModel),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    fontFamily = LocalAppFontFamily.current,
                                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
                                )
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(similarArtists) { simArtist ->
                                        ArtistCircleCard(
                                            name = simArtist.name,
                                            imageUrl = viewModel.getCoverArtUrl(simArtist.coverArt),
                                            onClick = { onArtistClick(simArtist) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Albums Title Header + Sort options
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 12.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = translate("همه آلبوم‌ها", "All Albums", viewModel),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                fontFamily = LocalAppFontFamily.current
                            )
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val sortOptions = listOf(
                                    "year" to translate("سال", "Year", viewModel),
                                    "title" to translate("عنوان", "Title", viewModel)
                                )
                                sortOptions.forEach { opt ->
                                    val isSelected = sortBy == opt.first
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) AppleRed.copy(alpha = 0.12f) else Color.Transparent)
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) AppleRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { sortBy = opt.first }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = opt.second,
                                            fontSize = 11.sp,
                                            fontFamily = LocalAppFontFamily.current,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) AppleRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (sortedAlbums.isEmpty()) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = translate("هیچ آلبومی یافت نشد.", "No albums found.", viewModel),
                                    color = Color.Gray,
                                    fontFamily = LocalAppFontFamily.current
                                )
                            }
                        }
                    } else {
                        items(sortedAlbums) { album ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                AlbumGridCard(
                                    album = album,
                                    onClick = { onAlbumClick(album) },
                                    viewModel = viewModel
                                )
                            }
                        }
                    }

                    // All Songs section
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        Text(
                            text = translate("همه آهنگ‌ها", "All Songs", viewModel),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            fontFamily = LocalAppFontFamily.current,
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 12.dp, bottom = 8.dp)
                        )
                    }
                    if (isLoadingAllSongs) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = AppleRed, modifier = Modifier.size(24.dp))
                            }
                        }
                    } else if (allSongs.isEmpty()) {
                        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = translate("هیچ آهنگی یافت نشد.", "No songs found.", viewModel),
                                    color = Color.Gray,
                                    fontFamily = LocalAppFontFamily.current
                                )
                            }
                        }
                    } else {
                        items(allSongs, span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) { song ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
                                TrackRowItem(
                                    song = song,
                                    onPlay = { viewModel.playSongDirectly(song, allSongs) },
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }

                // Minimal Glassmorphic Circular Overlaid Back Button
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack, // Back button should be standard back arrow
                        contentDescription = "بازگشت",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// --- Album Detail Subscreen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailView(
    album: SubsonicAlbum,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onAlbumClick: (SubsonicAlbum) -> Unit = {}
) {
    val songs by viewModel.selectedAlbumSongs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingDetail.collectAsStateWithLifecycle()
    val relatedAlbums by viewModel.relatedAlbums.collectAsStateWithLifecycle()

    LaunchedEffect(album.id) {
        viewModel.loadAlbumDetails(album)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(album.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppleRed)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(bottom = 80.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        AsyncImage(
                            model = viewModel.getCoverArtUrl(album.coverArt),
                            contentDescription = album.name,
                            modifier = Modifier
                                .size(180.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = album.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        Text(text = album.artist ?: "هنرمند ناشناس", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                        
                        // Beautiful aesthetic badges row for Year, Genre, Track Count, and Duration
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            val yearVal = album.year
                            val genreVal = album.genre
                            val countVal = songs.size
                            val durVal = album.duration ?: 0
                            
                            val details = mutableListOf<String>()
                            if (yearVal != null && yearVal > 0) {
                                details.add(translate("📅 $yearVal", "$yearVal", viewModel))
                            }
                            if (!genreVal.isNullOrBlank()) {
                                details.add(translate("🎵 $genreVal", "$genreVal", viewModel))
                            }
                            if (countVal > 0) {
                                details.add(translate("💿 $countVal آهنگ", "$countVal Tracks", viewModel))
                            }
                            if (durVal > 0) {
                                val mins = durVal / 60
                                details.add(translate("⏱️ $mins دقیقه", "$mins mins", viewModel))
                            }
                            
                            details.forEach { detail ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = detail,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = LocalAppFontFamily.current,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.playAllSongsInQueue(songs) },
                                colors = ButtonDefaults.buttonColors(containerColor = AppleRed)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                Text("پخش همه")
                            }

                            IconButton(onClick = { viewModel.shufflePlayAllSongsInQueue(songs) }) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = translate("پخش تصادفی", "Shuffle play", viewModel),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.toggleFavoriteAlbum(album) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AppleRed)
                            ) {
                                var isFav by remember { mutableStateOf(false) }
                                LaunchedEffect(album.id) {
                                    isFav = viewModel.favoriteAlbums.value.any { it.id == album.id }
                                }
                                Text(if (isFav) "پسندیده شده" else "پسندیدن")
                            }

                            IconButton(onClick = { viewModel.addToQueue(songs) }) {
                                Icon(
                                    imageVector = Icons.Default.QueueMusic,
                                    contentDescription = translate("افزودن به صف پخش", "Add to queue", viewModel),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(onClick = { viewModel.startDownloadAll(songs) }) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = translate("دانلود کل آلبوم", "Download whole album", viewModel),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                if (songs.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("آهنگی در این آلبوم یافت نشد.", color = Color.Gray)
                        }
                    }
                } else {
                    items(songs) { song ->
                        TrackRowItem(
                            song = song,
                            onPlay = { viewModel.playSongDirectly(song, songs) },
                            viewModel = viewModel
                        )
                        HorizontalDivider()
                    }
                }

                if (relatedAlbums.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(
                                text = translate("آلبوم‌های مرتبط", "Related Albums", viewModel),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = LocalAppFontFamily.current,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                items(relatedAlbums) { relatedAlbum ->
                                    VerticalBigCard(
                                        title = relatedAlbum.name,
                                        subtitle = relatedAlbum.artist ?: translate("هنرمند ناشناس", "Unknown Artist", viewModel),
                                        imageUrl = viewModel.getCoverArtUrl(relatedAlbum.coverArt),
                                        onClick = { onAlbumClick(relatedAlbum) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Playlist Detail Subscreen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailView(
    playlist: LocalPlaylist,
    viewModel: MusicViewModel,
    onBack: () -> Unit
) {
    val items by viewModel.activePlaylistItems.collectAsStateWithLifecycle()

    LaunchedEffect(playlist.id) {
        viewModel.loadPlaylistItems(playlist.id)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(bottom = 80.dp)) {
            if (items.isEmpty()) {
                EmptyStateView(message = "این لیست پخش خالی است.")
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Button(
                            onClick = {
                                val songsList = items.map { SubsonicSong(it.trackId, it.title, it.album, it.artist, coverArt = it.coverArtUrl, duration = it.duration) }
                                viewModel.playAllSongsInQueue(songsList)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppleRed),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("پخش تمام آهنگ‌ها")
                        }
                    }

                    items(items) { item ->
                        val song = SubsonicSong(
                            id = item.trackId,
                            title = item.title,
                            artist = item.artist,
                            album = item.album,
                            coverArt = item.coverArtUrl,
                            duration = item.duration
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                TrackRowItem(
                                    song = song,
                                    onPlay = { viewModel.playSongDirectly(song, items.map { SubsonicSong(it.trackId, it.title, it.album, it.artist, coverArt = it.coverArtUrl, duration = it.duration) }) },
                                    viewModel = viewModel
                                )
                            }
                            IconButton(onClick = { viewModel.removeTrackFromPlaylist(playlist.id, item.trackId) }) {
                                Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = "حذف از پلی‌لیست", tint = Color.Gray)
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

// --- Profile Screen ---
@Composable
fun ProfileScreen(viewModel: MusicViewModel, onSettingsClick: () -> Unit = {}) {
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val profilePhotoUri by viewModel.profilePhotoUri.collectAsStateWithLifecycle()
    val favoriteArtists by viewModel.favoriteArtists.collectAsStateWithLifecycle()
    val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val favoriteAlbums by viewModel.favoriteAlbums.collectAsStateWithLifecycle()
    val localPlaylists by viewModel.localPlaylists.collectAsStateWithLifecycle()
    val estimatedMinutes by viewModel.estimatedListeningMinutes.collectAsStateWithLifecycle()
    val recentPlays by viewModel.recentPlays.collectAsStateWithLifecycle()
    val randomSongs by viewModel.homeRandomSongs.collectAsStateWithLifecycle()
    val newestAlbums by viewModel.homeNewestAlbums.collectAsStateWithLifecycle()

    var showEditNameDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) viewModel.setProfilePhotoUri(uri.toString()) }

    val favoritedCount = favoriteArtists.size + favoriteTracks.size + favoriteAlbums.size
    val listeningHours = estimatedMinutes / 60
    val listeningMinutesRemainder = estimatedMinutes % 60

    val topGenres = remember(favoriteTracks, favoriteAlbums, randomSongs, newestAlbums) {
        val favArtistNames = (favoriteTracks.map { it.artist } + favoriteAlbums.map { it.artist })
            .filter { it.isNotBlank() }.toSet()
        val genreCounts = mutableMapOf<String, Int>()
        randomSongs.forEach { song ->
            if (!song.genre.isNullOrBlank() && song.artist != null && favArtistNames.any { it.equals(song.artist, ignoreCase = true) }) {
                genreCounts[song.genre] = (genreCounts[song.genre] ?: 0) + 1
            }
        }
        newestAlbums.forEach { album ->
            if (!album.genre.isNullOrBlank() && album.artist != null && favArtistNames.any { it.equals(album.artist, ignoreCase = true) }) {
                genreCounts[album.genre] = (genreCounts[album.genre] ?: 0) + 1
            }
        }
        genreCounts.toList().sortedByDescending { it.second }.take(5).map { it.first }
    }

    val weeklyPlayCounts = remember(recentPlays) {
        val counts = IntArray(7)
        val calendar = Calendar.getInstance()
        recentPlays.forEach { play ->
            calendar.timeInMillis = play.timestamp
            val dayIndex = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0=Saturday ... 6=Friday
            counts[dayIndex] = counts[dayIndex] + 1
        }
        counts.toList()
    }

    val topPlayedSongs = remember(recentPlays) {
        recentPlays.groupBy { it.trackId }
            .mapValues { it.value.first() to it.value.size }
            .toList()
            .sortedByDescending { it.second.second }
            .take(5)
            .map { it.second.first }
    }

    val topArtists = remember(recentPlays) {
        recentPlays.filter { !it.artist.isNullOrBlank() }
            .groupBy { it.artist!! }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Row: Title + Settings shortcut
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = translate("پروفایل", "Profile", viewModel),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LocalAppFontFamily.current
            )
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = translate("تنظیمات", "Settings", viewModel),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Avatar + Name Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AppleRed.copy(alpha = 0.15f))
                        .border(2.dp, AppleRed, CircleShape)
                        .clickable {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (profilePhotoUri.isNotBlank()) {
                        AsyncImage(
                            model = profilePhotoUri,
                            contentDescription = translate("عکس پروفایل", "Profile Photo", viewModel),
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = (userName.firstOrNull() ?: 'S').uppercaseChar().toString(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleRed,
                            fontFamily = LocalAppFontFamily.current
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName.ifBlank { translate("کاربر مهمان", "Guest User", viewModel) },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalAppFontFamily.current,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { showEditNameDialog = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = translate("ویرایش نام", "Edit Name", viewModel),
                        tint = AppleRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProfileStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Favorite,
                value = favoritedCount.toString(),
                label = translate("علاقه‌مندی", "Favorites", viewModel)
            )
            ProfileStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.QueueMusic,
                value = localPlaylists.size.toString(),
                label = translate("پلی‌لیست", "Playlists", viewModel)
            )
            ProfileStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Headphones,
                value = if (listeningHours > 0) "${listeningHours}h ${listeningMinutesRemainder}m" else "${listeningMinutesRemainder}m",
                label = translate("زمان گوش دادن", "Listening", viewModel)
            )
        }

        // Top Genres (derived from genre tags of favorited artists' tracks/albums)
        if (topGenres.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = translate("ژانرهای محبوب شما", "Your Top Genres", viewModel),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalAppFontFamily.current,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        topGenres.forEach { genre ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AppleRed.copy(alpha = 0.12f))
                                    .border(1.dp, AppleRed.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(text = genre, fontSize = 12.sp, color = AppleRed, fontFamily = LocalAppFontFamily.current)
                            }
                        }
                    }
                }
            }
        }

        // Weekly Listening Chart
        if (weeklyPlayCounts.any { it > 0 }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = translate("گوش دادن هفتگی", "Weekly Listening", viewModel),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalAppFontFamily.current,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val dayLabels = listOf(
                        translate("ش", "Sat", viewModel),
                        translate("ی", "Sun", viewModel),
                        translate("د", "Mon", viewModel),
                        translate("س", "Tue", viewModel),
                        translate("چ", "Wed", viewModel),
                        translate("پ", "Thu", viewModel),
                        translate("ج", "Fri", viewModel)
                    )
                    val maxCount = (weeklyPlayCounts.maxOrNull() ?: 1).coerceAtLeast(1)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        weeklyPlayCounts.forEachIndexed { index, count ->
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height((60.dp * count / maxCount).coerceAtLeast(4.dp))
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (count > 0) AppleRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = dayLabels[index],
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontFamily = LocalAppFontFamily.current
                                )
                            }
                        }
                    }
                }
            }
        }

        // Top Played Songs
        if (topPlayedSongs.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = translate("پربازی‌ترین آهنگ‌ها", "Most Played", viewModel),
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        fontFamily = LocalAppFontFamily.current,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    topPlayedSongs.forEachIndexed { idx, play ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "${idx + 1}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (idx == 0) AppleRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.width(18.dp),
                                textAlign = TextAlign.Center
                            )
                            AsyncImage(
                                model = play.coverArtUrl,
                                contentDescription = play.title,
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = play.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = LocalAppFontFamily.current)
                                Text(text = play.artist ?: "—", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = LocalAppFontFamily.current)
                            }
                        }
                    }
                }
            }
        }

        // Top Artists
        if (topArtists.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = translate("هنرمندان پربازی", "Top Artists", viewModel),
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        fontFamily = LocalAppFontFamily.current,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        topArtists.forEachIndexed { idx, artist ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(AppleRed.copy(alpha = 0.6f + idx * 0.13f), Color(0xFF8E24AA).copy(alpha = 0.5f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (artist.firstOrNull() ?: "?").toString().uppercase(),
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = artist,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    fontFamily = LocalAppFontFamily.current,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Logout
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Red.copy(alpha = 0.1f))
                .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .clickable { viewModel.logout() }
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = translate("خروج از حساب", "Log Out", viewModel),
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                fontFamily = LocalAppFontFamily.current
            )
        }
    }

    if (showEditNameDialog) {
        var editedName by remember { mutableStateOf(userName) }
        Dialog(onDismissRequest = { showEditNameDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = translate("ویرایش نام", "Edit Name", viewModel),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = LocalAppFontFamily.current
                    )
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text(translate("نام", "Name", viewModel)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showEditNameDialog = false }) {
                            Text(translate("انصراف", "Cancel", viewModel))
                        }
                        TextButton(
                            onClick = {
                                viewModel.setUserName(editedName.trim())
                                showEditNameDialog = false
                            },
                            enabled = editedName.isNotBlank()
                        ) {
                            Text(translate("ذخیره", "Save", viewModel), color = AppleRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStatCard(modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AppleRed, modifier = Modifier.size(20.dp))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun AddServerDialog(
    isAuthenticating: Boolean,
    loginError: String?,
    viewModel: MusicViewModel,
    onDismiss: () -> Unit,
    onConfirm: (url: String, username: String, pass: String, isToken: Boolean) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = translate("افزودن سرور جدید", "Add New Server", viewModel),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = LocalAppFontFamily.current
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(translate("آدرس سرور", "Server URL", viewModel)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(translate("نام کاربری", "Username", viewModel)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(translate("رمز عبور", "Password", viewModel)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (loginError != null) {
                    Text(text = loginError, color = Color.Red, fontSize = 12.sp, fontFamily = LocalAppFontFamily.current)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAuthenticating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AppleRed, strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text(translate("انصراف", "Cancel", viewModel))
                        }
                        TextButton(
                            onClick = { onConfirm(url.trim(), username.trim(), password, false) },
                            enabled = url.isNotBlank() && username.isNotBlank() && password.isNotBlank()
                        ) {
                            Text(translate("اتصال", "Connect", viewModel), color = AppleRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- Settings Screen ---
@Composable
fun SettingsScreen(viewModel: MusicViewModel, onBack: () -> Unit = {}) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val themeAccent by viewModel.themeAccent.collectAsStateWithLifecycle()
    val audioQuality by viewModel.audioQuality.collectAsStateWithLifecycle()
    val downloadQuality by viewModel.downloadQuality.collectAsStateWithLifecycle()
    val cacheSize by viewModel.cacheSizeMb.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val bassLevel by viewModel.bassLevel.collectAsStateWithLifecycle()
    val trebleLevel by viewModel.trebleLevel.collectAsStateWithLifecycle()
    val crossfadeEnabled by viewModel.crossfadeEnabled.collectAsStateWithLifecycle()
    val wifiOnlyDownload by viewModel.wifiOnlyDownload.collectAsStateWithLifecycle()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsStateWithLifecycle()
    val transientMessage by viewModel.transientMessage.collectAsStateWithLifecycle()
    val isLastFmLoggedIn by viewModel.isLastFmLoggedIn.collectAsStateWithLifecycle()
    val lastFmUsername by viewModel.lastFmUsername.collectAsStateWithLifecycle()
    val isLastFmLoggingIn by viewModel.isLastFmLoggingIn.collectAsStateWithLifecycle()
    val lastFmLoginError by viewModel.lastFmLoginError.collectAsStateWithLifecycle()
    val scrobbleEnabled by viewModel.scrobbleEnabled.collectAsStateWithLifecycle()

    var lastFmUsernameInput by remember { mutableStateOf("") }
    var lastFmPasswordInput by remember { mutableStateOf("") }
    var showLastFmPassword by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(transientMessage) {
        transientMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeTransientMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = translate("بازگشت", "Back", viewModel),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = translate("تنظیمات", "Settings", viewModel),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = LocalAppFontFamily.current
            )
        }

        // 1. Theme and Language Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Language
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = AppleRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = translate("زبان برنامه", "App Language", viewModel),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = LocalAppFontFamily.current,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val languages = listOf("fa" to "فارسی", "en" to "English")
                    languages.forEach { langOption ->
                        val isSelected = language == langOption.first
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AppleRed.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AppleRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.setLanguage(langOption.first) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = langOption.second,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontFamily = LocalAppFontFamily.current,
                                color = if (isSelected) AppleRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                // App Theme (Sleek minimalist switcher)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = AppleRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = translate("ظاهر برنامه (پوسته)", "App Theme", viewModel),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = LocalAppFontFamily.current,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val themes = listOf(
                        "light" to translate("روشن", "Light", viewModel), 
                        "dark" to translate("تاریک", "Dark", viewModel), 
                        "amoled" to translate("آمولد", "Amoled", viewModel), 
                        "glass" to translate("شیشه‌ای", "Glass", viewModel)
                    )
                    themes.forEach { t ->
                        val isSelected = themeMode == t.first
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AppleRed.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AppleRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.setThemeMode(t.first) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = t.second,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = LocalAppFontFamily.current,
                                color = if (isSelected) AppleRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                // Accent Color
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ColorLens,
                        contentDescription = null,
                        tint = AppleRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = translate("رنگ اصلی برنامه", "Accent Color", viewModel),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = LocalAppFontFamily.current,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val accents = listOf(
                        "red" to Color(0xFFFA2D48),
                        "blue" to Color(0xFF007AFF),
                        "green" to Color(0xFF34C759),
                        "orange" to Color(0xFFFF9500),
                        "purple" to Color(0xFFAF52DE),
                        "pink" to Color(0xFFFF2D55)
                    )
                    accents.forEach { (key, color) ->
                        val isSelected = themeAccent == key
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setThemeAccent(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

            }
        }

        // 2. Audio & Storage Manager Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Streaming Quality
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = null,
                        tint = AppleRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = translate("کیفیت پخش موسیقی", "Audio Streaming Quality", viewModel),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = LocalAppFontFamily.current,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val qualities = listOf(
                        "low" to translate("پایین", "Low", viewModel),
                        "medium" to translate("متوسط", "Medium", viewModel),
                        "high" to translate("عالی", "High", viewModel),
                        "lossless" to translate("شفاف", "FLAC", viewModel)
                    )
                    qualities.forEach { q ->
                        val isSelected = audioQuality == q.first
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AppleRed.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AppleRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.setAudioQuality(q.first) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = q.second,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = LocalAppFontFamily.current,
                                color = if (isSelected) AppleRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                // Download Quality
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = AppleRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = translate("کیفیت دانلود", "Download Quality", viewModel),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = LocalAppFontFamily.current,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val downloadQualities = listOf(
                        "low" to translate("پایین", "Low", viewModel),
                        "medium" to translate("متوسط", "Medium", viewModel),
                        "high" to translate("عالی", "High", viewModel),
                        "lossless" to translate("شفاف", "FLAC", viewModel)
                    )
                    downloadQualities.forEach { q ->
                        val isSelected = downloadQuality == q.first
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AppleRed.copy(alpha = 0.15f) else Color.Transparent)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AppleRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.setDownloadQuality(q.first) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = q.second,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = LocalAppFontFamily.current,
                                color = if (isSelected) AppleRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                // Cache size manager
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SdStorage,
                        contentDescription = null,
                        tint = AppleRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = translate("مدیریت حافظه کش", "Cache Storage Manager", viewModel),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = LocalAppFontFamily.current,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = translate("ذخیره موقت هوشمند با تخلیه خودکار برای افزایش سرعت پخش موسیقی.", "Smart local caching with auto-clear maximizes app responsiveness.", viewModel), 
                        fontSize = 12.sp, 
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontFamily = LocalAppFontFamily.current
                    )

                    OutlinedButton(
                        onClick = {
                            viewModel.clearCache()
                            val msg = if (language == "fa") "حافظه موقت پاکسازی شد." else "Temporary cache cleared."
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppleRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, AppleRed.copy(alpha = 0.4f))
                    ) {
                        Text(translate("پاک کردن کش موقت", "Clear Temporary Cache", viewModel), fontSize = 12.sp, fontFamily = LocalAppFontFamily.current)
                    }
                }
            }
        }

        // 2.5 Equalizer & Playback Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = AppleRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = translate("اکولایزر و پخش", "Equalizer & Playback", viewModel),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = LocalAppFontFamily.current,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = translate("بیس", "Bass", viewModel) + ": $bassLevel",
                    fontSize = 12.sp,
                    fontFamily = LocalAppFontFamily.current,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Slider(
                    value = bassLevel.toFloat(),
                    onValueChange = { viewModel.setBassLevel(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = AppleRed, activeTrackColor = AppleRed)
                )

                Text(
                    text = translate("تربل", "Treble", viewModel) + ": $trebleLevel",
                    fontSize = 12.sp,
                    fontFamily = LocalAppFontFamily.current,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Slider(
                    value = trebleLevel.toFloat(),
                    onValueChange = { viewModel.setTrebleLevel(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = AppleRed, activeTrackColor = AppleRed)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = translate("افکت محو شدن بین آهنگ‌ها (کراس‌فید)", "Crossfade between tracks", viewModel),
                        fontSize = 13.sp,
                        fontFamily = LocalAppFontFamily.current,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = crossfadeEnabled,
                        onCheckedChange = { viewModel.setCrossfadeEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AppleRed, checkedTrackColor = AppleRed.copy(alpha = 0.5f))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = translate("دانلود فقط با وای‌فای", "Download on Wi-Fi only", viewModel),
                        fontSize = 13.sp,
                        fontFamily = LocalAppFontFamily.current,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = wifiOnlyDownload,
                        onCheckedChange = { viewModel.setWifiOnlyDownload(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AppleRed, checkedTrackColor = AppleRed.copy(alpha = 0.5f))
                    )
                }
            }
        }

        // 2.6 Backup & Restore Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = AppleRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = translate("پشتیبان‌گیری پلی‌لیست‌ها", "Playlist Backup", viewModel),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = LocalAppFontFamily.current,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.exportPlaylistsBackup() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppleRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, AppleRed.copy(alpha = 0.4f))
                    ) {
                        Text(translate("ذخیره پشتیبان", "Backup", viewModel), fontSize = 12.sp, fontFamily = LocalAppFontFamily.current)
                    }
                    OutlinedButton(
                        onClick = { viewModel.restorePlaylistsBackup() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppleRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, AppleRed.copy(alpha = 0.4f))
                    ) {
                        Text(translate("بازیابی", "Restore", viewModel), fontSize = 12.sp, fontFamily = LocalAppFontFamily.current)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = translate("پشتیبان‌گیری خودکار", "Auto Backup", viewModel),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = LocalAppFontFamily.current,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = translate("هر ۶ ساعت یک‌بار پلی‌لیست‌ها به صورت خودکار ذخیره می‌شوند.", "Automatically saves a playlist backup every 6 hours.", viewModel),
                            fontSize = 11.sp,
                            fontFamily = LocalAppFontFamily.current,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = autoBackupEnabled,
                        onCheckedChange = { viewModel.setAutoBackupEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AppleRed, checkedTrackColor = AppleRed.copy(alpha = 0.5f))
                    )
                }
            }
        }

        // Last.fm Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFFD51007),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Last.fm",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = LocalAppFontFamily.current,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isLastFmLoggedIn) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = lastFmUsername,
                            fontSize = 12.sp,
                            fontFamily = LocalAppFontFamily.current,
                            color = Color(0xFFD51007),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isLastFmLoggedIn) {
                    // Scrobble toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = translate("اسکروبل آهنگ‌ها", "Scrobble Tracks", viewModel),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = LocalAppFontFamily.current,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = translate("پس از ۳۰ ثانیه، آهنگ جاری به Last.fm ارسال می‌شود.", "After 30s, the current track is sent to Last.fm.", viewModel),
                                fontSize = 11.sp,
                                fontFamily = LocalAppFontFamily.current,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        Switch(
                            checked = scrobbleEnabled,
                            onCheckedChange = { viewModel.setScrobbleEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFD51007), checkedTrackColor = Color(0xFFD51007).copy(alpha = 0.5f))
                        )
                    }
                    // Logout
                    Button(
                        onClick = { viewModel.logoutLastFm() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD51007).copy(alpha = 0.1f), contentColor = Color(0xFFD51007)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFD51007).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = translate("خروج از Last.fm", "Disconnect Last.fm", viewModel),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = LocalAppFontFamily.current
                        )
                    }
                } else {
                    // Login form
                    OutlinedTextField(
                        value = lastFmUsernameInput,
                        onValueChange = { lastFmUsernameInput = it },
                        label = { Text(translate("نام کاربری Last.fm", "Last.fm Username", viewModel), fontFamily = LocalAppFontFamily.current, fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD51007),
                            focusedLabelColor = Color(0xFFD51007)
                        )
                    )
                    OutlinedTextField(
                        value = lastFmPasswordInput,
                        onValueChange = { lastFmPasswordInput = it },
                        label = { Text(translate("رمز عبور", "Password", viewModel), fontFamily = LocalAppFontFamily.current, fontSize = 12.sp) },
                        singleLine = true,
                        visualTransformation = if (showLastFmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showLastFmPassword = !showLastFmPassword }) {
                                Icon(
                                    imageVector = if (showLastFmPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFD51007),
                            focusedLabelColor = Color(0xFFD51007)
                        )
                    )
                    lastFmLoginError?.let { err ->
                        Text(text = err, color = Color(0xFFD51007), fontSize = 12.sp, fontFamily = LocalAppFontFamily.current)
                    }
                    Button(
                        onClick = {
                            if (lastFmUsernameInput.isNotBlank() && lastFmPasswordInput.isNotBlank()) {
                                viewModel.loginLastFm(lastFmUsernameInput, lastFmPasswordInput)
                            }
                        },
                        enabled = !isLastFmLoggingIn,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD51007)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLastFmLoggingIn) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text(
                                text = translate("ورود به Last.fm", "Connect Last.fm", viewModel),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = LocalAppFontFamily.current,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // 3. User Account & Log Out Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = AppleRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = translate("حساب کاربری", "User Account", viewModel),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFamily = LocalAppFontFamily.current,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = { viewModel.logout() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleRed.copy(alpha = 0.1f), contentColor = AppleRed),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AppleRed.copy(alpha = 0.4f))
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = AppleRed
                        )
                        Text(
                            text = translate("خروج از حساب کاربری", "Log Out of Account", viewModel),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = LocalAppFontFamily.current
                        )
                    }
                }
            }
        }

    }
}

// --- Stylized waveform-style seek bar (deterministic per-song pattern, tap/drag to seek) ---
@Composable
fun WaveformSeekBar(
    songId: String,
    progressFraction: Float,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val barCount = 56
    val barHeights = remember(songId) {
        val random = kotlin.random.Random(songId.hashCode())
        List(barCount) { 0.25f + random.nextFloat() * 0.75f }
    }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val activeFraction = dragFraction ?: progressFraction.coerceIn(0f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(fraction)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onHorizontalDrag = { change, _ ->
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        dragFraction = fraction
                    },
                    onDragEnd = {
                        dragFraction?.let { onSeek(it) }
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null }
                )
            }
    ) {
        val barWidth = size.width / (barCount * 1.6f)
        val spacing = size.width / barCount
        val activeBarIndex = (activeFraction * barCount).toInt()
        for (i in 0 until barCount) {
            val barHeightFraction = barHeights[i]
            val barHeightPx = size.height * barHeightFraction
            val x = i * spacing
            val color = if (i <= activeBarIndex) Color.White else Color.White.copy(alpha = 0.28f)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, (size.height - barHeightPx) / 2f),
                size = Size(barWidth, barHeightPx),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
private fun CastButton(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f)),
        factory = { context ->
            try {
                MediaRouteButton(context).apply {
                    CastButtonFactory.setUpMediaRouteButton(context, this)
                }
            } catch (e: Exception) {
                android.view.View(context)
            }
        }
    )
}

// --- Gorgeous Sliding Fullscreen Player Sheet ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenPlayer(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit,
    onArtistClick: (SubsonicArtist) -> Unit
) {
    val currentTrack by viewModel.playbackConnection.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playbackConnection.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.playbackConnection.playbackProgress.collectAsStateWithLifecycle()
    val duration by viewModel.playbackConnection.trackDuration.collectAsStateWithLifecycle()
    val isShuffle by viewModel.playbackConnection.isShuffleEnabled.collectAsStateWithLifecycle()
    val repeatMode by viewModel.playbackConnection.repeatMode.collectAsStateWithLifecycle()
    val speed by viewModel.playbackConnection.playbackSpeed.collectAsStateWithLifecycle()
    val lyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsStateWithLifecycle()
    val sleepTimerLeft by viewModel.playbackConnection.sleepTimerMinutesLeft.collectAsStateWithLifecycle()
    val downloadedTracks by viewModel.downloadedTracks.collectAsStateWithLifecycle()
    val downloadProgresses by viewModel.downloadProgresses.collectAsStateWithLifecycle()
    val queue by viewModel.playbackConnection.playbackQueue.collectAsStateWithLifecycle()

    var showTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current

    // Toggle Tab: 0: AlbumArt Cover, 1: Lyrics list
    var playerTab by remember { mutableStateOf(0) }

    if (currentTrack == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val song = currentTrack!!

    // Palette-based dominant color that animates on track change (like Spotify/Apple Music)
    val context = LocalContext.current
    var dominantColor by remember(song.id) { mutableStateOf(Color(0xFF121212)) }
    val dominantColorAnimated by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 900),
        label = "DominantColor"
    )
    LaunchedEffect(song.id) {
        try {
            val req = ImageRequest.Builder(context)
                .data(viewModel.getCoverArtUrl(song.coverArt))
                .allowHardware(false)
                .build()
            val result = context.imageLoader.execute(req)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                bitmap?.let { bmp ->
                    val palette = Palette.from(bmp).generate()
                    val swatch = palette.dominantSwatch ?: palette.vibrantSwatch
                    swatch?.let { dominantColor = Color(it.rgb) }
                }
            }
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Dynamic Blur cover artwork background
        AsyncImage(
            model = viewModel.getCoverArtUrl(song.coverArt),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(80.dp)
                .graphicsLayer(alpha = 0.45f),
            contentScale = ContentScale.Crop
        )

        // Dynamic dominant-color gradient overlay (Spotify/Apple Music style)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to dominantColorAnimated.copy(alpha = 0.72f),
                        0.55f to dominantColorAnimated.copy(alpha = 0.25f),
                        1f to Color.Black.copy(alpha = 0.90f)
                    )
                )
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(0f, 0f),
                            radius = size.width * 1.1f
                        )
                    )
                }
        )

        // Actual content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "بستن", tint = Color.White, modifier = Modifier.size(32.dp))
                }

                // Apple Music inspired tab selector for artwork / lyrics
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(4.dp)
                ) {
                    Text(
                        text = "کاور",
                        color = if (playerTab == 0) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (playerTab == 0) Color.White else Color.Transparent)
                            .clickable { playerTab = 0 }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    Text(
                        text = "متن شعر",
                        color = if (playerTab == 1) Color.Black else Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (playerTab == 1) Color.White else Color.Transparent)
                            .clickable { playerTab = 1 }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                Row {
                    val isDownloaded = downloadedTracks.any { it.id == song.id }
                    val downloadProgress = downloadProgresses[song.id]
                    if (downloadProgress != null) {
                        CircularProgressIndicator(
                            progress = { downloadProgress },
                            color = AppleRed,
                            modifier = Modifier.padding(10.dp).size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (isDownloaded) {
                                    viewModel.deleteDownload(song.id)
                                } else {
                                    viewModel.startDownload(song)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isDownloaded) Icons.Default.OfflinePin else Icons.Outlined.FileDownload,
                                contentDescription = "دانلود",
                                tint = if (isDownloaded) AppleRed else Color.White
                            )
                        }
                    }

                    CastButton()

                    IconButton(onClick = { showTimerDialog = true }) {
                        Icon(
                            imageVector = if (sleepTimerLeft != null) Icons.Filled.Timer else Icons.Outlined.Timer,
                            contentDescription = "تایمر خواب",
                            tint = if (sleepTimerLeft != null) AppleRed else Color.White
                        )
                    }
                }
            }

            // Central Area (either big artwork or lyrics list)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .pointerInput(song.id) {
                        var totalDrag = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { totalDrag = 0f },
                            onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount },
                            onDragEnd = {
                                if (totalDrag < -120f) {
                                    viewModel.playbackConnection.skipToNext()
                                } else if (totalDrag > 120f) {
                                    viewModel.playbackConnection.skipToPrevious()
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (playerTab == 0) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Huge Cover Artwork with sleek animations
                        val scale by animateFloatAsState(
                            targetValue = if (isPlaying) 1.0f else 0.88f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "ArtworkScale"
                        )

                        // Slow turntable-style rotation while the track is playing,
                        // pausing (and resuming from the same angle) when paused.
                        val rotationDegrees = remember { mutableFloatStateOf(0f) }
                        LaunchedEffect(isPlaying) {
                            if (isPlaying) {
                                var lastFrameTime = withFrameNanos { it }
                                while (true) {
                                    val frameTime = withFrameNanos { it }
                                    val deltaSeconds = (frameTime - lastFrameTime) / 1_000_000_000f
                                    lastFrameTime = frameTime
                                    rotationDegrees.floatValue = (rotationDegrees.floatValue + deltaSeconds * 18f) % 360f
                                }
                            }
                        }

                        AsyncImage(
                            model = viewModel.getCoverArtUrl(song.coverArt),
                            contentDescription = song.title,
                            modifier = Modifier
                                .size(280.dp)
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    rotationZ = rotationDegrees.floatValue
                                )
                                .clip(CircleShape)
                                .shadow(16.dp, CircleShape)
                                .pointerInput(song.id) {
                                    detectTapGestures(
                                        onLongPress = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showAddToPlaylist = true
                                        }
                                    )
                                },
                            contentScale = ContentScale.Crop
                        )

                        // Real-Time Synchronized Lyrics Overlay Pill
                        if (isLoadingLyrics) {
                            Text(
                                text = translate("درحال بارگذاری متن...", "Loading lyrics...", viewModel),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                fontFamily = LocalAppFontFamily.current
                            )
                        } else if (lyrics != null) {
                            val parsedLyrics = remember(lyrics) { parseLrc(lyrics!!) }
                            val activeLine = remember(parsedLyrics, progress) {
                                parsedLyrics.findLast { progress >= it.timestampMs }?.text
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                    .clickable { playerTab = 1 }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "متن شعر",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    
                                    val lyricDisplay = activeLine ?: if (parsedLyrics.isNotEmpty()) {
                                        translate("شروع موسیقی...", "Music starts...", viewModel)
                                    } else {
                                        // Unsynced raw lyrics, show first line or instruction
                                        lyrics!!.lineSequence().firstOrNull { it.isNotBlank() } ?: translate("مشاهده کامل شعر موسیقی", "Tap to view lyrics", viewModel)
                                    }

                                    Text(
                                        text = lyricDisplay,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontFamily = LocalAppFontFamily.current,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                            }
                        } else {
                            // Offer button to reload lyrics or indicate unavailable
                            TextButton(
                                onClick = { playerTab = 1 },
                                modifier = Modifier.graphicsLayer(alpha = 0.6f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Text(
                                        text = translate("فاقد متن آنلاین (لمس برای ورود دستی/جستجو)", "No lyrics available (Tap to view details)", viewModel),
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        fontFamily = LocalAppFontFamily.current
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Scrolling Lyrics Board
                    if (isLoadingLyrics) {
                        CircularProgressIndicator(color = AppleRed)
                    } else if (lyrics == null) {
                        Text("متن موسیقی یافت نشد.", color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp)
                    } else {
                        val parsedLyrics = remember(lyrics) { parseLrc(lyrics!!) }
                        if (parsedLyrics.isEmpty()) {
                            // Unsynced raw lyrics
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = lyrics!!,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 32.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            // Synced Lyrics autoscrolling board
                            SyncedLyricsView(lyricsLines = parsedLyrics, currentProgressMs = progress)
                        }
                    }
                }
            }

            // Track metadata details
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val artistsListState by viewModel.artistsList.collectAsStateWithLifecycle()
                        val participatingArtists = remember(song, artistsListState) {
                            getSongArtists(song, artistsListState)
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = AppleRed,
                                modifier = Modifier.size(14.dp)
                            )

                            participatingArtists.forEachIndexed { index, artist ->
                                Text(
                                    text = artist.name + (if (index < participatingArtists.size - 1) "، " else ""),
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = LocalAppFontFamily.current,
                                    modifier = Modifier.clickable { onArtistClick(artist) }
                                )
                            }
                        }

                        if (!song.album.isNullOrBlank()) {
                            Row(
                                modifier = Modifier.padding(top = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Album,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.45f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = song.album,
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontFamily = LocalAppFontFamily.current
                                )
                            }
                        }
                    }

                    Row {
                        val favoriteTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
                        val isFav = favoriteTracks.any { it.id == song.id }
                        IconButton(onClick = { viewModel.toggleFavoriteTrack(song) }) {
                            Icon(
                                imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "پسندیدن",
                                tint = if (isFav) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress slider, time labels and transport controls are kept in a fixed
                // left-to-right direction regardless of app language, matching the convention
                // used by Spotify/Apple Music/YouTube Music: timelines and skip-track icons
                // (which point a fixed visual direction) are never mirrored for RTL locales.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Stylized waveform seek bar (visual pattern, not real audio analysis)
                        val sliderPosition = if (duration > 0) progress.toFloat() / duration else 0f
                        WaveformSeekBar(
                            songId = song.id,
                            progressFraction = sliderPosition,
                            onSeek = { fraction ->
                                viewModel.playbackConnection.seekTo((fraction * duration).toLong())
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = formatTime(progress), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                            Text(text = formatTime(duration), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Main playback buttons row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.playbackConnection.toggleShuffle() }) {
                                Icon(
                                    imageVector = Icons.Default.Shuffle,
                                    contentDescription = "تصادفی",
                                    tint = if (isShuffle) AppleRed else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            IconButton(onClick = { viewModel.playbackConnection.skipToPrevious() }) {
                                Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "قبلی", tint = Color.White, modifier = Modifier.size(36.dp))
                            }

                            // Giant centered play/pause trigger with AppleRed fill
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .shadow(8.dp, CircleShape)
                                    .background(AppleRed, CircleShape)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.playbackConnection.togglePlayPause()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "پخش/توقف",
                                    tint = Color.White,
                                    modifier = Modifier.size(42.dp)
                                )
                            }

                            IconButton(onClick = { viewModel.playbackConnection.skipToNext() }) {
                                Icon(imageVector = Icons.Default.SkipNext, contentDescription = "بعدی", tint = Color.White, modifier = Modifier.size(36.dp))
                            }

                            IconButton(onClick = { viewModel.playbackConnection.toggleRepeatMode() }) {
                                val icon = when (repeatMode) {
                                    Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                    else -> Icons.Default.Repeat
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = "تکرار",
                                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) AppleRed else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lower accessories toolbar (Speed dial, Queue, equalizer)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = if (speed != 1f) 0.2f else 0.08f))
                        .border(
                            width = if (speed != 1f) 1.dp else 0.dp,
                            color = if (speed != 1f) AppleRed else Color.Transparent,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .clickable { showSpeedDialog = true }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = if (speed != 1f) AppleRed else Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                        Text(text = "${speed}x", color = if (speed != 1f) AppleRed else Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Queue chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { showQueueSheet = true }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(imageVector = Icons.Default.QueueMusic, contentDescription = "صف پخش", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                        Text(text = "صف پخش", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Equalizer chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(imageVector = Icons.Default.Equalizer, contentDescription = null, tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(14.dp))
                        Text(text = "EQ", color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Up Next — show next 3 tracks from the queue (Spotify style)
            val currentIdx = remember(queue, song.id) { queue.indexOfFirst { it.id == song.id } }
            val upNextSongs = remember(currentIdx, queue) {
                if (currentIdx >= 0) queue.drop(currentIdx + 1).take(3) else emptyList()
            }
            if (upNextSongs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "بعدی در صف",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = LocalAppFontFamily.current
                    )
                    Text(
                        text = "${upNextSongs.size} آهنگ",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                upNextSongs.forEach { nextSong ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.06f))
                            .clickable { viewModel.playbackConnection.playTrack(nextSong, queue) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = viewModel.getCoverArtUrl(nextSong.coverArt),
                            contentDescription = nextSong.title,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nextSong.title,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontFamily = LocalAppFontFamily.current
                            )
                            Text(
                                text = nextSong.artist ?: "—",
                                color = Color.White.copy(alpha = 0.45f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                fontFamily = LocalAppFontFamily.current
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Timer dialog
    if (showTimerDialog) {
        SleepTimerDialog(
            currentMinutes = sleepTimerLeft,
            onDismiss = { showTimerDialog = false },
            onSelect = { min ->
                if (min == 0) {
                    viewModel.playbackConnection.cancelSleepTimer()
                } else {
                    viewModel.playbackConnection.startSleepTimer(min)
                }
                showTimerDialog = false
            }
        )
    }

    // Speed multiplier dialog
    if (showSpeedDialog) {
        PlaybackSpeedDialog(
            currentSpeed = speed,
            onDismiss = { showSpeedDialog = false },
            onSelect = { spd ->
                viewModel.playbackConnection.setPlaybackSpeed(spd)
                showSpeedDialog = false
            }
        )
    }

    // Add to playlist dialog (triggered by long-press on cover)
    if (showAddToPlaylist) {
        val localPlaylists by viewModel.localPlaylists.collectAsStateWithLifecycle()
        PlaylistSelectorDialog(
            playlists = localPlaylists,
            onDismiss = { showAddToPlaylist = false },
            onSelect = { playlistId ->
                viewModel.addTrackToPlaylist(playlistId, song)
                showAddToPlaylist = false
            }
        )
    }

    // Playback queue bottom sheet
    if (showQueueSheet) {
        val queue by viewModel.playbackConnection.playbackQueue.collectAsStateWithLifecycle()
        val currentTrackId = song.id
        Dialog(onDismissRequest = { showQueueSheet = false }) {
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = CardDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp).heightIn(max = 480.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "صف پخش (${queue.size} آهنگ)",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontFamily = LocalAppFontFamily.current
                        )
                        IconButton(onClick = { showQueueSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White.copy(alpha = 0.6f))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (queue.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("صف پخش خالی است", color = Color.Gray, fontFamily = LocalAppFontFamily.current)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            itemsIndexed(queue) { index, qSong ->
                                val isActive = qSong.id == currentTrackId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isActive) AppleRed.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            viewModel.playbackConnection.playTrack(qSong, queue)
                                            showQueueSheet = false
                                        }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 12.sp,
                                        color = if (isActive) AppleRed else Color.Gray,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.width(22.dp),
                                        textAlign = TextAlign.Center
                                    )
                                    AsyncImage(
                                        model = viewModel.getCoverArtUrl(qSong.coverArt),
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = qSong.title,
                                            color = if (isActive) AppleRed else Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontFamily = LocalAppFontFamily.current
                                        )
                                        Text(
                                            text = qSong.artist ?: "",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontFamily = LocalAppFontFamily.current
                                        )
                                    }
                                    if (isActive) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = AppleRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Sleep Timer selection Dialog ---
@Composable
fun SleepTimerDialog(
    currentMinutes: Int?,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = CardDark) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("تنظیم تایمر خواب", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                if (currentMinutes != null) {
                    Text("تایمر فعال: $currentMinutes دقیقه باقی مانده", color = AppleRed, fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }

                val options = listOf(
                    5 to "۵ دقیقه دیگر",
                    15 to "۱۵ دقیقه دیگر",
                    30 to "۳۰ دقیقه دیگر",
                    60 to "۶۰ دقیقه دیگر",
                    0 to "توقف تایمر"
                )

                options.forEach { opt ->
                    TextButton(
                        onClick = { onSelect(opt.first) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(opt.second, color = if (opt.first == 0) Color.Gray else Color.White)
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                }

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AppleRed)) {
                    Text("انصراف", color = Color.White)
                }
            }
        }
    }
}

// --- Playback Speed selector Dialog ---
@Composable
fun PlaybackSpeedDialog(
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onSelect: (Float) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = CardDark) {
            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("سرعت پخش", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

                speeds.forEach { spd ->
                    val isSelected = currentSpeed == spd
                    TextButton(
                        onClick = { onSelect(spd) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "${spd}x", color = if (isSelected) AppleRed else Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                }

                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AppleRed)) {
                    Text("انصراف", color = Color.White)
                }
            }
        }
    }
}

// --- Synced Lyrics View Container with Autoscroll ---
@Composable
fun SyncedLyricsView(
    lyricsLines: List<LrcLine>,
    currentProgressMs: Long
) {
    val listState = rememberLazyListState()
    
    // Find active lyric index
    val activeIndex = lyricsLines.indexOfLast { currentProgressMs >= it.timestampMs }.coerceAtLeast(0)

    // Automatically scroll to active line
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && activeIndex < lyricsLines.size) {
            listState.animateScrollToItem(activeIndex, scrollOffset = -150)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        itemsIndexed(lyricsLines) { index, line ->
            val isActive = index == activeIndex
            val scale by animateFloatAsState(targetValue = if (isActive) 1.15f else 1.0f, label = "LineScale")
            val alpha by animateFloatAsState(targetValue = if (isActive) 1.0f else 0.4f, label = "LineAlpha")

            Text(
                text = line.text,
                color = if (isActive) AppleRed else Color.White,
                fontSize = 18.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
                modifier = Modifier
                    .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
                    .padding(horizontal = 24.dp)
            )
        }
    }
}

// --- LRC Parsing Engine Helpers ---
data class LrcLine(val timestampMs: Long, val text: String)

fun parseLrc(lyricsText: String): List<LrcLine> {
    val lines = lyricsText.split("\n")
    val list = mutableListOf<LrcLine>()
    val pattern = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2})](.*)")

    for (line in lines) {
        val match = pattern.find(line.trim())
        if (match != null) {
            val min = match.groupValues[1].toLong()
            val sec = match.groupValues[2].toLong()
            val ms = match.groupValues[3].toLong() * 10
            val text = match.groupValues[4].trim()
            val totalMs = (min * 60 * 1000) + (sec * 1000) + ms
            list.add(LrcLine(totalMs, text))
        }
    }
    return list.sortedBy { it.timestampMs }
}

// --- Car Mode: simplified full-screen player for driving ---
@Composable
fun CarModeScreen(viewModel: MusicViewModel, onClose: () -> Unit) {
    val currentTrack by viewModel.playbackConnection.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playbackConnection.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.playbackConnection.playbackProgress.collectAsStateWithLifecycle()
    val duration by viewModel.playbackConnection.trackDuration.collectAsStateWithLifecycle()

    val progressFraction = remember(progress, duration) {
        if (duration > 0) (progress.toFloat() / duration).coerceIn(0f, 1f) else 0f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Blurred album art background
        currentTrack?.let { track ->
            AsyncImage(
                model = viewModel.getCoverArtUrl(track.coverArt),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.18f),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .windowInsetsPadding(WindowInsets.systemBars),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top row: Car icon + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, tint = AppleRed, modifier = Modifier.size(20.dp))
                    Text(text = "حالت خودرو", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = LocalAppFontFamily.current)
                }
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "بستن", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(26.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Album art
            currentTrack?.let { track ->
                AsyncImage(
                    model = viewModel.getCoverArtUrl(track.coverArt),
                    contentDescription = track.title,
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .shadow(24.dp, RoundedCornerShape(28.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Song title + artist
            Text(
                text = currentTrack?.title ?: "—",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontFamily = LocalAppFontFamily.current
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentTrack?.artist ?: "",
                fontSize = 17.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontFamily = LocalAppFontFamily.current
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = AppleRed,
                trackColor = Color.White.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Big playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.playbackConnection.skipToPrevious() },
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(imageVector = Icons.Filled.SkipPrevious, contentDescription = "قبلی", tint = Color.White, modifier = Modifier.size(60.dp))
                }
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .shadow(12.dp, CircleShape)
                        .background(AppleRed, CircleShape)
                        .clickable { viewModel.playbackConnection.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "توقف" else "پخش",
                        tint = Color.White,
                        modifier = Modifier.size(58.dp)
                    )
                }
                IconButton(
                    onClick = { viewModel.playbackConnection.skipToNext() },
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(imageVector = Icons.Filled.SkipNext, contentDescription = "بعدی", tint = Color.White, modifier = Modifier.size(60.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// --- Timing and formatting utilities ---
fun formatListenersCount(listeners: String): String {
    val n = listeners.toLongOrNull() ?: return listeners
    return when {
        n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
        n >= 1_000 -> "%.1fK".format(n / 1_000.0)
        else -> n.toString()
    }
}

fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val min = totalSecs / 60
    val sec = totalSecs % 60
    return "%02d:%02d".format(min, sec)
}
