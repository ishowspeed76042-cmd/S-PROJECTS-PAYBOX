package com.example

import com.example.service.PaymentNotificationParser
import com.example.service.PaymentNotificationParser.IgnoreReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentNotificationParserTest {

    @Test
    fun supportsAllMajorAndMinorIndianUpiAppsWhenSelected() {
        val validCases = listOf(
            Triple("com.phonepe.app", "Money Received", "You have received ₹250 from Priya") to "PhonePe",
            Triple("com.google.android.apps.nbu.paisa.user", "Google Pay", "₹150.50 received from Amit") to "Google Pay (GPay)",
            Triple("net.one97.paytm", "Paytm", "Received Rs. 500 from Suresh") to "Paytm",
            Triple("com.naviapp", "Navi UPI", "₹120 received from Rohan") to "Navi",
            Triple("in.fampay.app", "FamPay", "You received ₹50 from Rahul") to "FamPay / FamX",
            Triple("in.org.npci.upiapp", "BHIM", "Rs. 75 credited via UPI from Neha") to "BHIM UPI",
            Triple("in.amazon.mShop.android.shopping", "Amazon Pay", "₹300 received from Vikram via UPI") to "Amazon Pay",
            Triple("com.dreamplug.androidapp", "CRED", "₹1000 received from Kavita") to "CRED",
            Triple("com.bharatpe.app", "BharatPe", "Payment received ₹85 from Customer") to "BharatPe",
            Triple("money.super.payments", "super.money", "₹40 received via UPI") to "super.money"
        )

        validCases.forEach { (input, expectedApp) ->
            val (pkg, title, text) = input
            val result = PaymentNotificationParser.parse(pkg, title, text)
            assertTrue("Expected $expectedApp ($pkg) to succeed", result.isValidCredit)
            assertEquals(expectedApp, result.appName)
            assertTrue(result.spokenText.startsWith("S-PROJECTS Pay Box par "))
            assertTrue(result.spokenText.endsWith(" rupaye mile."))
        }
    }

    @Test
    fun respectsMerchantSelectedUpiAppFilter() {
        val selectedOnlyPhonePeAndFamPay = setOf("phonepe", "fampay")

        val phonePe = PaymentNotificationParser.parse(
            packageName = "com.phonepe.app",
            title = "PhonePe",
            text = "Received ₹100 from Ajay",
            allowedAppIds = selectedOnlyPhonePeAndFamPay
        )
        assertTrue(phonePe.isValidCredit)

        val paytmDisabled = PaymentNotificationParser.parse(
            packageName = "net.one97.paytm",
            title = "Paytm",
            text = "Received ₹100 from Ajay",
            allowedAppIds = selectedOnlyPhonePeAndFamPay
        )
        assertFalse(paytmDisabled.isValidCredit)
        assertEquals(IgnoreReason.APP_NOT_SELECTED_BY_USER, paytmDisabled.ignoreReason)
    }

    @Test
    fun validatesWhatsappPayReceiptAndBlocksCasualChats() {
        val casualChat = PaymentNotificationParser.parse(
            packageName = "com.whatsapp",
            title = "Rahul",
            text = "Hey bro you received ₹500 yesterday?"
        )
        assertFalse(casualChat.isValidCredit)
        assertEquals(IgnoreReason.WHATSAPP_NON_PAYMENT_CHAT, casualChat.ignoreReason)

        val validWhatsappPay = PaymentNotificationParser.parse(
            packageName = "com.whatsapp",
            title = "WhatsApp Pay",
            text = "Payment received: ₹450 credited to your bank account via UPI"
        )
        assertTrue(validWhatsappPay.isValidCredit)
        assertEquals(450.0, validWhatsappPay.amount, 0.001)
        assertEquals("S-PROJECTS Pay Box par 450 rupaye mile.", validWhatsappPay.spokenText)
    }

    @Test
    fun rejectsUnverifiedThirdPartyPackages() {
        val spoofedPackages = listOf("org.telegram.messenger", "com.fake.spoofer", "com.example.custom")
        spoofedPackages.forEach { pkg ->
            val result = PaymentNotificationParser.parse(pkg, "FamPay", "You received ₹500 from Rahul")
            assertFalse(result.isValidCredit)
            assertEquals(IgnoreReason.UNVERIFIED_PACKAGE, result.ignoreReason)
        }
    }

    @Test
    fun rejectsSpoofedEmailSendersAndAcceptsOfficialProviderDomains() {
        val spoofed = PaymentNotificationParser.parse(
            packageName = "com.google.android.gm",
            title = "PhonePe",
            text = "You received ₹500",
            senderEmail = "noreply@phonepe.com <attacker@gmail.com>"
        )
        assertFalse(spoofed.isValidCredit)
        assertEquals(IgnoreReason.UNVERIFIED_EMAIL_SENDER, spoofed.ignoreReason)

        val officialPhonePeEmail = PaymentNotificationParser.parseEmail(
            fromHeader = "PhonePe <noreply@phonepe.com>",
            subject = "Money Received",
            body = "You received ₹320.50 via UPI"
        )
        assertTrue(officialPhonePeEmail.isValidCredit)
        assertEquals("PhonePe Email", officialPhonePeEmail.appName)
        assertEquals("S-PROJECTS Pay Box par 320.50 rupaye mile.", officialPhonePeEmail.spokenText)
    }

    @Test
    fun rejectsPromotionalAndDebitMessages() {
        val promo = PaymentNotificationParser.parse(
            packageName = "com.phonepe.app",
            title = "PhonePe Scratch Card",
            text = "Get ₹5 cashback on your next payment!"
        )
        assertFalse(promo.isValidCredit)
        assertEquals(IgnoreReason.PROMOTIONAL, promo.ignoreReason)

        val debit = PaymentNotificationParser.parse(
            packageName = "com.google.android.apps.nbu.paisa.user",
            title = "Google Pay",
            text = "You sent ₹200 to Rohan"
        )
        assertFalse(debit.isValidCredit)
        assertEquals(IgnoreReason.DEBIT_OR_NON_CREDIT, debit.ignoreReason)
    }
}
