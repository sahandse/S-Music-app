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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.data.local.DownloadedTrack
import com.example.data.local.LocalPlaylist
import com.example.data.network.SubsonicAlbum
import com.example.data.network.SubsonicArtist
import com.example.ui.theme.CardDark
import com.example.data.network.SubsonicSong
import com.example.ui.MusicViewModel
import com.example.ui.theme.AppleRed
import com.example.ui.theme.AppleRedDark
import com.example.ui.theme.VazirFontFamily
import java.util.*
import kotlin.math.roundToInt

import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.Dp

@Composable
fun Modifier.glassmorphic(
    cornerRadius: Dp = 16.dp,
    borderColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
): Modifier {
    val surfaceColor = MaterialTheme.colorScheme.surface
    return this
        .clip(RoundedCornerShape(cornerRadius))
        .background(surfaceColor)
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
fun IntroScreen(viewModel: MusicViewModel) {
    val lang by viewModel.language.collectAsStateWithLifecycle()
    var currentPage by remember { mutableStateOf(0) }
    var nameInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x3BFA2D48), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.2f),
                        radius = size.width * 1.0f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x2BFA2D48), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.8f),
                        radius = size.width * 0.9f
                    )
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar with S Music Logo and Language Selection
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // S Music Logo (Small Header version)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(colors = listOf(AppleRed, Color(0xFFFF2D55)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Logo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "S Music",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = VazirFontFamily
                    )
                }

                // Language Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable {
                            viewModel.setLanguage(if (lang == "fa") "en" else "fa")
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (lang == "fa") "English" else "فارسی",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = VazirFontFamily
                    )
                }
            }

            // Image and Animated Content in the Center
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Beautiful Onboarding Feature Image (Using generated image)
                Box(
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                        .size(260.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                        .background(Color.DarkGray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.img_intro_feature_1782566304995),
                        contentDescription = "S Music Capabilities Onboarding Illustration",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                AnimatedContent(
                    targetState = currentPage,
                    transitionSpec = {
                        slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
                    },
                    label = "IntroPager",
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        when (page) {
                            0 -> {
                                Text(
                                    text = if (lang == "fa") "خوش آمدید به S Music" else "Welcome to S Music",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = VazirFontFamily,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (lang == "fa") 
                                        "تجربه پخشی بی‌نظیر و زیبا با استایل شیک اپل موزیک به همراه امکانات شخصی‌سازی بی‌نظیر." 
                                        else "An unparalleled, beautiful music streaming experience styled like Apple Music with high fidelity sound.",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontFamily = VazirFontFamily,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                            1 -> {
                                Text(
                                    text = if (lang == "fa") "دانلود هوشمند و آفلاین" else "Smart Offline Downloads",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = VazirFontFamily,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (lang == "fa") 
                                        "آهنگ‌های خود را با کیفیت بالا ذخیره کنید و به صورت کاملاً آفلاین بشنوید. شما می‌توانید مکان دانلود فایل‌ها را به دلخواه مشخص کنید." 
                                        else "Download tracks to your device and play them offline. You can choose where downloads are stored seamlessly.",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontFamily = VazirFontFamily,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                            }
                            2 -> {
                                Text(
                                    text = if (lang == "fa") "ورود بسیار آسان" else "Simple & Easy Login",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = VazirFontFamily,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = if (lang == "fa") 
                                        "برای شروع، لطفاً نام خود را وارد کنید تا محیط برنامه برای شما شخصی‌سازی شود." 
                                        else "To begin, please enter your name so we can personalize your experience.",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontFamily = VazirFontFamily,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text(if (lang == "fa") "نام شما" else "Your Name", fontFamily = VazirFontFamily) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppleRed,
                                        focusedLabelColor = AppleRed,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Controls
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Dot Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (currentPage == index) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (currentPage == index) AppleRed else Color.White.copy(alpha = 0.3f))
                        )
                    }
                }

                // Navigation Button
                Button(
                    onClick = {
                        if (currentPage < 2) {
                            currentPage++
                        } else {
                            if (nameInput.isBlank()) {
                                Toast.makeText(
                                    context,
                                    if (lang == "fa") "لطفاً نام خود را وارد کنید." else "Please enter your name.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                viewModel.performOnboardingLogin(nameInput)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleRed),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = if (currentPage < 2) (if (lang == "fa") "بعدی" else "Next") else (if (lang == "fa") "شروع سفر موسیقی" else "Start Music Journey"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = VazirFontFamily,
                        fontSize = 15.sp
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
    val introShown by viewModel.introShown.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (!introShown) {
            IntroScreen(viewModel = viewModel)
        } else if (!isConfigured) {
            LoginScreen(viewModel = viewModel)
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
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isTokenMode by remember { mutableStateOf(false) }

    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x3BFA2D48), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.2f),
                        radius = size.width * 1.0f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x2BFA2D48), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.8f),
                        radius = size.width * 0.9f
                    )
                )
            }
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(cornerRadius = 24.dp)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Apple Music inspired floating music note logo
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AppleRed, Color(0xFFFF2D55))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "S Music Logo",
                    tint = Color.White,
                    modifier = Modifier.size(54.dp)
                )
            }

            Text(
                text = "S Music",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "پخش موسیقی لذت‌بخش با استایل اپل موزیک",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("نام کاربری") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("username_input"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppleRed, focusedLabelColor = AppleRed)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("کلمه عبور یا توکن") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("password_input"),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppleRed, focusedLabelColor = AppleRed)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isTokenMode,
                    onCheckedChange = { isTokenMode = it },
                    colors = CheckboxDefaults.colors(checkedColor = AppleRed)
                )
                Text(
                    text = "ارسال رمز به صورت توکن مستقیم",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }

            if (loginError != null) {
                Text(
                    text = loginError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )
            }

            Button(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "لطفاً نام کاربری و کلمه عبور را تکمیل کنید", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.login(url, username, password, isTokenMode)
                    }
                },
                enabled = !isAuthenticating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("login_button"),
                colors = ButtonDefaults.buttonColors(containerColor = AppleRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isAuthenticating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("ورود به حساب کاربری", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            TextButton(
                onClick = { viewModel.bypassAuthAndGuestMode() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ورود به عنوان میهمان (نسخه نمایشی آفلاین)", color = AppleRed, fontWeight = FontWeight.SemiBold)
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

    val currentTrack by viewModel.playbackConnection.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.playbackConnection.isPlaying.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    // Internal sub-navigation state (Artist / Album details)
    var currentSubScreen by remember { mutableStateOf<SubScreen?>(null) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val isLight = themeMode == "light"
    val backgroundColor = if (isLight) Color.White else Color.Black
    val ambientColor1 = if (isLight) Color(0x1BFA2D48) else Color(0x3BFA2D48)
    val ambientColor2 = if (isLight) Color(0x13FA2D48) else Color(0x28FA2D48)

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
                        label = { Text("خانه", fontSize = 11.sp, fontFamily = VazirFontFamily) },
                        colors = NavigationRailItemDefaults.colors(selectedIconColor = AppleRed, selectedTextColor = AppleRed, indicatorColor = Color.Transparent),
                        modifier = Modifier.tvFocusable(cornerRadius = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    NavigationRailItem(
                        selected = selectedTab == 1 && currentSubScreen == null,
                        onClick = { selectedTab = 1; currentSubScreen = null },
                        icon = { Icon(imageVector = if (selectedTab == 1) Icons.Filled.Search else Icons.Outlined.Search, contentDescription = "جستجو") },
                        label = { Text("جستجو", fontSize = 11.sp, fontFamily = VazirFontFamily) },
                        colors = NavigationRailItemDefaults.colors(selectedIconColor = AppleRed, selectedTextColor = AppleRed, indicatorColor = Color.Transparent),
                        modifier = Modifier.tvFocusable(cornerRadius = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    NavigationRailItem(
                        selected = selectedTab == 2 && currentSubScreen == null,
                        onClick = { selectedTab = 2; currentSubScreen = null },
                        icon = { Icon(imageVector = if (selectedTab == 2) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic, contentDescription = "کتابخانه") },
                        label = { Text("کتابخانه", fontSize = 11.sp, fontFamily = VazirFontFamily) },
                        colors = NavigationRailItemDefaults.colors(selectedIconColor = AppleRed, selectedTextColor = AppleRed, indicatorColor = Color.Transparent),
                        modifier = Modifier.tvFocusable(cornerRadius = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    NavigationRailItem(
                        selected = selectedTab == 3 && currentSubScreen == null,
                        onClick = { selectedTab = 3; currentSubScreen = null },
                        icon = { Icon(imageVector = if (selectedTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings, contentDescription = "تنظیمات") },
                        label = { Text("تنظیمات", fontSize = 11.sp, fontFamily = VazirFontFamily) },
                        colors = NavigationRailItemDefaults.colors(selectedIconColor = AppleRed, selectedTextColor = AppleRed, indicatorColor = Color.Transparent),
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
                                    onAlbumClick = { currentSubScreen = SubScreen.AlbumDetail(it) }
                                )
                            }
                            currentSubScreen is SubScreen.AlbumDetail -> {
                                AlbumDetailView(
                                    album = (currentSubScreen as SubScreen.AlbumDetail).album,
                                    viewModel = viewModel,
                                    onBack = { currentSubScreen = null }
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
                            else -> {
                                when (selectedTab) {
                                    0 -> HomeScreen(
                                        viewModel = viewModel,
                                        onArtistClick = { currentSubScreen = SubScreen.ArtistDetail(it) },
                                        onAlbumClick = { currentSubScreen = SubScreen.AlbumDetail(it) },
                                        onDownloadClick = { currentSubScreen = SubScreen.SubLibraryItem("downloads") }
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
                                    3 -> SettingsScreen(viewModel = viewModel)
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
                                onDismiss = { isPlayerExpanded = false }
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
                                label = { Text("خانه", fontSize = 11.sp, fontFamily = VazirFontFamily) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = AppleRed, selectedTextColor = AppleRed, indicatorColor = Color.Transparent)
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1 && currentSubScreen == null,
                                onClick = { selectedTab = 1; currentSubScreen = null },
                                icon = { Icon(imageVector = if (selectedTab == 1) Icons.Filled.Search else Icons.Outlined.Search, contentDescription = "جستجو") },
                                label = { Text("جستجو", fontSize = 11.sp, fontFamily = VazirFontFamily) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = AppleRed, selectedTextColor = AppleRed, indicatorColor = Color.Transparent)
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2 && currentSubScreen == null,
                                onClick = { selectedTab = 2; currentSubScreen = null },
                                icon = { Icon(imageVector = if (selectedTab == 2) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic, contentDescription = "کتابخانه") },
                                label = { Text("کتابخانه", fontSize = 11.sp, fontFamily = VazirFontFamily) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = AppleRed, selectedTextColor = AppleRed, indicatorColor = Color.Transparent)
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3 && currentSubScreen == null,
                                onClick = { selectedTab = 3; currentSubScreen = null },
                                icon = { Icon(imageVector = if (selectedTab == 3) Icons.Filled.Settings else Icons.Outlined.Settings, contentDescription = "تنظیمات") },
                                label = { Text("تنظیمات", fontSize = 11.sp, fontFamily = VazirFontFamily) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = AppleRed, selectedTextColor = AppleRed, indicatorColor = Color.Transparent)
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
                                onAlbumClick = { currentSubScreen = SubScreen.AlbumDetail(it) }
                            )
                        }
                        currentSubScreen is SubScreen.AlbumDetail -> {
                            AlbumDetailView(
                                album = (currentSubScreen as SubScreen.AlbumDetail).album,
                                viewModel = viewModel,
                                onBack = { currentSubScreen = null }
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
                        else -> {
                            when (selectedTab) {
                                0 -> HomeScreen(
                                    viewModel = viewModel,
                                    onArtistClick = { currentSubScreen = SubScreen.ArtistDetail(it) },
                                    onAlbumClick = { currentSubScreen = SubScreen.AlbumDetail(it) },
                                    onDownloadClick = { currentSubScreen = SubScreen.SubLibraryItem("downloads") }
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
                                3 -> SettingsScreen(viewModel = viewModel)
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
                            onDismiss = { isPlayerExpanded = false }
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
}

// --- Mini Player Bar Component ---
@Composable
fun MiniPlayer(
    song: SubsonicSong,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    viewModel: MusicViewModel
) {
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
                IconButton(onClick = onPlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "پخش/توقف",
                        tint = AppleRed,
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
            }
        }
    }
}

// --- Home Screen ---
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onArtistClick: (SubsonicArtist) -> Unit,
    onAlbumClick: (SubsonicAlbum) -> Unit,
    onDownloadClick: () -> Unit
) {
    val randomSongs by viewModel.homeRandomSongs.collectAsStateWithLifecycle()
    val newestAlbums by viewModel.homeNewestAlbums.collectAsStateWithLifecycle()
    val artists by viewModel.artistsList.collectAsStateWithLifecycle()
    val recentPlays by viewModel.recentPlays.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingHome.collectAsStateWithLifecycle()

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AppleRed)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Top Bar with Greeting on right and Download Icon on left
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top Left: Download Icon
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

                // Top Right: Greeting Text using user's onboarding name
                val storedName by viewModel.userName.collectAsStateWithLifecycle()
                val greetingText = if (storedName.isNotBlank()) {
                    translate("سلام، $storedName!", "Hello, $storedName!", viewModel)
                } else {
                    translate("سلام، خوش آمدید!", "Hello, Welcome!", viewModel)
                }
                Text(
                    text = greetingText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = VazirFontFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Hero Header styled like Apple Music Live Banner with high-end glassmorphism
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AppleRed, AppleRedDark)
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .drawBehind {
                        // Drawing a glowing blurred backdrop bubble inside the banner
                        drawCircle(
                            color = Color.White.copy(alpha = 0.12f),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.1f),
                            radius = size.width * 0.25f
                        )
                    }
                    .padding(24.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Column {
                    Text(
                        text = "پیشنهاد ویژه امروز",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "S Music مجموعه برتر پاپ",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Continue Play / Recent History
            if (recentPlays.isNotEmpty()) {
                SectionTitle(title = "ادامه پخش")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentPlays) { play ->
                        val song = SubsonicSong(
                            id = play.trackId,
                            title = play.title,
                            artist = play.artist,
                            album = play.album,
                            coverArt = play.coverArtUrl,
                            duration = play.duration
                        )
                        HorizontalMediaCard(
                            title = play.title,
                            subtitle = play.artist,
                            imageUrl = play.coverArtUrl,
                            onClick = { viewModel.playSongDirectly(song, listOf(song)) }
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

// --- Section Header Title ---
@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
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
        AsyncImage(
            model = imageUrl,
            contentDescription = title,
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
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
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(90.dp)
            .tvFocusable(cornerRadius = 45.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = name,
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape),
            contentScale = ContentScale.Crop
        )
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
    val isFavorite = favoriteTracks.any { it.id == song.id }
    val isDownloaded = downloadedTracks.any { it.id == song.id }

    var showPlaylistDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
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
            AsyncImage(
                model = viewModel.getCoverArtUrl(song.coverArt),
                contentDescription = song.title,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(6.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = song.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
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

    var searchTab by remember { mutableStateOf(0) } // 0: آهنگ, 1: هنرمند

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
            // Search Input Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        viewModel.updateSearchQuery(it)
                    },
                    placeholder = { Text("جستجوی آهنگ، آلبوم، هنرمند...", fontFamily = VazirFontFamily) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_input"),
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppleRed)
                )

                // Voice Search
                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR") // Persian Voice Search
                            }
                            voiceLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "جستجوی صوتی در دسترس نیست.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .size(48.dp)
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice Search", tint = AppleRed)
                }
            }

            // Beautiful Minimalist Tab Selector (Songs / Artists)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Song Tab
                val isSongActive = searchTab == 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSongActive) AppleRed.copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSongActive) AppleRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .tvFocusable(cornerRadius = 12.dp)
                        .clickable { searchTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "آهنگ",
                        fontSize = 14.sp,
                        fontWeight = if (isSongActive) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = VazirFontFamily,
                        color = if (isSongActive) AppleRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }

                // Artist Tab
                val isArtistActive = searchTab == 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isArtistActive) AppleRed.copy(alpha = 0.15f) else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isArtistActive) AppleRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .tvFocusable(cornerRadius = 12.dp)
                        .clickable { searchTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "هنرمند",
                        fontSize = 14.sp,
                        fontWeight = if (isArtistActive) FontWeight.Bold else FontWeight.Medium,
                        fontFamily = VazirFontFamily,
                        color = if (isArtistActive) AppleRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }

            if (isSearching) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppleRed)
                }
            } else if (query.isEmpty()) {
                // Search History & Suggestions Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (history.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "تاریخچه جستجو", fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VazirFontFamily)
                            TextButton(onClick = { viewModel.clearSearchHistory() }) {
                                Text("پاک کردن همه", color = AppleRed, fontFamily = VazirFontFamily)
                            }
                        }

                        LazyColumn {
                            items(history) { hist ->
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
                                    Text(text = hist, fontSize = 14.sp, fontFamily = VazirFontFamily)
                                }
                                HorizontalDivider()
                            }
                        }
                    } else {
                        // Quick Search Suggestions
                        Text(text = "پیشنهادهای جستجو", fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VazirFontFamily)
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
                                    label = { Text(sugg, fontFamily = VazirFontFamily) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Search Results Panel
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (searchTab == 0) {
                        // Songs
                        val songs = searchResults.song ?: emptyList()
                        if (songs.isNotEmpty()) {
                            item {
                                Text(
                                    text = "آهنگ‌ها",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = AppleRed,
                                    fontFamily = VazirFontFamily,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(songs) { song ->
                                TrackRowItem(
                                    song = song,
                                    onPlay = { viewModel.playSongDirectly(song, songs) },
                                    viewModel = viewModel
                                )
                            }
                        } else {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("آهنگی یافت نشد", color = Color.Gray, fontFamily = VazirFontFamily)
                                }
                            }
                        }
                    } else {
                        // Artists & Albums
                        val artists = searchResults.artist ?: emptyList()
                        val albums = searchResults.album ?: emptyList()

                        if (artists.isNotEmpty() || albums.isNotEmpty()) {
                            if (artists.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "هنرمندان",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = AppleRed,
                                        fontFamily = VazirFontFamily,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                items(artists) { artist ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .tvFocusable(cornerRadius = 8.dp)
                                            .clickable { onArtistClick(artist) }
                                            .padding(vertical = 8.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        AsyncImage(
                                            model = viewModel.getCoverArtUrl(artist.coverArt),
                                            contentDescription = artist.name,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                        Text(text = artist.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = VazirFontFamily)
                                    }
                                    HorizontalDivider()
                                }
                            }

                            if (albums.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "آلبوم‌ها",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = AppleRed,
                                        fontFamily = VazirFontFamily,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                items(albums) { album ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .tvFocusable(cornerRadius = 8.dp)
                                            .clickable { onAlbumClick(album) }
                                            .padding(vertical = 8.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        AsyncImage(
                                            model = viewModel.getCoverArtUrl(album.coverArt),
                                            contentDescription = album.name,
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Column {
                                            Text(text = album.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = VazirFontFamily)
                                            Text(text = album.artist ?: "Unknown Artist", fontSize = 11.sp, color = Color.Gray, fontFamily = VazirFontFamily)
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        } else {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("هنرمند یا آلبومی یافت نشد", color = Color.Gray, fontFamily = VazirFontFamily)
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
            Triple("downloads", "دانلودهای آفلاین", Icons.Outlined.OfflinePin)
        )

        items(categories) { cat ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(cat.first) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(imageVector = cat.third, contentDescription = null, tint = AppleRed, modifier = Modifier.size(24.dp))
                Text(text = cat.second, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = null, tint = Color.Gray)
            }
            HorizontalDivider()
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
                        .clickable { onPlaylistClick(playlist) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Playlist dummy icon
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppleRed.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.QueueMusic, contentDescription = null, tint = AppleRed)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = playlist.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = playlist.description ?: "بدون توضیح", fontSize = 11.sp, color = Color.Gray)
                    }

                    IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = Color.Gray)
                    }
                }
                HorizontalDivider()
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
                    val usedLabel = translate("دانلودهای اس موزیک:", "S Music Downloads:", viewModel)
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
                                            fontFamily = VazirFontFamily
                                        )
                                        Text(
                                            text = translate("${downloads.size} آهنگ ذخیره شده", "${downloads.size} Tracks Saved", viewModel),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            fontFamily = VazirFontFamily
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
                                                Text(text = "$usedLabel $totalMB", fontSize = 10.sp, fontFamily = VazirFontFamily, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)))
                                                Text(text = "$otherLabel $otherMB", fontSize = 10.sp, fontFamily = VazirFontFamily, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.1f)).border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), CircleShape))
                                            Text(text = "$freeLabel $freeMB", fontSize = 10.sp, fontFamily = VazirFontFamily, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
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
                                            fontFamily = VazirFontFamily
                                        )
                                        Text(
                                            text = if (downloadLocation.isBlank()) defaultLoc else downloadLocation,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = VazirFontFamily,
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
                                        Text(text = changeBtn, fontSize = 11.sp, fontFamily = VazirFontFamily, fontWeight = FontWeight.Bold)
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
                                            fontFamily = VazirFontFamily,
                                            color = AppleRed
                                        )
                                        Text(
                                            text = translate(
                                                "${activeDownloads.size} آهنگ در صف دانلود قرار دارد.",
                                                "${activeDownloads.size} tracks in download queue.",
                                                viewModel
                                            ),
                                            fontSize = 10.sp,
                                            fontFamily = VazirFontFamily,
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
                            placeholder = { Text(searchPlaceholder, fontFamily = VazirFontFamily) },
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
                                fontFamily = VazirFontFamily,
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
                                            fontFamily = VazirFontFamily,
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
                                    TrackRowItem(
                                        song = song,
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
                            title = { Text(translate("تغییر مکان ذخیره‌سازی", "Change Storage Path", viewModel), fontFamily = VazirFontFamily, fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = translate(
                                            "مسیر دلخواه پوشه ذخیره‌سازی فایل‌های صوتی دانلود شده را وارد کنید (خالی بگذارید تا مسیر پیش‌فرض استفاده شود):",
                                            "Enter custom directory path to save audio downloads (leave empty for default path):",
                                            viewModel
                                        ),
                                        fontSize = 13.sp,
                                        fontFamily = VazirFontFamily
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
                                    Text(translate("ذخیره", "Save", viewModel), fontFamily = VazirFontFamily, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showLocationDialog = false }) {
                                    Text(translate("انصراف", "Cancel", viewModel), fontFamily = VazirFontFamily, color = Color.Gray)
                                }
                            }
                        )
                    }

                    // Delete All Downloads Confirmation Dialog
                    if (showDeleteAllDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteAllDialog = false },
                            title = { Text(translate("حذف همه فایل‌های دانلود شده", "Delete All Downloads", viewModel), fontFamily = VazirFontFamily, fontWeight = FontWeight.Bold) },
                            text = {
                                Text(
                                    text = translate(
                                        "آیا واقعاً می‌خواهید تمامی آهنگ‌های دانلود شده آفلاین را حذف کنید؟ این عمل غیرقابل بازگشت است.",
                                        "Are you sure you want to delete all offline downloaded tracks? This action cannot be undone.",
                                        viewModel
                                    ),
                                    fontSize = 14.sp,
                                    fontFamily = VazirFontFamily
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
                                    Text(translate("حذف همه", "Delete All", viewModel), fontFamily = VazirFontFamily, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteAllDialog = false }) {
                                    Text(translate("انصراف", "Cancel", viewModel), fontFamily = VazirFontFamily, color = Color.Gray)
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailView(
    artist: SubsonicArtist,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onAlbumClick: (SubsonicAlbum) -> Unit
) {
    val albums by viewModel.selectedArtistAlbums.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingDetail.collectAsStateWithLifecycle()

    LaunchedEffect(artist.id) {
        viewModel.loadArtistDetails(artist)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(artist.name, fontWeight = FontWeight.Bold) },
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
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(bottom = 80.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        AsyncImage(
                            model = viewModel.getCoverArtUrl(artist.coverArt),
                            contentDescription = artist.name,
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = artist.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.toggleFavoriteArtist(artist) },
                            colors = ButtonDefaults.buttonColors(containerColor = AppleRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            var isFav by remember { mutableStateOf(false) }
                            LaunchedEffect(artist.id) {
                                isFav = viewModel.favoriteArtists.value.any { it.id == artist.id }
                            }
                            Text(if (isFav) "حذف از علاقه‌مندی" else "افزودن به علاقه‌مندی")
                        }
                    }
                }

                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Text(text = "آلبوم‌ها", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
                }

                if (albums.isEmpty()) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("هیچ آلبومی یافت نشد.", color = Color.Gray)
                        }
                    }
                } else {
                    items(albums) { album ->
                        VerticalBigCard(
                            title = album.name,
                            subtitle = album.artist ?: artist.name,
                            imageUrl = viewModel.getCoverArtUrl(album.coverArt),
                            onClick = { onAlbumClick(album) }
                        )
                    }
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
    onBack: () -> Unit
) {
    val songs by viewModel.selectedAlbumSongs.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingDetail.collectAsStateWithLifecycle()

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

// --- Settings Screen ---
@Composable
fun SettingsScreen(viewModel: MusicViewModel) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val audioQuality by viewModel.audioQuality.collectAsStateWithLifecycle()
    val cacheSize by viewModel.cacheSizeMb.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = translate("تنظیمات", "Settings", viewModel), 
            fontSize = 32.sp, 
            fontWeight = FontWeight.Bold, 
            modifier = Modifier.padding(bottom = 8.dp),
            fontFamily = VazirFontFamily
        )

        // Language Selection Option
        Text(text = translate("زبان برنامه", "App Language", viewModel), fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VazirFontFamily)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val languages = listOf("fa" to "فارسی", "en" to "English")
            languages.forEach { langOption ->
                val isSelected = language == langOption.first
                Button(
                    onClick = { viewModel.setLanguage(langOption.first) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) AppleRed else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(langOption.second, fontSize = 13.sp, fontFamily = VazirFontFamily)
                }
            }
        }

        // Themes Customization Row
        Text(text = translate("ظاهر برنامه (پوسته)", "App Theme", viewModel), fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VazirFontFamily)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val themes = listOf(
                "light" to translate("روشن", "Light", viewModel), 
                "dark" to translate("تاریک", "Dark", viewModel), 
                "amoled" to translate("آمولد", "Amoled", viewModel), 
                "glass" to translate("شیشه‌ای", "Glass", viewModel)
            )
            themes.forEach { t ->
                val isSelected = themeMode == t.first
                Button(
                    onClick = { viewModel.setThemeMode(t.first) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(t.second, fontSize = 13.sp, fontFamily = VazirFontFamily)
                }
            }
        }

        // Accent Color Selection Row
        Text(
            text = translate("رنگ تأکید برنامه (طرح رنگی)", "App Accent Color", viewModel),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            fontFamily = VazirFontFamily
        )
        val themeAccent by viewModel.themeAccent.collectAsStateWithLifecycle()
        val accentOptions = listOf(
            "red" to (Color(0xFFFA2D48) to translate("قرمز", "Red", viewModel)),
            "blue" to (Color(0xFF007AFF) to translate("آبی", "Blue", viewModel)),
            "green" to (Color(0xFF34C759) to translate("سبز", "Green", viewModel)),
            "orange" to (Color(0xFFFF9500) to translate("نارنجی", "Orange", viewModel)),
            "purple" to (Color(0xFFAF52DE) to translate("بنفش", "Purple", viewModel)),
            "pink" to (Color(0xFFFF2D55) to translate("صورتی", "Pink", viewModel))
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            accentOptions.forEach { opt ->
                val isSelected = themeAccent == opt.first
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(opt.second.first)
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { viewModel.setThemeAccent(opt.first) }
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = opt.second.second,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Audio Quality settings
        Text(text = translate("کیفیت پخش موسیقی", "Audio Streaming Quality", viewModel), fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VazirFontFamily)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val qualities = listOf(
                "low" to translate("کیفیت پایین (صرفه‌جویی در مصرف دیتا)", "Low Quality (Data Saver)", viewModel),
                "medium" to translate("کیفیت متوسط (استاندارد)", "Medium Quality (Standard)", viewModel),
                "high" to translate("کیفیت عالی (پخش با وضوح بالا)", "High Quality (High Resolution)", viewModel),
                "lossless" to translate("کیفیت بدون افت (Lossless FLAC)", "Lossless Quality (FLAC)", viewModel)
            )
            qualities.forEach { q ->
                val isSelected = audioQuality == q.first
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) AppleRed.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { viewModel.setAudioQuality(q.first) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = q.second, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, fontFamily = VazirFontFamily)
                    if (isSelected) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = AppleRed)
                    }
                }
            }
        }

        // Cache manager
        Text(text = translate("مدیریت حافظه کش", "Cache Storage Manager", viewModel), fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = VazirFontFamily)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = translate("حداکثر حجم فایل‌های موقت: $cacheSize مگابایت", "Max Cache Size: $cacheSize MB", viewModel), fontSize = 13.sp, fontFamily = VazirFontFamily)
            Slider(
                value = cacheSize.toFloat(),
                onValueChange = { viewModel.setCacheSizeMb(it.roundToInt()) },
                valueRange = 64f..1024f,
                colors = SliderDefaults.colors(activeTrackColor = AppleRed, thumbColor = AppleRed)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.clearCache()
                        val msg = if (language == "fa") "حافظه موقت پاکسازی شد." else "Temporary cache cleared."
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AppleRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(translate("پاک کردن کش موقت", "Clear Temporary Cache", viewModel), fontFamily = VazirFontFamily)
                }
            }
        }

        // About the application
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(cornerRadius = 16.dp)
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = translate("درباره ما", "About Us", viewModel),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = AppleRed,
                    fontFamily = VazirFontFamily
                )
                
                Text(
                    text = translate("سهند رضوان", "Sahand Rezvan", viewModel),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = VazirFontFamily
                )
                
                Text(
                    text = translate("توسعه‌دهنده و طراح نرم‌افزار S Music", "S Music Developer & Designer", viewModel),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontFamily = VazirFontFamily
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppleRed.copy(alpha = 0.1f))
                        .clickable { uriHandler.openUri("https://t.me/sahandse") }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Telegram",
                            tint = AppleRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = translate("ارتباط در تلگرام", "Contact on Telegram", viewModel),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppleRed,
                            fontFamily = VazirFontFamily
                        )
                    }
                    Text(
                        text = "@sahandse",
                        fontSize = 13.sp,
                        color = AppleRed.copy(alpha = 0.8f)
                    )
                }

                Text(
                    text = translate("نسخه: 1.0.0 (پروژه ویژه S Music)", "Version: 1.0.0 (S Music Special Edition)", viewModel),
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontFamily = VazirFontFamily
                )
            }
        }
    }
}

