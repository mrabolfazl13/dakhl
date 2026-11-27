package ir.dakhlokharj.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ir.dakhlokharj.di.AppContainer

class DkViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(OverviewViewModel::class.java) -> OverviewViewModel(container) as T
        modelClass.isAssignableFrom(TransactionsViewModel::class.java) -> TransactionsViewModel(container) as T
        modelClass.isAssignableFrom(PlanningViewModel::class.java) -> PlanningViewModel(container) as T
        modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container) as T
        else -> error("ViewModel ناشناخته: ${modelClass.name}")
    }
}
