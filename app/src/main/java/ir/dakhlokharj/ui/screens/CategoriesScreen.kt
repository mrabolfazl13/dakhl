package ir.dakhlokharj.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.dakhlokharj.domain.model.Category
import ir.dakhlokharj.domain.model.CategoryKind
import ir.dakhlokharj.ui.categoryKindLabel
import ir.dakhlokharj.ui.components.*
import ir.dakhlokharj.ui.theme.fa
import ir.dakhlokharj.ui.theme.iconFor
import ir.dakhlokharj.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

private val palette = listOf(
    0xFF34D399.toInt(), 0xFF60A5FA.toInt(), 0xFFE9C46A.toInt(), 0xFFF472B6.toInt(),
    0xFF818CF8.toInt(), 0xFFF97316.toInt(), 0xFF14B8A6.toInt(), 0xFF94A3B8.toInt(),
)
private val iconKeys = listOf("food", "transport", "home", "health", "clothes", "fun", "education", "bills", "salary", "work", "savings", "category")

@Composable
fun CategoriesScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    var tab by remember { mutableIntStateOf(0) }
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var deleting by remember { mutableStateOf<Category?>(null) }
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf(palette.first()) }
    var iconKey by remember { mutableStateOf(iconKeys.first()) }
    var essential by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val kind = if (tab == 0) CategoryKind.EXPENSE else CategoryKind.INCOME
    val list = categories.filter { it.kind == kind }

    fun openForm(category: Category?) {
        editing = category
        name = category?.name ?: ""
        color = category?.colorArgb ?: palette.first()
        iconKey = category?.iconKey ?: iconKeys.first()
        essential = category?.essential ?: true
        error = null
        showForm = true
    }

    DkScreen(
        title = "دسته‌بندی‌ها",
        subtitle = "ساختار خرج و درآمد خود را شخصی‌سازی کنید",
        onBack = onBack,
        actions = {
            IconButton(onClick = { openForm(null) }) {
                Icon(Icons.Outlined.Add, contentDescription = "افزودن دسته")
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                SegmentedControl(listOf("هزینه‌ها", "درآمدها"), tab) { tab = it }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${fa(list.size)} دسته • ${fa(list.count { it.system })} پیش‌فرض",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            items(list, key = { it.id }) { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(colors.surface)
                        .clickable { openForm(category) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(category.colorArgb).copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(iconFor(category.iconKey), contentDescription = null, tint = Color(category.colorArgb))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(category.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (category.essential) "ضروری" else "غیرضروری",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    if (category.system) {
                        Icon(Icons.Outlined.Lock, contentDescription = "پیش‌فرض", tint = colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    } else {
                        IconButton(onClick = { deleting = category }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "حذف", tint = colors.error)
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        DkBottomSheet(onDismiss = { showForm = false }) {
            SheetTitle(if (editing == null) "دسته جدید" else "ویرایش دسته", categoryKindLabel(kind))
            DkTextField(name, { name = it }, label = "نام دسته", placeholder = "مثلاً قهوه")
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = essential, onCheckedChange = { essential = it })
                Spacer(Modifier.width(8.dp))
                Text("هزینه ضروری است", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(10.dp))
            Text("رنگ", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                palette.forEach { argb ->
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(argb))
                            .clickable { color = argb },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (color == argb) Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(Color.White))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text("آیکون", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(iconKeys) { key ->
                    val selected = iconKey == key
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) color.copy(alpha = 0.2f) else colors.surfaceVariant)
                            .clickable { iconKey = key },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(iconFor(key), contentDescription = null, tint = if (selected) Color(color) else colors.onSurfaceVariant)
                    }
                }
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = colors.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(14.dp))
            PrimaryButton(if (editing == null) "افزودن دسته" else "ذخیره تغییرات") {
                if (name.isBlank()) error = "نام دسته را وارد کنید"
                else {
                    scope.launch {
                        val category = (editing ?: Category(
                            name = name,
                            kind = kind,
                            iconKey = iconKey,
                            colorArgb = color,
                        )).copy(
                            name = name,
                            iconKey = iconKey,
                            colorArgb = color,
                            essential = essential,
                        )
                        val action = if (editing == null) viewModel.addCategory(category) else viewModel.updateCategory(category)
                        action.onSuccess { showForm = false }
                            .onFailure { error = it.message }
                    }
                }
            }
        }
    }

    val deleteTarget = deleting
    if (deleteTarget != null) {
        ConfirmDialog(
            title = "حذف دسته؟",
            message = if (deleteTarget.system) {
                "دسته‌های پیش‌فرض قابل حذف نیستند."
            } else {
                "این دسته فقط وقتی حذف می‌شود که در هیچ تراکنشی استفاده نشده باشد."
            },
            confirmLabel = "حذف",
            danger = true,
            onConfirm = {
                if (!deleteTarget.system) {
                    scope.launch {
                        viewModel.deleteCategory(deleteTarget)
                            .onFailure { error = it.message }
                    }
                }
            },
            onDismiss = { deleting = null },
        )
    }
}
