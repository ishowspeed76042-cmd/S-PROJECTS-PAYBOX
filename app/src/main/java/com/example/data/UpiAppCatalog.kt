package com.example.data

enum class UpiAppCategory(val label: String) {
    MAJOR("Major UPI Apps"),
    FINTECH("Fintech & Wallets"),
    BANK("Bank & Regional UPI Apps"),
    EMAIL("Verified Payment Emails")
}

data class UpiAppDefinition(
    val id: String,
    val displayName: String,
    val category: UpiAppCategory,
    val officialPackages: Set<String>,
    val description: String
)

object UpiAppCatalog {

    val ALL_APPS: List<UpiAppDefinition> = listOf(
        // Major Indian UPI Apps
        UpiAppDefinition(
            id = "phonepe",
            displayName = "PhonePe",
            category = UpiAppCategory.MAJOR,
            officialPackages = setOf("com.phonepe.app", "com.phonepe.app.business"),
            description = "PhonePe & PhonePe Business"
        ),
        UpiAppDefinition(
            id = "gpay",
            displayName = "Google Pay (GPay)",
            category = UpiAppCategory.MAJOR,
            officialPackages = setOf(
                "com.google.android.apps.nbu.paisa.user",
                "com.google.android.apps.nbu.paisa.merchant"
            ),
            description = "Google Pay & GPay for Business"
        ),
        UpiAppDefinition(
            id = "paytm",
            displayName = "Paytm",
            category = UpiAppCategory.MAJOR,
            officialPackages = setOf("net.one97.paytm", "com.paytm.business"),
            description = "Paytm & Paytm for Business"
        ),
        UpiAppDefinition(
            id = "fampay",
            displayName = "FamPay / FamX",
            category = UpiAppCategory.MAJOR,
            officialPackages = setOf("in.fampay.app", "com.fampay.in"),
            description = "FamPay & FamX UPI"
        ),
        UpiAppDefinition(
            id = "bhim",
            displayName = "BHIM UPI",
            category = UpiAppCategory.MAJOR,
            officialPackages = setOf("in.org.npci.upiapp"),
            description = "Official NPCI BHIM app"
        ),
        UpiAppDefinition(
            id = "navi",
            displayName = "Navi",
            category = UpiAppCategory.MAJOR,
            officialPackages = setOf("com.naviapp"),
            description = "Navi UPI Payments"
        ),
        UpiAppDefinition(
            id = "amazon_pay",
            displayName = "Amazon Pay",
            category = UpiAppCategory.MAJOR,
            officialPackages = setOf("in.amazon.mShop.android.shopping"),
            description = "Amazon Pay UPI"
        ),
        UpiAppDefinition(
            id = "cred",
            displayName = "CRED",
            category = UpiAppCategory.MAJOR,
            officialPackages = setOf("com.dreamplug.androidapp"),
            description = "CRED UPI & Pay"
        ),
        UpiAppDefinition(
            id = "whatsapp_pay",
            displayName = "WhatsApp Pay",
            category = UpiAppCategory.MAJOR,
            officialPackages = setOf("com.whatsapp", "com.whatsapp.w4b"),
            description = "WhatsApp & WA Business UPI receipts"
        ),

        // Fintech, Merchant & Minor Indian UPI Apps
        UpiAppDefinition(
            id = "bharatpe",
            displayName = "BharatPe",
            category = UpiAppCategory.FINTECH,
            officialPackages = setOf("com.bharatpe.app"),
            description = "BharatPe Merchant UPI"
        ),
        UpiAppDefinition(
            id = "super_money",
            displayName = "super.money",
            category = UpiAppCategory.FINTECH,
            officialPackages = setOf("money.super.payments"),
            description = "Flipkart super.money UPI"
        ),
        UpiAppDefinition(
            id = "mobikwik",
            displayName = "MobiKwik",
            category = UpiAppCategory.FINTECH,
            officialPackages = setOf("com.mobikwik_new"),
            description = "MobiKwik Pocket UPI"
        ),
        UpiAppDefinition(
            id = "freecharge",
            displayName = "Freecharge",
            category = UpiAppCategory.FINTECH,
            officialPackages = setOf("com.freecharge.android"),
            description = "Freecharge UPI"
        ),
        UpiAppDefinition(
            id = "airtel_thanks",
            displayName = "Airtel Thanks",
            category = UpiAppCategory.FINTECH,
            officialPackages = setOf("com.myairtelapp"),
            description = "Airtel Payments Bank UPI"
        ),
        UpiAppDefinition(
            id = "jiopay",
            displayName = "JioPay / MyJio",
            category = UpiAppCategory.FINTECH,
            officialPackages = setOf("com.jio.myjio", "com.jiopay.merchant"),
            description = "JioPay & JioPay Business"
        ),
        UpiAppDefinition(
            id = "slice",
            displayName = "slice",
            category = UpiAppCategory.FINTECH,
            officialPackages = setOf("indwin.c3.shareapp"),
            description = "slice UPI"
        ),
        UpiAppDefinition(
            id = "jupiter",
            displayName = "Jupiter",
            category = UpiAppCategory.FINTECH,
            officialPackages = setOf("money.jupiter"),
            description = "Jupiter Bank & UPI"
        ),
        UpiAppDefinition(
            id = "fi_money",
            displayName = "Fi Money",
            category = UpiAppCategory.FINTECH,
            officialPackages = setOf("com.epifi.paisa"),
            description = "Fi Federal UPI"
        ),
        UpiAppDefinition(
            id = "kiwi",
            displayName = "Kiwi",
            category = UpiAppCategory.FINTECH,
            officialPackages = setOf("in.gokiwi.kiwi"),
            description = "Kiwi RuPay UPI"
        ),

        // Bank & Regional UPI Apps
        UpiAppDefinition(
            id = "icici_imobile",
            displayName = "iMobile Pay (ICICI)",
            category = UpiAppCategory.BANK,
            officialPackages = setOf("com.csam.icici.bank.imobile"),
            description = "ICICI Bank iMobile UPI"
        ),
        UpiAppDefinition(
            id = "hdfc_payzapp",
            displayName = "HDFC PayZapp / Mobile",
            category = UpiAppCategory.BANK,
            officialPackages = setOf("com.enstage.wibmo.hdfc", "com.snapwork.hdfc"),
            description = "HDFC PayZapp & MobileBanking"
        ),
        UpiAppDefinition(
            id = "sbi_yono",
            displayName = "YONO SBI",
            category = UpiAppCategory.BANK,
            officialPackages = setOf("com.sbi.lotusintouch", "com.sbi.SBIFreedomPlus"),
            description = "SBI YONO & YONO Lite UPI"
        ),
        UpiAppDefinition(
            id = "kotak_811",
            displayName = "Kotak 811 / Mobile",
            category = UpiAppCategory.BANK,
            officialPackages = setOf("com.msf.kbank.mobile", "com.kotak.bank"),
            description = "Kotak Mahindra Bank UPI"
        ),
        UpiAppDefinition(
            id = "axis_mobile",
            displayName = "Axis Mobile / Axis Pay",
            category = UpiAppCategory.BANK,
            officialPackages = setOf("com.axis.mobile", "com.upi.axispay"),
            description = "Axis Bank UPI"
        ),

        // Official Payment Email Clients (strictly verified by sender domain)
        UpiAppDefinition(
            id = "verified_email",
            displayName = "Verified Payment Emails",
            category = UpiAppCategory.EMAIL,
            officialPackages = setOf(
                "com.google.android.gm",
                "com.microsoft.office.outlook",
                "com.yahoo.mobile.client.android.mail",
                "com.samsung.android.email.provider"
            ),
            description = "Gmail / Outlook alerts from official UPI & Bank domains"
        )
    )

    val ALL_APP_IDS: Set<String> = ALL_APPS.map { it.id }.toSet()

    val PACKAGE_TO_APP: Map<String, UpiAppDefinition> = buildMap {
        ALL_APPS.forEach { app ->
            app.officialPackages.forEach { pkg ->
                put(pkg.lowercase(), app)
            }
        }
    }

    fun findByPackage(packageName: String): UpiAppDefinition? {
        return PACKAGE_TO_APP[packageName.trim().lowercase()]
    }
}
