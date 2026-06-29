package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ShopRepository(private val shopDao: ShopDao) {

    val allAccounts: Flow<List<Account>> = shopDao.getAllAccounts()
    val allProducts: Flow<List<Product>> = shopDao.getAllProducts()
    val allPurchases: Flow<List<Purchase>> = shopDao.getAllPurchases()

    fun getPurchasesForAccount(accountId: Int): Flow<List<Purchase>> =
        shopDao.getPurchasesByAccountId(accountId)

    suspend fun insertAccount(account: Account): Long = shopDao.insertAccount(account)
    suspend fun updateAccount(account: Account) = shopDao.updateAccount(account)
    suspend fun deleteAccount(account: Account) = shopDao.deleteAccount(account)

    suspend fun insertProduct(product: Product): Long = shopDao.insertProduct(product)
    suspend fun updateProduct(product: Product) = shopDao.updateProduct(product)
    suspend fun deleteProduct(product: Product) = shopDao.deleteProduct(product)

    suspend fun getAccount(id: Int): Account? = shopDao.getAccount(id)

    // Safely handles buying products, updating wallet balance and stock
    suspend fun buyProduct(accountId: Int, productId: Int): PurchaseResult {
        val account = shopDao.getAccount(accountId) ?: return PurchaseResult.Error("Account not found")
        val product = shopDao.getProductById(productId) ?: return PurchaseResult.Error("Product not found")

        if (product.stock <= 0) {
            return PurchaseResult.Error("Sorry, '${product.name}' is out of stock!")
        }

        if (account.balance < product.price) {
            return PurchaseResult.Error("Insufficient money in '${account.name}'!")
        }

        // Deduct money and save
        val updatedAccount = account.copy(balance = account.balance - product.price)
        shopDao.updateAccount(updatedAccount)

        // Decrement stock and save
        val updatedProduct = product.copy(stock = product.stock - 1)
        shopDao.updateProduct(updatedProduct)

        // Record purchase
        val purchase = Purchase(
            accountId = accountId,
            productId = productId,
            productName = product.name,
            pricePaid = product.price
        )
        shopDao.insertPurchase(purchase)

        return PurchaseResult.Success(purchase)
    }

    // Seed realistic products and initial accounts if the DB is blank
    suspend fun prepopulateIfEmpty() {
        val currentAccounts = shopDao.getAllAccounts().first()
        val currentProducts = shopDao.getAllProducts().first()

        if (currentAccounts.isEmpty()) {
            shopDao.insertAccount(
                Account(
                    name = "Personal Card",
                    balance = 650.00,
                    colorHex = "#4F46E5" // Modern Indigo
                )
            )
            shopDao.insertAccount(
                Account(
                    name = "Business Card",
                    balance = 2500.00,
                    colorHex = "#0EA5E9" // Modern Sky Blue
                )
            )
            shopDao.insertAccount(
                Account(
                    name = "Fun Money",
                    balance = 120.00,
                    colorHex = "#10B981" // Modern Emerald Mint
                )
            )
        }

        if (currentProducts.isEmpty()) {
            val defaultProducts = listOf(
                Product(
                    name = "Cyberpunk Neo Watch",
                    description = "Futuristic smartwatch featuring a customizable holographic interface, advanced bio-sensors, and customizable neon glows.",
                    price = 299.99,
                    iconName = "Watch",
                    stock = 5
                ),
                Product(
                    name = "Aero Hifi Headphones",
                    description = "Studio grade over-ear wireless headphones with top-tier active noise cancellation and spatial audio immersion.",
                    price = 189.50,
                    iconName = "Headphones",
                    stock = 12
                ),
                Product(
                    name = "Mechanical RGB Keyboard",
                    description = "Low profile aluminum gaming mechanical keyboard with hyper-responsive tactile switches and keycap custom design.",
                    price = 129.00,
                    iconName = "Keyboard",
                    stock = 8
                ),
                Product(
                    name = "Warm Premium Hoodie",
                    description = "Soft premium 450GSM cotton weave streetwear hoodie designed for ultimate styling and thick thermal insulation.",
                    price = 79.99,
                    iconName = "Apparel",
                    stock = 15
                ),
                Product(
                    name = "Precision Espresso Maker",
                    description = "Professional grade semi-automatic espresso extraction machine with manual pressure valve and steam wand.",
                    price = 450.00,
                    iconName = "Coffee",
                    stock = 3
                ),
                Product(
                    name = "Cosmic Golden Ticket",
                    description = "Exclusive digital golden certificate. The ultimate showpiece and status symbol for wealthy collectors.",
                    price = 9999.99,
                    iconName = "Ticket",
                    stock = 1
                )
            )
            for (p in defaultProducts) {
                shopDao.insertProduct(p)
            }
        }
    }

    suspend fun clearAllAndPrepopulate() {
        shopDao.deleteAllAccounts()
        shopDao.deleteAllProducts()
        shopDao.deleteAllPurchases()
        prepopulateIfEmpty()
    }
}

sealed interface PurchaseResult {
    data class Success(val purchase: Purchase) : PurchaseResult
    data class Error(val message: String) : PurchaseResult
}
