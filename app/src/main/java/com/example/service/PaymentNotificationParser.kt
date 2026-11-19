package com.example.service

import com.example.data.UpiAppCatalog
import com.example.data.UpiAppCategory
import java.util.Locale
import java.util.regex.Pattern

object PaymentNotificationParser {

    enum class IgnoreReason {
        NONE,
        EMPTY_CONTENT,
        UNVERIFIED_PACKAGE,
        APP_NOT_SELECTED_BY_USER,
        UNVERIFIED_EMAIL_SENDER,
        WHATSAPP_NON_PAYMENT_CHAT,
        PROMOTIONAL,
        DEBIT_OR_NON_CREDIT,
        MISSING_CREDIT_CONFIRMATION,
        NO_VALID_AMOUNT
    }

    data class ParseResult(
        val isValidCredit: Boolean,
        val amount: Double = 0.0,
        val formattedAmount: String = "",
        val senderName: String = "UPI Sender",
        val appName: String = "UPI App",
        val appId: String = "",
        val verifiedSource: String = "",
        val spokenText: String = "",
        val ignoreReason: IgnoreReason = IgnoreReason.NONE
    )

    // Strict Official Email Sender Addresses
    private val VERIFIED_EMAIL_ADDRESSES: Set<String> = setOf(
        "noreply@fampay.in",
        "no-reply@fampay.in",
        "alerts@fampay.in",
        "payments@fampay.in",
        "support@fampay.in",
        "notifications@fampay.in",
        "noreply@famapp.in",
        "no-reply@famapp.in",
        "alerts@famapp.in",
        "noreply@phonepe.com",
        "no-reply@phonepe.com",
        "googlepay-noreply@google.com",
        "payments-noreply@google.com",
        "no-reply@paytm.com",
        "noreply@paytm.com",
        "payments-update@amazon.in",
        "no-reply@amazon.in",
        "noreply@cred.club",
        "alerts@navi.com",
        "noreply@bharatpe.com"
    )

    // Strict Official Payment Provider & Bank Email Domains (exact domain or subdomain match only)
    private val VERIFIED_EMAIL_DOMAINS: Set<String> = setOf(
        "fampay.in",
        "famapp.in",
        "phonepe.com",
        "google.com",
        "paytm.com",
        "paytmbank.com",
        "navi.com",
        "amazon.in",
        "cred.club",
        "whatsapp.com",
        "npci.org.in",
        "bhimupi.org.in",
        "super.money",
        "bharatpe.com",
        "mobikwik.com",
        "freecharge.in",
        "airtel.in",
        "airtelbank.com",
        "jio.com",
        "sliceit.com",
        "jupiter.money",
        "fi.money",
        "gokiwi.in",
        "hdfcbank.net",
        "hdfcbank.com",
        "icicibank.com",
        "sbi.co.in",
        "axisbank.com",
        "kotak.com",
        "kotakbank.com",
        "bankofbaroda.co.in",
        "yesbank.in",
        "indusind.com",
        "idfcfirstbank.com",
        "federalbank.co.in",
        "pnb.co.in",
        "canarabank.com",
        "unionbankofindia.co.in"
    )

    private val ANGLE_BRACKET_EMAIL_PATTERN = Pattern.compile(
        "<([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})>"
    )

    private val PLAIN_EMAIL_PATTERN = Pattern.compile(
        "\\b([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})\\b"
    )

    // Extra guard for WhatsApp Pay so casual chat messages cannot spoof a payment alert
    private val WHATSAPP_PAY_CONTEXT_PATTERN = Pattern.compile(
        "\\b(upi|whatsapp\\s+pay|payment\\s+received|money\\s+received|credited\\s+to\\s+your\\s+bank|to\\s+your\\s+bank\\s+account|transaction\\s+id|utr)\\b",
        Pattern.CASE_INSENSITIVE
    )

