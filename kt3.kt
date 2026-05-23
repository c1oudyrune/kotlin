
package models


enum class OrderStatus {
    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED
}


abstract class User(val userId: String, val name: String) {

    abstract fun displayInfo()
}


class Customer(
    userId: String,
    name: String,
    val email: String
) : User(userId, name) {


    override fun displayInfo() {
        println("Пользовательский ID: $userId, Ник: $name, Почта: $email")
    }
}


class Admin(
    userId: String,
    name: String
) : User(userId, name) {


    override fun displayInfo() {
        println("Адмнинский ID: $userId, Ник: $name")
    }
}


data class Product(val productId: String, val name: String, val price: Double)


interface OrderItem {
    val product: Product
    val quantity: Int
    fun getTotalPrice(): Double
}


class OrderItemImpl(
    override val product: Product,
    override val quantity: Int
) : OrderItem {


    override fun getTotalPrice(): Double = product.price * quantity
}


sealed class OrderResult {
    data class Success(val message: String) : OrderResult()
    data class Error(val errorMessage: String) : OrderResult()
}


class Order(
    val orderId: String,
    val customer: Customer,
    private val items: MutableList<OrderItemImpl> = mutableListOf(),
    var status: OrderStatus = OrderStatus.PENDING
) {


    fun addItem(product: Product, quantity: Int): OrderResult {
        if (quantity <= 0) {
            return OrderResult.Error("Количество должно быть положительным.")
        }

        val existingItem = items.find { it.product.productId == product.productId }
        return if (existingItem != null) {

            val index = items.indexOf(existingItem)
            items[index] = OrderItemImpl(product, existingItem.quantity + quantity)
            OrderResult.Success("Количество товара обновлено ${product.name}.")
        } else {

            items.add(OrderItemImpl(product, quantity))
            OrderResult.Success("Продукт ${product.name} добавлено в заказ.")
        }
    }


    fun removeItem(productId: String): OrderResult {
        val removed = items.removeIf { it.product.productId == productId }
        return if (removed) {
            OrderResult.Success("Товар с ID $productId удалено из заказа.")
        } else {
            OrderResult.Error("Товар с ID $productId не найдено в порядке.")
        }
    }


    fun getTotalPrice(): Double {
        return items.sumOf { it.getTotalPrice() }
    }


    fun displayOrderDetails() {
        println("--- Детали заказа ---")
        println("Заказ ID: $orderId")
        customer.displayInfo()
        println("Статус: $status")
        if (items.isEmpty()) {
            println("В заказе нет товаров.")
        } else {
            println("Предметы:")
            items.forEach { item ->
                println("- ${item.product.name} (x${item.quantity}): $${item.getTotalPrice()}")
            }
            println("Тотальная цена: $${getTotalPrice()}")
        }
        println("---------------------")
    }


    fun updateStatus(newStatus: OrderStatus): OrderResult {

        this.status = newStatus
        return OrderResult.Success("Статус заказа обновлен $newStatus.")
    }
}


fun main() {

    val laptop = Product("P001", "Ноутбук", 1200.00)
    val mouse = Product("P002", "Мышка", 25.00)
    val keyboard = Product("P003", "Клавиатура", 75.00)


    val customer = Customer("C101", "Алиса", "alice@example.com")


    val order = Order("O5001", customer)


    println(order.addItem(laptop, 1))
    println(order.addItem(mouse, 2))
    println(order.addItem(keyboard, 1))
    println(order.addItem(mouse, 1))


    order.displayOrderDetails()


    println(order.updateStatus(OrderStatus.PROCESSING))
    order.displayOrderDetails()


    println(order.removeItem("P003"))
    order.displayOrderDetails()


    println(order.removeItem("P999"))


    val admin = Admin("A001", "Никита")
    val anotherCustomer = Customer("C102", "Максим", "maxim@example.com")
    val anotherOrder = Order("O5002", anotherCustomer)
    anotherOrder.addItem(laptop, 1)
    anotherOrder.updateStatus(OrderStatus.SHIPPED)
    anotherOrder.displayOrderDetails()


    println(order.addItem(mouse, 0))
}
