package com.lifeos.data

import kotlinx.serialization.Serializable
import java.util.Calendar

/** Money entering the household, stored in cents like [Expense]. */
@Serializable
data class Income(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String = "",
    val amountCents: Long = 0L,
    val category: String = "Stipendio",
    val dateMillis: Long = System.currentTimeMillis()
)

/** A monthly spending limit for one expense category. */
@Serializable
data class CategoryBudget(
    val category: String = "",
    val limitCents: Long = 0L
)

/** Derived view of one category budget; never stored. */
data class CategoryBudgetStatus(
    val category: String,
    val spentCents: Long,
    val limitCents: Long
) {
    val percent: Int get() = if (limitCents <= 0) 0 else ((spentCents * 100) / limitCents).toInt()
    val remainingCents: Long get() = limitCents - spentCents
    val overBudget: Boolean get() = limitCents > 0 && spentCents > limitCents
}

/** One month of the finance chart. */
data class MonthFinance(
    val year: Int,
    val month: Int,
    val label: String,
    val incomeCents: Long,
    val expenseCents: Long
) {
    val balanceCents: Long get() = incomeCents - expenseCents
}

fun normalizedCategory(category: String): String =
    category.trim().replaceFirstChar { it.uppercase() }.ifBlank { "Generale" }

fun LifeOSState.addingIncome(
    label: String,
    amountCents: Long,
    category: String = "Stipendio",
    dateMillis: Long = System.currentTimeMillis()
): LifeOSState = copy(
    incomes = incomes + Income(
        label = label.trim(),
        amountCents = amountCents,
        category = normalizedCategory(category),
        dateMillis = dateMillis
    )
)

fun LifeOSState.removingIncome(id: String): LifeOSState =
    copy(incomes = incomes.filterNot { it.id == id })

fun LifeOSState.incomesInMonth(now: Long = System.currentTimeMillis()): List<Income> {
    val range = monthRange(now)
    return incomes.filter { it.dateMillis in range }.sortedByDescending { it.dateMillis }
}

fun LifeOSState.monthIncomeTotalCents(now: Long = System.currentTimeMillis()): Long =
    incomesInMonth(now).sumOf { it.amountCents }

/** Income minus expenses for the current month. */
fun LifeOSState.monthBalanceCents(now: Long = System.currentTimeMillis()): Long =
    monthIncomeTotalCents(now) - monthExpenseTotalCents(now)

/**
 * Sets (or replaces) the budget of a category. A zero or negative limit removes
 * the budget instead of leaving an unusable entry behind.
 */
fun LifeOSState.settingCategoryBudget(category: String, limitCents: Long): LifeOSState {
    val name = normalizedCategory(category)
    val others = categoryBudgets.filterNot { it.category.equals(name, ignoreCase = true) }
    return if (limitCents <= 0) {
        copy(categoryBudgets = others)
    } else {
        copy(categoryBudgets = others + CategoryBudget(name, limitCents))
    }
}

fun LifeOSState.removingCategoryBudget(category: String): LifeOSState =
    copy(categoryBudgets = categoryBudgets.filterNot { it.category.equals(category, ignoreCase = true) })

/** Configured budgets with current-month spending, over-budget ones first. */
fun LifeOSState.categoryBudgetStatuses(now: Long = System.currentTimeMillis()): List<CategoryBudgetStatus> {
    val spentByCategory = expensesInMonth(now).groupBy { normalizedCategory(it.category) }
        .mapValues { entry -> entry.value.sumOf { it.amountCents } }
    return categoryBudgets
        .mapNotNull { budget ->
            val limit = budget.limitCents
            if (limit <= 0) null
            else CategoryBudgetStatus(
                category = budget.category,
                spentCents = spentByCategory.entries
                    .firstOrNull { it.key.equals(budget.category, ignoreCase = true) }?.value ?: 0L,
                limitCents = limit
            )
        }
        .sortedWith(compareByDescending<CategoryBudgetStatus> { it.overBudget }.thenByDescending { it.percent })
}

/** Total spending per category for the current month, highest first. */
fun LifeOSState.spendingByCategory(now: Long = System.currentTimeMillis()): List<Pair<String, Long>> =
    expensesInMonth(now)
        .groupBy { normalizedCategory(it.category) }
        .mapValues { entry -> entry.value.sumOf { it.amountCents } }
        .toList()
        .sortedByDescending { it.second }

/** Last [months] months including the current one, oldest first. */
fun LifeOSState.financeSeries(months: Int = 6, now: Long = System.currentTimeMillis()): List<MonthFinance> {
    val count = months.coerceIn(1, 24)
    val calendar = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    calendar.add(Calendar.MONTH, -(count - 1))
    return (0 until count).map {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val start = calendar.timeInMillis
        val label = String.format("%02d/%02d", month + 1, year % 100)
        calendar.add(Calendar.MONTH, 1)
        MonthFinance(
            year = year,
            month = month,
            label = label,
            incomeCents = incomes.filter { it.dateMillis in start until calendar.timeInMillis }.sumOf { it.amountCents },
            expenseCents = expenses.filter { it.dateMillis in start until calendar.timeInMillis }.sumOf { it.amountCents }
        )
    }
}

/** Active subscriptions normalised to a monthly cost, in cents. */
fun LifeOSState.monthlySubscriptionCents(): Long = subscriptions.filter { it.active }.sumOf { subscription ->
    when (subscription.repeat) {
        RepeatRule.MONTHLY -> subscription.amountCents
        RepeatRule.WEEKLY -> subscription.amountCents * 52 / 12
        RepeatRule.DAILY -> subscription.amountCents * 365 / 12
        RepeatRule.NONE -> 0L
    }
}

fun LifeOSState.yearlySubscriptionCents(): Long = monthlySubscriptionCents() * 12

/** Quotes a CSV field only when it needs it, doubling embedded quotes. */
fun csvField(value: String): String =
    if (value.any { it == ';' || it == '"' || it == '\n' }) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }

/** Full export with one row per income/expense, ordered by date. */
fun LifeOSState.financeCsv(): String {
    val rows = buildList {
        add(listOf("tipo", "data", "descrizione", "categoria", "importo_eur"))
        (incomes.map {
            FinanceRow("Entrata", it.dateMillis, it.label, it.category, it.amountCents)
        } + expenses.map {
            FinanceRow("Uscita", it.dateMillis, it.label, it.category, it.amountCents)
        })
            .sortedBy { it.dateMillis }
            .forEach { row ->
                add(
                    listOf(
                        row.kind,
                        String.format("%tF", row.dateMillis),
                        csvField(row.label),
                        csvField(row.category),
                        String.format("%.2f", row.cents / 100.0).replace('.', ',')
                    )
                )
            }
    }
    return rows.joinToString("\n") { it.joinToString(";") } + "\n"
}

private data class FinanceRow(
    val kind: String,
    val dateMillis: Long,
    val label: String,
    val category: String,
    val cents: Long
)
