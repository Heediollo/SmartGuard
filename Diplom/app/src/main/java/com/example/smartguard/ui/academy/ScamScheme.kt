package com.example.smartguard.ui.academy

import java.io.Serializable

data class ScamScheme(
    val id: String,
    val title: String,
    val shortDesc: String,
    val fullDesc: String,        // может быть HTML или plain text
    val redFlags: List<String>,
    val tips: List<String>,
    val imageName: String = ""   // если есть иллюстрация
) : Serializable