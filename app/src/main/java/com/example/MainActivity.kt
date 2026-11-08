package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StorehouseRepository
import com.example.ui.components.ChurchLogoView
import com.example.ui.components.AboutHowItWorksDialog
import com.example.ui.screens.ChurchSettingsScreen
import com.example.ui.screens.CovenantScripturesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.StorehouseGivingScreen
import com.example.ui.screens.SubmitTitheScreen
import com.example.ui.theme.CaciGold
import com.example.ui.theme.CaciRoyalBlue
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val repository = StorehouseRepository()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                var currentTab by remember { mutableIntStateOf(0) }
                var showSettingsScreen by remember { mutableStateOf(false) }
                var showAboutDialog by remember { mutableStateOf(false) }

                val branding by repository.branding.collectAsState()
                val transactions by repository.transactions.collectAsState()
                val totalSum = transactions.filter { it.status == "success" }.sumOf { it.amount }

                BackHandler(enabled = showSettingsScreen || currentTab != 0) {
                    if (showSettingsScreen) {
                        showSettingsScreen = false
                    } else {
                        currentTab = 0
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ChurchLogoView(
                                        branding = branding,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clickable { showSettingsScreen = !showSettingsScreen },
                                        contentDescription = "Dominion Centre Logo"
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = "CACI Storehouse",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = branding.assemblyBranch,
                                            fontSize = 10.sp,
                                            color = CaciGold
                                        )
                                    }
                                }
                            },
                            actions = {
                                Surface(
                                    color = Color(0xFF10B981).copy(alpha = 0.25f),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Savings,
                                            contentDescription = "Total Fund Sum",
                                            tint = CaciGold,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "GHS ${String.format(Locale.US, "%.0f", totalSum)}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { showAboutDialog = true },
                                    modifier = Modifier.size(36.dp).testTag("top_about_guide_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = "About App & How It Works",
                                        tint = Color.White
                                    )
                                }

                                IconButton(
                                    onClick = { showSettingsScreen = !showSettingsScreen },
                                    modifier = Modifier.size(36.dp).testTag("top_settings_button")
                                ) {
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = "Church Logo & Settings",
                                        tint = Color.White
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = CaciRoyalBlue,
                                titleContentColor = Color.White,
                                actionIconContentColor = Color.White
                            ),
                            modifier = Modifier.testTag("app_top_bar")
                        )
                    },
                    bottomBar = {
                        if (!showSettingsScreen) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp,
                                modifier = Modifier.testTag("app_bottom_nav")
                            ) {
                                NavigationBarItem(
                                    selected = currentTab == 0,
                                    onClick = { currentTab = 0 },
                                    icon = { Icon(Icons.Default.VolunteerActivism, contentDescription = "Storehouse") },
                                    label = { Text("Storehouse", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                    modifier = Modifier.testTag("nav_item_storehouse")
                                )
                                NavigationBarItem(
                                    selected = currentTab == 1,
                                    onClick = { currentTab = 1 },
                                    icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Tithe") },
                                    label = { Text("Tithe", fontSize = 10.sp, maxLines = 1, fontWeight = FontWeight.SemiBold) },
                                    alwaysShowLabel = true,
                                    modifier = Modifier.testTag("nav_item_submit_tithe")
                                )
                                NavigationBarItem(
                                    selected = currentTab == 2,
                                    onClick = { currentTab = 2 },
                                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Scriptures") },
                                    label = { Text("Scriptures", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                    modifier = Modifier.testTag("nav_item_scriptures")
                                )
                                NavigationBarItem(
                                    selected = currentTab == 3,
                                    onClick = { currentTab = 3 },
                                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Treasury") },
                                    label = { Text("Treasury", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                    modifier = Modifier.testTag("nav_item_treasury")
                                )
                                NavigationBarItem(
                                    selected = currentTab == 4,
                                    onClick = { currentTab = 4 },
                                    icon = { Icon(Icons.Default.People, contentDescription = "Members") },
                                    label = { Text("Members", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                    modifier = Modifier.testTag("nav_item_members")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 680.dp)
                        ) {
                            if (showSettingsScreen) {
                                ChurchSettingsScreen(
                                    repository = repository,
                                    onBack = { showSettingsScreen = false },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                when (currentTab) {
                                    0 -> StorehouseGivingScreen(
                                        repository = repository,
                                        onOpenSettings = { showSettingsScreen = true },
                                        onNavigateToSubmitTithe = { currentTab = 1 },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    1 -> SubmitTitheScreen(
                                        repository = repository,
                                        onNavigateToTreasury = { currentTab = 3 },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    2 -> CovenantScripturesScreen(
                                        repository = repository,
                                        onGiveClicked = { currentTab = 1 },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    3 -> DashboardScreen(
                                        repository = repository,
                                        onNavigateToUssd = { currentTab = 0 },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    4 -> MembersScreen(
                                        repository = repository,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            if (showAboutDialog) {
                                AboutHowItWorksDialog(
                                    branding = branding,
                                    onDismiss = { showAboutDialog = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
