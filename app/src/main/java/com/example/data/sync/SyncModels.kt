package com.example.data.sync

import com.example.data.database.BlockedSite
import com.example.data.database.Skill
import com.example.data.database.WebHistory

data class SyncPayload(
    val syncKey: String,
    val lastSyncedAt: Long,
    val skills: List<Skill>,
    val bookmarks: List<WebHistory>,
    val blockedSites: List<BlockedSite>
)
