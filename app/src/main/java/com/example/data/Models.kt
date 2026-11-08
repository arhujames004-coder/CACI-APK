package com.example.data

data class Fund(
    val code: String,
    val name: String,
    val description: String,
    val iconName: String = "church"
)

data class ChurchMember(
    val id: Int,
    val name: String,
    val phone: String,
    val communityGroup: String,
    val joinedDate: String = "2026-10-15",
    val city: String = "Accra"
)

data class GivingTransaction(
    val reference: String,
    val memberName: String,
    val phone: String,
    val fundCode: String,
    val amount: Double,       // Base net amount to church
    val fee: Double,          // Configurable processing fee
    val totalAmount: Double,  // Total paid by donor
    val currency: String = "GHS",
    val status: String = "success", // pending, success, failed
    val channel: String = "ussd", // ussd, web, momo_card, direct_slip
    val createdAt: String,
    val paidAt: String? = null,
    val smsReceiptSent: Boolean = false,
    val city: String = "Accra",
    val titheType: String = "Monthly Tithe (10%)",
    val notes: String = ""
)

data class AuditEntry(
    val id: Int,
    val actor: String,
    val action: String,
    val entityId: String?,
    val details: String,
    val timestamp: String
)

data class SmsMessage(
    val id: String,
    val toPhone: String,
    val message: String,
    val timestamp: String,
    val isReceipt: Boolean = false,
    val paymentUrl: String? = null,
    val reference: String? = null
)

data class BibleVerse(
    val id: String,
    val reference: String,
    val text: String,
    val category: String, // Tithe, Covenant, Blessing, Sowing
    val theme: String
)

data class ChurchBranding(
    val churchName: String = "Christ Apostolic Church International",
    val assemblyBranch: String = "Dominion Centre",
    val motto: String = "Reaching the World through the Gospel of Christ",
    val logoResourceName: String = "ic_caci_logo_1793811954335",
    val customLogoUrl: String = "",
    val customGalleryLogoUri: String = "", // Saved absolute file path of picked gallery logo
    val residentPastor: String = "Rev. Dr. Emmanuel Owusu",
    val serviceTimes: String = "Sundays: 8:30 AM & 10:30 AM | Wednesdays: 6:00 PM",
    val helpline: String = "+233 24 111 2233",
    val churchAddress: String = "Dominion Sanctuary, Off High Street"
)

data class DominionGoal(
    val monthName: String = "Storehouse Covenant Target",
    val targetAmount: Double = 50000.0
)

data class FeeConfig(
    val type: String = "PERCENT", // PERCENT or FLAT
    val rate: Double = 1.95,      // 1.95%
    val cap: Double = 25.0        // GHS 25 max fee
) {
    fun calculate(amount: Double): Triple<Double, Double, Double> {
        val base = kotlin.math.round(amount * 100.0) / 100.0
        var fee = if (type == "PERCENT") {
            kotlin.math.round((base * (rate / 100.0)) * 100.0) / 100.0
        } else {
            rate
        }
        if (cap > 0 && fee > cap) {
            fee = cap
        }
        val total = kotlin.math.round((base + fee) * 100.0) / 100.0
        return Triple(base, fee, total)
    }
}
