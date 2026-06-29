package com.example

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Account
import com.example.data.Product
import com.example.data.Purchase
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Converts hex color strings from database safely to Compose Color
fun String.toComposeColor(fallback: Color = Color(0xFF00639B)): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        fallback
    }
}

// Maps stored icon name strings to standard Material Icons
fun getProductIcon(iconName: String): ImageVector {
    return when (iconName) {
        "Watch" -> Icons.Default.Watch
        "Headphones" -> Icons.Default.Headphones
        "Keyboard" -> Icons.Default.Keyboard
        "Apparel", "Shirt" -> Icons.Default.Checkroom
        "Coffee" -> Icons.Default.Coffee
        "Ticket" -> Icons.Default.ConfirmationNumber
        "Book" -> Icons.Default.Book
        "Gamepad" -> Icons.Default.SportsEsports
        "Gift" -> Icons.Default.CardGiftcard
        else -> Icons.Default.ShoppingBag
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDashboard(
    viewModel: ShopViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val activeAccount by viewModel.activeAccount.collectAsStateWithLifecycle()
    val activeAccountPurchases by viewModel.activeAccountPurchases.collectAsStateWithLifecycle()
    val purchases by viewModel.purchases.collectAsStateWithLifecycle()

    // Current selected tab: "Home", "Wallet", "Stock", "Config"
    var currentTab by remember { mutableStateOf("Home") }

    // Dialog flags
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showDepositDialog by remember { mutableStateOf(false) }
    
    // Quick buy confirmation
    var showConfirmBuyDialog by remember { mutableStateOf(false) }
    var selectedProductToBuy by remember { mutableStateOf<Product?>(null) }

    // Listen for toast messages from the ViewModel
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collectLatest { event ->
            when (event) {
                is ToastMessage.Success -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is ToastMessage.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // High Density Material 3 bottom navigation bar
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = Color(0xFFF0F4FA),
                tonalElevation = 0.dp
            ) {
                val tabs = listOf(
                    Triple("Home", Icons.Default.GridView, "Home"),
                    Triple("Wallet", Icons.Default.AccountBalanceWallet, "Wallet"),
                    Triple("Stock", Icons.Default.Inventory2, "Stock"),
                    Triple("Config", Icons.Default.Settings, "Config")
                )
                tabs.forEach { (tabName, icon, label) ->
                    val isSelected = currentTab == tabName
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tabName },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF001D36),
                            selectedTextColor = Color(0xFF001D36),
                            indicatorColor = Color(0xFFD3E4FF),
                            unselectedIconColor = Color(0xFF191C1E).copy(alpha = 0.6f),
                            unselectedTextColor = Color(0xFF191C1E).copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.testTag("nav_tab_${tabName.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF7F9FF)) // High Density background color
        ) {
            // --- Top App Bar / Header ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDDE2F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = "Market Manager Icon",
                            tint = Color(0xFF43474E),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Market Manager",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF191C1E)
                        )
                        Text(
                            text = "Store Dashboard v2.4",
                            fontSize = 12.sp,
                            color = Color(0xFF43474E).copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                IconButton(
                    onClick = {
                        Toast.makeText(context, "System operations normal. Secure connection active.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f))
                        .testTag("notification_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color(0xFF191C1E)
                    )
                }
            }

            // --- Main Content Views ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentTab) {
                    "Home" -> HomeScreen(
                        accounts = accounts,
                        products = products,
                        purchases = purchases,
                        activeAccount = activeAccount,
                        onAddMoneyClick = {
                            if (activeAccount != null) {
                                showDepositDialog = true
                            } else {
                                Toast.makeText(context, "Please create a card/account first!", Toast.LENGTH_SHORT).show()
                                showAddAccountDialog = true
                            }
                        },
                        onSelectAccount = { id -> viewModel.selectAccount(id) },
                        onAddProductClick = { showAddProductDialog = true },
                        onProductClick = { product ->
                            selectedProductToBuy = product
                            showConfirmBuyDialog = true
                        },
                        onNavigateToWallet = { currentTab = "Wallet" }
                    )

                    "Wallet" -> WalletScreen(
                        accounts = accounts,
                        activeAccount = activeAccount,
                        activeAccountPurchases = activeAccountPurchases,
                        onSelectAccount = { id -> viewModel.selectAccount(id) },
                        onAddCardClick = { showAddAccountDialog = true },
                        onDepositClick = { showDepositDialog = true },
                        onDeleteAccount = { acc -> viewModel.deleteAccount(acc) }
                    )

                    "Stock" -> StockScreen(
                        products = products,
                        onAddProductClick = { showAddProductDialog = true },
                        onDeleteProduct = { prod -> viewModel.deleteProduct(prod) },
                        onRestockProduct = { prod -> viewModel.restockProduct(prod) }
                    )

                    "Config" -> ConfigScreen(
                        onResetAndSeed = {
                            viewModel.resetAndSeed()
                        }
                    )
                }
            }
        }
    }

    // --- DIALOGS FOR ACCOUNT & PRODUCT ACTIONS ---

    // 1. Confirm Purchase Dialog
    if (showConfirmBuyDialog && selectedProductToBuy != null) {
        val product = selectedProductToBuy!!
        val activeAcc = activeAccount

        AlertDialog(
            onDismissRequest = { showConfirmBuyDialog = false },
            title = { Text("Confirm Purchase", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Are you sure you want to purchase:")
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDDE2F1)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getProductIcon(product.iconName),
                                    contentDescription = "Icon",
                                    tint = Color(0xFF191C1E)
                                )
                            }
                            Column {
                                Text(text = product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(text = "Price: $${String.format("%.2f", product.price)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (activeAcc != null) {
                        Text(
                            text = "Payment card: ${activeAcc.name} (Balance: $${String.format("%.2f", activeAcc.balance)})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF43474E)
                        )
                    } else {
                        Text(
                            text = "Warning: No active card selected. Please create a wallet card first.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.buyProduct(product.id)
                        showConfirmBuyDialog = false
                    },
                    enabled = activeAcc != null && product.stock > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00639B)),
                    modifier = Modifier.testTag("buy_product_${product.id}")
                ) {
                    Text("Buy Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmBuyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 2. Add Wallet / Card Dialog
    if (showAddAccountDialog) {
        var accountName by remember { mutableStateOf("") }
        var startBalance by remember { mutableStateOf("") }
        var selectedCardColorHex by remember { mutableStateOf("#4F46E5") }

        val palette = listOf(
            "#4F46E5", // Indigo
            "#0EA5E9", // Sky Blue
            "#10B981", // Emerald Mint
            "#F43F5E", // Rose Pink
            "#8B5CF6", // Purple Violet
            "#F59E0B"  // Amber Orange
        )

        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("Create Wallet Card", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = { accountName = it },
                        label = { Text("Card/Account Name") },
                        placeholder = { Text("e.g., Personal Wallet") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_account_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = startBalance,
                        onValueChange = { startBalance = it },
                        label = { Text("Initial Money Balance ($)") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_account_balance_input"),
                        singleLine = true
                    )

                    // Card Color Picker Label
                    Text(
                        text = "Card Theme Style",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF43474E)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        palette.forEach { colorStr ->
                            val parsed = colorStr.toComposeColor()
                            val isSelectedColor = selectedCardColorHex == colorStr

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(parsed)
                                    .border(
                                        width = if (isSelectedColor) 3.dp else 1.dp,
                                        color = if (isSelectedColor) Color(0xFF00639B) else Color.White.copy(
                                            alpha = 0.5f
                                        ),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedCardColorHex = colorStr }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedBalance = startBalance.toDoubleOrNull() ?: 0.0
                        viewModel.createAccount(accountName, parsedBalance, selectedCardColorHex)
                        showAddAccountDialog = false
                    },
                    modifier = Modifier.testTag("confirm_create_account")
                ) {
                    Text("Create Card")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 3. Deposit Credits / Add Money Dialog
    if (showDepositDialog && activeAccount != null) {
        var depositAmount by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDepositDialog = false },
            title = { Text("Deposit Credits", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Add digital credits directly to your active card: '${activeAccount?.name}'",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF43474E)
                    )
                    OutlinedTextField(
                        value = depositAmount,
                        onValueChange = { depositAmount = it },
                        label = { Text("Credit Amount ($)") },
                        placeholder = { Text("100.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deposit_amount_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedAmt = depositAmount.toDoubleOrNull() ?: 0.0
                        viewModel.addMoney(activeAccount?.id ?: 0, parsedAmt)
                        showDepositDialog = false
                    },
                    modifier = Modifier.testTag("confirm_deposit_button")
                ) {
                    Text("Deposit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDepositDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // 4. Add Custom Product / Stuff Dialog
    if (showAddProductDialog) {
        var prodName by remember { mutableStateOf("") }
        var prodDesc by remember { mutableStateOf("") }
        var prodPrice by remember { mutableStateOf("") }
        var prodStock by remember { mutableStateOf("") }
        var selectedIconName by remember { mutableStateOf("ShoppingBag") }

        val iconOptions = listOf(
            "ShoppingBag",
            "Watch",
            "Headphones",
            "Keyboard",
            "Coffee",
            "Ticket",
            "Book",
            "Gamepad",
            "Gift"
        )

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("Add Stuff to Shop", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = prodName,
                        onValueChange = { prodName = it },
                        label = { Text("Product Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_product_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = prodDesc,
                        onValueChange = { prodDesc = it },
                        label = { Text("Product Description") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_product_desc_input"),
                        maxLines = 3
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = prodPrice,
                            onValueChange = { prodPrice = it },
                            label = { Text("Price ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_product_price_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = prodStock,
                            onValueChange = { prodStock = it },
                            label = { Text("Stock Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_product_stock_input"),
                            singleLine = true
                        )
                    }

                    // Product Icon Option picker
                    Text(
                        text = "Product Category Icon",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF43474E)
                    )

                    // Row showing icons to pick
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        iconOptions.forEach { iconName ->
                            val isSelected = selectedIconName == iconName

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) Color(0xFFD3E4FF)
                                        else Color(0xFFDDE2F1)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) Color(0xFF00639B) else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedIconName = iconName },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getProductIcon(iconName),
                                    contentDescription = iconName,
                                    tint = if (isSelected) Color(0xFF001D36) else Color(0xFF43474E),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = prodPrice.toDoubleOrNull() ?: 0.0
                        val stock = prodStock.toIntOrNull() ?: 1
                        viewModel.createProduct(prodName, prodDesc, price, selectedIconName, stock)
                        showAddProductDialog = false
                    },
                    modifier = Modifier.testTag("confirm_create_product")
                ) {
                    Text("Add Product")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// --- TAB 1: HOME DASHBOARD SCREEN ---
@Composable
fun HomeScreen(
    accounts: List<Account>,
    products: List<Product>,
    purchases: List<Purchase>,
    activeAccount: Account?,
    onAddMoneyClick: () -> Unit,
    onSelectAccount: (Int) -> Unit,
    onAddProductClick: () -> Unit,
    onProductClick: (Product) -> Unit,
    onNavigateToWallet: () -> Unit
) {
    val totalCapital = accounts.sumOf { it.balance }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Quick Wallet Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick_wallet_section"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFDDE2F1)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "TOTAL CAPITAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF43474E),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$${String.format("%,.2f", totalCapital)}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF191C1E)
                        )
                    }
                    Button(
                        onClick = onAddMoneyClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00639B)),
                        shape = RoundedCornerShape(100.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("add_money_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add money",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Add Money", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Stat 1 Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE2F0D9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Trending Up",
                                    tint = Color(0xFF385723),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "+14% this week",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF191C1E),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Stat 2 Box
                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFD9E2F3)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync",
                                    tint = Color(0xFF1F4E79),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "${purchases.size} Trans.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF191C1E),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 2. Linked Accounts
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Accounts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF43474E)
                )
                TextButton(
                    onClick = onNavigateToWallet,
                    modifier = Modifier.testTag("manage_accounts_trigger")
                ) {
                    Text(text = "Manage", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00639B))
                }
            }

            if (accounts.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFDDE2F1))
                ) {
                    Text(
                        text = "No active cards. Create one under Wallet!",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        fontSize = 12.sp,
                        color = Color(0xFF43474E),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Two-column High Density Account Grid (max 4 accounts)
                val displayAccounts = accounts.take(4)
                displayAccounts.chunked(2).forEach { rowAccounts ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowAccounts.forEach { account ->
                            val isSelected = activeAccount?.id == account.id
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectAccount(account.id) }
                                    .testTag("wallet_chip_${account.id}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF00639B) else Color(0xFFDDE2F1)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF0F4FA)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (account.colorHex.contains("0EA5E9")) Icons.Default.CreditCard else Icons.Default.AccountBalance,
                                            contentDescription = "Icon",
                                            tint = if (isSelected) Color(0xFF00639B) else Color(0xFF43474E),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = account.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF191C1E),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$${String.format("%.2f", account.balance)}",
                                            fontSize = 10.sp,
                                            color = Color(0xFF43474E),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                        if (rowAccounts.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 3. Shop Inventory
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shop Inventory",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF43474E)
                )
                Button(
                    onClick = onAddProductClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00639B).copy(alpha = 0.1f),
                        contentColor = Color(0xFF00639B)
                    ),
                    shape = RoundedCornerShape(100.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("create_product_trigger")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Item",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Add Item", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (products.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFDDE2F1))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "No products",
                            tint = Color(0xFF43474E).copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No Products in DB",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF191C1E)
                        )
                    }
                }
            } else {
                // Two-column High Density Product Grid
                products.chunked(2).forEach { rowProducts ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowProducts.forEach { product ->
                            val isOutOfStock = product.stock <= 0
                            val isLowStock = product.stock in 1..2

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onProductClick(product) }
                                    .testTag("product_item_${product.id}"),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFDDE2F1)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFF0F4FA)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getProductIcon(product.iconName),
                                                contentDescription = "Icon",
                                                tint = Color(0xFF43474E),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Stock levels badge
                                        val (badgeText, badgeBg, badgeTextClr) = when {
                                            isOutOfStock -> Triple("OUT", Color(0xFFFFDAD7), Color(0xFFBA1A1A))
                                            isLowStock -> Triple("LOW", Color(0xFFFFE0B2), Color(0xFFE65100))
                                            else -> Triple("IN STOCK", Color(0xFFD1F2D9), Color(0xFF0F5132))
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(100.dp))
                                                .background(badgeBg)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = badgeText,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeTextClr
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF191C1E),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Qty: ${product.stock}",
                                            fontSize = 9.sp,
                                            color = Color(0xFF43474E).copy(alpha = 0.7f),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "$${String.format("%.2f", product.price)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF191C1E)
                                        )
                                    }
                                }
                            }
                        }
                        if (rowProducts.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 2: DETAILED WALLET SCREEN ---
@Composable
fun WalletScreen(
    accounts: List<Account>,
    activeAccount: Account?,
    activeAccountPurchases: List<Purchase>,
    onSelectAccount: (Int) -> Unit,
    onAddCardClick: () -> Unit,
    onDepositClick: () -> Unit,
    onDeleteAccount: (Account) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Credit Card UI Card
        item {
            Text(
                text = "ACTIVE CARD DETAILS",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color(0xFF00639B),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (activeAccount != null) {
                val cardColor = activeAccount.colorHex.toComposeColor()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("active_wallet_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeAccount.name.uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.2.sp,
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Contactless,
                                contentDescription = "Contactless payment active",
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        Text(
                            text = "AVAILABLE BALANCE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "$${String.format("%.2f", activeAccount.balance)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.testTag("wallet_balance_text")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = onDepositClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = cardColor
                                ),
                                shape = RoundedCornerShape(100.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("add_money_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddCard,
                                    contentDescription = "Add credits",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Deposit", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            IconButton(
                                onClick = { onDeleteAccount(activeAccount) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .testTag("delete_wallet_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Close Card",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFDAD7)),
                    border = BorderStroke(1.dp, Color(0xFFBA1A1A))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFBA1A1A)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "No Active Cards", fontWeight = FontWeight.Bold, color = Color(0xFFBA1A1A))
                        Text(
                            text = "Create a card using the 'Add Card' button below to make payments.",
                            fontSize = 11.sp,
                            color = Color(0xFFBA1A1A).copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Available Cards Carousel Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WALLETS & CARDS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF43474E),
                    letterSpacing = 1.sp
                )

                TextButton(
                    onClick = onAddCardClick,
                    modifier = Modifier.testTag("create_wallet_trigger")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircleOutline,
                        contentDescription = "New Wallet",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Card", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00639B))
                }
            }
        }

        // List of all configured accounts
        items(accounts) { account ->
            val isSelected = activeAccount?.id == account.id
            val cardColor = account.colorHex.toComposeColor()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectAccount(account.id) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) cardColor else Color(0xFFDDE2F1)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(cardColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = "Card Icon",
                                tint = cardColor
                            )
                        }
                        Column {
                            Text(text = account.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = "ID: #${account.id} • Balance Secured",
                                fontSize = 11.sp,
                                color = Color(0xFF43474E).copy(alpha = 0.7f)
                            )
                        }
                    }

                    Text(
                        text = "$${String.format("%.2f", account.balance)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color(0xFF191C1E)
                    )
                }
            }
        }

        // Active Account Purchased Receipts / Transactions
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "OWNED STUFF & TRANSACTIONS",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color(0xFF43474E),
                letterSpacing = 1.sp
            )
        }

        if (activeAccountPurchases.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFDDE2F1))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = "No receipts",
                            tint = Color(0xFF43474E).copy(alpha = 0.35f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "No Purchases Yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF191C1E)
                        )
                    }
                }
            }
        } else {
            items(activeAccountPurchases) { purchase ->
                val formatter = remember { SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()) }
                val formattedDate = remember(purchase.purchasedAt) {
                    formatter.format(Date(purchase.purchasedAt))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFDDE2F1)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF0F4FA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = "Receipt Icon",
                                    tint = Color(0xFF43474E),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = purchase.productName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF191C1E)
                                )
                                Text(
                                    text = formattedDate,
                                    fontSize = 10.sp,
                                    color = Color(0xFF43474E).copy(alpha = 0.7f)
                                )
                            }
                        }

                        Text(
                            text = "-$${String.format("%.2f", purchase.pricePaid)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFFBA1A1A)
                        )
                    }
                }
            }
        }
    }
}

