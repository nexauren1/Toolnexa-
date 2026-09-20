package com.toolnexa.app

import android.content.Intent
import android.app.AlertDialog
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class LoginActivity : ComponentActivity() {

    private val auth by lazy {
        FirebaseAuth.getInstance()
    }

    private val credentialManager by lazy {
        CredentialManager.create(this)
    }

    private val analytics by lazy {
        AnalyticsTracker(this)
    }

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var nameInput: EditText
    private lateinit var primaryButton: Button
    private lateinit var forgotButton: Button
    private lateinit var phoneCountryButton: Button
    private lateinit var phoneInput: EditText
    private lateinit var phoneButton: Button
    private lateinit var phoneCodeInput: EditText
    private lateinit var phoneVerifyButton: Button
    private lateinit var phoneResendButton: Button
    private lateinit var phoneChangeButton: Button

    private var registerMode = false
    private var selectedPhoneCountry: PhoneCountry? = null
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var lastPhoneNumber: String? = null
    private var phoneLoginCompleted = false
    private var resendTimer: CountDownTimer? = null

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

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = surface
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        if (auth.currentUser != null) {
            openMain()
            return
        }

        buildUi()
        analytics.screen("login")
    }

    private fun buildUi() {
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(
            dp(22),
            dp(28),
            dp(22),
            dp(22)
        )
        root.setBackgroundColor(bg)

        val scroll = android.widget.ScrollView(this)
        val body = LinearLayout(this)
        body.orientation = LinearLayout.VERTICAL

        val brandRail =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.HORIZONTAL
            }

        listOf(
            R.color.toolnexa_green,
            R.color.toolnexa_blue,
            R.color.toolnexa_red,
            R.color.toolnexa_yellow
        ).forEach { colorRes ->
            brandRail.addView(
                View(this).apply {
                    setBackgroundColor(
                        getColor(colorRes)
                    )
                },
                LinearLayout.LayoutParams(
                    0,
                    dp(5),
                    1f
                )
            )
        }

        body.addView(
            brandRail,
            LinearLayout.LayoutParams(
                -1,
                dp(5)
            )
        )

        val brand = TextView(this)
        brand.text = "ToolNexa"
        brand.textSize = 30f
        brand.typeface =
            android.graphics.Typeface.DEFAULT_BOLD
        brand.setTextColor(textColor)
        body.addView(brand)

        val subtitle = TextView(this)
        subtitle.text =
            "Entra para guardar a tua identidade no ToolNexa."
        subtitle.textSize = 15f
        subtitle.setTextColor(muted)
        subtitle.setPadding(0, dp(4), 0, dp(20))
        body.addView(subtitle)

        val google = Button(this)
        google.text = "Continuar com Google"
        stylePrimary(google)
        google.setOnClickListener {
            analytics.event("auth_google_start")
            launchGoogleSignIn()
        }
        body.addView(
            google,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        val divider = TextView(this)
        divider.text = "ou usa o e-mail"
        divider.textSize = 13f
        divider.gravity = Gravity.CENTER
        divider.setTextColor(muted)
        divider.setPadding(0, dp(12), 0, dp(12))
        body.addView(
            divider,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        nameInput = edit(
            "Nome",
            InputType.TYPE_CLASS_TEXT
        )
        nameInput.visibility =
            android.view.View.GONE
        body.addView(nameInput)

        emailInput = edit(
            "E-mail",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )
        body.addView(emailInput)

        passwordInput = edit(
            "Senha",
            InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
        )
        body.addView(passwordInput)

        primaryButton = Button(this)
        primaryButton.text = "Entrar"
        stylePrimary(primaryButton)
        primaryButton.setOnClickListener {
            submitEmail()
        }
        body.addView(
            primaryButton,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            ).apply {
                setMargins(
                    0,
                    dp(14),
                    0,
                    0
                )
            }
        )

        forgotButton = Button(this)
        forgotButton.text = "Esqueci a senha"
        styleSecondary(forgotButton)
        forgotButton.setOnClickListener {
            resetPassword()
        }
        body.addView(
            forgotButton,
            LinearLayout.LayoutParams(
                -1,
                dp(48)
            ).apply {
                setMargins(
                    0,
                    dp(8),
                    0,
                    0
                )
            }
        )

        val phoneDivider = TextView(this)
        phoneDivider.text = "ou usa o telefone"
        phoneDivider.textSize = 13f
        phoneDivider.gravity = Gravity.CENTER
        phoneDivider.setTextColor(muted)
        phoneDivider.setPadding(0, dp(12), 0, dp(12))
        body.addView(
            phoneDivider,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        phoneCountryButton = Button(this)
        phoneCountryButton.text = "Escolher país"
        styleSecondary(phoneCountryButton)
        phoneCountryButton.setOnClickListener {
            showPhoneCountryPicker()
        }
        body.addView(
            phoneCountryButton,
            LinearLayout.LayoutParams(
                -1,
                dp(50)
            ).apply {
                setMargins(0, 0, 0, dp(10))
            }
        )

        phoneInput = edit(
            "Número com 9 dígitos, ex.: 841234567",
            InputType.TYPE_CLASS_PHONE
        )
        body.addView(phoneInput)

        val phoneHint = TextView(this)
        phoneHint.text =
            "Receberás um código por SMS. Podem aplicar-se taxas da tua operadora."
        phoneHint.textSize = 12f
        phoneHint.setTextColor(muted)
        phoneHint.setPadding(
            dp(2),
            dp(0),
            dp(2),
            dp(8)
        )
        body.addView(phoneHint)

        phoneButton = Button(this)
        phoneButton.text = "Enviar código SMS"
        stylePrimary(phoneButton)
        phoneButton.setOnClickListener {
            sendPhoneCode()
        }
        body.addView(
            phoneButton,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            ).apply {
                setMargins(
                    0,
                    dp(2),
                    0,
                    dp(8)
                )
            }
        )

        phoneCodeInput = edit(
            "Código de 6 dígitos",
            InputType.TYPE_CLASS_NUMBER
        )
        phoneCodeInput.inputType =
            InputType.TYPE_CLASS_NUMBER
        phoneCodeInput.visibility =
            android.view.View.GONE
        body.addView(phoneCodeInput)

        phoneVerifyButton = Button(this)
        phoneVerifyButton.text = "Confirmar código"
        stylePrimary(phoneVerifyButton)
        phoneVerifyButton.visibility =
            android.view.View.GONE
        phoneVerifyButton.setOnClickListener {
            verifyPhoneCode()
        }
        body.addView(
            phoneVerifyButton,
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            ).apply {
                setMargins(
                    0,
                    0,
                    0,
                    dp(8)
                )
            }
        )

        phoneResendButton = Button(this)
        phoneResendButton.text = "Reenviar código"
        styleSecondary(phoneResendButton)
        phoneResendButton.visibility =
            android.view.View.GONE
        phoneResendButton.setOnClickListener {
            resendPhoneCode()
        }
        body.addView(
            phoneResendButton,
            LinearLayout.LayoutParams(
                -1,
                dp(48)
            ).apply {
                setMargins(
                    0,
                    0,
                    0,
                    dp(8)
                )
            }
        )

        phoneChangeButton = Button(this)
        phoneChangeButton.text = "Trocar número"
        styleSecondary(phoneChangeButton)
        phoneChangeButton.visibility =
            android.view.View.GONE
        phoneChangeButton.setOnClickListener {
            resetPhoneUi()
            analytics.event("auth_phone_change")
        }
        body.addView(
            phoneChangeButton,
            LinearLayout.LayoutParams(
                -1,
                dp(48)
            ).apply {
                setMargins(
                    0,
                    0,
                    0,
                    dp(10)
                )
            }
        )

        val toggle = Button(this)
        toggle.text = "Criar uma conta"
        styleSecondary(toggle)
        toggle.setOnClickListener {
            registerMode = !registerMode
            nameInput.visibility =
                if (registerMode) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            forgotButton.visibility =
                if (registerMode) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }
            primaryButton.text =
                if (registerMode) {
                    "Criar conta"
                } else {
                    "Entrar"
                }
            toggle.text =
                if (registerMode) {
                    "Já tenho uma conta"
                } else {
                    "Criar uma conta"
                }

            analytics.event(
                "auth_mode_change",
                "mode" to
                    if (registerMode) {
                        "register"
                    } else {
                        "login"
                    }
            )
        }

        body.addView(
            toggle,
            LinearLayout.LayoutParams(
                -1,
                dp(50)
            ).apply {
                setMargins(
                    0,
                    dp(10),
                    0,
                    0
                )
            }
        )

        val legal = TextView(this)
        legal.text =
            "O acesso é gerido pelo Firebase Authentication."
        legal.textSize = 12f
        legal.setTextColor(muted)
        legal.setPadding(0, dp(18), 0, 0)
        body.addView(legal)

        scroll.addView(
            body,
            ViewGroup.LayoutParams(
                -1,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        val close = Button(this)
        close.text = "Voltar"
        styleSecondary(close)
        close.setOnClickListener {
            analytics.event("login_close")
            finish()
        }

        root.addView(
            close,
            LinearLayout.LayoutParams(
                -1,
                dp(50)
            ).apply {
                setMargins(
                    0,
                    dp(12),
                    0,
                    0
                )
            }
        )

        setContentView(root)
    }

    private fun openMain() {
        analytics.event("auth_session_ready")
        startActivity(
            Intent(
                this,
                MainActivity::class.java
            )
        )
        finish()
    }

    private fun submitEmail() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString()
        val name = nameInput.text.toString().trim()

        if (email.isBlank() || password.isBlank()) {
            toast("Preencha o e-mail e a senha.")
            analytics.event("auth_validation_error")
            return
        }

        if (password.length < 6) {
            toast("A senha deve ter pelo menos 6 caracteres.")
            analytics.event("auth_password_too_short")
            return
        }

        if (registerMode && name.isBlank()) {
            toast("Indique o teu nome.")
            analytics.event("auth_name_missing")
            return
        }

        val dialog = showProcessing(
            if (registerMode) {
                "A criar a conta..."
            } else {
                "A entrar..."
            }
        )

        primaryButton.isEnabled = false

        val task = if (registerMode) {
            auth.createUserWithEmailAndPassword(
                email,
                password
            )
        } else {
            auth.signInWithEmailAndPassword(
                email,
                password
            )
        }

        task.addOnCompleteListener { result ->
            primaryButton.isEnabled = true
            dialog.dismiss()

            if (result.isSuccessful) {
                val user = auth.currentUser

                if (
                    registerMode &&
                    user != null &&
                    name.isNotBlank()
                ) {
                    user.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build()
                    )
                }

                analytics.event(
                    if (registerMode) {
                        "auth_register_success"
                    } else {
                        "auth_email_success"
                    }
                )

                if (user != null) {
                    analytics.setUser(user)
                }

                Toast.makeText(
                    this,
                    if (registerMode) {
                        "Conta criada com sucesso."
                    } else {
                        "Login efetuado com sucesso."
                    },
                    Toast.LENGTH_SHORT
                ).show()

                openMain()
            } else {
                analytics.event(
                    if (registerMode) {
                        "auth_register_failed"
                    } else {
                        "auth_email_failed"
                    },
                    "error" to
                        (
                            result.exception
                                ?.javaClass
                                ?.simpleName
                                ?: "unknown"
                        )
                )

                toast(
                    authErrorMessage(
                        result.exception
                    )
                )
            }
        }
    }

    private fun resetPassword() {
        val email = emailInput.text.toString().trim()
        if (email.isBlank()) {
            toast("Indique primeiro o e-mail da tua conta.")
            analytics.event("auth_reset_validation_error")
            return
        }

        val dialog = showProcessing(
            "A enviar o e-mail de recuperação..."
        )
        analytics.event("auth_reset_start")

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { result ->
                dialog.dismiss()
                if (result.isSuccessful) {
                    analytics.event("auth_reset_success")
                    toast("Enviámos as instruções para o teu e-mail.")
                } else {
                    analytics.event(
                        "auth_reset_failed",
                        "error" to (
                            result.exception?.javaClass?.simpleName
                                ?: "unknown"
                        )
                    )
                    toast("Não foi possível enviar o e-mail de recuperação.")
                }
            }
    }

    private fun sendPhoneCode() {
        val phone = normalizePhoneNumber(
            phoneInput.text.toString()
        )

        if (phone == null) {
            toast(
                "Escolhe o país e coloca exatamente 9 dígitos, por exemplo 841234567."
            )
            analytics.event(
                "auth_phone_validation_error"
            )
            return
        }

        lastPhoneNumber = phone
        phoneLoginCompleted = false

        val dialog = showProcessing(
            "A enviar o código por SMS..."
        )
        phoneButton.isEnabled = false
        phoneInput.isEnabled = false
        auth.setLanguageCode("pt")

        val callbacks =
            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(
                    credential: PhoneAuthCredential
                ) {
                    if (phoneLoginCompleted) {
                        return
                    }

                    phoneLoginCompleted = true
                    signInWithPhoneAuthCredential(
                        credential,
                        dialog
                    )
                }

                override fun onVerificationFailed(
                    error: com.google.firebase.FirebaseException
                ) {
                    dialog.dismiss()
                    phoneButton.isEnabled = true
                    phoneInput.isEnabled = true
                    phoneLoginCompleted = false

                    analytics.event(
                        "auth_phone_failed",
                        "error" to
                            error.javaClass.simpleName
                    )
                    toast(
                        phoneAuthErrorMessage(error)
                    )
                }

                override fun onCodeSent(
                    id: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = id
                    resendToken = token

                    dialog.dismiss()
                    phoneButton.isEnabled = true

                    if (!phoneLoginCompleted) {
                        showPhoneCodeUi()
                        analytics.event(
                            "auth_phone_code_sent"
                        )
                        toast(
                            "Código enviado por SMS."
                        )
                        startResendCooldown()
                    }
                }

                override fun onCodeAutoRetrievalTimeOut(
                    id: String
                ) {
                    verificationId = id
                    analytics.event(
                        "auth_phone_auto_retrieval_timeout"
                    )
                }
            }

        val options =
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(
                    60L,
                    java.util.concurrent.TimeUnit.SECONDS
                )
                .setActivity(this)
                .setCallbacks(callbacks)
                .build()

        PhoneAuthProvider.verifyPhoneNumber(
            options
        )
    }

    private fun verifyPhoneCode() {
        val code = phoneCodeInput.text
            .toString()
            .trim()

        val id = verificationId

        if (id.isNullOrBlank()) {
            toast(
                "Pede primeiro um novo código SMS."
            )
            analytics.event(
                "auth_phone_session_missing"
            )
            return
        }

        if (!code.matches(Regex("\\d{6}"))) {
            toast(
                "Introduz o código de 6 dígitos."
            )
            analytics.event(
                "auth_phone_code_validation_error"
            )
            return
        }

        val dialog = showProcessing(
            "A verificar o código..."
        )
        phoneVerifyButton.isEnabled = false

        val credential =
            PhoneAuthProvider.getCredential(
                id,
                code
            )

        signInWithPhoneAuthCredential(
            credential,
            dialog
        )
    }

    private fun resendPhoneCode() {
        val phone = lastPhoneNumber
        val token = resendToken

        if (phone.isNullOrBlank() || token == null) {
            toast(
                "Não é possível reenviar o código agora."
            )
            return
        }

        phoneLoginCompleted = false

        val dialog = showProcessing(
            "A reenviar o código SMS..."
        )
        phoneResendButton.isEnabled = false
        analytics.event("auth_phone_resend")

        val callbacks =
            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(
                    credential: PhoneAuthCredential
                ) {
                    if (phoneLoginCompleted) {
                        return
                    }

                    phoneLoginCompleted = true
                    signInWithPhoneAuthCredential(
                        credential,
                        dialog
                    )
                }

                override fun onVerificationFailed(
                    error: com.google.firebase.FirebaseException
                ) {
                    dialog.dismiss()
                    phoneResendButton.isEnabled = true
                    analytics.event(
                        "auth_phone_resend_failed",
                        "error" to
                            error.javaClass.simpleName
                    )
                    toast(
                        phoneAuthErrorMessage(error)
                    )
                }

                override fun onCodeSent(
                    id: String,
                    newToken: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = id
                    resendToken = newToken
                    dialog.dismiss()

                    if (!phoneLoginCompleted) {
                        analytics.event(
                            "auth_phone_resend_sent"
                        )
                        toast(
                            "Novo código enviado por SMS."
                        )
                        startResendCooldown()
                    }
                }
            }

        val options =
            PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(
                    60L,
                    java.util.concurrent.TimeUnit.SECONDS
                )
                .setActivity(this)
                .setCallbacks(callbacks)
                .setForceResendingToken(token)
                .build()

        PhoneAuthProvider.verifyPhoneNumber(
            options
        )
    }

    private fun showPhoneCodeUi() {
        phoneInput.isEnabled = false
        phoneButton.visibility =
            android.view.View.GONE
        phoneCodeInput.visibility =
            android.view.View.VISIBLE
        phoneVerifyButton.visibility =
            android.view.View.VISIBLE
        phoneResendButton.visibility =
            android.view.View.VISIBLE
        phoneChangeButton.visibility =
            android.view.View.VISIBLE
        phoneCodeInput.requestFocus()
    }

    private fun resetPhoneUi() {
        resendTimer?.cancel()
        resendTimer = null
        verificationId = null
        resendToken = null
        lastPhoneNumber = null
        phoneLoginCompleted = false

        phoneInput.text?.clear()
        phoneCodeInput.text?.clear()
        selectedPhoneCountry = null
        phoneCountryButton.text = "Escolher país"

        phoneInput.isEnabled = true
        phoneButton.isEnabled = true
        phoneButton.visibility =
            android.view.View.VISIBLE
        phoneCodeInput.visibility =
            android.view.View.GONE
        phoneVerifyButton.visibility =
            android.view.View.GONE
        phoneResendButton.visibility =
            android.view.View.GONE
        phoneChangeButton.visibility =
            android.view.View.GONE
    }

    private fun startResendCooldown() {
        resendTimer?.cancel()

        phoneResendButton.isEnabled = false
        resendTimer =
            object : CountDownTimer(
                30_000L,
                1_000L
            ) {
                override fun onTick(
                    millisUntilFinished: Long
                ) {
                    val seconds =
                        (
                            millisUntilFinished / 1_000L
                        ).toInt()

                    phoneResendButton.text =
                        "Reenviar em ${seconds}s"
                }

                override fun onFinish() {
                    phoneResendButton.text =
                        "Reenviar código"
                    phoneResendButton.isEnabled =
                        true
                }
            }.start()
    }

    private fun normalizePhoneNumber(
        raw: String
    ): String? {
        val country = selectedPhoneCountry
            ?: return null

        val value = raw
            .trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        if (!value.matches(Regex("^\\d{9}$"))) {
            return null
        }

        return country.code + value
    }

    private fun showPhoneCountryPicker() {
        val names = PhoneCountries.all.map {
            "${it.flag}  ${it.name}  (${it.code})"
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Escolher país")
            .setItems(names) { _, which ->
                val country = PhoneCountries.all[which]
                selectedPhoneCountry = country
                phoneCountryButton.text =
                    "${country.flag}  ${country.name}  ${country.code}"
                analytics.event(
                    "auth_phone_country_selected",
                    "country" to country.name,
                    "country_code" to country.code
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun phoneAuthErrorMessage(
        error: Exception?
    ): String {
        val code =
            (error as? FirebaseAuthException)
                ?.errorCode
                ?.uppercase()
                ?: ""

        return when {
            code.contains("INVALID_PHONE") ->
                "O número de telefone não é válido."

            code.contains("INVALID_VERIFICATION_CODE") ->
                "O código SMS está incorreto."

            code.contains("SESSION_EXPIRED") ->
                "O código expirou. Pede um novo código."

            code.contains("TOO_MANY_REQUESTS") ||
                code.contains("QUOTA_EXCEEDED") ->
                "Muitas tentativas. Tenta novamente mais tarde."

            code.contains("MISSING_ACTIVITY") ->
                "Não foi possível validar o aplicativo para o SMS."

            else ->
                error?.localizedMessage
                    ?: "Não foi possível enviar ou validar o código SMS."
        }
    }

    private fun signInWithPhoneAuthCredential(
        credential: PhoneAuthCredential,
        dialog: AlertDialog
    ) {
        auth.signInWithCredential(
            credential
        ).addOnCompleteListener { result ->
            dialog.dismiss()
            phoneVerifyButton.isEnabled = true

            if (result.isSuccessful) {
                phoneLoginCompleted = true
                auth.currentUser?.let {
                    analytics.setUser(it)
                }
                analytics.event(
                    "auth_phone_success"
                )
                toast(
                    "Login por telefone efetuado com sucesso."
                )
                openMain()
            } else {
                analytics.event(
                    "auth_phone_code_failed",
                    "error" to
                        (
                            result.exception
                                ?.javaClass
                                ?.simpleName
                                ?: "unknown"
                        )
                )
                toast(
                    phoneAuthErrorMessage(
                        result.exception
                    )
                )
            }
        }
    }

    private fun launchGoogleSignIn() {
        lifecycleScope.launch {
            try {
                val googleIdOption =
                    GetGoogleIdOption.Builder()
                        .setServerClientId(
                            getString(
                                R.string.default_web_client_id
                            )
                        )
                        .setFilterByAuthorizedAccounts(false)
                        .build()

                val request =
                    GetCredentialRequest.Builder()
                        .addCredentialOption(
                            googleIdOption
                        )
                        .build()

                val result =
                    credentialManager.getCredential(
                        this@LoginActivity,
                        request
                    )

                showProcessing(
                    "A validar a conta Google..."
                ).also {
                    handleGoogleCredential(
                        result.credential,
                        it
                    )
                }
            } catch (
                error: GetCredentialException
            ) {
                analytics.event(
                    "auth_google_failed",
                    "error" to
                        (
                            error.message
                                ?: "credential_error"
                        )
                )
                toast(
                    "Não foi possível abrir o login Google."
                )
            }
        }
    }

    private fun handleGoogleCredential(
        credential: Credential,
        dialog: AlertDialog
    ) {
        if (
            credential is CustomCredential &&
            credential.type ==
                GoogleIdTokenCredential
                    .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            try {
                val google =
                    GoogleIdTokenCredential
                        .createFrom(
                            credential.data
                        )

                firebaseAuthWithGoogle(
                    google.idToken,
                    dialog
                )
            } catch (_: Exception) {
                dialog.dismiss()
                analytics.event(
                    "auth_google_token_failed"
                )
                toast(
                    "A credencial Google é inválida."
                )
            }
        } else {
            dialog.dismiss()
            analytics.event(
                "auth_google_unsupported_credential"
            )
            toast(
                "Tipo de credencial Google não suportado."
            )
        }
    }

    private fun firebaseAuthWithGoogle(
        idToken: String,
        dialog: AlertDialog
    ) {
        val credential =
            com.google.firebase.auth
                .GoogleAuthProvider
                .getCredential(
                    idToken,
                    null
                )

        auth.signInWithCredential(
            credential
        ).addOnCompleteListener { result ->
            dialog.dismiss()

            if (result.isSuccessful) {
                auth.currentUser?.let {
                    analytics.setUser(it)
                }
                analytics.event(
                    "auth_google_success"
                )
                toast(
                    "Login Google efetuado com sucesso."
                )
                setResult(
                    RESULT_OK
                )
                finish()
            } else {
                analytics.event(
                    "auth_google_failed",
                    "error" to
                        (
                            result.exception
                                ?.javaClass
                                ?.simpleName
                                ?: "unknown"
                        )
                )
                toast(
                    "Não foi possível concluir o login Google."
                )
            }
        }
    }

    private fun authErrorMessage(
        error: Exception?
    ): String {
        val code =
            (error as? com.google.firebase.auth.FirebaseAuthException)
                ?.errorCode
                ?: ""

        return when (code) {
            "ERROR_INVALID_EMAIL" ->
                "O e-mail não é válido."

            "ERROR_USER_NOT_FOUND",
            "ERROR_WRONG_PASSWORD",
            "ERROR_INVALID_CREDENTIAL" ->
                "E-mail ou senha incorretos."

            "ERROR_EMAIL_ALREADY_IN_USE" ->
                "Este e-mail já tem uma conta."

            "ERROR_WEAK_PASSWORD" ->
                "Escolha uma senha mais forte."

            else ->
                error?.localizedMessage
                    ?: "Não foi possível concluir a operação."
        }
    }

    private fun showProcessing(
        message: String
    ): AlertDialog {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.HORIZONTAL
        layout.gravity = Gravity.CENTER_VERTICAL
        layout.setPadding(
            dp(24),
            dp(18),
            dp(24),
            dp(18)
        )

        val gear = TextView(this)
        gear.text = "⚙"
        gear.textSize = 32f
        gear.setTextColor(blue)

        val label = TextView(this)
        label.text = message
        label.textSize = 16f
        label.setTextColor(textColor)
        label.setPadding(
            dp(18),
            0,
            0,
            0
        )

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
            AlertDialog.Builder(this)
                .setView(layout)
                .setCancelable(false)
                .create()

        dialog.setOnShowListener {
            spinGear(gear, dialog)
        }

        dialog.show()
        return dialog
    }

    private fun spinGear(
        gear: TextView,
        dialog: AlertDialog
    ) {
        if (!dialog.isShowing) return

        gear.animate()
            .rotationBy(360f)
            .setDuration(850)
            .setInterpolator(
                android.view.animation.LinearInterpolator()
            )
            .withEndAction {
                gear.rotation = 0f
                spinGear(gear, dialog)
            }
            .start()
    }

    private fun edit(
        hint: String,
        type: Int
    ): EditText {
        return EditText(this).apply {
            this.hint = hint
            inputType = type
            setSingleLine(true)
            textSize = 15f
            setTextColor(textColor)
            setHintTextColor(muted)
            setPadding(
                dp(16),
                dp(12),
                dp(16),
                dp(12)
            )
            background =
                android.graphics.drawable.GradientDrawable().apply {
                    setColor(surface)
                    setStroke(
                        dp(1),
                        getColor(
                            R.color.toolnexa_border
                        )
                    )
                    cornerRadius =
                        dp(14).toFloat()
                }

            layoutParams =
                LinearLayout.LayoutParams(
                    -1,
                    dp(54)
                ).apply {
                    setMargins(
                        0,
                        0,
                        0,
                        dp(10)
                    )
                }
        }
    }

    private fun stylePrimary(
        button: Button
    ) {
        button.setTextColor(
            android.graphics.Color.WHITE
        )
        button.textSize = 15f
        button.background =
            android.graphics.drawable.GradientDrawable().apply {
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
            android.graphics.drawable.GradientDrawable().apply {
                setColor(
                    android.graphics.Color.TRANSPARENT
                )
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

    private fun toast(
        message: String
    ) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {
        resendTimer?.cancel()
        resendTimer = null
        super.onDestroy()
    }

    private fun dp(
        value: Int
    ): Int {
        return (
            value *
                resources.displayMetrics.density
            ).roundToInt()
    }
}
