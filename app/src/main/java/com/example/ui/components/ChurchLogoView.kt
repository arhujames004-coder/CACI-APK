package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ChurchBranding
import com.example.ui.theme.CaciGold
import java.io.File

@Composable
fun ChurchLogoView(
    branding: ChurchBranding,
    modifier: Modifier = Modifier,
    contentDescription: String = "Church Logo",
    showBorder: Boolean = true
) {
    val finalModifier = if (showBorder) {
        modifier
            .clip(CircleShape)
            .border(1.5.dp, CaciGold, CircleShape)
    } else {
        modifier.clip(CircleShape)
    }

    val galleryFilePath = branding.customGalleryLogoUri

    val customBitmap = remember(galleryFilePath) {
        if (galleryFilePath.isNotBlank()) {
            try {
                val file = File(galleryFilePath)
                if (file.exists() && file.length() > 0) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    if (customBitmap != null) {
        Image(
            bitmap = customBitmap.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = finalModifier,
            contentScale = ContentScale.Crop
        )
    } else {
        val logoRes = if (branding.logoResourceName == "ic_tithe_storehouse_1793809781217") {
            R.drawable.ic_tithe_storehouse_1793809781217
        } else {
            R.drawable.ic_caci_logo_1793811954335
        }

        Image(
            painter = painterResource(id = logoRes),
            contentDescription = contentDescription,
            modifier = finalModifier,
            contentScale = ContentScale.Crop
        )
    }
}
