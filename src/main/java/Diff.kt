// Based on https://dl.acm.org/citation.cfm?id=359467
object Diff {
    @JvmStatic
    fun <T : Any> calculate(from: Array<T>, to: Array<T>): List<EditOperation> {
        val symbolTable = mutableMapOf<Int, SymbolTableEntry>()
        val oldElementReferences = mutableListOf<ElementReference>()
        val newElementReferences = mutableListOf<ElementReference>()

        registerNewArrayToSymbolTable(array = to, symbolTable = symbolTable, elementReferences = newElementReferences)
        registerOldArrayToSymbolTable(array = from, symbolTable = symbolTable, elementReferences = oldElementReferences)
        computeForSharedUniqueElement(newElementReferences, oldElementReferences)
        computeForConsecutiveSharedElement(newElementReferences, oldElementReferences)
        computeForConsecutiveSharedElementWithDescendingOrder(newElementReferences, oldElementReferences)
        return editScript(from, to, newElementReferences, oldElementReferences);
    }

    private fun <T : Any> registerNewArrayToSymbolTable(array: Array<T>, symbolTable: MutableMap<Int, SymbolTableEntry>, elementReferences: MutableList<ElementReference>) {
        array.forEach {
            val entry = symbolTable[it.hashCode()] ?: SymbolTableEntry()
            entry.newCounter = entry.newCounter.increment(0)
            elementReferences.add(ElementReference.SymbolTable(entry))
            symbolTable[it.hashCode()] = entry
        }
    }

    private fun <T : Any> registerOldArrayToSymbolTable(array: Array<T>, symbolTable: MutableMap<Int, SymbolTableEntry>, elementReferences: MutableList<ElementReference>) {
        array.forEachIndexed { index, it ->
            val entry = symbolTable[it.hashCode()] ?: SymbolTableEntry()
            entry.oldCounter = entry.oldCounter.increment(index)
            elementReferences.add(ElementReference.SymbolTable(entry))
            symbolTable[it.hashCode()] = entry
        }
    }

    private fun computeForSharedUniqueElement(newElementReferences: MutableList<ElementReference>, oldElementReferences: MutableList<ElementReference>) {
        newElementReferences.forEachIndexed { index, it ->
            if (it is ElementReference.SymbolTable) {
                val oldCounter = it.entry.oldCounter
                val newCounter = it.entry.newCounter
                if (oldCounter is Counter.One && newCounter is Counter.One) {
                    newElementReferences[index] = ElementReference.TheOther(oldCounter.index)
                    oldElementReferences[oldCounter.index] = ElementReference.TheOther(index)
                }
            }
        }
    }

    private fun computeForConsecutiveSharedElement(newElementReferences: MutableList<ElementReference>, oldElementReferences: MutableList<ElementReference>) {
        if (oldElementReferences.isNotEmpty() && newElementReferences.isNotEmpty()) {
            val oldFirst = oldElementReferences.first()
            val newFirst = newElementReferences.first()
            if (oldFirst is ElementReference.SymbolTable && newFirst is ElementReference.SymbolTable && oldFirst == newFirst) {
                oldElementReferences[0] = ElementReference.TheOther(0)
                newElementReferences[0] = ElementReference.TheOther(0)
            }
        }

        newElementReferences.forEachIndexed { newIndex, it ->
            if (it is ElementReference.TheOther) {
                val oldIndex = it.at
                if (oldIndex < oldElementReferences.size - 1 && newIndex < newElementReferences.size - 1) {
                    val nextNewElement = newElementReferences[newIndex + 1]
                    val nextOldElement = oldElementReferences[oldIndex + 1]
                    if (nextNewElement is ElementReference.SymbolTable && nextOldElement is ElementReference.SymbolTable) {
                        if (nextNewElement.entry == nextOldElement.entry) {
                            newElementReferences[newIndex + 1] = ElementReference.TheOther(oldIndex + 1)
                            oldElementReferences[oldIndex + 1] = ElementReference.TheOther(newIndex + 1)
                        }
                    }
                }
            }
        }
    }

    private fun computeForConsecutiveSharedElementWithDescendingOrder(newElementReferences: MutableList<ElementReference>, oldElementReferences: MutableList<ElementReference>) {
        if (oldElementReferences.isNotEmpty() && newElementReferences.isNotEmpty()) {
            val oldLast = oldElementReferences.last()
            val newLast = newElementReferences.last()
            if (oldLast is ElementReference.SymbolTable && newLast is ElementReference.SymbolTable && oldLast == newLast) {
                oldElementReferences[0] = ElementReference.TheOther(0)
                newElementReferences[0] = ElementReference.TheOther(0)
            }
        }

        newElementReferences.reversed().forEachIndexed { newIndex, it ->
            if (it is ElementReference.TheOther) {
                val oldIndex = it.at
                if (oldIndex > 0 && newIndex > 0) {
                    val nextNewElement = newElementReferences[newIndex - 1]
                    val nextOldElement = oldElementReferences[oldIndex - 1]
                    if (nextNewElement is ElementReference.SymbolTable && nextOldElement is ElementReference.SymbolTable) {
                        if (nextNewElement.entry == nextOldElement.entry) {
                            newElementReferences[newIndex - 1] = ElementReference.TheOther(oldIndex - 1)
                            oldElementReferences[oldIndex - 1] = ElementReference.TheOther(newIndex - 1)
                        }
                    }
                }
            }
        }
    }

    private fun <T : Any> editScript(newArray: Array<T>, oldArray: Array<T>, newElementReferences: MutableList<ElementReference>, oldElementReferences: MutableList<ElementReference>): List<EditOperation> {
        val editScript = mutableListOf<EditOperation>()
        val oldIndexOffsets = mutableListOf<Int>()

        var offsetByDelete = 0
        oldElementReferences.forEachIndexed { oldIndex, reference ->
            oldIndexOffsets[oldIndex] = offsetByDelete
            if (reference is ElementReference.SymbolTable) {
                editScript.add(EditOperation.Delete(oldArray[oldIndex], oldIndex))
                offsetByDelete++;
            }
        }

        var offsetByInsert = 0
        newElementReferences.forEachIndexed { newIndex, reference ->
            when (reference) {
                is ElementReference.SymbolTable -> {
                    editScript.add(EditOperation.Insert(newArray[newIndex], index = newIndex))
                    offsetByInsert++;
                }
                is ElementReference.TheOther -> {
                    val oldIndex = reference.at
                    if (oldIndex - oldIndexOffsets[oldIndex] != newIndex - offsetByInsert) {
                        editScript.add(EditOperation.Move(newArray[newIndex], oldIndex, newIndex))
                    }
                }
            }
        }
        return editScript
    }
}