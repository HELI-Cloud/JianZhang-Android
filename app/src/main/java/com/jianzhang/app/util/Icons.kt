package com.jianzhang.app.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.WineBar
import androidx.compose.ui.graphics.vector.ImageVector

/** Lucide 图标名 -> Material 图标（用于分类与设置页图标选择器） */
fun categoryIcon(name: String): ImageVector = when (name) {
    "utensils" -> Icons.Filled.Restaurant
    "coffee" -> Icons.Filled.Coffee
    "pizza" -> Icons.Filled.LocalPizza
    "wine" -> Icons.Filled.WineBar
    "cake" -> Icons.Filled.Cake
    "car" -> Icons.Filled.DirectionsCar
    "bus" -> Icons.Filled.DirectionsBus
    "plane" -> Icons.Filled.Flight
    "shopping-bag" -> Icons.Filled.ShoppingBag
    "shopping-cart" -> Icons.Filled.ShoppingCart
    "credit-card" -> Icons.Filled.CreditCard
    "shirt" -> Icons.Filled.Checkroom
    "home" -> Icons.Filled.Home
    "gamepad-2" -> Icons.Filled.SportsEsports
    "film" -> Icons.Filled.Movie
    "music" -> Icons.Filled.MusicNote
    "book" -> Icons.Filled.MenuBook
    "graduation-cap" -> Icons.Filled.School
    "heart-pulse" -> Icons.Filled.LocalHospital
    "stethoscope" -> Icons.Filled.MedicalServices
    "dumbbell" -> Icons.Filled.FitnessCenter
    "gift" -> Icons.Filled.CardGiftcard
    "briefcase" -> Icons.Filled.Work
    "medal" -> Icons.Filled.EmojiEvents
    "trending-up" -> Icons.Filled.TrendingUp
    "laptop" -> Icons.Filled.Laptop
    "piggy-bank" -> Icons.Filled.Savings
    "coins" -> Icons.Filled.Payments
    "banknote" -> Icons.Filled.AccountBalanceWallet
    "rocket" -> Icons.Filled.RocketLaunch
    "heart" -> Icons.Filled.Favorite
    "zap" -> Icons.Filled.Bolt
    "umbrella" -> Icons.Filled.BeachAccess
    "phone" -> Icons.Filled.Phone
    "wifi" -> Icons.Filled.Wifi
    else -> Icons.Filled.MoreHoriz
}

/** 分类图标选择器选项（与桌面版 ICONS 列表一致） */
val CATEGORY_ICON_NAMES = listOf(
    "utensils", "coffee", "pizza", "wine", "cake", "car", "bus", "plane",
    "shopping-bag", "shopping-cart", "credit-card", "shirt", "home", "gamepad-2",
    "film", "music", "book", "graduation-cap", "heart-pulse", "stethoscope",
    "dumbbell", "gift", "briefcase", "medal", "trending-up", "laptop", "piggy-bank",
    "coins", "banknote", "rocket", "heart", "zap", "umbrella", "phone", "wifi", "more-horizontal",
)