package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
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
import com.example.data.SmsMessage
import com.example.data.StorehouseRepository
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UssdSimulatorScreen(
    repository: StorehouseRepository,
    modifier: Modifier = Modifier
) {
    var dialInput by remember { mutableStateOf(repository.shortCode) }
    var selectedPhone by remember { mutableStateOf("+233241112233") }
    var smsSearchQuery by remember { mutableStateOf("") }

    // USSD Session Modal State
    var isUssdActive by remember { mutableStateOf(false) }
    var sessionHistoryText by remember { mutableStateOf("") }
    var currentPrompt by remember { mutableStateOf("") }
    var isTerminal by remember { mutableStateOf(false) }
    var userInput by remember { mutableStateOf("") }

    // SMS Drawer State
    val smsMessages by repository.smsInbox.collectAsState()
    val transactions by repository.transactions.collectAsState()
    var showCheckoutDialog by remember { mutableStateOf<SmsMessage?>(null) }

    val feeConfig by repository.feeConfig.collectAsState()
    val totalSum = repository.totalGivingSum()

    val filteredSms = smsMessages.filter {
        it.message.contains(smsSearchQuery, ignoreCase = true) ||
                it.toPhone.contains(smsSearchQuery) ||
                (it.reference?.contains(smsSearchQuery, ignoreCase = true) == true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChurchBackground)
            .padding(12.dp)
    ) {
        // Hero Visual Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(10.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(90.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.img_storehouse_banner_1793811561569),
                    contentDescription = "Storehouse Church Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A).copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Tithe Storehouse USSD",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Dial *384*100# • Africa's Talking & Paystack Gateway",
                                color = ChurchGold,
                                fontSize = 10.sp
                            )
                        }
                        Surface(
                            color = GreenSuccess,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Sum: GHS ${String.format(Locale.US, "%.0f", totalSum)}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Phone Selector & Fee Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Member:",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                AssistChip(
                    onClick = {
                        selectedPhone = if (selectedPhone == "+233241112233") "+233202223344" else "+233241112233"
                    },
                    label = { Text(selectedPhone, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.height(28.dp).testTag("switch_phone_chip")
                )
            }

            Surface(
                color = OrangeWarningLight,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "Fee: ${feeConfig.rate}%",
                    color = OrangeWarning,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Quick Code Shortcuts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                repository.shortCode to "All",
                "1" to "Tithe",
                "2" to "Offering",
                "3" to "Special",
                "4" to "History"
            ).forEach { (code, label) ->
                Button(
                    onClick = {
                        dialInput = if (code.startsWith("*")) code else code
                        // Trigger USSD with code
                        isUssdActive = true
                        sessionHistoryText = if (code.startsWith("*")) "" else code
                        val (prompt, terminal) = repository.processUssd(sessionHistoryText, selectedPhone)
                        currentPrompt = prompt
                        isTerminal = terminal
                        userInput = ""
                    },
                    modifier = Modifier.weight(1f).height(30.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ChurchSurface, contentColor = ChurchNavyPrimary),
                    border = BorderStroke(1.dp, ChurchCardBorder),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Dialer Display
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = ChurchSurface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = dialInput.ifEmpty { "Enter USSD Code" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (dialInput.isEmpty()) TextMuted else ChurchNavyPrimary
                    )
                    Text(
                        text = "Shortcode: ${repository.shortCode}",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
                if (dialInput.isNotEmpty()) {
                    IconButton(
                        onClick = { dialInput = "" },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Dial Keypad
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("*", "0", "#")
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    row.forEach { key ->
                        Button(
                            onClick = { dialInput += key },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("dial_key_$key"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChurchSurface,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, ChurchCardBorder),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(text = key, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Dial / Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (dialInput.isNotEmpty()) {
                            dialInput = dialInput.dropLast(1)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("dial_backspace"),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Backspace", tint = TextSecondary, modifier = Modifier.size(18.dp))
                }

                Button(
                    onClick = {
                        // Start USSD Session
                        isUssdActive = true
                        val initialStep = if (dialInput == repository.shortCode || dialInput.isEmpty()) "" else dialInput.replace(repository.shortCode, "").trim('*', '#')
                        sessionHistoryText = initialStep
                        val (prompt, terminal) = repository.processUssd(initialStep, selectedPhone)
                        currentPrompt = prompt
                        isTerminal = terminal
                        userInput = ""
                    },
                    modifier = Modifier
                        .weight(2.5f)
                        .height(42.dp)
                        .testTag("dial_call_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = "Dial USSD", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dial USSD Code", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Fund Sums Strip (Finding the sum and putting it inside the space left)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            colors = CardDefaults.cardColors(containerColor = ChurchSurface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = ChurchNavyPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Fund Sums: Tithe GHS ${String.format(Locale.US, "%.0f", repository.findFundSum("tithe"))} | Offering GHS ${String.format(Locale.US, "%.0f", repository.findFundSum("offering"))}",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "Total: GHS ${String.format(Locale.US, "%.2f", totalSum)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenSuccess
                )
            }
        }

        // SMS Inbox Section with Search "Find"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Sms, contentDescription = null, tint = ChurchNavyPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SMS Alerts (${filteredSms.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = TextPrimary
                )
            }
            Text(
                text = "Tap prompt to simulate MoMo pay",
                fontSize = 10.sp,
                color = GreenSuccess,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Find Search Field
        OutlinedTextField(
            value = smsSearchQuery,
            onValueChange = { smsSearchQuery = it },
            placeholder = { Text("Find SMS receipt or payment reference...", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Find", modifier = Modifier.size(16.dp)) },
            trailingIcon = {
                if (smsSearchQuery.isNotEmpty()) {
                    IconButton(onClick = { smsSearchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(bottom = 6.dp)
                .testTag("sms_search_field"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ChurchSurface,
                unfocusedContainerColor = ChurchSurface
            ),
            shape = RoundedCornerShape(6.dp)
        )

        // SMS List (Populated in the space left)
        if (filteredSms.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(ChurchSurface, RoundedCornerShape(8.dp))
                    .border(1.dp, ChurchCardBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (smsSearchQuery.isNotEmpty()) "No SMS found matching '$smsSearchQuery'" else "No SMS received yet.",
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredSms) { sms ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!sms.isReceipt && sms.reference != null) {
                                    showCheckoutDialog = sms
                                }
                            }
                            .testTag("sms_card_${sms.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (sms.isReceipt) GreenSuccessLight.copy(alpha = 0.5f) else ChurchSurface
                        ),
                        border = BorderStroke(1.dp, if (sms.isReceipt) GreenSuccess.copy(alpha = 0.4f) else ChurchCardBorder)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (sms.isReceipt) "RECEIPT (PAID)" else "PAYMENT PROMPT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sms.isReceipt) GreenSuccess else ChurchNavyPrimary
                                )
                                Text(text = sms.timestamp, fontSize = 10.sp, color = TextMuted)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = sms.message,
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                            if (!sms.isReceipt) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.background(ChurchNavyPrimary.copy(alpha = 0.08f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null, tint = ChurchNavyPrimary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tap to simulate Paystack MoMo Payment approval",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ChurchNavyPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // USSD SESSION POPUP DIALOG (SIMULATING AFRICA'S TALKING PROTOCOL)
    // =========================================================================
    if (isUssdActive) {
        Dialog(onDismissRequest = { isUssdActive = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .testTag("ussd_dialog"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = repository.shortCode,
                            color = ChurchGold,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (isTerminal) "SESSION END" else "AFRICA'S TALKING",
                            color = if (isTerminal) Color.Red else Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // USSD Prompt Text
                    Text(
                        text = currentPrompt,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 19.sp,
                        modifier = Modifier.testTag("ussd_prompt_text")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Input field (only if not terminal)
                    if (!isTerminal) {
                        OutlinedTextField(
                            value = userInput,
                            onValueChange = { userInput = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ussd_input_field"),
                            placeholder = { Text("Enter number or amount", color = Color(0xFF64748B), fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = ChurchGold,
                                unfocusedBorderColor = Color(0xFF475569)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dialog Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { isUssdActive = false },
                            modifier = Modifier.testTag("ussd_cancel_button")
                        ) {
                            Text(if (isTerminal) "Close" else "Cancel", color = Color(0xFF94A3B8))
                        }

                        if (!isTerminal) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val nextSession = if (sessionHistoryText.isEmpty()) userInput else "$sessionHistoryText*$userInput"
                                    sessionHistoryText = nextSession
                                    val (prompt, terminal) = repository.processUssd(nextSession, selectedPhone)
                                    currentPrompt = prompt
                                    isTerminal = terminal
                                    userInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ChurchGoldDark),
                                modifier = Modifier.testTag("ussd_send_button")
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
    // SIMULATED PAYSTACK MOMO APPROVAL DIALOG
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
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ChurchNavyPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Paystack Checkout Simulation", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Mobile Money Prompt / Webhook", fontSize = 11.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Ref: ${sms.reference}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Simulate donor approving mobile money prompt on their phone. This triggers the Paystack webhook callback and Africa's Talking receipt SMS.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

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
                                sms.reference?.let { ref ->
                                    repository.confirmPayment(ref)
                                }
                                showCheckoutDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
                        ) {
                            Text("Approve & Pay", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
