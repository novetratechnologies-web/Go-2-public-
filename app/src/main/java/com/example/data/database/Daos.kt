package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillDao {
    @Query("SELECT * FROM skills ORDER BY lastStudiedAt DESC")
    fun getAllSkills(): Flow<List<Skill>>

    @Query("SELECT * FROM skills WHERE id = :id LIMIT 1")
    suspend fun getSkillById(id: Int): Skill?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkill(skill: Skill): Long

    @Update
    suspend fun updateSkill(skill: Skill)

    @Delete
    suspend fun deleteSkill(skill: Skill)

    @Query("DELETE FROM skills WHERE id = :id")
    suspend fun deleteSkillById(id: Int)
}

@Dao
interface WebHistoryDao {
    @Query("SELECT * FROM web_history WHERE isBookmark = 0 ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<WebHistory>>

    @Query("SELECT * FROM web_history WHERE isBookmark = 1 ORDER BY timestamp DESC")
    fun getBookmarks(): Flow<List<WebHistory>>

    @Query("SELECT * FROM web_history WHERE url = :url LIMIT 1")
    suspend fun getHistoryItemByUrl(url: String): WebHistory?

    @Query("SELECT * FROM web_history WHERE url = :url AND isBookmark = 1 LIMIT 1")
    suspend fun getBookmarkByUrl(url: String): WebHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WebHistory)

    @Delete
    suspend fun delete(item: WebHistory)

    @Query("DELETE FROM web_history WHERE url = :url AND isBookmark = :isBookmark")
    suspend fun deleteByUrlAndType(url: String, isBookmark: Boolean)

    @Query("DELETE FROM web_history WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM web_history WHERE isBookmark = 0")
    suspend fun clearHistory()
}

@Dao
interface BlockedSiteDao {
    @Query("SELECT * FROM blocked_sites ORDER BY domain ASC")
    fun getAllBlockedSites(): Flow<List<BlockedSite>>

    @Query("SELECT * FROM blocked_sites WHERE isActive = 1")
    suspend fun getActiveBlockedSites(): List<BlockedSite>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(site: BlockedSite)

    @Update
    suspend fun update(site: BlockedSite)

    @Delete
    suspend fun delete(site: BlockedSite)

    @Query("DELETE FROM blocked_sites WHERE id = :id")
    suspend fun deleteById(id: Int)
}
