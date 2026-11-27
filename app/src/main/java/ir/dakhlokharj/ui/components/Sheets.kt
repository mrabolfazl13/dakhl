package ir.dakhlokharj.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.dakhlokharj.core.date.JalaliCalendar
import ir.dakhlokharj.core.date.JalaliDate
import ir.dakhlokharj.core.format.PersianText
import ir.dakhlokharj.ui.theme.fa
import ir.dakhlokharj.ui.theme.faDate
import ir.dakhlokharj.ui.theme.moneyText
import ir.dakhlokharj.domain.model.CurrencyUnit
import ir.dakhlokharj.core.money.Money

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DkBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .size(width = 42.dp, height = 4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(50),
                    ),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            content = content,
        )
    }
}

@Composable
fun SheetTitle(title: String, subtitle: String? = null) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    if (subtitle != null) {
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(16.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmountInputSheet(
    title: String,
    initialToman: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var digits by remember { mutableStateOf(if (initialToman > 0) initialToman.toString() else "") }
    val value = digits.toLongOrNull() ?: 0L
    DkBottomSheet(onDismiss = onDismiss) {
        SheetTitle(title, "مبلغ را به تومان وارد کنید")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (digits.isEmpty()) "۰" else PersianText.toPersianDigits(PersianText.grouped(value)),
                style = MaterialTheme.typography.displayMedium,
                color = if (digits.isEmpty()) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
        Spacer(Modifier.height(16.dp))
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "000", "0", "⌫")
        keys.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable {
                                when (key) {
                                    "⌫" -> digits = digits.dropLast(1)
                                    "000" -> if (digits.isNotEmpty() && digits.length < 13) digits += "000"
                                    else -> if (digits.length < 13) digits += key
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (key == "⌫") "⌫" else ThemePersianText.toPersianDigits(key),
                            style = MaterialTheme.typography.headlineSmall,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        PrimaryButton(
            text = "تأیید مبلغ",
            enabled = value > 0,
            onClick = {
                onConfirm(value)
                onDismiss()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(
    title: String = "انتخاب تاریخ",
    initial: JalaliDate = JalaliDate.today(),
    onDismiss: () -> Unit,
    onConfirm: (JalaliDate) -> Unit,
) {
    var viewYear by remember { mutableIntStateOf(initial.year) }
    var viewMonth by remember { mutableIntStateOf(initial.month) }
    var selected by remember { mutableStateOf(initial) }
    DkBottomSheet(onDismiss = onDismiss) {
        SheetTitle(title, "تقویم شمسی")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = {
                if (viewMonth == 1) {
                    viewMonth = 12
                    viewYear -= 1
                } else viewMonth -= 1
            }) { Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = "ماه قبل") }
            Text(
                text = "${JalaliDate.MONTH_NAMES[viewMonth - 1]} ${fa(viewYear)}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            IconButton(onClick = {
                if (viewMonth == 12) {
                    viewMonth = 1
                    viewYear += 1
                } else viewMonth += 1
            }) { Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "ماه بعد") }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            JalaliDate.WEEKDAY_NAMES.forEach { day ->
                Text(
                    text = day.take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        val offset = JalaliDate(viewYear, viewMonth, 1).weekdayIndex
        val monthLength = JalaliCalendar.monthLength(viewYear, viewMonth)
        val cells: List<Int?> = List(offset) { null } + (1..monthLength).toList()
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) {
                            val date = JalaliDate(viewYear, viewMonth, day)
                            val isSelected = date == selected
                            val isToday = date == JalaliDate.today()
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) Modifier.background(MaterialTheme.colorScheme.primary)
                                        else Modifier,
                                    )
                                    .then(
                                        if (isToday && !isSelected) Modifier.border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            CircleShape,
                                        ) else Modifier,
                                    )
                                    .clickable { selected = date },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = fa(day),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = selected.formatFull(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            text = "تأیید تاریخ",
            onClick = {
                onConfirm(selected)
                onDismiss()
            },
        )
    }
}

data class SheetOption(
    val id: Long,
    val title: String,
    val subtitle: String? = null,
    val color: Color? = null,
    val iconKey: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionPickerSheet(
    title: String,
    options: List<SheetOption>,
    selectedId: Long?,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    DkBottomSheet(onDismiss = onDismiss) {
        SheetTitle(title)
        LazyColumn(modifier = Modifier.heightIn(max = 440.dp)) {
            items(options, key = { it.id }) { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            onSelect(option.id)
                            onDismiss()
                        }
                        .background(
                            if (option.id == selectedId) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else Color.Transparent,
                        )
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (option.color != null) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(option.color, CircleShape),
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(option.title, style = MaterialTheme.typography.bodyLarge)
                        if (option.subtitle != null) {
                            Text(
                                option.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (option.id == selectedId) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PickerRow(
    label: String,
    value: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                MaterialTheme.shapes.medium,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(
            Icons.Outlined.ChevronLeft,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun DkChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                1.dp,
                if (selected) color.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "تأیید",
    dismissLabel: String = "انصراف",
    danger: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(
                    confirmLabel,
                    color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
fun TextPromptDialog(
    title: String,
    description: String,
    label: String,
    confirmLabel: String = "تأیید",
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(label) },
                    singleLine = true,
                    visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(text)
                    onDismiss()
                },
                enabled = text.isNotBlank(),
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        },
    )
}