    // Promotional / Ad / Offer / Scratch-card keywords (instant reject)
    private val PROMOTIONAL_PATTERNS = listOf(
        Pattern.compile("\\boffers?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bdiscounts?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bget\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bwin\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bcashback\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bpromo(?:tional|tion|code)?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bdeals?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bsave\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bearn\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\brewards?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bvouchers?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bcoupons?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bscratch\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bclaim\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bunlock\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bflat\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bup\\s*to\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bfree\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bsale\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\blimited\\s+time\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\brefer(?:ral)?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\binvite\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bsubscribe\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bupgrade\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bapply\\s+now\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bbuy\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bshop\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bqr\\s+for\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bad(?:s|vertisement)?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bbonus\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bloan\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bemi\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bcredit\\s+(?:limit|card|score)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bjackpot\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bspin\\b", Pattern.CASE_INSENSITIVE)
    )

    // Negative / Debit / Money Sent / Failed keywords (instant reject)
    private val DEBIT_NEGATIVE_PATTERNS = listOf(
        Pattern.compile("\\bsent\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bpaid\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bdebited\\s+from\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bdebited\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bdebit\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\btransferred\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\btransfer\\s+to\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bsent\\s+to\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bpaid\\s+to\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bpayment\\s+to\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bwithdrawn\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bwithdrawal\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bspent\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bdeducted\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\boutgoing\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bpurchased?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bcharged\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bautopay\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\brequest(?:ed)?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bcollect\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bpending\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bfailed\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bdeclined\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\brejected\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bexpired\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bcancel(?:led|ed)?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\breversed\\b", Pattern.CASE_INSENSITIVE)
    )

    // Explicit credit confirmation keywords (at least one required)
    private val CREDIT_CONFIRMATION_PATTERNS = listOf(
        Pattern.compile("\\breceived\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bcredited\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\badded\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bdeposited\\b", Pattern.CASE_INSENSITIVE)
    )

    // Strict currency & credit-context amount extraction regexes
    private val AMOUNT_PATTERNS = listOf(
        Pattern.compile("(?:₹|rs\\.?|inr)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*(?:₹|rs\\.?|inr|rupees?)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:received|credited|added|deposited)(?:\\s+(?:with|by|of))?\\s+(?:₹|rs\\.?|inr)?\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)", Pattern.CASE_INSENSITIVE)
    )

