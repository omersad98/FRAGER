package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val balance: Double,
    val colorHex: String, // Hex string for unique visual color branding
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val price: Double,
    val iconName: String, // Reference name to standard Material Icons
    val stock: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchases")
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: Int,
    val productId: Int,
    val productName: String,
    val pricePaid: Double,
    val purchasedAt: Long = System.currentTimeMillis()
)
