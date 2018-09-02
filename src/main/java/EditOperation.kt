sealed class EditOperation {
    data class Delete<E>(val element: E, val index: Int) : EditOperation()
    data class Insert<E>(val element: E, val index: Int) : EditOperation()
    data class Move<E>(val element: E, val from: Int, val to: Int) : EditOperation()
}