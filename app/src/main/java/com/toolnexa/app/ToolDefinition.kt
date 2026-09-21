package com.toolnexa.app

data class ToolDefinition(
    val name: String,
    val category: String,
    val description: String,
    val icon: Int,
    val id: String = ""
)