// --- TAB 3: STOCK MANAGEMENT SCREEN ---
@Composable
fun StockScreen(
    products: List<Product>,
    onAddProductClick: () -> Unit,
    onDeleteProduct: (Product) -> Unit,
    onRestockProduct: (Product) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SHOP PRODUCTS MANAGER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF43474E),
                    letterSpacing = 1.sp
                )

                Button(
                    onClick = onAddProductClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00639B)),
                    shape = RoundedCornerShape(100.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("create_product_trigger")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Product",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Stuff", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (products.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFDDE2F1))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = "No Products",
                            tint = Color(0xFF43474E).copy(alpha = 0.4f),
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "The Database is Empty", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            items(products) { product ->
                val isOutOfStock = product.stock <= 0
                val isLowStock = product.stock in 1..2

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_item_${product.id}"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFDDE2F1)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF0F4FA)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getProductIcon(product.iconName),
                                        contentDescription = "Product Icon",
                                        tint = Color(0xFF43474E)
                                    )
                                }

                                Column {
                                    Text(
                                        text = product.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFF191C1E)
                                    )
                                    Text(
                                        text = "$${String.format("%.2f", product.price)} • ${product.stock} in Stock",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = Color(0xFF00639B)
                                    )
                                }
                            }

                            // Small level badge
                            val (badgeText, badgeBg, badgeTextClr) = when {
                                isOutOfStock -> Triple("OUT", Color(0xFFFFDAD7), Color(0xFFBA1A1A))
                                isLowStock -> Triple("LOW", Color(0xFFFFE0B2), Color(0xFFE65100))
                                else -> Triple("IN STOCK", Color(0xFFD1F2D9), Color(0xFF0F5132))
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeTextClr
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = product.description,
                            fontSize = 11.sp,
                            color = Color(0xFF43474E),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onRestockProduct(product) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00639B).copy(alpha = 0.08f),
                                    contentColor = Color(0xFF00639B)
                                ),
                                shape = RoundedCornerShape(100.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Restock",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Restock +10", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = { onDeleteProduct(product) },
                                modifier = Modifier.testTag("delete_product_${product.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete product",
                                    tint = Color(0xFFBA1A1A).copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- TAB 4: SYSTEM CONFIG SCREEN ---
@Composable
fun ConfigScreen(
    onResetAndSeed: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFFDDE2F1)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Config Icon",
                tint = Color(0xFF00639B),
                modifier = Modifier.size(36.dp)
            )
        }

        Text(
            text = "Market Manager v2.4",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF191C1E)
        )

        Text(
            text = "High Density Design System Theme enabled. Designed for retail analytics and rapid multi-account wallet simulations.",
            fontSize = 12.sp,
            color = Color(0xFF43474E),
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFDDE2F1)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "DEVELOPER TOOLS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF00639B),
                    letterSpacing = 1.sp
                )

                Text(
                    text = "If you want to quickly test the application with real preset cards (Personal, Business, Fun Money) and preset digital items, use the seed tool below. It clears the database and loads pristine mock data.",
                    fontSize = 11.sp,
                    color = Color(0xFF43474E),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onResetAndSeed,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00639B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_db_seed_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsBackupRestore,
                        contentDescription = "Restore Defaults"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Reset & Seed Preset Data", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Database Secured via Room SQLite • AES-256 Mock Simulation",
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF43474E).copy(alpha = 0.5f)
        )
    }
}
