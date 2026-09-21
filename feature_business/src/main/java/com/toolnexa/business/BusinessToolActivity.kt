package com.toolnexa.business

import com.toolnexa.app.LanguageManager
import com.toolnexa.app.R

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.text.InputType
import android.widget.*
import java.text.NumberFormat
import java.util.Locale
import kotlin.random.Random

class BusinessToolActivity : Activity() {
    private lateinit var body: LinearLayout
    private val fields = mutableMapOf<String, EditText>()
    private var toolId = ""

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        LanguageManager.apply(this)
        toolId = intent.getStringExtra("tool_id").orEmpty()
        build()
    }

    private fun build() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(getColor(R.color.toolnexa_bg))

        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.setPadding(dp(18), dp(16), dp(18), dp(12))
        header.setBackgroundColor(getColor(R.color.toolnexa_surface))
        header.addView(TextView(this).apply {
            text = "ToolNexa"; textSize = 20f
            setTextColor(getColor(R.color.toolnexa_text))
        })
        header.addView(TextView(this).apply {
            text = title(); textSize = 27f
            setTextColor(getColor(R.color.toolnexa_text))
            setTypeface(null, android.graphics.Typeface.BOLD)
        })
        root.addView(header)

        body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL
        body.setPadding(dp(16), dp(14), dp(16), dp(30))
        root.addView(ScrollView(this).apply { addView(body) },
            LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        renderForm()
    }

    private fun renderForm() {
        when (toolId) {
            "business-invoice-maker" -> {
                text("Crie uma fatura com cálculo automático.")
                field("Empresa", "company"); field("Cliente", "client")
                field("Descrição", "description"); money("Preço", "price")
                number("Quantidade", "quantity", "1")
                money("Desconto (%)", "discount", "0"); money("Imposto (%)", "tax", "0")
            }
            "business-receipt-maker" -> {
                text("Gere um recibo profissional.")
                field("Empresa", "company"); field("Cliente", "client")
                field("Descrição", "description"); money("Valor", "price")
                field("Método de pagamento", "method")
            }
            "business-quote-maker" -> {
                text("Monte um orçamento para o cliente.")
                field("Empresa", "company"); field("Cliente", "client")
                field("Produto / serviço", "description"); money("Preço unitário", "price")
                number("Quantidade", "quantity", "1")
                money("Desconto (%)", "discount", "0"); money("Imposto (%)", "tax", "0")
            }
            "business-profit-calculator" -> {
                text("Calcule receita, custos, lucro e margem.")
                money("Preço de venda por unidade", "price")
                number("Quantidade", "quantity", "1")
                money("Custo por unidade", "cost"); money("Outros custos", "other", "0")
            }
            "business-expense-tracker" -> {
                text("Registre uma despesa.")
                field("Descrição", "description"); money("Valor", "price")
                field("Categoria", "category")
            }
            "business-pricing-calculator" -> {
                text("Calcule o preço usando custo e margem.")
                money("Custo por unidade", "cost"); money("Outros custos", "other", "0")
                money("Margem desejada (%)", "margin", "30"); money("Imposto (%)", "tax", "0")
            }
            "business-plan" -> {
                text("Monte um plano de negócio estruturado.")
                field("Nome do negócio", "company"); field("Setor", "sector")
                field("Produto ou serviço", "description"); field("Público-alvo", "audience")
                field("Objetivo principal", "goal")
            }
            "business-proposal" -> {
                text("Crie uma proposta comercial.")
                field("Sua empresa", "company"); field("Cliente", "client")
                field("Serviço proposto", "description"); money("Valor", "price")
                field("Prazo", "deadline")
            }
            "business-contract-maker" -> {
                text("Crie um modelo comercial. Revise antes de utilizar.")
                field("Prestador / empresa", "company"); field("Cliente / parceiro", "client")
                field("Serviço", "description"); money("Valor", "price")
                field("Prazo", "deadline")
            }
            "business-name-generator" -> {
                text("Gere ideias de nomes para o seu negócio.")
                field("Setor", "sector"); field("Palavra-chave", "keyword")
                field("Estilo", "style", "moderno")
            }
        }
        button("Gerar resultado", true).setOnClickListener { calculate() }
    }

    private fun calculate() {
        val result: String
        when (toolId) {
            "business-invoice-maker" ->
                result = document("FATURA",
                    "Empresa: " + v("company") + "\nCliente: " + v("client") +
                    "\nDescrição: " + v("description") + "\nQuantidade: " + v("quantity") +
                    "\nPreço: " + money(num("price")) + "\nDesconto: " + v("discount") +
                    "%\nImposto: " + v("tax") + "%\n\nTOTAL: " + money(total()))
            "business-quote-maker" ->
                result = document("ORÇAMENTO",
                    "Empresa: " + v("company") + "\nCliente: " + v("client") +
                    "\nItem: " + v("description") + "\nQuantidade: " + v("quantity") +
                    "\nSubtotal: " + money(subtotal()) + "\nDesconto: " + v("discount") +
                    "%\nImposto: " + v("tax") + "%\n\nTOTAL: " + money(total()))
            "business-receipt-maker" ->
                result = document("RECIBO",
                    "Recebemos de: " + v("client") + "\nEmpresa: " + v("company") +
                    "\nDescrição: " + v("description") + "\nMétodo: " + v("method") +
                    "\n\nVALOR: " + money(num("price")))
            "business-profit-calculator" -> {
                val revenue = num("price") * num("quantity")
                val costs = num("cost") * num("quantity") + num("other")
                val profit = revenue - costs
                val margin = if (revenue > 0) profit / revenue * 100 else 0.0
                result = "RESULTADO\nReceita: " + money(revenue) +
                    "\nCustos: " + money(costs) + "\nLucro: " + money(profit) +
                    "\nMargem: " + String.format(Locale.getDefault(), "%.2f", margin) + "%"
            }
            "business-expense-tracker" ->
                result = "DESPESA\n" + v("description") +
                    "\nCategoria: " + v("category") + "\nValor: " + money(num("price"))
            "business-pricing-calculator" -> {
                val base = num("cost") + num("other")
                val margin = num("margin").coerceIn(0.0, 99.0)
                val beforeTax = base / (1 - margin / 100)
                val finalPrice = beforeTax * (1 + num("tax") / 100)
                result = "PREÇO SUGERIDO\nCusto: " + money(base) +
                    "\nMargem: " + v("margin") + "%\nImposto: " + v("tax") +
                    "%\n\nPreço de venda: " + money(finalPrice)
            }
            "business-plan" ->
                result = document("PLANO DE NEGÓCIO",
                    "Negócio: " + v("company") + "\nSetor: " + v("sector") +
                    "\nProduto/serviço: " + v("description") + "\nPúblico-alvo: " +
                    v("audience") + "\nObjetivo: " + v("goal") +
                    "\n\nPROPOSTA DE VALOR\nDefina o problema do cliente e como o negócio o resolve." +
                    "\n\nOPERAÇÃO\nDefina fornecedores, processos, equipa e canais." +
                    "\n\nFINANÇAS\nListe custos, preço e metas de receita.")
            "business-proposal" ->
                result = document("PROPOSTA COMERCIAL",
                    "De: " + v("company") + "\nPara: " + v("client") +
                    "\nServiço: " + v("description") + "\nValor: " + money(num("price")) +
                    "\nPrazo: " + v("deadline") +
                    "\n\nESCOPO\nDescreva as entregas incluídas." +
                    "\n\nCONDIÇÕES\nDefina pagamento, prazo e responsabilidades.")
            "business-contract-maker" ->
                result = document("MODELO DE CONTRATO",
                    "PRESTADOR: " + v("company") + "\nCLIENTE: " + v("client") +
                    "\nSERVIÇO: " + v("description") + "\nVALOR: " + money(num("price")) +
                    "\nPRAZO: " + v("deadline") +
                    "\n\nOBJETO\nAs partes acordam a prestação do serviço descrito." +
                    "\n\nPAGAMENTO\nDefina datas, método e condições." +
                    "\n\nRESPONSABILIDADES\nDefina as obrigações de cada parte." +
                    "\n\nASSINATURAS\nPrestador: ____________________\nCliente: ______________________" +
                    "\n\nAviso: modelo geral; pode exigir revisão jurídica.")
            "business-name-generator" -> {
                val key = v("keyword").ifBlank { "Nova" }
                val roots = listOf("Nova", "Nexa", "Prime", "Flow", "Pulse", "Core", "Bright", "Urban")
                val endings = listOf("Labs", "Works", "Hub", "Pro", "Group", "Go", "Plus", "Studio")
                val names = (1..8).map {
                    roots[Random.nextInt(roots.size)] + key + endings[Random.nextInt(endings.size)]
                }.distinct().take(6)
                result = "IDEIAS DE NOMES\nSetor: " + v("sector") + "\n\n" +
                    names.joinToString("\n") +
                    "\n\nVerifique marca, domínio e redes sociais antes de usar."
            }
            else -> result = "Ferramenta não encontrada."
        }
        showResult(result)
    }

    private fun showResult(value: String) {
        body.removeAllViews()
        text("Resultado")
        body.addView(TextView(this).apply {
            text = value; textSize = 16f
            setTextColor(getColor(R.color.toolnexa_text))
            setPadding(dp(16), dp(16), dp(16), dp(16))
            setBackgroundColor(getColor(R.color.toolnexa_surface))
        })
        button("Copiar resultado", true).setOnClickListener {
            (getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                .setPrimaryClip(android.content.ClipData.newPlainText("ToolNexa", value))
            Toast.makeText(this, "Copiado.", Toast.LENGTH_SHORT).show()
        }
        button("Fazer novamente", false).setOnClickListener { fields.clear(); build() }
    }

    private fun document(title: String, content: String) =
        title + "\n" + "=".repeat(title.length) + "\n" + content
    private fun subtotal() = num("price") * num("quantity")
    private fun total(): Double {
        val discounted = subtotal() * (1 - num("discount").coerceIn(0.0, 100.0) / 100)
        return discounted * (1 + num("tax").coerceAtLeast(0.0) / 100)
    }
    private fun v(key: String) = fields[key]?.text?.toString()?.trim().orEmpty()
    private fun num(key: String) = v(key).replace(",", ".").toDoubleOrNull() ?: 0.0
    private fun money(v: Double) = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(v)

    private fun title() = when (toolId) {
        "business-invoice-maker" -> "Invoice Maker"
        "business-receipt-maker" -> "Receipt Maker"
        "business-quote-maker" -> "Quote Maker"
        "business-profit-calculator" -> "Profit Calculator"
        "business-expense-tracker" -> "Expense Tracker"
        "business-plan" -> "Business Plan"
        "business-proposal" -> "Business Proposal"
        "business-contract-maker" -> "Contract Maker"
        "business-name-generator" -> "Business Name Generator"
        "business-pricing-calculator" -> "Pricing Calculator"
        else -> "Business"
    }

    private fun field(label: String, key: String, value: String = "") {
        text(label)
        val e = EditText(this).apply {
            hint = label; setText(value)
            inputType = InputType.TYPE_CLASS_TEXT
            setTextColor(getColor(R.color.toolnexa_text))
        }
        fields[key] = e
        body.addView(e, LinearLayout.LayoutParams(-1, dp(56)))
    }
    private fun money(label: String, key: String, value: String = "") {
        field(label, key, value)
        fields[key]?.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
    }
    private fun number(label: String, key: String, value: String) {
        field(label, key, value)
        fields[key]?.inputType = InputType.TYPE_CLASS_NUMBER
    }
    private fun text(value: String) {
        body.addView(TextView(this).apply {
            text = value; textSize = 14f
            setTextColor(getColor(R.color.toolnexa_muted))
        })
    }
    private fun button(label: String, primary: Boolean): Button {
        val b = Button(this).apply {
            text = label
            setTextColor(if (primary) Color.WHITE else getColor(R.color.toolnexa_text))
            if (primary) setBackgroundColor(getColor(R.color.toolnexa_blue))
        }
        body.addView(b, LinearLayout.LayoutParams(-1, dp(52)))
        return b
    }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
