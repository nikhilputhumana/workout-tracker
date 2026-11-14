package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BorderedCard
import com.example.ui.components.KpiTile
import com.example.ui.components.SectionLabel
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LocalWorkoutPalette
import kotlin.math.roundToInt

@Composable
fun FoodPreviewScreen(
    bodyWeightKg: Double,
    modifier: Modifier = Modifier
) {
    val palette = LocalWorkoutPalette.current
    val suggestedCalories = (bodyWeightKg * 31.0).roundToInt().coerceIn(1600, 3800)
    val suggestedProtein = (bodyWeightKg * 2.0).roundToInt().coerceIn(90, 240)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.appBg)
            .testTag("food_preview_screen"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            BorderedCard(
                backgroundColor = palette.brandSoft,
                borderColor = palette.brandBd
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Restaurant,
                            contentDescription = "Food and calories preview",
                            tint = palette.brand,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Food & calories",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.brandInk
                        )
                        Text(
                            text = "Meal logging with calorie, protein, and macro totals is planned for a future release.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.brandHover
                        )
                    }
                    StatusBadge(
                        label = "Soon",
                        bg = palette.surface,
                        fg = palette.brandInk
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiTile(
                    label = "Daily target",
                    value = "$suggestedCalories kcal",
                    sub = "estimated from weight",
                    modifier = Modifier.weight(1f)
                )
                KpiTile(
                    label = "Protein goal",
                    value = "${suggestedProtein} g",
                    sub = "~2.0 g / kg target",
                    subColor = palette.greenInk,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            BorderedCard {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SectionLabel("Planned meal log")
                    PlannedMealRow(
                        icon = Icons.Filled.Coffee,
                        title = "Breakfast",
                        subtitle = "Log morning meals, oats, eggs, and shakes"
                    )
                    PlannedMealRow(
                        icon = Icons.Filled.LunchDining,
                        title = "Lunch",
                        subtitle = "Track daily protein & carb portions"
                    )
                    PlannedMealRow(
                        icon = Icons.Filled.DinnerDining,
                        title = "Dinner",
                        subtitle = "Compare net calories vs workout burn"
                    )
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun PlannedMealRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val palette = LocalWorkoutPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(palette.appBg)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = palette.faint,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = palette.text
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = palette.muted
            )
        }
        StatusBadge(
            label = "Planned",
            bg = palette.subtle,
            fg = palette.muted
        )
    }
}
