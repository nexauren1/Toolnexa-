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
                Color.parseColor("#4F46E5"),
                Color.parseColor("#EEF2FF")
            ),
            CategoryDefinition(
                "Vídeo",
                "Ferramentas para ficheiros de vídeo",
                android.R.drawable.ic_menu_slideshow,
                Color.parseColor("#DB2777"),
                Color.parseColor("#FCE7F3")
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
                Color.parseColor("#172033"),
                Color.parseColor("#F1F5F9")
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
                Color.parseColor("#EA580C"),
                Color.parseColor("#FFF7ED")
            ),
            CategoryDefinition(
                "Marketplace",
                "Conteúdo e preparação para vendas online",
                android.R.drawable.ic_menu_share,
                Color.parseColor("#DB2777"),
                Color.parseColor("#FCE7F3")
            ),
            CategoryDefinition(
                "Texto",
                "Edição, limpeza e transformação de texto",
                android.R.drawable.ic_menu_edit,
                Color.parseColor("#4F46E5"),
                Color.parseColor("#EEF2FF")
            ),
            CategoryDefinition(
                "PDF",
                "Ferramentas para documentos PDF",
                android.R.drawable.ic_menu_save,
                Color.parseColor("#DB2777"),
                Color.parseColor("#FEE2E2")
            ),
            CategoryDefinition(
                "Áudio",
                "Ferramentas para ficheiros e áudio",
                android.R.drawable.ic_menu_view,
                Color.parseColor("#EA580C"),
                Color.parseColor("#FFF7ED")
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
                        android.R.drawable.ic_menu_save
                    ),
                    ToolDefinition(
                        "Image Resizer",
                        "Imagem",
                        "Altere a largura mantendo a proporção.",
                        android.R.drawable.ic_menu_crop
                    ),
                    ToolDefinition(
                        "Image Converter",
                        "Imagem",
                        "Converta para JPG, PNG ou WebP.",
                        android.R.drawable.ic_menu_manage
                    ),
                    ToolDefinition(
                        "Background Remover",
                        "Imagem",
                        "Remova o fundo com IA e preserve transparência.",
                        android.R.drawable.ic_menu_delete
                    )
                )

            "Áudio" ->
                listOf(
                    ToolDefinition(
                        "Audio to MIDI",
                        "Áudio",
                        "Converta uma melodia de áudio em notas MIDI editáveis.",
                        android.R.drawable.ic_media_play
                    )
                )

            else ->
                emptyList()
        }
    }
}
