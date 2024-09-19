package com.dev.notenest

data class Note(
    val id: Long,
    var title: String,
    var content: String,
    var timestamp: String  // New field for timestamp
)
