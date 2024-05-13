package th.co.bkkps.edc.util

object NumPadUtils {
    fun calculateValue(currentValue: String, number: String, maximumSizeOfAmount: Int): String {
        var newValue: String

        if (currentValue.length == maximumSizeOfAmount) {
            newValue = currentValue
        }
        else {
            if (currentValue.isEmpty()) {
                newValue = number
            } else {
                if (currentValue.length + number.length <= maximumSizeOfAmount) {
                    newValue = "$currentValue$number"
                } else {
                    val remaining: Int = maximumSizeOfAmount - currentValue.length
                    val trimmedNumber: String = number.substring(IntRange(0, remaining - 1));

                    newValue = "$currentValue$trimmedNumber"
                }
            }
        }

        return newValue
    }

    fun deleteValue(currentValue: String): String {
        var newValue: String = ""

        if (currentValue.isNotEmpty()) {
            if (currentValue.length > 1) {
                val range: IntRange = IntRange(0, currentValue.lastIndex-1)
                newValue = currentValue.substring(range)
            }
        }

        return newValue
    }
}