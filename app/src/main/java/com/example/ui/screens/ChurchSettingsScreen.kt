package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.StorehouseRepository
import com.example.ui.components.ChurchLogoView
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChurchSettingsScreen(
    repository: StorehouseRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val branding by repository.branding.collectAsState()
    val feeConfig by repository.feeConfig.collectAsState()

    var churchNameInput by remember(branding) { mutableStateOf(branding.churchName) }
    var assemblyBranchInput by remember(branding) { mutableStateOf(branding.assemblyBranch) }
    var selectedLogoChoice by remember(branding) { mutableStateOf(branding.logoResourceName) }
    var customLogoUrlInput by remember(branding) { mutableStateOf(branding.customLogoUrl) }
    var residentPastorInput by remember(branding) { mutableStateOf(branding.residentPastor) }
    var serviceTimesInput by remember(branding) { mutableStateOf(branding.serviceTimes) }
    var helplineInput by remember(branding) { mutableStateOf(branding.helpline) }
    var churchAddressInput by remember(branding) { mutableStateOf(branding.churchAddress) }

    // Zero-permission modern Android Photo Picker
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val destinationFile = File(context.filesDir, "church_custom_logo.png")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }
                repository.updateGalleryLogo(destinationFile.absolutePath)
                Toast.makeText(context, "Custom gallery logo applied to Dominion Centre!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ChurchBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Navigation & Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Dominion Centre Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = CaciRoyalBlue
                    )
                    Text(
                        text = "Customize church logo, gallery image, assembly info, and policies",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Section 1: Gallery Logo & Church Emblem Customization (Requested Feature)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "1. Church Logo & Gallery Photo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = CaciRoyalBlue
                        )
                        if (branding.customGalleryLogoUri.isNotBlank()) {
                            Surface(
                                color = GreenBlessing.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "GALLERY LOGO ACTIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenBlessing,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Change the logo using your phone's photo gallery or choose from official church crests.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // Current Active Logo Preview
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ChurchBackground, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChurchLogoView(
                            branding = branding,
                            modifier = Modifier.size(60.dp),
                            contentDescription = "Active Logo Preview"
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Logo Displayed in App",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = if (branding.customGalleryLogoUri.isNotBlank()) {
                                    "Custom Photo from Device Gallery"
                                } else if (selectedLogoChoice == "ic_tithe_storehouse_1793809781217") {
                                    "Tithe Storehouse Crest"
                                } else {
                                    "Official CACI Golden Crest"
                                },
                                fontSize = 11.sp,
                                color = CaciGoldDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Gallery Picker Button (Requested Feature)
                    Button(
                        onClick = {
                            galleryPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pick_gallery_logo_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CaciRoyalBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = CaciGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Choose Logo from Phone Gallery",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }

                    if (branding.customGalleryLogoUri.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                repository.resetToDefaultLogo()
                                Toast.makeText(context, "Reset to official CACI crest", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("reset_logo_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset to Official CACI Crest", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "Or Select Built-in Church Emblems:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Logo Choice 1: Official CACI Logo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                if (branding.customGalleryLogoUri.isBlank() && selectedLogoChoice == "ic_caci_logo_1793811954335") CaciRoyalBlue else ChurchCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .background(
                                if (branding.customGalleryLogoUri.isBlank() && selectedLogoChoice == "ic_caci_logo_1793811954335") CaciRoyalBlue.copy(alpha = 0.05f) else ChurchSurface
                            )
                            .clickable {
                                repository.resetToDefaultLogo()
                                selectedLogoChoice = "ic_caci_logo_1793811954335"
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = branding.customGalleryLogoUri.isBlank() && selectedLogoChoice == "ic_caci_logo_1793811954335",
                            onClick = {
                                repository.resetToDefaultLogo()
                                selectedLogoChoice = "ic_caci_logo_1793811954335"
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Image(
                            painter = painterResource(id = R.drawable.ic_caci_logo_1793811954335),
                            contentDescription = "CACI Logo",
                            modifier = Modifier.size(36.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Official CACI Global Crest", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Shield of faith, Holy Bible, golden cross, and peace dove", fontSize = 10.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Logo Choice 2: Storehouse Crest
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                if (branding.customGalleryLogoUri.isBlank() && selectedLogoChoice == "ic_tithe_storehouse_1793809781217") CaciRoyalBlue else ChurchCardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .background(
                                if (branding.customGalleryLogoUri.isBlank() && selectedLogoChoice == "ic_tithe_storehouse_1793809781217") CaciRoyalBlue.copy(alpha = 0.05f) else ChurchSurface
                            )
                            .clickable {
                                repository.resetToDefaultLogo()
                                selectedLogoChoice = "ic_tithe_storehouse_1793809781217"
                                repository.updateBranding(
                                    churchName = churchNameInput,
                                    assemblyBranch = assemblyBranchInput,
                                    logoResourceName = "ic_tithe_storehouse_1793809781217",
                                    customLogoUrl = customLogoUrlInput
                                )
                            }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = branding.customGalleryLogoUri.isBlank() && selectedLogoChoice == "ic_tithe_storehouse_1793809781217",
                            onClick = {
                                repository.resetToDefaultLogo()
                                selectedLogoChoice = "ic_tithe_storehouse_1793809781217"
                                repository.updateBranding(
                                    churchName = churchNameInput,
                                    assemblyBranch = assemblyBranchInput,
                                    logoResourceName = "ic_tithe_storehouse_1793809781217",
                                    customLogoUrl = customLogoUrlInput
                                )
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Image(
                            painter = painterResource(id = R.drawable.ic_tithe_storehouse_1793809781217),
                            contentDescription = "Storehouse Crest",
                            modifier = Modifier.size(36.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Storehouse Cathedral Emblem", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Golden wheat sheaves & Malachi 3:10 storehouse", fontSize = 10.sp, color = TextSecondary)
                        }
                    }
                }
            }
        }

        // Section 2: Assembly & Branch Details (Dominion Centre)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "2. Assembly & Pastoral Information",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CaciRoyalBlue
                    )

                    OutlinedTextField(
                        value = assemblyBranchInput,
                        onValueChange = { assemblyBranchInput = it },
                        label = { Text("Assembly / Branch Name *") },
                        placeholder = { Text("Dominion Centre") },
                        leadingIcon = { Icon(Icons.Default.Church, contentDescription = null, tint = CaciRoyalBlue) },
                        modifier = Modifier.fillMaxWidth().testTag("assembly_branch_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = churchNameInput,
                        onValueChange = { churchNameInput = it },
                        label = { Text("Church Denomination") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("church_name_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = residentPastorInput,
                        onValueChange = { residentPastorInput = it },
                        label = { Text("Resident Pastor / Minister") },
                        placeholder = { Text("Rev. Dr. Emmanuel Owusu") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("resident_pastor_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = serviceTimesInput,
                        onValueChange = { serviceTimesInput = it },
                        label = { Text("Dominion Centre Service Times") },
                        placeholder = { Text("Sundays: 8:30 AM & 10:30 AM | Wed: 6:00 PM") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("service_times_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = helplineInput,
                        onValueChange = { helplineInput = it },
                        label = { Text("Church Contact / Helpline") },
                        placeholder = { Text("+233 24 111 2233") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("helpline_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = churchAddressInput,
                        onValueChange = { churchAddressInput = it },
                        label = { Text("Sanctuary Location / Address") },
                        placeholder = { Text("Dominion Sanctuary, Off High Street") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("address_field"),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            repository.updateBranding(
                                churchName = churchNameInput.ifBlank { "Christ Apostolic Church International" },
                                assemblyBranch = assemblyBranchInput.ifBlank { "Dominion Centre" },
                                logoResourceName = selectedLogoChoice,
                                customLogoUrl = customLogoUrlInput,
                                residentPastor = residentPastorInput,
                                serviceTimes = serviceTimesInput,
                                helpline = helplineInput
                            )
                            Toast.makeText(context, "Dominion Centre details updated successfully!", Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_branding_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CaciRoyalBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Dominion Centre Details", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Section 3: Transparent Fee Policy
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ChurchSurface),
                border = CardDefaults.outlinedCardBorder(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. Giving & Storehouse Transparency Policy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CaciRoyalBlue
                    )
                    Text(
                        text = "Configure how payment provider fees (Paystack / Mobile Money) are managed.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ChurchBackground, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Fee Rate: ${feeConfig.rate}%", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Cap: GHS ${String.format("%.2f", feeConfig.cap)} per transaction", fontSize = 10.sp, color = TextSecondary)
                        }
                        Surface(
                            color = GreenBlessing.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenBlessing,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Dominion Centre operates a zero-hidden-fee policy. Every donor sees the transparent processing fee separately before giving.",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
