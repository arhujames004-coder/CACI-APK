package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BibleVerse
import com.example.data.StorehouseRepository
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CovenantScripturesScreen(
    repository: StorehouseRepository,
    onGiveClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Tithe", "Covenant", "Blessing", "Sowing")

    val filteredVerses = if (selectedCategory == "All") {
        repository.bibleVerses
    } else {
        repository.bibleVerses.filter { it.category.equals(selectedCategory, ignoreCase = true) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ChurchBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Scripture Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CaciRoyalBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                text = "WORD OF FAITH",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = CaciGold, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tithe & Covenant Scriptures",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Scriptural foundations of God's holy storehouse, biblical prosperity, and covenant partnership at Dominion Centre.",
                        fontSize = 12.sp,
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Covenant Confession of Faith
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = BorderStroke(1.dp, CaciGoldDark.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(CaciGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = CaciGoldDark, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Apostolic Covenant Declaration",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = CaciRoyalBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "\"I declare that as a covenant seed in Christ Apostolic Church International, I walk in supernatural favor. As I bring my tithe into God's storehouse, the windows of heaven are opened over my household, and the devourer is rebuked for my sake. I am blessed to be a blessing!\"",
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = TextPrimary,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onGiveClicked,
                        colors = ButtonDefaults.buttonColors(containerColor = CaciRoyalBlue),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Act on the Word: Sown in Faith", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CaciRoyalBlue,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("filter_verse_$cat")
                    )
                }
            }
        }

        // Verses List
        items(filteredVerses) { verse ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("verse_card_${verse.id}"),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = when (verse.category) {
                                "Tithe" -> CaciRoyalBlue.copy(alpha = 0.1f)
                                "Covenant" -> CaciGold.copy(alpha = 0.2f)
                                else -> GreenBlessingLight
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = verse.theme.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (verse.category) {
                                    "Tithe" -> CaciRoyalBlue
                                    "Covenant" -> CaciGoldDark
                                    else -> GreenBlessing
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val clip = ClipData.newPlainText(verse.reference, "${verse.text} - ${verse.reference}")
                                clipboardManager.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied ${verse.reference}!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Scripture", tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "\"${verse.text}\"",
                        fontSize = 13.sp,
                        color = TextPrimary,
                        lineHeight = 19.sp,
                        fontStyle = FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = verse.reference,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CaciGoldDark
                        )

                        TextButton(
                            onClick = onGiveClicked,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Honor God with Tithe", fontSize = 11.sp, color = CaciRoyalBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
