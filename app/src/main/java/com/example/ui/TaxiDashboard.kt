package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.TaxiTransaction
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Currency Formatter for Brazilian Real (R$)
private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

// Date Formatter
private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxiDashboard(viewModel: TaxiViewModel) {
    // Collect states
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val rawTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val generalMetrics by viewModel.generalMetrics.collectAsStateWithLifecycle()
    
    val driversList by viewModel.dynamicDrivers.collectAsStateWithLifecycle()
    val routesList by viewModel.dynamicRoutes.collectAsStateWithLifecycle()
    val categoriesList by viewModel.dynamicCategories.collectAsStateWithLifecycle()

    val revenueCategoriesList by viewModel.configurableRevenueCategories.collectAsStateWithLifecycle()
    val expenseCategoriesList by viewModel.configurableExpenseCategories.collectAsStateWithLifecycle()

    val driverReportList by viewModel.driverPerformanceReport.collectAsStateWithLifecycle()
    val routeReportList by viewModel.routeReport.collectAsStateWithLifecycle()

    // Filter selectors
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedDriver by viewModel.selectedDriverFilter.collectAsStateWithLifecycle()
    val selectedRoute by viewModel.selectedRouteFilter.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val dateFilterType by viewModel.dateFilterType.collectAsStateWithLifecycle()

    // Form Screen and Dialog open state
    var activeScreen by varOf("dashboard") // "dashboard" or "add_transaction" or "config_revenue_categories" or "config_expense_categories"
    var showAddDialog by varOf(false)
    var addDialogIsRevenue by varOf(true)

    // Main level Screen navigation: 0 = Lançamentos, 1 = Relatórios, 2 = Filtros
    var currentTab by varOf(0)

    // Pre-populate database with dummy data on startup if it's currently empty
    LaunchedEffect(Unit) {
        viewModel.prePopulateIfEmpty()
    }

    val context = LocalContext.current
    val activity = remember(context) {
        var c = context
        while (c is android.content.ContextWrapper) {
            if (c is android.app.Activity) break
            c = c.baseContext
        }
        c as? android.app.Activity
    }

    var showExitDialog by varOf(false)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Intercept back button when on dashboard screen
    if (activeScreen == "dashboard") {
        BackHandler(enabled = true) {
            showExitDialog = true
        }
    } else if (activeScreen == "add_transaction") {
        // Intercept back button when on full-screen form to go back to dashboard
        BackHandler(enabled = true) {
            activeScreen = "dashboard"
        }
    } else if (activeScreen == "config_revenue_categories" || activeScreen == "config_expense_categories") {
        // Intercept back button on custom configuration screen to return home
        BackHandler(enabled = true) {
            activeScreen = "dashboard"
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = "Sair do Aplicativo",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Deseja realmente sair do aplicativo?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        activity?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sair", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showExitDialog = false },
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.testTag("exit_confirmation_dialog")
        )
    }

    if (activeScreen == "add_transaction") {
        val currentFormCategories = if (addDialogIsRevenue) revenueCategoriesList else expenseCategoriesList
        AddTransactionScreen(
            isRevenue = addDialogIsRevenue,
            categories = currentFormCategories,
            onDismiss = { activeScreen = "dashboard" },
            onSave = { isRevenue, amount, timestamp, category, route, driverName, notes ->
                viewModel.addTransaction(
                    isRevenue = isRevenue,
                    amount = amount,
                    timestamp = timestamp,
                    category = category,
                    route = route,
                    driverName = driverName,
                    notes = notes
                )
                activeScreen = "dashboard"
            }
        )
    } else if (activeScreen == "config_revenue_categories") {
        CategoryConfigScreen(
            isRevenue = true,
            categories = revenueCategoriesList,
            onAddCategory = { viewModel.addRevenueCategory(it) },
            onRemoveCategory = { viewModel.removeRevenueCategory(it) },
            onBack = { activeScreen = "dashboard" }
        )
    } else if (activeScreen == "config_expense_categories") {
        CategoryConfigScreen(
            isRevenue = false,
            categories = expenseCategoriesList,
            onAddCategory = { viewModel.addExpenseCategory(it) },
            onRemoveCategory = { viewModel.removeExpenseCategory(it) },
            onBack = { activeScreen = "dashboard" }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Táxi Controle",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = "Menu Geral",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
                        HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))

                        NavigationDrawerItem(
                            label = { Text("Filtros", fontWeight = FontWeight.Bold) },
                            selected = (currentTab == 2),
                            onClick = {
                                scope.launch { drawerState.close() }
                                currentTab = 2
                            },
                            icon = { Icon(Icons.Default.Search, contentDescription = "Filtros") },
                            modifier = Modifier.padding(vertical = 4.dp).testTag("drawer_item_filtros")
                        )

                        NavigationDrawerItem(
                            label = { Text("Configuração de Receita", fontWeight = FontWeight.Bold) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                activeScreen = "config_revenue_categories"
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Configuração de Receita", tint = Color(0xFF1B5E20)) },
                            modifier = Modifier.padding(vertical = 4.dp).testTag("drawer_item_config_receita")
                        )

                        NavigationDrawerItem(
                            label = { Text("Configuração de Despesa", fontWeight = FontWeight.Bold) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                activeScreen = "config_expense_categories"
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Configuração de Despesa", tint = Color(0xFFB71C1C)) },
                            modifier = Modifier.padding(vertical = 4.dp).testTag("drawer_item_config_despesa")
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "Versão 1.2",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        ) {
            Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("main_scaffold"),
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch { drawerState.open() }
                            },
                            modifier = Modifier.testTag("btn_open_drawer")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Abrir Menu",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Táxi Controle",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                letterSpacing = (-0.5).sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Receitas & Despesas",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.prePopulateIfEmpty() },
                        modifier = Modifier.testTag("refresh_demo")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Carregar Demo",
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = (currentTab == 0),
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Lançamentos") },
                    label = { 
                        Text(
                            text = "Lançamentos",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_lançamentos")
                )
                NavigationBarItem(
                    selected = (currentTab == 1),
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Relatórios") },
                    label = { 
                        Text(
                            text = "Relatórios",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_relatorios")
                )
                NavigationBarItem(
                    selected = (currentTab == 2),
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Filtros") },
                    label = { 
                        Text(
                            text = "Filtros",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_filtros")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Action buttons above the summary header card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        addDialogIsRevenue = true
                        activeScreen = "add_transaction"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B5E20), // Dark Green
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_nova_receita")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Nova Receita",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = {
                        addDialogIsRevenue = false
                        activeScreen = "add_transaction"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB71C1C), // Dark Red
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_nova_despesa")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Nova Despesa",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Quick Overall Card indicating live balance based on active filters
            LiveSummaryHeader(
                totalRevenue = generalMetrics.totalRevenue,
                totalExpenses = generalMetrics.totalExpenses,
                balance = generalMetrics.balance,
                filteredCount = generalMetrics.totalCount,
                hasActiveFilters = (selectedDriver != "Todos" || selectedRoute != "Todas" || selectedCategory != "Todas" || selectedType != "TODOS" || dateFilterType != "Todos")
            )

            // Dynamic view based on chosen tab
            when (currentTab) {
                0 -> TransactionsTabView(
                    transactions = transactions,
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.searchQuery.value = it },
                    onDelete = { viewModel.deleteTransaction(it) }
                )
                1 -> ReportsTabView(
                    driverReportList = driverReportList,
                    routeReportList = routeReportList,
                    generalRevenue = generalMetrics.totalRevenue,
                    generalExpenses = generalMetrics.totalExpenses,
                    generalBalance = generalMetrics.balance,
                    averageRevenueRide = generalMetrics.averageRevenueRide
                )
                2 -> FiltersTabView(
                    searchQuery = searchQuery,
                    onSearchChange = { viewModel.searchQuery.value = it },
                    selectedDriver = selectedDriver,
                    onDriverSelect = { viewModel.selectedDriverFilter.value = it },
                    driversList = driversList,
                    selectedRoute = selectedRoute,
                    onRouteSelect = { viewModel.selectedRouteFilter.value = it },
                    routesList = routesList,
                    selectedCategory = selectedCategory,
                    onCategorySelect = { viewModel.selectedCategoryFilter.value = it },
                    categoriesList = categoriesList,
                    selectedType = selectedType,
                    onTypeSelect = { viewModel.selectedTypeFilter.value = it },
                    dateFilterType = dateFilterType,
                    onDateFilterTypeSelect = { viewModel.dateFilterType.value = it },
                    onClearFilters = {
                        viewModel.searchQuery.value = ""
                        viewModel.selectedDriverFilter.value = "Todos"
                        viewModel.selectedRouteFilter.value = "Todas"
                        viewModel.selectedCategoryFilter.value = "Todas"
                        viewModel.selectedTypeFilter.value = "TODOS"
                        viewModel.dateFilterType.value = "Todos"
                    }
                )
            }
        }
    }
    }
    }

    // Add Form Dialog
    if (showAddDialog) {
        AddTransactionDialog(
            drivers = driversList,
            routes = routesList,
            revenueCategories = viewModel.defaultRevenueCategories,
            expenseCategories = viewModel.defaultExpenseCategories,
            initialIsRevenue = addDialogIsRevenue,
            onDismiss = { showAddDialog = false },
            onSave = { isRevenue, amount, cat, rt, dName, note ->
                viewModel.addTransaction(
                    isRevenue = isRevenue,
                    amount = amount,
                    timestamp = System.currentTimeMillis(),
                    category = cat,
                    route = rt,
                    driverName = dName,
                    notes = note
                )
                showAddDialog = false
            }
        )
    }
}

// Helper block for standard syntactic State representation creation (avoiding extra boilerplate)
@Composable
fun <T> varOf(initialValue: T): MutableState<T> = remember { mutableStateOf(initialValue) }

// --- BEAUTIFUL SUB-COMPOSABLES ---

@Composable
fun LiveSummaryHeader(
    totalRevenue: Double,
    totalExpenses: Double,
    balance: Double,
    filteredCount: Int,
    hasActiveFilters: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = (if (hasActiveFilters) "Resultados Filtrados" else "Saldo do Dia").uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                if (hasActiveFilters) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Filtro Ativo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Net balance
            Text(
                text = currencyFormat.format(balance),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1.5).sp
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Receitas",
                        tint = Color(0xFF1B5E20),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = currencyFormat.format(totalRevenue),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Despesas",
                        tint = Color(0xFFB71C1C),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = currencyFormat.format(totalExpenses),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.60f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Mostrando $filteredCount registros",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
            )
        }
    }
}

