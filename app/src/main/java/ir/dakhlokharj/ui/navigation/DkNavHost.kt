package ir.dakhlokharj.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ir.dakhlokharj.di.AppContainer
import ir.dakhlokharj.ui.screens.*
import ir.dakhlokharj.ui.viewmodel.DkViewModelFactory
import ir.dakhlokharj.ui.viewmodel.OverviewViewModel
import ir.dakhlokharj.ui.viewmodel.PlanningViewModel
import ir.dakhlokharj.ui.viewmodel.SettingsViewModel
import ir.dakhlokharj.ui.viewmodel.TransactionsViewModel

private data class MainTab(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val mainTabs = listOf(
    MainTab("dashboard", "خانه", Icons.Outlined.Home),
    MainTab("transactions", "تراکنش‌ها", Icons.Outlined.ReceiptLong),
    MainTab("budgets", "بودجه", Icons.Outlined.AccountBalanceWallet),
    MainTab("calendar", "تقویم", Icons.Outlined.CalendarMonth),
    MainTab("profile", "پروفایل", Icons.Outlined.Person),
)

@Composable
fun DkNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val factory = DkViewModelFactory(container)
    val overview: OverviewViewModel = viewModel(factory = factory)
    val transactions: TransactionsViewModel = viewModel(factory = factory)
    val planning: PlanningViewModel = viewModel(factory = factory)
    val settings: SettingsViewModel = viewModel(factory = factory)

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = mainTabs.any { it.route == currentRoute }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    mainTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = overview,
                    onNavigate = { route -> navController.navigate(route) },
                    onAddTransaction = { type -> navController.navigate("add?type=$type") },
                )
            }
            composable("transactions") {
                TransactionsScreen(
                    viewModel = transactions,
                    onOpenTransaction = { id -> navController.navigate("transaction/$id") },
                    onAddTransaction = { type -> navController.navigate("add?type=$type") },
                    onOpenAccounts = { navController.navigate("accounts") },
                )
            }
            composable(
                route = "add?type={type}&id={id}",
                arguments = listOf(
                    navArgument("type") { defaultValue = "EXPENSE" },
                    navArgument("id") { type = NavType.LongType; defaultValue = -1L },
                ),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: -1L
                AddTransactionScreen(
                    viewModel = transactions,
                    editingId = id.takeIf { it > 0 },
                    initialType = entry.arguments?.getString("type") ?: "EXPENSE",
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "transaction/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                TransactionDetailScreen(
                    viewModel = transactions,
                    transactionId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() },
                    onEdit = { id -> navController.navigate("add?id=$id") },
                )
            }
            composable("accounts") {
                AccountsScreen(
                    viewModel = planning,
                    onBack = { navController.popBackStack() },
                    onOpenAccount = { id -> navController.navigate("account/$id") },
                    onAddTransaction = { type -> navController.navigate("add?type=$type") },
                )
            }
            composable(
                route = "account/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                AccountDetailScreen(
                    viewModel = planning,
                    accountId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() },
                    onAddTransaction = { type -> navController.navigate("add?type=$type") },
                )
            }
            composable("budgets") {
                BudgetsScreen(
                    viewModel = planning,
                    onBack = { navController.popBackStack() },
                    onOpenBudget = { id -> navController.navigate("budget/$id") },
                    onAddTransaction = { type -> navController.navigate("add?type=$type") },
                )
            }
            composable(
                route = "budget/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                BudgetDetailScreen(
                    viewModel = planning,
                    budgetId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("debts") {
                DebtsScreen(
                    viewModel = planning,
                    onBack = { navController.popBackStack() },
                    onOpenDebt = { id -> navController.navigate("debt/$id") },
                    onOpenReceivable = { id -> navController.navigate("receivable/$id") },
                )
            }
            composable(
                route = "debt/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                DebtDetailScreen(
                    viewModel = planning,
                    debtId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "receivable/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                ReceivableDetailScreen(
                    viewModel = planning,
                    receivableId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("installments") {
                InstallmentsScreen(
                    viewModel = planning,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("subscriptions") {
                SubscriptionsScreen(
                    viewModel = planning,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("savings") {
                SavingsScreen(
                    viewModel = planning,
                    onBack = { navController.popBackStack() },
                    onOpenGoal = { id -> navController.navigate("savings/$id") },
                )
            }
            composable(
                route = "savings/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                SavingsDetailScreen(
                    viewModel = planning,
                    goalId = entry.arguments?.getLong("id") ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("calendar") {
                CalendarScreen(
                    overview = overview,
                    planning = planning,
                    onBack = { navController.popBackStack() },
                    onOpenTransaction = { id -> navController.navigate("transaction/$id") },
                )
            }
            composable("forecast") {
                ForecastScreen(viewModel = overview, onBack = { navController.popBackStack() })
            }
            composable("whatif") {
                WhatIfScreen(viewModel = overview, onBack = { navController.popBackStack() })
            }
            composable("health") {
                HealthScreen(viewModel = overview, onBack = { navController.popBackStack() })
            }
            composable("insights") {
                InsightsScreen(viewModel = overview, onBack = { navController.popBackStack() })
            }
            composable("reports") {
                ReportsScreen(
                    viewModel = overview,
                    settingsViewModel = settings,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("categories") {
                CategoriesScreen(viewModel = settings, onBack = { navController.popBackStack() })
            }
            composable("backup") {
                BackupScreen(viewModel = settings, onBack = { navController.popBackStack() })
            }
            composable("profile") {
                ProfileScreen(viewModel = overview, onNavigate = { route -> navController.navigate(route) })
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = settings,
                    planning = planning,
                    onBack = { navController.popBackStack() },
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
            composable("appearance") {
                AppearanceScreen(viewModel = settings, onBack = { navController.popBackStack() })
            }
            composable("privacy") {
                PrivacyScreen(
                    onBack = { navController.popBackStack() },
                    onOpenBackup = { navController.navigate("backup") },
                    onOpenSettings = { navController.navigate("settings") },
                )
            }
            composable("about") {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
