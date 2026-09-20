package com.toolnexa.app

import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.Locale

object I18n {

    private val translations =
        mapOf(
            "Useful tools. One app." to
                mapOf(
                    "en" to "Useful tools. One app.",
                    "es" to "Herramientas útiles. Una sola app.",
                    "fr" to "Des outils utiles. Une seule app.",
                    "ar" to "أدوات مفيدة. تطبيق واحد."
                ),
            "Início" to mapOf(
                "en" to "Home",
                "es" to "Inicio",
                "fr" to "Accueil",
                "ar" to "الرئيسية"
            ),
            "Categorias" to mapOf(
                "en" to "Categories",
                "es" to "Categorías",
                "fr" to "Catégories",
                "ar" to "الفئات"
            ),
            "Histórico" to mapOf(
                "en" to "History",
                "es" to "Historial",
                "fr" to "Historique",
                "ar" to "السجل"
            ),
            "Planos" to mapOf(
                "en" to "Plans",
                "es" to "Planes",
                "fr" to "Forfaits",
                "ar" to "الخطط"
            ),
            "Definições" to mapOf(
                "en" to "Settings",
                "es" to "Ajustes",
                "fr" to "Paramètres",
                "ar" to "الإعدادات"
            ),
            "Suporte" to mapOf(
                "en" to "Support",
                "es" to "Soporte",
                "fr" to "Assistance",
                "ar" to "الدعم"
            ),
            "Idioma" to mapOf(
                "en" to "Language",
                "es" to "Idioma",
                "fr" to "Langue",
                "ar" to "اللغة"
            ),
            "Sobre" to mapOf(
                "en" to "About",
                "es" to "Acerca de",
                "fr" to "À propos",
                "ar" to "حول"
            ),
            "Conta" to mapOf(
                "en" to "Account",
                "es" to "Cuenta",
                "fr" to "Compte",
                "ar" to "الحساب"
            ),
            "Encontre a ferramenta certa" to mapOf(
                "en" to "Find the right tool",
                "es" to "Encuentra la herramienta adecuada",
                "fr" to "Trouvez le bon outil",
                "ar" to "اعثر على الأداة المناسبة"
            ),
            "Explore por categoria. As ferramentas ficam organizadas dentro da área onde fazem sentido." to
                mapOf(
                    "en" to "Explore by category. Tools are organized where they make sense.",
                    "es" to "Explora por categoría. Las herramientas están organizadas donde tienen sentido.",
                    "fr" to "Explorez par catégorie. Les outils sont organisés là où ils sont utiles.",
                    "ar" to "استكشف حسب الفئة. الأدوات منظمة في المكان المناسب لها."
                ),
            "Ferramentas em destaque" to mapOf(
                "en" to "Featured tools",
                "es" to "Herramientas destacadas",
                "fr" to "Outils en vedette",
                "ar" to "أدوات مميزة"
            ),
            "Acede rapidamente às ferramentas mais usadas no ToolNexa." to mapOf(
                "en" to "Quickly access the most-used tools in ToolNexa.",
                "es" to "Accede rápidamente a las herramientas más usadas de ToolNexa.",
                "fr" to "Accédez rapidement aux outils les plus utilisés de ToolNexa.",
                "ar" to "الوصول السريع إلى الأدوات الأكثر استخدامًا في ToolNexa."
            ),
            "Pesquisar categoria" to mapOf(
                "en" to "Search category",
                "es" to "Buscar categoría",
                "fr" to "Rechercher une catégorie",
                "ar" to "البحث عن فئة"
            ),
            "Nenhuma categoria encontrada." to mapOf(
                "en" to "No category found.",
                "es" to "No se encontró ninguna categoría.",
                "fr" to "Aucune catégorie trouvée.",
                "ar" to "لم يتم العثور على فئة."
            ),
            "Preparada" to mapOf(
                "en" to "Ready",
                "es" to "Preparada",
                "fr" to "Prête",
                "ar" to "جاهزة"
            ),
            "Pesquisar ferramenta" to mapOf(
                "en" to "Search tool",
                "es" to "Buscar herramienta",
                "fr" to "Rechercher un outil",
                "ar" to "البحث عن أداة"
            ),
            "Nenhuma ferramenta encontrada." to mapOf(
                "en" to "No tool found.",
                "es" to "No se encontró ninguna herramienta.",
                "fr" to "Aucun outil trouvé.",
                "ar" to "لم يتم العثور على أداة."
            ),
            "Reduza o tamanho da imagem com qualidade ajustável." to mapOf(
                "en" to "Reduce image size with adjustable quality.",
                "es" to "Reduce el tamaño de la imagen con calidad ajustable.",
                "fr" to "Réduisez la taille de l’image avec une qualité réglable.",
                "ar" to "قلّل حجم الصورة مع جودة قابلة للتعديل."
            ),
            "Redimensione a imagem e veja a nova prévia." to mapOf(
                "en" to "Resize the image and preview the result.",
                "es" to "Cambia el tamaño de la imagen y previsualiza el resultado.",
                "fr" to "Redimensionnez l’image et prévisualisez le résultat.",
                "ar" to "غيّر حجم الصورة وشاهد المعاينة الجديدة."
            ),
            "Converta imagens para JPG, PNG ou WebP." to mapOf(
                "en" to "Convert images to JPG, PNG or WebP.",
                "es" to "Convierte imágenes a JPG, PNG o WebP.",
                "fr" to "Convertissez des images en JPG, PNG ou WebP.",
                "ar" to "حوّل الصور إلى JPG أو PNG أو WebP."
            ),
            "Remova o fundo com IA e preserve transparência." to mapOf(
                "en" to "Remove backgrounds with AI and keep transparency.",
                "es" to "Elimina fondos con IA y conserva la transparencia.",
                "fr" to "Supprimez les arrière-plans avec l’IA en conservant la transparence.",
                "ar" to "أزل الخلفيات بالذكاء الاصطناعي مع الحفاظ على الشفافية."
            ),
            "A tua conta ToolNexa" to mapOf(
                "en" to "Your ToolNexa account",
                "es" to "Tu cuenta de ToolNexa",
                "fr" to "Votre compte ToolNexa",
                "ar" to "حسابك في ToolNexa"
            ),
            "Entra com Google ou e-mail para sincronizar a tua identidade no ToolNexa." to mapOf(
                "en" to "Sign in with Google or email to sync your ToolNexa identity.",
                "es" to "Inicia sesión con Google o correo para sincronizar tu identidad en ToolNexa.",
                "fr" to "Connectez-vous avec Google ou e-mail pour synchroniser votre identité ToolNexa.",
                "ar" to "سجّل الدخول باستخدام Google أو البريد الإلكتروني لمزامنة هويتك في ToolNexa."
            ),
            "Utilizador ToolNexa" to mapOf(
                "en" to "ToolNexa user",
                "es" to "Usuario de ToolNexa",
                "fr" to "Utilisateur ToolNexa",
                "ar" to "مستخدم ToolNexa"
            ),
            "Sem e-mail disponível" to mapOf(
                "en" to "No email available",
                "es" to "No hay correo disponible",
                "fr" to "Aucun e-mail disponible",
                "ar" to "لا يوجد بريد إلكتروني"
            ),
            "CONTA ATIVA" to mapOf(
                "en" to "ACTIVE ACCOUNT",
                "es" to "CUENTA ACTIVA",
                "fr" to "COMPTE ACTIF",
                "ar" to "حساب نشط"
            ),
            "Dados da conta" to mapOf(
                "en" to "Account details",
                "es" to "Datos de la cuenta",
                "fr" to "Données du compte",
                "ar" to "بيانات الحساب"
            ),
            "Terminar sessão" to mapOf(
                "en" to "Sign out",
                "es" to "Cerrar sesión",
                "fr" to "Se déconnecter",
                "ar" to "تسجيل الخروج"
            ),
            "Sessão terminada." to mapOf(
                "en" to "Signed out.",
                "es" to "Sesión cerrada.",
                "fr" to "Session fermée.",
                "ar" to "تم تسجيل الخروج."
            ),
            "Diretório de salvamento" to mapOf(
                "en" to "Save directory",
                "es" to "Directorio de guardado",
                "fr" to "Répertoire d’enregistrement",
                "ar" to "مجلد الحفظ"
            ),
            "Nenhum diretório selecionado" to mapOf(
                "en" to "No directory selected",
                "es" to "Ningún directorio seleccionado",
                "fr" to "Aucun répertoire sélectionné",
                "ar" to "لم يتم تحديد مجلد"
            ),
            "Alterar diretório" to mapOf(
                "en" to "Change directory",
                "es" to "Cambiar directorio",
                "fr" to "Modifier le répertoire",
                "ar" to "تغيير المجلد"
            ),
            "Selecionar diretório" to mapOf(
                "en" to "Select directory",
                "es" to "Seleccionar directorio",
                "fr" to "Sélectionner le répertoire",
                "ar" to "اختيار المجلد"
            ),
            "Idioma da aplicação" to mapOf(
                "en" to "App language",
                "es" to "Idioma de la aplicación",
                "fr" to "Langue de l’application",
                "ar" to "لغة التطبيق"
            ),
            "Escolhe Português, English, Español, Français ou العربية." to mapOf(
                "en" to "Choose Portuguese, English, Spanish, French or Arabic.",
                "es" to "Elige portugués, inglés, español, francés o árabe.",
                "fr" to "Choisissez le portugais, l’anglais, l’espagnol, le français ou l’arabe.",
                "ar" to "اختر البرتغالية أو الإنجليزية أو الإسبانية أو الفرنسية أو العربية."
            ),
            "Precisas de ajuda?" to mapOf(
                "en" to "Need help?",
                "es" to "¿Necesitas ayuda?",
                "fr" to "Besoin d’aide ?",
                "ar" to "هل تحتاج إلى مساعدة؟"
            ),
            "Envia uma reclamação, sugestão ou pedido de suporte diretamente para a equipa ToolNexa." to mapOf(
                "en" to "Send a complaint, suggestion or support request directly to the ToolNexa team.",
                "es" to "Envía una reclamación, sugerencia o solicitud de soporte directamente al equipo de ToolNexa.",
                "fr" to "Envoyez une réclamation, une suggestion ou une demande d’assistance directement à l’équipe ToolNexa.",
                "ar" to "أرسل شكوى أو اقتراحًا أو طلب دعم مباشرةً إلى فريق ToolNexa."
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
            "Aparência" to mapOf(
                "en" to "Appearance",
                "es" to "Apariencia",
                "fr" to "Apparence",
                "ar" to "المظهر"
            ),
            "Tema claro, superfícies suaves, microanimações e alto contraste para uma experiência confortável." to mapOf(
                "en" to "Light theme, soft surfaces, micro-animations and high contrast for a comfortable experience.",
                "es" to "Tema claro, superficies suaves, microanimaciones y alto contraste para una experiencia cómoda.",
                "fr" to "Thème clair, surfaces douces, micro-animations et contraste élevé pour une expérience confortable.",
                "ar" to "مظهر فاتح وأسطح ناعمة وحركات خفيفة وتباين مرتفع لتجربة مريحة."
            ),
            "Abrir menu" to mapOf(
                "en" to "Open menu",
                "es" to "Abrir menú",
                "fr" to "Ouvrir le menu",
                "ar" to "فتح القائمة"
            ),
            "Pesquisar categoria" to mapOf(
                "en" to "Search category",
                "es" to "Buscar categoría",
                "fr" to "Rechercher une catégorie",
                "ar" to "البحث عن فئة"
            ),
            "Categorias" to mapOf(
                "en" to "Categories",
                "es" to "Categorías",
                "fr" to "Catégories",
                "ar" to "الفئات"
            ),
            "Começa grátis ou desbloqueia os recursos Pro." to mapOf(
                "en" to "Start free or unlock Pro features.",
                "es" to "Empieza gratis o desbloquea las funciones Pro.",
                "fr" to "Commencez gratuitement ou débloquez les fonctions Pro.",
                "ar" to "ابدأ مجانًا أو افتح مزايا Pro."
            ),
            "Ferramentas mais completas, uma experiência mais fluida." to mapOf(
                "en" to "More complete tools, a smoother experience.",
                "es" to "Herramientas más completas y una experiencia más fluida.",
                "fr" to "Des outils plus complets pour une expérience plus fluide.",
                "ar" to "أدوات أكثر اكتمالًا وتجربة أكثر سلاسة."
            ),
            "Assinar Pro" to mapOf(
                "en" to "Subscribe to Pro",
                "es" to "Suscribirse a Pro",
                "fr" to "S’abonner à Pro",
                "ar" to "الاشتراك في Pro"
            ),
            "Plano atual" to mapOf(
                "en" to "Current plan",
                "es" to "Plan actual",
                "fr" to "Forfait actuel",
                "ar" to "الخطة الحالية"
            ),
            "Sem subscrição" to mapOf(
                "en" to "No subscription",
                "es" to "Sin suscripción",
                "fr" to "Aucun abonnement",
                "ar" to "لا يوجد اشتراك"
            ),
            "Image Compressor" to mapOf(
                "pt" to "Image Compressor",
                "en" to "Image Compressor",
                "es" to "Compresor de imágenes",
                "fr" to "Compresseur d’images",
                "ar" to "ضغط الصور"
            ),
            "Image Resizer" to mapOf(
                "pt" to "Image Resizer",
                "en" to "Image Resizer",
                "es" to "Redimensionador de imágenes",
                "fr" to "Redimensionneur d’images",
                "ar" to "تغيير حجم الصور"
            ),
            "Image Converter" to mapOf(
                "pt" to "Image Converter",
                "en" to "Image Converter",
                "es" to "Conversor de imágenes",
                "fr" to "Convertisseur d’images",
                "ar" to "تحويل الصور"
            ),
            "Background Remover" to mapOf(
                "pt" to "Background Remover",
                "en" to "Background Remover",
                "es" to "Eliminador de fondo",
                "fr" to "Suppression d’arrière-plan",
                "ar" to "إزالة الخلفية"
            ),
            "Remova o fundo automaticamente e depois edite " to mapOf(
                "en" to "Remove the background automatically and then edit ",
                "es" to "Elimina el fondo automáticamente y luego edita ",
                "fr" to "Supprimez automatiquement l’arrière-plan puis modifiez ",
                "ar" to "أزل الخلفية تلقائيًا ثم حرّر "
            ),
            "Começar com uma imagem" to mapOf(
                "en" to "Start with an image",
                "es" to "Comenzar con una imagen",
                "fr" to "Commencer avec une image",
                "ar" to "ابدأ بصورة"
            ),
            "Escolher imagem" to mapOf(
                "en" to "Choose image",
                "es" to "Elegir imagen",
                "fr" to "Choisir une image",
                "ar" to "اختيار صورة"
            ),
            "Confirmar imagem" to mapOf(
                "en" to "Confirm image",
                "es" to "Confirmar imagen",
                "fr" to "Confirmer l’image",
                "ar" to "تأكيد الصورة"
            ),
            "Remover fundo" to mapOf(
                "en" to "Remove background",
                "es" to "Eliminar fondo",
                "fr" to "Supprimer l’arrière-plan",
                "ar" to "إزالة الخلفية"
            ),
            "Escolher outra imagem" to mapOf(
                "en" to "Choose another image",
                "es" to "Elegir otra imagen",
                "fr" to "Choisir une autre image",
                "ar" to "اختيار صورة أخرى"
            ),
            "Fundo" to mapOf(
                "en" to "Background",
                "es" to "Fondo",
                "fr" to "Arrière-plan",
                "ar" to "الخلفية"
            ),
            "Ajustes" to mapOf(
                "en" to "Adjustments",
                "es" to "Ajustes",
                "fr" to "Réglages",
                "ar" to "التعديلات"
            ),
            "Recorte" to mapOf(
                "en" to "Cutout",
                "es" to "Recorte",
                "fr" to "Détourage",
                "ar" to "القص"
            ),
            "Exportação" to mapOf(
                "en" to "Export",
                "es" to "Exportación",
                "fr" to "Exportation",
                "ar" to "التصدير"
            ),
            "Salvar imagem editada" to mapOf(
                "en" to "Save edited image",
                "es" to "Guardar imagen editada",
                "fr" to "Enregistrer l’image modifiée",
                "ar" to "حفظ الصورة المعدلة"
            ),
            "Partilhar imagem editada" to mapOf(
                "en" to "Share edited image",
                "es" to "Compartir imagen editada",
                "fr" to "Partager l’image modifiée",
                "ar" to "مشاركة الصورة المعدلة"
            ),
            "Transparente" to mapOf(
                "en" to "Transparent",
                "es" to "Transparente",
                "fr" to "Transparent",
                "ar" to "شفاف"
            ),
            "original" to mapOf(
                "en" to "original",
                "es" to "original",
                "fr" to "original",
                "ar" to "أصلي"
            ),
            "Original" to mapOf(
                "en" to "Original",
                "es" to "Original",
                "fr" to "Original",
                "ar" to "أصلي"
            ),
            "Preto" to mapOf(
                "en" to "Black",
                "es" to "Negro",
                "fr" to "Noir",
                "ar" to "أسود"
            ),
            "Branco" to mapOf(
                "en" to "White",
                "es" to "Blanco",
                "fr" to "Blanc",
                "ar" to "أبيض"
            ),
            "Vermelho" to mapOf(
                "en" to "Red",
                "es" to "Rojo",
                "fr" to "Rouge",
                "ar" to "أحمر"
            ),
            "Verde" to mapOf(
                "en" to "Green",
                "es" to "Verde",
                "fr" to "Vert",
                "ar" to "أخضر"
            ),
            "Roxo" to mapOf(
                "en" to "Purple",
                "es" to "Morado",
                "fr" to "Violet",
                "ar" to "بنفسجي"
            ),
            "Laranja" to mapOf(
                "en" to "Orange",
                "es" to "Naranja",
                "fr" to "Orange",
                "ar" to "برتقالي"
            ),
            "Amarelo" to mapOf(
                "en" to "Yellow",
                "es" to "Amarillo",
                "fr" to "Jaune",
                "ar" to "أصفر"
            ),
            "Rosa" to mapOf(
                "en" to "Pink",
                "es" to "Rosa",
                "fr" to "Rose",
                "ar" to "وردي"
            ),
            "Ciano" to mapOf(
                "en" to "Cyan",
                "es" to "Cian",
                "fr" to "Cyan",
                "ar" to "سماوي"
            ),
            "Arco-íris" to mapOf(
                "en" to "Rainbow",
                "es" to "Arcoíris",
                "fr" to "Arc-en-ciel",
                "ar" to "قوس قزح"
            ),
            "Céu" to mapOf(
                "en" to "Sky",
                "es" to "Cielo",
                "fr" to "Ciel",
                "ar" to "سماء"
            ),
            "Pôr do sol" to mapOf(
                "en" to "Sunset",
                "es" to "Atardecer",
                "fr" to "Coucher de soleil",
                "ar" to "غروب الشمس"
            ),
            "Praia" to mapOf(
                "en" to "Beach",
                "es" to "Playa",
                "fr" to "Plage",
                "ar" to "شاطئ"
            ),
            "Tropical" to mapOf(
                "en" to "Tropical",
                "es" to "Tropical",
                "fr" to "Tropical",
                "ar" to "استوائي"
            ),
            "Barco" to mapOf(
                "en" to "Boat",
                "es" to "Barco",
                "fr" to "Bateau",
                "ar" to "قارب"
            ),
            "Montanhas" to mapOf(
                "en" to "Mountains",
                "es" to "Montañas",
                "fr" to "Montagnes",
                "ar" to "جبال"
            ),
            "Cidade" to mapOf(
                "en" to "City",
                "es" to "Ciudad",
                "fr" to "Ville",
                "ar" to "مدينة"
            ),
            "Floresta" to mapOf(
                "en" to "Forest",
                "es" to "Bosque",
                "fr" to "Forêt",
                "ar" to "غابة"
            ),
            "Estúdio" to mapOf(
                "en" to "Studio",
                "es" to "Estudio",
                "fr" to "Studio",
                "ar" to "استوديو"
            ),
            "Espaço" to mapOf(
                "en" to "Space",
                "es" to "Espacio",
                "fr" to "Espace",
                "ar" to "فضاء"
            ),
            "Dourado" to mapOf(
                "en" to "Gold",
                "es" to "Dorado",
                "fr" to "Doré",
                "ar" to "ذهبي"
            ),
            "Prata" to mapOf(
                "en" to "Silver",
                "es" to "Plata",
                "fr" to "Argent",
                "ar" to "فضي"
            ),
            "Gerar" to mapOf(
                "en" to "Generate",
                "es" to "Generar",
                "fr" to "Générer",
                "ar" to "إنشاء"
            ),
            "Cancelar" to mapOf(
                "en" to "Cancel",
                "es" to "Cancelar",
                "fr" to "Annuler",
                "ar" to "إلغاء"
            ),
            "Descrição" to mapOf(
                "en" to "Description",
                "es" to "Descripción",
                "fr" to "Description",
                "ar" to "الوصف"
            ),
            "Qualidade" to mapOf(
                "en" to "Quality",
                "es" to "Calidad",
                "fr" to "Qualité",
                "ar" to "الجودة"
            ),
            "Natural" to mapOf(
                "en" to "Natural",
                "es" to "Natural",
                "fr" to "Naturel",
                "ar" to "طبيعي"
            ),
            "Vivo" to mapOf(
                "en" to "Vivid",
                "es" to "Intenso",
                "fr" to "Vif",
                "ar" to "حيوي"
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
            "Posição vertical" to mapOf(
                "en" to "Vertical position",
                "es" to "Posición vertical",
                "fr" to "Position verticale",
                "ar" to "الموضع الرأسي"
            ),
            "Salvar transparente" to mapOf(
                "en" to "Save transparent",
                "es" to "Guardar transparente",
                "fr" to "Enregistrer avec transparence",
                "ar" to "حفظ مع الشفافية"
            ),
            "Como deseja salvar?" to mapOf(
                "en" to "How would you like to save?",
                "es" to "¿Cómo quieres guardar?",
                "fr" to "Comment souhaitez-vous enregistrer ?",
                "ar" to "كيف تريد الحفظ؟"
            ),
            "Imagem salva com qualidade selecionada." to mapOf(
                "en" to "Image saved with the selected quality.",
                "es" to "Imagen guardada con la calidad seleccionada.",
                "fr" to "Image enregistrée avec la qualité sélectionnée.",
                "ar" to "تم حفظ الصورة بالجودة المحددة."
            ),
            "Resultado salvo." to mapOf(
                "en" to "Result saved.",
                "es" to "Resultado guardado.",
                "fr" to "Résultat enregistré.",
                "ar" to "تم حفظ النتيجة."
            ),
            "Partilhar resultado" to mapOf(
                "en" to "Share result",
                "es" to "Compartir resultado",
                "fr" to "Partager le résultat",
                "ar" to "مشاركة النتيجة"
            ),
            "Abrir" to mapOf(
                "en" to "Open",
                "es" to "Abrir",
                "fr" to "Ouvrir",
                "ar" to "فتح"
            ),
            "Limpar histórico" to mapOf(
                "en" to "Clear history",
                "es" to "Borrar historial",
                "fr" to "Effacer l’historique",
                "ar" to "مسح السجل"
            ),
            "Histórico limpo." to mapOf(
                "en" to "History cleared.",
                "es" to "Historial borrado.",
                "fr" to "Historique effacé.",
                "ar" to "تم مسح السجل."
            ),
            "Ainda não existem resultados guardados." to mapOf(
                "en" to "There are no saved results yet.",
                "es" to "Todavía no hay resultados guardados.",
                "fr" to "Aucun résultat enregistré pour le moment.",
                "ar" to "لا توجد نتائج محفوظة بعد."
            ),
            "Pasta de armazenamento" to mapOf(
                "en" to "Storage folder",
                "es" to "Carpeta de almacenamiento",
                "fr" to "Dossier de stockage",
                "ar" to "مجلد التخزين"
            ),
            "Nenhuma pasta definida" to mapOf(
                "en" to "No folder set",
                "es" to "No hay carpeta definida",
                "fr" to "Aucun dossier défini",
                "ar" to "لم يتم تعيين مجلد"
            ),
            "Destino atual" to mapOf(
                "en" to "Current destination",
                "es" to "Destino actual",
                "fr" to "Destination actuelle",
                "ar" to "الوجهة الحالية"
            ),
            "Escolher pasta" to mapOf(
                "en" to "Choose folder",
                "es" to "Elegir carpeta",
                "fr" to "Choisir un dossier",
                "ar" to "اختيار مجلد"
            ),
            "Pasta atualizada." to mapOf(
                "en" to "Folder updated.",
                "es" to "Carpeta actualizada.",
                "fr" to "Dossier mis à jour.",
                "ar" to "تم تحديث المجلد."
            ),
            "Entra para guardar a tua identidade no ToolNexa." to mapOf(
                "en" to "Sign in to keep your ToolNexa identity.",
                "es" to "Inicia sesión para guardar tu identidad en ToolNexa.",
                "fr" to "Connectez-vous pour conserver votre identité ToolNexa.",
                "ar" to "سجّل الدخول للاحتفاظ بهويتك في ToolNexa."
            ),
            "Continuar com Google" to mapOf(
                "en" to "Continue with Google",
                "es" to "Continuar con Google",
                "fr" to "Continuer avec Google",
                "ar" to "المتابعة باستخدام Google"
            ),
            "ou usa o e-mail" to mapOf(
                "en" to "or use email",
                "es" to "o usa el correo",
                "fr" to "ou utilisez l’e-mail",
                "ar" to "أو استخدم البريد الإلكتروني"
            ),
            "Nome" to mapOf(
                "en" to "Name",
                "es" to "Nombre",
                "fr" to "Nom",
                "ar" to "الاسم"
            ),
            "E-mail" to mapOf(
                "en" to "Email",
                "es" to "Correo",
                "fr" to "E-mail",
                "ar" to "البريد الإلكتروني"
            ),
            "Senha" to mapOf(
                "en" to "Password",
                "es" to "Contraseña",
                "fr" to "Mot de passe",
                "ar" to "كلمة المرور"
            ),
            "Entrar" to mapOf(
                "en" to "Sign in",
                "es" to "Iniciar sesión",
                "fr" to "Se connecter",
                "ar" to "تسجيل الدخول"
            ),
            "Esqueci a senha" to mapOf(
                "en" to "Forgot password",
                "es" to "Olvidé la contraseña",
                "fr" to "Mot de passe oublié",
                "ar" to "نسيت كلمة المرور"
            ),
            "ou usa o telefone" to mapOf(
                "en" to "or use phone",
                "es" to "o usa el teléfono",
                "fr" to "ou utilisez le téléphone",
                "ar" to "أو استخدم الهاتف"
            ),
            "Escolher país" to mapOf(
                "en" to "Choose country",
                "es" to "Elegir país",
                "fr" to "Choisir le pays",
                "ar" to "اختيار الدولة"
            ),
            "Enviar código SMS" to mapOf(
                "en" to "Send SMS code",
                "es" to "Enviar código SMS",
                "fr" to "Envoyer le code SMS",
                "ar" to "إرسال رمز SMS"
            ),
            "Código de 6 dígitos" to mapOf(
                "en" to "6-digit code",
                "es" to "Código de 6 dígitos",
                "fr" to "Code à 6 chiffres",
                "ar" to "رمز من 6 أرقام"
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
            "Criar conta" to mapOf(
                "en" to "Create account",
                "es" to "Crear cuenta",
                "fr" to "Créer un compte",
                "ar" to "إنشاء حساب"
            ),
            "Já tenho uma conta" to mapOf(
                "en" to "I already have an account",
                "es" to "Ya tengo una cuenta",
                "fr" to "J’ai déjà un compte",
                "ar" to "لدي حساب بالفعل"
            ),
            "Voltar" to mapOf(
                "en" to "Back",
                "es" to "Volver",
                "fr" to "Retour",
                "ar" to "رجوع"
            ),
            "Conta criada com sucesso." to mapOf(
                "en" to "Account created successfully.",
                "es" to "Cuenta creada correctamente.",
                "fr" to "Compte créé avec succès.",
                "ar" to "تم إنشاء الحساب بنجاح."
            ),
            "Login efetuado com sucesso." to mapOf(
                "en" to "Signed in successfully.",
                "es" to "Inicio de sesión correcto.",
                "fr" to "Connexion réussie.",
                "ar" to "تم تسجيل الدخول بنجاح."
            ),
            "Preencha o e-mail e a senha." to mapOf(
                "en" to "Enter your email and password.",
                "es" to "Introduce tu correo y contraseña.",
                "fr" to "Saisissez votre e-mail et votre mot de passe.",
                "ar" to "أدخل بريدك الإلكتروني وكلمة المرور."
            ),
            "A senha deve ter pelo menos 6 caracteres." to mapOf(
                "en" to "The password must be at least 6 characters.",
                "es" to "La contraseña debe tener al menos 6 caracteres.",
                "fr" to "Le mot de passe doit comporter au moins 6 caractères.",
                "ar" to "يجب أن تتكون كلمة المرور من 6 أحرف على الأقل."
            ),
            "Indique o teu nome." to mapOf(
                "en" to "Enter your name.",
                "es" to "Indica tu nombre.",
                "fr" to "Indiquez votre nom.",
                "ar" to "أدخل اسمك."
            ),
            "A criar a conta..." to mapOf(
                "en" to "Creating account...",
                "es" to "Creando cuenta...",
                "fr" to "Création du compte...",
                "ar" to "جارٍ إنشاء الحساب..."
            ),
            "A entrar..." to mapOf(
                "en" to "Signing in...",
                "es" to "Iniciando sesión...",
                "fr" to "Connexion...",
                "ar" to "جارٍ تسجيل الدخول..."
            ),
            "Enviámos as instruções para o teu e-mail." to mapOf(
                "en" to "Recovery instructions were sent to your email.",
                "es" to "Hemos enviado las instrucciones a tu correo.",
                "fr" to "Les instructions ont été envoyées à votre e-mail.",
                "ar" to "تم إرسال تعليمات الاسترداد إلى بريدك الإلكتروني."
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
            "Introduz o código de 6 dígitos." to mapOf(
                "en" to "Enter the 6-digit code.",
                "es" to "Introduce el código de 6 dígitos.",
                "fr" to "Saisissez le code à 6 chiffres.",
                "ar" to "أدخل الرمز المكون من 6 أرقام."
            ),
            "A verificar o código..." to mapOf(
                "en" to "Verifying code...",
                "es" to "Verificando el código...",
                "fr" to "Vérification du code...",
                "ar" to "جارٍ التحقق من الرمز..."
            ),
            "Novo código enviado por SMS." to mapOf(
                "en" to "A new SMS code was sent.",
                "es" to "Se envió un nuevo código SMS.",
                "fr" to "Un nouveau code SMS a été envoyé.",
                "ar" to "تم إرسال رمز SMS جديد."
            ),
            "A validar a conta Google..." to mapOf(
                "en" to "Checking Google account...",
                "es" to "Validando la cuenta de Google...",
                "fr" to "Validation du compte Google...",
                "ar" to "جارٍ التحقق من حساب Google..."
            ),
            "Login Google efetuado com sucesso." to mapOf(
                "en" to "Google sign-in successful.",
                "es" to "Inicio de sesión con Google correcto.",
                "fr" to "Connexion Google réussie.",
                "ar" to "تم تسجيل الدخول باستخدام Google بنجاح."
            ),
            "A aplicação fecha / falha" to mapOf(
                "en" to "App closes / crashes",
                "es" to "La app se cierra / falla",
                "fr" to "L’application se ferme / plante",
                "ar" to "يغلق التطبيق / يتعطل"
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
            "Não consigo salvar ou partilhar" to mapOf(
                "en" to "Cannot save or share",
                "es" to "No puedo guardar ni compartir",
                "fr" to "Impossible d’enregistrer ou de partager",
                "ar" to "لا يمكن الحفظ أو المشاركة"
            ),
            "Suporte, sugestões e reclamações" to mapOf(
                "en" to "Support, suggestions and complaints",
                "es" to "Soporte, sugerencias y reclamaciones",
                "fr" to "Assistance, suggestions et réclamations",
                "ar" to "الدعم والاقتراحات والشكاوى"
            ),
            "Enviar para o suporte" to mapOf(
                "en" to "Send to support",
                "es" to "Enviar al soporte",
                "fr" to "Envoyer à l’assistance",
                "ar" to "إرسال إلى الدعم"
            ),
            "Escreva a sua mensagem." to mapOf(
                "en" to "Write your message.",
                "es" to "Escribe tu mensaje.",
                "fr" to "Écrivez votre message.",
                "ar" to "اكتب رسالتك."
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
            "Escolha uma imagem. O processamento é feito no próprio dispositivo." to mapOf(
                "en" to "Choose an image. Processing happens on the device.",
                "es" to "Elige una imagen. El procesamiento se realiza en el dispositivo.",
                "fr" to "Choisissez une image. Le traitement est effectué sur l’appareil.",
                "ar" to "اختر صورة. تتم المعالجة على الجهاز."
            ),
            "Processar e ver resultado" to mapOf(
                "en" to "Process and view result",
                "es" to "Procesar y ver resultado",
                "fr" to "Traiter et voir le résultat",
                "ar" to "معالجة وعرض النتيجة"
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
            ),
            "Salvar" to mapOf(
                "en" to "Save",
                "es" to "Guardar",
                "fr" to "Enregistrer",
                "ar" to "حفظ"
            ),
            "Processar outro arquivo" to mapOf(
                "en" to "Process another file",
                "es" to "Procesar otro archivo",
                "fr" to "Traiter un autre fichier",
                "ar" to "معالجة ملف آخر"
            ),
            "Diretório não definido" to mapOf(
                "en" to "Directory not set",
                "es" to "Directorio no definido",
                "fr" to "Répertoire non défini",
                "ar" to "لم يتم تعيين مجلد"
            ),
            "Escolha o diretório em Definições antes de salvar." to mapOf(
                "en" to "Choose the directory in Settings before saving.",
                "es" to "Elige el directorio en Ajustes antes de guardar.",
                "fr" to "Choisissez le répertoire dans Paramètres avant d’enregistrer.",
                "ar" to "اختر المجلد من الإعدادات قبل الحفظ."
            )
        )

    fun t(
        context: Context,
        value: String
    ): String {
        if (value.isBlank()) {
            return value
        }

        val language =
            LanguageManager.current(context)

        if (language == LanguageManager.PORTUGUESE) {
            return value
        }

        translations[value]
            ?.get(language)
            ?.let { return it }

        ToolNexaTranslation.exact[value]
            ?.get(language)
            ?.let { return it }

        val dynamicValue =
            dynamic(
                value,
                language
            )

        if (dynamicValue != value) {
            return dynamicValue
        }

        return ToolNexaTranslation.words(
            value,
            language
        )
    }

    fun localizeWindow(
        context: Context
    ) {
        val decor =
            try {
                context
                    .let { it as android.app.Activity }
                    .window
                    .decorView
            } catch (_: Exception) {
                null
            }

        if (decor != null) {
            localizeViewTree(
                context,
                decor
            )
            decor.post {
                localizeViewTree(
                    context,
                    decor
                )
            }
        }
    }

    fun localizeViewTree(
        context: Context,
        root: View
    ) {
        localizeView(
            context,
            root
        )
    }

    private fun localizeView(
        context: Context,
        view: View
    ) {
        if (view is TextView) {
            val current =
                view.text
                    ?.toString()
                    .orEmpty()

            if (
                current.isNotBlank() &&
                !current.contains(
                    "\$"
                )
            ) {
                view.text =
                    t(
                        context,
                        current
                    )
            }

            val hint =
                view.hint
                    ?.toString()
                    .orEmpty()

            if (hint.isNotBlank()) {
                view.hint =
                    t(
                        context,
                        hint
                    )
            }

            val description =
                view.contentDescription
                    ?.toString()
                    .orEmpty()

            if (description.isNotBlank()) {
                view.contentDescription =
                    t(
                        context,
                        description
                    )
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                localizeView(
                    context,
                    view.getChildAt(i)
                )
            }
        }
    }

    private fun dynamic(
        value: String,
        language: String
    ): String {
        Regex(
            """^(\\d+) ferramenta\(s\)$"""
        ).matchEntire(value)?.let { match ->
            val n = match.groupValues[1]
            return when (language) {
                "en" -> "$n tool(s)"
                "es" -> "$n herramienta(s)"
                "fr" -> "$n outil(s)"
                "ar" -> "$n أداة"
                else -> value
            }
        }

        Regex(
            """^Versão instalada: (.+)$"""
        ).matchEntire(value)?.let { match ->
            val v = match.groupValues[1]
            return when (language) {
                "en" -> "Installed version: $v"
                "es" -> "Versión instalada: $v"
                "fr" -> "Version installée : $v"
                "ar" -> "الإصدار المثبت: $v"
                else -> value
            }
        }

        Regex(
            """^Versão (.+) • conta protegida • Analytics ativo$"""
        ).matchEntire(value)?.let { match ->
            val v = match.groupValues[1]
            return when (language) {
                "en" -> "Version $v • protected account • Analytics active"
                "es" -> "Versión $v • cuenta protegida • Analytics activo"
                "fr" -> "Version $v • compte protégé • Analytics actif"
                "ar" -> "الإصدار $v • الحساب محمي • التحليلات مفعّلة"
                else -> value
            }
        }

        return listOf(
            "Tamanho: " to mapOf(
                "en" to "Size: ",
                "es" to "Tamaño: ",
                "fr" to "Taille : ",
                "ar" to "الحجم: "
            ),
            "Original: " to mapOf(
                "en" to "Original: ",
                "es" to "Original: ",
                "fr" to "Original : ",
                "ar" to "الأصلي: "
            ),
            "Dimensões: " to mapOf(
                "en" to "Dimensions: ",
                "es" to "Dimensiones: ",
                "fr" to "Dimensions : ",
                "ar" to "الأبعاد: "
            ),
            "Qualidade: " to mapOf(
                "en" to "Quality: ",
                "es" to "Calidad: ",
                "fr" to "Qualité : ",
                "ar" to "الجودة: "
            ),
            "UID: " to mapOf(
                "en" to "UID: ",
                "es" to "UID: ",
                "fr" to "UID : ",
                "ar" to "المعرّف: "
            ),
            "Plano atual: " to mapOf(
                "en" to "Current plan: ",
                "es" to "Plan actual: ",
                "fr" to "Forfait actuel : ",
                "ar" to "الخطة الحالية: "
            ),
            "Estado da subscrição: " to mapOf(
                "en" to "Subscription status: ",
                "es" to "Estado de la suscripción: ",
                "fr" to "État de l’abonnement : ",
                "ar" to "حالة الاشتراك: "
            )
        ).firstNotNullOfOrNull { (prefix, values) ->
            if (value.startsWith(prefix)) {
                values[language]?.plus(
                    value.removePrefix(prefix)
                )
            } else {
                null
            }
        } ?: value
    }
}
