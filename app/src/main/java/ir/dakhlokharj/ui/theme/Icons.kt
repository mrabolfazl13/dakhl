package ir.dakhlokharj.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector

fun iconFor(key: String): ImageVector = when (key) {
    "bank" -> Icons.Outlined.AccountBalance
    "wallet", "cash" -> Icons.Outlined.AccountBalanceWallet
    "card" -> Icons.Outlined.CreditCard
    "salary", "work" -> Icons.Outlined.Work
    "business" -> Icons.Outlined.Home
    "project" -> Icons.Outlined.School
    "gift" -> Icons.Outlined.Favorite
    "profit" -> Icons.Outlined.Payments
    "food" -> Icons.Outlined.Fastfood
    "transport" -> Icons.Outlined.DirectionsCar
    "home" -> Icons.Outlined.Home
    "clothes" -> Icons.Outlined.ShoppingBag
    "health" -> Icons.Outlined.LocalHospital
    "fun" -> Icons.Outlined.Movie
    "education" -> Icons.Outlined.School
    "bills" -> Icons.Outlined.Receipt
    "shopping" -> Icons.Outlined.ShoppingBag
    "family" -> Icons.Outlined.Person
    "phone" -> Icons.Outlined.Phone
    "savings", "goal" -> Icons.Outlined.Savings
    "category" -> Icons.Outlined.Category
    "calendar" -> Icons.Outlined.DateRange
    "alert" -> Icons.Outlined.Notifications
    "flag" -> Icons.Outlined.Flag
    else -> Icons.Outlined.Category
}
