package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.components.ChurchLogoView
import com.example.ui.components.AboutHowItWorksCard
import com.example.ui.components.AboutHowItWorksDialog
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.Fund
import com.example.data.SmsMessage
import com.example.data.StorehouseRepository
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorehouseGivingScreen(
    repository: StorehouseRepository,
    onOpenSettings: () -> Unit,
    onNavigateToSubmitTithe: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val branding by repository.branding.collectAsState()
    val feeConfig by repository.feeConfig.collectAsState()
    val smsMessages by repository.smsInbox.collectAsState()

    // Pop-up dialog states
    var selectedFundForGiving by remember { mutableStateOf<Fund?>(null) }
    var isUssdPopupOpen by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf<SmsMessage?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // USSD state inside popup
    var ussdDialInput by remember { mutableStateOf(repository.churchShortcode) }
    var ussdSessionHistory by remember { mutableStateOf("") }
    var ussdPromptText by remember { mutableStateOf("") }
    var isUssdTerminal by remember { mutableStateOf(false) }
    var ussdUserInput by remember { mutableStateOf("") }
    var activeMemberPhone by remember { mutableStateOf("+233241112233") }

    val totalSum = repository.totalGivingSum()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ChurchBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // CACI Church Crest & Assembly Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = CardDefaults.outlinedCardBorder(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Church Logo (supports phone gallery custom photo and official crests)
                    ChurchLogoView(
                        branding = branding,
                        modifier = Modifier.size(60.dp),
                        contentDescription = "CACI Dominion Centre Logo"
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = branding.churchName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CaciRoyalBlue,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = branding.assemblyBranch,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CaciGoldDark
                        )
                        Text(
                            text = "\"${branding.motto}\"",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(36.dp).testTag("open_church_branding_button")
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Church Logo & Settings", tint = TextSecondary)
                    }
                }
            }
        }

        // Dominion Centre Assembly Information & Service Times Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = BorderStroke(1.dp, CaciBorderGold),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Church, contentDescription = null, tint = CaciRoyalBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Dominion Centre Fellowship",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = CaciRoyalBlue
                            )
                        }
                        Surface(
                            color = CaciGold.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "OPEN SANCTUARY",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = CaciRoyalBlue,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pastor: ${branding.residentPastor} • ${branding.churchAddress}",
                        fontSize = 11.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Services: ${branding.serviceTimes}",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Section explaining what the app is about and how it works
        item {
            AboutHowItWorksCard(
                branding = branding,
                onOpenFullGuide = { showAboutDialog = true }
            )
        }

        // Dominion Storehouse Target Progress Goal Card
        item {
            val targetAmount = repository.covenantGoal.targetAmount
            val progress = (totalSum / targetAmount).coerceIn(0.0, 1.0).toFloat()
            val percent = (progress * 100).toInt()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = repository.covenantGoal.monthName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CaciRoyalBlue
                        )
                        Text(
                            text = "$percent% Achieved",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenBlessing
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = GreenBlessing,
                        trackColor = Color(0xFFE2E8F0)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Received: GHS ${String.format(Locale.US, "%.2f", totalSum)}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "Target: GHS ${String.format(Locale.US, "%.2f", targetAmount)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Covenant Scripture Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CaciRoyalBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = CaciGold,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "THE STOREHOUSE PROMISE",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Malachi 3:10",
                            color = CaciGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "\"Bring ye all the tithes into the storehouse, that there may be meat in mine house, and prove me now herewith, saith the Lord of hosts... and pour you out a blessing.\"",
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }

        // Dedicated Tithe Slip Action Card (Requested Feature: Name, City, Amount)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToSubmitTithe() }
                    .testTag("submit_tithe_banner_card"),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = BorderStroke(1.5.dp, CaciGold),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = CaciGold.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = CaciRoyalBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Submit Tithe & Covenant Slip",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = CaciRoyalBlue
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = GreenBlessing.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "NEW",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenBlessing,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Input member name, city/town, and tithe amount with digital storehouse certificate.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }

                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Submit Tithe",
                        tint = CaciGoldDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Quick Actions Row: In-App Payment Pop-up & USSD Pop-up Launchers
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button 1: Direct Payment Pop-up
                Button(
                    onClick = {
                        selectedFundForGiving = repository.funds.first()
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("quick_give_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenBlessing),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Give Online", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Button 2: USSD Code Pop-up
                Button(
                    onClick = {
                        isUssdPopupOpen = true
                        ussdSessionHistory = ""
                        val (prompt, terminal) = repository.processUssd("", activeMemberPhone)
                        ussdPromptText = prompt
                        isUssdTerminal = terminal
                        ussdUserInput = ""
                    },
                    modifier = Modifier.weight(1f).height(48.dp).testTag("open_ussd_popup_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CaciRoyalBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dial USSD", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Section Title: Storehouse Giving Funds
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tithe & Covenant Giving Funds",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                Text(
                    text = "Tap to give in pop-up",
                    fontSize = 11.sp,
                    color = CaciGoldDark,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Interactive Giving Fund Cards (Click opens payment pop-up)
        items(repository.funds) { fund ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedFundForGiving = fund }
                    .testTag("fund_card_${fund.code}"),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = BorderStroke(1.dp, ChurchCardBorder),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                when (fund.code) {
                                    "tithe" -> CaciRoyalBlue.copy(alpha = 0.12f)
                                    "covenant" -> CaciGold.copy(alpha = 0.2f)
                                    "building" -> OrangeWarning.copy(alpha = 0.15f)
                                    else -> GreenBlessing.copy(alpha = 0.15f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (fund.code) {
                                "tithe" -> Icons.Default.Savings
                                "covenant" -> Icons.Default.Handshake
                                "building" -> Icons.Default.Foundation
                                "welfare" -> Icons.Default.Favorite
                                else -> Icons.Default.VolunteerActivism
                            },
                            contentDescription = fund.name,
                            tint = when (fund.code) {
                                "tithe" -> CaciRoyalBlue
                                "covenant" -> CaciGoldDark
                                "building" -> OrangeWarning
                                else -> GreenBlessing
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fund.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = fund.description,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Give to ${fund.name}",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Live Africa's Talking SMS Receipts Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sms, contentDescription = null, tint = CaciRoyalBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Storehouse SMS Receipts & Prompts (${smsMessages.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }
            }
        }

        items(smsMessages) { sms ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!sms.isReceipt && sms.reference != null) {
                            showCheckoutDialog = sms
                        }
                    }
                    .testTag("sms_item_${sms.id}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (sms.isReceipt) GreenBlessingLight.copy(alpha = 0.5f) else ChurchSurface
                ),
                border = BorderStroke(1.dp, if (sms.isReceipt) GreenBlessing.copy(alpha = 0.4f) else ChurchCardBorder),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (sms.isReceipt) "OFFICIAL SMS RECEIPT (PAID)" else "PAYMENT PROMPT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sms.isReceipt) GreenBlessing else CaciRoyalBlue
                        )
                        Text(text = sms.timestamp, fontSize = 10.sp, color = TextMuted)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = sms.message, fontSize = 11.sp, color = TextPrimary)
                    if (!sms.isReceipt) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(CaciRoyalBlue.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = CaciRoyalBlue, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Tap to open Paystack MoMo Payment Pop-up",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CaciRoyalBlue
                            )
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // 1. DEDICATED IN-APP PAYMENT POP-UP DIALOG
    // =========================================================================
    selectedFundForGiving?.let { fund ->
        var amountInput by remember { mutableStateOf("100") }
        var donorNameInput by remember { mutableStateOf("Brother Emmanuel Mensah") }
        var donorCityInput by remember { mutableStateOf("Accra") }
        var donorPhoneInput by remember { mutableStateOf(activeMemberPhone) }
        var paymentChannel by remember { mutableStateOf("momo") } // momo or card

        val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0
        val (base, fee, total) = feeConfig.calculate(parsedAmount.coerceAtLeast(1.0))

        Dialog(onDismissRequest = { selectedFundForGiving = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .padding(6.dp)
                    .testTag("giving_payment_dialog"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Title with church emblem
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CaciRoyalBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = CaciGold, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = fund.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = CaciRoyalBlue
                            )
                            Text(
                                text = branding.churchName,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Amount Chips
                    Text(text = "Select or Enter Amount (${repository.currency}):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("50", "100", "200", "500").forEach { preset ->
                            OutlinedButton(
                                onClick = { amountInput = preset },
                                modifier = Modifier.weight(1f).height(34.dp),
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (amountInput == preset) CaciRoyalBlue.copy(alpha = 0.1f) else ChurchSurface
                                ),
                                border = BorderStroke(1.dp, if (amountInput == preset) CaciRoyalBlue else ChurchCardBorder),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(preset, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("Amount (${repository.currency})") },
                        modifier = Modifier.fillMaxWidth().testTag("payment_amount_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = donorNameInput,
                        onValueChange = { donorNameInput = it },
                        label = { Text("Donor Name / Covenant Partner") },
                        modifier = Modifier.fillMaxWidth().testTag("payment_donor_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = donorCityInput,
                        onValueChange = { donorCityInput = it },
                        label = { Text("Member's City / Location") },
                        placeholder = { Text("e.g. Accra, Kumasi, Takoradi") },
                        modifier = Modifier.fillMaxWidth().testTag("payment_donor_city_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = donorPhoneInput,
                        onValueChange = { donorPhoneInput = it },
                        label = { Text("Mobile Money / Phone Number") },
                        modifier = Modifier.fillMaxWidth().testTag("payment_donor_phone_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Transparent Charge Handling Breakdown Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ChurchBackground),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Gift Amount to Church:", fontSize = 11.sp, color = TextSecondary)
                                Text("${repository.currency} ${String.format(Locale.US, "%.2f", base)}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Processing Fee (${feeConfig.rate}%):", fontSize = 11.sp, color = OrangeWarning)
                                Text("+${repository.currency} ${String.format(Locale.US, "%.2f", fee)}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = OrangeWarning)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Paid by Donor:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("${repository.currency} ${String.format(Locale.US, "%.2f", total)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GreenBlessing)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Modal Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedFundForGiving = null }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (parsedAmount > 0) {
                                    repository.makeDirectDonation(
                                        fundCode = fund.code,
                                        amount = parsedAmount,
                                        memberName = donorNameInput,
                                        phone = donorPhoneInput,
                                        city = donorCityInput.ifBlank { "Accra" }
                                    )
                                    selectedFundForGiving = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenBlessing),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("confirm_donation_button")
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pay via MoMo / Card", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // 2. DEDICATED USSD DIALER IN A POP-UP DIALOG
    // =========================================================================
    if (isUssdPopupOpen) {
        Dialog(onDismissRequest = { isUssdPopupOpen = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .padding(6.dp)
                    .testTag("ussd_popup_dialog"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // USSD Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(GreenBlessing)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = repository.churchShortcode,
                                color = CaciGold,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Text(
                            text = if (isUssdTerminal) "SESSION END" else "AFRICA'S TALKING USSD",
                            color = if (isUssdTerminal) Color.Red else Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // USSD Prompt Text
                    Text(
                        text = ussdPromptText,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.testTag("ussd_popup_prompt")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (!isUssdTerminal) {
                        OutlinedTextField(
                            value = ussdUserInput,
                            onValueChange = { ussdUserInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ussd_popup_input"),
                            placeholder = { Text("Enter choice or amount", color = Color(0xFF64748B), fontSize = 12.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CaciGold,
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isUssdPopupOpen = false }) {
                            Text(if (isUssdTerminal) "Close" else "Cancel", color = Color(0xFF94A3B8))
                        }

                        if (!isUssdTerminal) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val nextSession = if (ussdSessionHistory.isEmpty()) ussdUserInput else "$ussdSessionHistory*$ussdUserInput"
                                    ussdSessionHistory = nextSession
                                    val (prompt, terminal) = repository.processUssd(nextSession, activeMemberPhone)
                                    ussdPromptText = prompt
                                    isUssdTerminal = terminal
                                    ussdUserInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CaciGoldDark),
                                modifier = Modifier.testTag("ussd_popup_send_button")
                            ) {
                                Text("Send", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // 3. PAYSTACK SIMULATION POP-UP
    // =========================================================================
    showCheckoutDialog?.let { sms ->
        Dialog(onDismissRequest = { showCheckoutDialog = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Paystack MoMo Approval Simulation", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CaciRoyalBlue)
                    Text("Ref: ${sms.reference}", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Approve simulated transaction to trigger the Paystack webhook callback and receive the official Africa's Talking receipt SMS.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCheckoutDialog = null }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                sms.reference?.let { repository.confirmPayment(it) }
                                showCheckoutDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenBlessing)
                        ) {
                            Text("Approve & Confirm", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // 4. ABOUT & HOW IT WORKS DIALOG
    // =========================================================================
    if (showAboutDialog) {
        AboutHowItWorksDialog(
            branding = branding,
            onDismiss = { showAboutDialog = false }
        )
    }
}
