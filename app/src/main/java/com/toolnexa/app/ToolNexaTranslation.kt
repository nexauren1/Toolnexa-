package com.toolnexa.app

import java.util.Locale

object ToolNexaTranslation {

    val exact =
        mapOf(
            "A preparar a imagem..." to mapOf(
                "en" to "Preparing the image...",
                "es" to "Preparando la imagen...",
                "fr" to "Préparation de l’image...",
                "ar" to "جارٍ تجهيز الصورة..."
            ),
            "A validar a imagem..." to mapOf(
                "en" to "Validating the image...",
                "es" to "Validando la imagen...",
                "fr" to "Validation de l’image...",
                "ar" to "جارٍ التحقق من الصورة..."
            ),
            "A enviar para o processamento seguro..." to mapOf(
                "en" to "Sending for secure processing...",
                "es" to "Enviando para procesamiento seguro...",
                "fr" to "Envoi vers le traitement sécurisé...",
                "ar" to "جارٍ الإرسال للمعالجة الآمنة..."
            ),
            "A remover o fundo com BiRefNet..." to mapOf(
                "en" to "Removing the background with BiRefNet...",
                "es" to "Eliminando el fondo con BiRefNet...",
                "fr" to "Suppression de l’arrière-plan avec BiRefNet...",
                "ar" to "جارٍ إزالة الخلفية باستخدام BiRefNet..."
            ),
            "A preparar o PNG transparente..." to mapOf(
                "en" to "Preparing the transparent PNG...",
                "es" to "Preparando el PNG transparente...",
                "fr" to "Préparation du PNG transparent...",
                "ar" to "جارٍ تجهيز PNG الشفاف..."
            ),
            "Troque o cenário, ajuste o recorte e exporte uma versão de alta qualidade." to mapOf(
                "en" to "Change the scene, refine the cutout and export a high-quality version.",
                "es" to "Cambia el escenario, ajusta el recorte y exporta una versión de alta calidad.",
                "fr" to "Changez le décor, ajustez le détourage et exportez une version haute qualité.",
                "ar" to "غيّر المشهد واضبط القص وصدّر نسخة عالية الجودة."
            ),
            "PNG conserva transparência e é sem perdas. JPG pode ser usado quando o fundo foi preenchido." to mapOf(
                "en" to "PNG keeps transparency and is lossless. JPG can be used when the background is filled.",
                "es" to "PNG conserva la transparencia y no pierde calidad. JPG puede usarse cuando el fondo está relleno.",
                "fr" to "Le PNG conserve la transparence sans perte. Le JPG peut être utilisé lorsque l’arrière-plan est rempli.",
                "ar" to "يحافظ PNG على الشفافية دون فقدان الجودة. يمكن استخدام JPG عند تعبئة الخلفية."
            ),
            "Use Desfazer e Refazer para corrigir pinceladas sem perder o recorte." to mapOf(
                "en" to "Use Undo and Redo to correct brush strokes without losing the cutout.",
                "es" to "Usa Deshacer y Rehacer para corregir pinceladas sin perder el recorte.",
                "fr" to "Utilisez Annuler et Rétablir pour corriger les coups de pinceau sans perdre le détourage.",
                "ar" to "استخدم التراجع والإعادة لتصحيح ضربات الفرشاة دون فقدان القص."
            ),
            "Fundo fotográfico com IA" to mapOf(
                "en" to "AI photo background",
                "es" to "Fondo fotográfico con IA",
                "fr" to "Arrière-plan photo avec IA",
                "ar" to "خلفية فوتوغرافية بالذكاء الاصطناعي"
            ),
            "Descreva o cenário que quer colocar atrás do recorte. A IA cria uma fotografia nova." to mapOf(
                "en" to "Describe the scene you want behind the cutout. AI creates a new photo.",
                "es" to "Describe el escenario que quieres detrás del recorte. La IA crea una nueva foto.",
                "fr" to "Décrivez le décor souhaité derrière le détourage. L’IA crée une nouvelle photo.",
                "ar" to "صف المشهد الذي تريده خلف القص. سينشئ الذكاء الاصطناعي صورة جديدة."
            ),
            "Criar fundo fotográfico com IA" to mapOf(
                "en" to "Create AI photo background",
                "es" to "Crear fondo fotográfico con IA",
                "fr" to "Créer un arrière-plan photo avec IA",
                "ar" to "إنشاء خلفية فوتوغرافية بالذكاء الاصطناعي"
            ),
            "Minha imagem" to mapOf(
                "en" to "My image",
                "es" to "Mi imagen",
                "fr" to "Mon image",
                "ar" to "صورتي"
            ),
            "Tamanho do pincel" to mapOf(
                "en" to "Brush size",
                "es" to "Tamaño del pincel",
                "fr" to "Taille du pinceau",
                "ar" to "حجم الفرشاة"
            ),
            "Tamanho do recorte" to mapOf(
                "en" to "Cutout size",
                "es" to "Tamaño del recorte",
                "fr" to "Taille du détourage",
                "ar" to "حجم القص"
            ),
            "Posição horizontal" to mapOf(
                "en" to "Horizontal position",
                "es" to "Posición horizontal",
                "fr" to "Position horizontale",
                "ar" to "الموضع الأفقي"
            ),
            "Posição vertical" to mapOf(
                "en" to "Vertical position",
                "es" to "Posición vertical",
                "fr" to "Position verticale",
                "ar" to "الموضع الرأسي"
            ),
            "Rotação" to mapOf(
                "en" to "Rotation",
                "es" to "Rotación",
                "fr" to "Rotation",
                "ar" to "الدوران"
            ),
            "Desfoque do fundo" to mapOf(
                "en" to "Background blur",
                "es" to "Desenfoque del fondo",
                "fr" to "Flou de l’arrière-plan",
                "ar" to "ضبابية الخلفية"
            ),
            "Reiniciar edição" to mapOf(
                "en" to "Reset editing",
                "es" to "Restablecer edición",
                "fr" to "Réinitialiser l’édition",
                "ar" to "إعادة ضبط التحرير"
            ),
            "A tua conta ToolNexa" to mapOf(
                "en" to "Your ToolNexa account",
                "es" to "Tu cuenta de ToolNexa",
                "fr" to "Votre compte ToolNexa",
                "ar" to "حسابك في ToolNexa"
            ),
            "Entrar / Criar conta" to mapOf(
                "en" to "Sign in / Create account",
                "es" to "Iniciar sesión / Crear cuenta",
                "fr" to "Se connecter / Créer un compte",
                "ar" to "تسجيل الدخول / إنشاء حساب"
            ),
            "Dados da conta" to mapOf(
                "en" to "Account details",
                "es" to "Datos de la cuenta",
                "fr" to "Données du compte",
                "ar" to "بيانات الحساب"
            ),
            "Idioma da aplicação" to mapOf(
                "en" to "App language",
                "es" to "Idioma de la aplicación",
                "fr" to "Langue de l’application",
                "ar" to "لغة التطبيق"
            ),
            "Alterar idioma" to mapOf(
                "en" to "Change language",
                "es" to "Cambiar idioma",
                "fr" to "Changer de langue",
                "ar" to "تغيير اللغة"
            ),
            "Abrir suporte" to mapOf(
                "en" to "Open support",
                "es" to "Abrir soporte",
                "fr" to "Ouvrir l’assistance",
                "ar" to "فتح الدعم"
            ),
            "Verificar atualizações automaticamente" to mapOf(
                "en" to "Check for updates automatically",
                "es" to "Buscar actualizaciones automáticamente",
                "fr" to "Vérifier automatiquement les mises à jour",
                "ar" to "التحقق من التحديثات تلقائيًا"
            ),
            "Verificar agora" to mapOf(
                "en" to "Check now",
                "es" to "Comprobar ahora",
                "fr" to "Vérifier maintenant",
                "ar" to "تحقق الآن"
            ),
            "Uma coleção de ferramentas Android para tarefas rápidas." to mapOf(
                "en" to "A collection of Android tools for quick tasks.",
                "es" to "Una colección de herramientas Android para tareas rápidas.",
                "fr" to "Une collection d’outils Android pour les tâches rapides.",
                "ar" to "مجموعة من أدوات Android للمهام السريعة."
            ),
            "Diretório pronto para salvar." to mapOf(
                "en" to "Directory ready for saving.",
                "es" to "Directorio listo para guardar.",
                "fr" to "Répertoire prêt pour l’enregistrement.",
                "ar" to "المجلد جاهز للحفظ."
            ),
            "Diretório não disponível" to mapOf(
                "en" to "Directory unavailable",
                "es" to "Directorio no disponible",
                "fr" to "Répertoire indisponible",
                "ar" to "المجلد غير متاح"
            ),
            "Escolher outra" to mapOf(
                "en" to "Choose another",
                "es" to "Elegir otra",
                "fr" to "Choisir un autre",
                "ar" to "اختيار مجلد آخر"
            ),
            "Fechar" to mapOf(
                "en" to "Close",
                "es" to "Cerrar",
                "fr" to "Fermer",
                "ar" to "إغلاق"
            ),
            "Cancelar" to mapOf(
                "en" to "Cancel",
                "es" to "Cancelar",
                "fr" to "Annuler",
                "ar" to "إلغاء"
            ),
            "Gerar" to mapOf(
                "en" to "Generate",
                "es" to "Generar",
                "fr" to "Générer",
                "ar" to "إنشاء"
            ),
            "Apagar" to mapOf(
                "en" to "Erase",
                "es" to "Borrar",
                "fr" to "Effacer",
                "ar" to "مسح"
            ),
            "Restaurar" to mapOf(
                "en" to "Restore",
                "es" to "Restaurar",
                "fr" to "Restaurer",
                "ar" to "استعادة"
            ),
            "Brilho" to mapOf(
                "en" to "Brightness",
                "es" to "Brillo",
                "fr" to "Luminosité",
                "ar" to "السطوع"
            ),
            "Contraste" to mapOf(
                "en" to "Contrast",
                "es" to "Contraste",
                "fr" to "Contraste",
                "ar" to "التباين"
            ),
            "Saturação" to mapOf(
                "en" to "Saturation",
                "es" to "Saturación",
                "fr" to "Saturation",
                "ar" to "التشبع"
            ),
            "Opacidade" to mapOf(
                "en" to "Opacity",
                "es" to "Opacidad",
                "fr" to "Opacité",
                "ar" to "الشفافية"
            ),
            "Natural" to mapOf(
                "en" to "Natural",
                "es" to "Natural",
                "fr" to "Naturel",
                "ar" to "طبيعي"
            ),
            "Vivo" to mapOf(
                "en" to "Vivid",
                "es" to "Vivo",
                "fr" to "Vif",
                "ar" to "زاهٍ"
            ),
            "P&B" to mapOf(
                "en" to "B&W",
                "es" to "B&N",
                "fr" to "N&B",
                "ar" to "أبيض وأسود"
            ),
            "Abrir aplicação de email" to mapOf(
                "en" to "Open email app",
                "es" to "Abrir aplicación de correo",
                "fr" to "Ouvrir l’application e-mail",
                "ar" to "فتح تطبيق البريد الإلكتروني"
            ),
            "Não foi possível abrir uma aplicação de email." to mapOf(
                "en" to "Could not open an email app.",
                "es" to "No se pudo abrir una aplicación de correo.",
                "fr" to "Impossible d’ouvrir une application e-mail.",
                "ar" to "تعذر فتح تطبيق بريد إلكتروني."
            ),
            "Enviar para o suporte" to mapOf(
                "en" to "Send to support",
                "es" to "Enviar al soporte",
                "fr" to "Envoyer à l’assistance",
                "ar" to "إرسال إلى الدعم"
            ),
            "Precisas de ajuda?" to mapOf(
                "en" to "Need help?",
                "es" to "¿Necesitas ayuda?",
                "fr" to "Besoin d’aide ?",
                "ar" to "هل تحتاج إلى مساعدة؟"
            ),
            "Suporte técnico" to mapOf(
                "en" to "Technical support",
                "es" to "Soporte técnico",
                "fr" to "Assistance technique",
                "ar" to "دعم فني"
            ),
            "Reclamação" to mapOf(
                "en" to "Complaint",
                "es" to "Reclamación",
                "fr" to "Réclamation",
                "ar" to "شكوى"
            ),
            "Sugestão" to mapOf(
                "en" to "Suggestion",
                "es" to "Sugerencia",
                "fr" to "Suggestion",
                "ar" to "اقتراح"
            ),
            "Problema de pagamento" to mapOf(
                "en" to "Payment problem",
                "es" to "Problema de pago",
                "fr" to "Problème de paiement",
                "ar" to "مشكلة في الدفع"
            ),
            "Problema de conta" to mapOf(
                "en" to "Account problem",
                "es" to "Problema de cuenta",
                "fr" to "Problème de compte",
                "ar" to "مشكلة في الحساب"
            ),
            "Problema específico" to mapOf(
                "en" to "Specific problem",
                "es" to "Problema específico",
                "fr" to "Problème spécifique",
                "ar" to "مشكلة محددة"
            ),
            "A aplicação fecha / falha" to mapOf(
                "en" to "App closes / crashes",
                "es" to "La aplicación se cierra / falla",
                "fr" to "L’application se ferme / plante",
                "ar" to "يتوقف التطبيق / يتعطل"
            ),
            "Tela branca" to mapOf(
                "en" to "White screen",
                "es" to "Pantalla blanca",
                "fr" to "Écran blanc",
                "ar" to "شاشة بيضاء"
            ),
            "Função não funciona" to mapOf(
                "en" to "Feature does not work",
                "es" to "La función no funciona",
                "fr" to "La fonction ne fonctionne pas",
                "ar" to "الميزة لا تعمل"
            ),
            "IA não funciona" to mapOf(
                "en" to "AI does not work",
                "es" to "La IA no funciona",
                "fr" to "L’IA ne fonctionne pas",
                "ar" to "الذكاء الاصطناعي لا يعمل"
            ),
            "Resultado incorreto" to mapOf(
                "en" to "Incorrect result",
                "es" to "Resultado incorrecto",
                "fr" to "Résultat incorrect",
                "ar" to "نتيجة غير صحيحة"
            ),
            "Não consigo salvar ou partilhar" to mapOf(
                "en" to "I cannot save or share",
                "es" to "No puedo guardar ni compartir",
                "fr" to "Je ne peux pas enregistrer ni partager",
                "ar" to "لا أستطيع الحفظ أو المشاركة"
            ),
            "Lento ou bloqueia" to mapOf(
                "en" to "Slow or freezes",
                "es" to "Lento o se bloquea",
                "fr" to "Lent ou se bloque",
                "ar" to "بطيء أو يتجمد"
            ),
            "Escolher país" to mapOf(
                "en" to "Choose country",
                "es" to "Elegir país",
                "fr" to "Choisir le pays",
                "ar" to "اختيار البلد"
            ),
            "Enviar código SMS" to mapOf(
                "en" to "Send SMS code",
                "es" to "Enviar código SMS",
                "fr" to "Envoyer le code SMS",
                "ar" to "إرسال رمز SMS"
            ),
            "Confirmar código" to mapOf(
                "en" to "Confirm code",
                "es" to "Confirmar código",
                "fr" to "Confirmer le code",
                "ar" to "تأكيد الرمز"
            ),
            "Reenviar código" to mapOf(
                "en" to "Resend code",
                "es" to "Reenviar código",
                "fr" to "Renvoyer le code",
                "ar" to "إعادة إرسال الرمز"
            ),
            "Trocar número" to mapOf(
                "en" to "Change number",
                "es" to "Cambiar número",
                "fr" to "Changer de numéro",
                "ar" to "تغيير الرقم"
            ),
            "Criar uma conta" to mapOf(
                "en" to "Create an account",
                "es" to "Crear una cuenta",
                "fr" to "Créer un compte",
                "ar" to "إنشاء حساب"
            ),
            "Já tenho uma conta" to mapOf(
                "en" to "I already have an account",
                "es" to "Ya tengo una cuenta",
                "fr" to "J’ai déjà un compte",
                "ar" to "لدي حساب بالفعل"
            ),
            "Conta criada com sucesso." to mapOf(
                "en" to "Account created successfully.",
                "es" to "Cuenta creada correctamente.",
                "fr" to "Compte créé avec succès.",
                "ar" to "تم إنشاء الحساب بنجاح."
            ),
            "A enviar o e-mail de recuperação..." to mapOf(
                "en" to "Sending the recovery email...",
                "es" to "Enviando el correo de recuperación...",
                "fr" to "Envoi de l’e-mail de récupération...",
                "ar" to "جارٍ إرسال بريد الاسترداد..."
            ),
            "Não foi possível enviar o e-mail de recuperação." to mapOf(
                "en" to "Could not send the recovery email.",
                "es" to "No se pudo enviar el correo de recuperación.",
                "fr" to "Impossible d’envoyer l’e-mail de récupération.",
                "ar" to "تعذر إرسال بريد الاسترداد."
            ),
            "Código enviado por SMS." to mapOf(
                "en" to "SMS code sent.",
                "es" to "Código SMS enviado.",
                "fr" to "Code SMS envoyé.",
                "ar" to "تم إرسال رمز SMS."
            ),
            "O e-mail não é válido." to mapOf(
                "en" to "The email is not valid.",
                "es" to "El correo no es válido.",
                "fr" to "L’e-mail n’est pas valide.",
                "ar" to "البريد الإلكتروني غير صالح."
            ),
            "Este e-mail já tem uma conta." to mapOf(
                "en" to "This email already has an account.",
                "es" to "Este correo ya tiene una cuenta.",
                "fr" to "Cet e-mail possède déjà un compte.",
                "ar" to "هذا البريد لديه حساب بالفعل."
            ),
            "Não foi possível concluir a operação." to mapOf(
                "en" to "Could not complete the operation.",
                "es" to "No se pudo completar la operación.",
                "fr" to "Impossible de terminer l’opération.",
                "ar" to "تعذر إكمال العملية."
            ),
            "Escolhe o teu plano" to mapOf(
                "en" to "Choose your plan",
                "es" to "Elige tu plan",
                "fr" to "Choisissez votre forfait",
                "ar" to "اختر خطتك"
            ),
            "Começa grátis ou desbloqueia os recursos Pro." to mapOf(
                "en" to "Start free or unlock Pro features.",
                "es" to "Empieza gratis o desbloquea las funciones Pro.",
                "fr" to "Commencez gratuitement ou débloquez les fonctions Pro.",
                "ar" to "ابدأ مجانًا أو افتح مزايا Pro."
            ),
            "Pagamento processado de forma segura pelo PayPal. A tua identidade continua ligada ao Firebase." to mapOf(
                "en" to "Payment is securely processed by PayPal. Your identity remains linked to Firebase.",
                "es" to "El pago se procesa de forma segura mediante PayPal. Tu identidad sigue vinculada a Firebase.",
                "fr" to "Le paiement est traité en toute sécurité par PayPal. Votre identité reste liée à Firebase.",
                "ar" to "تتم معالجة الدفع بأمان عبر PayPal. تظل هويتك مرتبطة بـ Firebase."
            ),
            "Preço não definido" to mapOf(
                "en" to "Price not set",
                "es" to "Precio no definido",
                "fr" to "Prix non défini",
                "ar" to "السعر غير محدد"
            ),
            "Renovação automática mensal" to mapOf(
                "en" to "Automatic monthly renewal",
                "es" to "Renovación mensual automática",
                "fr" to "Renouvellement mensuel automatique",
                "ar" to "تجديد شهري تلقائي"
            ),
            "Tudo do plano Free" to mapOf(
                "en" to "Everything in the Free plan",
                "es" to "Todo lo incluido en el plan Free",
                "fr" to "Tout le contenu du forfait Free",
                "ar" to "كل ما في الخطة المجانية"
            ),
            "Acesso contínuo enquanto ativo" to mapOf(
                "en" to "Continuous access while active",
                "es" to "Acceso continuo mientras esté activo",
                "fr" to "Accès continu pendant l’activation",
                "ar" to "وصول مستمر أثناء التفعيل"
            ),
            "Histórico do ToolNexa" to mapOf(
                "en" to "ToolNexa history",
                "es" to "Historial de ToolNexa",
                "fr" to "Historique ToolNexa",
                "ar" to "سجل ToolNexa"
            ),
            "Atualizações do aplicativo" to mapOf(
                "en" to "App updates",
                "es" to "Actualizaciones de la aplicación",
                "fr" to "Mises à jour de l’application",
                "ar" to "تحديثات التطبيق"
            ),
            "Plano Pro ativado com sucesso." to mapOf(
                "en" to "Pro plan activated successfully.",
                "es" to "Plan Pro activado correctamente.",
                "fr" to "Forfait Pro activé avec succès.",
                "ar" to "تم تفعيل خطة Pro بنجاح."
            ),
            "Resultado pronto" to mapOf(
                "en" to "Result ready",
                "es" to "Resultado listo",
                "fr" to "Résultat prêt",
                "ar" to "النتيجة جاهزة"
            ),
            "Veja o resultado e salve no diretório escolhido." to mapOf(
                "en" to "View the result and save it to the selected directory.",
                "es" to "Ve el resultado y guárdalo en el directorio elegido.",
                "fr" to "Consultez le résultat et enregistrez-le dans le répertoire choisi.",
                "ar" to "شاهد النتيجة واحفظها في المجلد المحدد."
            )
        )

