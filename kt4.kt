
data class User(val id: Int, val name: String, val age: Int)
data class Product(val id: Int, val name: String, val price: Int)
data class Purchase(val userId: Int, val productId: Int, val quantity: Int)


fun calculatePurchaseCost(purchase: Purchase, productMap: Map<Int, Product>): Int {
    val product = productMap[purchase.productId] ?: return 0
    return product.price * purchase.quantity
}


fun analyzeCustomerData(users: List<User>, products: List<Product>, purchases: List<Purchase>) {


    val productMap = products.associateBy { it.id }


    println("1. Список всех пользователей:")
    users.forEach { println(" - ${it.name} (ID: ${it.id}, Возраст: ${it.age})") }
    println()


    val ageThreshold = 30
    println("2. Пользователи старше $ageThreshold лет:")
    val usersOlderThan = users.filter { it.age > ageThreshold }
    if (usersOlderThan.isNotEmpty()) {
        usersOlderThan.forEach { println(" - ${it.name} (ID: ${it.id}, Возраст: ${it.age})") }
    } else {
        println(" - Пользователей старше $ageThreshold лет не найдено.")
    }
    println()


    println("3. Топ-3 самых дорогих товара:")
    val top3ExpensiveProducts = products
        .sortedByDescending { it.price }
        .take(3)
    if (top3ExpensiveProducts.isNotEmpty()) {
        top3ExpensiveProducts.forEach { println(" - ${it.name} (Цена: ${it.price})") }
    } else {
        println(" - Товары не найдены.")
    }
    println()



    val purchasesByUser = purchases.groupBy { it.userId }


    val userIdToCheck = 1
    val productIdToCheck = 101
    println("4. Пользователь с ID $userIdToCheck покупал товар с ID $productIdToCheck: ${
        purchasesByUser[userIdToCheck]?.any { it.productId == productIdToCheck } ?: false
    }")
    println()



    println("5. Сумма трат каждого пользователя:")
    val userSpending = users.associate { user ->
        user.id to purchases
            .filter { it.userId == user.id }
            .sumOf { calculatePurchaseCost(it, productMap) }
    }
    userSpending.forEach { (userId, totalSpent) ->
        val userName = users.find { it.id == userId }?.name ?: "Неизвестный пользователь"
        println(" - $userName (ID: $userId): $totalSpent")
    }
    println()


    println("6. Топ-1 покупателя по сумме трат:")
    val top1Customer = userSpending.maxByOrNull { it.value }
    if (top1Customer != null) {
        val userName = users.find { it.id == top1Customer.key }?.name ?: "Неизвестный пользователь"
        println(" - ${userName} (ID: ${top1Customer.key}) потратил: ${top1Customer.value}")
    } else {
        println(" - Данные о тратах не найдены.")
    }
    println()



    println("7. Результат использования generic-функции:")

    fun <T> findFirstOrNull(list: List<T>, predicate: (T) -> Boolean): T? {
        return list.firstOrNull(predicate)
    }


    val firstUserOlderThan25 = findFirstOrNull(users) { it.age > 25 }
    if (firstUserOlderThan25 != null) {
        println(" - Первый пользователь старше 25 лет: ${firstUserOlderThan25.name}")
    } else {
        println(" - Пользователи старше 25 лет не найдены.")
    }


    val firstExpensiveProduct = findFirstOrNull(products) { it.price > 50 }
    if (firstExpensiveProduct != null) {
        println(" - Первый продукт дороже 50: ${firstExpensiveProduct.name}")
    } else {
        println(" - Продукты дороже 50 не найдены.")
    }
}


fun main() {
    val users = listOf(
        User(1, "Алиса", 28),
        User(2, "Максим", 35),
        User(3, "Коля", 22),
        User(4, "Сережа", 40),
        User(5, "Саша", 29)
    )

    val products = listOf(
        Product(101, "Ноутбут", 1200),
        Product(102, "Мышка", 25),
        Product(103, "Клавиатура", 75),
        Product(104, "Монитор", 300),
        Product(105, "Камера", 50)
    )

    val purchases = listOf(
        Purchase(1, 101, 1),
        Purchase(1, 102, 2),
        Purchase(2, 103, 1),
        Purchase(3, 104, 1),
        Purchase(2, 101, 1),
        Purchase(4, 101, 1),
        Purchase(5, 102, 1),
        Purchase(1, 103, 1)
    )

    analyzeCustomerData(users, products, purchases)
}
