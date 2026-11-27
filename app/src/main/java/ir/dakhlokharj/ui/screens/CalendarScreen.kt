package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.core.date.JalaliCalendar
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.money.Money
import ir.dakhlokharj.domain.engine.CalendarEvent
import ir.dakhlokharj.domain.model.FinancialEvent
import ir.dakhlokharj.domain.model.PlannedEventKind
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.plannedEventKindLabel
import ir.dakhlokharj.ui.theme.*
import ir.dakhlokharj.ui.viewmodel.OverviewViewModel
import ir.dakhlokharj.ui.viewmodel.PlanningViewModel
import kotlinx.coroutines.launch

@Composable
fun CalendarScreen(
    overview: OverviewViewModel,
    planning: PlanningViewModel,
    onBack: () -> Unit,
    onOpenTransaction: (Long) -> Unit,
) {
    val state by overview.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val month = state.calendarMonth
    var selected by remember { mutableStateOf(state.today) }
    var showAddEvent by remember { mutableStateOf(false) }
    var showAmount by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var amountToman by remember { mutableStateOf(0L) }
    var kind by remember { mutableStateOf(PlannedEventKind.EXPENSE) }
    var eventDate by remember { mutableStateOf(state.today) }
    var definite by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(month?.year, month?.month) {
        if (month != null) selected = JalaliDate(month.year, month.month, 1)
    }

    DkScreen(
        title = "تقویم مالی",
        subtitle = "رویدادها، اقساط و سررسیدها",
        onBack = onBack,
        actions = {
            IconButton(onClick = { showAddEvent = true }) {
                Icon(Icons.Outlined.Add, contentDescription = "رویداد جدید")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 6.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                DkCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { overview.previousMonth() }) {
                            Icon(Icons.Outlined.ChevronRight, contentDescription = "ماه قبل")
                        }
                        Text(
                            if (month != null) "${JalaliDate.MONTH_NAMES[month.month - 1]} ${fa(month.year)}" else "",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        IconButton(onClick = { overview.nextMonth() }) {
                            Icon(Icons.Outlined.ChevronLeft, contentDescription = "ماه بعد")
                        }
                    }
                    if (month != null) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            JalaliDate.WEEKDAY_NAMES.forEach { day ->
                                Text(
                                    day.take(1),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        val offset = JalaliDate(month.year, month.month, 1).weekdayIndex
                        val cells: List<JalaliDate?> = List(offset) { null } + month.days.map { it.date }
                        cells.chunked(7).forEach { week ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                week.forEach { date ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(0.95f)
                                            .padding(1.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (date != null) {
                                            val day = month.days.firstOrNull { it.date == date }
                                            val isSelected = selected == date
                                            val isToday = date == state.today
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(MaterialTheme.shapes.small)
                                                    .background(
                                                        when {
                                                            isSelected -> colors.primary.copy(alpha = 0.18f)
                                                            isToday -> colors.primary.copy(alpha = 0.07f)
                                                            else -> androidx.compose.ui.graphics.Color.Transparent
                                                        },
                                                    )
                                                    .border(
                                                        width = if (isToday) 1.dp else 0.dp,
                                                        color = if (isToday) colors.primary.copy(alpha = 0.5f) else androidx.compose.ui.graphics.Color.Transparent,
                                                        shape = MaterialTheme.shapes.small,
                                                    )
                                                    .clickable { selected = date },
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                            ) {
                                                Text(
                                                    fa(date.day),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (isSelected) colors.primary else colors.onSurface,
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    if (day != null && day.income.isPositive) {
                                                        Box(Modifier.size(4.dp).clip(CircleShape).background(colors.primary))
                                                    }
                                                    if (day != null && day.expense.isPositive) {
                                                        Box(Modifier.size(4.dp).clip(CircleShape).background(colors.secondary))
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(Modifier.fillMaxSize())
                                        }
                                    }
                                }
                                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }

            val selectedDay = month?.days?.firstOrNull { it.date == selected }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        faDate(selected, DateStyle.FULL),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (selectedDay != null) {
                        Text(
                            "درآمد ${moneyShort(selectedDay.income)} • هزینه ${moneyShort(selectedDay.expense)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }

            val events = selectedDay?.events.orEmpty()
            if (events.isEmpty()) {
                item {
                    EmptyState(
                        title = "رویدادی در این روز نیست",
                        description = "برای این روز رویداد مالی یا یادآور برنامه‌ریزی کنید.",
                        icon = Icons.Outlined.Event,
                        actionLabel = "افزودن رویداد",
                        onAction = {
                            eventDate = selected
                            showAddEvent = true
                        },
                    )
                }
            } else {
                items(events, key = { "${it.kind.name}-${it.title}-${it.amount.rial}" }) { event ->
                    CalendarEventRow(event) {
                        if (event.paid) {
                            event.referenceId?.let(onOpenTransaction)
                        }
                    }
                }
            }

            if (state.plannedEvents.isNotEmpty()) {
                item { SectionHeader("رویدادهای برنامه‌ریزی‌شده") }
                items(state.plannedEvents.sortedBy { it.date }.take(8), key = { it.id }) { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { eventDate = event.date; showAddEvent = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(event.title, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${plannedEventKindLabel(event.kind)} • ${faDate(event.date, DateStyle.SHORT)} • ${if (event.definite) "قطعی" else "احتمالی"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                        Text(moneyShort(event.amount), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (showAddEvent) {
        DkBottomSheet(onDismiss = { showAddEvent = false }) {
            SheetTitle("رویداد مالی جدید", "برای پیش‌بینی دقیق‌تر، درآمد و هزینه‌های آینده را ثبت کنید")
            DkTextField(title, { title = it }, label = "عنوان رویداد", placeholder = "مثلاً پاداش پایان سال")
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PlannedEventKind.entries.toList()) { entry ->
                    DkChoiceChip(plannedEventKindLabel(entry), kind == entry) { kind = entry }
                }
            }
            Spacer(Modifier.height(10.dp))
            PickerRow("مبلغ", if (amountToman > 0) moneyText(Money.toman(amountToman)) else "تعیین مبلغ") { showAmount = true }
            Spacer(Modifier.height(10.dp))
            PickerRow("تاریخ", faDate(eventDate, DateStyle.FULL)) { showDate = true }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = definite, onCheckedChange = { definite = it })
                Spacer(Modifier.width(8.dp))
                Text(if (definite) "قطعی" else "احتمالی", style = MaterialTheme.typography.bodySmall)
            }
            if (error != null) {
                Text(error!!, color = colors.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton("ثبت رویداد") {
                if (title.isBlank()) error = "عنوان را وارد کنید"
                else if (amountToman <= 0) error = "مبلغ را وارد کنید"
                else {
                    scope.launch {
                        planning.saveEvent(
                            FinancialEvent(
                                kind = kind,
                                title = title,
                                amount = Money.toman(amountToman),
                                date = eventDate,
                                definite = definite,
                            ),
                            editing = false,
                        ).onSuccess {
                            showAddEvent = false
                            title = ""
                            amountToman = 0
                            error = null
                        }.onFailure { error = it.message }
                    }
                }
            }
        }
    }

    if (showAmount) {
        AmountInputSheet("مبلغ رویداد", amountToman, { showAmount = false }) { amountToman = it }
    }
    if (showDate) {
        DatePickerSheet(initial = eventDate, onDismiss = { showDate = false }) { eventDate = it }
    }
}

@Composable
private fun CalendarEventRow(event: CalendarEvent, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tint = if (event.kind.income) colors.primary else colors.secondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (event.paid) tint else tint.copy(alpha = 0.4f)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${event.kind.label} • ${if (event.paid) "انجام‌شده" else "پیش‌رو"}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
        Text(
            (if (event.kind.income) "+" else "−") + moneyShort(event.amount),
            color = tint,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
