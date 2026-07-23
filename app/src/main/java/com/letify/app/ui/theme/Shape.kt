package com.letify.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object LetifyShapes {
    // Softened to match Telegram-style settings cards and the water-tracker
    // reference screenshots (was 28/20 — felt oversized on phone).
    val Card = RoundedCornerShape(20.dp)
    val CardSmall = RoundedCornerShape(16.dp)
    val Pill = RoundedCornerShape(999.dp)
    val Chip = RoundedCornerShape(12.dp)
    val ChipSmall = RoundedCornerShape(10.dp)
    val IconBox = RoundedCornerShape(12.dp)
    val Button = RoundedCornerShape(16.dp)
}
