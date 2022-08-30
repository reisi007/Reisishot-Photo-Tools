package eu.reisihub.sync.data

import java.math.BigDecimal


class Metadata(private val comments: Map<Person, String>, private val ratings: Map<Person, BigDecimal>) {
    val avgRating by lazy { (ratings.values.sumOf { it } / BigDecimal(ratings.size)).toDouble() }

    override fun toString(): String {
        return buildString {
            append(avgRating)
            append(" stars")
            if (comments.isNotEmpty()) {
                append(" Comments: ")
                append(comments)
            }
        }
    }

    fun buildComment(): String = buildString {
        append(" == ").append("Ratings (").append(avgRating).append(") == ")
        appendLine()
        ratings.forEach { (person, value) ->
            append(" - ")
            append(person)
            append(": ")
            append(value)
            appendLine()
        }
        if (comments.isNotEmpty()) {
            appendLine()
            append(" == ").append("Comments").append(" == ")
            appendLine()
            appendLine()
            comments.forEach { (person, value) ->
                append(" = ").append(person).append(" = ").appendLine()
                appendLine(value)
                appendLine()
            }
        }
    }
}

