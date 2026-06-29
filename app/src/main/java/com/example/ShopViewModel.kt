package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModel(private val repository: ShopRepository) : ViewModel() {

    // Active Selected Account ID. If null, we will select the first available account automatically
    private val _activeAccountId = MutableStateFlow<Int?>(null)
    val activeAccountId: StateFlow<Int?> = _activeAccountId.asStateFlow()

    val accounts: StateFlow<List<Account>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<Purchase>> = repository.allPurchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active selected Account details, falling back to the first account in the list if none is selected
    val activeAccount: StateFlow<Account?> = combine(accounts, _activeAccountId) { accountList, selectedId ->
        if (selectedId != null) {
            accountList.find { it.id == selectedId }
        } else {
            accountList.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Purchases made specifically by the active selected account
    val activeAccountPurchases: StateFlow<List<Purchase>> = activeAccount
        .flatMapLatest { activeAcc ->
            if (activeAcc != null) {
                repository.getPurchasesForAccount(activeAcc.id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Feedback messages
    private val _toastEvent = MutableSharedFlow<ToastMessage>()
    val toastEvent: SharedFlow<ToastMessage> = _toastEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.prepopulateIfEmpty()
            // Try to set the initial active account to the first one available
            val list = repository.allAccounts.first()
            if (list.isNotEmpty() && _activeAccountId.value == null) {
                _activeAccountId.value = list.first().id
            }
        }
    }

    fun selectAccount(accountId: Int) {
        _activeAccountId.value = accountId
    }

    fun createAccount(name: String, initialBalance: Double, colorHex: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _toastEvent.emit(ToastMessage.Error("Account name cannot be empty"))
                return@launch
            }
            if (initialBalance < 0) {
                _toastEvent.emit(ToastMessage.Error("Starting balance cannot be negative"))
                return@launch
            }
            val id = repository.insertAccount(
                Account(
                    name = name.trim(),
                    balance = initialBalance,
                    colorHex = colorHex
                )
            )
            // Automatically select the newly created account
            _activeAccountId.value = id.toInt()
            _toastEvent.emit(ToastMessage.Success("Account '$name' created!"))
        }
    }

    fun addMoney(accountId: Int, amount: Double) {
        viewModelScope.launch {
            if (amount <= 0) {
                _toastEvent.emit(ToastMessage.Error("Deposit amount must be positive"))
                return@launch
            }
            val account = repository.getAccount(accountId)
            if (account != null) {
                val updated = account.copy(balance = account.balance + amount)
                repository.updateAccount(updated)
                _toastEvent.emit(ToastMessage.Success("Deposited $${String.format("%.2f", amount)} into '${account.name}'"))
            } else {
                _toastEvent.emit(ToastMessage.Error("Account not found"))
            }
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            _toastEvent.emit(ToastMessage.Success("Account '${account.name}' deleted"))
            if (_activeAccountId.value == account.id) {
                _activeAccountId.value = null
            }
        }
    }

    fun createProduct(name: String, description: String, price: Double, iconName: String, stock: Int) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _toastEvent.emit(ToastMessage.Error("Product name cannot be empty"))
                return@launch
            }
            if (price < 0) {
                _toastEvent.emit(ToastMessage.Error("Price cannot be negative"))
                return@launch
            }
            if (stock < 0) {
                _toastEvent.emit(ToastMessage.Error("Stock cannot be negative"))
                return@launch
            }
            repository.insertProduct(
                Product(
                    name = name.trim(),
                    description = description.trim(),
                    price = price,
                    iconName = iconName,
                    stock = stock
                )
            )
            _toastEvent.emit(ToastMessage.Success("Product '$name' added to the shop!"))
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _toastEvent.emit(ToastMessage.Success("Product '${product.name}' deleted"))
        }
    }

    fun restockProduct(product: Product, quantity: Int = 10) {
        viewModelScope.launch {
            val updated = product.copy(stock = product.stock + quantity)
            repository.updateProduct(updated)
            _toastEvent.emit(ToastMessage.Success("Restocked '${product.name}' with $quantity units!"))
        }
    }

    fun resetAndSeed() {
        viewModelScope.launch {
            repository.clearAllAndPrepopulate()
            _toastEvent.emit(ToastMessage.Success("Database reset to High Density presets!"))
            // Auto select the first account
            val list = repository.allAccounts.first()
            if (list.isNotEmpty()) {
                _activeAccountId.value = list.first().id
            } else {
                _activeAccountId.value = null
            }
        }
    }

    fun buyProduct(productId: Int) {
        viewModelScope.launch {
            val activeAcc = activeAccount.value
            if (activeAcc == null) {
                _toastEvent.emit(ToastMessage.Error("Create and select an account first!"))
                return@launch
            }
            when (val result = repository.buyProduct(activeAcc.id, productId)) {
                is PurchaseResult.Success -> {
                    _toastEvent.emit(ToastMessage.Success("Purchased '${result.purchase.productName}'!"))
                }
                is PurchaseResult.Error -> {
                    _toastEvent.emit(ToastMessage.Error(result.message))
                }
            }
        }
    }
}

sealed interface ToastMessage {
    data class Success(val message: String) : ToastMessage
    data class Error(val message: String) : ToastMessage
}

class ShopViewModelFactory(private val repository: ShopRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShopViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShopViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
