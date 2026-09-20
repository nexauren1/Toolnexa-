package com.toolnexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class PlansActivity : Activity() {

    private val auth by lazy {
        FirebaseAuth.getInstance()
    }

    private val bg by lazy {
        getColor(
            R.color.toolnexa_bg
        )
    }

    private val surface by lazy {
        getColor(
            R.color.toolnexa_surface
        )
    }

    private val textColor by lazy {
        getColor(
            R.color.toolnexa_text
        )
    }

    private val muted by lazy {
        getColor(
            R.color.toolnexa_muted
        )
    }

    private val border by lazy {
        getColor(
            R.color.toolnexa_border
        )
    }

    private var currentPlanView:
        TextView? = null

    private var subscriptionView:
        TextView? = null

    private var plansContainer:
        LinearLayout? = null

    private var subscribeButton:
        Button? = null

    private var loadingAccount =
        false

    private var loadingPlans =
        false

    private var activatingSubscription =
        false

    override fun onCreate(
        state: Bundle?
    ) {
        super.onCreate(
            state
        )

        showPlans()
        handlePaymentReturn(
            intent
        )
    }

    override fun onNewIntent(
        intent: Intent
    ) {
        super.onNewIntent(
            intent
        )

        setIntent(
            intent
        )

        handlePaymentReturn(
            intent
        )
    }

    override fun onResume() {
        super.onResume()

        if (
            !activatingSubscription
        ) {
            refreshAccount()
        }

        loadPlansFromServer()
    }

    private fun showPlans() {
        window.statusBarColor =
            bg
        window.navigationBarColor =
            bg

        val root =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setBackgroundColor(bg)
            }

        val scroll =
            ScrollView(this).apply {
                isFillViewport = true
            }

        val body =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    18,
                    20,
                    18,
                    32
                )
            }

        scroll.addView(body)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        setContentView(root)

        val back =
            TextView(this).apply {
                text = "‹  ToolNexa"
                textSize = 15f
                typeface =
                    android.graphics.Typeface
                        .DEFAULT_BOLD
                setTextColor(
                    getColor(
                        R.color.toolnexa_blue
                    )
                )
                setPadding(
                    0,
                    2,
                    0,
                    10
                )
                setOnClickListener {
                    finish()
                }
            }

        body.addView(back)

        val hero =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    20,
                    20,
                    20,
                    20
                )
                background =
                    GradientDrawable().apply {
                        setColor(
                            getColor(
                                R.color.toolnexa_surface
                            )
                        )
                        setStroke(
                            1,
                            getColor(
                                R.color.toolnexa_blue
                            )
                        )
                        cornerRadius =
                            22f
                    }
            }

        hero.addView(
            TextView(this).apply {
                text = "ToolNexa Pro"
                textSize = 28f
                typeface =
                    android.graphics.Typeface
                        .DEFAULT_BOLD
                setTextColor(textColor)
            }
        )

        hero.addView(
            TextView(this).apply {
                text =
                    "Ferramentas mais completas, uma experiência mais fluida."
                textSize = 15f
                setTextColor(muted)
                setPadding(
                    0,
                    7,
                    0,
                    12
                )
            }
        )

        val heroBadge =
            TextView(this).apply {
                text = "ASSINATURA MENSAL"
                textSize = 10.5f
                typeface =
                    android.graphics.Typeface
                        .DEFAULT_BOLD
                setTextColor(
                    getColor(
                        R.color.toolnexa_blue
                    )
                )
                setPadding(
                    10,
                    7,
                    10,
                    7
                )
                background =
                    GradientDrawable().apply {
                        setColor(
                            android.graphics.Color.parseColor(
                                "#EAF0FF"
                            )
                        )
                        cornerRadius =
                            12f
                    }
            }

        hero.addView(
            heroBadge,
            LinearLayout.LayoutParams(
                -2,
                -2
            )
        )

        body.addView(
            hero,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        )

        val accountCard =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setPadding(
                    18,
                    17,
                    18,
                    17
                )
                background =
                    GradientDrawable().apply {
                        setColor(surface)
                        setStroke(
                            1,
                            border
                        )
                        cornerRadius =
                            18f
                    }
            }

        accountCard.addView(
            TextView(this).apply {
                text = "O teu acesso"
                textSize = 16f
                typeface =
                    android.graphics.Typeface
                        .DEFAULT_BOLD
                setTextColor(textColor)
                setPadding(
                    0,
                    0,
                    0,
                    8
                )
            }
        )

        currentPlanView =
            body(
                "Plano atual: a verificar..."
            )

        accountCard.addView(
            currentPlanView
        )

        subscriptionView =
            body(
                "Estado da subscrição: a verificar..."
            )

        accountCard.addView(
            subscriptionView
        )

        val refreshButton =
            Button(this).apply {
                text = "Atualizar estado"
                isAllCaps = false
            }

        styleSecondary(refreshButton)

        refreshButton.setOnClickListener {
            refreshAccount()
            loadPlansFromServer()
        }

        accountCard.addView(
            refreshButton,
            LinearLayout.LayoutParams(
                -1,
                50
            )
        )

        body.addView(
            accountCard,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 22
            }
        )

        body.addView(
            TextView(this).apply {
                text = "Escolhe o teu plano"
                textSize = 22f
                typeface =
                    android.graphics.Typeface
                        .DEFAULT_BOLD
                setTextColor(textColor)
                setPadding(
                    0,
                    0,
                    0,
                    5
                )
            }
        )

        body.addView(
            TextView(this).apply {
                text =
                    "Começa grátis ou desbloqueia os recursos Pro."
                textSize = 14f
                setTextColor(muted)
                setPadding(
                    0,
                    0,
                    0,
                    12
                )
            }
        )

        plansContainer =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        body.addView(
            plansContainer,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        body.addView(
            TextView(this).apply {
                text =
                    "Pagamento processado de forma segura pelo PayPal. A tua identidade continua ligada ao Firebase."
                textSize = 12f
                setTextColor(muted)
                setPadding(
                    4,
                    14,
                    4,
                    0
                )
            }
        )

        showPlansLoading()
    }

    private fun showPlansLoading() {
        plansContainer?.removeAllViews()

        plansContainer?.addView(
            body(
                "A carregar os planos..."
            )
        )
    }

    private fun loadPlansFromServer() {
        if (
            loadingPlans
        ) {
            return
        }

        loadingPlans =
            true

        showPlansLoading()

        Thread {
            try {
                val plans =
                    CloudflareApi.loadPlans()

                runOnUiThread {
                    loadingPlans =
                        false

                    if (
                        plans.isEmpty()
                    ) {
                        showPlansError(
                            "Nenhum plano ativo foi encontrado no billing."
                        )

                        return@runOnUiThread
                    }

                    renderPlans(
                        plans
                    )
                }
            } catch (
                error: Exception
            ) {
                runOnUiThread {
                    loadingPlans =
                        false

                    showPlansError(
                        error.message
                            ?: "Não foi possível carregar os planos."
                    )
                }
            }
        }.start()
    }

    private fun renderPlans(
        plans:
            List<CloudflareApi.PlanInfo>
    ) {
        plansContainer?.removeAllViews()
        subscribeButton = null

        plans.forEach { plan ->
            val isPro =
                plan.code.equals(
                    "PRO",
                    ignoreCase = true
                )

            val interval =
                plan.interval
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        " / " + it.lowercase()
                    }
                    ?: ""

            val price =
                if (
                    plan.priceUsd.isBlank()
                ) {
                    "Preço não definido"
                } else {
                    "US$ " +
                        plan.priceUsd +
                        interval
                }

            val description =
                if (
                    isPro
                ) {
                    "Acesso ao plano Pro do ToolNexa."
                } else {
                    "Plano " +
                        plan.name +
                        " disponibilizado pelo billing."
                }

            plansContainer?.addView(
                planCard(
                    plan,
                    price,
                    description
                ),
                LinearLayout.LayoutParams(
                    -1,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin =
                        14
                }
            )
        }
    }

    private fun showPlansError(
        message: String
    ) {
        plansContainer?.removeAllViews()

        plansContainer?.addView(
            body(
                "Não foi possível carregar os planos."
            )
        )

        plansContainer?.addView(
            body(
                message
            )
        )
    }

    private fun planCard(
        plan: CloudflareApi.PlanInfo,
        price: String,
        description: String
    ): LinearLayout {
        val isPro =
            plan.code.equals(
                "PRO",
                ignoreCase = true
            )

        return LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL

            setPadding(
                18,
                18,
                18,
                18
            )

            background =
                GradientDrawable().apply {
                    setColor(surface)
                    setStroke(
                        if (isPro) 2 else 1,
                        if (isPro) {
                            getColor(
                                R.color.toolnexa_blue
                            )
                        } else {
                            border
                        }
                    )
                    cornerRadius =
                        20f
                }

            val top =
                LinearLayout(
                    this@PlansActivity
                ).apply {
                    orientation =
                        LinearLayout.HORIZONTAL
                    gravity =
                        android.view.Gravity.CENTER_VERTICAL
                }

            top.addView(
                TextView(
                    this@PlansActivity
                ).apply {
                    text =
                        plan.name.ifBlank {
                            plan.code
                        }
                    textSize = 21f
                    typeface =
                        android.graphics.Typeface
                            .DEFAULT_BOLD
                    setTextColor(textColor)
                },
                LinearLayout.LayoutParams(
                    0,
                    -2,
                    1f
                )
            )

            top.addView(
                TextView(
                    this@PlansActivity
                ).apply {
                    text =
                        if (isPro) {
                            "PRO"
                        } else {
                            "FREE"
                        }
                    textSize = 10.5f
                    typeface =
                        android.graphics.Typeface
                            .DEFAULT_BOLD
                    setTextColor(
                        if (isPro) {
                            android.graphics.Color.WHITE
                        } else {
                            getColor(
                                R.color.toolnexa_blue
                            )
                        }
                    )
                    setPadding(
                        10,
                        6,
                        10,
                        6
                    )
                    background =
                        GradientDrawable().apply {
                            setColor(
                                if (isPro) {
                                    getColor(
                                        R.color.toolnexa_blue
                                    )
                                } else {
                                    android.graphics.Color.parseColor(
                                        "#EAF0FF"
                                    )
                                }
                            )
                            cornerRadius =
                                11f
                        }
                }
            )

            addView(
                top,
                LinearLayout.LayoutParams(
                    -1,
                    48
                )
            )

            addView(
                TextView(
                    this@PlansActivity
                ).apply {
                    text =
                        if (isPro) {
                            "Mais recursos para quem quer usar o ToolNexa a sério."
                        } else {
                            "O essencial para começar a explorar o ToolNexa."
                        }
                    textSize = 13.5f
                    setTextColor(muted)
                    setPadding(
                        0,
                        4,
                        0,
                        12
                    )
                }
            )

            addView(
                TextView(
                    this@PlansActivity
                ).apply {
                    text = price
                    textSize = 27f
                    typeface =
                        android.graphics.Typeface
                            .DEFAULT_BOLD
                    setTextColor(textColor)
                }
            )

            if (isPro) {
                addView(
                    TextView(
                        this@PlansActivity
                    ).apply {
                        text =
                            "Renovação automática mensal"
                        textSize = 12f
                        setTextColor(muted)
                        setPadding(
                            0,
                            2,
                            0,
                            13
                        )
                    }
                )
            } else {
                addView(
                    TextView(
                        this@PlansActivity
                    ).apply {
                        text =
                            "Sem compromisso"
                        textSize = 12f
                        setTextColor(muted)
                        setPadding(
                            0,
                            2,
                            0,
                            13
                        )
                    }
                )
            }

            val features =
                if (isPro) {
                    listOf(
                        "Tudo do plano Free",
                        "Ferramentas e recursos Pro",
                        "Novos recursos Pro",
                        "Acesso contínuo enquanto ativo"
                    )
                } else {
                    listOf(
                        "Ferramentas Free",
                        "Conta Firebase protegida",
                        "Histórico do ToolNexa",
                        "Atualizações do aplicativo"
                    )
                }

            features.forEach { feature ->
                addView(
                    TextView(
                        this@PlansActivity
                    ).apply {
                        text =
                            "✓  " + feature
                        textSize = 13.5f
                        setTextColor(textColor)
                        setPadding(
                            0,
                            3,
                            0,
                            7
                        )
                    }
                )
            }

            if (isPro) {
                subscribeButton =
                    Button(
                        this@PlansActivity
                    ).apply {
                        text = "Assinar Pro"
                        isAllCaps = false
                        minHeight = 54
                        setTextColor(
                            android.graphics.Color.WHITE
                        )
                        background =
                            GradientDrawable().apply {
                                setColor(
                                    getColor(
                                        R.color.toolnexa_blue
                                    )
                                )
                                cornerRadius =
                                    14f
                            }
                        stateListAnimator =
                            null
                        setOnClickListener {
                            startProSubscription()
                        }
                    }

                addView(
                    subscribeButton,
                    LinearLayout.LayoutParams(
                        -1,
                        54
                    ).apply {
                        topMargin = 8
                    }
                )
            } else {
                addView(
                    TextView(
                        this@PlansActivity
                    ).apply {
                        text =
                            "Plano atual sem mensalidade"
                        textSize = 12f
                        setTextColor(
                            getColor(
                                R.color.toolnexa_muted
                            )
                        )
                        setPadding(
                            0,
                            10,
                            0,
                            0
                        )
                    }
                )
            }
        }
    }

    private fun refreshAccount() {
        if (
            loadingAccount ||
            activatingSubscription
        ) {
            return
        }

        val user =
            auth.currentUser

        if (
            user == null
        ) {
            currentPlanView?.text =
                "Plano atual: Free"

            subscriptionView?.text =
                "Estado da subscrição: sessão não iniciada"

            return
        }

        loadingAccount =
            true

        currentPlanView?.text =
            "Plano atual: a verificar..."

        subscriptionView?.text =
            "Estado da subscrição: a verificar..."

        user.getIdToken(
            false
        ).addOnCompleteListener { task ->
            val token =
                task.result?.token

            if (
                !task.isSuccessful ||
                token.isNullOrBlank()
            ) {
                loadingAccount =
                    false

                showAccountError(
                    "Não foi possível validar a sessão Firebase."
                )

                return@addOnCompleteListener
            }

            Thread {
                try {
                    val account =
                        CloudflareApi.loadAccount(
                            token
                        )

                    runOnUiThread {
                        loadingAccount =
                            false

                        renderAccount(
                            account
                        )
                    }
                } catch (
                    error: Exception
                ) {
                    runOnUiThread {
                        loadingAccount =
                            false

                        showAccountError(
                            error.message
                        )
                    }
                }
            }.start()
        }
    }

    private fun renderAccount(
        account:
            CloudflareApi.AccountInfo
    ) {
        currentPlanView?.text =
            "Plano atual: " +
                account.planName +
                " (US$ " +
                account.priceUsd +
                ")"

        subscriptionView?.text =
            "Estado da subscrição: " +
                (
                    account.subscriptionStatus
                        ?: "Sem subscrição"
                )

        val isPro =
            account.planCode.equals(
                "PRO",
                ignoreCase = true
            )

        subscribeButton?.apply {
            isEnabled = !isPro
            text =
                if (isPro) {
                    "Plano atual"
                } else {
                    "Assinar Pro"
                }
        }
    }

    private fun showAccountError(
        message: String? =
            null
    ) {
        currentPlanView?.text =
            "Plano atual: não foi possível verificar"

        subscriptionView?.text =
            message?.takeIf {
                it.isNotBlank()
            }
                ?: "Verifique a internet e tente novamente."

        if (
            auth.currentUser != null
        ) {
            subscribeButton?.isEnabled =
                true
        }
    }

    private fun startProSubscription() {
        val user =
            auth.currentUser

        if (
            user == null
        ) {
            toast(
                "Inicie sessão antes de assinar o Pro."
            )
            return
        }

        subscribeButton?.isEnabled =
            false

        subscriptionView?.text =
            "Estado da subscrição: a preparar PayPal..."

        user.getIdToken(
            false
        ).addOnCompleteListener { task ->
            val token =
                task.result?.token

            if (
                !task.isSuccessful ||
                token.isNullOrBlank()
            ) {
                subscribeButton?.isEnabled =
                    true

                toast(
                    "Não foi possível validar a conta Firebase."
                )

                return@addOnCompleteListener
            }

            Thread {
                try {
                    val subscription =
                        CloudflareApi
                            .createProSubscription(
                                token
                            )

                    runOnUiThread {
                        subscribeButton?.isEnabled =
                            true

                        subscriptionView?.text =
                            "Estado da subscrição: aguardando aprovação no PayPal..."

                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(
                                    subscription.approvalUrl
                                )
                            )
                        )
                    }
                } catch (
                    error: Exception
                ) {
                    runOnUiThread {
                        subscribeButton?.isEnabled =
                            true

                        subscriptionView?.text =
                            "Estado da subscrição: erro ao iniciar"

                        toast(
                            error.message
                                ?: "Não foi possível iniciar o PayPal Sandbox."
                        )
                    }
                }
            }.start()
        }
    }

    private fun handlePaymentReturn(
        incoming:
            Intent?
    ) {
        val data =
            incoming?.data
                ?: return

        if (
            data.scheme !=
                "toolnexa" ||
            data.host !=
                "paypal"
        ) {
            return
        }

        if (
            data.path ==
                "/cancel"
        ) {
            toast(
                "A assinatura Pro foi cancelada."
            )

            refreshAccount()
            return
        }

        if (
            data.path !=
                "/complete"
        ) {
            return
        }

        val subscriptionId =
            data.getQueryParameter(
                "subscription_id"
            ).orEmpty()

        if (
            subscriptionId.isBlank()
        ) {
            toast(
                "O PayPal voltou sem o ID da assinatura."
            )

            return
        }

        activateReturnedSubscription(
            subscriptionId
        )
    }

    private fun activateReturnedSubscription(
        subscriptionId: String
    ) {
        if (
            activatingSubscription
        ) {
            return
        }

        val user =
            auth.currentUser
                ?: return

        activatingSubscription =
            true

        subscribeButton?.isEnabled =
            false

        currentPlanView?.text =
            "Plano atual: a ativar..."

        subscriptionView?.text =
            "Estado da subscrição: a confirmar com PayPal..."

        user.getIdToken(
            false
        ).addOnCompleteListener { task ->
            val token =
                task.result?.token

            if (
                !task.isSuccessful ||
                token.isNullOrBlank()
            ) {
                activatingSubscription =
                    false

                showAccountError(
                    "Não foi possível validar a sessão para ativar a assinatura."
                )

                return@addOnCompleteListener
            }

            Thread {
                try {
                    val account =
                        CloudflareApi
                            .activateProSubscription(
                                token,
                                subscriptionId
                            )

                    runOnUiThread {
                        activatingSubscription =
                            false

                        renderAccount(
                            account
                        )

                        toast(
                            "Plano Pro ativado com sucesso."
                        )

                        loadPlansFromServer()
                    }
                } catch (
                    error: Exception
                ) {
                    runOnUiThread {
                        activatingSubscription =
                            false

                        subscribeButton?.isEnabled =
                            true

                        subscriptionView?.text =
                            error.message
                                ?: "A assinatura ainda não pôde ser ativada."

                        toast(
                            error.message
                                ?: "Não foi possível ativar a assinatura."
                        )
                    }
                }
            }.start()
        }
    }

    private fun styleSecondary(
        button: Button
    ) {
        button.setTextColor(
            getColor(
                R.color.toolnexa_blue
            )
        )

        button.background =
            GradientDrawable().apply {
                setColor(
                    android.graphics.Color.TRANSPARENT
                )

                setStroke(
                    1,
                    getColor(
                        R.color.toolnexa_blue
                    )
                )

                cornerRadius =
                    14f
            }

        button.stateListAnimator =
            null
    }

    private fun toast(
        message: String
    ) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun title(
        value: String,
        size: Float
    ): TextView =
        TextView(this).apply {
            text =
                value

            textSize =
                size

            typeface =
                android.graphics.Typeface
                    .DEFAULT_BOLD

            setTextColor(
                textColor
            )

            setPadding(
                0,
                6,
                0,
                8
            )
        }

    private fun body(
        value: String
    ): TextView =
        TextView(this).apply {
            text =
                value

            textSize =
                14f

            setTextColor(
                muted
            )

            setPadding(
                0,
                4,
                0,
                12
            )
        }
}