    private val wordTranslations =
        mapOf(
            "en" to mapOf(
                "Escolher" to "Choose",
                "Escolha" to "Choose",
                "imagem" to "image",
                "Imagem" to "Image",
                "imagens" to "images",
                "fundo" to "background",
                "Fundo" to "Background",
                "remover" to "remove",
                "Remover" to "Remove",
                "Salvar" to "Save",
                "salvar" to "save",
                "salvo" to "saved",
                "salva" to "saved",
                "Editar" to "Edit",
                "edite" to "edit",
                "Processar" to "Process",
                "processamento" to "processing",
                "resultado" to "result",
                "Resultado" to "Result",
                "Qualidade" to "Quality",
                "qualidade" to "quality",
                "Tamanho" to "Size",
                "tamanho" to "size",
                "Largura" to "Width",
                "largura" to "width",
                "Altura" to "Height",
                "altura" to "height",
                "Diretório" to "Directory",
                "diretório" to "directory",
                "Definições" to "Settings",
                "Configurações" to "Settings",
                "Suporte" to "Support",
                "conta" to "account",
                "Conta" to "Account",
                "Idioma" to "Language",
                "idioma" to "language",
                "Histórico" to "History",
                "histórico" to "history",
                "Plano" to "Plan",
                "plano" to "plan",
                "Planos" to "Plans",
                "Ferramenta" to "Tool",
                "ferramenta" to "tool",
                "Ferramentas" to "Tools",
                "ferramentas" to "tools",
                "Categorias" to "Categories",
                "categoria" to "category",
                "categoria" to "category",
                "Abrir" to "Open",
                "abrir" to "open",
                "Fechar" to "Close",
                "fechar" to "close",
                "Cancelar" to "Cancel",
                "cancelar" to "cancel",
                "Enviar" to "Send",
                "enviar" to "send",
                "Gerar" to "Generate",
                "gerar" to "generate",
                "código" to "code",
                "Código" to "Code",
                "número" to "number",
                "Número" to "Number",
                "país" to "country",
                "País" to "Country",
                "problema" to "problem",
                "Problema" to "Problem",
                "sugestão" to "suggestion",
                "Sugestão" to "Suggestion",
                "reclamação" to "complaint",
                "Reclamação" to "Complaint",
                "partilhar" to "share",
                "Partilhar" to "Share",
                "Verificar" to "Check",
                "verificar" to "check",
                "agora" to "now",
                "Atualizações" to "Updates",
                "atualizações" to "updates",
                "Acesso" to "Access",
                "acesso" to "access",
                "seguro" to "secure",
                "segura" to "secure",
                "automático" to "automatic",
                "automática" to "automatic",
                "mensal" to "monthly",
                "iniciar" to "start",
                "Iniciar" to "Start",
                "sessão" to "session",
                "contínuo" to "continuous",
                "Enquanto" to "While",
                "enquanto" to "while",
                "ativo" to "active",
                "ativa" to "active",
                "pronto" to "ready",
                "Pronto" to "Ready",
                "Nenhuma" to "No",
                "nenhuma" to "no",
                "não" to "not",
                "Não" to "Not",
                "possível" to "possible",
                "Possível" to "Possible",
                "novo" to "new",
                "nova" to "new",
                "outra" to "another",
                "Outra" to "Another",
                "Salvar" to "Save",
                "Pincel" to "Brush",
                "pincel" to "brush",
                "posição" to "position",
                "Posição" to "Position",
                "Rotação" to "Rotation",
                "Brilho" to "Brightness",
                "Contraste" to "Contrast",
                "Saturação" to "Saturation",
                "Opacidade" to "Opacity",
                "Transparente" to "Transparent",
                "transparência" to "transparency"
            ),
            "es" to mapOf(
                "Escolher" to "Elegir",
                "Escolha" to "Elige",
                "imagem" to "imagen",
                "Imagem" to "Imagen",
                "imagens" to "imágenes",
                "fundo" to "fondo",
                "Fundo" to "Fondo",
                "remover" to "eliminar",
                "Remover" to "Eliminar",
                "Salvar" to "Guardar",
                "salvar" to "guardar",
                "Editar" to "Editar",
                "Processar" to "Procesar",
                "processamento" to "procesamiento",
                "resultado" to "resultado",
                "Resultado" to "Resultado",
                "Qualidade" to "Calidad",
                "qualidade" to "calidad",
                "Tamanho" to "Tamaño",
                "tamanho" to "tamaño",
                "Largura" to "Ancho",
                "largura" to "ancho",
                "Altura" to "Altura",
                "altura" to "altura",
                "Diretório" to "Directorio",
                "diretório" to "directorio",
                "Definições" to "Ajustes",
                "Suporte" to "Soporte",
                "conta" to "cuenta",
                "Conta" to "Cuenta",
                "Idioma" to "Idioma",
                "idioma" to "idioma",
                "Histórico" to "Historial",
                "histórico" to "historial",
                "Plano" to "Plan",
                "plano" to "plan",
                "Planos" to "Planes",
                "Ferramenta" to "Herramienta",
                "ferramenta" to "herramienta",
                "Ferramentas" to "Herramientas",
                "ferramentas" to "herramientas",
                "Categorias" to "Categorías",
                "categoria" to "categoría",
                "Abrir" to "Abrir",
                "Fechar" to "Cerrar",
                "Cancelar" to "Cancelar",
                "Enviar" to "Enviar",
                "Gerar" to "Generar",
                "código" to "código",
                "Código" to "Código",
                "número" to "número",
                "Número" to "Número",
                "país" to "país",
                "País" to "País",
                "problema" to "problema",
                "Problema" to "Problema",
                "sugestão" to "sugerencia",
                "Sugestão" to "Sugerencia",
                "reclamação" to "reclamación",
                "Reclamação" to "Reclamación",
                "partilhar" to "compartir",
                "Partilhar" to "Compartir",
                "Verificar" to "Comprobar",
                "verificar" to "comprobar",
                "agora" to "ahora",
                "atualizações" to "actualizaciones",
                "Atualizações" to "Actualizaciones",
                "Acesso" to "Acceso",
                "acesso" to "acceso",
                "seguro" to "seguro",
                "segura" to "segura",
                "automático" to "automático",
                "automática" to "automática",
                "mensal" to "mensual",
                "iniciar" to "iniciar",
                "Iniciar" to "Iniciar",
                "sessão" to "sesión",
                "contínuo" to "continuo",
                "Enquanto" to "Mientras",
                "enquanto" to "mientras",
                "ativo" to "activo",
                "ativa" to "activa",
                "pronto" to "listo",
                "Pronto" to "Listo",
                "Nenhuma" to "Ninguna",
                "nenhuma" to "ninguna",
                "não" to "no",
                "Não" to "No",
                "possível" to "posible",
                "novo" to "nuevo",
                "nova" to "nueva",
                "outra" to "otra",
                "Outra" to "Otra",
                "Pincel" to "Pincel",
                "pincel" to "pincel",
                "posição" to "posición",
                "Posição" to "Posición",
                "Rotação" to "Rotación",
                "Brilho" to "Brillo",
                "Contraste" to "Contraste",
                "Saturação" to "Saturación",
                "Opacidade" to "Opacidad",
                "Transparente" to "Transparente",
                "transparência" to "transparencia"
            ),
            "fr" to mapOf(
                "Escolher" to "Choisir",
                "Escolha" to "Choisissez",
                "imagem" to "image",
                "Imagem" to "Image",
                "imagens" to "images",
                "fundo" to "arrière-plan",
                "Fundo" to "Arrière-plan",
                "remover" to "supprimer",
                "Remover" to "Supprimer",
                "Salvar" to "Enregistrer",
                "salvar" to "enregistrer",
                "Editar" to "Modifier",
                "Processar" to "Traiter",
                "processamento" to "traitement",
                "resultado" to "résultat",
                "Resultado" to "Résultat",
                "Qualidade" to "Qualité",
                "qualidade" to "qualité",
                "Tamanho" to "Taille",
                "tamanho" to "taille",
                "Largura" to "Largeur",
                "largura" to "largeur",
                "Altura" to "Hauteur",
                "altura" to "hauteur",
                "Diretório" to "Répertoire",
                "diretório" to "répertoire",
                "Definições" to "Paramètres",
                "Suporte" to "Assistance",
                "conta" to "compte",
                "Conta" to "Compte",
                "Idioma" to "Langue",
                "idioma" to "langue",
                "Histórico" to "Historique",
                "histórico" to "historique",
                "Plano" to "Forfait",
                "plano" to "forfait",
                "Planos" to "Forfaits",
                "Ferramenta" to "Outil",
                "ferramenta" to "outil",
                "Ferramentas" to "Outils",
                "ferramentas" to "outils",
                "Categorias" to "Catégories",
                "categoria" to "catégorie",
                "Abrir" to "Ouvrir",
                "Fechar" to "Fermer",
                "Cancelar" to "Annuler",
                "Enviar" to "Envoyer",
                "Gerar" to "Générer",
                "código" to "code",
                "Código" to "Code",
                "número" to "numéro",
                "Número" to "Numéro",
                "país" to "pays",
                "País" to "Pays",
                "problema" to "problème",
                "Problema" to "Problème",
                "sugestão" to "suggestion",
                "Sugestão" to "Suggestion",
                "reclamação" to "réclamation",
                "Reclamação" to "Réclamation",
                "partilhar" to "partager",
                "Partilhar" to "Partager",
                "Verificar" to "Vérifier",
                "verificar" to "vérifier",
                "agora" to "maintenant",
                "atualizações" to "mises à jour",
                "Atualizações" to "Mises à jour",
                "Acesso" to "Accès",
                "acesso" to "accès",
                "seguro" to "sécurisé",
                "segura" to "sécurisée",
                "automático" to "automatique",
                "automática" to "automatique",
                "mensal" to "mensuel",
                "iniciar" to "démarrer",
                "Iniciar" to "Démarrer",
                "sessão" to "session",
                "contínuo" to "continu",
                "Enquanto" to "Pendant que",
                "enquanto" to "pendant que",
                "ativo" to "actif",
                "ativa" to "active",
                "pronto" to "prêt",
                "Pronto" to "Prêt",
                "Nenhuma" to "Aucune",
                "nenhuma" to "aucune",
                "não" to "ne",
                "Não" to "Non",
                "possível" to "possible",
                "novo" to "nouveau",
                "nova" to "nouvelle",
                "outra" to "autre",
                "Outra" to "Autre",
                "Pincel" to "Pinceau",
                "pincel" to "pinceau",
                "posição" to "position",
                "Posição" to "Position",
                "Rotação" to "Rotation",
                "Brilho" to "Luminosité",
                "Contraste" to "Contraste",
                "Saturação" to "Saturation",
                "Opacidade" to "Opacité",
                "Transparente" to "Transparent",
                "transparência" to "transparence"
            ),
            "ar" to mapOf(
                "Escolher" to "اختيار",
                "Escolha" to "اختر",
                "imagem" to "صورة",
                "Imagem" to "صورة",
                "imagens" to "صور",
                "fundo" to "الخلفية",
                "Fundo" to "الخلفية",
                "remover" to "إزالة",
                "Remover" to "إزالة",
                "Salvar" to "حفظ",
                "salvar" to "حفظ",
                "Editar" to "تحرير",
                "Processar" to "معالجة",
                "processamento" to "المعالجة",
                "resultado" to "النتيجة",
                "Resultado" to "النتيجة",
                "Qualidade" to "الجودة",
                "qualidade" to "الجودة",
                "Tamanho" to "الحجم",
                "tamanho" to "الحجم",
                "Largura" to "العرض",
                "largura" to "العرض",
                "Altura" to "الارتفاع",
                "altura" to "الارتفاع",
                "Diretório" to "المجلد",
                "diretório" to "المجلد",
                "Definições" to "الإعدادات",
                "Suporte" to "الدعم",
                "conta" to "الحساب",
                "Conta" to "الحساب",
                "Idioma" to "اللغة",
                "idioma" to "اللغة",
                "Histórico" to "السجل",
                "histórico" to "السجل",
                "Plano" to "الخطة",
                "plano" to "الخطة",
                "Planos" to "الخطط",
                "Ferramenta" to "أداة",
                "ferramenta" to "أداة",
                "Ferramentas" to "أدوات",
                "ferramentas" to "أدوات",
                "Categorias" to "الفئات",
                "categoria" to "فئة",
                "Abrir" to "فتح",
                "Fechar" to "إغلاق",
                "Cancelar" to "إلغاء",
                "Enviar" to "إرسال",
                "Gerar" to "إنشاء",
                "código" to "رمز",
                "Código" to "رمز",
                "número" to "رقم",
                "Número" to "رقم",
                "país" to "البلد",
                "País" to "البلد",
                "problema" to "مشكلة",
                "Problema" to "مشكلة",
                "sugestão" to "اقتراح",
                "Sugestão" to "اقتراح",
                "reclamação" to "شكوى",
                "Reclamação" to "شكوى",
                "partilhar" to "مشاركة",
                "Partilhar" to "مشاركة",
                "Verificar" to "تحقق",
                "verificar" to "تحقق",
                "agora" to "الآن",
                "atualizações" to "التحديثات",
                "Atualizações" to "التحديثات",
                "Acesso" to "الوصول",
                "acesso" to "الوصول",
                "seguro" to "آمن",
                "segura" to "آمنة",
                "automático" to "تلقائي",
                "automática" to "تلقائية",
                "mensal" to "شهري",
                "iniciar" to "بدء",
                "Iniciar" to "بدء",
                "sessão" to "الجلسة",
                "contínuo" to "مستمر",
                "Enquanto" to "أثناء",
                "enquanto" to "أثناء",
                "ativo" to "نشط",
                "ativa" to "نشطة",
                "pronto" to "جاهز",
                "Pronto" to "جاهز",
                "Nenhuma" to "لا توجد",
                "nenhuma" to "لا توجد",
                "não" to "لا",
                "Não" to "لا",
                "possível" to "ممكن",
                "novo" to "جديد",
                "nova" to "جديدة",
                "outra" to "أخرى",
                "Outra" to "أخرى",
                "Pincel" to "الفرشاة",
                "pincel" to "الفرشاة",
                "posição" to "الموضع",
                "Posição" to "الموضع",
                "Rotação" to "الدوران",
                "Brilho" to "السطوع",
                "Contraste" to "التباين",
                "Saturação" to "التشبع",
                "Opacidade" to "الشفافية",
                "Transparente" to "شفاف",
                "transparência" to "الشفافية"
            )
        )