// TAB 1: Transactions View & Simple Search
@Composable
fun TransactionsTabView(
    transactions: List<TaxiTransaction>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onDelete: (TaxiTransaction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Simple fast filtering search bar styled as a modern rounded pill
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("search_bar_tx_tab"),
            placeholder = { Text("Pesquisar por motorista, rota ou nota...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Sem dados",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Nenhum lançamento encontrado.",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Adicione uma nova receita de corrida ou despesa clicando no botão abaixo ou clique no ícone de sincronizar no topo para carregar dados demo de simulação.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("transactions_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(transactions, key = { it.id }) { tx ->
                    TransactionCardItem(tx = tx, onDelete = { onDelete(tx) })
                }
            }
        }
    }
}

@Composable
fun TransactionCardItem(
    tx: TaxiTransaction,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_item_${tx.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Time tag
                val timeString = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(tx.timestamp))
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Column {
                    Text(
                        text = "${tx.driverName} • ${tx.route}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${tx.category}${if (tx.notes.isNotBlank()) " • ${tx.notes}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = if (tx.isRevenue) "+${currencyFormat.format(tx.amount)}" else "-${currencyFormat.format(tx.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (tx.isRevenue) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_tx_${tx.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Deletar registro",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// TAB 2: Clean Reports & Metrics View
@Composable
fun ReportsTabView(
    driverReportList: List<DriverPerformance>,
    routeReportList: List<RouteReport>,
    generalRevenue: Double,
    generalExpenses: Double,
    generalBalance: Double,
    averageRevenueRide: Double
) {
    var reportSection by varOf(0) // 0 = Geral/Resumo, 1 = Por Motorista, 2 = Por Rota
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(12.dp)
            .testTag("reports_tab_view")
    ) {
        // Tab segment selectors for report type
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { reportSection = 0 },
                modifier = Modifier
                    .weight(1f)
                    .testTag("report_sec_general"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (reportSection == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (reportSection == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text("Visão Geral", style = MaterialTheme.typography.labelSmall)
            }
            Button(
                onClick = { reportSection = 1 },
                modifier = Modifier
                    .weight(1f)
                    .testTag("report_sec_driver"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (reportSection == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (reportSection == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text("Por Motorista", style = MaterialTheme.typography.labelSmall)
            }
            Button(
                onClick = { reportSection = 2 },
                modifier = Modifier
                    .weight(1f)
                    .testTag("report_sec_route"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (reportSection == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                    contentColor = if (reportSection == 2) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                ),
                contentPadding = PaddingValues(2.dp)
            ) {
                Text("Por Rota", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (reportSection) {
            0 -> {
                // General metrics report
                Text(
                    "Desempenho Geral / Métricas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Margin Metric
                val marginPercentage = if (generalRevenue == 0.0) 0.0 else (generalBalance / generalRevenue) * 100
                MetricStatusCard(
                    title = "Margem de Lucro",
                    subtitle = "Saldo líquido comparado às receitas faturadas",
                    value = "${String.format("%.1f", marginPercentage)}%",
                    description = if (marginPercentage >= 30) "Ótimo aproveitamento do faturamento!" else "Atenção com gastos excessivos.",
                    colorAccent = if (marginPercentage >= 30) Color(0xFF2E7D32) else if (marginPercentage >= 10) Color(0xFFF57C00) else Color(0xFFC62828)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Average Ride Revenue Metric
                MetricStatusCard(
                    title = "Faturamento Médio por Corrida",
                    subtitle = "Valor médio faturado em cada corrida de táxi",
                    value = currencyFormat.format(averageRevenueRide),
                    description = "Média de receitas de corridas completadas",
                    colorAccent = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Operating Expense Ratio
                val expenseRatio = if (generalRevenue == 0.0) 0.0 else (generalExpenses / generalRevenue) * 100
                MetricStatusCard(
                    title = "Índice de Despesa Operacional",
                    subtitle = "Fração do faturamento consumida pelas despesas",
                    value = "${String.format("%.1f", expenseRatio)}%",
                    description = "Combustível, manutenção e comissões representam este percentual",
                    colorAccent = if (expenseRatio < 40) Color(0xFF2E7D32) else if (expenseRatio < 65) Color(0xFFF57C00) else Color(0xFFC62828)
                )
            }

            1 -> {
                // Driver performance report
                Text(
                    "Desempenho de Motoristas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Faturamento, custos e saldo líquido calculados acumulados por motorista",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (driverReportList.isEmpty()) {
                    Text(
                        "Sem dados para gerar relatório de motoristas.",
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    driverReportList.forEach { p ->
                        DriverReportCard(p = p)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            2 -> {
                // Route reports
                Text(
                    "Gastos e Ganhos por Rota",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    "Frequência, faturamento médio e saldo líquido por destino/trajeto",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (routeReportList.isEmpty()) {
                    Text(
                        "Sem dados para gerar relatório de rotas.",
                        modifier = Modifier.padding(24.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    routeReportList.forEach { r ->
                        RouteReportCard(r = r)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MetricStatusCard(
    title: String,
    subtitle: String,
    value: String,
    description: String,
    colorAccent: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                fontWeight = FontWeight.ExtraBold,
                color = colorAccent
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DriverReportCard(p: DriverPerformance) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Motorista Icon", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(p.driverName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${p.totalRides} Corridas",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("FATURAMENTO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(currencyFormat.format(p.totalRevenue), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
                Column {
                    Text("DESPESAS ACC.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(currencyFormat.format(p.totalExpenses), fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("SALDO LÍQUIDO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        currencyFormat.format(p.netBalance),
                        fontWeight = FontWeight.ExtraBold,
                        color = if (p.netBalance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
        }
    }
}

@Composable
fun RouteReportCard(r: RouteReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(r.route, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    "${r.totalTrips} viagens",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("FATURAMENTO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(currencyFormat.format(r.totalRevenue), fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Média: " + currencyFormat.format(r.averageRevenue), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
                Column {
                    Text("DESPESAS DA ROTA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(currencyFormat.format(r.totalExpenses), fontWeight = FontWeight.SemiBold, color = Color(0xFFC62828))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("SALDO LÍQUIDO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text(
                        currencyFormat.format(r.netBalance),
                        fontWeight = FontWeight.ExtraBold,
                        color = if (r.netBalance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
        }
    }
}

// TAB 3: Search and Advanced Filters Tab
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiltersTabView(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedDriver: String,
    onDriverSelect: (String) -> Unit,
    driversList: List<String>,
    selectedRoute: String,
    onRouteSelect: (String) -> Unit,
    routesList: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    categoriesList: List<String>,
    selectedType: String,
    onTypeSelect: (String) -> Unit,
    dateFilterType: String,
    onDateFilterTypeSelect: (String) -> Unit,
    onClearFilters: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(14.dp)
            .testTag("filters_tab_view")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Pesquisa e Filtros Avançados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = onClearFilters,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error).run {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                },
                modifier = Modifier.testTag("clear_filters_button")
            ) {
                Text("Limpar Tudo", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Text Search
        Text("Palavra-Chave", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("filter_search_field"),
            placeholder = { Text("Pesquise por motorista, rota, categoria...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisa") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Type Filter (Receita / Despesa / Todas)
        Text("Tipo de Lançamento", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("TODOS", "RECEITAS", "DESPESAS").forEach { type ->
                val isSelected = selectedType == type
                OutlinedCard(
                    onClick = { onTypeSelect(type) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("filter_type_$type"),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when(type) {
                                "TODOS" -> "Todos"
                                "RECEITAS" -> "Receitas"
                                else -> "Despesas"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Date Period Filter
        Text("Período de Data", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Todos", "Hoje", "Esta Semana", "Este Mês").forEach { dateType ->
                val isSelected = dateFilterType == dateType
                FilterChip(
                    selected = isSelected,
                    onClick = { onDateFilterTypeSelect(dateType) },
                    label = { Text(dateType) },
                    modifier = Modifier.testTag("filter_date_$dateType")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Driver Selection
        Text("Filtrar por Motorista", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(6.dp))
        SpinnerSelector(
            currentValue = selectedDriver,
            items = listOf("Todos") + driversList.filter { it.isNotBlank() },
            onItemSelected = onDriverSelect,
            tagPrefix = "driver_filter"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Route Selection
        Text("Filtrar por Rota", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(6.dp))
        SpinnerSelector(
            currentValue = selectedRoute,
            items = listOf("Todas") + routesList.filter { it.isNotBlank() },
            onItemSelected = onRouteSelect,
            tagPrefix = "route_filter"
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Category Selection
        Text("Filtrar por Categoria", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(6.dp))
        SpinnerSelector(
            currentValue = selectedCategory,
            items = listOf("Todas") + categoriesList.filter { it.isNotBlank() },
            onItemSelected = onCategorySelect,
            tagPrefix = "category_filter"
        )

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// Custom Helper Dropdown Spinner Composable
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpinnerSelector(
    currentValue: String,
    items: List<String>,
    onItemSelected: (String) -> Unit,
    tagPrefix: String
) {
    var expanded by varOf(false)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = currentValue,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .testTag("spinner_$tagPrefix"),
            colors = OutlinedTextFieldDefaults.colors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEach { selection ->
                DropdownMenuItem(
                    text = { Text(text = selection) },
                    onClick = {
                        onItemSelected(selection)
                        expanded = false
                    },
                    modifier = Modifier.testTag("spinner_item_${tagPrefix}_${selection.replace(" ", "_").lowercase()}")
                )
            }
        }
    }
}

// FORM DIALOG Composable for Adding Transactions
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    drivers: List<String>,
    routes: List<String>,
    revenueCategories: List<String>,
    expenseCategories: List<String>,
    initialIsRevenue: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (isRevenue: Boolean, amount: Double, category: String, route: String, driverName: String, notes: String) -> Unit
) {
    var isRevenue by varOf(initialIsRevenue)
    var amountText by varOf("")
    var selectedCategory by varOf("")
    var selectedRoute by varOf("")
    var selectedDriver by varOf("")
    var notesText by varOf("")

    // Expanded states for dropdown selectors (which also allow custom input typing!)
    var driverDropdownExpanded by varOf(false)
    var routeDropdownExpanded by varOf(false)
    var categoryDropdownExpanded by varOf(false)

    var showErrorMsg by varOf("")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_transaction_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    text = "Novo Registro Financeiro",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Segmented control choice: Tab choice for Revenue vs Expense
                TabRow(
                    selectedTabIndex = if (isRevenue) 0 else 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = isRevenue,
                        onClick = { isRevenue = true },
                        text = { Text("Receita (Corrida)", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("form_tab_revenue")
                    )
                    Tab(
                        selected = !isRevenue,
                        onClick = { isRevenue = false },
                        text = { Text("Despesa (Gasto)", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("form_tab_expense")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount text input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val clean = input.replace(Regex("\\D"), "")
                        amountText = if (clean.isEmpty() || (clean.toLongOrNull() ?: 0L) == 0L) {
                            ""
                        } else {
                            val parsed = clean.toDoubleOrNull() ?: 0.0
                            val value = parsed / 100.0
                            val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("pt", "BR"))
                            format.format(value)
                        }
                    },
                    label = { Text("Valor *") },
                    placeholder = { Text("R$ 0,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_amount_field"),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Driver selector (Editable field with presets dropdown menu)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedDriver,
                        onValueChange = {
                            selectedDriver = it
                            driverDropdownExpanded = true
                        },
                        label = { Text("Motorista *") },
                        placeholder = { Text("Selecione ou digite um motorista...") },
                        trailingIcon = {
                            IconButton(onClick = { driverDropdownExpanded = !driverDropdownExpanded }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Mostrar motoristas")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_driver_field"),
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = driverDropdownExpanded,
                        onDismissRequest = { driverDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        drivers.filter { it.isNotBlank() }.forEach { drv ->
                            DropdownMenuItem(
                                text = { Text(drv) },
                                onClick = {
                                    selectedDriver = drv
                                    driverDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Route selector (Editable field with presets dropdown menu)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedRoute,
                        onValueChange = {
                            selectedRoute = it
                            routeDropdownExpanded = true
                        },
                        label = { Text("Rota / Trajeto *") },
                        placeholder = { Text("Selecione ou digite a rota...") },
                        trailingIcon = {
                            IconButton(onClick = { routeDropdownExpanded = !routeDropdownExpanded }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Mostrar rotas")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_route_field"),
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = routeDropdownExpanded,
                        onDismissRequest = { routeDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        routes.filter { it.isNotBlank() }.forEach { rt ->
                            DropdownMenuItem(
                                text = { Text(rt) },
                                onClick = {
                                    selectedRoute = rt
                                    routeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Selector (Editable field with presets dropdown based on active tab type)
                val categoryOptions = if (isRevenue) revenueCategories else expenseCategories
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {
                            selectedCategory = it
                            categoryDropdownExpanded = true
                        },
                        label = { Text("Categoria *") },
                        placeholder = { Text("Selecione ou digite a categoria...") },
                        trailingIcon = {
                            IconButton(onClick = { categoryDropdownExpanded = !categoryDropdownExpanded }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Mostrar categorias")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_category_field"),
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        categoryOptions.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes textField
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notas / Observações (opcional)") },
                    placeholder = { Text("Ex: Pix, Troco, reparo do radiador...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("form_notes_field"),
                    maxLines = 3
                )

                if (showErrorMsg.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = showErrorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel and Save actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("form_cancel_button")
                    ) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val digits = amountText.replace(Regex("\\D"), "")
                            val parsedAmount = if (digits.isNotEmpty()) digits.toDouble() / 100.0 else null
                            when {
                                parsedAmount == null || parsedAmount <= 0.0 -> {
                                    showErrorMsg = "Por favor, insira um valor numérico válido."
                                }
                                selectedDriver.isBlank() -> {
                                    showErrorMsg = "O preenchimento do Motorista é obrigatório."
                                }
                                selectedRoute.isBlank() -> {
                                    showErrorMsg = "O preenchimento da Rota é obrigatório."
                                }
                                selectedCategory.isBlank() -> {
                                    showErrorMsg = "A escolha de uma Categoria é obrigatória."
                                }
                                else -> {
                                    onSave(isRevenue, parsedAmount, selectedCategory, selectedRoute, selectedDriver, notesText)
                                }
                            }
                        },
                        modifier = Modifier.testTag("form_save_button")
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}

// FULL-SCREEN FORM Composable for Adding Transactions
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    isRevenue: Boolean,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (isRevenue: Boolean, amount: Double, timestamp: Long, category: String, route: String, driverName: String, notes: String) -> Unit
) {
    val context = LocalContext.current
    
    // States
    var amountText by varOf("")
    var notesText by varOf("")
    
    // Date state
    val calendar = remember { Calendar.getInstance() }
    var selectedTimestamp by varOf(calendar.timeInMillis)
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    
    // Categories dropdown selection
    var selectedCategory by varOf(categories.firstOrNull() ?: "")
    var categoryExpanded by varOf(false)
    
    var showErrorMsg by varOf("")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Elegant Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("btn_back_form")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = if (isRevenue) "Nova Receita" else "Nova Despesa",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Large Value Display field (Formatted as Brazilian Real R$)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRevenue) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "VALOR".uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = if (isRevenue) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Text Field nested elegantly
                        BasicTextField(
                            value = amountText,
                            onValueChange = { input ->
                                val clean = input.replace(Regex("\\D"), "")
                                amountText = if (clean.isEmpty() || (clean.toLongOrNull() ?: 0L) == 0L) {
                                    ""
                                } else {
                                    val parsed = clean.toDoubleOrNull() ?: 0.0
                                    val value = parsed / 100.0
                                    currencyFormat.format(value)
                                }
                            },
                            textStyle = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = if (isRevenue) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("form_screen_amount_field"),
                            decorationBox = { innerTextField ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (amountText.isEmpty()) {
                                        Text(
                                            text = "R$ 0,00",
                                            style = MaterialTheme.typography.headlineLarge.copy(
                                                fontSize = 42.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                color = (if (isRevenue) Color(0xFF1B5E20) else Color(0xFFB71C1C)).copy(alpha = 0.35f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
                
                // Form Fields Card container
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        
                        // Date picker input
                        Column {
                            Text(
                                text = "Data",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            val dateString = dateFormatter.format(Date(selectedTimestamp))
                            
                            OutlinedTextField(
                                value = dateString,
                                onValueChange = {},
                                readOnly = true,
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val dp = android.app.DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    calendar.set(Calendar.YEAR, year)
                                                    calendar.set(Calendar.MONTH, month)
                                                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                    selectedTimestamp = calendar.timeInMillis
                                                },
                                                calendar.get(Calendar.YEAR),
                                                calendar.get(Calendar.MONTH),
                                                calendar.get(Calendar.DAY_OF_MONTH)
                                            )
                                            dp.show()
                                        }
                                    ) {
                                        Icon(Icons.Default.DateRange, contentDescription = "Selecionar data")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val dp = android.app.DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                calendar.set(Calendar.YEAR, year)
                                                calendar.set(Calendar.MONTH, month)
                                                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                selectedTimestamp = calendar.timeInMillis
                                            },
                                            calendar.get(Calendar.YEAR),
                                            calendar.get(Calendar.MONTH),
                                            calendar.get(Calendar.DAY_OF_MONTH)
                                        )
                                        dp.show()
                                    }
                                    .testTag("form_screen_date_field")
                            )
                        }
                        
                        // Category Dropdown input
                        Column {
                            Text(
                                text = "Categoria",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            ExposedDropdownMenuBox(
                                expanded = categoryExpanded,
                                onExpandedChange = { categoryExpanded = !categoryExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                        .testTag("form_screen_category_field")
                                )
                                ExposedDropdownMenu(
                                    expanded = categoryExpanded,
                                    onDismissRequest = { categoryExpanded = false }
                                ) {
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat, fontWeight = FontWeight.Medium) },
                                            onClick = {
                                                selectedCategory = cat
                                                categoryExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Notes text input
                        Column {
                            Text(
                                text = "Observações",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            OutlinedTextField(
                                value = notesText,
                                onValueChange = { notesText = it },
                                placeholder = { Text("Ex: Pix, Troco, observações do dia...") },
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("form_screen_notes_field")
                            )
                        }
                    }
                }
                
                if (showErrorMsg.isNotBlank()) {
                    Text(
                        text = showErrorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
            
            // Cancel and Save buttons at bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("form_screen_cancel_btn")
                ) {
                    Text(
                        text = "Cancelar",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Button(
                    onClick = {
                        val digits = amountText.replace(Regex("\\D"), "")
                        val parsedAmount = if (digits.isNotEmpty()) digits.toDouble() / 100.0 else null
                        if (parsedAmount == null || parsedAmount <= 0.0) {
                            showErrorMsg = "Por favor, insira um valor numérico válido."
                        } else {
                            onSave(isRevenue, parsedAmount, selectedTimestamp, selectedCategory, "Geral", "Principal", notesText)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRevenue) Color(0xFF1B5E20) else Color(0xFFB71C1C),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("form_screen_save_btn")
                ) {
                    Text(
                        text = "Salvar",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryConfigScreen(
    isRevenue: Boolean,
    categories: List<String>,
    onAddCategory: (String) -> Unit,
    onRemoveCategory: (String) -> Unit,
    onBack: () -> Unit
) {
    var newCategoryText by varOf("")
    var showErrorMsg by varOf("")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Elegant Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("btn_back_config_categories")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Voltar",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = if (isRevenue) "Configuração de Receita" else "Configuração de Despesa",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Intro Text
                Text(
                    text = if (isRevenue) "Gerencie as categorias de receitas disponíveis para seleção." else "Gerencie as categorias de despesas disponíveis para seleção.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Input Field to add a new Category
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCategoryText,
                            onValueChange = { 
                                newCategoryText = it
                                showErrorMsg = ""
                            },
                            placeholder = { Text("Nova categoria... Ex: Pix") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("config_screen_new_category_input")
                        )

                        Button(
                            onClick = {
                                val trimmed = newCategoryText.trim()
                                if (trimmed.isBlank()) {
                                    showErrorMsg = "O nome da categoria não pode ser vazio."
                                } else if (categories.any { it.equals(trimmed, ignoreCase = true) }) {
                                    showErrorMsg = "Esta categoria já existe."
                                } else {
                                    onAddCategory(trimmed)
                                    newCategoryText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRevenue) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(56.dp)
                                .testTag("config_screen_add_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Adicionar")
                        }
                    }
                }

                if (showErrorMsg.isNotBlank()) {
                    Text(
                        text = showErrorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                // List of existing categories
                Text(
                    text = "Categorias Ativas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { category ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isRevenue) Color(0xFF1B5E20) else Color(0xFFB71C1C))
                                    )
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                IconButton(
                                    onClick = { onRemoveCategory(category) },
                                    modifier = Modifier.testTag("delete_category_$category")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Excluir categoria $category",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
