import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Scanner
import kotlin.random.Random


enum class Priority {
    LOW, MEDIUM, HIGH
}


data class Task(
    val id: Int,
    val title: String,
    val description: String?,
    val priority: Priority,
    val isDone: Boolean = false,

    val createdArt: LocalDateTime = LocalDateTime.now()
)


class TaskRepository {

    private val tasks = mutableListOf<Task>()
    private var nextId = 1


    fun addTask(title: String, description: String?, priority: Priority): Task {

        val id = nextId++

        val newTask = Task(id, title, description, priority)

        tasks.add(newTask)
        return newTask
    }


    fun getAllTasks(): List<Task> {
        return tasks.toList()
    }


    fun getTaskById(id: Int): Task? {

        return tasks.find { it.id == id }
    }


    fun updateTask(
        id: Int,
        newTitle: String?,
        newDescription: String?,
        newPriority: Priority?,
        newIsDone: Boolean?
    ): Boolean {

        val task = getTaskById(id) ?: return false


        val updatedTask = task.copy(
            title = newTitle ?: task.title,
            description = newDescription ?: task.description,
            priority = newPriority ?: task.priority,
            isDone = newIsDone ?: task.isDone
        )

        val index = tasks.indexOf(task)
        if (index != -1) {
            tasks[index] = updatedTask
            return true
        }
        return false
    }


    fun deleteTask(id: Int): Boolean {

        return tasks.removeIf { it.id == id }
    }


    fun searchTasks(query: String?, priority: Priority?, isDone: Boolean?): List<Task> {
        var result = tasks.toList()


        if (isDone != null) {
            result = result.filter { it.isDone == isDone }
        }


        if (priority != null) {
            result = result.filter { it.priority == priority }
        }


        if (!query.isNullOrBlank()) {
            result = result.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.description?.contains(query, ignoreCase = true) == true
            }
        }
        return result
    }


    fun sortTasks(sortBy: String, order: String): List<Task> {
        val comparator: Comparator<Task> = when (sortBy.lowercase()) {
            "title" -> compareBy { it.title }
            "priority" -> compareBy { it.priority.ordinal }
            "createdat" -> compareBy { it.createdArt }
            "id" -> compareBy { it.id }
            else -> return tasks.toList()
        }

        return if (order.lowercase() == "desc") {
            tasks.sortedWith(comparator.reversed())
        } else {
            tasks.sortedWith(comparator)
        }
    }


    fun saveToFile(filename: String) {
        val file = File(filename)

        val jsonString = tasks.joinToString(prefix = "[\n  ", separator = ",\n  ", postfix = "\n]") { task ->

            val createdAtString = task.createdArt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

            """{
        "id": ${task.id},
        "title": "${task.title.replace("\"", "\\\"")}",
        "description": ${task.description?.let { "\"${it.replace("\"", "\\\"")}\"" }},
        "priority": "${task.priority.name}",
        "isDone": ${task.isDone},
        "createdArt": "$createdAtString"
      }"""
        }
        file.writeText(jsonString)
        println("Задачи сохранены в файл '$filename'.")
    }


    fun loadFromFile(filename: String) {
        val file = File(filename)
        if (!file.exists()) {
            println("Файл '$filename' не найден. Создаем пустой список задач.")
            return
        }

        try {
            val jsonString = file.readText()

            val tasksJson = jsonString.trim().removePrefix("[").removeSuffix("]")

            val taskEntries = tasksJson.splitToSequence(",\n  ")
                .filter { it.isNotBlank() }
                .map { it.trim() }
                .toList()

            if (taskEntries.isEmpty()) {
                println("Файл '$filename' пуст.")
                tasks.clear()
                nextId = 1
                return
            }

            val loadedTasks = mutableListOf<Task>()
            var maxId = 0
            taskEntries.forEach { taskJson ->

                val regex = Regex("""
                    \{
                      "id": (\d+),
                      "title": "(.*?)",
                      "description": (.*?|null),
                      "priority": "(LOW|MEDIUM|HIGH)",
                      "isDone": (true|false),
                      "createdArt": "(.*)"
                    }
                """.trimIndent())

                val matchResult = regex.find(taskJson)
                if (matchResult != null) {
                    val (idStr, title, descriptionStr, priorityStr, isDoneStr, createdAtStr) = matchResult.destructured
                    val id = idStr.toInt()
                    val description = if (descriptionStr == "null") null else descriptionStr
                    val priority = Priority.valueOf(priorityStr)
                    val isDone = isDoneStr.toBoolean()

                    val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
                    val createdAt = LocalDateTime.parse(createdAtStr, formatter)

                    loadedTasks.add(Task(id, title, description, priority, isDone, createdAt))
                    if (id > maxId) {
                        maxId = id
                    }
                } else {
                    println("Не удалось разобрать задачу из строки: $taskJson")
                }
            }
            tasks.clear()
            tasks.addAll(loadedTasks)
            nextId = maxId + 1 
            println("Задачи успешно загружены из файла '$filename'.")

        } catch (e: Exception) {
            println("Ошибка при загрузке задач из файла '$filename': ${e.message}")
            e.printStackTrace()
            
            tasks.clear()
            nextId = 1
        }
    }
}


