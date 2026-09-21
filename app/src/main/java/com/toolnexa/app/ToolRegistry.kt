package com.toolnexa.app

object ToolRegistry {

    val allTools: List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "Image Compressor",
            category = "Imagem",
            description =
                "Reduza o tamanho da imagem com qualidade ajustável.",
            icon = R.drawable.ic_tool_compress,
            id = "image-compressor"
        ),
        ToolDefinition(
            name = "Image Resizer",
            category = "Imagem",
            description =
                "Redimensione a imagem e veja a nova prévia.",
            icon = R.drawable.ic_tool_resize,
            id = "image-resizer"
        ),
        ToolDefinition(
            name = "Image Converter",
            category = "Imagem",
            description =
                "Converta imagens para JPG, PNG ou WebP.",
            icon = R.drawable.ic_tool_convert,
            id = "image-converter"
        ),
        ToolDefinition(
            name = "Background Remover",
            category = "Imagem",
            description =
                "Remova o fundo com IA e preserve transparência.",
            icon = R.drawable.ic_tool_background,
            id = "background-remover"
        ),
        ToolDefinition(
            name = "Audio to MIDI",
            category = "Áudio",
            description =
                "Converta uma melodia de áudio em notas MIDI editáveis.",
            icon = android.R.drawable.ic_media_play,
            id = "audio-to-midi"
        )
    )

    fun toolsFor(category: String): List<ToolDefinition> {
        return allTools.filter { tool ->
            tool.category.equals(category, ignoreCase = true)
        }
    }

    fun countFor(category: String): Int {
        return toolsFor(category).size
    }

    fun countAll(): Int {
        return allTools.size
    }

    fun findById(id: String): ToolDefinition? {
        return allTools.firstOrNull {
            it.id.equals(id, ignoreCase = true)
        }
    }

    fun findByName(name: String): ToolDefinition? {
        return allTools.firstOrNull {
            it.name.equals(name, ignoreCase = true)
        }
    }
}
