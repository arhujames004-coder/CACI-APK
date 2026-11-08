package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StorehouseRepository
import com.example.ui.theme.*

@Composable
fun TermuxGuideScreen(
    repository: StorehouseRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val auditLogs by repository.auditLogs.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Termux Commands, 1: Tunnel & Webhooks, 2: Audit Trail

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChurchBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "Termux & Server Hub",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = ChurchNavyPrimary
        )
        Text(
            text = "Run Tithe Storehouse in Termux on Android or deploy to a VPS",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Subtabs
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = ChurchSurface,
            contentColor = ChurchNavyPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("Termux Commands", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_termux_commands")
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("Tunnel & URLs", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_tunnel_urls")
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = { Text("Audit Trail (${auditLogs.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_audit_trail")
            )
        }

        when (activeTab) {
            0 -> TermuxCommandsView(clipboardManager, context)
            1 -> TunnelAndWebhooksView(repository, clipboardManager, context)
            2 -> AuditLogsView(auditLogs)
        }
    }
}

@Composable
fun TermuxCommandsView(clipboardManager: ClipboardManager, context: Context) {
    val copyToClipboard: (String, String) -> Unit = { label, text ->
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "1. Install Dependencies in Termux",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ChurchNavyPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CodeSnippetBox(
                        code = "pkg update && pkg install -y python python-pip sqlite git openssl clang libffi",
                        onCopy = { copyToClipboard("Step 1", it) }
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "2. Quick Setup & Run (One-Command)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ChurchNavyPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CodeSnippetBox(
                        code = "bash tithe_storehouse/termux_setup.sh",
                        onCopy = { copyToClipboard("Step 2", it) }
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "3. Manual Start Command",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ChurchNavyPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CodeSnippetBox(
                        code = "source venv/bin/activate && python3 tithe_storehouse/run.py",
                        onCopy = { copyToClipboard("Step 3", it) }
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "4. Single-File SQLite Database Backup",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ChurchNavyPrimary
                    )
                    Text(
                        text = "The entire church ledger is stored in a single SQLite file with append-only audit triggers:",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    CodeSnippetBox(
                        code = "cp tithe_storehouse.db backup_$(date +%F).db",
                        onCopy = { copyToClipboard("Step 4", it) }
                    )
                }
            }
        }
    }
}

@Composable
fun TunnelAndWebhooksView(repository: StorehouseRepository, clipboardManager: ClipboardManager, context: Context) {
    val copyToClipboard: (String, String) -> Unit = { label, text ->
        val clip = ClipData.newPlainText(label, text)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(context, "Copied URL!", Toast.LENGTH_SHORT).show()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Cloudflared Tunnel (Free, No Signup)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ChurchNavyPrimary
                    )
                    Text(
                        text = "Run this in a second Termux tab to get a public HTTPS address for Africa's Talking & Paystack:",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    CodeSnippetBox(
                        code = "pkg install cloudflared -y\ncloudflared tunnel --url http://localhost:5000",
                        onCopy = { copyToClipboard("Cloudflared", it) }
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Africa's Talking USSD Callback URL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ChurchNavyPrimary
                    )
                    Text(
                        text = "Register in Africa's Talking -> USSD -> Service Codes -> Callback:",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    CodeSnippetBox(
                        code = "POST https://your-tunnel.trycloudflare.com/ussd",
                        onCopy = { copyToClipboard("USSD URL", it) }
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Paystack Webhook URL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ChurchNavyPrimary
                    )
                    Text(
                        text = "Configure in Paystack Dashboard -> Settings -> Preferences -> Webhook:",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    CodeSnippetBox(
                        code = "POST https://your-tunnel.trycloudflare.com/webhook/paystack",
                        onCopy = { copyToClipboard("Webhook URL", it) }
                    )
                }
            }
        }
    }
}

@Composable
fun AuditLogsView(logs: List<com.example.data.AuditEntry>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GreenSuccessLight),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = GreenSuccess)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Append-only database triggers active. Audit records cannot be modified or deleted.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenSuccess
                    )
                }
            }
        }

        items(logs) { entry ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = when (entry.actor) {
                                "ussd" -> ChurchNavyPrimary.copy(alpha = 0.15f)
                                "paystack_webhook" -> GreenSuccessLight
                                else -> OrangeWarningLight
                            },
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = entry.actor.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(text = entry.timestamp, fontSize = 10.sp, color = TextMuted)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = entry.action,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = entry.details,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun CodeSnippetBox(code: String, onCopy: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = code,
            color = Color(0xFF38BDF8),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = { onCopy(code) },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}