    private val wordRegexCache =
        mutableMapOf<String, Regex>()

    private val wordMapCache =
        mutableMapOf<String, Map<String, String>>()

    private fun compiledWordRegex(
        language: String
    ): Regex? {
        wordRegexCache[language]?.let {
            return it
        }

        val dictionary =
            wordTranslations[language]
                ?: return null

        val normalized =
            dictionary.entries
                .sortedByDescending {
                    it.key.length
                }

        val pattern =
            normalized
                .joinToString(
                    "|",
                    prefix =
                        "(?i)(?<![\\p{L}])(?:",
                    postfix =
                        ")(?![\\p{L}])"
                ) {
                    Regex.escape(it.key)
                }

        return Regex(pattern).also {
            wordRegexCache[language] = it
        }
    }

    private fun compiledWordMap(
        language: String
    ): Map<String, String> {
        return wordMapCache.getOrPut(language) {
            (wordTranslations[language]
                ?: emptyMap())
                .entries
                .associate {
                    it.key.lowercase(
                        Locale.ROOT
                    ) to it.value
                }
        }
    }

    fun words(
        value: String,
        language: String
    ): String {
        if (value.isBlank()) {
            return value
        }

        val regex =
            compiledWordRegex(language)
                ?: return value

        val dictionary =
            compiledWordMap(language)

        return regex.replace(value) { match ->
            dictionary[
                match.value.lowercase(
                    Locale.ROOT
                )
            ] ?: match.value
        }
    }
}
