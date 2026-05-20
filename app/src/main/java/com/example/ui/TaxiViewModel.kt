package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.TaxiDatabase
import com.example.data.TaxiRepository
import com.example.data.TaxiTransaction
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class DriverPerformance(
    val driverName: String,
    val totalRides: Int,
    val totalRevenue: Double,
    val totalExpenses: Double,
    val netBalance: Double
)

data class RouteReport(
    val route: String,
    val totalTrips: Int,
    val totalRevenue: Double,
    val averageRevenue: Double,
    val totalExpenses: Double,
    val netBalance: Double
)

data class GeneralMetrics(
    val totalRevenue: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val balance: Double = 0.0,
    val totalCount: Int = 0,
    val averageRevenueRide: Double = 0.0
)

data class FilterState1(
    val query: String,
    val driver: String,
    val route: String
)

data class FilterState2(
    val category: String,
    val type: String,
    val dateType: String
)

class TaxiViewModel(
    application: Application,
    private val repository: TaxiRepository
) : AndroidViewModel(application) {

    // Configurable category lists stored in UI state
    val configurableRevenueCategories = MutableStateFlow(listOf("Uber", "Táxi", "Fidelizado", "Maçaneta"))
    val configurableExpenseCategories = MutableStateFlow(listOf("Combustível", "Manutenção", "Alimentação", "Pedágio", "Outros"))

    fun addRevenueCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isNotBlank() && !configurableRevenueCategories.value.contains(trimmed)) {
            configurableRevenueCategories.value = configurableRevenueCategories.value + trimmed
        }
    }

    fun removeRevenueCategory(category: String) {
        configurableRevenueCategories.value = configurableRevenueCategories.value - category
    }

    fun addExpenseCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isNotBlank() && !configurableExpenseCategories.value.contains(trimmed)) {
            configurableExpenseCategories.value = configurableExpenseCategories.value + trimmed
        }
    }

    fun removeExpenseCategory(category: String) {
        configurableExpenseCategories.value = configurableExpenseCategories.value - category
    }

    // Default pre-populated items to help auto-suggest and UI dropdowns
    val defaultDrivers = listOf("Carlos", "Ana", "Bruno", "Patrícia")
    val defaultRoutes = listOf("Centro - Aeroporto", "Zona Sul - Aeroporto", "Rodoviária - Centro", "Zona Norte - Centro", "Corrida Local (Bairro)")
    val defaultRevenueCategories = listOf("Corrida (App)", "Corrida (Rua)", "Viagem Particular", "Encomenda", "Outra Receita")
    val defaultExpenseCategories = listOf("Combustível (Gasolina/Etanol)", "Combustível (GNV)", "Manutenção / Oficina", "Alimentação", "Lavagem / Limpeza", "Pedágio", "Outra Despesa")

    // Filter states
    val searchQuery = MutableStateFlow("")
    val selectedDriverFilter = MutableStateFlow("Todos")
    val selectedRouteFilter = MutableStateFlow("Todas")
    val selectedCategoryFilter = MutableStateFlow("Todas")
    val selectedTypeFilter = MutableStateFlow("TODOS") // TODOS, RECEITAS, DESPESAS
    val dateFilterType = MutableStateFlow("Todos") // Todos, Hoje, Esta Semana, Este Mês

    // Raw transactions stream from Room
    val allTransactions: StateFlow<List<TaxiTransaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Dynamic Lists (extracted from transactions database so dynamic entries also appear)
    val dynamicDrivers: StateFlow<List<String>> = allTransactions.map { list ->
        val fromDb = list.map { it.driverName }.filter { it.isNotBlank() }.distinct()
        (defaultDrivers + fromDb).distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultDrivers)

    val dynamicRoutes: StateFlow<List<String>> = allTransactions.map { list ->
        val fromDb = list.map { it.route }.filter { it.isNotBlank() }.distinct()
        (defaultRoutes + fromDb).distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultRoutes)

    val dynamicCategories: StateFlow<List<String>> = allTransactions.map { list ->
        val fromDb = list.map { it.category }.filter { it.isNotBlank() }.distinct()
        (defaultRevenueCategories + defaultExpenseCategories + fromDb).distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), defaultRevenueCategories + defaultExpenseCategories)

    // Sub-combining filters for robustness and absolute type safety
    private val filterGroup1: Flow<FilterState1> = combine(
        searchQuery,
        selectedDriverFilter,
        selectedRouteFilter
    ) { query, driver, route ->
        FilterState1(query, driver, route)
    }

    private val filterGroup2: Flow<FilterState2> = combine(
        selectedCategoryFilter,
        selectedTypeFilter,
        dateFilterType
    ) { category, type, dateType ->
        FilterState2(category, type, dateType)
    }

    // Filtered transaction list based on active filters
    val filteredTransactions: StateFlow<List<TaxiTransaction>> = combine(
        allTransactions,
        filterGroup1,
        filterGroup2
    ) { transactions, fg1, fg2 ->
        transactions.filter { tx ->
            // Search Query
            val matchesQuery = fg1.query.isBlank() || 
                tx.route.contains(fg1.query, ignoreCase = true) ||
                tx.driverName.contains(fg1.query, ignoreCase = true) ||
                tx.category.contains(fg1.query, ignoreCase = true) ||
                tx.notes.contains(fg1.query, ignoreCase = true)

            // Driver Filter
            val matchesDriver = fg1.driver == "Todos" || tx.driverName.equals(fg1.driver, ignoreCase = true)

            // Route Filter
            val matchesRoute = fg1.route == "Todas" || tx.route.equals(fg1.route, ignoreCase = true)

            // Category Filter
            val matchesCategory = fg2.category == "Todas" || tx.category.equals(fg2.category, ignoreCase = true)

            // Type Filter
            val matchesType = when (fg2.type) {
                "RECEITAS" -> tx.isRevenue
                "DESPESAS" -> !tx.isRevenue
                else -> true
            }

            // Date Filter
            val matchesDate = when (fg2.dateType) {
                "Hoje" -> isToday(tx.timestamp)
                "Esta Semana" -> isThisWeek(tx.timestamp)
                "Este Mês" -> isThisMonth(tx.timestamp)
                else -> true
            }

            matchesQuery && matchesDriver && matchesRoute && matchesCategory && matchesType && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // General Metrics for CURRENT filters
    val generalMetrics: StateFlow<GeneralMetrics> = filteredTransactions.map { list ->
        val revenue = list.filter { it.isRevenue }.sumOf { it.amount }
        val expenses = list.filter { !it.isRevenue }.sumOf { it.amount }
        val net = revenue - expenses
        val count = list.size
        val avgRide = list.filter { it.isRevenue }.let { revs ->
            if (revs.isEmpty()) 0.0 else revs.sumOf { it.amount } / revs.size
        }
        GeneralMetrics(
            totalRevenue = revenue,
            totalExpenses = expenses,
            balance = net,
            totalCount = count,
            averageRevenueRide = avgRide
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GeneralMetrics())

    // Driver Performance Reports
    val driverPerformanceReport: StateFlow<List<DriverPerformance>> = filteredTransactions.map { list ->
        list.groupBy { it.driverName }
            .map { (driverName, txs) ->
                val totalRides = txs.count { it.isRevenue }
                val totalRevenue = txs.filter { it.isRevenue }.sumOf { it.amount }
                val totalExpenses = txs.filter { !it.isRevenue }.sumOf { it.amount }
                val netBalance = totalRevenue - totalExpenses
                DriverPerformance(
                    driverName = driverName,
                    totalRides = totalRides,
                    totalRevenue = totalRevenue,
                    totalExpenses = totalExpenses,
                    netBalance = netBalance
                )
            }.sortedByDescending { it.totalRevenue }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Route Reports
    val routeReport: StateFlow<List<RouteReport>> = filteredTransactions.map { list ->
        list.groupBy { it.route }
            .map { (routeName, txs) ->
                val totalTrips = txs.count { it.isRevenue }
                val totalRevenue = txs.filter { it.isRevenue }.sumOf { it.amount }
                val averageRevenue = if (totalTrips == 0) 0.0 else totalRevenue / totalTrips
                val totalExpenses = txs.filter { !it.isRevenue }.sumOf { it.amount }
                val netBalance = totalRevenue - totalExpenses
                RouteReport(
                    route = routeName,
                    totalTrips = totalTrips,
                    totalRevenue = totalRevenue,
                    averageRevenue = averageRevenue,
                    totalExpenses = totalExpenses,
                    netBalance = netBalance
                )
            }.sortedByDescending { it.totalRevenue }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Database Actions
    fun addTransaction(
        isRevenue: Boolean,
        amount: Double,
        timestamp: Long,
        category: String,
        route: String,
        driverName: String,
        notes: String
    ) {
        viewModelScope.launch {
            val tx = TaxiTransaction(
                isRevenue = isRevenue,
                amount = amount,
                timestamp = timestamp,
                category = category.trim(),
                route = route.trim(),
                driverName = driverName.trim(),
                notes = notes.trim()
            )
            repository.insert(tx)
        }
    }

    fun deleteTransaction(tx: TaxiTransaction) {
        viewModelScope.launch {
            repository.delete(tx)
        }
    }

    // Pre-populate some demo data on first-launch if the DB is completely empty!
    // This creates an incredibly polished, non-empty experience right out of the box.
    fun prePopulateIfEmpty() {
        viewModelScope.launch {
            val txs = repository.allTransactions.first()
            if (txs.isEmpty()) {
                val now = System.currentTimeMillis()
                val oneHour = 3600_000L
                val oneDay = 86400_000L

                // 2 days ago
                repository.insert(TaxiTransaction(isRevenue = true, amount = 120.0, timestamp = now - 2 * oneDay, category = "Viagem Particular", route = "Centro - Aeroporto", driverName = "Carlos", notes = "Dinheiro"))
                repository.insert(TaxiTransaction(isRevenue = false, amount = 50.0, timestamp = now - 2 * oneDay, category = "Combustível (Gasolina/Etanol)", route = "Centro - Aeroporto", driverName = "Carlos", notes = "Posto BR"))
                
                // Yesterday
                repository.insert(TaxiTransaction(isRevenue = true, amount = 85.0, timestamp = now - oneDay, category = "Corrida (App)", route = "Zona Sul - Aeroporto", driverName = "Ana", notes = "Uber"))
                repository.insert(TaxiTransaction(isRevenue = true, amount = 45.0, timestamp = now - oneDay - 2 * oneHour, category = "Corrida (Rua)", route = "Rodoviária - Centro", driverName = "Bruno", notes = "Pix"))
                repository.insert(TaxiTransaction(isRevenue = false, amount = 35.0, timestamp = now - oneDay, category = "Alimentação", route = "Rodoviária - Centro", driverName = "Bruno", notes = "Almoço"))

                // Today
                repository.insert(TaxiTransaction(isRevenue = true, amount = 150.0, timestamp = now - oneHour, category = "Viagem Particular", route = "Centro - Aeroporto", driverName = "Patrícia", notes = "Cartão"))
                repository.insert(TaxiTransaction(isRevenue = false, amount = 20.0, timestamp = now - oneHour / 2, category = "Pedágio", route = "Centro - Aeroporto", driverName = "Patrícia"))
                repository.insert(TaxiTransaction(isRevenue = false, amount = 180.0, timestamp = now - 4 * oneHour, category = "Manutenção / Oficina", route = "Corrida Local (Bairro)", driverName = "Carlos", notes = "Troca de óleo"))
            }
        }
    }

    // Helper Date utilities
    private fun isToday(timeMs: Long): Boolean {
        val today = Calendar.getInstance()
        val txDate = Calendar.getInstance().apply { timeInMillis = timeMs }
        return today.get(Calendar.YEAR) == txDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == txDate.get(Calendar.DAY_OF_YEAR)
    }

    private fun isThisWeek(timeMs: Long): Boolean {
        val today = Calendar.getInstance()
        val txDate = Calendar.getInstance().apply { timeInMillis = timeMs }
        return today.get(Calendar.YEAR) == txDate.get(Calendar.YEAR) &&
                today.get(Calendar.WEEK_OF_YEAR) == txDate.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isThisMonth(timeMs: Long): Boolean {
        val today = Calendar.getInstance()
        val txDate = Calendar.getInstance().apply { timeInMillis = timeMs }
        return today.get(Calendar.YEAR) == txDate.get(Calendar.YEAR) &&
                today.get(Calendar.MONTH) == txDate.get(Calendar.MONTH)
    }
}

class TaxiViewModelFactory(
    private val application: Application,
    private val repository: TaxiRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaxiViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaxiViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
