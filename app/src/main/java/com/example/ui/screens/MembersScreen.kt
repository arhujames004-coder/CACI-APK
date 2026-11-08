package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ChurchMember
import com.example.data.StorehouseRepository
import com.example.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    repository: StorehouseRepository,
    modifier: Modifier = Modifier
) {
    val members by repository.members.collectAsState()
    val transactions by repository.transactions.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedMemberForStatement by remember { mutableStateOf<ChurchMember?>(null) }

    val filteredMembers = members.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery) ||
                it.communityGroup.contains(searchQuery, ignoreCase = true) ||
                it.city.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ChurchBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = ChurchNavyPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_member_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Member")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Church Member Directory",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = ChurchNavyPrimary
            )
            Text(
                text = "${members.size} registered donors across fellowship ministries",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_members_field"),
                placeholder = { Text("Search name, phone, or ministry...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ChurchSurface,
                    unfocusedContainerColor = ChurchSurface
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Member list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredMembers) { member ->
                    val memberTx = transactions.filter { it.phone == member.phone && it.status == "success" }
                    val totalGiven = memberTx.sumOf { it.amount }
                    val titheTotal = memberTx.filter { it.fundCode == "tithe" }.sumOf { it.amount }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMemberForStatement = member }
                            .testTag("member_card_${member.id}"),
                        colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(ChurchNavyPrimary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = member.name.take(1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = ChurchNavyPrimary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = member.phone,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Row(
                                    modifier = Modifier.padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = ChurchBackground,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = member.communityGroup,
                                            fontSize = 10.sp,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    Surface(
                                        color = CaciGold.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = member.city,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CaciRoyalBlue,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${repository.currency} ${String.format(Locale.US, "%.2f", totalGiven)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = GreenSuccess
                                )
                                Text(
                                    text = "Tithe: GHS ${String.format(Locale.US, "%.0f", titheTotal)}",
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "Tap statement",
                                    fontSize = 9.sp,
                                    color = ChurchNavyPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // MEMBER GIVING STATEMENT DIALOG
    // =========================================================================
    selectedMemberForStatement?.let { member ->
        val memberTx = transactions.filter { it.phone == member.phone && it.status == "success" }
        val titheTotal = memberTx.filter { it.fundCode == "tithe" }.sumOf { it.amount }
        val offeringTotal = memberTx.filter { it.fundCode == "offering" }.sumOf { it.amount }
        val specialTotal = memberTx.filter { it.fundCode !in listOf("tithe", "offering") }.sumOf { it.amount }
        val totalAll = memberTx.sumOf { it.amount }

        Dialog(onDismissRequest = { selectedMemberForStatement = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .padding(6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Official Giving Statement",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChurchGoldDark
                    )
                    Text(
                        text = member.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${member.communityGroup} • ${member.phone}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ChurchBackground)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Tithe Total:", fontSize = 12.sp, color = TextSecondary)
                                Text("${repository.currency} ${String.format(Locale.US, "%.2f", titheTotal)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Sunday Offering:", fontSize = 12.sp, color = TextSecondary)
                                Text("${repository.currency} ${String.format(Locale.US, "%.2f", offeringTotal)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Building / Special:", fontSize = 12.sp, color = TextSecondary)
                                Text("${repository.currency} ${String.format(Locale.US, "%.2f", specialTotal)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Given:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ChurchNavyPrimary)
                                Text("${repository.currency} ${String.format(Locale.US, "%.2f", totalAll)}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GreenSuccess)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Recent Ledger Records:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    memberTx.take(4).forEach { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${tx.fundCode.replaceFirstChar { it.uppercase() }} (${tx.createdAt.take(10)})",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "${repository.currency} ${String.format(Locale.US, "%.2f", tx.amount)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { selectedMemberForStatement = null },
                            colors = ButtonDefaults.buttonColors(containerColor = ChurchNavyPrimary)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // ADD MEMBER DIALOG
    // =========================================================================
    if (showAddDialog) {
        var nameInput by remember { mutableStateOf("") }
        var phoneInput by remember { mutableStateOf("") }
        var groupInput by remember { mutableStateOf("General Congregation") }
        var cityInput by remember { mutableStateOf("Accra") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .padding(6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Register New Member",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ChurchNavyPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("e.g. Deaconess Mary") },
                        modifier = Modifier.fillMaxWidth().testTag("add_member_name"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = cityInput,
                        onValueChange = { cityInput = it },
                        label = { Text("City / Town / Location") },
                        placeholder = { Text("e.g. Accra, Kumasi, London") },
                        modifier = Modifier.fillMaxWidth().testTag("add_member_city"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+233240000000 or 0240000000") },
                        modifier = Modifier.fillMaxWidth().testTag("add_member_phone"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = groupInput,
                        onValueChange = { groupInput = it },
                        label = { Text("Community / Ministry Group") },
                        placeholder = { Text("e.g. Men's Movement, Women's Movement, Youth Fellowship") },
                        modifier = Modifier.fillMaxWidth().testTag("add_member_group"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (phoneInput.isNotBlank()) {
                                    val formattedPhone = if (phoneInput.startsWith("0")) "+233" + phoneInput.drop(1) else phoneInput
                                    repository.addMember(nameInput.ifBlank { "Member" }, formattedPhone, groupInput, cityInput.ifBlank { "Accra" })
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ChurchNavyPrimary)
                        ) {
                            Text("Save Member")
                        }
                    }
                }
            }
        }
    }
}
