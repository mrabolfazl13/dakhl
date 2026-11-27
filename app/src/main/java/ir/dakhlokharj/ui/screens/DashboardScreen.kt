package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.*
import ir.dakhlokharj.ui.viewmodel.OverviewViewModel

@Composable
fun DashboardScreen(
    viewModel: OverviewViewModel,
    onNavigate: (String) -> Unit,
    onAddTransaction: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val today = state.today

    DkScreen(
        title = "دخل‌وخرج",
        subtitle = faDate(today, DateStyle.FULL),
        actions = {
            IconButton(onClick = { viewModel.setHideBalances(!state.settings.hideBalances) }) {
                Icon(
                    if (state.settings.hideBalances) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (state.settings.hideBalances) "نمایش موجودی" else "پنهان‌کردن موجودی",
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddTransaction("EXPENSE") },
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("ثبت جدید", fontWeight = FontWeight.Medium) },
            )
        },
    ) { padding ->
        if (!state.ready) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
            return@DkScreen
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 6.dp,
                bottom = padding.calculateBottomPadding() + 110.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                BalanceHero(
                    totalBalance = state.totalBalance,
                    income = state.monthIncome,
                    expense = state.monthExpense,
                    savings = state.monthSavings,
                    hidden = state.settings.hideBalances,
                    onToggleHidden = { viewModel.setHideBalances(!state.settings.hideBalances) },
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAction("هزینه", Icons.Outlined.ArrowDownward, colors.secondary) { onAddTransaction("EXPENSE") }
                    QuickAction("درآمد", Icons.Outlined.ArrowUpward, colors.primary) { onAddTransaction("INCOME") }
                    QuickAction("انتقال", Icons.Outlined.SwapHoriz, colors.tertiary) { onAddTransaction("TRANSFER") }
                    QuickAction("حساب‌ها", Icons.Outlined.ChevronLeft, colors.onSurfaceVariant) { onNavigate("accounts") }
                }
            }

            if (state.alerts.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.alerts.take(2).forEach { alert ->
                            FinancialAlertCard(alert) {
                                when {
                                    alert.iconKey == "budget" -> onNavigate("budgets")
                                    alert.iconKey == "installment" -> onNavigate("installments")
                                    alert.iconKey == "debt" -> onNavigate("debts")
                                    else -> onNavigate("forecast")
                                }
                            }
                        }
                    }
                }
            }

            item {
                DkCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "پیش‌بینی پایان ماه",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                moneyText(state.forecast?.projectedEndBalance ?: Money.ZERO),
                                style = MaterialTheme.typography.headlineSmall,
                                color = if ((state.forecast?.projectedEndBalance ?: Money.ZERO).isNegative) colors.error else colors.onSurface,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "قابل خرج تا پایان ماه: ${moneyText(state.forecast?.freeToSpend ?: Money.ZERO)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(colors.tertiary.copy(alpha = 0.14f))
                                .clickable { onNavigate("forecast") },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Outlined.ChevronLeft, contentDescription = null, tint = colors.tertiary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile("تعهدات باقی‌مانده", moneyShort(state.forecast?.remainingCommitments ?: Money.ZERO), Modifier.weight(1f))
                        StatTile("اقساط پیش‌رو", moneyShort(state.forecast?.upcomingInstallments ?: Money.ZERO), Modifier.weight(1f))
                    }
                }
            }

            item {
                SectionHeader("جریان نقدی شش ماه", actionLabel = "گزارش‌ها") { onNavigate("reports") }
            }
            item {
                DkCard {
                    if (state.monthlyTotals.isEmpty()) {
                        Text("برای مشاهده نمودار، تراکنش ثبت کنید.", style = MaterialTheme.typography.bodySmall)
                    } else {
                        CashFlowChart(state.monthlyTotals)
                    }
                }
            }

            item {
                DkCard(onClick = { onNavigate("health") }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HealthRing(score = state.health?.score ?: 0, size = 104)
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "سلامت مالی",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                state.health?.grade ?: "در حال محاسبه",
                                style = MaterialTheme.typography.headlineSmall,
                                color = colors.primary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                state.health?.summary ?: "برای محاسبه دقیق، چند تراکنش ثبت کنید.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            if (state.budgetProgresses.isNotEmpty()) {
                item {
                    SectionHeader("بودجه‌های این ماه", actionLabel = "همه") { onNavigate("budgets") }
                }
                items(state.budgetProgresses.take(2), key = { it.budget.id }) { progress ->
                    BudgetProgressCard(progress) { onNavigate("budgets") }
                }
            }

            if (state.insights.isNotEmpty()) {
                item {
                    SectionHeader("بینش‌های مالی", actionLabel = "بیشتر") { onNavigate("insights") }
                }
                item {
                    InsightPreview(state.insights.first()) { onNavigate("insights") }
                }
            }

            item {
                SectionHeader("تراکنش‌های اخیر", actionLabel = "همه") { onNavigate("transactions") }
            }
            if (state.recentTransactions.isEmpty()) {
                item {
                    EmptyState(
                        title = "هنوز تراکنشی ثبت نشده",
                        description = "اولین هزینه یا درآمد خود را ثبت کنید تا تحلیل‌های هوشمند آغاز شود.",
                        icon = Icons.Outlined.Add,
                        actionLabel = "ثبت اولین تراکنش",
                        onAction = { onAddTransaction("EXPENSE") },
                    )
                }
            } else {
                items(state.recentTransactions, key = { it.id }) { transaction ->
                    TransactionTile(
                        transaction = transaction,
                        categoryName = state.categoryById[transaction.categoryId]?.name,
                        accountName = state.accountById[transaction.accountId]?.name,
                        showDate = true,
                    ) { onNavigate("transaction/${transaction.id}") }
                }
            }
        }
    }
}

@Composable
private fun BalanceHero(
    totalBalance: Money,
    income: Money,
    expense: Money,
    savings: Money,
    hidden: Boolean,
    onToggleHidden: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0F766E),
                        Color(0xFF123B4A),
                        Color(0xFF1B2540),
                    ),
                ),
            )
            .padding(20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "موجودی کل",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f),
                )
                SystemBadge()
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (hidden) "•••••• تومان" else moneyText(totalBalance),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroStat("درآمد ماه", income, Color(0xFFA7F3D0), Modifier.weight(1f))
                HeroStat("هزینه ماه", expense, Color(0xFFFDE68A), Modifier.weight(1f))
                HeroStat("پس‌انداز", savings, Color(0xFFC7D2FE), Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (savings.isPositive) "این ماه ${moneyText(savings)} مثبت هستید" else "برای بهبود، هزینه‌های غیرضروری را کم کنید",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (hidden) "نمایش" else "پنهان",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA7F3D0),
                    modifier = Modifier.clickable(onClick = onToggleHidden),
                )
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: Money, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.55f))
        Spacer(Modifier.height(2.dp))
        Text(
            moneyShort(value),
            style = MaterialTheme.typography.labelLarge,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SystemBadge() {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.12f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color(0xFF34D399), CircleShape),
        )
        Spacer(Modifier.width(5.dp))
        Text("آفلاین و امن", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InsightPreview(insight: ir.dakhlokharj.domain.engine.Insight, onClick: () -> Unit) {
    DkCard(onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ToneColor(insight.tone).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(iconFor(insight.iconKey), contentDescription = null, tint = ToneColor(insight.tone), modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(insight.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    insight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
