package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.example.ui.components.ChurchLogoView
import com.example.R
import com.example.data.StorehouseRepository
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: StorehouseRepository,
    onNavigateToUssd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transactions by repository.transactions.collectAsState()
    val members by repository.members.collectAsState()
    val feeConfig by repository.feeConfig.collectAsState()

    var selectedPeriod by remember { mutableStateOf("All Time") }
    var txSearchQuery by remember { mutableStateOf("") }

    val successfulTx = transactions.filter { it.status == "success" }
    val netReceived = successfulTx.sumOf { it.amount }
    val feesCollected = successfulTx.sumOf { it.fee }
    val grossDonated = successfulTx.sumOf { it.totalAmount }

    val filteredTx = transactions.filter {
        it.reference.contains(txSearchQuery, ignoreCase = true) ||
                it.memberName.contains(txSearchQuery, ignoreCase = true) ||
                it.phone.contains(txSearchQuery) ||
                it.fundCode.contains(txSearchQuery, ignoreCase = true) ||
                it.city.contains(txSearchQuery, ignoreCase = true)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ChurchBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Church Header
        item {
            val branding by repository.branding.collectAsState()
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChurchLogoView(
                    branding = branding,
                    modifier = Modifier.size(44.dp),
                    contentDescription = "Dominion Centre Logo"
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = repository.churchName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = ChurchNavyPrimary
                    )
                    Text(
                        text = "${branding.assemblyBranch} • Treasury",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Period Filter Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All Time", "This Month", "This Week", "Today").forEach { period ->
                    FilterChip(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        label = { Text(period, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ChurchNavyPrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_$period")
                    )
                }
            }
        }

        // 4 Core Financial Metrics
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Net to Church",
                        value = "${repository.currency} ${String.format(Locale.US, "%.2f", netReceived)}",
                        subtitle = "Available ministry funds",
                        accentColor = GreenSuccess,
                        icon = Icons.Default.Savings,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Processing Fees",
                        value = "${repository.currency} ${String.format(Locale.US, "%.2f", feesCollected)}",
                        subtitle = "${feeConfig.rate}% donor-covered",
                        accentColor = OrangeWarning,
                        icon = Icons.Default.Percent,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Total Gross Donated",
                        value = "${repository.currency} ${String.format(Locale.US, "%.2f", grossDonated)}",
                        subtitle = "${successfulTx.size} gifts received",
                        accentColor = ChurchNavyPrimary,
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Active Donors",
                        value = "${members.size}",
                        subtitle = "Registered in directory",
                        accentColor = ChurchGoldDark,
                        icon = Icons.Default.PeopleAlt,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Totals by Fund
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Giving by Church Fund",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Text(text = "Net Amounts", fontSize = 11.sp, color = TextMuted)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    repository.funds.forEach { fund ->
                        val fundTx = successfulTx.filter { it.fundCode == fund.code }
                        val fundTotal = fundTx.sumOf { it.amount }
                        val progress = if (netReceived > 0) (fundTotal / netReceived).toFloat() else 0f

                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${fund.name} (${fundTx.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${repository.currency} ${String.format(Locale.US, "%.2f", fundTotal)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ChurchNavyPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = when (fund.code) {
                                    "tithe" -> ChurchNavyPrimary
                                    "offering" -> GreenSuccess
                                    "building" -> OrangeWarning
                                    else -> ChurchGoldDark
                                },
                                trackColor = ChurchCardBorder
                            )
                        }
                    }
                }
            }
        }

        // Giving by Fellowship / Community Group
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Giving by Fellowship / Ministry Group",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val groups = members.map { it.communityGroup }.distinct()
                    groups.forEach { groupName ->
                        val groupMembers = members.filter { it.communityGroup == groupName }
                        val groupPhones = groupMembers.map { it.phone }
                        val groupTotal = successfulTx.filter { it.phone in groupPhones }.sumOf { it.amount }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ChurchGold)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = groupName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(text = "${groupMembers.size} members", fontSize = 10.sp, color = TextMuted)
                                }
                            }
                            Text(
                                text = "${repository.currency} ${String.format(Locale.US, "%.2f", groupTotal)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenSuccess
                            )
                        }
                        HorizontalDivider(color = ChurchCardBorder.copy(alpha = 0.5f))
                    }
                }
            }
        }

        // Recent Donations Section Header & Search
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions (${filteredTx.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = txSearchQuery,
                    onValueChange = { txSearchQuery = it },
                    placeholder = { Text("Find by reference, donor or fund...", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Find", modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (txSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { txSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("dashboard_tx_search_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ChurchSurface,
                        unfocusedContainerColor = ChurchSurface
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Transaction list items
        items(filteredTx) { tx ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tx_card_${tx.reference}"),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                text = "${tx.reference} • ${tx.titheType}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${tx.currency} ${String.format(Locale.US, "%.2f", tx.amount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (tx.status == "success") GreenSuccess else TextPrimary
                            )
                            Text(
                                text = "+${tx.currency} ${String.format(Locale.US, "%.2f", tx.fee)} fee",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {},
                            label = { Text(tx.fundCode.replaceFirstChar { it.uppercase() }, fontSize = 10.sp) },
                            modifier = Modifier.height(24.dp)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (tx.status == "success") {
                                Surface(
                                    color = GreenSuccessLight,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "PAID (MoMo)",
                                        color = GreenSuccess,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    color = OrangeWarningLight,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "PENDING",
                                        color = OrangeWarning,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            if (tx.smsReceiptSent) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    Icons.Default.MarkChatRead,
                                    contentDescription = "SMS Sent",
                                    tint = GreenSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = ChurchSurface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = subtitle, fontSize = 10.sp, color = TextMuted)
        }
    }
}
