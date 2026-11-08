package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChurchBranding
import com.example.ui.theme.*

/**
 * An inline card displayed in StorehouseGivingScreen that introduces the app,
 * explains what it is about, and provides quick guidance with a button to open
 * the complete interactive User Guide.
 */
@Composable
fun AboutHowItWorksCard(
    branding: ChurchBranding,
    onOpenFullGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("about_app_summary_card"),
        colors = CardDefaults.cardColors(containerColor = ChurchSurface),
        border = BorderStroke(1.2.dp, CaciBorderGold),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row with expandable toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = CaciRoyalBlue.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Info,
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
                                text = "About This App & How It Works",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = CaciRoyalBlue
                            )
                        }
                        Text(
                            text = "Dominion Centre Digital Stewardship Guide",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp).testTag("toggle_about_card_expansion")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = CaciRoyalBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Short summary always visible
            Text(
                text = "Tithe Storehouse is CACI Dominion Centre's official platform for paying tithes and offerings safely via Mobile Money, Visa/MasterCard, or USSD (*714*123#) with zero hidden fees and instant SMS receipts.",
                fontSize = 12.sp,
                color = TextPrimary,
                lineHeight = 17.sp
            )

            // Animated expansion with detailed pillars
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)

                    QuickGuideItem(
                        icon = Icons.Default.Calculate,
                        title = "1. Calculate & Fill Slip",
                        description = "Use the built-in 10% Tithe Calculator or choose from church project funds."
                    )

                    QuickGuideItem(
                        icon = Icons.Default.PhoneAndroid,
                        title = "2. Pay Online or Dial USSD",
                        description = "Pay via Paystack (MoMo/Card) or dial shortcode *714*123# on any feature phone."
                    )

                    QuickGuideItem(
                        icon = Icons.Default.MarkEmailRead,
                        title = "3. Automated SMS Receipts",
                        description = "Receive instant scripture-blessed SMS receipts and digital covenant certificates."
                    )

                    QuickGuideItem(
                        icon = Icons.Default.AccountBalance,
                        title = "4. Verified Treasury Audit",
                        description = "100% transparent: every gift is audited in real-time by church leadership."
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row to launch comprehensive guide dialog
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Show Less" else "Quick Highlights",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = onOpenFullGuide,
                    colors = ButtonDefaults.buttonColors(containerColor = CaciRoyalBlue),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("open_full_user_guide_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Read Full Guide",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickGuideItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = CaciRoyalBlue,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
            Text(text = description, fontSize = 10.sp, color = TextSecondary, lineHeight = 14.sp)
        }
    }
}