class MenuController(private val repository: TaskRepository) {
    private val scanner = Scanner(System.`in`)
    
    private val displayDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    
    fun run() {
        loadDataIfNeeded() 
        while (true) {
            displayMenu()
            val choice = scanner.nextLine()
            when (choice) {
                "1" -> createTask()
                "2" -> viewAllTasks()
                "3" -> searchTasks()
                "4" -> editTask()
                "5" -> deleteTask()
                "6" -> markTasksDoneOrNot()
                "7" -> sortTasks()
                "8" -> exportTasks()
                "9" -> importTasks()
                "0" -> {
                    println("Выход из программы. Пожалуйста, сохраните данные, если хотите их сохранить.")
                    break 
                }
                else -> println("Неверный выбор. Пожалуйста, попробуйте снова.")
            }
        }
        scanner.close() 
    }

    
    private fun displayMenu() {
        println("\n--- Меню ---")
        println("1. Создать задачу")
        println("2. Показать все задачи")
        println("3. Найти задачи")
        println("4. Редактировать задачу")
        println("5. Удалить задачу")
        println("6. Отметить задачи как выполненные/невыполненные")
        println("7. Сортировать задачи")
        println("8. Экспорт задач в файл")
        println("9. Импорт задач из файла")
        println("0. Выход")
        print("Введите ваш выбор: ")
    }

    
    private fun loadDataIfNeeded() {
        print("Хотите загрузить задачи из файла 'tasks.json' при старте? (y/n): ")
        val answer = scanner.nextLine().lowercase()
        if (answer == "y") {
            repository.loadFromFile("tasks.json")
        }
    }


    
    private fun createTask() {
        println("\n--- Создание новой задачи ---")
        print("Введите заголовок: ")
        val title = scanner.nextLine()
        if (title.isBlank()) {
            println("Ошибка: Заголовок не может быть пустым.")
            return
        }

        print("Введите описание (нажмите Enter, чтобы пропустить): ")
        val description = scanner.nextLine().takeIf { it.isNotBlank() } 

        val priority = selectPriority("Выберите приоритет") ?: Priority.MEDIUM 

        val newTask = repository.addTask(title, description, priority)
        println("Задача успешно создана:")
        displayTaskDetails(newTask)
    }

   
    private fun viewAllTasks() {
        println("\n--- Список всех задач ---")
        val tasks = repository.getAllTasks()
        displayTasks(tasks)
    }

    
    private fun searchTasks() {
        println("\n--- Поиск задач ---")
        print("Поиск по заголовку/описанию (оставьте пустым для пропуска): ")
        val query = scanner.nextLine().takeIf { it.isNotBlank() }

        val priority = selectPriority("Выберите приоритет для фильтрации (или Enter, чтобы пропустить)")

        
        val isDone: Boolean? = if (query == null && priority == null) {
            selectDoneStatus("Искать выполненные задачи? (true/false, или Enter, чтобы пропустить)")
        } else {
            null 
        }

        val foundTasks = repository.searchTasks(query, priority, isDone)

        if (foundTasks.isEmpty()) {
            println("Задачи, соответствующие вашему запросу, не найдены.")
        } else {
            println("Найденные задачи:")
            displayTasks(foundTasks)
        }
    }

    
    private fun editTask() {
        println("\n--- Редактирование задачи ---")
        print("Введите ID задачи, которую хотите редактировать: ")
        val idString = scanner.nextLine()
        val id = idString.toIntOrNull()
        if (id == null) {
            println("Ошибка: Неверный формат ID.")
            return
        }

        val task = repository.getTaskById(id)
        if (task == null) {
            println("Задача с ID $id не найдена.")
            return
        }

        println("Текущие данные задачи:")
        displayTaskDetails(task)

        print("Введите новый заголовок (оставьте пустым, чтобы не менять: '${task.title}'): ")
        val newTitle = scanner.nextLine().takeIf { it.isNotBlank() }

        print("Введите новое описание (оставьте пустым, чтобы не менять: '${task.description ?: ""}'): ")
        val newDescription = scanner.nextLine().let { if (it.isBlank()) null else it }

        val newPriority = selectPriority("Выберите новый приоритет (или Enter, чтобы не менять)")

        val newIsDone = selectDoneStatus("Выберите новый статус выполнения (true/false, или Enter, чтобы не менять)")

        
        val success = repository.updateTask(id, newTitle, newDescription, newPriority, newIsDone)
        if (success) {
            println("Задача ID $id успешно обновлена!")
        } else {
            println("Не удалось обновить задачу ID $id.")
        }
    }

    
    private fun deleteTask() {
        println("\n--- Удаление задачи ---")
        print("Введите ID задачи, которую хотите удалить: ")
        val idString = scanner.nextLine()
        val id = idString.toIntOrNull()
        if (id == null) {
            println("Ошибка: Неверный формат ID.")
            return
        }

        val task = repository.getTaskById(id)
        if (task == null) {
            println("Задача с ID $id не найдена.")
            return
        }

        print("Вы уверены, что хотите удалить задачу '${task.title}' (ID: $id)? (y/n): ")
        val confirmation = scanner.nextLine().lowercase()
        if (confirmation == "y") {
            val success = repository.deleteTask(id)
            if (success) {
                println("Задача ID $id успешно удалена.")
            } else {
                println("Не удалось удалить задачу ID $id.")
            }
        } else {
            println("Удаление отменено.")
        }
    }

    
    private fun markTasksDoneOrNot() {
        println("\n--- Отметить задачи ---")
        print("Введите ID задач для изменения статуса (через запятую, например: 1,3,5): ")
        val idsString = scanner.nextLine()
        val ids = idsString.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }

