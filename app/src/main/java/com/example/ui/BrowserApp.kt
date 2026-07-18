package com.example.ui

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.BlockedSite
import com.example.data.database.Skill
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.SyncState
import kotlinx.coroutines.launch

// Custom Brand Colors derived from Go learn a skill logo
val NavyPrimary = Color(0xFF1E3A5F)
val NavyDark = Color(0xFF102542)
val GreenAccent = Color(0xFF5CAD2F)
val SoftCream = Color(0xFFFAFAFA)
val SurfaceCardLight = Color(0xFFF0F4F8)
val FocusOrange = Color(0xFFE0533C)

data class AppColors(
    val primary: Color,
    val primaryDark: Color,
    val accent: Color,
    val bg: Color,
    val card: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val isDark: Boolean
)

val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        primary = Color(0xFF1E3A5F),
        primaryDark = Color(0xFF102542),
        accent = Color(0xFF5CAD2F),
        bg = Color(0xFFFAFAFA),
        card = Color(0xFFF0F4F8),
        textPrimary = Color(0xFF102542),
        textSecondary = Color.Gray,
        isDark = false
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserApp(viewModel: BrowserViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val activeSkill by viewModel.activeSkill.collectAsStateWithLifecycle()
    val sessionTime by viewModel.sessionTimeSeconds.collectAsStateWithLifecycle()
    val isFocusEnabled by viewModel.isFocusModeEnabled.collectAsStateWithLifecycle()
    val pomoState by viewModel.pomodoroState.collectAsStateWithLifecycle()
    val pomoMode by viewModel.pomodoroMode.collectAsStateWithLifecycle()
    val pomoTimeLeft by viewModel.pomodoroTimeLeft.collectAsStateWithLifecycle()
    
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val themeAccent by viewModel.themeAccent.collectAsStateWithLifecycle()

    val isDark = when (themeMode) {
        "Dark" -> true
        "Light" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    val (primary, primaryDark, accent) = when (themeAccent) {
        "Emerald" -> Triple(Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFF4CAF50))
        "Ocean" -> Triple(Color(0xFF0288D1), Color(0xFF01579B), Color(0xFF00BCD4))
        "Cosmic" -> Triple(Color(0xFF7B1FA2), Color(0xFF4A148C), Color(0xFF9C27B0))
        "Sunset" -> Triple(Color(0xFFE64A19), Color(0xFFBF360C), Color(0xFFFFA726))
        else -> Triple(Color(0xFF1E3A5F), Color(0xFF102542), Color(0xFF5CAD2F))
    }

    val appColors = AppColors(
        primary = primary,
        primaryDark = primaryDark,
        accent = accent,
        bg = if (isDark) Color(0xFF121212) else Color(0xFFFAFAFA),
        card = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF0F4F8),
        textPrimary = if (isDark) Color.White else primaryDark,
        textSecondary = if (isDark) Color.LightGray else Color.Gray,
        isDark = isDark
    )

    CompositionLocalProvider(LocalAppColors provides appColors) {
        val colors = LocalAppColors.current
        val NavyPrimary = colors.primary
        val NavyDark = colors.primaryDark
        val GreenAccent = colors.accent
        val SoftCream = colors.bg
        val SurfaceCardLight = colors.card

        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var showAddSkillDialog by remember { mutableStateOf(false) }

        Scaffold(
        topBar = {
            Column {
                // Header Banner / Branding Bar
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Render our generated brand logo if possible
                            Image(
                                painter = painterResource(id = com.example.R.drawable.app_logo_1784385198428),
                                contentDescription = "Go logo",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, GreenAccent, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Go",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = NavyPrimary,
                                fontSize = 24.sp
                            )
                        }
                    },
                    actions = {
                        // Focus mode toggle right in the top bar
                        IconButton(
                            onClick = { 
                                viewModel.toggleFocusMode()
                                val msg = if (!isFocusEnabled) "Focus Mode Shield Activated! 🛡️" else "Focus Mode Paused."
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("top_focus_toggle")
                        ) {
                            Icon(
                                imageVector = if (isFocusEnabled) Icons.Filled.Security else Icons.Outlined.Security,
                                contentDescription = "Toggle Focus Shield",
                                tint = if (isFocusEnabled) GreenAccent else Color.Gray,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Active Learning & Pomodoro persistent header bar
                val showPomoBanner = pomoState != "IDLE"
                AnimatedVisibility(
                    visible = activeSkill != null || showPomoBanner,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    val bannerBg: List<Color>
                    val bannerIcon: androidx.compose.ui.graphics.vector.ImageVector
                    val bannerTitle: String
                    val bannerTime: String
                    val actionBtnText: String
                    val onActionBtnClick: () -> Unit

                    when {
                        activeSkill != null && showPomoBanner -> {
                            bannerTitle = when (pomoMode) {
                                "WORK" -> "FOCUS WORK: ${activeSkill?.name}"
                                "SHORT_BREAK" -> "☕ BREAK: Refreshing"
                                "LONG_BREAK" -> "🌿 BREAK: Restful"
                                else -> "FOCUS WORK: ${activeSkill?.name}"
                            }
                            bannerTime = "${formatTime(pomoTimeLeft.toLong())} ($pomoState)"
                            bannerBg = when (pomoMode) {
                                "WORK" -> listOf(Color(0xFFE0533C), Color(0xFFC0392B))
                                else -> listOf(GreenAccent, NavyPrimary)
                            }
                            actionBtnText = if (pomoState == "RUNNING") "Pause" else "Focus"
                            onActionBtnClick = {
                                if (pomoState == "RUNNING") viewModel.pausePomodoro() else viewModel.startPomodoro()
                            }
                            bannerIcon = if (pomoMode == "WORK") Icons.Default.Timer else Icons.Default.Spa
                        }
                        showPomoBanner -> {
                            bannerTitle = when (pomoMode) {
                                "WORK" -> "DEEP FOCUS WORK"
                                "SHORT_BREAK" -> "☕ SHORT BREAK"
                                "LONG_BREAK" -> "🌿 LONG BREAK"
                                else -> "FOCUS SESSION"
                            }
                            bannerTime = "${formatTime(pomoTimeLeft.toLong())} ($pomoState)"
                            bannerBg = when (pomoMode) {
                                "WORK" -> listOf(Color(0xFFE0533C), Color(0xFFBF360C))
                                else -> listOf(GreenAccent, NavyPrimary)
                            }
                            actionBtnText = if (pomoState == "RUNNING") "Pause" else "Focus"
                            onActionBtnClick = {
                                if (pomoState == "RUNNING") viewModel.pausePomodoro() else viewModel.startPomodoro()
                            }
                            bannerIcon = if (pomoMode == "WORK") Icons.Default.Timer else Icons.Default.Spa
                        }
                        activeSkill != null -> {
                            bannerTitle = "STUDYING: ${activeSkill?.name}"
                            bannerTime = "Session Timer: ${formatTime(sessionTime)}"
                            bannerBg = listOf(NavyPrimary, NavyDark)
                            actionBtnText = "End Session"
                            onActionBtnClick = {
                                viewModel.stopStudySession()
                                Toast.makeText(context, "Session Saved!", Toast.LENGTH_SHORT).show()
                            }
                            bannerIcon = Icons.Default.Timer
                        }
                        else -> {
                            bannerTitle = ""
                            bannerTime = ""
                            bannerBg = listOf(NavyPrimary, NavyDark)
                            actionBtnText = ""
                            onActionBtnClick = {}
                            bannerIcon = Icons.Default.Timer
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(colors = bannerBg))
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = bannerIcon,
                                contentDescription = "Active progress",
                                tint = if (pomoMode == "WORK" && showPomoBanner) Color.White else GreenAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = bannerTitle,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = bannerTime,
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Button(
                            onClick = onActionBtnClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (pomoMode == "WORK" && showPomoBanner) Color.White else GreenAccent,
                                contentColor = if (pomoMode == "WORK" && showPomoBanner) Color(0xFFE0533C) else Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("top_banner_action_btn")
                        ) {
                            Text(text = actionBtnText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(1.dp),
                    color = GreenAccent,
                    trackColor = Color.Transparent
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("app_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { viewModel.setTab(0) },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Learn") },
                    label = { Text("Learn") },
                    modifier = Modifier.testTag("nav_tab_learn")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { viewModel.setTab(1) },
                    icon = { Icon(Icons.Default.Language, contentDescription = "Browser") },
                    label = { Text("Browser") },
                    modifier = Modifier.testTag("nav_tab_browser")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { viewModel.setTab(2) },
                    icon = { 
                        BadgedBox(
                            badge = { 
                                if (isFocusEnabled) {
                                    Badge(containerColor = GreenAccent) {
                                        Text("🛡️", fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Block, contentDescription = "Focus Shield")
                        }
                    },
                    label = { Text("Shield") },
                    modifier = Modifier.testTag("nav_tab_shield")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { viewModel.setTab(3) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 0) {
                FloatingActionButton(
                    onClick = { showAddSkillDialog = true },
                    containerColor = NavyPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_skill_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Skill")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> SkillsTab(viewModel)
                1 -> BrowserTab(viewModel)
                2 -> FocusShieldTab(viewModel)
                3 -> SyncTab(viewModel)
            }
        }
    }

    if (showAddSkillDialog) {
        AddSkillDialog(
            onDismiss = { showAddSkillDialog = false },
            onAdd = { name, url ->
                viewModel.addSkill(name, url)
                showAddSkillDialog = false
                Toast.makeText(context, "New Skill Added!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
}

// ----------------------------------------
// 1. SKILLS DASHBOARD TAB
// ----------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SkillsTab(viewModel: BrowserViewModel) {
    val colors = LocalAppColors.current
    val NavyPrimary = colors.primary
    val NavyDark = colors.primaryDark
    val GreenAccent = colors.accent
    val SoftCream = colors.bg
    val SurfaceCardLight = colors.card

    val skills by viewModel.skills.collectAsStateWithLifecycle()
    val activeSkill by viewModel.activeSkill.collectAsStateWithLifecycle()

    var selectedSubTab by remember { mutableStateOf(0) } // 0: My Goals, 1: Discover Free Courses

    Column(modifier = Modifier.fillMaxSize()) {
        // Dual Sub-Tab Switcher
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = SoftCream,
            contentColor = NavyPrimary
        ) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("My Goals", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                selectedContentColor = NavyPrimary,
                unselectedContentColor = Color.Gray,
                modifier = Modifier.testTag("skills_tab_sub_goals")
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Discover Courses", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                selectedContentColor = NavyPrimary,
                unselectedContentColor = Color.Gray,
                modifier = Modifier.testTag("skills_tab_sub_courses")
            )
        }

        if (selectedSubTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("skills_list"),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "My Study Goals",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Track your study minutes, draft notes, and browse resources distraction-free.",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Pomodoro Focus Timer Card
                item {
                    PomodoroTimerCard(viewModel)
                }

                if (skills.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Empty board",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No Skills added yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tap the '+' button below to define a skill or discover courses in the other tab!",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(skills, key = { it.id }) { skill ->
                        val isStudyingThis = activeSkill?.id == skill.id
                        SkillItemCard(
                            skill = skill,
                            isStudyingActive = isStudyingThis,
                            onStartSession = { viewModel.startStudySession(skill) },
                            onToggleComplete = { viewModel.toggleSkillCompleted(skill) },
                            onDelete = { viewModel.deleteSkill(skill) },
                            onSaveNotes = { notes -> viewModel.updateSkillNotes(skill, notes) }
                        )
                    }
                }
            }
        } else {
            // Render the beautiful Discover Courses panel!
            DiscoverCoursesSection(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverCoursesSection(viewModel: BrowserViewModel) {
    val colors = LocalAppColors.current
    val NavyPrimary = colors.primary
    val NavyDark = colors.primaryDark
    val GreenAccent = colors.accent
    val SoftCream = colors.bg
    val SurfaceCardLight = colors.card

    val courses by viewModel.filteredCourses.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoadingCourses.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.courseCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.courseSearchQuery.collectAsStateWithLifecycle()

    val categories = listOf("All", "Android & Kotlin", "Web Dev", "AI & Data Science", "Design")
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftCream)
            .padding(16.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setCourseSearchQuery(it) },
            placeholder = { Text("Search free courses...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = Color.Gray) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setCourseSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color.Gray)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("course_search_input"),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NavyPrimary,
                unfocusedBorderColor = Color.LightGray,
                focusedContainerColor = if (colors.isDark) Color(0xFF262626) else Color.White,
                unfocusedContainerColor = if (colors.isDark) Color(0xFF1E1E1E) else Color.White
            ),
            textStyle = TextStyle(fontSize = 13.sp, color = colors.textPrimary)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Category Chips Row
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(categories) { cat ->
                val isSelected = cat == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setCourseCategory(cat) },
                    label = { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NavyPrimary,
                        selectedLabelColor = Color.White,
                        containerColor = SurfaceCardLight,
                        labelColor = colors.textSecondary
                    ),
                    modifier = Modifier.testTag("course_category_$cat")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Courses List
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenAccent)
            }
        } else if (courses.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No free courses match your filter criteria.", color = colors.textSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().testTag("courses_list")
            ) {
                items(courses) { course ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("course_card_${course.id}"),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCardLight),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = course.category.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenAccent
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = course.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "By ${course.provider}",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(NavyPrimary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Rating",
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = course.rating.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = course.description,
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                lineHeight = 17.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = "Duration",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = course.duration, fontSize = 11.sp, color = colors.textSecondary)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Book,
                                            contentDescription = "Difficulty",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = course.difficulty, fontSize = 11.sp, color = colors.textSecondary)
                                    }
                                }

                                Button(
                                    onClick = {
                                        viewModel.enrollInCourse(course)
                                        Toast.makeText(context, "Enrolled in ${course.title}! Added to Goals.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (course.isEnrolled) Color.Gray else GreenAccent
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp).testTag("course_enroll_btn_${course.id}"),
                                    enabled = !course.isEnrolled
                                ) {
                                    if (course.isEnrolled) {
                                        Icon(Icons.Default.Check, contentDescription = "Enrolled", modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Enrolled", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    } else {
                                        Text("Enroll & Study", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

@Composable
fun SkillItemCard(
    skill: Skill,
    isStudyingActive: Boolean,
    onStartSession: () -> Unit,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onSaveNotes: (String) -> Unit
) {
    val colors = LocalAppColors.current
    val NavyPrimary = colors.primary
    val NavyDark = colors.primaryDark
    val GreenAccent = colors.accent
    val SoftCream = colors.bg
    val SurfaceCardLight = colors.card

    var isExpanded by remember { mutableStateOf(false) }
    var notesText by remember { mutableStateOf(skill.notes) }
    val context = LocalContext.current

    // Sync state locally if external db updates notes
    LaunchedEffect(skill.notes) {
        notesText = skill.notes
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isStudyingActive) 2.dp else 0.dp,
                color = if (isStudyingActive) GreenAccent else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .testTag("skill_card_${skill.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (skill.isCompleted) Color(0xFFE8F5E9) else SurfaceCardLight
        ),
        shape = RoundedCornerShape(12.dp),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = skill.isCompleted,
                        onCheckedChange = { onToggleComplete() },
                        colors = CheckboxDefaults.colors(checkedColor = GreenAccent),
                        modifier = Modifier.testTag("checkbox_${skill.id}")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = skill.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = NavyDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (skill.targetUrl.isNotEmpty()) skill.targetUrl else "Search Web",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onStartSession() },
                        modifier = Modifier.testTag("start_session_${skill.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = "Start Study Session",
                            tint = if (isStudyingActive) GreenAccent else NavyPrimary
                        )
                    }
                    IconButton(
                        onClick = { onDelete() },
                        modifier = Modifier.testTag("delete_${skill.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete skill",
                            tint = Color.Red
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Study hours",
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Studied: ${formatTime(skill.studyTimeSeconds)}",
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Text(
                    text = if (isExpanded) "Collapse Notes ▲" else "Write Notes ▼",
                    fontSize = 11.sp,
                    color = NavyPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                    Text(
                        text = "📝 Study Notebook",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NavyPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("notes_input_${skill.id}"),
                        placeholder = { Text("Draft active bullet points, summaries, or insights here...", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyPrimary,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        textStyle = TextStyle(fontSize = 13.sp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { 
                                onSaveNotes(notesText)
                                Toast.makeText(context, "Notes Updated!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("save_notes_${skill.id}")
                        ) {
                            Text("Save Notes", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------
// 2. DISTRACTION-FREE BROWSER TAB
// ----------------------------------------
@Composable
fun BrowserTab(viewModel: BrowserViewModel) {
    val colors = LocalAppColors.current
    val NavyPrimary = colors.primary
    val NavyDark = colors.primaryDark
    val GreenAccent = colors.accent
    val SoftCream = colors.bg
    val SurfaceCardLight = colors.card

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isUrlBlocked by viewModel.isUrlBlocked.collectAsStateWithLifecycle()
    val blockedUrlAttempt by viewModel.blockedUrlAttempt.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val progress by viewModel.loadingProgress.collectAsStateWithLifecycle()
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isBookmarked by remember { mutableStateOf(false) }
    var isAiPanelExpanded by remember { mutableStateOf(false) }

    // Check if the current URL is bookmarked on change
    LaunchedEffect(currentUrl) {
        isBookmarked = viewModel.isBookmarked(currentUrl)
    }

    val isGuestMode by viewModel.isGuestMode.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        // Custom Browser Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { webViewRef?.goBack() },
                enabled = webViewRef?.canGoBack() == true,
                modifier = Modifier.size(36.dp).testTag("web_back")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Web back")
            }

            IconButton(
                onClick = { webViewRef?.goForward() },
                enabled = webViewRef?.canGoForward() == true,
                modifier = Modifier.size(36.dp).testTag("web_forward")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Web forward")
            }

            IconButton(
                onClick = { webViewRef?.reload() },
                modifier = Modifier.size(36.dp).testTag("web_reload")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Web reload")
            }

            // Brand Logo placed next to the search bar
            if (isGuestMode) {
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp, end = 2.dp)
                        .size(34.dp)
                        .background(Color(0xFFDE5833), CircleShape)
                        .testTag("brand_logo_guest"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = "Guest Mode",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp, end = 2.dp)
                        .size(34.dp)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(NavyPrimary, GreenAccent)
                            ),
                            shape = CircleShape
                        )
                        .testTag("brand_logo_normal"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Go",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            // Filled address search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchInputChanged(it) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .padding(horizontal = 4.dp)
                    .testTag("browser_address_bar"),
                placeholder = { 
                    Text(
                        text = if (isGuestMode) "Private Guest Session" else "Search or type URL", 
                        fontSize = 12.sp, 
                        color = Color.Gray
                    ) 
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isGuestMode) Color(0xFFDE5833) else GreenAccent,
                    unfocusedBorderColor = if (isGuestMode) Color(0xFFDE5833).copy(alpha = 0.5f) else Color.LightGray,
                    focusedContainerColor = if (isGuestMode) (if (colors.isDark) Color(0xFF261D1D) else Color(0xFFFFF2EF)) else SurfaceCardLight,
                    unfocusedContainerColor = if (isGuestMode) (if (colors.isDark) Color(0xFF1F1616) else Color(0xFFFFF2EF)) else SurfaceCardLight
                ),
                textStyle = TextStyle(fontSize = 12.sp),
                leadingIcon = {
                    Icon(
                        imageVector = if (isGuestMode) Icons.Default.VisibilityOff else Icons.Default.Search, 
                        contentDescription = "Search", 
                        modifier = Modifier.size(16.dp),
                        tint = if (isGuestMode) Color(0xFFDE5833) else colors.textSecondary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchInputChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear address bar", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { viewModel.performSearchOrLoad(searchQuery) }
                )
            )

            // Bookmark Toggle
            IconButton(
                onClick = {
                    viewModel.toggleBookmark("Student Page", currentUrl)
                    isBookmarked = !isBookmarked
                    val msg = if (isBookmarked) "Added to Bookmarks!" else "Removed Bookmark."
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(36.dp).testTag("web_bookmark_toggle")
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.Star,
                    tint = if (isBookmarked) Color(0xFFFFC107) else Color.Gray,
                    contentDescription = "Bookmark site"
                )
            }

            // AI Toggle Button
            IconButton(
                onClick = { isAiPanelExpanded = !isAiPanelExpanded },
                modifier = Modifier.size(36.dp).testTag("web_ai_assistant_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    tint = if (isAiPanelExpanded) GreenAccent else Color.Gray,
                    contentDescription = "Toggle Gemini AI Assistant"
                )
            }
        }

        // Loading ProgressBar
        if (isLoading) {
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = GreenAccent,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }

        // Split Layout for WebView and Collapsible AI Assistant
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                    if (isUrlBlocked) {
                        // RENDER GORGEOUS WARNING FOCUS SHIELD
                        FocusShieldScreen(
                            blockedUrl = blockedUrlAttempt,
                            viewModel = viewModel
                        )
                    } else {
                        // RENDER ACTIVE WEBVIEW
                        BrowserWebView(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize().testTag("native_webview"),
                            onWebViewCreated = { webViewRef = it }
                        )
                    }
                }
                
                // Gemini AI Assistant Collapsible Panel
                AiAssistantPanel(
                    viewModel = viewModel,
                    isExpanded = isAiPanelExpanded,
                    onToggleExpand = { isAiPanelExpanded = !isAiPanelExpanded }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantPanel(
    viewModel: com.example.viewmodel.BrowserViewModel,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val NavyPrimary = colors.primary
    val GreenAccent = colors.accent
    val SurfaceCardLight = colors.card

    val chatHistory by viewModel.aiChatHistory.collectAsStateWithLifecycle()
    val useSearch by viewModel.useGoogleSearch.collectAsStateWithLifecycle()
    val useMaps by viewModel.useGoogleMaps.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecordingAudio.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessingAi.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()
    val transcriptionStatus by viewModel.transcriptionStatus.collectAsStateWithLifecycle()
    val promptInput by viewModel.aiPromptInput.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startAudioRecording()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice input", Toast.LENGTH_SHORT).show()
        }
    }

    if (!isExpanded) {
        // Collapsed AI Assistant Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NavyPrimary)
                .clickable { onToggleExpand() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Gemini AI",
                tint = GreenAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Gemini AI Assistant (Search & Maps Grounding)",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Expand AI Panel",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    } else {
        // Expanded AI Assistant console
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .testTag("ai_assistant_panel"),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyPrimary)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Gemini AI logo",
                        tint = GreenAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Gemini 3.5 Assistant",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Clear button
                    IconButton(
                        onClick = { viewModel.clearChat() },
                        modifier = Modifier.size(28.dp).testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear Chat History",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Minimize button
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(28.dp).testTag("minimize_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimize AI Panel",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Toggles for Search & Maps Grounding
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Grounding Tools:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray,
                        modifier = Modifier.padding(end = 4.dp)
                    )

                    // Google Search Grounding Chip
                    FilterChip(
                        selected = useSearch,
                        onClick = { viewModel.setUseGoogleSearch(!useSearch) },
                        label = { Text("Google Search", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search icon",
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenAccent.copy(alpha = 0.2f),
                            selectedLabelColor = NavyPrimary,
                            selectedLeadingIconColor = NavyPrimary
                        ),
                        modifier = Modifier.height(28.dp).testTag("chip_search_grounding")
                    )

                    // Google Maps Grounding Chip
                    FilterChip(
                        selected = useMaps,
                        onClick = { viewModel.setUseGoogleMaps(!useMaps) },
                        label = { Text("Google Maps", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = "Map pin icon",
                                modifier = Modifier.size(12.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenAccent.copy(alpha = 0.2f),
                            selectedLabelColor = NavyPrimary,
                            selectedLeadingIconColor = NavyPrimary
                        ),
                        modifier = Modifier.height(28.dp).testTag("chip_maps_grounding")
                    )
                }

                Divider(color = Color.LightGray.copy(alpha = 0.5f))

                // Chat message stream or Empty state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    if (chatHistory.isEmpty()) {
                        // Welcome / Suggestions Empty State
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Companion Welcome",
                                tint = GreenAccent,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "What can I help you find today?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = NavyPrimary
                            )
                            Text(
                                text = "Ask questions with up-to-date Google Search & Maps data.",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Proactive quick suggestions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.sendChatMessage("What are the top 3 tech news stories today?") },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary.copy(alpha = 0.08f), contentColor = NavyPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(28.dp).padding(horizontal = 4.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Tech news today", fontSize = 10.sp)
                                }
                                Button(
                                    onClick = { viewModel.sendChatMessage("Find highly rated coffee shops near Seattle WA") },
                                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary.copy(alpha = 0.08f), contentColor = NavyPrimary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(28.dp).padding(horizontal = 4.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Seattle coffee shops", fontSize = 10.sp)
                                }
                            }
                        }
                    } else {
                        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                        LaunchedEffect(chatHistory.size) {
                            if (chatHistory.isNotEmpty()) {
                                listState.animateScrollToItem(chatHistory.size - 1)
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chatHistory) { msg ->
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
                                ) {
                                    Card(
                                        shape = RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (msg.isUser) 12.dp else 2.dp,
                                            bottomEnd = if (msg.isUser) 2.dp else 12.dp
                                        ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (msg.isUser) NavyPrimary else SurfaceCardLight
                                        ),
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                            Text(
                                                text = msg.text,
                                                fontSize = 12.sp,
                                                color = if (msg.isUser) Color.White else Color.Black
                                            )
                                        }
                                    }

                                    // Display grounded queries if present
                                    if (!msg.isUser && !msg.webSearchQueries.isNullOrEmpty()) {
                                        Row(
                                            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = "Search grounding",
                                                tint = GreenAccent,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Search source: \"${msg.webSearchQueries.firstOrNull()}\"",
                                                fontSize = 9.sp,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    // Display citation sources/links if present
                                    if (!msg.isUser && !msg.sourceLinks.isNullOrEmpty()) {
                                        androidx.compose.foundation.lazy.LazyRow(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp, start = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            items(msg.sourceLinks) { source ->
                                                Card(
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = CardDefaults.cardColors(containerColor = GreenAccent.copy(alpha = 0.1f)),
                                                    modifier = Modifier.clickable {
                                                        // Load the source URL in the Browser!
                                                        viewModel.performSearchOrLoad(source.second)
                                                        Toast.makeText(context, "Opening source: ${source.first}", Toast.LENGTH_SHORT).show()
                                                    }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = source.first.take(20) + (if (source.first.length > 20) "..." else ""),
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = NavyPrimary
                                                        )
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Launch,
                                                            contentDescription = "Open link",
                                                            tint = NavyPrimary,
                                                            modifier = Modifier.size(8.dp)
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
                }

                Divider(color = Color.LightGray.copy(alpha = 0.5f))

                // Error indicator
                if (aiError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFDE8E8))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = aiError ?: "",
                                color = Color.Red,
                                fontSize = 10.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.clearAiError() }, modifier = Modifier.size(16.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close error", tint = Color.Red, modifier = Modifier.size(10.dp))
                            }
                        }
                    }
                }

                // Recording or transcription progress bar
                if (transcriptionStatus != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(GreenAccent.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = transcriptionStatus ?: "",
                            color = NavyPrimary,
                            fontSize = 10.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Loading spinner
                if (isProcessing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = GreenAccent,
                        trackColor = Color.Transparent
                    )
                }

                // Input bar (text box, mic button, send button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Microphone / Speech Transcription trigger
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                viewModel.stopAudioRecordingAndTranscribe()
                            } else {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    viewModel.startAudioRecording()
                                } else {
                                    permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isRecording) Color.Red else NavyPrimary.copy(alpha = 0.1f),
                                CircleShape
                            ).testTag("mic_button")
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop voice input" else "Start voice input",
                            tint = if (isRecording) Color.White else NavyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // TextField
                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { viewModel.setAiPromptInput(it) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("ai_prompt_input"),
                        placeholder = { Text("Ask Gemini...", fontSize = 11.sp, color = Color.Gray) },
                        singleLine = true,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GreenAccent,
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        ),
                        textStyle = TextStyle(fontSize = 11.sp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Send
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onSend = {
                                if (promptInput.isNotBlank() && !isProcessing) {
                                    viewModel.sendChatMessage(promptInput)
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    IconButton(
                        onClick = {
                            if (promptInput.isNotBlank() && !isProcessing) {
                                viewModel.sendChatMessage(promptInput)
                            }
                        },
                        enabled = promptInput.isNotBlank() && !isProcessing,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (promptInput.isNotBlank() && !isProcessing) NavyPrimary else Color.LightGray,
                                CircleShape
                            ).testTag("send_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Send prompt",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BrowserWebView(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier,
    onWebViewCreated: (WebView) -> Unit
) {
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isDark = when (themeMode) {
        "Dark" -> true
        "Light" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val scope = rememberCoroutineScope()

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true // Enable JavaScript for interactive learnings
                    domStorageEnabled = true
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: return false
                        
                        // Run intercept checks synchronously via cached set
                        var isAllowed = true
                        scope.launch {
                            isAllowed = viewModel.checkUrlAllowed(url)
                        }
                        
                        return !isAllowed
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        viewModel.setBrowserLoading(true)
                        if (url != null) {
                            scope.launch {
                                viewModel.checkUrlAllowed(url)
                            }
                        }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        viewModel.setBrowserLoading(false)
                        val title = view?.title ?: ""
                        if (url != null) {
                            viewModel.recordVisit(title, url)
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        viewModel.setBrowserLoadingProgress(newProgress)
                    }
                }

                onWebViewCreated(this)
            }
        },
        update = { webView ->
            if (webView.url != currentUrl) {
                webView.loadUrl(currentUrl)
            }
            // Dynamically apply dark mode to WebView content
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                webView.settings.isAlgorithmicDarkeningAllowed = isDark
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                webView.settings.forceDark = if (isDark) {
                    android.webkit.WebSettings.FORCE_DARK_ON
                } else {
                    android.webkit.WebSettings.FORCE_DARK_OFF
                }
            }
        },
        modifier = modifier
    )
}

// ----------------------------------------
// DISTRACTION BLOCK WARNING SCREEN (Focus Shield)
// ----------------------------------------
@Composable
fun FocusShieldScreen(blockedUrl: String, viewModel: BrowserViewModel) {
    val activeSkill by viewModel.activeSkill.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF2F0))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("shield_warning_card"),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Warning Red Shield Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFFFEECEB), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Shield Guard Active",
                        tint = FocusOrange,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Focus Mode Shield Active",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = NavyDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Distracting website blocked to keep you on track!",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCardLight, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Blocked Attempt:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = blockedUrl,
                            fontSize = 12.sp,
                            color = FocusOrange,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (activeSkill != null) {
                    Text(
                        text = "🎯 You are scheduled to study:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary
                    )
                    Text(
                        text = activeSkill!!.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenAccent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = { viewModel.dismissBlockAndGoHome() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray),
                        modifier = Modifier.testTag("shield_go_home")
                    ) {
                        Text("Search Web", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { 
                            if (activeSkill != null && activeSkill!!.targetUrl.isNotEmpty()) {
                                viewModel.loadUrl(activeSkill!!.targetUrl)
                            } else {
                                viewModel.setTab(0) // Go back to learn tab
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                        modifier = Modifier.testTag("shield_return_study")
                    ) {
                        Text("Return to Study", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

// ----------------------------------------
// 3. FOCUS SHIELD (BLOCKLIST SETTINGS) TAB
// ----------------------------------------
@Composable
fun FocusShieldTab(viewModel: BrowserViewModel) {
    val colors = LocalAppColors.current
    val NavyPrimary = colors.primary
    val NavyDark = colors.primaryDark
    val GreenAccent = colors.accent
    val SoftCream = colors.bg
    val SurfaceCardLight = colors.card

    val isFocusEnabled by viewModel.isFocusModeEnabled.collectAsStateWithLifecycle()
    val blockedSites by viewModel.blockedSites.collectAsStateWithLifecycle()
    val skills by viewModel.skills.collectAsStateWithLifecycle()
    
    var newBlockInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Calculate nice stats
    val totalSkills = skills.size
    val completedSkills = skills.count { it.isCompleted }
    val totalStudySeconds = skills.sumOf { it.studyTimeSeconds }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("focus_shield_tab"),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Focus Control Room",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NavyDark
            )
            Text(
                text = "Configure built-in website blocks to stay protected from distraction hubs.",
                fontSize = 13.sp,
                color = Color.Gray
            )
        }

        // Master switch card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFocusEnabled) Color(0xFFF0FDF4) else SurfaceCardLight
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            tint = if (isFocusEnabled) GreenAccent else Color.Gray,
                            contentDescription = "Focus Shield Icon",
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Focus Blocker Shield",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = NavyDark
                            )
                            Text(
                                text = if (isFocusEnabled) "Enabled (Distracting sites blocked)" else "Disabled (Free browser access)",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Switch(
                        checked = isFocusEnabled,
                        onCheckedChange = { viewModel.toggleFocusMode() },
                        colors = SwitchDefaults.colors(checkedThumbColor = GreenAccent),
                        modifier = Modifier.testTag("master_focus_switch")
                    )
                }
            }
        }

        // Study Statistics Box
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavyDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎓 Learning Progress Metrics",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Skills Set", fontSize = 11.sp, color = Color.LightGray)
                            Text(text = "$totalSkills", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Completed", fontSize = 11.sp, color = Color.LightGray)
                            Text(text = "$completedSkills", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GreenAccent)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Total Study", fontSize = 11.sp, color = Color.LightGray)
                            Text(text = formatTime(totalStudySeconds), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Add domain to block
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardLight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🚫 Block Custom Distracting Domain",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NavyDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newBlockInput,
                            onValueChange = { newBlockInput = it },
                            placeholder = { Text("e.g. youtube.com, x.com", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("block_site_input"),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NavyPrimary,
                                unfocusedBorderColor = Color.LightGray,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            textStyle = TextStyle(fontSize = 12.sp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newBlockInput.trim().isNotEmpty()) {
                                    viewModel.addBlockedSite(newBlockInput)
                                    newBlockInput = ""
                                    Toast.makeText(context, "Domain Blocked!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("block_site_add_button")
                        ) {
                            Text("Block", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Blocked Target Hubs (${blockedSites.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = NavyDark,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (blockedSites.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No websites restricted currently.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            items(blockedSites, key = { it.id }) { site ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceCardLight, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("blocked_site_item_${site.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = site.isActive,
                            onCheckedChange = { viewModel.toggleBlockedSiteActive(site) },
                            colors = CheckboxDefaults.colors(checkedColor = FocusOrange),
                            modifier = Modifier.testTag("toggle_block_${site.id}")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = site.domain,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (site.isActive) FocusOrange else Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { viewModel.deleteBlockedSite(site) },
                        modifier = Modifier.testTag("delete_block_${site.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Remove block",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------
// 4. SYNC & DEVICES TAB
// ----------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncTab(viewModel: BrowserViewModel) {
    val colors = LocalAppColors.current
    val NavyPrimary = colors.primary
    val NavyDark = colors.primaryDark
    val GreenAccent = colors.accent
    val SoftCream = colors.bg
    val SurfaceCardLight = colors.card

    val syncKey by viewModel.syncKey.collectAsStateWithLifecycle()
    val lastSynced by viewModel.lastSyncedAt.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val themeAccent by viewModel.themeAccent.collectAsStateWithLifecycle()
    val searchEngine by viewModel.searchEngine.collectAsStateWithLifecycle()
    val shieldStrictness by viewModel.shieldStrictness.collectAsStateWithLifecycle()
    val sessionGoalMinutes by viewModel.sessionGoalMinutes.collectAsStateWithLifecycle()
    val isGuestMode by viewModel.isGuestMode.collectAsStateWithLifecycle()
    val doNotTrack by viewModel.doNotTrack.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var inputSyncKey by remember { mutableStateOf("") }
    var selectedLoginProvider by remember { mutableStateOf<String?>(null) }
    var selectedSettingsSubTab by remember { mutableStateOf(0) } // 0 = Sync & Account, 1 = Search & Privacy, 2 = Theme & Limits

    // Clear sync notifications after showing toast
    LaunchedEffect(syncState) {
        if (syncState is SyncState.Success) {
            Toast.makeText(context, (syncState as SyncState.Success).message, Toast.LENGTH_LONG).show()
            viewModel.clearSyncMessage()
        } else if (syncState is SyncState.Error) {
            Toast.makeText(context, (syncState as SyncState.Error).error, Toast.LENGTH_LONG).show()
            viewModel.clearSyncMessage()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Settings Tab Selector
        TabRow(
            selectedTabIndex = selectedSettingsSubTab,
            containerColor = SoftCream,
            contentColor = NavyPrimary
        ) {
            Tab(
                selected = selectedSettingsSubTab == 0,
                onClick = { selectedSettingsSubTab = 0 },
                text = { Text("Account & Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                selectedContentColor = NavyPrimary,
                unselectedContentColor = Color.Gray,
                modifier = Modifier.testTag("settings_subtab_account")
            )
            Tab(
                selected = selectedSettingsSubTab == 1,
                onClick = { selectedSettingsSubTab = 1 },
                text = { Text("Search & Privacy", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                selectedContentColor = NavyPrimary,
                unselectedContentColor = Color.Gray,
                modifier = Modifier.testTag("settings_subtab_privacy")
            )
            Tab(
                selected = selectedSettingsSubTab == 2,
                onClick = { selectedSettingsSubTab = 2 },
                text = { Text("Theme & Limits", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                selectedContentColor = NavyPrimary,
                unselectedContentColor = Color.Gray,
                modifier = Modifier.testTag("settings_subtab_theme")
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("sync_tab"),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = when (selectedSettingsSubTab) {
                        0 -> "Cloud Sync Security Center"
                        1 -> "Search & Privacy Shield"
                        else -> "Appearance & Focus Benchmarks"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = when (selectedSettingsSubTab) {
                        0 -> "Secure your study goals with cloud backups and direct physical pairing codes."
                        1 -> "Choose your learning engine, activate guest mode, and configure tracking preventions."
                        else -> "Make the UI your own with dynamic themes, shield configurations, and stopwatch benchmarks."
                    },
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

        // Section 1: User Security & Cloud Login Hub
        if (selectedSettingsSubTab == 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("auth_section_card"),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (userProfile != null) {
                        val profile = userProfile!!
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val providerColor = when (profile.provider) {
                                "Google" -> Color(0xFF4285F4)
                                "Microsoft" -> Color(0xFF00A4EF)
                                "DuckDuckGo" -> Color(0xFFDE5833)
                                else -> Color(0xFF24292E)
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(providerColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.email.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Color.White
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Connected via ${profile.provider}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = providerColor
                                )
                                Text(
                                    text = profile.username,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = profile.email,
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.logout() },
                                modifier = Modifier.testTag("logout_button")
                            ) {
                                Text("Sign Out", color = Color.Red, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { viewModel.forceSyncPush() },
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp).testTag("sync_now_btn")
                            ) {
                                if (syncState is SyncState.Syncing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = "Sync Now", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sync Now", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "🔐 Cloud Sync Security Center",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Link your preferred secure credentials to save your progress across all connected devices.",
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // Vertical list of providers styled cleanly
                        val providers = listOf(
                            Triple("Google", "Sign in with Google Sync", Color(0xFF4285F4)),
                            Triple("Microsoft", "Sign in with Microsoft Sync", Color(0xFF00A4EF)),
                            Triple("DuckDuckGo", "Sign in with DuckDuckGo Sync", Color(0xFFDE5833)),
                            Triple("GitHub", "Sign in with GitHub Secure", Color(0xFF24292E))
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            providers.forEach { (prov, label, pColor) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (colors.isDark) Color(0xFF262626) else Color.White,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (colors.isDark) Color(0xFF333333) else Color(0xFFE5E5E5),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { selectedLoginProvider = prov }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("login_provider_$prov"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(pColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }

        // Section 2: Browser & Custom Search Configuration
        if (selectedSettingsSubTab == 1) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("browser_config_card"),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCardLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔍 Default Search Engine",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Select your preferred learning search interface.",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val engines = listOf("Google", "DuckDuckGo", "Bing", "Brave", "Yahoo")
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(engines) { engine ->
                            val isSel = engine == searchEngine
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.setSearchEngine(engine) },
                                label = { Text(engine, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NavyPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = if (colors.isDark) Color(0xFF262626) else Color.White,
                                    labelColor = colors.textSecondary
                                ),
                                modifier = Modifier.testTag("search_engine_$engine")
                            )
                        }
                    }
                }
            }
        }

        // Section: Privacy Protection & Guest Browsing
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("privacy_settings_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardLight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "🛡️ Privacy Guard & Security Options",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    )

                    // Guest Browsing Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Anonymous Guest Browsing",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "When active, your history is completely hidden/paused, and cookies are automatically wiped on toggle.",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }
                        Switch(
                            checked = isGuestMode,
                            onCheckedChange = { 
                                viewModel.setGuestMode(it)
                                val msg = if (it) "Anonymous Guest Mode Enabled" else "Guest Mode Disabled"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = GreenAccent),
                            modifier = Modifier.testTag("guest_browsing_toggle")
                        )
                    }

                    Divider(color = if (colors.isDark) Color(0xFF333333) else Color(0xFFE5E5E5))

                    // Do Not Track Toggle Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Send 'Do Not Track' Header",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Asks search engines and web portals not to track your browsing sessions for marketing.",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }
                        Switch(
                            checked = doNotTrack,
                            onCheckedChange = { 
                                viewModel.setDoNotTrack(it)
                                val msg = if (it) "Do Not Track Requested" else "DNT Disabled"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = GreenAccent),
                            modifier = Modifier.testTag("dnt_toggle")
                        )
                    }

                    Divider(color = if (colors.isDark) Color(0xFF333333) else Color(0xFFE5E5E5))

                    // Immediately Clear History/Cache Button
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Browser Database & Cookies Maintenance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Completely purges all web page cookies, clear WebView caching buffers, and resets the student's history db log.",
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Button(
                            onClick = {
                                viewModel.clearBrowsingData()
                                Toast.makeText(context, "History, cache, and cookies successfully cleared!", Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("clear_browsing_data_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Data", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear History, Cache & Cookies", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
        }

        // Section 3: Visual Theme and Style Accent
        if (selectedSettingsSubTab == 2) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("theme_accent_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardLight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎨 Theme Accent & Colors",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Switch color mood and see the whole app adapt in real-time.",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    val accents = listOf(
                        "Navy" to Color(0xFF1E3A5F),
                        "Emerald" to Color(0xFF2E7D32),
                        "Ocean" to Color(0xFF0288D1),
                        "Cosmic" to Color(0xFF7B1FA2),
                        "Sunset" to Color(0xFFE64A19)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        accents.forEach { (name, hexColor) ->
                            val isSel = name == themeAccent
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(hexColor, CircleShape)
                                    .border(
                                        width = if (isSel) 3.dp else 1.dp,
                                        color = if (isSel) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.setThemeAccent(name) }
                                    .testTag("accent_color_$name"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "☀️ Theme Mode Preference",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val modes = listOf("System", "Light", "Dark")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        modes.forEach { m ->
                            val isSel = m == themeMode
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.setThemeMode(m) },
                                label = { Text(m, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NavyPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = if (colors.isDark) Color(0xFF262626) else Color.White,
                                    labelColor = colors.textSecondary
                                ),
                                modifier = Modifier.testTag("theme_mode_$m")
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Study Shield & Timer Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("shield_timer_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardLight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🛡️ Focus Shield Interceptor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Standard: Block custom lists. Strict: Auto-block social media and gaming dynamically during active studies.",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val strictnessModes = listOf("Standard", "Strict", "Disabled")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        strictnessModes.forEach { mode ->
                            val isSel = mode == shieldStrictness
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.setShieldStrictness(mode) },
                                label = { Text(mode, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NavyPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = if (colors.isDark) Color(0xFF262626) else Color.White,
                                    labelColor = colors.textSecondary
                                ),
                                modifier = Modifier.testTag("shield_strictness_$mode")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "⏱️ Stopwatch Pomodoro Limit",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val targets = listOf(
                        "Continuous" to 0,
                        "15 Mins" to 15,
                        "25 Mins" to 25,
                        "45 Mins" to 45,
                        "60 Mins" to 60
                    )

                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(targets) { (lbl, mins) ->
                            val isSel = mins == sessionGoalMinutes
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.setSessionGoalMinutes(mins) },
                                label = { Text(lbl, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NavyPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = if (colors.isDark) Color(0xFF262626) else Color.White,
                                    labelColor = colors.textSecondary
                                ),
                                modifier = Modifier.testTag("session_limit_$mins")
                            )
                        }
                    }
                }
            }
        }
        }

        // Section 5: Legacy Physical Devices Connection
        if (selectedSettingsSubTab == 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("legacy_sync_card"),
                colors = CardDefaults.cardColors(containerColor = SurfaceCardLight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔗 Advanced Device Pairing (No Account)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "You can also sync directly with a physical device key without logging into an account.",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (colors.isDark) Color(0xFF262626) else Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, if (colors.isDark) Color(0xFF333333) else Color.LightGray, RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = syncKey,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyPrimary,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.testTag("device_sync_key")
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(syncKey))
                                Toast.makeText(context, "Sync Key Copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp).testTag("copy_sync_key")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", tint = NavyPrimary, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputSyncKey,
                        onValueChange = { inputSyncKey = it },
                        placeholder = { Text("Enter other device sync key...", fontSize = 12.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("merge_sync_key_input"),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyPrimary,
                            unfocusedBorderColor = Color.LightGray,
                            focusedContainerColor = if (colors.isDark) Color(0xFF262626) else Color.White,
                            unfocusedContainerColor = if (colors.isDark) Color(0xFF262626) else Color.White,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        ),
                        textStyle = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (inputSyncKey.trim().isNotEmpty()) {
                                viewModel.connectAndPullSync(inputSyncKey)
                                inputSyncKey = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("pull_sync_button"),
                        enabled = syncState !is SyncState.Syncing
                    ) {
                        if (syncState is SyncState.Syncing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Default.Devices, contentDescription = "Merge devices", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pair & Link Progress", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }

        // Sync description footer
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Sync information",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Go's Cloud Sync doesn't overwrite your local notes or study hours blindly. Instead, our advanced algorithm merges active skill progress, accumulated study stopwatch times, and bookmarks intelligently across all linked devices. Secure and protected.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 16.sp
                )
            }
        }
        }
    }
    }

    // Interactive OAuth Secure Handshake Dialogue
    selectedLoginProvider?.let { provider ->
        SimulatedLoginDialog(
            provider = provider,
            onDismiss = { selectedLoginProvider = null },
            onLoginSuccess = { email ->
                val username = email.split("@")[0].replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                viewModel.loginWithProvider(provider, username, email)
                selectedLoginProvider = null
            }
        )
    }
}

@Composable
fun SimulatedLoginDialog(
    provider: String,
    onDismiss: () -> Unit,
    onLoginSuccess: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Dialog(onDismissRequest = { if (!isConnecting) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("simulated_login_dialog_${provider.lowercase()}"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val providerColor = when (provider) {
                    "Google" -> Color(0xFF4285F4)
                    "Microsoft" -> Color(0xFF00A4EF)
                    "DuckDuckGo" -> Color(0xFFDE5833)
                    else -> Color(0xFF24292E)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(providerColor, CircleShape)
                    )
                    Text(
                        text = "Sign in with $provider Secure Sync",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                }

                Text(
                    text = "Go will use $provider secure OAuth2 endpoint to store and sync your progress safely.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                if (!isConnecting) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Email Address / Username", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = { Text("username@domain.com", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = providerColor,
                                unfocusedBorderColor = Color.LightGray,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Password", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = { Text("••••••••", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = providerColor,
                                unfocusedBorderColor = Color.LightGray,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            textStyle = TextStyle(fontSize = 13.sp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { onDismiss() }) {
                            Text("Cancel", color = Color.Gray, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (email.trim().isNotEmpty()) {
                                    isConnecting = true
                                    scope.launch {
                                        statusText = "Authenticating secure handshake..."
                                        kotlinx.coroutines.delay(1000)
                                        statusText = "Fetching private sync vault certificates..."
                                        kotlinx.coroutines.delay(1000)
                                        statusText = "Syncing devices metadata..."
                                        kotlinx.coroutines.delay(800)
                                        onLoginSuccess(email.trim())
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = providerColor),
                            enabled = email.trim().isNotEmpty() && password.isNotEmpty(),
                            modifier = Modifier.testTag("auth_submit_btn")
                        ) {
                            Text("Secure Connect", color = Color.White, fontSize = 13.sp)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = providerColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = statusText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------
// MODALS AND DIALOGS
// ----------------------------------------
@Composable
fun AddSkillDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("add_skill_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Define New Skill Goal",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyDark
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Skill Title / Goal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("e.g. Learn Kotlin, UI Design", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("add_skill_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyPrimary,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Target Study Website (Optional)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = { Text("e.g. developer.android.com", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().testTag("add_skill_url_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NavyPrimary,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.sp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.testTag("add_skill_cancel")
                    ) {
                        Text("Cancel", color = Color.Gray, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            if (name.trim().isNotEmpty()) {
                                onAdd(name, url)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.testTag("add_skill_submit"),
                        enabled = name.trim().isNotEmpty()
                    ) {
                        Text("Add Skill", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// Helper formatting utilities
fun formatTime(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) {
        String.format("%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

fun formatLastSynced(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    val diff = System.currentTimeMillis() - timestamp
    val mins = diff / 60000
    return when {
        mins < 1 -> "Just now"
        mins < 60 -> "$mins min ago"
        else -> "${mins / 60} hour(s) ago"
    }
}

@Composable
fun PomodoroTimerCard(viewModel: BrowserViewModel) {
    val colors = LocalAppColors.current
    val NavyPrimary = colors.primary
    val NavyDark = colors.primaryDark
    val GreenAccent = colors.accent
    val SurfaceCardLight = colors.card

    val pomoState by viewModel.pomodoroState.collectAsStateWithLifecycle()
    val pomoMode by viewModel.pomodoroMode.collectAsStateWithLifecycle()
    val pomoTimeLeft by viewModel.pomodoroTimeLeft.collectAsStateWithLifecycle()
    val pomoDuration by viewModel.pomodoroDuration.collectAsStateWithLifecycle()
    val completedPomos by viewModel.completedPomodoros.collectAsStateWithLifecycle()

    val workMins by viewModel.pomodoroWorkMinutes.collectAsStateWithLifecycle()
    val shortBreakMins by viewModel.pomodoroShortBreakMinutes.collectAsStateWithLifecycle()
    val longBreakMins by viewModel.pomodoroLongBreakMinutes.collectAsStateWithLifecycle()

    var showSettings by remember { mutableStateOf(false) }

    var inputWorkMins by remember(workMins) { mutableIntStateOf(workMins) }
    var inputShortMins by remember(shortBreakMins) { mutableIntStateOf(shortBreakMins) }
    var inputLongMins by remember(longBreakMins) { mutableIntStateOf(longBreakMins) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pomodoro_timer_card"),
        colors = CardDefaults.cardColors(containerColor = SurfaceCardLight),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val (modeLabel, modeColor, modeIcon) = when (pomoMode) {
                    "WORK" -> Triple("Deep Focus Work", Color(0xFFE0533C), Icons.Default.Timer)
                    "SHORT_BREAK" -> Triple("Short Coffee Break", GreenAccent, Icons.Default.LocalCafe)
                    "LONG_BREAK" -> Triple("Long Relaxing Break", NavyPrimary, Icons.Default.Spa)
                    else -> Triple("Focus", Color(0xFFE0533C), Icons.Default.Timer)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = modeIcon,
                        contentDescription = modeLabel,
                        tint = modeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = modeLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = colors.textPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Sessions:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary
                    )
                    Box(
                        modifier = Modifier
                            .background(NavyPrimary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = completedPomos.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = NavyPrimary
                        )
                    }
                    if (completedPomos in 1..4) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(completedPomos) {
                                Text("🍅", fontSize = 11.sp)
                            }
                        }
                    } else if (completedPomos > 4) {
                        Text("🍅 x$completedPomos", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = formatTime(pomoTimeLeft.toLong()),
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = when (pomoMode) {
                    "WORK" -> Color(0xFFE0533C)
                    else -> GreenAccent
                },
                modifier = Modifier.testTag("pomodoro_digital_clock")
            )

            Spacer(modifier = Modifier.height(10.dp))

            val progressPercent = if (pomoDuration > 0) pomoTimeLeft.toFloat() / pomoDuration.toFloat() else 1f
            LinearProgressIndicator(
                progress = progressPercent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when (pomoMode) {
                    "WORK" -> Color(0xFFE0533C)
                    else -> GreenAccent
                },
                trackColor = if (colors.isDark) Color(0xFF333333) else Color(0xFFE5E5E5)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.resetPomodoro() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("pomodoro_reset_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Timer",
                        tint = colors.textSecondary
                    )
                }

                Button(
                    onClick = {
                        if (pomoState == "RUNNING") {
                            viewModel.pausePomodoro()
                        } else {
                            viewModel.startPomodoro()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (pomoMode) {
                            "WORK" -> Color(0xFFE0533C)
                            else -> GreenAccent
                        }
                    ),
                    shape = CircleShape,
                    modifier = Modifier
                        .height(48.dp)
                        .widthIn(min = 120.dp)
                        .testTag("pomodoro_play_pause_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (pomoState == "RUNNING") Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (pomoState == "RUNNING") "Pause" else "Start",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (pomoState == "RUNNING") "Pause" else "Focus Now",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.skipPomodoro() },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("pomodoro_skip_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Skip Session",
                        tint = colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val modesList = listOf("WORK" to "Work", "SHORT_BREAK" to "Short", "LONG_BREAK" to "Long")
                    modesList.forEach { (m, lbl) ->
                        val isSel = pomoMode == m
                        val activeColor = when (m) {
                            "WORK" -> Color(0xFFE0533C)
                            else -> GreenAccent
                        }
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSel) activeColor else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(
                                    width = if (isSel) 0.dp else 1.dp,
                                    color = if (colors.isDark) Color(0xFF333333) else Color.LightGray,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.setPomodoroModeDirectly(m) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("pomo_mode_chip_$m")
                        ) {
                            Text(
                                text = lbl,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSel) Color.White else colors.textSecondary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showSettings = !showSettings },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("pomodoro_settings_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configure Pomodoro Intervals",
                        tint = if (showSettings) NavyPrimary else colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            AnimatedVisibility(visible = showSettings) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(
                            if (colors.isDark) Color(0xFF222222) else Color.White,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (colors.isDark) Color(0xFF333333) else Color(0xFFE5E5E5),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Customize Focus Intervals",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = colors.textPrimary
                    )

                    DurationAdjuster(
                        label = "Focus Work Block:",
                        value = inputWorkMins,
                        onValueChange = { inputWorkMins = it },
                        range = 1..60,
                        tagPrefix = "pomo_cfg_work"
                    )

                    DurationAdjuster(
                        label = "Short Coffee Break:",
                        value = inputShortMins,
                        onValueChange = { inputShortMins = it },
                        range = 1..30,
                        tagPrefix = "pomo_cfg_short"
                    )

                    DurationAdjuster(
                        label = "Long Relaxing Break:",
                        value = inputLongMins,
                        onValueChange = { inputLongMins = it },
                        range = 1..45,
                        tagPrefix = "pomo_cfg_long"
                    )

                    Button(
                        onClick = {
                            viewModel.updatePomodoroSettings(inputWorkMins, inputShortMins, inputLongMins)
                            showSettings = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .testTag("pomodoro_save_settings_btn"),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Save & Apply New Intervals", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun DurationAdjuster(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    tagPrefix: String
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 11.sp, color = colors.textSecondary)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { if (value > range.first) onValueChange(value - 1) },
                modifier = Modifier
                    .size(24.dp)
                    .testTag("${tagPrefix}_minus")
            ) {
                Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }

            Text(
                text = "$value mins",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = colors.textPrimary,
                modifier = Modifier.widthIn(min = 44.dp),
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = { if (value < range.last) onValueChange(value + 1) },
                modifier = Modifier
                    .size(24.dp)
                    .testTag("${tagPrefix}_plus")
            ) {
                Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
            }
        }
    }
}
