package com.toolnexa.app

import android.Manifest
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Space
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private lateinit var root: FrameLayout
    private lateinit var content: LinearLayout
    private lateinit var drawer: LinearLayout
    private lateinit var drawerShade: View
    private lateinit var toolbarTitle: TextView
    private lateinit var updateManager: UpdateManager

    private val compressor by lazy {
        ImageCompressor(this)
    }

    private var selectedImage: Uri? = null
    private var selectedResizeImage: Uri? = null
    private var compressorPreview: ImageView? = null
    private var resizerPreview: ImageView? = null
    private var qualityValue = 82

    private val auth by lazy {
        FirebaseAuth.getInstance()
    }

    private val analytics by lazy {
        AnalyticsTracker(this)
    }

    private val notificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            analytics.event(
                "notification_permission_result",
                "granted" to granted.toString()
            )
        }

    private val blue by lazy {
        getColor(R.color.toolnexa_blue)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = surface
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        buildShell()
        updateManager = UpdateManager(this)
        analytics.screen("home")

        showHome()
        setupNotifications()
        updateManager.checkForUpdate()
    }

    private fun setupNotifications() {
        analytics.event("notification_setup_start")

        com.google.firebase.messaging.FirebaseMessaging
            .getInstance()
            .subscribeToTopic("app_updates")
            .addOnCompleteListener { task ->
                analytics.event(
                    "notification_topic_subscription",
                    "success" to task.isSuccessful.toString()
                )
            }

        if (
            android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            val granted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!granted) {
                analytics.event(
                    "notification_permission_request"
                )
                notificationPermission.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (::updateManager.isInitialized) {
            updateManager.retryInstallAfterSettings()
        }
    }

    private fun buildShell() {
        root = FrameLayout(this)
        root.setBackgroundColor(bg)

        val body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL

        val toolbar = LinearLayout(this)
        toolbar.orientation = LinearLayout.HORIZONTAL
        toolbar.gravity = Gravity.CENTER_VERTICAL
        toolbar.setPadding(
            dp(8),
            dp(8),
            dp(14),
            dp(8)
        )
        toolbar.background = solid(surface)

        val menuButton = ImageButton(this)
        menuButton.setImageResource(
            android.R.drawable.ic_menu_sort_by_size
        )
        menuButton.setColorFilter(blue)
        menuButton.background = transparent()
        menuButton.contentDescription = "Abrir menu"
        menuButton.setOnClickListener {
            openDrawer()
        }

        toolbar.addView(
            menuButton,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

        toolbarTitle = TextView(this)
        toolbarTitle.text = "ToolNexa"
        toolbarTitle.textSize = 19f
        toolbarTitle.setTextColor(textColor)
        toolbarTitle.gravity = Gravity.CENTER_VERTICAL
        toolbarTitle.typeface =
            android.graphics.Typeface.DEFAULT_BOLD

        toolbar.addView(
            toolbarTitle,
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            )
        )

        val accountButton = ImageButton(this)
        accountButton.setImageResource(
            android.R.drawable.ic_menu_myplaces
        )
        accountButton.setColorFilter(muted)
        accountButton.background = transparent()
        accountButton.contentDescription = "Conta"
        accountButton.setOnClickListener {
            showAccount()
        }

        toolbar.addView(
            accountButton,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

        body.addView(
            toolbar,
            LinearLayout.LayoutParams(-1, dp(64))
        )

        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL

        val scroll = ScrollView(this)
        scroll.addView(content)
        scroll.isFillViewport = true

        body.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        root.addView(
            body,
            FrameLayout.LayoutParams(-1, -1)
        )

        drawerShade = View(this)
        drawerShade.setBackgroundColor(
            Color.argb(90, 0, 0, 0)
        )
        drawerShade.visibility = View.GONE
        drawerShade.setOnClickListener {
            closeDrawer()
        }

        root.addView(
            drawerShade,
            FrameLayout.LayoutParams(-1, -1)
        )

        drawer = LinearLayout(this)
        drawer.orientation = LinearLayout.VERTICAL
        drawer.setPadding(
            dp(18),
            dp(24),
            dp(18),
            dp(18)
        )
        drawer.background = solid(surface)
        drawer.elevation = dp(10).toFloat()
        drawer.translationX = -dp(320).toFloat()

        root.addView(
            drawer,
            FrameLayout.LayoutParams(
                dp(320),
                -1,
                Gravity.START
            )
        )

        setContentView(root)
        buildDrawer()
    }

    private fun buildDrawer() {
        drawer.removeAllViews()

        val brand = TextView(this)
        brand.text = "ToolNexa"
        brand.textSize = 24f
        brand.typeface =
            android.graphics.Typeface.DEFAULT_BOLD
        brand.setTextColor(textColor)
        drawer.addView(brand)

        val subtitle = TextView(this)
        subtitle.text = "Useful tools. One app."
        subtitle.textSize = 13f
        subtitle.setTextColor(muted)
        drawer.addView(
            subtitle,
            LinearLayout.LayoutParams(-1, dp(40))
        )

        drawer.addView(space(8))

        drawerItem(
            "Início",
            android.R.drawable.ic_menu_view
        ) {
            showHome()
            closeDrawer()
        }

        drawerItem(
            "Ferramentas",
            android.R.drawable.ic_menu_manage
        ) {
            showTools()
            closeDrawer()
        }

        drawerItem(
            "Conta",
            android.R.drawable.ic_menu_myplaces
        ) {
            showAccount()
            closeDrawer()
        }

        drawerItem(
            "Definições",
            android.R.drawable.ic_menu_preferences
        ) {
            showSettings()
            closeDrawer()
        }

        drawerItem(
            "Sobre",
            android.R.drawable.ic_menu_info_details
        ) {
            showAbout()
            closeDrawer()
        }

        drawer.addView(space(8))

        val version = TextView(this)
        version.text = "v" + BuildConfig.VERSION_NAME
        version.textSize = 12f
        version.setTextColor(muted)
        drawer.addView(version)
    }


    private fun addSectionCard(titleText: String, subtitle: String) {
        val box = card()
        box.setPadding(dp(16), dp(14), dp(16), dp(14))
        box.addView(title(titleText, 18f))
        box.addView(bodyText(subtitle))
        content.addView(
            box,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(16), dp(8), dp(16), dp(8))
            }
        )
    }

    private fun addFeatureCard(
        titleText: String,
        description: String,
        badge: String
    ) {
        val box = card()
        box.setPadding(dp(16), dp(16), dp(16), dp(16))
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        texts.addView(title(titleText, 17f))
        texts.addView(bodyText(description))
        row.addView(
            texts,
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        val badgeView = TextView(this).apply {
            text = badge
            textSize = 11f
            setTextColor(blue)
            setPadding(dp(9), dp(5), dp(9), dp(5))
            background = GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#EAF0FF"))
                cornerRadius = dp(10).toFloat()
            }
        }
        row.addView(badgeView)
        box.addView(row)
        content.addView(
            box,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(16), dp(6), dp(16), dp(6))
            }
        )
    }

    private fun showHome() {
        toolbarTitle.text = "Início"
        content.removeAllViews()
        analytics.screen("home")

        content.addView(space(18))
        content.addView(
            title(
                "Ferramentas simples, resultados claros",
                27f
            )
        )
        content.addView(
            bodyText(
                "Escolha uma ferramenta, processe o ficheiro e veja o resultado antes de guardar."
            )
        )
        content.addView(space(14))

        addCategoryCard(
            "Imagem",
            "Otimização, tamanho e conversão de imagens",
            R.drawable.ic_tool_compress
        ) {
            showCategory("Imagem")
        }
        addHomeToolsGrid()

        addSectionCard(
            "Em breve",
            "Novas ferramentas para documentos e produtividade"
        )
        addFeatureCard(
            "Conversor de imagens",
            "JPG, PNG e WebP com preview e preservação de qualidade.",
            "Planeado"
        )
        addFeatureCard(
            "Histórico",
            "Aceda rapidamente aos resultados guardados no Nexauren X.",
            "Planeado"
        )

        content.addView(space(18))

        val info = card()
        info.setPadding(
            dp(18),
            dp(18),
            dp(18),
            dp(18)
        )
        info.addView(
            title("ToolNexa", 18f)
        )
        info.addView(
            bodyText(
                "Versão " +
                    BuildConfig.VERSION_NAME +
                    " • Firebase Analytics ativo • conta com Google e e-mail"
            )
        )

        content.addView(
            info,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    dp(16),
                    0,
                    dp(16),
                    0
                )
            }
        )
        content.addView(space(30))
    }

    private fun addHomeToolsGrid() {
        val search = EditText(this)
        search.hint = "Pesquisar ferramenta"
        search.setSingleLine(true)
        search.textSize = 15f
        search.setTextColor(textColor)
        search.setHintTextColor(muted)
        search.setPadding(
            dp(16),
            dp(12),
            dp(16),
            dp(12)
        )
        search.background = GradientDrawable().apply {
            setColor(surface)
            setStroke(dp(1), border)
            cornerRadius = dp(14).toFloat()
        }

        content.addView(
            search,
            LinearLayout.LayoutParams(
                -1,
                dp(54)
            ).apply {
                setMargins(
                    dp(16),
                    0,
                    dp(16),
                    dp(14)
                )
            }
        )

        val grid = LinearLayout(this)
        grid.orientation = LinearLayout.VERTICAL
        content.addView(
            grid,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        fun render(query: String) {
            grid.removeAllViews()

            val tools = listOf(
                ToolDefinition(
                    "Image Compressor",
                    "Imagem",
                    "Reduza o tamanho da imagem com qualidade ajustável.",
                    R.drawable.ic_tool_compress
                ),
                ToolDefinition(
                    "Image Resizer",
                    "Imagem",
                    "Redimensione a imagem e veja a nova prévia.",
                    R.drawable.ic_tool_resize
                ),
                ToolDefinition(
                    "Image Converter",
                    "Imagem",
                    "Converta imagens para JPG, PNG ou WebP.",
                    R.drawable.ic_tool_resize
                )
            ).filter {
                it.name.contains(query.trim(), true) ||
                    it.category.contains(query.trim(), true)
            }

            if (tools.isEmpty()) {
                grid.addView(
                    bodyText(
                        "Nenhuma ferramenta encontrada."
                    )
                )
                return
            }

            for (index in tools.indices step 2) {
                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL

                addToolGridCard(
                    row,
                    tools[index]
                )

                if (index + 1 < tools.size) {
                    addToolGridCard(
                        row,
                        tools[index + 1]
                    )
                } else {
                    row.addView(
                        Space(this),
                        LinearLayout.LayoutParams(
                            0,
                            dp(150),
                            1f
                        ).apply {
                            setMargins(
                                dp(6),
                                0,
                                dp(6),
                                0
                            )
                        }
                    )
                }

                grid.addView(
                    row,
                    LinearLayout.LayoutParams(
                        -1,
                        dp(150)
                    )
                )
            }
        }

        search.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    analytics.event(
                        "tool_search",
                        "query" to (s?.toString() ?: "")
                    )
                    render(s?.toString() ?: "")
                }

                override fun afterTextChanged(
                    s: Editable?
                ) = Unit
            }
        )

        render("")
    }

    private fun addToolGridCard(
        row: LinearLayout,
        tool: ToolDefinition
    ) {
        val item = card()
        item.setPadding(
            dp(14),
            dp(13),
            dp(14),
            dp(12)
        )

        val icon = ImageView(this)
        icon.setImageResource(tool.icon)
        icon.setColorFilter(blue)
        icon.contentDescription = tool.name

        item.addView(
            icon,
            LinearLayout.LayoutParams(
                dp(34),
                dp(34)
            )
        )

        val name = TextView(this)
        name.text = tool.name
        name.textSize = 15f
        name.typeface =
            android.graphics.Typeface.DEFAULT_BOLD
        name.setTextColor(textColor)
        name.setPadding(0, dp(5), 0, 0)
        item.addView(name)

        val desc = TextView(this)
        desc.text = tool.description
        desc.textSize = 12f
        desc.setTextColor(muted)
        item.addView(desc)

        item.setOnClickListener {
            analytics.event(
                "tool_open",
                "tool_name" to tool.name
            )

            when (tool.name) {
                "Image Compressor" ->
                    showImageCompressor()

                "Image Resizer" ->
                    showImageResizer()

                "Image Converter" ->
                    showImageConverter()
            }
        }

        row.addView(
            item,
            LinearLayout.LayoutParams(
                0,
                dp(150),
                1f
            ).apply {
                setMargins(
                    dp(6),
                    dp(0),
                    dp(6),
                    dp(0)
                )
            }
        )
    }


    private fun addCategoryCard(
        name: String,
        description: String,
        icon: Int,
        action: () -> Unit
    ) {
        val box = card()
        box.setPadding(dp(16), dp(14), dp(16), dp(14))
        box.alpha = 0f
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val image = ImageView(this).apply {
            setImageResource(icon)
            setColorFilter(blue)
            contentDescription = name
        }
        row.addView(
            image,
            LinearLayout.LayoutParams(dp(44), dp(44))
        )
        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        texts.addView(title(name, 18f))
        texts.addView(bodyText(description))
        row.addView(
            texts,
            LinearLayout.LayoutParams(0, -2, 1f)
        )
        val arrow = TextView(this).apply {
            text = "›"
            textSize = 30f
            setTextColor(blue)
        }
        row.addView(arrow)
        box.addView(row)
        box.setOnClickListener {
            analytics.event("category_open", "category" to name)
            action()
        }
        content.addView(
            box,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(16), dp(8), dp(16), dp(8))
            }
        )
        box.animate().alpha(1f).setDuration(260).start()
    }

    private fun showCategory(category: String) {
        startActivity(
            Intent(this, CategoryActivity::class.java).apply {
                putExtra("category", category)
            }
        )
    }

    private fun showImageConverter() {
        analytics.event("tool_open_workflow", "tool" to "converter")
        startActivity(
            Intent(this, ToolWorkflowActivity::class.java).apply {
                putExtra("tool", "converter")
            }
        )
    }

    private fun showTools() {
        toolbarTitle.text = "Ferramentas"
        content.removeAllViews()
        analytics.screen("tools")

        content.addView(space(18))
        content.addView(
            title("Todas as ferramentas", 25f)
        )
        content.addView(
            bodyText(
                "Pesquise e abra qualquer ferramenta disponível no ToolNexa."
            )
        )
        addHomeToolsGrid()
        content.addView(space(28))
    }


    private fun showImageCompressor() {
        analytics.event("tool_open_workflow", "tool" to "compressor")
        startActivity(
            Intent(this, ToolWorkflowActivity::class.java).apply {
                putExtra("tool", "compressor")
            }
        )
    }

    private fun showImageResizer() {
        analytics.event("tool_open_workflow", "tool" to "resizer")
        startActivity(
            Intent(this, ToolWorkflowActivity::class.java).apply {
                putExtra("tool", "resizer")
            }
        )
    }

    private fun showAccount() {
        toolbarTitle.text = "Conta"
        content.removeAllViews()
        analytics.screen("account")

        val user = auth.currentUser

        content.addView(space(18))

        if (user == null) {
            val account = card()
            account.setPadding(
                dp(20),
                dp(22),
                dp(20),
                dp(22)
            )

            account.addView(
                title("A tua conta ToolNexa", 24f)
            )
            account.addView(
                bodyText(
                    "Entra com Google ou e-mail para sincronizar a tua identidade no ToolNexa."
                )
            )

            val login = Button(this)
            login.text = "Entrar / Criar conta"
            stylePrimary(login)
            login.setOnClickListener {
                analytics.event("login_screen_open")
                startActivityForResult(
                    Intent(
                        this,
                        LoginActivity::class.java
                    ),
                    REQUEST_LOGIN
                )
            }
            account.addView(login)

            content.addView(
                account,
                LinearLayout.LayoutParams(
                    -1,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(
                        dp(16),
                        0,
                        dp(16),
                        0
                    )
                }
            )
            return
        }

        analytics.setUser(user)

        val profile = card()
        profile.setPadding(
            dp(20),
            dp(22),
            dp(20),
            dp(22)
        )

        val avatar = ImageView(this)
        avatar.setImageResource(
            android.R.drawable.ic_menu_myplaces
        )
        avatar.setColorFilter(blue)

        profile.addView(
            avatar,
            LinearLayout.LayoutParams(
                dp(54),
                dp(54)
            )
        )

        profile.addView(
            title(
                user.displayName ?: "Utilizador ToolNexa",
                24f
            )
        )

        profile.addView(
            bodyText(
                user.email ?: "Sem e-mail disponível"
            )
        )

        val status = TextView(this)
        status.text = "CONTA ATIVA"
        status.textSize = 12f
        status.typeface =
            android.graphics.Typeface.DEFAULT_BOLD
        status.setTextColor(blue)
        profile.addView(status)

        content.addView(
            profile,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    dp(16),
                    0,
                    dp(16),
                    0
                )
            }
        )

        val data = card()
        data.setPadding(
            dp(18),
            dp(18),
            dp(18),
            dp(18)
        )

        val providers =
            user.providerData
                .filter { it.providerId.isNotBlank() }
                .joinToString(", ") {
                    when (it.providerId) {
                        "google.com" -> "Google"
                        "password" -> "E-mail / senha"
                        else -> it.providerId
                    }
                }
                .ifBlank { "—" }

        fun dateText(
            timestamp: Long?
        ): String {
            if (timestamp == null || timestamp <= 0L) {
                return "—"
            }

            return java.text.SimpleDateFormat(
                "dd/MM/yyyy HH:mm",
                java.util.Locale.getDefault()
            ).format(
                java.util.Date(timestamp)
            )
        }

        data.addView(
            title("Dados da conta", 18f)
        )
        data.addView(
            bodyText(
                "UID: " + user.uid +
                    "\nNome: " +
                    (user.displayName ?: "—") +
                    "\nE-mail: " +
                    (user.email ?: "—") +
                    "\nTelefone: " +
                    (user.phoneNumber ?: "—") +
                    "\nProvedores: " +
                    providers +
                    "\nE-mail verificado: " +
                    if (user.isEmailVerified) "Sim" else "Não" +
                    "\nCriada em: " +
                    dateText(
                        user.metadata?.creationTimestamp
                    ) +
                    "\nÚltimo acesso: " +
                    dateText(
                        user.metadata?.lastSignInTimestamp
                    )
            )
        )

        content.addView(
            data,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    dp(16),
                    dp(14),
                    dp(16),
                    0
                )
            }
        )

        val logout = Button(this)
        logout.text = "Terminar sessão"
        styleSecondary(logout)
        logout.setOnClickListener {
            analytics.event("logout")
            auth.signOut()
            analytics.clearUser()
            toast("Sessão terminada.")
            showAccount()
        }

        content.addView(
            logout,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            ).apply {
                setMargins(
                    dp(16),
                    dp(16),
                    dp(16),
                    0
                )
            }
        )
    }

    private fun showSettings() {
        toolbarTitle.text = "Definições"
        content.removeAllViews()
        analytics.screen("settings")

        content.addView(space(18))

        val card = card()
        card.setPadding(
            dp(18),
            dp(14),
            dp(18),
            dp(14)
        )

        val auto = Switch(this)
        auto.text =
            "Verificar atualizações automaticamente"
        auto.textSize = 16f
        auto.setTextColor(textColor)

        val prefs = getSharedPreferences(
            "toolnexa_settings",
            MODE_PRIVATE
        )

        auto.isChecked =
            prefs.getBoolean(
                "auto_update_check",
                true
            )

        auto.setOnCheckedChangeListener { _, checked ->
            analytics.event(
                "auto_update_toggle",
                "enabled" to checked.toString()
            )
            prefs.edit()
                .putBoolean(
                    "auto_update_check",
                    checked
                )
                .apply()
        }

        card.addView(auto)

        val updateNow = Button(this)
        updateNow.text = "Verificar agora"
        styleSecondary(updateNow)
        updateNow.setOnClickListener {
            analytics.event("update_check_manual")
            updateManager.checkForUpdate(
                showErrors = true
            )
        }

        card.addView(updateNow)

        content.addView(
            card,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    dp(16),
                    0,
                    dp(16),
                    0
                )
            }
        )

        content.addView(space(14))

        val appearance = card()
        appearance.setPadding(
            dp(18),
            dp(18),
            dp(18),
            dp(18)
        )

        appearance.addView(
            title(
                "Aparência",
                18f
            )
        )
        appearance.addView(
            bodyText(
                "A identidade atual usa tema claro e superfícies claras para manter a experiência legível."
            )
        )

        content.addView(
            appearance,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    dp(16),
                    0,
                    dp(16),
                    0
                )
            }
        )
    }

    private fun showAbout() {
        toolbarTitle.text = "Sobre"
        content.removeAllViews()
        analytics.screen("about")

        content.addView(space(18))

        val card = card()
        card.setPadding(
            dp(20),
            dp(22),
            dp(20),
            dp(22)
        )

        card.addView(
            title(
                "ToolNexa",
                27f
            )
        )
        card.addView(
            bodyText(
                "Uma coleção de ferramentas Android para tarefas rápidas. A versão 1.1.0 inclui Image Compressor, Image Resizer, contas Firebase, Analytics, pesquisa de ferramentas, previews e atualizações pelo GitHub."
            )
        )
        card.addView(
            bodyText(
                "Versão instalada: " +
                    BuildConfig.VERSION_NAME
            )
        )
        card.addView(
            bodyText(
                "Atualizações: GitHub Releases"
            )
        )

        content.addView(
            card,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    dp(16),
                    0,
                    dp(16),
                    0
                )
            }
        )
    }

    private fun toolRow(
        titleText: String,
        description: String,
        category: String,
        action: () -> Unit
    ) {
        val row = card()
        row.setPadding(
            dp(18),
            dp(16),
            dp(18),
            dp(16)
        )

        val tag = TextView(this)
        tag.text = category
        tag.textSize = 11f
        tag.typeface =
            android.graphics.Typeface.DEFAULT_BOLD
        tag.setTextColor(blue)
        row.addView(tag)

        row.addView(
            title(
                titleText,
                19f
            )
        )
        row.addView(
            bodyText(description)
        )

        val button = Button(this)
        button.text = "Abrir"
        styleSecondary(button)
        button.setOnClickListener {
            action()
        }

        row.addView(button)

        content.addView(
            row,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    dp(16),
                    dp(10),
                    dp(16),
                    0
                )
            }
        )
    }

    private fun drawerItem(
        label: String,
        icon: Int,
        action: () -> Unit
    ) {
        val row = LinearLayout(this)
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(
            dp(8),
            0,
            dp(8),
            0
        )
        row.background = selectable()

        row.setOnClickListener {
            analytics.event(
                "navigation_click",
                "destination" to label
            )
            action()
        }

        val image = ImageView(this)
        image.setImageResource(icon)
        image.setColorFilter(blue)

        row.addView(
            image,
            LinearLayout.LayoutParams(
                dp(32),
                dp(52)
            )
        )

        val textView = TextView(this)
        textView.text = label
        textView.textSize = 16f
        textView.setTextColor(textColor)

        row.addView(
            textView,
            LinearLayout.LayoutParams(
                0,
                dp(52),
                1f
            )
        )

        drawer.addView(row)
    }

    private fun openDrawer() {
        drawerShade.visibility =
            View.VISIBLE

        drawer.animate()
            .translationX(0f)
            .setDuration(190)
            .start()
    }

    private fun closeDrawer() {
        drawer.animate()
            .translationX(
                -dp(320).toFloat()
            )
            .setDuration(160)
            .withEndAction {
                drawerShade.visibility =
                    View.GONE
            }
            .start()
    }

    private fun pickImage() {
        analytics.event("image_picker_start")
        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(
                    Intent.CATEGORY_OPENABLE
                )
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                addFlags(
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            }

        startActivityForResult(
            intent,
            REQUEST_PICK_IMAGE
        )
    }

    @Deprecated(
        "Uses the classic Activity result callback in v1."
    )
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            resultCode != RESULT_OK
        ) {
            return
        }

        if (
            requestCode == REQUEST_LOGIN
        ) {
            analytics.event("login_returned_to_app")
            showAccount()
            return
        }

        if (
            requestCode != REQUEST_PICK_IMAGE &&
            requestCode != REQUEST_PICK_RESIZE_IMAGE
        ) {
            return
        }

        val uri = data?.data ?: return

        if (requestCode == REQUEST_PICK_RESIZE_IMAGE) {
            selectedResizeImage = uri
            analytics.event("resize_image_selected")
        } else {
            selectedImage = uri
            analytics.event("image_selected")
        }

        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }

        if (
            requestCode == REQUEST_PICK_RESIZE_IMAGE
        ) {
            showImageResizer()

            val preview = resizerPreview

            Thread {
                val bitmap =
                    compressor.decodeSampled(uri)

                runOnUiThread {
                    if (bitmap != null) {
                        preview?.setImageBitmap(bitmap)
                    }
                    toast(
                        if (bitmap != null) {
                            "Imagem selecionada."
                        } else {
                            "Não foi possível ler a imagem."
                        }
                    )
                }
            }.start()
            return
        }

        if (
            toolbarTitle.text.toString() !=
            "Image Compressor"
        ) {
            showImageCompressor()
        }

        val preview = compressorPreview

        Thread {
            val bitmap =
                compressor.decodeSampled(uri)

            runOnUiThread {
                if (bitmap != null) {
                    preview?.setImageBitmap(
                        bitmap
                    )
                    toast(
                        "Imagem selecionada."
                    )
                } else {
                    toast(
                        "Não foi possível ler a imagem."
                    )
                }
            }
        }.start()
    }

    private fun pickResizeImage() {
        val intent =
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }

        startActivityForResult(
            intent,
            REQUEST_PICK_RESIZE_IMAGE
        )
    }

    private fun showProcessingDialog(
        message: String
    ): android.app.AlertDialog {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.HORIZONTAL
        layout.gravity = Gravity.CENTER_VERTICAL
        layout.setPadding(
            dp(24),
            dp(20),
            dp(24),
            dp(20)
        )

        val gear = TextView(this)
        gear.text = "⚙"
        gear.textSize = 32f
        gear.setTextColor(blue)
        gear.gravity = Gravity.CENTER

        val label = TextView(this)
        label.text = message
        label.textSize = 16f
        label.setTextColor(textColor)
        label.setPadding(dp(18), 0, 0, 0)

        layout.addView(
            gear,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )
        layout.addView(
            label,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val dialog =
            android.app.AlertDialog.Builder(this)
                .setView(layout)
                .setCancelable(false)
                .create()

        dialog.setOnShowListener {
            gear.animate()
                .rotationBy(360f)
                .setDuration(850)
                .setInterpolator(
                    android.view.animation.LinearInterpolator()
                )
                .setListener(
                    object : android.animation.Animator.AnimatorListener {
                        override fun onAnimationStart(
                            animation: android.animation.Animator
                        ) = Unit

                        override fun onAnimationEnd(
                            animation: android.animation.Animator
                        ) {
                            if (dialog.isShowing) {
                                gear.rotation = 0f
                                gear.animate()
                                    .rotationBy(360f)
                                    .setDuration(850)
                                    .setInterpolator(
                                        android.view.animation.LinearInterpolator()
                                    )
                                    .start()
                            }
                        }

                        override fun onAnimationCancel(
                            animation: android.animation.Animator
                        ) = Unit

                        override fun onAnimationRepeat(
                            animation: android.animation.Animator
                        ) = Unit
                    }
                )
                .start()
        }

        dialog.show()
        return dialog
    }

    private fun shareFile(file: File) {
        analytics.event("share_started")
        val uri = FileProvider.getUriForFile(
            this,
            "com.toolnexa.app.fileprovider",
            file
        )

        val send =
            Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        startActivity(
            Intent.createChooser(
                send,
                "Compartilhar imagem"
            )
        )
    }

    private fun card(): LinearLayout {
        return LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL
            background =
                GradientDrawable().apply {
                    setColor(surface)
                    setStroke(
                        dp(1),
                        border
                    )
                    cornerRadius =
                        dp(18).toFloat()
                }
        }
    }

    private fun title(
        value: String,
        size: Float
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            typeface =
                android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            setPadding(
                0,
                dp(7),
                0,
                dp(7)
            )
        }
    }

    private fun bodyText(
        value: String
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = 15f
            setTextColor(muted)
            setPadding(
                0,
                dp(3),
                0,
                dp(10)
            )
        }
    }

    private fun stylePrimary(
        button: Button
    ) {
        button.setTextColor(Color.WHITE)
        button.textSize = 15f
        button.background =
            GradientDrawable().apply {
                setColor(blue)
                cornerRadius =
                    dp(14).toFloat()
            }
        button.minHeight = dp(52)
        button.stateListAnimator = null
    }

    private fun styleSecondary(
        button: Button
    ) {
        button.setTextColor(blue)
        button.textSize = 15f
        button.background =
            GradientDrawable().apply {
                setColor(Color.TRANSPARENT)
                setStroke(
                    dp(1),
                    blue
                )
                cornerRadius =
                    dp(14).toFloat()
            }
        button.minHeight = dp(48)
        button.stateListAnimator = null
    }

    private fun solid(
        color: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
        }
    }

    private fun selectable():
        GradientDrawable {
        return GradientDrawable().apply {
            setColor(surface)
            cornerRadius =
                dp(12).toFloat()
        }
    }

    private fun transparent():
        GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
        }
    }

    private fun space(
        height: Int
    ): Space {
        return Space(this).apply {
            layoutParams =
                LinearLayout.LayoutParams(
                    1,
                    dp(height)
                )
        }
    }

    private fun formatBytes(
        bytes: Long
    ): String {
        if (bytes <= 0) return "—"

        return when {
            bytes >= 1024L * 1024L ->
                String.format(
                    java.util.Locale.US,
                    "%.2f MB",
                    bytes / 1024.0 / 1024.0
                )

            bytes >= 1024L ->
                String.format(
                    java.util.Locale.US,
                    "%.1f KB",
                    bytes / 1024.0
                )

            else ->
                bytes.toString() + " B"
        }
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

    private fun dp(
        value: Int
    ): Int {
        return (
            value *
                resources.displayMetrics.density
            ).roundToInt()
    }

    companion object {
        private const val REQUEST_PICK_IMAGE =
            401
        private const val REQUEST_PICK_RESIZE_IMAGE =
            402
        private const val REQUEST_LOGIN =
            403
    }
}
