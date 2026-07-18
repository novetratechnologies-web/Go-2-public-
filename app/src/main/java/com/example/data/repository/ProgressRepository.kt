package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ProgressRepository(private val database: AppDatabase) {
    private val skillDao = database.skillDao()
    private val webHistoryDao = database.webHistoryDao()
    private val blockedSiteDao = database.blockedSiteDao()

    val allSkills: Flow<List<Skill>> = skillDao.getAllSkills()
    val history: Flow<List<WebHistory>> = webHistoryDao.getHistory()
    val bookmarks: Flow<List<WebHistory>> = webHistoryDao.getBookmarks()
    val blockedSites: Flow<List<BlockedSite>> = blockedSiteDao.getAllBlockedSites()

    suspend fun getSkillById(id: Int): Skill? = withContext(Dispatchers.IO) {
        skillDao.getSkillById(id)
    }

    suspend fun insertSkill(skill: Skill): Long = withContext(Dispatchers.IO) {
        skillDao.insertSkill(skill)
    }

    suspend fun updateSkill(skill: Skill) = withContext(Dispatchers.IO) {
        skillDao.updateSkill(skill)
    }

    suspend fun deleteSkill(skill: Skill) = withContext(Dispatchers.IO) {
        skillDao.deleteSkill(skill)
    }

    suspend fun deleteSkillById(id: Int) = withContext(Dispatchers.IO) {
        skillDao.deleteSkillById(id)
    }

    suspend fun addHistory(title: String, url: String) = withContext(Dispatchers.IO) {
        val existing = webHistoryDao.getHistoryItemByUrl(url)
        if (existing != null) {
            webHistoryDao.insert(existing.copy(timestamp = System.currentTimeMillis()))
        } else {
            webHistoryDao.insert(WebHistory(title = title, url = url, isBookmark = false))
        }
    }

    suspend fun toggleBookmark(title: String, url: String) = withContext(Dispatchers.IO) {
        val existingBookmark = webHistoryDao.getBookmarkByUrl(url)
        if (existingBookmark != null) {
            webHistoryDao.delete(existingBookmark)
        } else {
            webHistoryDao.insert(WebHistory(title = title, url = url, isBookmark = true))
        }
    }

    suspend fun isBookmarked(url: String): Boolean = withContext(Dispatchers.IO) {
        webHistoryDao.getBookmarkByUrl(url) != null
    }

    suspend fun deleteBookmark(url: String) = withContext(Dispatchers.IO) {
        webHistoryDao.deleteByUrlAndType(url, isBookmark = true)
    }

    suspend fun deleteHistoryById(id: Int) = withContext(Dispatchers.IO) {
        webHistoryDao.deleteById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        webHistoryDao.clearHistory()
    }

    suspend fun insertBlockedSite(site: BlockedSite) = withContext(Dispatchers.IO) {
        blockedSiteDao.insert(site)
    }

    suspend fun updateBlockedSite(site: BlockedSite) = withContext(Dispatchers.IO) {
        blockedSiteDao.update(site)
    }

    suspend fun deleteBlockedSite(site: BlockedSite) = withContext(Dispatchers.IO) {
        blockedSiteDao.delete(site)
    }

    suspend fun deleteBlockedSiteById(id: Int) = withContext(Dispatchers.IO) {
        blockedSiteDao.deleteById(id)
    }

    suspend fun getActiveBlockedDomains(): Set<String> = withContext(Dispatchers.IO) {
        blockedSiteDao.getActiveBlockedSites()
            .map { it.domain.lowercase().trim() }
            .toSet()
    }

    suspend fun checkAndPopulateDefaults() = withContext(Dispatchers.IO) {
        // Populate default skills if empty
        val currentSkills = allSkills.first()
        if (currentSkills.isEmpty()) {
            val defaultSkills = listOf(
                Skill(
                    name = "Android Development with Kotlin",
                    targetUrl = "https://developer.android.com/courses",
                    notes = "Welcome to Go Learn! Use this card to track your Android learnings. Take notes here during study sessions.",
                    studyTimeSeconds = 0
                ),
                Skill(
                    name = "Web Design Essentials (HTML/CSS/JS)",
                    targetUrl = "https://web.dev",
                    notes = "Take beautiful notes here. Click the Globe icon to browse the target learning site with Focus Mode active!",
                    studyTimeSeconds = 0
                ),
                Skill(
                    name = "Machine Learning Basics",
                    targetUrl = "https://www.google.com/search?q=machine+learning+for+beginners",
                    notes = "Use this space to outline your study plan for Machine Learning. Track study minutes with the built-in session timer.",
                    studyTimeSeconds = 0
                )
            )
            for (skill in defaultSkills) {
                skillDao.insertSkill(skill)
            }
        }

        // Populate default distracting/blocked sites if empty
        val currentBlocked = blockedSites.first()
        if (currentBlocked.isEmpty()) {
            val defaultBlocked = listOf(
                "facebook.com",
                "instagram.com",
                "tiktok.com",
                "twitter.com",
                "x.com",
                "youtube.com",
                "reddit.com",
                "netflix.com",
                "twitch.tv"
            )
            for (domain in defaultBlocked) {
                blockedSiteDao.insert(BlockedSite(domain = domain, isActive = true))
            }
        }
    }
}
