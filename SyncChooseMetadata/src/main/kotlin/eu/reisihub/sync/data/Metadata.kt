package eu.reisihub.sync.data

import java.math.BigDecimal
import kotlin.math.roundToInt


class Metadata(private val comments: Map<Person, String>, private val ratings: Map<Person, BigDecimal>) {
    private val avgRating = (ratings.values.sumOf { it } / BigDecimal(ratings.size)).toDouble().roundToInt()

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
}
