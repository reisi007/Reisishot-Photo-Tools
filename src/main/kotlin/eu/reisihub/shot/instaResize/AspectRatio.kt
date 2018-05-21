package eu.reisihub.shot.instaResize

import java.awt.image.BufferedImage


data class AspectRatio(val widthAspect: Int, val heightAspect: Int) : Comparable<AspectRatio> {

    init {
        (widthAspect > 0 && heightAspect > 0).let { legal -> if (!legal) throw IllegalStateException("Aspect ratio of $widthAspect:$heightAspect is not valid!") }
    }

    /**
     * Returns a positive value
     */
    private val asFloat = widthAspect.toFloat() / heightAspect

    override fun compareTo(other: AspectRatio): Int = (asFloat - other.asFloat).let { result ->
        if (Math.abs(result) < 0.000000001) return@let 0
        else if (result > 0)
            return@let 1
        else
            return@let -1
    }

    override fun equals(other: Any?): Boolean {
        (other as? AspectRatio)!!
        return compareTo(other) == 0
    }

    override fun hashCode(): Int {
        return asFloat.hashCode()
    }

    override fun toString(): String {
        return "$widthAspect:$heightAspect"
    }

    operator fun rangeTo(other: AspectRatio): ClosedRange<AspectRatio> =
        (if (this < other) {
            this to other
        } else {
            other to this
        }).let { (start, end) ->
            object : ClosedRange<AspectRatio> {
                override val endInclusive: AspectRatio
                    get() = end
                override val start: AspectRatio
                    get() = start
            }
        }
}

val BufferedImage.aspectRatio: AspectRatio
    get() = AspectRatio(width, height)
