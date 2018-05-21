package eu.reisihub.shot

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AspectRatioTest {

    @Test
    fun simpleAssertions() {
        assertTrue {
            AspectRatio(16, 9) == AspectRatio(32, 18)
        }
        assertTrue {
            AspectRatio(21, 9) == AspectRatio(63, 27)
        }
        assertTrue {
            AspectRatio(4, 3) == AspectRatio(4, 3)
        }
        assertFalse {
            AspectRatio(10000, 10001) == AspectRatio(1, 1)
        }
    }

    @Test
    fun rangeAssertions() {
        assertTrue {
            AspectRatio(1, 1) in AspectRatio(2, 1)..AspectRatio(1, 2)
                    &&
                    AspectRatio(1, 1) in AspectRatio(1, 2)..AspectRatio(2, 1)
        }
    }
}