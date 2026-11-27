package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.model.SavingsGoal
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.DateStyle
import ir.dakhlokharj.ui.theme.fa
import ir.dakhlokharj.ui.theme.faDate
import ir.dakhlokharj.ui.theme.moneyShort
import ir.dakhlokharj.ui.theme.moneyText
import ir.dakhlokharj.ui.viewmodel.PlanningViewModel
import kotlinx.coroutines.launch

private val goalColors = listOf(
    0xFF34D399.toInt(), 0xFF60A5FA.toInt(), 0xFFE9C46A.toInt(),
    0xFFF472B6.toInt(), 0xFF818CF8.toInt(), 0xFFF97316.toInt(),
)

@Composable
fun SavingsScreen(
    viewModel: PlanningViewModel,