// --- Gorgeous Sliding Fullscreen Player Sheet ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenPlayer(
    viewModel: MusicViewModel,
    onDismiss: () -> Unit
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

    var showTimerDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Toggle Tab: 0: AlbumArt Cover, 1: Lyrics list
    var playerTab by remember { mutableStateOf(0) }

    if (currentTrack == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }

    val song = currentTrack!!

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Premium dark background for player always!
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

        // Dark Overlay to keep text perfectly high-contrast, with frosted glass highlight gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .drawBehind {
                    // White frosted glass highlight from top left
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.08f), Color.Transparent),
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

                IconButton(onClick = { showTimerDialog = true }) {
                    Icon(
                        imageVector = if (sleepTimerLeft != null) Icons.Filled.Timer else Icons.Outlined.Timer,
                        contentDescription = "تایمر خواب",
                        tint = if (sleepTimerLeft != null) AppleRed else Color.White
                    )
                }
            }

            // Central Area (either big artwork or lyrics list)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
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

                        AsyncImage(
                            model = viewModel.getCoverArtUrl(song.coverArt),
                            contentDescription = song.title,
                            modifier = Modifier
                                .size(280.dp)
                                .graphicsLayer(scaleX = scale, scaleY = scale)
                                .clip(RoundedCornerShape(24.dp))
                                .shadow(16.dp, RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop
                        )

                        // Real-Time Synchronized Lyrics Overlay Pill
                        if (isLoadingLyrics) {
                            Text(
                                text = translate("درحال بارگذاری متن...", "Loading lyrics...", viewModel),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                fontFamily = VazirFontFamily
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
                                        fontFamily = VazirFontFamily,
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
                                        fontFamily = VazirFontFamily
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
                        Text(
                            text = song.artist ?: "هنرمند ناشناس",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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

                // Progress slider track
                val sliderPosition = if (duration > 0) progress.toFloat() / duration else 0f
                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        viewModel.playbackConnection.seekTo((it * duration).toLong())
                    },
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.24f),
                        thumbColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatTime(progress), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    Text(text = formatTime(duration), color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                }
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

                // Giant centered play/pause trigger with background ripple accent
                IconButton(
                    onClick = { viewModel.playbackConnection.togglePlayPause() },
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "پخش/توقف",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
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

            Spacer(modifier = Modifier.height(16.dp))

            // Lower accessories toolbar (Speed dial, equalizer trigger info)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showSpeedDialog = true }) {
                    Text(text = "سرعت: ${speed}x", color = Color.White, fontSize = 13.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(imageVector = Icons.Default.Equalizer, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    Text(text = "اکولایزر خودکار فعال", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
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

// --- Timing and formatting utilities ---
fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val min = totalSecs / 60
    val sec = totalSecs % 60
    return "%02d:%02d".format(min, sec)
}
