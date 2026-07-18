package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skills")
data class Skill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetUrl: String = "",
    val notes: String = "",
    val studyTimeSeconds: Long = 0,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastStudiedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "web_history")
data class WebHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val isBookmark: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "blocked_sites")
data class BlockedSite(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val domain: String,
    val isActive: Boolean = true
)
