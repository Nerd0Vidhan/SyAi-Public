package com.mato.syai.notes.domain.model

data class Folder(
    val id: String,
    val name: String,
    val color: Long,
    val noteCount: Int
)