        if (ids.isEmpty()) {
            println("Не указаны корректные ID задач.")
            return
        }

        val newStatus = selectDoneStatus("Выберите новый статус для выбранных задач (true/false)")
        if (newStatus == null) {
            println("Изменение статуса отменено.")
            return
        }

        var updatedCount = 0
        for (id in ids) {
            val success = repository.updateTask(id, null, null, null, newStatus)
            if (success) {
                updatedCount++
            }
        }

        println("$updatedCount из ${ids.size} выбранных задач успешно обновлены.")
    }


   
    private fun sortTasks() {
        println("\n--- Сортировка задач ---")
        print("Сортировать по (title, priority, createdAt, id): ")
        val sortBy = scanner.nextLine().lowercase()
        val validSortByFields = listOf("title", "priority", "createdat", "id")
        if (sortBy !in validSortByFields) {
            println("Неверное поле для сортировки. Доступны: ${validSortByFields.joinToString()}.")
            return
        }

        print("Порядок сортировки (asc/desc): ")
        val order = scanner.nextLine().lowercase()
        if (order != "asc" && order != "desc") {
            println("Неверный порядок сортировки. Доступны: asc, desc.")
            return
        }

        val sortedTasks = repository.sortTasks(sortBy, order)
        displayTasks(sortedTasks)
    }

    
    private fun exportTasks() {
        println("\n--- Экспорт задач ---")
        print("Введите имя файла для экспорта (рекомендуется .json, например, my_tasks.json): ")
        val filename = scanner.nextLine()
        if (filename.isBlank()) {
            println("Имя файла не может быть пустым.")
            return
        }
        
        repository.saveToFile(filename.takeIf { it.endsWith(".json", ignoreCase = true) } ?: "$filename.json")
    }

    
    private fun importTasks() {
        println("\n--- Импорт задач ---")
        print("Введите имя файла для импорта (например, my_tasks.json): ")
        val filename = scanner.nextLine()
        if (filename.isBlank()) {
            println("Имя файла не может быть пустым.")
            return
        }
        
        repository.loadFromFile(filename.takeIf { it.endsWith(".json", ignoreCase = true) } ?: "$filename.json")
    }

    
    private fun selectPriority(prompt: String): Priority? {
        println("$prompt (LOW, MEDIUM, HIGH) (или нажмите Enter, чтобы пропустить):")
        val input = scanner.nextLine().trim().uppercase()
        return when (input) {
            "LOW" -> Priority.LOW
            "MEDIUM" -> Priority.MEDIUM
            "HIGH" -> Priority.HIGH
            "" -> null 
            else -> {
                println("Неверный ввод. Пожалуйста, выберите один из предложенных вариантов.")
                selectPriority(prompt) 
            }
        }
    }

    
    private fun selectDoneStatus(prompt: String): Boolean? {
        println("$prompt (true / false) (или нажмите Enter, чтобы пропустить):")
        val input = scanner.nextLine().trim().lowercase()
        return when (input) {
            "true" -> true
            "false" -> false
            "" -> null 
            else -> {
                println("Неверный ввод. Пожалуйста, введите 'true' или 'false'.")
                selectDoneStatus(prompt) 
            }
        }
    }

    
    private fun displayTasks(tasks: List<Task>) {
        if (tasks.isEmpty()) {
            println("Нет задач для отображения.")
            return
        }
        
        val idWidth = 5
        val priorityWidth = 10
        val doneWidth = 10
        val titleMaxWidth = 40 
        val dateWidth = 16 

        
        val header = String.format("| %-${idWidth}s | %-${priorityWidth}s | %-${doneWidth}s | %-${titleMaxWidth}s | %-${dateWidth}s |",
            "ID", "Priority", "Done", "Title")
        val separator = "-".repeat(header.length)

        println(separator)
        println(header)
        println(separator)

        
        tasks.forEach { task ->
            
            val idStr = task.id.toString().take(idWidth).padEnd(idWidth)
            val priorityStr = task.priority.name.take(priorityWidth).padEnd(priorityWidth)
            val doneStr = task.isDone.toString().take(doneWidth).padEnd(doneWidth)
            val titleStr = task.title.take(titleMaxWidth).padEnd(titleMaxWidth) 
            val dateStr = task.createdArt.format(displayDateTimeFormatter).take(dateWidth).padEnd(dateWidth)

            println(String.format("| %s | %s | %s | %s | %s |", idStr, priorityStr, doneStr, titleStr, dateStr))
        }
        println(separator)
    }

    
    private fun displayTaskDetails(task: Task) {
        println("  ID: ${task.id}")
        println("  Заголовок: ${task.title}")
        println("  Описание: ${task.description ?: "—"}") 
        println("  Приоритет: ${task.priority}")
        println("  Выполнено: ${if (task.isDone) "Да" else "Нет"}")
        println("  Дата создания: ${task.createdArt.format(displayDateTimeFormatter)}")
    }
}


fun main() {
    val repository = TaskRepository() 
    val controller = MenuController(repository) 
    controller.run() 
}
