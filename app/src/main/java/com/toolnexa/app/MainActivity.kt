package com.toolnexa.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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
import androidx.core.content.FileProvider
import java.io.File
import kotlin.math.roundToInt

class MainActivity : Activity() {

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
    private var qualityValue = 82

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

        buildShell()
        updateManager = UpdateManager(this)

        showHome()
        updateManager.checkForUpdate()
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

    private fun showHome() {
        toolbarTitle.text = "Início"
        content.removeAllViews()

        content.addView(space(18))
        content.addView(
            title(
                "Ferramentas que simplesmente funcionam",
                27f
            )
        )
        content.addView(
            bodyText(
                "O ToolNexa reúne utilidades rápidas em uma experiência limpa, simples e preparada para crescer."
            )
        )
        content.addView(space(20))

        val hero = card()
        hero.setPadding(
            dp(20),
            dp(22),
            dp(20),
            dp(22)
        )

        val badge = TextView(this)
        badge.text = "TOOL 01  •  IMAGEM"
        badge.textSize = 12f
        badge.typeface =
            android.graphics.Typeface.DEFAULT_BOLD
        badge.setTextColor(blue)
        hero.addView(badge)

        hero.addView(
            title(
                "Image Compressor",
                22f
            )
        )
        hero.addView(
            bodyText(
                "Reduza o tamanho de uma imagem sem complicação e salve o resultado na galeria."
            )
        )

        val open = Button(this)
        open.text = "Abrir ferramenta"
        stylePrimary(open)
        open.setOnClickListener {
            showImageCompressor()
        }

        hero.addView(
            open,
            LinearLayout.LayoutParams(-1, dp(52))
        )

        content.addView(
            hero,
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

        content.addView(space(18))

        val status = card()
        status.setPadding(
            dp(18),
            dp(18),
            dp(18),
            dp(18)
        )
        status.addView(
            title(
                "Primeira versão",
                18f
            )
        )
        status.addView(
            bodyText(
                "Android nativo • sem conta nesta v1 • atualizações por GitHub Release"
            )
        )

        content.addView(
            status,
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

    private fun showTools() {
        toolbarTitle.text = "Ferramentas"
        content.removeAllViews()

        content.addView(space(18))
        content.addView(
            title(
                "Todas as ferramentas",
                25f
            )
        )
        content.addView(
            bodyText(
                "A primeira ferramenta já está pronta. Mais ferramentas entrarão sem mudar a base do aplicativo."
            )
        )
        content.addView(space(12))

        toolRow(
            "Image Compressor",
            "Comprime imagens e salva uma cópia otimizada.",
            "IMAGEM"
        ) {
            showImageCompressor()
        }

        content.addView(space(12))
        content.addView(
            bodyText(
                "Próximas categorias: PDF, texto, arquivos, áudio e mais."
            )
        )
    }

    private fun showImageCompressor() {
        toolbarTitle.text = "Image Compressor"
        content.removeAllViews()

        content.addView(space(18))
        content.addView(
            title(
                "Comprimir imagem",
                25f
            )
        )
        content.addView(
            bodyText(
                "Escolha uma imagem, ajuste a qualidade e gere uma versão JPEG menor."
            )
        )

        val picker = Button(this)
        picker.text = "Escolher imagem"
        stylePrimary(picker)
        picker.setOnClickListener {
            pickImage()
        }

        content.addView(
            picker,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            ).apply {
                setMargins(
                    dp(16),
                    dp(18),
                    dp(16),
                    0
                )
            }
        )

        val preview = ImageView(this)
        preview.id = android.R.id.icon
        preview.scaleType =
            ImageView.ScaleType.CENTER_INSIDE
        preview.setBackgroundColor(Color.WHITE)

        content.addView(
            preview,
            LinearLayout.LayoutParams(
                -1,
                dp(260)
            ).apply {
                setMargins(
                    dp(16),
                    dp(18),
                    dp(16),
                    0
                )
            }
        )

        val qualityCard = card()
        qualityCard.setPadding(
            dp(18),
            dp(16),
            dp(18),
            dp(16)
        )

        val qualityTitle = TextView(this)
        qualityTitle.text =
            "Qualidade: " +
                qualityValue +
                "%"
        qualityTitle.textSize = 16f
        qualityTitle.typeface =
            android.graphics.Typeface.DEFAULT_BOLD
        qualityTitle.setTextColor(textColor)
        qualityCard.addView(qualityTitle)

        val seek = SeekBar(this)
        seek.max = 80
        seek.progress = qualityValue - 20
        seek.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    bar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    qualityValue = progress + 20
                    qualityTitle.text =
                        "Qualidade: " +
                            qualityValue +
                            "%"
                }

                override fun onStartTrackingTouch(
                    bar: SeekBar?
                ) = Unit

                override fun onStopTrackingTouch(
                    bar: SeekBar?
                ) = Unit
            }
        )

