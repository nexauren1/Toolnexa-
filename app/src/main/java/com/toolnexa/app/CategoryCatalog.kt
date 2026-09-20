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
                Color.parseColor("#2F6BFF"),
                Color.parseColor("#EAF0FF")
            ),
            CategoryDefinition(
                "Vídeo",
                "Ferramentas para ficheiros de vídeo",
                android.R.drawable.ic_menu_slideshow,
                Color.parseColor("#8B5CF6"),
                Color.parseColor("#F0E9FF")
            ),
            CategoryDefinition(
                "Produtividade",
                "Tarefas, organização e trabalho",
                android.R.drawable.ic_menu_agenda,
                Color.parseColor("#0EA5A4"),
                Color.parseColor("#E4FAF8")
            ),
            CategoryDefinition(
                "Código",
                "Ferramentas para escrever e tratar código",
                android.R.drawable.ic_menu_edit,
                Color.parseColor("#2563EB"),
                Color.parseColor("#E8F0FF")
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
                Color.parseColor("#EC4899"),
                Color.parseColor("#FDE9F3")
            ),
            CategoryDefinition(
                "Texto",
                "Edição, limpeza e transformação de texto",
                android.R.drawable.ic_menu_edit,
                Color.parseColor("#EF4444"),
                Color.parseColor("#FFE9E9")
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
                Color.parseColor("#06B6D4"),
                Color.parseColor("#E4F9FD")
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
                    )
                )

            else ->
                emptyList()
        }
    }
}
