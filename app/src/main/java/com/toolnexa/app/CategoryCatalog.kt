package com.toolnexa.app

import android.graphics.Color

data class CategoryDefinition(
    val name: String,
    val description: String,
    val icon: Int,
    val accent: Int,
    val softColor: Int
)

object CategoryCatalog {
    val all =
        listOf(
            CategoryDefinition(
                "Imagem",
                "Compressão, tamanho e formatos",
                android.R.drawable.ic_menu_gallery,
                Color.parseColor("#2563EB"),
                Color.parseColor("#E8F0FF")
            ),
            CategoryDefinition(
                "Vídeo",
                "Ferramentas para ficheiros de vídeo",
                android.R.drawable.ic_menu_slideshow,
                Color.parseColor("#DC2626"),
                Color.parseColor("#FFECEC")
            ),
            CategoryDefinition(
                "Produtividade",
                "Tarefas, organização e trabalho",
                android.R.drawable.ic_menu_agenda,
                Color.parseColor("#16A34A"),
                Color.parseColor("#E8F8EC")
            ),
            CategoryDefinition(
                "Código",
                "Ferramentas para escrever e tratar código",
                android.R.drawable.ic_menu_edit,
                Color.parseColor("#111827"),
                Color.parseColor("#EEF1F5")
            ),
            CategoryDefinition(
                "Dev",
                "Utilitários para desenvolvimento",
                android.R.drawable.ic_menu_manage,
                Color.parseColor("#16A34A"),
                Color.parseColor("#E8F8EC")
            ),
            CategoryDefinition(
                "Business",
                "Ferramentas para negócios e operações",
                android.R.drawable.ic_menu_info_details,
                Color.parseColor("#F59E0B"),
                Color.parseColor("#FFF5DB")
            ),
            CategoryDefinition(
                "Marketplace",
                "Conteúdo e preparação para vendas online",
                android.R.drawable.ic_menu_share,
                Color.parseColor("#DC2626"),
                Color.parseColor("#FFECEC")
            ),
            CategoryDefinition(
                "Texto",
                "Edição, limpeza e transformação de texto",
                android.R.drawable.ic_menu_edit,
                Color.parseColor("#2563EB"),
                Color.parseColor("#E8F0FF")
            ),
            CategoryDefinition(
                "PDF",
                "Ferramentas para documentos PDF",
                android.R.drawable.ic_menu_save,
                Color.parseColor("#DC2626"),
                Color.parseColor("#FFEDED")
            ),
            CategoryDefinition(
                "Áudio",
                "Ferramentas para ficheiros e áudio",
                android.R.drawable.ic_menu_view,
                Color.parseColor("#F59E0B"),
                Color.parseColor("#FFF5DB")
            )
        )

    fun toolsFor(
        category: String
    ): List<ToolDefinition> {
        return when (category) {
            "Imagem" ->
                listOf(
                    ToolDefinition(
                        "Image Compressor",
                        "Imagem",
                        "Reduza o tamanho com qualidade ajustável.",
                        R.drawable.ic_tool_compress
                    ),
                    ToolDefinition(
                        "Image Resizer",
                        "Imagem",
                        "Altere a largura mantendo a proporção.",
                        R.drawable.ic_tool_resize
                    ),
                    ToolDefinition(
                        "Image Converter",
                        "Imagem",
                        "Converta para JPG, PNG ou WebP.",
                        R.drawable.ic_tool_convert
                    ),
                    ToolDefinition(
                        "Background Remover",
                        "Imagem",
                        "Remova o fundo com IA e preserve transparência.",
                        R.drawable.ic_tool_background
                    )
                )

            else ->
                emptyList()
        }
    }
}