        qualityCard.addView(seek)

        content.addView(
            qualityCard,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    dp(16),
                    dp(18),
                    dp(16),
                    0
                )
            }
        )

        val compressButton = Button(this)
        compressButton.text = "Comprimir agora"
        stylePrimary(compressButton)

        compressButton.setOnClickListener {
            val uri = selectedImage

            if (uri == null) {
                toast(
                    "Escolha uma imagem primeiro."
                )
                return@setOnClickListener
            }

            compressButton.isEnabled = false

            Thread {
                val result =
                    compressor.compress(
                        uri,
                        qualityValue
                    )

                runOnUiThread {
                    compressButton.isEnabled = true

                    if (result == null) {
                        toast(
                            "Não foi possível comprimir esta imagem."
                        )
                    } else {
                        showCompressionResult(result)
                    }
                }
            }.start()
        }

        content.addView(
            compressButton,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            ).apply {
                setMargins(
                    dp(16),
                    dp(18),
                    dp(16),
                    0
                )
            }
        )

        content.addView(space(30))
    }

    private fun showCompressionResult(
        result: CompressionResult
    ) {
        val before = formatBytes(
            result.originalBytes
        )
        val after = formatBytes(
            result.compressedBytes
        )

        val saved = if (
            result.originalBytes > 0
        ) {
            (
                (
                    (
                        result.originalBytes -
                            result.compressedBytes
                    ).toDouble() /
                        result.originalBytes
                ) * 100.0
            )
                .coerceAtLeast(0.0)
                .roundToInt()
        } else {
            0
        }

        val resultCard = card()
        resultCard.setPadding(
            dp(18),
            dp(18),
            dp(18),
            dp(18)
        )

        resultCard.addView(
            title(
                "Resultado",
                19f
            )
        )

        resultCard.addView(
            bodyText(
                "Dimensão: " +
                    result.width +
                    " × " +
                    result.height +
                    "\nAntes: " +
                    before +
                    "\nDepois: " +
                    after +
                    "\nRedução: " +
                    saved +
                    "%"
            )
        )

        val save = Button(this)
        save.text = "Salvar na galeria"
        stylePrimary(save)

        save.setOnClickListener {
            val uri =
                compressor.saveToGallery(
                    result.file
                )

            if (uri != null) {
                toast(
                    "Imagem salva em Pictures/ToolNexa."
                )
            } else {
                toast(
                    "Não foi possível salvar a imagem."
                )
            }
        }

        resultCard.addView(save)

        val share = Button(this)
        share.text = "Compartilhar"
        styleSecondary(share)
        share.setOnClickListener {
            shareFile(result.file)
        }

        resultCard.addView(share)

        content.addView(
            resultCard,
            LinearLayout.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    dp(16),
                    dp(18),
                    dp(16),
                    0
                )
            }
        )
    }

    private fun showAccount() {
        toolbarTitle.text = "Conta"
        content.removeAllViews()

        content.addView(space(18))

        val account = card()
        account.setPadding(
            dp(20),
            dp(22),
            dp(20),
            dp(22)
        )

        account.addView(
            title(
                "Conta ToolNexa",
                24f
            )
        )
        account.addView(
            bodyText(
                "A conta ainda não está ativa nesta versão. O sistema foi deixado preparado para receber Firebase Authentication em uma atualização futura."
            )
        )

        val status = TextView(this)
        status.text = "CONVIDADO"
        status.textSize = 12f
        status.typeface =
            android.graphics.Typeface.DEFAULT_BOLD
        status.setTextColor(blue)
        account.addView(status)

        val login = Button(this)
        login.text = "Login / Registo — em breve"
        styleSecondary(login)
        login.isEnabled = false
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
    }

    private fun showSettings() {
        toolbarTitle.text = "Definições"
        content.removeAllViews()

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
                "Uma coleção de ferramentas Android pensadas para tarefas rápidas. A versão 1.0.0 começa com Image Compressor e uma arquitetura preparada para contas, Firebase, backend Cloudflare, PayPal e planos futuros."
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
            requestCode != REQUEST_PICK_IMAGE ||
            resultCode != RESULT_OK
        ) {
            return
        }

        val uri = data?.data ?: return
        selectedImage = uri

        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }

        if (
            toolbarTitle.text.toString() !=
            "Image Compressor"
        ) {
            showImageCompressor()
        }

        val preview =
            root.findViewById<ImageView>(
                android.R.id.icon
            )

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

    private fun shareFile(file: File) {
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
    }
}
