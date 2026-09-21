package com.toolnexa.app

object ToolRegistry {

    val allTools: List<ToolDefinition> = listOf(
        ToolDefinition(
            name = "Invoice Maker",
            category = "Business",
            description = "Crie uma fatura profissional com cálculo automático.",
            icon = R.drawable.ic_tool_plans,
            id = "business-invoice-maker"
        ),
        ToolDefinition(
            name = "Receipt Maker",
            category = "Business",
            description = "Crie recibos com total e dados do cliente.",
            icon = R.drawable.ic_tool_plans,
            id = "business-receipt-maker"
        ),
        ToolDefinition(
            name = "Quote Maker",
            category = "Business",
            description = "Monte orçamentos com itens, desconto e imposto.",
            icon = R.drawable.ic_tool_plans,
            id = "business-quote-maker"
        ),
        ToolDefinition(
            name = "Profit Calculator",
            category = "Business",
            description = "Calcule receita, custos, lucro e margem.",
            icon = R.drawable.ic_tool_plans,
            id = "business-profit-calculator"
        ),
        ToolDefinition(
            name = "Expense Tracker",
            category = "Business",
            description = "Registre despesas e veja totais por categoria.",
            icon = R.drawable.ic_tool_plans,
            id = "business-expense-tracker"
        ),
        ToolDefinition(
            name = "Business Plan",
            category = "Business",
            description = "Monte um plano de negócio estruturado.",
            icon = R.drawable.ic_tool_plans,
            id = "business-plan"
        ),
        ToolDefinition(
            name = "Business Proposal",
            category = "Business",
            description = "Crie uma proposta comercial pronta para compartilhar.",
            icon = R.drawable.ic_tool_plans,
            id = "business-proposal"
        ),
        ToolDefinition(
            name = "Contract Maker",
            category = "Business",
            description = "Gere um modelo de contrato comercial editável.",
            icon = R.drawable.ic_tool_plans,
            id = "business-contract-maker"
        ),
        ToolDefinition(
            name = "Business Name Generator",
            category = "Business",
            description = "Gere nomes de negócio a partir do seu setor.",
            icon = R.drawable.ic_tool_plans,
            id = "business-name-generator"
        ),
        ToolDefinition(
            name = "Pricing Calculator",
            category = "Business",
            description = "Calcule preço de venda com custos e margem.",
            icon = R.drawable.ic_tool_plans,
            id = "business-pricing-calculator"
        ),
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
