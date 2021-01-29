package pl.erizo.gradle.jcstress.reordering

class AtomicInt {
    @Volatile
    private var value: Int = 0

    fun inc() {
        value++
    }

    fun get() = value
}