    private val SENDER_FROM_PATTERN = Pattern.compile(
        "(?:from|by)\\s+([A-Za-z][A-Za-z0-9 ._-]{1,40}?)(?:\\s+(?:on|via|to|in|through|for|at|upi|\\(|$)|[.,!]|$)",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Parses and strictly verifies an incoming Android notification across all supported Indian UPI apps.
     *
     * @param allowedAppIds Optional set of app IDs enabled by the merchant. If null, all catalog apps are allowed.
     */
    fun parse(
        packageName: String,
        title: String,
        text: String,
        bigText: String = "",
        subText: String = "",
        infoText: String = "",
        senderEmail: String = "",
        allowedAppIds: Set<String>? = null
    ): ParseResult {
        val normalizedPackage = packageName.trim().lowercase(Locale.ROOT)
        val appDefinition = UpiAppCatalog.findByPackage(normalizedPackage)
            ?: return ParseResult(isValidCredit = false, ignoreReason = IgnoreReason.UNVERIFIED_PACKAGE)

        if (allowedAppIds != null && appDefinition.id !in allowedAppIds) {
            return ParseResult(isValidCredit = false, ignoreReason = IgnoreReason.APP_NOT_SELECTED_BY_USER)
        }

        val rawCombined = listOf(title, text, bigText, subText, infoText)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()

        if (rawCombined.isBlank()) {
            return ParseResult(isValidCredit = false, ignoreReason = IgnoreReason.EMPTY_CONTENT)
        }

        val isEmailClient = appDefinition.category == UpiAppCategory.EMAIL
        var verifiedEmailSource = ""
        if (isEmailClient) {
            val candidateFields = listOf(senderEmail, subText, infoText, title, text, bigText)
                .filter { it.isNotBlank() }
            val verifiedAddress = extractAndVerifyEmailSender(candidateFields)
                ?: return ParseResult(
                    isValidCredit = false,
                    ignoreReason = IgnoreReason.UNVERIFIED_EMAIL_SENDER
                )
            verifiedEmailSource = verifiedAddress
        }

        // Extra anti-spoofing guard for WhatsApp Pay: require genuine UPI/payment receipt context
        if (appDefinition.id == "whatsapp_pay" && !WHATSAPP_PAY_CONTEXT_PATTERN.matcher(rawCombined).find()) {
            return ParseResult(isValidCredit = false, ignoreReason = IgnoreReason.WHATSAPP_NON_PAYMENT_CHAT)
        }

        return parseVerifiedContent(
            appId = appDefinition.id,
            appDisplayName = if (isEmailClient) {
                detectProviderFromEmailOrBody(verifiedEmailSource, rawCombined)
            } else {
                appDefinition.displayName
            },
            title = title,
            rawCombined = rawCombined,
            verifiedSource = verifiedEmailSource.ifBlank { normalizedPackage }
        )
    }

    /**
     * Dedicated Email Parser (for IMAP / Gmail API / Direct Email payloads).
     */
    fun parseEmail(
        fromHeader: String,
        subject: String,
        body: String
    ): ParseResult {
        val verifiedAddress = extractAndVerifyEmailSender(listOf(fromHeader))
            ?: return ParseResult(
                isValidCredit = false,
                ignoreReason = IgnoreReason.UNVERIFIED_EMAIL_SENDER
            )

        val rawCombined = listOf(subject, body)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()

        if (rawCombined.isBlank()) {
            return ParseResult(isValidCredit = false, ignoreReason = IgnoreReason.EMPTY_CONTENT)
        }

        return parseVerifiedContent(
            appId = "verified_email",
            appDisplayName = detectProviderFromEmailOrBody(verifiedAddress, rawCombined),
            title = subject,
            rawCombined = rawCombined,
            verifiedSource = verifiedAddress
        )
    }

    fun extractAndVerifyEmailSender(candidates: List<String>): String? {
        for (raw in candidates) {
            val email = extractActualEmailAddress(raw) ?: continue
            if (isWhitelistedEmailAddress(email)) {
                return email
            }
        }
        return null
    }

    fun extractActualEmailAddress(rawInput: String): String? {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return null

        val bracketMatcher = ANGLE_BRACKET_EMAIL_PATTERN.matcher(trimmed)
        if (bracketMatcher.find()) {
            val insideBrackets = bracketMatcher.group(1)?.lowercase(Locale.ROOT)
            if (bracketMatcher.find()) return null
            return insideBrackets
        }

        val plainMatcher = PLAIN_EMAIL_PATTERN.matcher(trimmed)
        if (plainMatcher.find()) {
            val firstMatch = plainMatcher.group(1)?.lowercase(Locale.ROOT)
            while (plainMatcher.find()) {
                val nextMatch = plainMatcher.group(1)?.lowercase(Locale.ROOT)
                if (nextMatch != null && nextMatch != firstMatch) {
                    return null
                }
            }
            return firstMatch
        }
        return null
    }

    fun isWhitelistedEmailAddress(email: String): Boolean {
        val normalized = email.trim().lowercase(Locale.ROOT)
        val atIndex = normalized.lastIndexOf('@')
        if (atIndex <= 0 || atIndex >= normalized.length - 1) return false

        val domain = normalized.substring(atIndex + 1)
        if (normalized in VERIFIED_EMAIL_ADDRESSES) {
            return true
        }

        return VERIFIED_EMAIL_DOMAINS.any { allowedDomain ->
            domain == allowedDomain || domain.endsWith(".$allowedDomain")
        }
    }

    private fun parseVerifiedContent(
        appId: String,
        appDisplayName: String,
        title: String,
        rawCombined: String,
        verifiedSource: String
    ): ParseResult {
        if (PROMOTIONAL_PATTERNS.any { it.matcher(rawCombined).find() }) {
            return ParseResult(isValidCredit = false, ignoreReason = IgnoreReason.PROMOTIONAL)
        }

        if (DEBIT_NEGATIVE_PATTERNS.any { it.matcher(rawCombined).find() }) {
            return ParseResult(isValidCredit = false, ignoreReason = IgnoreReason.DEBIT_OR_NON_CREDIT)
        }

        if (CREDIT_CONFIRMATION_PATTERNS.none { it.matcher(rawCombined).find() }) {
            return ParseResult(isValidCredit = false, ignoreReason = IgnoreReason.MISSING_CREDIT_CONFIRMATION)
        }

        val amount = extractAmount(rawCombined)
            ?: return ParseResult(isValidCredit = false, ignoreReason = IgnoreReason.NO_VALID_AMOUNT)

        val formattedAmount = formatAmount(amount)
        val sender = extractSender(title, rawCombined)
        val spokenText = "S-PROJECTS Pay Box par $formattedAmount rupaye mile."

        return ParseResult(
            isValidCredit = true,
            amount = amount,
            formattedAmount = formattedAmount,
            senderName = sender,
            appName = appDisplayName,
            appId = appId,
            verifiedSource = verifiedSource,
            spokenText = spokenText,
            ignoreReason = IgnoreReason.NONE
        )
    }

    fun formatAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", amount)
        }
    }

    private fun extractAmount(content: String): Double? {
        for (pattern in AMOUNT_PATTERNS) {
            val matcher = pattern.matcher(content)
            if (matcher.find()) {
                val amountStr = matcher.group(1)?.replace(",", "")
                val amt = amountStr?.toDoubleOrNull()
                if (amt != null && amt > 0.0 && amt <= 1_000_000.0) {
                    return amt
                }
            }
        }
        return null
    }

    private fun extractSender(title: String, fullContent: String): String {
        val matcher = SENDER_FROM_PATTERN.matcher(fullContent)
        if (matcher.find()) {
            val candidate = matcher.group(1)?.trim().orEmpty()
            if (candidate.isNotBlank() &&
                !candidate.equals("fampay", ignoreCase = true) &&
                !candidate.equals("phonepe", ignoreCase = true) &&
                !candidate.equals("paytm", ignoreCase = true) &&
                !candidate.equals("upi", ignoreCase = true) &&
                !candidate.equals("your", ignoreCase = true)
            ) {
                return candidate
            }
        }

        val cleanTitle = title.trim()
        val lowerTitle = cleanTitle.lowercase(Locale.ROOT)
        val genericTitles = listOf(
            "fampay", "google pay", "phonepe", "paytm", "bhim", "navi",
            "amazon pay", "cred", "whatsapp", "money received", "payment received", "credited"
        )
        if (cleanTitle.isNotBlank() &&
            genericTitles.none { lowerTitle.contains(it) } &&
            !cleanTitle.contains("@")
        ) {
            return cleanTitle
        }
        return "UPI Sender"
    }

    private fun detectProviderFromEmailOrBody(email: String, body: String): String {
        val lowerEmail = email.lowercase(Locale.ROOT)
        val lowerBody = body.lowercase(Locale.ROOT)
        return when {
            lowerEmail.contains("fampay") || lowerEmail.contains("famapp") || lowerBody.contains("fampay") -> "FamPay Email"
            lowerEmail.contains("phonepe") -> "PhonePe Email"
            lowerEmail.contains("paytm") -> "Paytm Email"
            lowerEmail.contains("google") -> "GPay Email"
            lowerEmail.contains("amazon") -> "Amazon Pay Email"
            lowerEmail.contains("cred.club") -> "CRED Email"
            lowerEmail.contains("navi.com") -> "Navi Email"
            lowerEmail.contains("hdfc") -> "HDFC Bank Email"
            lowerEmail.contains("icici") -> "ICICI Bank Email"
            lowerEmail.contains("sbi") -> "SBI Email"
            lowerEmail.contains("axis") -> "Axis Bank Email"
            lowerEmail.contains("kotak") -> "Kotak Email"
            else -> "Verified Email"
        }
    }
}
