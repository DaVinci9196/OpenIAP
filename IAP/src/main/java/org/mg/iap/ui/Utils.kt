package org.mg.iap.ui

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.mg.iap.ContextProvider
import org.mg.iap.LogUtils
import org.mg.iap.core.ui.BGravity
import org.mg.iap.core.ui.BTextInfo
import org.mg.iap.core.ui.ColorType
import org.mg.iap.core.ui.TextAlignmentType

fun getWindowWidth(): Int {
    val resources = ContextProvider.context.resources
    return when (resources.configuration.orientation) {
        Configuration.ORIENTATION_PORTRAIT -> resources.displayMetrics.widthPixels
        Configuration.ORIENTATION_LANDSCAPE -> (resources.displayMetrics.widthPixels * 0.6).toInt()
        else -> resources.displayMetrics.widthPixels
    }
}

@Composable
fun getColorByType(t: ColorType?): Color? {
    if (t == null)
        return null
    return when (t) {
        ColorType.TEXT_PRIMARY -> if (isSystemInDarkTheme()) Color(0xffe8eaed) else Color(
            0xff202124
        )

        ColorType.TEXT_SECONDARY -> if (isSystemInDarkTheme()) Color(0xff9aa0a6) else Color(
            0xff5f6368
        )

        ColorType.APPS_2 -> if (isSystemInDarkTheme()) Color(0xffcae2ff) else Color(
            0xff5f6368
        )

        ColorType.APPS_3 -> if (isSystemInDarkTheme()) Color(0xff003b92) else Color(
            0xff5f6368
        )

        ColorType.MUSIC_3 -> Color.Transparent

        ColorType.ERROR_COLOR_PRIMARY -> if (isSystemInDarkTheme()) Color(0xff003b92) else Color(
            0xff5f6368
        )

        ColorType.PRIMARY_BUTTON_LABEL_DISABLED -> if (isSystemInDarkTheme()) Color(0xff5b5e64) else Color(
            0xff5f6368
        )

        ColorType.APPS_PRIMARY -> MaterialTheme.colorScheme.primary
        ColorType.BACKGROUND_PRIMARY -> MaterialTheme.colorScheme.background
        else -> {
            LogUtils.d("invalid color type $t")
            Color.Transparent
        }
    }
}

fun getFontSizeByType(t: Int?): TextUnit {
    return when (t) {
        10 -> 18.sp
        12 -> 16.sp
        14 -> 14.sp
        20 -> 14.sp
        21 -> 12.sp
        null -> 14.sp
        else -> {
            LogUtils.d("invalid font type $t")
            14.sp
        }
    }
}

fun getTextAlignment(textInfo: BTextInfo?): TextAlign {
    val gravitys = textInfo?.gravityList
    if (gravitys?.containsAll(listOf(BGravity.LEFT, BGravity.START)) == true)
        return TextAlign.Left
    if (gravitys?.containsAll(listOf(BGravity.CENTER, BGravity.CENTER_HORIZONTAL)) == true)
        return TextAlign.Center
    if (gravitys?.containsAll(listOf(BGravity.RIGHT, BGravity.END)) == true)
        return TextAlign.Right
    return when (val t = textInfo?.textAlignmentType) {
        TextAlignmentType.TEXT_ALIGNMENT_TEXT_END -> TextAlign.Right
        TextAlignmentType.TEXT_ALIGNMENT_TEXT_START -> TextAlign.Left
        TextAlignmentType.TEXT_ALIGNMENT_CENTER -> TextAlign.Center
        TextAlignmentType.TEXT_ALIGNMENT_VIEW_END -> TextAlign.Right
        null -> TextAlign.Left
        else -> {
            LogUtils.d("invalid text alignment type $t")
            TextAlign.Left
        }
    }
}