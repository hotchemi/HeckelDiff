import org.junit.Test

class DiffTest {
    @Test
    fun testSomeLibraryMethod() {
        val from = arrayOf(1, 2, 3, 4, 5)
        val to = arrayOf(1,3,5,7)
        val diff = Diff.calculate(from, to)
        println(diff)
    }
}