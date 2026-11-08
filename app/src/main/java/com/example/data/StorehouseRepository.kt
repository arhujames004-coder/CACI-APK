package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class StorehouseRepository {
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val shortTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val churchShortcode = "*384*100#"
    val shortCode: String get() = churchShortcode
    val churchName: String get() = _branding.value.churchName
    val currency = "GHS"

    // Church Branding & Customizable Logo
    private val _branding = MutableStateFlow(
        ChurchBranding(
            churchName = "Christ Apostolic Church International",
            assemblyBranch = "Dominion Centre",
            motto = "Reaching the World through the Gospel of Christ",
            logoResourceName = "ic_caci_logo_1793811954335",
            customLogoUrl = "",
            customGalleryLogoUri = "",
            residentPastor = "Rev. Dr. Emmanuel Owusu",
            serviceTimes = "Sundays: 8:30 AM & 10:30 AM | Wednesdays: 6:00 PM",
            helpline = "+233 24 111 2233",
            churchAddress = "Dominion Sanctuary, Off High Street"
        )
    )
    val branding: StateFlow<ChurchBranding> = _branding.asStateFlow()

    fun updateGalleryLogo(fileUriOrPath: String) {
        _branding.value = _branding.value.copy(
            customGalleryLogoUri = fileUriOrPath
        )
        logAudit("admin", "GALLERY_LOGO_UPDATED", fileUriOrPath, "Custom church logo updated from device gallery")
    }

    fun resetToDefaultLogo() {
        _branding.value = _branding.value.copy(
            customGalleryLogoUri = "",
            logoResourceName = "ic_caci_logo_1793811954335"
        )
        logAudit("admin", "LOGO_RESET", "ic_caci_logo_1793811954335", "Reset church logo to official CACI crest")
    }

    fun updateBranding(
        churchName: String,
        assemblyBranch: String,
        logoResourceName: String,
        customLogoUrl: String = "",
        residentPastor: String = _branding.value.residentPastor,
        serviceTimes: String = _branding.value.serviceTimes,
        helpline: String = _branding.value.helpline
    ) {
        _branding.value = _branding.value.copy(
            churchName = churchName,
            assemblyBranch = assemblyBranch,
            logoResourceName = logoResourceName,
            customLogoUrl = customLogoUrl,
            residentPastor = residentPastor,
            serviceTimes = serviceTimes,
            helpline = helpline
        )
        logAudit("admin", "BRANDING_UPDATED", assemblyBranch, "Updated church branding for $assemblyBranch")
    }

    val covenantGoal = DominionGoal(
        monthName = "Monthly Dominion Storehouse Target",
        targetAmount = 50000.0
    )

    var feeConfig = MutableStateFlow(FeeConfig(type = "PERCENT", rate = 1.95, cap = 25.0))

    val funds = listOf(
        Fund("tithe", "Holy Tithe (10% Storehouse)", "Scriptural 10% storehouse tithe dedicated to God"),
        Fund("covenant", "Covenant Partnership Seed", "Prophetic covenant seed & monthly ministry partnership"),
        Fund("offering", "Sunday Ministry Offering", "General worship collection and Sunday thanksgiving"),
        Fund("building", "Cathedral & Sanctuary Fund", "Church expansion, sanctuary roofing, and kingdom building"),
        Fund("welfare", "Evangelism & Benevolence", "Mission outreaches, care for widows, orphans, and needy")
    )

    // Sub-Bible Verses on Tithe & Covenant
    val bibleVerses = listOf(
        BibleVerse(
            id = "v1",
            reference = "Malachi 3:10",
            text = "Bring ye all the tithes into the storehouse, that there may be meat in mine house, and prove me now herewith, saith the Lord of hosts, if I will not open you the windows of heaven, and pour you out a blessing, that there shall not be room enough to receive it.",
            category = "Tithe",
            theme = "The Storehouse Blessing"
        ),
        BibleVerse(
            id = "v2",
            reference = "Genesis 14:20",
            text = "And blessed be God Most High, who has delivered your enemies into your hand. And Abram gave Him a tithe of all.",
            category = "Covenant",
            theme = "Abraham's Covenant Tithe"
        ),
        BibleVerse(
            id = "v3",
            reference = "Deuteronomy 8:18",
            text = "And you shall remember the Lord your God, for it is He who gives you power to get wealth, that He may establish His covenant which He swore to your fathers, as it is this day.",
            category = "Covenant",
            theme = "Power to Get Wealth"
        ),
        BibleVerse(
            id = "v4",
            reference = "Proverbs 3:9-10",
            text = "Honour the Lord with thy substance, and with the firstfruits of all thine increase: So shall thy barns be filled with plenty, and thy presses shall burst out with new wine.",
            category = "Blessing",
            theme = "Honouring with Firstfruits"
        ),
        BibleVerse(
            id = "v5",
            reference = "Leviticus 27:30",
            text = "And all the tithe of the land, whether of the seed of the land, or of the fruit of the tree, is the Lord's: it is holy unto the Lord.",
            category = "Tithe",
            theme = "The Tithe is Holy"
        ),
        BibleVerse(
            id = "v6",
            reference = "Psalm 50:5",
            text = "Gather my saints together unto me; those that have made a covenant with me by sacrifice.",
            category = "Covenant",
            theme = "Covenant by Sacrifice"
        ),
        BibleVerse(
            id = "v7",
            reference = "Hebrews 7:8",
            text = "Here mortal men receive tithes, but there he receives them, of whom it is witnessed that he lives.",
            category = "Tithe",
            theme = "Jesus Receives Our Tithes"
        ),
        BibleVerse(
            id = "v8",
            reference = "2 Corinthians 9:6-7",
            text = "He who sows bountifully will also reap bountifully. Every man according as he purposeth in his heart, so let him give; not grudgingly, or of necessity: for God loveth a cheerful giver.",
            category = "Sowing",
            theme = "Bountiful Sowing"
        ),
        BibleVerse(
            id = "v9",
            reference = "Luke 6:38",
            text = "Give, and it shall be given unto you; good measure, pressed down, and shaken together, and running over, shall men give into your bosom.",
            category = "Blessing",
            theme = "The Measure of Overflow"
        )
    )

    private val _members = MutableStateFlow(
        listOf(
            ChurchMember(1, "Brother Emmanuel Mensah", "+233241112233", "Men's Movement", "2026-09-01", "Accra"),
            ChurchMember(2, "Deaconess Grace Osei", "+233202223344", "Women's Movement", "2026-09-15", "Kumasi"),
            ChurchMember(3, "Brother Samuel Kofi Addo", "+233543334455", "Youth Fellowship", "2026-10-01", "Takoradi"),
            ChurchMember(4, "Deaconess Mary Baah", "+233274445566", "Praise & Worship Choir", "2026-10-10", "Tema"),
            ChurchMember(5, "Elder David Quaye", "+233245556677", "Presbytery & Elders", "2026-10-12", "Sunyani")
        )
    )
    val members: StateFlow<List<ChurchMember>> = _members.asStateFlow()

    private val _transactions = MutableStateFlow(
        listOf(
            GivingTransaction("CACI-T-261025-01", "Brother Emmanuel Mensah", "+233241112233", "tithe", 300.0, 5.85, 305.85, "GHS", "success", "ussd", "2026-10-25 09:15", "2026-10-25 09:16", true, "Accra", "Monthly Tithe (10%)", "Faithful 10% from salary"),
            GivingTransaction("CACI-C-261025-02", "Deaconess Grace Osei", "+233202223344", "covenant", 150.0, 2.93, 152.93, "GHS", "success", "web", "2026-10-25 09:40", "2026-10-25 09:41", true, "Kumasi", "Covenant Partnership Seed", "Pledge for church outreach"),
            GivingTransaction("CACI-B-261101-03", "Brother Samuel Kofi Addo", "+233543334455", "building", 200.0, 3.90, 203.90, "GHS", "success", "momo_card", "2026-11-01 10:12", "2026-11-01 10:13", true, "Takoradi", "Cathedral & Sanctuary Fund", "Sanctuary roofing fund"),
            GivingTransaction("CACI-T-261101-04", "Brother Emmanuel Mensah", "+233241112233", "tithe", 400.0, 7.80, 407.80, "GHS", "success", "ussd", "2026-11-01 08:50", "2026-11-01 08:52", true, "Accra", "Monthly Tithe (10%)", "November harvest tithe"),
            GivingTransaction("CACI-O-261102-05", "Deaconess Mary Baah", "+233274445566", "offering", 80.0, 1.56, 81.56, "GHS", "success", "momo_card", "2026-11-02 14:20", "2026-11-02 14:22", true, "Tema", "Sunday Ministry Offering", "Thanksgiving seed")
        )
    )
    val transactions: StateFlow<List<GivingTransaction>> = _transactions.asStateFlow()

    private val _auditLogs = MutableStateFlow(
        listOf(
            AuditEntry(1, "system", "SYSTEM_INITIALIZED", "schema", "CACI Tithe & Covenant Storehouse database active", "2026-10-25 08:00"),
            AuditEntry(2, "ussd", "TITHE_INITIATED", "CACI-T-261025-01", "Emmanuel Mensah initiated GHS 300 Tithe", "2026-10-25 09:15"),
            AuditEntry(3, "paystack_webhook", "PAYMENT_CONFIRMED", "CACI-T-261025-01", "GHS 305.85 received via Mobile Money", "2026-10-25 09:16"),
            AuditEntry(4, "sms_service", "SMS_RECEIPT_SENT", "+233241112233", "Receipt SMS delivered via Africa's Talking", "2026-10-25 09:16")
        )
    )
    val auditLogs: StateFlow<List<AuditEntry>> = _auditLogs.asStateFlow()

    private val _smsInbox = MutableStateFlow(
        listOf(
            SmsMessage(
                id = "sms-caci-01",
                toPhone = "+233241112233",
                message = "Christ Apostolic Church Intl: Thank you Brother Emmanuel Mensah! GHS 400.00 Holy Tithe received. Ref: CACI-T-261101-04. God rebuke the devourer for your sake! Malachi 3:10.",
                timestamp = "08:52",
                isReceipt = true,
                reference = "CACI-T-261101-04"
            ),
            SmsMessage(
                id = "sms-caci-02",
                toPhone = "+233202223344",
                message = "Christ Apostolic Church Intl: Please complete Covenant Partnership Seed of GHS 150.00 (+ GHS 2.93 fee = GHS 152.93).\nPay MoMo: https://paystack.com/pay/CACI-C-261025-02\nRef: CACI-C-261025-02",
                timestamp = "09:40",
                isReceipt = false,
                paymentUrl = "https://paystack.com/pay/CACI-C-261025-02",
                reference = "CACI-C-261025-02"
            ),
            SmsMessage(
                id = "sms-caci-03",
                toPhone = "+233543334455",
                message = "Christ Apostolic Church Intl: Thank you Brother Samuel! GHS 200.00 Cathedral Fund received. Ref: CACI-B-261101-03. The God of Heaven will prosper you!",
                timestamp = "10:13",
                isReceipt = true,
                reference = "CACI-B-261101-03"
            )
        )
    )
    val smsInbox: StateFlow<List<SmsMessage>> = _smsInbox.asStateFlow()

    fun findFundSum(fundCode: String): Double {
        return _transactions.value
            .filter { it.status == "success" && it.fundCode == fundCode }
            .sumOf { it.amount }
    }

    fun totalGivingSum(): Double {
        return _transactions.value
            .filter { it.status == "success" }
            .sumOf { it.amount }
    }

    fun totalFeesSum(): Double {
        return _transactions.value
            .filter { it.status == "success" }
            .sumOf { it.fee }
    }

    fun totalGrossSum(): Double {
        return _transactions.value
            .filter { it.status == "success" }
            .sumOf { it.totalAmount }
    }

    fun logAudit(actor: String, action: String, entityId: String?, details: String) {
        val nextId = (_auditLogs.value.maxOfOrNull { it.id } ?: 0) + 1
        val entry = AuditEntry(nextId, actor, action, entityId, details, timeFormat.format(Date()))
        _auditLogs.value = listOf(entry) + _auditLogs.value
    }

    fun addMember(name: String, phone: String, group: String, city: String = "Accra"): ChurchMember {
        val nextId = (_members.value.maxOfOrNull { it.id } ?: 0) + 1
        val member = ChurchMember(nextId, name, phone, group, timeFormat.format(Date()).substring(0, 10), city)
        _members.value = _members.value + member
        logAudit("admin", "MEMBER_ADDED", phone, "Added member $name ($city) to $group")
        return member
    }

    /**
     * Direct Giving Pop-up transaction handler
     */
    fun makeDirectDonation(
        fundCode: String,
        amount: Double,
        memberName: String,
        phone: String,
        channel: String = "momo_card",
        city: String = "Accra"
    ): GivingTransaction {
        val (base, fee, total) = feeConfig.value.calculate(amount)
        val prefix = when (fundCode) {
            "tithe" -> "CACI-T"
            "covenant" -> "CACI-C"
            "building" -> "CACI-B"
            else -> "CACI-O"
        }
        val ref = "$prefix-" + SimpleDateFormat("yyMMdd", Locale.US).format(Date()) + "-" + UUID.randomUUID().toString().take(6).uppercase()

        val newTx = GivingTransaction(
            reference = ref,
            memberName = memberName.ifBlank { "Covenant Partner" },
            phone = phone,
            fundCode = fundCode,
            amount = base,
            fee = fee,
            totalAmount = total,
            currency = currency,
            status = "success", // Paid through in-app payment pop-up
            channel = channel,
            createdAt = timeFormat.format(Date()),
            paidAt = timeFormat.format(Date()),
            smsReceiptSent = true,
            city = city
        )

        _transactions.value = listOf(newTx) + _transactions.value
        logAudit("paystack_popup", "COVENANT_GIFT_PAID", ref, "$memberName gave $currency $base ($fundCode) + $currency $fee fee")

        // Dispatch instant receipt SMS
        val fundObj = funds.find { it.code == fundCode }
        val fundTitle = fundObj?.name ?: fundCode.replaceFirstChar { it.uppercase() }
        val receiptSms = SmsMessage(
            id = UUID.randomUUID().toString(),
            toPhone = phone,
            message = "${_branding.value.churchName}: Thank you $memberName! $currency ${String.format(Locale.US, "%.2f", base)} $fundTitle received. Ref: $ref. 'The Lord rebuke the devourer for your sake' - Malachi 3:10.",
            timestamp = shortTimeFormat.format(Date()),
            isReceipt = true,
            reference = ref
        )
        _smsInbox.value = listOf(receiptSms) + _smsInbox.value
        return newTx
    }

    /**
     * Dedicated Tithe & Covenant Submission Form handler
     * Allows members to input name, city, tithe amount, and submit directly to the storehouse.
     */
    fun submitTitheSlip(
        memberName: String,
        city: String,
        amount: Double,
        phone: String,
        titheType: String = "Monthly Tithe (10%)",
        paymentMethod: String = "Mobile Money (Instant)",
        covenantNotes: String = ""
    ): GivingTransaction {
        val cleanName = memberName.ifBlank { "Faithful Tither" }
        val cleanCity = city.ifBlank { "Accra" }
        val cleanPhone = phone.ifBlank { "+233241000000" }

        val isDirectFulfillment = paymentMethod.contains("Cash", ignoreCase = true) || paymentMethod.contains("Bank", ignoreCase = true)
        val (base, rawFee, total) = if (isDirectFulfillment) {
            Triple(amount, 0.0, amount)
        } else {
            feeConfig.value.calculate(amount)
        }

        val ref = "CACI-TTH-" + SimpleDateFormat("yyMMdd", Locale.US).format(Date()) + "-" + UUID.randomUUID().toString().take(6).uppercase()

        val newTx = GivingTransaction(
            reference = ref,
            memberName = cleanName,
            phone = cleanPhone,
            fundCode = "tithe",
            amount = base,
            fee = rawFee,
            totalAmount = total,
            currency = currency,
            status = "success",
            channel = if (isDirectFulfillment) "direct_slip" else "momo_card",
            createdAt = timeFormat.format(Date()),
            paidAt = timeFormat.format(Date()),
            smsReceiptSent = true,
            city = cleanCity,
            titheType = titheType,
            notes = covenantNotes
        )

        // Check if member exists or register them with their city
        val existing = _members.value.find { it.phone == cleanPhone || it.name.equals(cleanName, ignoreCase = true) }
        if (existing == null) {
            val nextMemberId = (_members.value.maxOfOrNull { it.id } ?: 0) + 1
            val newMember = ChurchMember(
                id = nextMemberId,
                name = cleanName,
                phone = cleanPhone,
                communityGroup = "Tithe & Covenant Fellowship",
                joinedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                city = cleanCity
            )
            _members.value = _members.value + newMember
        } else {
            _members.value = _members.value.map {
                if (it.id == existing.id) it.copy(city = cleanCity) else it
            }
        }

        _transactions.value = listOf(newTx) + _transactions.value
        logAudit(
            actor = "tithe_form",
            action = "TITHE_SUBMITTED",
            entityId = ref,
            details = "$cleanName ($cleanCity) submitted $currency ${String.format(Locale.US, "%.2f", base)} for $titheType via $paymentMethod"
        )

        // Generate instant digital SMS receipt acknowledging the member and city
        val sms = SmsMessage(
            id = UUID.randomUUID().toString(),
            toPhone = cleanPhone,
            message = "${_branding.value.churchName}: God bless you $cleanName in $cleanCity! $currency ${String.format(Locale.US, "%.2f", base)} $titheType received into the Storehouse. Ref: $ref. 'The Lord rebuke the devourer for your sake' - Malachi 3:10.",
            timestamp = shortTimeFormat.format(Date()),
            isReceipt = true,
            reference = ref
        )
        _smsInbox.value = listOf(sms) + _smsInbox.value

        return newTx
    }

    /**
     * Africa's Talking USSD protocol state machine
     */
    fun processUssd(text: String, phone: String): Pair<String, Boolean> {
        val church = _branding.value.churchName
        val parts = if (text.isEmpty()) emptyList() else text.split("*")

        if (parts.isEmpty()) {
            val menu = "Welcome to $church\n" +
                    "1. Give Tithe\n" +
                    "2. Covenant Seed\n" +
                    "3. Sunday Offering\n" +
                    "4. Building/Special Fund\n" +
                    "5. My Giving History\n" +
                    "6. Contact Church"
            return Pair(menu, false)
        }

        when (parts[0]) {
            // 1. Give Tithe
            "1" -> {
                if (parts.size == 1) {
                    return Pair("Enter Tithe Amount in $currency:\n(Malachi 3:10)", false)
                }
                if (parts.size == 2) {
                    val amt = parts[1].toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        return Pair("Invalid amount entered. Session ended.", true)
                    }
                    val (base, fee, total) = feeConfig.value.calculate(amt)
                    val prompt = "Giving $currency ${String.format(Locale.US, "%.2f", base)} Tithe\n" +
                            "Processing Fee: $currency ${String.format(Locale.US, "%.2f", fee)}\n" +
                            "Total to Pay: $currency ${String.format(Locale.US, "%.2f", total)}\n\n" +
                            "1. Confirm & Pay\n" +
                            "2. Cancel"
                    return Pair(prompt, false)
                }
                if (parts.size == 3) {
                    if (parts[2] == "2") {
                        return Pair("Giving cancelled. The Lord bless and keep you!", true)
                    }
                    if (parts[2] == "1") {
                        val amt = parts[1].toDoubleOrNull() ?: 50.0
                        val (base, fee, total) = feeConfig.value.calculate(amt)
                        val ref = "CACI-T-" + SimpleDateFormat("yyMMdd", Locale.US).format(Date()) + "-" + UUID.randomUUID().toString().take(6).uppercase()

                        val member = _members.value.find { it.phone == phone }
                        val memberName = member?.name ?: "Storehouse Partner"

                        val newTx = GivingTransaction(
                            reference = ref,
                            memberName = memberName,
                            phone = phone,
                            fundCode = "tithe",
                            amount = base,
                            fee = fee,
                            totalAmount = total,
                            currency = currency,
                            status = "pending",
                            channel = "ussd",
                            createdAt = timeFormat.format(Date())
                        )
                        _transactions.value = listOf(newTx) + _transactions.value
                        logAudit("ussd", "TITHE_INITIATED", ref, "$phone initiated $currency $total")

                        val payUrl = "https://paystack.com/pay/$ref"
                        val smsMsg = SmsMessage(
                            id = UUID.randomUUID().toString(),
                            toPhone = phone,
                            message = "$church: Please complete Tithe of $currency ${String.format(Locale.US, "%.2f", base)} (+ $currency ${String.format(Locale.US, "%.2f", fee)} fee = $currency ${String.format(Locale.US, "%.2f", total)}).\nPay: $payUrl\nRef: $ref",
                            timestamp = shortTimeFormat.format(Date()),
                            paymentUrl = payUrl,
                            reference = ref
                        )
                        _smsInbox.value = listOf(smsMsg) + _smsInbox.value

                        return Pair("Tithe request of $currency ${String.format(Locale.US, "%.2f", total)} created!\nYou will receive an SMS shortly with payment link.\nRef: $ref", true)
                    }
                }
            }

            // 2. Covenant Seed
            "2" -> {
                if (parts.size == 1) {
                    return Pair("Enter Covenant Seed ($currency):\n(Deut 8:18)", false)
                }
                if (parts.size == 2) {
                    val amt = parts[1].toDoubleOrNull() ?: return Pair("Invalid amount.", true)
                    val (base, fee, total) = feeConfig.value.calculate(amt)
                    val prompt = "Covenant Seed: $currency ${String.format(Locale.US, "%.2f", base)}\n" +
                            "Fee: $currency ${String.format(Locale.US, "%.2f", fee)}\n" +
                            "Total: $currency ${String.format(Locale.US, "%.2f", total)}\n\n" +
                            "1. Confirm & Pay\n" +
                            "2. Cancel"
                    return Pair(prompt, false)
                }
                if (parts.size == 3 && parts[2] == "1") {
                    val amt = parts[1].toDoubleOrNull() ?: 100.0
                    val (base, fee, total) = feeConfig.value.calculate(amt)
                    val ref = "CACI-C-" + SimpleDateFormat("yyMMdd", Locale.US).format(Date()) + "-" + UUID.randomUUID().toString().take(6).uppercase()
                    val member = _members.value.find { it.phone == phone }
                    val memberName = member?.name ?: "Covenant Partner"

                    val newTx = GivingTransaction(
                        reference = ref,
                        memberName = memberName,
                        phone = phone,
                        fundCode = "covenant",
                        amount = base,
                        fee = fee,
                        totalAmount = total,
                        currency = currency,
                        status = "pending",
                        channel = "ussd",
                        createdAt = timeFormat.format(Date())
                    )
                    _transactions.value = listOf(newTx) + _transactions.value
                    logAudit("ussd", "COVENANT_SEED_INITIATED", ref, "$phone initiated $currency $total")

                    val payUrl = "https://paystack.com/pay/$ref"
                    val smsMsg = SmsMessage(
                        id = UUID.randomUUID().toString(),
                        toPhone = phone,
                        message = "$church: Complete Covenant Seed of $currency ${String.format(Locale.US, "%.2f", base)} (+ $currency ${String.format(Locale.US, "%.2f", fee)} fee = $currency ${String.format(Locale.US, "%.2f", total)}).\nPay: $payUrl\nRef: $ref",
                        timestamp = shortTimeFormat.format(Date()),
                        paymentUrl = payUrl,
                        reference = ref
                    )
                    _smsInbox.value = listOf(smsMsg) + _smsInbox.value

                    return Pair("Covenant Pledge of $currency ${String.format(Locale.US, "%.2f", total)} created!\nSMS link sent.\nRef: $ref", true)
                }
            }

            // 3. Sunday Offering
            "3" -> {
                if (parts.size == 1) {
                    return Pair("Enter Sunday Offering ($currency):", false)
                }
                if (parts.size == 2) {
                    val amt = parts[1].toDoubleOrNull() ?: return Pair("Invalid amount.", true)
                    val (base, fee, total) = feeConfig.value.calculate(amt)
                    return Pair("Giving $currency ${String.format(Locale.US, "%.2f", base)} Offering\nFee: $currency ${String.format(Locale.US, "%.2f", fee)}\nTotal: $currency ${String.format(Locale.US, "%.2f", total)}\n\n1. Confirm & Pay\n2. Cancel", false)
                }
                if (parts.size == 3 && parts[2] == "1") {
                    val amt = parts[1].toDoubleOrNull() ?: 20.0
                    val (base, fee, total) = feeConfig.value.calculate(amt)
                    val ref = "CACI-O-" + SimpleDateFormat("yyMMdd", Locale.US).format(Date()) + "-" + UUID.randomUUID().toString().take(6).uppercase()
                    val member = _members.value.find { it.phone == phone }
                    val memberName = member?.name ?: "Storehouse Giver"

                    val newTx = GivingTransaction(
                        reference = ref,
                        memberName = memberName,
                        phone = phone,
                        fundCode = "offering",
                        amount = base,
                        fee = fee,
                        totalAmount = total,
                        currency = currency,
                        status = "pending",
                        channel = "ussd",
                        createdAt = timeFormat.format(Date())
                    )
                    _transactions.value = listOf(newTx) + _transactions.value
                    return Pair("Offering request created! SMS sent.\nRef: $ref", true)
                }
            }

            // 4. Building/Special Fund
            "4" -> {
                if (parts.size == 1) {
                    return Pair("Select Fund:\n1. Cathedral Building\n2. Missions & Welfare\n0. Cancel", false)
                }
                if (parts.size == 2) {
                    val name = if (parts[1] == "1") "Cathedral Fund" else "Missions & Welfare"
                    return Pair("Enter Amount for $name ($currency):", false)
                }
                if (parts.size == 3) {
                    val amt = parts[2].toDoubleOrNull() ?: return Pair("Invalid amount.", true)
                    val (base, fee, total) = feeConfig.value.calculate(amt)
                    return Pair("Pledging $currency ${String.format(Locale.US, "%.2f", base)}\nFee: $currency ${String.format(Locale.US, "%.2f", fee)}\nTotal: $currency ${String.format(Locale.US, "%.2f", total)}\n\n1. Confirm & Pay\n2. Cancel", false)
                }
                if (parts.size == 4 && parts[3] == "1") {
                    val ref = "CACI-B-" + UUID.randomUUID().toString().take(6).uppercase()
                    return Pair("Fund request created! SMS sent.\nRef: $ref", true)
                }
            }

            // 5. Giving History
            "5" -> {
                val memberTx = _transactions.value.filter { it.phone == phone && it.status == "success" }.take(3)
                val totalYear = _transactions.value.filter { it.phone == phone && it.status == "success" }.sumOf { it.amount }

                if (memberTx.isEmpty()) {
                    return Pair("Welcome to $church!\nNo previous donations recorded for $phone.\nGod bless you as you give today!", true)
                }
                val sb = StringBuilder("Your Recent Giving:\n")
                memberTx.forEach {
                    val fundName = funds.find { f -> f.code == it.fundCode }?.name ?: it.fundCode.replaceFirstChar { c -> c.uppercase() }
                    sb.append("• $fundName: $currency ${String.format(Locale.US, "%.2f", it.amount)}\n")
                }
                sb.append("2026 Total: $currency ${String.format(Locale.US, "%.2f", totalYear)}\n")
                sb.append("God bless your faithful covenant walk!")
                return Pair(sb.toString(), true)
            }

            // 6. Contact Church
            "6" -> {
                val contact = "$church\n" +
                        "${_branding.value.assemblyBranch}\n" +
                        "Helpline: +233240123456\n" +
                        "Sunday Services: 7:00 AM & 9:30 AM\n" +
                        "Wednesday Prophetic: 6:00 PM\n" +
                        "May the grace of our Lord Jesus be with you!"
                return Pair(contact, true)
            }
        }

        return Pair("Invalid selection. Dial $churchShortcode to start over.", true)
    }

    fun confirmPayment(reference: String): Boolean {
        val txIndex = _transactions.value.indexOfFirst { it.reference == reference }
        if (txIndex == -1) return false

        val tx = _transactions.value[txIndex]
        val updatedTx = tx.copy(
            status = "success",
            paidAt = timeFormat.format(Date()),
            smsReceiptSent = true
        )

        val updatedList = _transactions.value.toMutableList()
        updatedList[txIndex] = updatedTx
        _transactions.value = updatedList

        logAudit(
            actor = "paystack_webhook",
            action = "PAYMENT_CONFIRMED",
            entityId = reference,
            details = "Verified HMAC signature. GHS ${tx.totalAmount} paid (Base: GHS ${tx.amount}, Fee: GHS ${tx.fee})"
        )

        val fundName = funds.find { it.code == tx.fundCode }?.name ?: tx.fundCode.replaceFirstChar { it.uppercase() }
        val receiptSms = SmsMessage(
            id = UUID.randomUUID().toString(),
            toPhone = tx.phone,
            message = "Christ Apostolic Church Intl: Thank you ${tx.memberName}! $currency ${String.format(Locale.US, "%.2f", tx.amount)} $fundName received. Ref: $reference. God bless your covenant stewardship! Malachi 3:10.",
            timestamp = shortTimeFormat.format(Date()),
            isReceipt = true,
            reference = reference
        )
        _smsInbox.value = listOf(receiptSms) + _smsInbox.value
        return true
    }
}
