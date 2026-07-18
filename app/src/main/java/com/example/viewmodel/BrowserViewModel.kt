package com.example.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.BlockedSite
import com.example.data.database.Skill
import com.example.data.database.WebHistory
import com.example.data.repository.ProgressRepository
import com.example.data.sync.CloudSyncService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.utils.AudioRecorder
import com.example.data.api.GeminiServiceClient

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timeMillis: Long = System.currentTimeMillis(),
    val webSearchQueries: List<String>? = null,
    val sourceLinks: List<Pair<String, String>>? = null // Title -> URL
)

data class UserProfile(
    val provider: String, // Google, Microsoft, DuckDuckGo, GitHub
    val username: String,
    val email: String,
    val avatarUrl: String? = null,
    val isLoggedIn: Boolean = false
)

data class Course(
    val id: String,
    val title: String,
    val provider: String,
    val description: String,
    val url: String,
    val duration: String,
    val category: String,
    val difficulty: String,
    val rating: Float,
    val isEnrolled: Boolean = false
)

sealed interface SyncState {
    object Idle : SyncState
    object Syncing : SyncState
    data class Success(val message: String) : SyncState
    data class Error(val error: String) : SyncState
}

class BrowserViewModel(
    private val context: Context,
    private val repository: ProgressRepository,
    private val syncService: CloudSyncService
) : ViewModel() {

    private val prefs: SharedPreferences = context.getSharedPreferences("go_browser_prefs", Context.MODE_PRIVATE)

    // Sync Key & Focus State stored in SharedPreferences
    private val _syncKey = MutableStateFlow("")
    val syncKey: StateFlow<String> = _syncKey.asStateFlow()

    private val _isFocusModeEnabled = MutableStateFlow(true)
    val isFocusModeEnabled: StateFlow<Boolean> = _isFocusModeEnabled.asStateFlow()

    // NEW customizable settings flows
    private val _themeMode = MutableStateFlow("System")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _themeAccent = MutableStateFlow("Navy")
    val themeAccent: StateFlow<String> = _themeAccent.asStateFlow()

    private val _searchEngine = MutableStateFlow("Google")
    val searchEngine: StateFlow<String> = _searchEngine.asStateFlow()

    private val _shieldStrictness = MutableStateFlow("Standard")
    val shieldStrictness: StateFlow<String> = _shieldStrictness.asStateFlow()

    private val _sessionGoalMinutes = MutableStateFlow(0)
    val sessionGoalMinutes: StateFlow<Int> = _sessionGoalMinutes.asStateFlow()

    // NEW Guest Browsing & Privacy Protection states
    private val _isGuestMode = MutableStateFlow(false)
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    private val _doNotTrack = MutableStateFlow(true)
    val doNotTrack: StateFlow<Boolean> = _doNotTrack.asStateFlow()

    // User Profile Authentication Simulator
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    // Free Courses State
    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> = _courses.asStateFlow()

    private val _isLoadingCourses = MutableStateFlow(false)
    val isLoadingCourses: StateFlow<Boolean> = _isLoadingCourses.asStateFlow()

    private val _courseCategory = MutableStateFlow("All")
    val courseCategory: StateFlow<String> = _courseCategory.asStateFlow()

    private val _courseSearchQuery = MutableStateFlow("")
    val courseSearchQuery: StateFlow<String> = _courseSearchQuery.asStateFlow()

    // UI Navigation Tab (0: Learn, 1: Browser, 2: Focus Shield, 3: Sync)
    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Active Study State
    private val _activeSkill = MutableStateFlow<Skill?>(null)
    val activeSkill: StateFlow<Skill?> = _activeSkill.asStateFlow()

    private val _sessionTimeSeconds = MutableStateFlow(0L)
    val sessionTimeSeconds: StateFlow<Long> = _sessionTimeSeconds.asStateFlow()

    private var timerJob: Job? = null

    // --- POMODORO TIMER STATE ---
    private val _pomodoroState = MutableStateFlow("IDLE") // IDLE, RUNNING, PAUSED
    val pomodoroState: StateFlow<String> = _pomodoroState.asStateFlow()

    private val _pomodoroMode = MutableStateFlow("WORK") // WORK, SHORT_BREAK, LONG_BREAK
    val pomodoroMode: StateFlow<String> = _pomodoroMode.asStateFlow()

    private val _pomodoroTimeLeft = MutableStateFlow(1500) // seconds
    val pomodoroTimeLeft: StateFlow<Int> = _pomodoroTimeLeft.asStateFlow()

    private val _pomodoroDuration = MutableStateFlow(1500) // seconds total for progress bar
    val pomodoroDuration: StateFlow<Int> = _pomodoroDuration.asStateFlow()

    private val _completedPomodoros = MutableStateFlow(0)
    val completedPomodoros: StateFlow<Int> = _completedPomodoros.asStateFlow()

    private val _pomodoroWorkMinutes = MutableStateFlow(25)
    val pomodoroWorkMinutes: StateFlow<Int> = _pomodoroWorkMinutes.asStateFlow()

    private val _pomodoroShortBreakMinutes = MutableStateFlow(5)
    val pomodoroShortBreakMinutes: StateFlow<Int> = _pomodoroShortBreakMinutes.asStateFlow()

    private val _pomodoroLongBreakMinutes = MutableStateFlow(15)
    val pomodoroLongBreakMinutes: StateFlow<Int> = _pomodoroLongBreakMinutes.asStateFlow()

    private var pomodoroJob: Job? = null

    // Browser State
    private val _currentUrl = MutableStateFlow("https://google.com")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0)
    val loadingProgress: StateFlow<Int> = _loadingProgress.asStateFlow()

    private val _isUrlBlocked = MutableStateFlow(false)
    val isUrlBlocked: StateFlow<Boolean> = _isUrlBlocked.asStateFlow()

    private val _blockedUrlAttempt = MutableStateFlow("")
    val blockedUrlAttempt: StateFlow<String> = _blockedUrlAttempt.asStateFlow()

    // Sync state for UI feedback
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncedAt = MutableStateFlow(0L)
    val lastSyncedAt: StateFlow<Long> = _lastSyncedAt.asStateFlow()

    // Database flows
    val skills: StateFlow<List<Skill>> = repository.allSkills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<WebHistory>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarks: StateFlow<List<WebHistory>> = repository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedSites: StateFlow<List<BlockedSite>> = repository.blockedSites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var cachedBlockedSites = setOf<String>()

    init {
        // Load initial SharedPreferences
        _isFocusModeEnabled.value = prefs.getBoolean("focus_mode_enabled", true)
        _lastSyncedAt.value = prefs.getLong("last_synced_at", 0L)
        
        _themeMode.value = prefs.getString("theme_mode", "System") ?: "System"
        _themeAccent.value = prefs.getString("theme_accent", "Navy") ?: "Navy"
        _searchEngine.value = prefs.getString("search_engine", "Google") ?: "Google"
        _shieldStrictness.value = prefs.getString("shield_strictness", "Standard") ?: "Standard"
        _sessionGoalMinutes.value = prefs.getInt("session_goal_minutes", 0)
        _doNotTrack.value = prefs.getBoolean("do_not_track", true)
        _isGuestMode.value = false // Guest mode always resets on fresh launch for privacy safety

        // Load simulator user profile if it exists
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        if (isLoggedIn) {
            val provider = prefs.getString("login_provider", "Google") ?: "Google"
            val username = prefs.getString("login_username", "Student Account") ?: "Student Account"
            val email = prefs.getString("login_email", "student@go-study.com") ?: "student@go-study.com"
            _userProfile.value = UserProfile(provider, username, email, isLoggedIn = true)
        }

        var savedKey = prefs.getString("sync_key", "") ?: ""
        if (savedKey.isEmpty()) {
            savedKey = syncService.generateSyncKey()
            prefs.edit().putString("sync_key", savedKey).apply()
        }
        _syncKey.value = savedKey

        viewModelScope.launch {
            // Populate databases with standard initial configurations on first boot
            repository.checkAndPopulateDefaults()
        }

        // Cache active blocked domains synchronously for fast, non-blocking UI intercept checks
        viewModelScope.launch {
            blockedSites.collect { list ->
                cachedBlockedSites = list.filter { it.isActive }.map { it.domain.lowercase().trim() }.toSet()
            }
        }

        // Initialize free course listing
        loadFreeCourses()

        // Load Pomodoro Settings and State
        _pomodoroWorkMinutes.value = prefs.getInt("pomo_work_minutes", 25)
        _pomodoroShortBreakMinutes.value = prefs.getInt("pomo_short_break_minutes", 5)
        _pomodoroLongBreakMinutes.value = prefs.getInt("pomo_long_break_minutes", 15)

        val savedPomoState = prefs.getString("pomo_state", "IDLE") ?: "IDLE"
        val savedPomoMode = prefs.getString("pomo_mode", "WORK") ?: "WORK"
        val savedPomoTimeLeft = prefs.getInt("pomo_time_left", 25 * 60)
        val savedPomoDuration = prefs.getInt("pomo_duration", 25 * 60)
        val savedCompletedPomos = prefs.getInt("pomo_completed", 0)

        _pomodoroMode.value = savedPomoMode
        _pomodoroDuration.value = savedPomoDuration
        _completedPomodoros.value = savedCompletedPomos

        if (savedPomoState == "RUNNING") {
            val lastActive = prefs.getLong("pomo_last_active_time", 0L)
            val currentTime = System.currentTimeMillis()
            if (lastActive > 0L) {
                val elapsedSeconds = ((currentTime - lastActive) / 1000).toInt()
                if (elapsedSeconds < savedPomoTimeLeft) {
                    _pomodoroTimeLeft.value = savedPomoTimeLeft - elapsedSeconds
                    startPomodoro()
                } else {
                    _pomodoroTimeLeft.value = 0
                    _pomodoroState.value = "IDLE"
                    handlePomoCompletion(savedPomoMode)
                }
            } else {
                _pomodoroTimeLeft.value = savedPomoTimeLeft
                _pomodoroState.value = "PAUSED"
            }
        } else {
            _pomodoroTimeLeft.value = savedPomoTimeLeft
            _pomodoroState.value = savedPomoState
        }
    }

    // Navigation and Tab management
    fun setTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    // Toggle Focus Mode
    fun toggleFocusMode() {
        val newValue = !_isFocusModeEnabled.value
        _isFocusModeEnabled.value = newValue
        prefs.edit().putBoolean("focus_mode_enabled", newValue).apply()
    }

    // Timer and active study session
    fun startStudySession(skill: Skill) {
        // If a session is already active, stop it first
        stopStudySession()

        _activeSkill.value = skill
        _sessionTimeSeconds.value = 0L

        // Open browser and load URL
        val targetUrl = if (skill.targetUrl.isNotEmpty()) skill.targetUrl else "https://google.com"
        loadUrl(targetUrl)
        setTab(1) // Navigate to Browser Tab

        // Start Stopwatch timer
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _sessionTimeSeconds.value += 1
                
                // Periodically save to DB to avoid loss of tracked progress
                if (_sessionTimeSeconds.value % 5 == 0L) {
                    saveAccumulatedTime()
                }
            }
        }
    }

    fun stopStudySession() {
        timerJob?.cancel()
        timerJob = null

        val currentActive = _activeSkill.value
        val accumulated = _sessionTimeSeconds.value

        if (currentActive != null && accumulated > 0) {
            viewModelScope.launch {
                val latestSkill = repository.getSkillById(currentActive.id)
                if (latestSkill != null) {
                    val updated = latestSkill.copy(
                        studyTimeSeconds = latestSkill.studyTimeSeconds + accumulated,
                        lastStudiedAt = System.currentTimeMillis()
                    )
                    repository.updateSkill(updated)
                }
            }
        }

        _activeSkill.value = null
        _sessionTimeSeconds.value = 0L
    }

    private suspend fun saveAccumulatedTime() {
        val currentActive = _activeSkill.value
        val accumulated = _sessionTimeSeconds.value
        if (currentActive != null && accumulated > 0) {
            val latestSkill = repository.getSkillById(currentActive.id)
            if (latestSkill != null) {
                val updated = latestSkill.copy(
                    studyTimeSeconds = latestSkill.studyTimeSeconds + accumulated,
                    lastStudiedAt = System.currentTimeMillis()
                )
                repository.updateSkill(updated)
                // update current active reference as well to reflect in real-time UI
                _activeSkill.value = updated
                _sessionTimeSeconds.value = 0L // reset and start recording next block
            }
        }
    }

    // Skill management
    fun addSkill(name: String, targetUrl: String) {
        viewModelScope.launch {
            repository.insertSkill(
                Skill(
                    name = name,
                    targetUrl = formatUrl(targetUrl),
                    createdAt = System.currentTimeMillis(),
                    lastStudiedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateSkillNotes(skill: Skill, newNotes: String) {
        viewModelScope.launch {
            repository.updateSkill(skill.copy(notes = newNotes))
            // Update active skill notes in-realtime if studying it
            if (_activeSkill.value?.id == skill.id) {
                _activeSkill.value = _activeSkill.value?.copy(notes = newNotes)
            }
        }
    }

    fun toggleSkillCompleted(skill: Skill) {
        viewModelScope.launch {
            repository.updateSkill(skill.copy(isCompleted = !skill.isCompleted))
        }
    }

    fun deleteSkill(skill: Skill) {
        viewModelScope.launch {
            if (_activeSkill.value?.id == skill.id) {
                stopStudySession()
            }
            repository.deleteSkill(skill)
        }
    }

    // Blocklist management
    fun addBlockedSite(domain: String) {
        viewModelScope.launch {
            val cleanDomain = domain.lowercase()
                .replace("https://", "")
                .replace("http://", "")
                .replace("www.", "")
                .split("/")[0]
                .trim()
            if (cleanDomain.isNotEmpty()) {
                repository.insertBlockedSite(BlockedSite(domain = cleanDomain, isActive = true))
            }
        }
    }

    fun toggleBlockedSiteActive(site: BlockedSite) {
        viewModelScope.launch {
            repository.updateBlockedSite(site.copy(isActive = !site.isActive))
        }
    }

    fun deleteBlockedSite(site: BlockedSite) {
        viewModelScope.launch {
            repository.deleteBlockedSite(site)
        }
    }

    // Browser Actions
    fun loadUrl(url: String) {
        val formatted = formatUrl(url)
        _searchQuery.value = formatted
        _currentUrl.value = formatted
        _isUrlBlocked.value = false
    }

    fun onSearchInputChanged(query: String) {
        _searchQuery.value = query
    }

    fun performSearchOrLoad(input: String) {
        if (input.trim().isEmpty()) return
        
        val url = if (input.contains(".") && !input.contains(" ")) {
            formatUrl(input)
        } else {
            val encodedQuery = try {
                java.net.URLEncoder.encode(input, "UTF-8")
            } catch (e: Exception) {
                input.replace(" ", "+")
            }
            when (_searchEngine.value) {
                "DuckDuckGo" -> "https://duckduckgo.com/?q=$encodedQuery"
                "Bing" -> "https://www.bing.com/search?q=$encodedQuery"
                "Brave" -> "https://search.brave.com/search?q=$encodedQuery"
                "Yahoo" -> "https://search.yahoo.com/search?p=$encodedQuery"
                else -> "https://www.google.com/search?q=$encodedQuery"
            }
        }
        loadUrl(url)
    }

    fun setBrowserLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setBrowserLoadingProgress(progress: Int) {
        _loadingProgress.value = progress
    }

    fun recordVisit(title: String, url: String) {
        if (_isGuestMode.value) return // Completely bypass history recording during Guest Browsing!
        viewModelScope.launch {
            if (url.startsWith("http")) {
                repository.addHistory(title, url)
            }
        }
    }

    fun toggleBookmark(title: String, url: String) {
        viewModelScope.launch {
            repository.toggleBookmark(title, url)
        }
    }

    fun deleteBookmarkByUrl(url: String) {
        viewModelScope.launch {
            repository.deleteBookmark(url)
        }
    }

    suspend fun isBookmarked(url: String): Boolean {
        return repository.isBookmarked(url)
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Settings modifiers
    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun setThemeAccent(accent: String) {
        _themeAccent.value = accent
        prefs.edit().putString("theme_accent", accent).apply()
    }

    fun setSearchEngine(engine: String) {
        _searchEngine.value = engine
        prefs.edit().putString("search_engine", engine).apply()
    }

    fun setShieldStrictness(strictness: String) {
        _shieldStrictness.value = strictness
        prefs.edit().putString("shield_strictness", strictness).apply()
    }

    fun setSessionGoalMinutes(minutes: Int) {
        _sessionGoalMinutes.value = minutes
        prefs.edit().putInt("session_goal_minutes", minutes).apply()
    }

    fun setGuestMode(enabled: Boolean) {
        _isGuestMode.value = enabled
        if (enabled) {
            // Instantly remove cookies for private browsing start
            try {
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setDoNotTrack(enabled: Boolean) {
        _doNotTrack.value = enabled
        prefs.edit().putBoolean("do_not_track", enabled).apply()
    }

    fun clearBrowsingData() {
        viewModelScope.launch {
            repository.clearHistory()
            try {
                android.webkit.CookieManager.getInstance().removeAllCookies(null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _currentUrl.value = "about:blank"
        }
    }

    // User authentication simulation
    fun loginWithProvider(provider: String, username: String, email: String) {
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("login_provider", provider)
            .putString("login_username", username)
            .putString("login_email", email)
            .apply()
        _userProfile.value = UserProfile(provider, username, email, isLoggedIn = true)
    }

    fun logout() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .remove("login_provider")
            .remove("login_username")
            .remove("login_email")
            .apply()
        _userProfile.value = null
    }

    // Simulated Courses Catalog API Engine
    fun loadFreeCourses() {
        _isLoadingCourses.value = true
        viewModelScope.launch {
            delay(800) // Simulated API load delay
            val list = listOf(
                Course(
                    id = "c1",
                    title = "Android Basics with Compose",
                    provider = "Google Developers",
                    description = "Learn the fundamentals of building Android apps with Jetpack Compose, the modern toolkit for creating native UI. Highly recommended for complete beginners.",
                    url = "https://developer.android.com/courses/android-basics-compose/course",
                    duration = "12 hours",
                    category = "Android & Kotlin",
                    difficulty = "Beginner",
                    rating = 4.9f
                ),
                Course(
                    id = "c2",
                    title = "Kotlin BootCamp for Programmers",
                    provider = "Google on Udacity",
                    description = "Master the modern Kotlin programming language. Understand null safety, object-oriented principles, functional extensions, and coroutines.",
                    url = "https://developer.android.com/courses/kotlin-bootcamp/course",
                    duration = "8 hours",
                    category = "Android & Kotlin",
                    difficulty = "Intermediate",
                    rating = 4.8f
                ),
                Course(
                    id = "c3",
                    title = "Responsive Web Design Certification",
                    provider = "FreeCodeCamp",
                    description = "Learn HTML5 and CSS3 by building projects ranging from a tribute page to a fully functioning CSS Grid portfolio. 100% free and certificate-backed.",
                    url = "https://www.freecodecamp.org/learn/2022/responsive-web-design/",
                    duration = "300 hours",
                    category = "Web Dev",
                    difficulty = "Beginner",
                    rating = 4.9f
                ),
                Course(
                    id = "c4",
                    title = "JavaScript Algorithms and Data Structures",
                    provider = "FreeCodeCamp",
                    description = "Dive into fundamental programming concepts including variables, loops, objects, arrays, and standard data structures. Covers OOP and functional paradigms.",
                    url = "https://www.freecodecamp.org/learn/javascript-algorithms-and-data-structures/",
                    duration = "300 hours",
                    category = "Web Dev",
                    difficulty = "Beginner",
                    rating = 4.7f
                ),
                Course(
                    id = "c5",
                    title = "CS50: Intro to Computer Science",
                    provider = "Harvard University",
                    description = "An entry-level course teaching students how to think algorithmically and solve problems efficiently. Covers C, Python, SQL, HTML, CSS, and JS.",
                    url = "https://pll.harvard.edu/course/cs50-introduction-computer-science",
                    duration = "12 weeks",
                    category = "Web Dev",
                    difficulty = "Beginner",
                    rating = 4.9f
                ),
                Course(
                    id = "c6",
                    title = "Prompt Engineering for ChatGPT",
                    provider = "Vanderbilt University on Coursera",
                    description = "Learn to tap into the massive potential of Large Language Models. Master standard prompt templates, zero-shot, few-shot, and chains of thought.",
                    url = "https://www.coursera.org/learn/prompt-engineering",
                    duration = "6 hours",
                    category = "AI & Data Science",
                    difficulty = "Beginner",
                    rating = 4.8f
                ),
                Course(
                    id = "c7",
                    title = "Google UX Design Professional Certificate",
                    provider = "Google on Coursera",
                    description = "Design user interfaces and user experiences. Learn standard digital design thinking, wireframing, high-fidelity prototypes, and Figma tools.",
                    url = "https://www.coursera.org/professional-certificates/google-ux-design",
                    duration = "6 months",
                    category = "Design",
                    difficulty = "Beginner",
                    rating = 4.8f
                ),
                Course(
                    id = "c8",
                    title = "Introduction to Generative AI Course",
                    provider = "Google Cloud",
                    description = "Understand what Generative AI is, how it is used, and how it differs from traditional machine learning methods. Covers large language models.",
                    url = "https://www.cloudskillsboost.google/course_templates/536",
                    duration = "2 hours",
                    category = "AI & Data Science",
                    difficulty = "Beginner",
                    rating = 4.6f
                ),
                Course(
                    id = "c9",
                    title = "Material Design 3 (M3) System Guide",
                    provider = "Material Design Group",
                    description = "Explore Google's latest open-source design system. Master dynamic color schemes, token structures, responsive grids, and standard components.",
                    url = "https://m3.material.io/",
                    duration = "5 hours",
                    category = "Design",
                    difficulty = "Intermediate",
                    rating = 4.9f
                ),
                Course(
                    id = "c10",
                    title = "Gemini API SDK Quickstart Guide",
                    provider = "Google AI Studio Developers",
                    description = "Master native Gemini integration inside modern Android applications. Learn structured prompt generation, JSON output constraints, and multimodal API inputs.",
                    url = "https://ai.google.dev/gemini-api/docs/quickstart",
                    duration = "1.5 hours",
                    category = "AI & Data Science",
                    difficulty = "Beginner",
                    rating = 4.9f
                ),
                Course(
                    id = "c11",
                    title = "ChatGPT Prompt Engineering Course",
                    provider = "DeepLearning.AI",
                    description = "Master the art of crafting system context prompts, zero-shot/few-shot templates, role assignments, and standard chain-of-thought paradigms.",
                    url = "https://www.deeplearning.ai/short-courses/chatgpt-prompt-engineering-for-developers/",
                    duration = "2 hours",
                    category = "AI & Data Science",
                    difficulty = "Intermediate",
                    rating = 4.8f
                ),
                Course(
                    id = "c12",
                    title = "Jetpack Compose State & Navigation Hub",
                    provider = "Android Developer Academy",
                    description = "Deep dive into Compose state hoisting, composition local scoping, rememberSaveable lifecycles, and type-safe navigation routing.",
                    url = "https://developer.android.com/codelabs/jetpack-compose-state",
                    duration = "3.5 hours",
                    category = "Android & Kotlin",
                    difficulty = "Intermediate",
                    rating = 4.9f
                ),
                Course(
                    id = "c13",
                    title = "Web Security Fundamentals (MDN Docs)",
                    provider = "Mozilla Developer Network",
                    description = "Understand web sandbox architectures, Content Security Policy (CSP) configurations, Cross-Origin Resource Sharing (CORS), and tracking mitigations.",
                    url = "https://developer.mozilla.org/en-US/docs/Web/Security",
                    duration = "4 hours",
                    category = "Web Dev",
                    difficulty = "Intermediate",
                    rating = 4.7f
                ),
                Course(
                    id = "c14",
                    title = "Figma UI/UX Layout & Grid Systems",
                    provider = "Figma Design Academy",
                    description = "Master professional prototyping, atomic component structures, auto-layout variants, constraints, and standard screen grid configurations.",
                    url = "https://www.figma.com/resource-library/design-basics/",
                    duration = "3 hours",
                    category = "Design",
                    difficulty = "Beginner",
                    rating = 4.8f
                )
            )
            _courses.value = list
            _isLoadingCourses.value = false
        }
    }

    fun setCourseCategory(category: String) {
        _courseCategory.value = category
    }

    fun setCourseSearchQuery(query: String) {
        _courseSearchQuery.value = query
    }

    fun enrollInCourse(course: Course) {
        viewModelScope.launch {
            val isAlreadyEnrolled = skills.value.any { it.name == course.title }
            if (!isAlreadyEnrolled) {
                addSkill(course.title, course.url)
            }
            setTab(0) // Go back to learn dashboard to see added course skill
        }
    }

    val filteredCourses: StateFlow<List<Course>> = combine(
        _courses, _courseCategory, _courseSearchQuery, skills
    ) { allCourses, category, search, activeSkills ->
        allCourses.filter { course ->
            val matchesCategory = category == "All" || course.category == category
            val matchesSearch = search.trim().isEmpty() || 
                    course.title.lowercase().contains(search.lowercase()) ||
                    course.description.lowercase().contains(search.lowercase()) ||
                    course.provider.lowercase().contains(search.lowercase())
            matchesCategory && matchesSearch
        }.map { course ->
            val isEnrolled = activeSkills.any { it.name == course.title }
            course.copy(isEnrolled = isEnrolled)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Intercepts loading URLs inside WebView.
     * Checks against the student blocklist when Focus Mode is active.
     */
    suspend fun checkUrlAllowed(url: String): Boolean {
        val strictness = _shieldStrictness.value
        if (strictness == "Disabled") return true
        if (!_isFocusModeEnabled.value) return true

        val host = try {
            val uri = android.net.Uri.parse(url)
            uri.host?.lowercase() ?: ""
        } catch (e: Exception) {
            ""
        }

        if (host.isEmpty()) return true

        // Strict mode additional checks: blocks social media, gaming, video streaming
        if (strictness == "Strict") {
            val lowerUrl = url.lowercase()
            val blacklistedKeywords = listOf(
                "facebook.com", "twitter.com", "x.com", "instagram.com", "tiktok.com",
                "reddit.com", "youtube.com", "netflix.com", "twitch.tv", "twitch.com",
                "pinterest.com", "gaming", "discord.com"
            )
            for (keyword in blacklistedKeywords) {
                if (lowerUrl.contains(keyword)) {
                    withContext(Dispatchers.Main) {
                        _blockedUrlAttempt.value = url
                        _isUrlBlocked.value = true
                    }
                    return false
                }
            }
        }

        val blockedDomains = cachedBlockedSites
        for (blocked in blockedDomains) {
            if (host == blocked || host.endsWith(".$blocked")) {
                withContext(Dispatchers.Main) {
                    _blockedUrlAttempt.value = url
                    _isUrlBlocked.value = true
                    // Pause active session stopwatch during distraction interception
                    // to ensure study stats remain authentic!
                    if (timerJob != null) {
                        Log.d("BrowserViewModel", "Focus Intercept: Paused stopwatch session")
                    }
                }
                return false
            }
        }
        return true
    }

    fun dismissBlockAndGoHome() {
        _isUrlBlocked.value = false
        loadUrl("https://google.com")
    }

    // Cloud Sync Core Engine
    fun forceSyncPush() {
        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SyncState.Syncing
            // Save active times first
            saveAccumulatedTime()

            val skillList = skills.value
            val bookmarkList = bookmarks.value
            val blockList = blockedSites.value
            val key = _syncKey.value

            val success = syncService.pushProgress(
                syncKey = key,
                skills = skillList,
                bookmarks = bookmarkList,
                blockedSites = blockList
            )

            withContext(Dispatchers.Main) {
                if (success) {
                    _lastSyncedAt.value = System.currentTimeMillis()
                    prefs.edit().putLong("last_synced_at", _lastSyncedAt.value).apply()
                    _syncState.value = SyncState.Success("Progress successfully backed up to cloud!")
                } else {
                    _syncState.value = SyncState.Error("Backup failed. Check internet connection and retry.")
                }
            }
        }
    }

    fun connectAndPullSync(targetKey: String) {
        val sanitized = targetKey.trim().uppercase()
        if (sanitized.isEmpty() || sanitized.length < 4) {
            _syncState.value = SyncState.Error("Please enter a valid Sync Key.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _syncState.value = SyncState.Syncing
            val payload = syncService.pullProgress(sanitized)

            withContext(Dispatchers.Main) {
                if (payload != null) {
                    // Update our Sync Key
                    _syncKey.value = sanitized
                    prefs.edit().putString("sync_key", sanitized).apply()

                    _lastSyncedAt.value = payload.lastSyncedAt
                    prefs.edit().putLong("last_synced_at", payload.lastSyncedAt).apply()

                    // Merge Room Database
                    mergeLocalData(payload.skills, payload.bookmarks, payload.blockedSites)
                    _syncState.value = SyncState.Success("Successfully synced and merged devices!")
                } else {
                    _syncState.value = SyncState.Error("Sync Key not found or network error occurred.")
                }
            }
        }
    }

    private fun mergeLocalData(
        newSkills: List<Skill>,
        newBookmarks: List<WebHistory>,
        newBlockedSites: List<BlockedSite>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // Stop active sessions to prevent database locks
            withContext(Dispatchers.Main) {
                stopStudySession()
            }

            // Simple replace/merge protocol
            // 1. Merge Skills: insert if new, or update with higher studyTime
            val existingSkills = skills.value.associateBy { it.name }
            for (ns in newSkills) {
                val match = existingSkills[ns.name]
                if (match != null) {
                    val merged = match.copy(
                        studyTimeSeconds = maxOf(match.studyTimeSeconds, ns.studyTimeSeconds),
                        notes = if (ns.notes.length > match.notes.length) ns.notes else match.notes,
                        isCompleted = match.isCompleted || ns.isCompleted,
                        lastStudiedAt = maxOf(match.lastStudiedAt, ns.lastStudiedAt)
                    )
                    repository.updateSkill(merged)
                } else {
                    repository.insertSkill(ns.copy(id = 0)) // Clear ID for autogeneration
                }
            }

            // 2. Merge Bookmarks
            val existingBookmarks = bookmarks.value.map { it.url }.toSet()
            for (nb in newBookmarks) {
                if (!existingBookmarks.contains(nb.url)) {
                    repository.toggleBookmark(nb.title, nb.url)
                }
            }

            // 3. Merge Blocked sites
            val existingBlocked = blockedSites.value.map { it.domain.lowercase() }.toSet()
            for (nbs in newBlockedSites) {
                if (!existingBlocked.contains(nbs.domain.lowercase())) {
                    repository.insertBlockedSite(nbs.copy(id = 0))
                }
            }
        }
    }

    fun clearSyncMessage() {
        _syncState.value = SyncState.Idle
    }

    private fun formatUrl(input: String): String {
        val trimmed = input.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        return "https://$trimmed"
    }

    // --- POMODORO CONTROL FUNCTIONS ---
    fun startPomodoro() {
        pomodoroJob?.cancel()
        _pomodoroState.value = "RUNNING"
        savePomoStateToPrefs()

        pomodoroJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (_pomodoroTimeLeft.value > 0) {
                    _pomodoroTimeLeft.value -= 1
                    savePomoProgressToPrefs()

                    // If we have an active skill studying during a WORK Pomodoro, log study progress
                    if (_pomodoroMode.value == "WORK" && _activeSkill.value != null) {
                        _sessionTimeSeconds.value += 1
                        if (_sessionTimeSeconds.value % 5 == 0L) {
                            saveAccumulatedTime()
                        }
                    }
                } else {
                    val finishedMode = _pomodoroMode.value
                    onPomodoroFinishedOnline(finishedMode)
                    break
                }
            }
        }
    }

    fun pausePomodoro() {
        pomodoroJob?.cancel()
        pomodoroJob = null
        _pomodoroState.value = "PAUSED"
        savePomoStateToPrefs()
    }

    fun resetPomodoro() {
        pomodoroJob?.cancel()
        pomodoroJob = null
        _pomodoroState.value = "IDLE"
        
        val duration = getDurationForMode(_pomodoroMode.value)
        _pomodoroTimeLeft.value = duration
        _pomodoroDuration.value = duration
        
        savePomoStateToPrefs()
    }

    fun skipPomodoro() {
        pomodoroJob?.cancel()
        pomodoroJob = null
        
        val nextMode = when (_pomodoroMode.value) {
            "WORK" -> {
                val nextCount = _completedPomodoros.value + 1
                _completedPomodoros.value = nextCount
                prefs.edit().putInt("pomo_completed", nextCount).apply()
                if (nextCount % 4 == 0) "LONG_BREAK" else "SHORT_BREAK"
            }
            "SHORT_BREAK", "LONG_BREAK" -> "WORK"
            else -> "WORK"
        }
        
        _pomodoroMode.value = nextMode
        _pomodoroState.value = "IDLE"
        
        val duration = getDurationForMode(nextMode)
        _pomodoroTimeLeft.value = duration
        _pomodoroDuration.value = duration
        
        savePomoStateToPrefs()
    }

    fun updatePomodoroSettings(workMins: Int, shortBreakMins: Int, longBreakMins: Int) {
        _pomodoroWorkMinutes.value = workMins
        _pomodoroShortBreakMinutes.value = shortBreakMins
        _pomodoroLongBreakMinutes.value = longBreakMins

        prefs.edit()
            .putInt("pomo_work_minutes", workMins)
            .putInt("pomo_short_break_minutes", shortBreakMins)
            .putInt("pomo_long_break_minutes", longBreakMins)
            .apply()

        if (_pomodoroState.value == "IDLE" || _pomodoroState.value == "PAUSED") {
            resetPomodoro()
        }
    }

    fun setPomodoroModeDirectly(mode: String) {
        pomodoroJob?.cancel()
        pomodoroJob = null
        _pomodoroMode.value = mode
        _pomodoroState.value = "IDLE"
        val duration = getDurationForMode(mode)
        _pomodoroTimeLeft.value = duration
        _pomodoroDuration.value = duration
        savePomoStateToPrefs()
    }

    private fun getDurationForMode(mode: String): Int {
        return when (mode) {
            "WORK" -> _pomodoroWorkMinutes.value * 60
            "SHORT_BREAK" -> _pomodoroShortBreakMinutes.value * 60
            "LONG_BREAK" -> _pomodoroLongBreakMinutes.value * 60
            else -> 25 * 60
        }
    }

    private fun onPomodoroFinishedOnline(finishedMode: String) {
        viewModelScope.launch(Dispatchers.Main) {
            playCompletionSound()
        }
        handlePomoCompletion(finishedMode)
    }

    fun handlePomoCompletion(finishedMode: String) {
        val nextMode = when (finishedMode) {
            "WORK" -> {
                val nextCount = _completedPomodoros.value + 1
                _completedPomodoros.value = nextCount
                prefs.edit().putInt("pomo_completed", nextCount).apply()
                
                viewModelScope.launch {
                    saveAccumulatedTime()
                }

                if (nextCount % 4 == 0) "LONG_BREAK" else "SHORT_BREAK"
            }
            "SHORT_BREAK", "LONG_BREAK" -> "WORK"
            else -> "WORK"
        }

        _pomodoroMode.value = nextMode
        _pomodoroState.value = "IDLE"
        val duration = getDurationForMode(nextMode)
        _pomodoroTimeLeft.value = duration
        _pomodoroDuration.value = duration

        savePomoStateToPrefs()
    }

    private fun playCompletionSound() {
        try {
            val notificationUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = android.media.RingtoneManager.getRingtone(context, notificationUri)
            ringtone.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun savePomoStateToPrefs() {
        prefs.edit()
            .putString("pomo_state", _pomodoroState.value)
            .putString("pomo_mode", _pomodoroMode.value)
            .putInt("pomo_time_left", _pomodoroTimeLeft.value)
            .putInt("pomo_duration", _pomodoroDuration.value)
            .putLong("pomo_last_active_time", System.currentTimeMillis())
            .apply()
    }

    private fun savePomoProgressToPrefs() {
        prefs.edit()
            .putInt("pomo_time_left", _pomodoroTimeLeft.value)
            .putLong("pomo_last_active_time", System.currentTimeMillis())
            .apply()
    }

    // --- GEMINI AI ASSISTANT STATE & CONTROLS ---
    private val recorder = AudioRecorder(context)

    private val _useGoogleSearch = MutableStateFlow(true)
    val useGoogleSearch: StateFlow<Boolean> = _useGoogleSearch.asStateFlow()

    private val _useGoogleMaps = MutableStateFlow(false)
    val useGoogleMaps: StateFlow<Boolean> = _useGoogleMaps.asStateFlow()

    private val _isRecordingAudio = MutableStateFlow(false)
    val isRecordingAudio: StateFlow<Boolean> = _isRecordingAudio.asStateFlow()

    private val _isProcessingAi = MutableStateFlow(false)
    val isProcessingAi: StateFlow<Boolean> = _isProcessingAi.asStateFlow()

    private val _aiChatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val aiChatHistory: StateFlow<List<ChatMessage>> = _aiChatHistory.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val _transcriptionStatus = MutableStateFlow<String?>(null)
    val transcriptionStatus: StateFlow<String?> = _transcriptionStatus.asStateFlow()

    private val _aiPromptInput = MutableStateFlow("")
    val aiPromptInput: StateFlow<String> = _aiPromptInput.asStateFlow()

    fun setUseGoogleSearch(enabled: Boolean) {
        _useGoogleSearch.value = enabled
    }

    fun setUseGoogleMaps(enabled: Boolean) {
        _useGoogleMaps.value = enabled
    }

    fun setAiPromptInput(input: String) {
        _aiPromptInput.value = input
    }

    fun clearAiError() {
        _aiError.value = null
    }

    fun startAudioRecording() {
        _aiError.value = null
        _transcriptionStatus.value = "Recording..."
        val success = recorder.startRecording()
        if (success) {
            _isRecordingAudio.value = true
        } else {
            _transcriptionStatus.value = "Failed to start microphone"
        }
    }

    fun stopAudioRecordingAndTranscribe() {
        if (!_isRecordingAudio.value) return
        _isRecordingAudio.value = false
        _transcriptionStatus.value = "Transcribing audio with Gemini..."
        
        viewModelScope.launch {
            val base64Audio = recorder.stopRecording()
            if (base64Audio == null) {
                _transcriptionStatus.value = "Failed to record audio or no audio detected"
                return@launch
            }

            _isProcessingAi.value = true
            try {
                // Request Gemini 3.5 Flash to transcribe the audio precisely
                val response = GeminiServiceClient.generateContent(
                    prompt = "Transcribe the accompanying audio exactly as spoken. Do not add any introductory text, prefix, or extra words. Return ONLY the literal transcribed text.",
                    audioBase64 = base64Audio,
                    audioMimeType = "audio/mp4" // AAC encoded inside MPEG_4 is audio/mp4
                )
                
                val text = response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                if (!text.isNullOrEmpty()) {
                    _aiPromptInput.value = text
                    _transcriptionStatus.value = "Transcribed successfully!"
                } else {
                    _transcriptionStatus.value = "Gemini could not detect spoken words. Please speak clearly."
                }
            } catch (e: Exception) {
                Log.e("BrowserViewModel", "Transcription error", e)
                _transcriptionStatus.value = "Transcription failed: ${e.localizedMessage}"
            } finally {
                _isProcessingAi.value = false
            }
        }
    }

    fun sendChatMessage(prompt: String) {
        if (prompt.isBlank()) return
        _aiPromptInput.value = ""
        _aiError.value = null
        _transcriptionStatus.value = null

        // Add user message to history
        val userMsg = ChatMessage(text = prompt, isUser = true)
        _aiChatHistory.value = _aiChatHistory.value + userMsg

        _isProcessingAi.value = true

        viewModelScope.launch {
            try {
                // Convert ChatMessage history to Gemini Contents format for context
                val apiHistory = _aiChatHistory.value.dropLast(1).map { msg ->
                    com.example.data.api.Content(
                        parts = listOf(com.example.data.api.Part(text = msg.text)),
                        role = if (msg.isUser) "user" else "model"
                    )
                }

                val response = GeminiServiceClient.generateContent(
                    prompt = prompt,
                    useSearch = _useGoogleSearch.value,
                    useMaps = _useGoogleMaps.value,
                    history = apiHistory
                )

                val replyText = response?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (replyText != null) {
                    val metadata = response.candidates.firstOrNull()?.groundingMetadata
                    val searchQueries = metadata?.webSearchQueries
                    
                    val sourceLinks = mutableListOf<Pair<String, String>>()
                    metadata?.groundingChunks?.forEach { chunk ->
                        val web = chunk.web
                        if (web?.title != null && web.uri != null) {
                            sourceLinks.add(Pair(web.title, web.uri))
                        }
                    }

                    val assistantMsg = ChatMessage(
                        text = replyText,
                        isUser = false,
                        webSearchQueries = searchQueries,
                        sourceLinks = if (sourceLinks.isNotEmpty()) sourceLinks.distinctBy { it.second } else null
                    )
                    _aiChatHistory.value = _aiChatHistory.value + assistantMsg
                } else {
                    _aiError.value = "No response. Check your API Key or Network connection."
                }
            } catch (e: Exception) {
                Log.e("BrowserViewModel", "Gemini Chat error", e)
                _aiError.value = "Error: ${e.localizedMessage}"
            } finally {
                _isProcessingAi.value = false
            }
        }
    }

    fun clearChat() {
        _aiChatHistory.value = emptyList()
        _aiError.value = null
        _transcriptionStatus.value = null
    }

    override fun onCleared() {
        stopStudySession()
        pomodoroJob?.cancel()
        super.onCleared()
    }
}

class BrowserViewModelFactory(
    private val context: Context,
    private val repository: ProgressRepository,
    private val syncService: CloudSyncService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrowserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BrowserViewModel(context, repository, syncService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
