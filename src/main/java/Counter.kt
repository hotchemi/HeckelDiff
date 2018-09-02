sealed class Counter {
    object Zero : Counter()
    data class One(val index: Int) : Counter()
    object Many : Counter()

    fun increment(index: Int): Counter {
        return when (this) {
            is Zero -> One(index)
            else -> Many
        }
    }
}