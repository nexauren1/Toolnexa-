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
        getColor(R.color.toolnexa_bg)
    }

    private val surface by lazy {
        getColor(R.color.toolnexa_surface)
    }

    private val textColor by lazy {
        getColor(R.color.toolnexa_text)
    }

    private val muted by lazy {
        getColor(R.color.toolnexa_muted)
    }

    private val border by lazy {
        getColor(R.color.toolnexa_border)
    }

    private var currentPlanView: TextView? = null
    private var subscriptionView: TextView? = null
    private var plansContainer: LinearLayout? = null
    private var subscribeButton: Button? = null
    private var loadingAccount = false
    private var loadingPlans = false

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        showPlans()
    }

    override fun onResume() {
        super.onResume()
        refreshAccount()
        loadPlansFromServer()
    }

    private fun showPlans() {
        val root =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
                setBackgroundColor(bg)
                setPadding(
                    18,
                    24,
                    18,
                    30
                )
            }

        val scroll =
            ScrollView(this)

        scroll.addView(root)
        setContentView(scroll)

        root.addView(
            title(
                "Planos do ToolNexa",
                29f
            )
        )

        root.addView(
            body(
                "Os planos e preços são carregados do sistema de billing do ToolNexa."
            )
        )

        val accountCard =
            LinearLayout(this).apply {
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
                            1,
                            border
                        )
                        cornerRadius = 18f
                    }
            }

        accountCard.addView(
            title(
                "A sua conta",
                20f
            )
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

                setOnClickListener {
                    refreshAccount()
                    loadPlansFromServer()
                }
            }

        styleSecondary(
            refreshButton
        )

        accountCard.addView(
            refreshButton,
            LinearLayout.LayoutParams(
                -1,
                52
            )
        )

        root.addView(
            accountCard,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        )

        root.addView(
            title(
                "Planos disponíveis",
                22f
            )
        )

        plansContainer =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        root.addView(
            plansContainer,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
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
        if (loadingPlans) {
            return
        }

        loadingPlans = true
        showPlansLoading()

        Thread {
            try {
                val plans =
                    CloudflareApi.loadPlans()

                runOnUiThread {
                    loadingPlans = false

                    if (plans.isEmpty()) {
                        showPlansError(
                            "Nenhum plano ativo foi encontrado no billing."
                        )
                        return@runOnUiThread
                    }

                    renderPlans(
                        plans
                    )
                }
            } catch (error: Exception) {
                runOnUiThread {
                    loadingPlans = false

                    showPlansError(
                        error.message
                            ?: "Não foi possível carregar os planos."
                    )
                }
            }
        }.start()
    }

    private fun renderPlans(
        plans: List<CloudflareApi.PlanInfo>
    ) {
        plansContainer?.removeAllViews()
        subscribeButton = null

        plans.forEach { plan ->
            val isPro =
                plan.code.equals(
                    "pro",
                    ignoreCase = true
                )

            val interval =
                plan.interval
                    .takeIf {
                        it.isNotBlank()
                    }
                    ?.let {
                        " / $it"
                    }
                    ?: ""

            val price =
                if (
                    plan.priceUsd
                        .isBlank()
                ) {
                    "Preço não definido"
                } else {
                    "US$ " +
                        plan.priceUsd +
                        interval
                }

            val description =
                if (isPro) {
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
                    bottomMargin = 14
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
    ): LinearLayout =
        LinearLayout(this).apply {
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
                        1,
                        border
                    )
                    cornerRadius = 18f
                }

            addView(
                title(
                    plan.name.ifBlank {
                        plan.code
                    },
                    21f
                )
            )

            addView(
                body(
                    price
                )
            )

            addView(
                body(
                    description
                )
            )

            if (
                plan.code.equals(
                    "pro",
                    ignoreCase = true
                )
            ) {
                subscribeButton =
                    Button(
                        this@PlansActivity
                    ).apply {
                        text =
                            "Assinar Pro"

                        minHeight =
                            54

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

                        setOnClickListener {
                            startProSubscription()
                        }
                    }

                addView(
                    subscribeButton,
                    LinearLayout.LayoutParams(
                        -1,
                        54
                    )
                )
            }
        }

    private fun refreshAccount() {
        if (loadingAccount) {
            return
        }

        val user =
            auth.currentUser

        if (user == null) {
            currentPlanView?.text =
                "Plano atual: Free"

            subscriptionView?.text =
                "Estado da subscrição: sessão não iniciada"

            subscribeButton?.isEnabled =
                false

            return
        }

        loadingAccount = true

        currentPlanView?.text =
            "Plano atual: a verificar..."

        subscriptionView?.text =
            "Estado da subscrição: a verificar..."

        user.getIdToken(true)
            .addOnCompleteListener { task ->
                val token =
                    task.result?.token

                if (
                    !task.isSuccessful ||
                    token.isNullOrBlank()
                ) {
                    loadingAccount = false
                    showAccountError()
                    return@addOnCompleteListener
                }

                Thread {
                    try {
                        val account =
                            CloudflareApi
                                .loadAccount(
                                    token
                                )

                        runOnUiThread {
                            loadingAccount =
                                false

                            currentPlanView?.text =
                                "Plano atual: " +
                                    account.planName +
                                    " (US$ " +
                                    account.priceUsd +
                                    ")"

                            subscriptionView?.text =
                                "Estado da subscrição: " +
                                    (
                                        account
                                            .subscriptionStatus
                                            ?: "Sem subscrição"
                                    )

                            subscribeButton?.isEnabled =
                                account.planCode !=
                                    "pro"
                        }
                    } catch (error: Exception) {
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

    private fun showAccountError(
        message: String? = null
    ) {
        currentPlanView?.text =
            "Plano atual: não foi possível verificar"

        subscriptionView?.text =
            message?.takeIf {
                it.isNotBlank()
            }
                ?: "Verifique a internet e tente novamente."
    }

    private fun startProSubscription() {
        val user =
            auth.currentUser

        if (user == null) {
            toast(
                "Inicie sessão antes de assinar o Pro."
            )
            return
        }

        subscribeButton?.isEnabled =
            false

        subscriptionView?.text =
            "Estado da subscrição: a iniciar PayPal..."

        user.getIdToken(true)
            .addOnCompleteListener { task ->
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
                        val approvalUrl =
                            CloudflareApi
                                .createProSubscription(
                                    token
                                )

                        runOnUiThread {
                            subscribeButton?.isEnabled =
                                true

                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(
                                        approvalUrl
                                    )
                                )
                            )
                        }
                    } catch (error: Exception) {
                        runOnUiThread {
                            subscribeButton?.isEnabled =
                                true

                            subscriptionView?.text =
                                "Estado da subscrição: erro ao iniciar"

                            toast(
                                error.message
                                    ?: "Não foi possível iniciar o PayPal."
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
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun title(
        value: String,
        size: Float
    ): TextView =
        TextView(this).apply {
            text = value
            textSize = size

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
            text = value
            textSize = 14f

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
