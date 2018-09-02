sealed class ElementReference {
    data class SymbolTable(val entry: SymbolTableEntry): ElementReference()
    data class TheOther(val at: Int): ElementReference()
}