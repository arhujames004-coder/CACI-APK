package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ChurchBranding
import com.example.ui.theme.*

/**
 * Explanatory dialog introducing what the Tithe Storehouse app is about,
 * its biblical mission, and a step-by-step guide on how it works.
 */
@Composable
fun AboutHowItWorksDialog(
    branding: ChurchBranding,
    onDismiss: () -> Unit
) {
    var selectedSection by remember { mutableIntStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .testTag("about_how_it_works_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ChurchSurface),
            border = BorderStroke(1.5.dp, CaciGold)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // Header with Church Logo & Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        ChurchLogoView(
                            branding = branding,
                            modifier = Modifier.size(42.dp),
                            contentDescription = "CACI Crest"
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "About Tithe Storehouse",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = CaciRoyalBlue
                            )
                            Text(
                                text = "${branding.assemblyBranch} • User Guide",
                                fontSize = 11.sp,
                                color = CaciGoldDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("close_about_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Pills / Category Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedSection == 0,
                        onClick = { selectedSection = 0 },
                        label = { Text("What It Is", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CaciRoyalBlue,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedSection == 1,
                        onClick = { selectedSection = 1 },
                        label = { Text("How It Works", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CaciRoyalBlue,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedSection == 2,
                        onClick = { selectedSection = 2 },
                        label = { Text("Giving Ways", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CaciRoyalBlue,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedSection) {
                    0 -> AboutOverviewSection(branding)
                    1 -> HowItWorksStepByStepSection()
                    2 -> GivingChannelsAndTransparencySection(branding)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dismiss / Action Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_dialog_got_it_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CaciRoyalBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Got It • Return to Storehouse", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AboutOverviewSection(branding: ChurchBranding) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Mission Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CaciRoyalBlue.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, CaciRoyalBlue.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Church, contentDescription = null, tint = CaciRoyalBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "The Storehouse Mission",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = CaciRoyalBlue
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tithe Storehouse is the official digital stewardship platform of Christ Apostolic Church International (${branding.assemblyBranch}). It connects church members directly with the altar of giving through a simple, phone-first, and tamper-proof financial ecosystem.",
                    fontSize = 12.sp,
                    color = TextPrimary,
                    lineHeight = 17.sp
                )
            }
        }

        // Why Was It Built?
        Text(
            text = "Why Was This App Built?",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = CaciRoyalBlue
        )

        KeyPillarItem(
            icon = Icons.Default.PhoneAndroid,
            iconTint = CaciRoyalBlue,
            title = "1. Phone-First Accessibility for All",
            description = "Designed for both smartphone owners (Jetpack Compose app) and members using basic feature phones via USSD (*714*123#) without requiring active internet data."
        )

        KeyPillarItem(
            icon = Icons.Default.Visibility,
            iconTint = GreenBlessing,
            title = "2. Absolute Financial Transparency",
            description = "Never any hidden charges. Processing fees (Mobile Money / Paystack) are computed and explicitly shown before payment, and net giving vs. fees are tracked separately in the church ledger."
        )

        KeyPillarItem(
            icon = Icons.Default.VerifiedUser,
            iconTint = CaciGoldDark,
            title = "3. Complete Auditability & Security",
            description = "Every tithe and offering is recorded in an immutable audit log. Pastors and treasurers can export reports, monitor targets, and ensure every coin serves God's kingdom."
        )

        KeyPillarItem(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            iconTint = CaciRoyalBlue,
            title = "4. Covenant-Centered Giving",
            description = "Built around biblical scriptures (Malachi 3:10, 2 Corinthians 9:7). Includes instant scripture blessings upon giving and digital Storehouse Tithe Certificates."
        )
    }
}

@Composable
private fun HowItWorksStepByStepSection() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Step-by-Step Giving Guide",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = CaciRoyalBlue
        )
        Text(
            text = "Follow these 4 simple steps to give tithes and offerings through the app or USSD:",
            fontSize = 11.sp,
            color = TextSecondary
        )

        StepItem(
            stepNumber = "1",
            title = "Choose Fund or Enter Tithe Slip",
            description = "Tap 'Storehouse' to select from Tithes, Sunday Offerings, Dominion Building Project, or Welfare. Or tap 'Tithe' to enter your Member Name, City, and compute your exact 10% with the built-in Tithe Calculator."
        )

        StepItem(
            stepNumber = "2",
            title = "Review Fee & Select Payment Channel",
            description = "Enter your desired amount. The system automatically computes and displays the network processing fee (e.g., 1.5%). Choose between instant Mobile Money / Card (Paystack) or USSD."
        )

        StepItem(
            stepNumber = "3",
            title = "Confirm Payment on Your Phone",
            description = "For Mobile Money, approve the prompt on your phone (MTN MoMo, Telecel Cash, AirtelTigo). For USSD, dial *714*123# and follow the menu prompts."
        )

        StepItem(
            stepNumber = "4",
            title = "Instant SMS Receipt & Digital Certificate",
            description = "Receive an automated SMS receipt with God's blessings. Generate and view your verifiable Storehouse Tithe Certificate. All contributions are credited in real time to the treasury."
        )
    }
}

@Composable
private fun GivingChannelsAndTransparencySection(branding: ChurchBranding) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Supported Giving Channels",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = CaciRoyalBlue
        )

        // Mobile Money Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ChurchSurface),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payment, contentDescription = null, tint = GreenBlessing, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mobile Money & Visa/MasterCard", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Powered by Paystack. Supports MTN Mobile Money, Telecel Cash, AirtelTigo Money, and local/international debit cards. Fast, encrypted, and PCI-DSS compliant.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }
        }

        // USSD Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ChurchSurface),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dialpad, contentDescription = null, tint = CaciRoyalBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Offline USSD Menu (*714*123#)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Powered by Africa's Talking. Allows members without smartphones or data to dial the church shortcode directly, check giving history, and give tithes via mobile money prompts.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }
        }

        // Pastoral & Treasury Contacts
        Surface(
            color = CaciGold.copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, CaciBorderGold)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "Have questions or need assistance?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = CaciRoyalBlue
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Call Dominion Centre helpline: ${branding.helpline} or speak with Resident Pastor ${branding.residentPastor} at ${branding.churchAddress}.",
                    fontSize = 10.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun KeyPillarItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = iconTint.copy(alpha = 0.12f),
            shape = CircleShape,
            modifier = Modifier.size(30.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun StepItem(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = CaciRoyalBlue,
            shape = CircleShape,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stepNumber,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = description, fontSize = 11.sp, color = TextSecondary, lineHeight = 15.sp)
        }
    }
}
