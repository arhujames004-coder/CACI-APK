package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.components.ChurchLogoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.GivingTransaction
import com.example.data.StorehouseRepository
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitTitheScreen(
    repository: StorehouseRepository,
    onNavigateToTreasury: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val branding by repository.branding.collectAsState()
    val feeConfig by repository.feeConfig.collectAsState()
    val transactions by repository.transactions.collectAsState()

    // Form inputs: Name, City, Amount, Phone, Tithe Type, Payment Method, Prayer Notes
    var memberName by remember { mutableStateOf("") }
    var memberCity by remember { mutableStateOf("Accra") }
    var phoneInput by remember { mutableStateOf("+233241112233") }
    var amountInput by remember { mutableStateOf("200") }
    var selectedTitheType by remember { mutableStateOf("Monthly Tithe (10%)") }
    var selectedPaymentMethod by remember { mutableStateOf("Mobile Money (Instant)") }
    var prayerNotes by remember { mutableStateOf("") }
    var showTitheCalculator by remember { mutableStateOf(false) }
    var incomeInput by remember { mutableStateOf("") }

    // Dialog state for completed Tithe Receipt / Certificate
    var submittedReceipt by remember { mutableStateOf<GivingTransaction?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val quickCities = listOf(
        "Accra", "Kumasi", "Takoradi", "Tema", "Sunyani",
        "Cape Coast", "Koforidua", "Tamale", "London (UK)", "Dallas (USA)"
    )

    val titheTypes = listOf(
        "Monthly Tithe (10%)",
        "Covenant Partnership Seed",
        "First Fruits Offering",
        "Business & Harvest Tithe"
    )

    val quickAmounts = listOf("50", "100", "250", "500", "1000", "2500")

    val paymentMethods = listOf(
        "Mobile Money (Instant)",
        "Bank Card (Visa/Mastercard)",
        "Direct Bank Deposit",
        "Cash in Tithe Envelope"
    )

    val parsedAmount = amountInput.toDoubleOrNull() ?: 0.0
    val isDirect = selectedPaymentMethod.contains("Cash") || selectedPaymentMethod.contains("Bank Deposit")
    val (baseAmt, feeAmt, totalAmt) = if (isDirect) {
        Triple(parsedAmount, 0.0, parsedAmount)
    } else {
        feeConfig.calculate(parsedAmount)
    }

    // Filter recent tithe submissions
    val recentTithes = transactions.filter { it.fundCode == "tithe" || it.fundCode == "covenant" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ChurchBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header Card: CACI Tithe Slip Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CaciRoyalBlue),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChurchLogoView(
                            branding = branding,
                            modifier = Modifier.size(48.dp),
                            contentDescription = "Dominion Centre Logo"
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Surface(
                                color = CaciGold,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "OFFICIAL STOREHOUSE SLIP",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Submit Member Tithe & Covenant",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = "${branding.churchName} • ${branding.assemblyBranch}",
                                fontSize = 11.sp,
                                color = CaciGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "\"Bring ye all the tithes into the storehouse, that there may be meat in mine house, and prove me now herewith...\" — Malachi 3:10",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // The Tithe Input Form Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = BorderStroke(1.dp, CaciBorderGold),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "1. Member Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CaciRoyalBlue
                    )

                    // Member Name Input
                    OutlinedTextField(
                        value = memberName,
                        onValueChange = {
                            memberName = it
                            validationError = null
                        },
                        label = { Text("Member's Full Name *") },
                        placeholder = { Text("e.g., Bro. Kwame Mensah / Sister Grace") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = CaciRoyalBlue)
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("member_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CaciRoyalBlue,
                            unfocusedBorderColor = Color(0xFFD1D5DB)
                        )
                    )

                    // Member City Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = memberCity,
                            onValueChange = {
                                memberCity = it
                                validationError = null
                            },
                            label = { Text("Member's City / Town / Diaspora *") },
                            placeholder = { Text("e.g. Accra, Kumasi, London, Atlanta") },
                            leadingIcon = {
                                Icon(Icons.Default.LocationCity, contentDescription = null, tint = CaciGoldDark)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("member_city_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CaciRoyalBlue,
                                unfocusedBorderColor = Color(0xFFD1D5DB)
                            )
                        )

                        // Quick City Selection Chips
                        Text(
                            text = "Quick Select City:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(quickCities) { city ->
                                val isSelected = memberCity.equals(city, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { memberCity = city },
                                    label = { Text(city, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CaciGold,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }
                    }

                    // Phone Number Input
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone / MoMo Number (for SMS Receipt)") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = TextSecondary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("member_phone_input")
                    )

                    HorizontalDivider(color = Color(0xFFE5E7EB))

                    Text(
                        text = "2. Tithe & Covenant Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CaciRoyalBlue
                    )

                    // Tithe Type Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Select Tithe Category:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        titheTypes.forEach { type ->
                            val isSelected = selectedTitheType == type
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTitheType = type },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CaciRoyalBlue.copy(alpha = 0.08f) else Color.Transparent,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) CaciRoyalBlue else Color(0xFFE5E7EB)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedTitheType = type },
                                        colors = RadioButtonDefaults.colors(selectedColor = CaciRoyalBlue)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = type,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) CaciRoyalBlue else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Interactive 10% Tithe Calculator Tool
                    Surface(
                        color = CaciRoyalBlue.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, CaciBorderGold)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showTitheCalculator = !showTitheCalculator },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Calculate, contentDescription = null, tint = CaciRoyalBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Biblical 10% Tithe Calculator", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CaciRoyalBlue)
                                }
                                Text(
                                    text = if (showTitheCalculator) "Hide" else "Calculate Tithe",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CaciGoldDark
                                )
                            }

                            if (showTitheCalculator) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Enter your monthly salary, business turnover, or profit to compute your exact 10% storehouse tithe:",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = incomeInput,
                                    onValueChange = { incomeInput = it },
                                    label = { Text("Income / Revenue (GHS)") },
                                    placeholder = { Text("e.g. 3500.00") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("income_calculator_input")
                                )

                                val parsedIncome = incomeInput.toDoubleOrNull() ?: 0.0
                                val calculatedTithe = parsedIncome * 0.10

                                if (parsedIncome > 0) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "10% Tithe: GHS ${String.format(Locale.US, "%.2f", calculatedTithe)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = GreenBlessing
                                        )

                                        Button(
                                            onClick = {
                                                amountInput = String.format(Locale.US, "%.2f", calculatedTithe)
                                                Toast.makeText(context, "Applied GHS ${String.format(Locale.US, "%.2f", calculatedTithe)} to tithe amount!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = GreenBlessing),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(34.dp).testTag("apply_calculated_tithe_button")
                                        ) {
                                            Text("Apply to Slip", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Tithe Amount Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = {
                                amountInput = it
                                validationError = null
                            },
                            label = { Text("Tithe Amount (GHS) *") },
                            placeholder = { Text("e.g. 200.00") },
                            leadingIcon = {
                                Icon(Icons.Default.Payments, contentDescription = null, tint = GreenBlessing)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tithe_amount_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GreenBlessing,
                                unfocusedBorderColor = Color(0xFFD1D5DB)
                            )
                        )

                        // Quick Amount Preset Chips
                        Text(
                            text = "Preset Tithe Amounts:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(quickAmounts) { amt ->
                                val isSelected = amountInput == amt
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { amountInput = amt },
                                    label = { Text("GHS $amt", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = GreenBlessing,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Payment / Fulfillment Method
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Submission & Payment Method:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(paymentMethods) { method ->
                                val isSelected = selectedPaymentMethod == method
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedPaymentMethod = method },
                                    label = { Text(method, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CaciRoyalBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Covenant Prayer Request / Faith Confession
                    OutlinedTextField(
                        value = prayerNotes,
                        onValueChange = { prayerNotes = it },
                        label = { Text("Covenant Confession / Prayer Request (Optional)") },
                        placeholder = { Text("e.g., Believing God for favor and health in 2026") },
                        leadingIcon = {
                            Icon(Icons.Default.EditNote, contentDescription = null, tint = TextSecondary)
                        },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("prayer_notes_input")
                    )

                    // Transparent Fee & Total Calculation Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Base Tithe to Storehouse:", fontSize = 12.sp, color = TextSecondary)
                                Text("GHS ${String.format(Locale.US, "%.2f", baseAmt)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val feeLabel = if (isDirect) "Processing Fee (Cash/Bank Direct):" else "Transparent Processing Fee (${feeConfig.rate}%):"
                                Text(feeLabel, fontSize = 12.sp, color = TextSecondary)
                                Text("GHS ${String.format(Locale.US, "%.2f", feeAmt)}", fontSize = 12.sp, color = if (feeAmt > 0) CaciGoldDark else GreenBlessing)
                            }

                            HorizontalDivider(color = Color(0xFFE5E7EB))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Amount Payable:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CaciRoyalBlue)
                                Text(
                                    "GHS ${String.format(Locale.US, "%.2f", totalAmt)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenBlessing
                                )
                            }

                            Text(
                                text = "Never any hidden fee. 100% of your GHS ${String.format(Locale.US, "%.2f", baseAmt)} base tithe goes directly to the CACI Storehouse.",
                                fontSize = 10.sp,
                                color = TextMuted,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }

                    // Validation Error Text
                    validationError?.let { err ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Submit Tithe Button
                    Button(
                        onClick = {
                            if (memberName.isBlank()) {
                                validationError = "Please enter the member's full name."
                                return@Button
                            }
                            if (memberCity.isBlank()) {
                                validationError = "Please enter the member's city/town."
                                return@Button
                            }
                            if (parsedAmount <= 0) {
                                validationError = "Please enter a valid tithe amount greater than GHS 0."
                                return@Button
                            }

                            val newTx = repository.submitTitheSlip(
                                memberName = memberName.trim(),
                                city = memberCity.trim(),
                                amount = parsedAmount,
                                phone = phoneInput.trim(),
                                titheType = selectedTitheType,
                                paymentMethod = selectedPaymentMethod,
                                covenantNotes = prayerNotes.trim()
                            )

                            submittedReceipt = newTx
                            Toast.makeText(context, "Tithe submitted successfully to Storehouse!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_tithe_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CaciRoyalBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = CaciGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Submit Tithe to Storehouse (GHS ${String.format(Locale.US, "%.2f", totalAmt)})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Section Title: Recent Tithes Submitted by City
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Storehouse Tithe Submissions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = TextPrimary
                )
                TextButton(onClick = onNavigateToTreasury) {
                    Text("Treasury Audit", fontSize = 11.sp, color = CaciRoyalBlue, fontWeight = FontWeight.Bold)
                }
            }
        }

        // List of Recent Tithes
        items(recentTithes.take(8)) { tx ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = CaciRoyalBlue.copy(alpha = 0.1f),
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = CaciRoyalBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = tx.memberName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = CaciGold.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = tx.city,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CaciRoyalBlue,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "${tx.titheType} • ${tx.createdAt}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            if (tx.notes.isNotBlank()) {
                                Text(
                                    text = "\"${tx.notes}\"",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "GHS ${String.format(Locale.US, "%.2f", tx.amount)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GreenBlessing
                        )
                        Surface(
                            color = GreenBlessing.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "STOREHOUSE VERIFIED",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenBlessing,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Official CACI Digital Tithe Receipt / Certificate Dialog
    submittedReceipt?.let { tx ->
        Dialog(onDismissRequest = { submittedReceipt = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .padding(6.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = BorderStroke(2.dp, CaciGold)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ChurchLogoView(
                        branding = branding,
                        modifier = Modifier.size(54.dp),
                        contentDescription = "Dominion Centre Logo"
                    )

                    Text(
                        text = "${branding.churchName}\n${branding.assemblyBranch}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CaciRoyalBlue,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )

                    Text(
                        text = "OFFICIAL STOREHOUSE TITHE CERTIFICATE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = CaciGoldDark,
                        letterSpacing = 0.5.sp
                    )

                    HorizontalDivider(color = CaciBorderGold)

                    // Certificate Body
                    Surface(
                        color = Color(0xFFF9FAFB),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Faithful Partner:", fontSize = 11.sp, color = TextSecondary)
                                Text(tx.memberName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("City / Town:", fontSize = 11.sp, color = TextSecondary)
                                Text(tx.city, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CaciRoyalBlue)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tithe Category:", fontSize = 11.sp, color = TextSecondary)
                                Text(tx.titheType, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tithe Amount:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("GHS ${String.format(Locale.US, "%.2f", tx.amount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GreenBlessing)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Reference #:", fontSize = 10.sp, color = TextSecondary)
                                Text(tx.reference, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Recorded At:", fontSize = 10.sp, color = TextSecondary)
                                Text(tx.createdAt, fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                    }

                    // Covenant Scripture Blessing
                    Surface(
                        color = CaciRoyalBlue.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Pastor's Storehouse Blessing:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = CaciRoyalBlue
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "\"May the Lord open unto you His good treasure, the heaven to give the rain unto your land in his season, and to bless all the work of thine hand.\" — Deuteronomy 28:12",
                                fontSize = 10.sp,
                                fontStyle = FontStyle.Italic,
                                color = TextPrimary,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // Actions: Copy & Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val receiptText = """
                                    *** ${branding.churchName} ***
                                    STOREHOUSE TITHE RECEIPT
                                    Member: ${tx.memberName}
                                    City: ${tx.city}
                                    Category: ${tx.titheType}
                                    Amount: GHS ${String.format(Locale.US, "%.2f", tx.amount)}
                                    Ref: ${tx.reference}
                                    Date: ${tx.createdAt}
                                    Status: STOREHOUSE VERIFIED
                                    "Windows of Heaven are Opened Over You!" (Malachi 3:10)
                                """.trimIndent()
                                clipboard.setPrimaryClip(ClipData.newPlainText("CACI Tithe Receipt", receiptText))
                                Toast.makeText(context, "Tithe receipt copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy Receipt", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { submittedReceipt = null },
                            colors = ButtonDefaults.buttonColors(containerColor = CaciRoyalBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
