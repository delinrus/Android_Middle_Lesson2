package ru.skillbranch.kotlinexample.extensions

fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
    val list = ArrayList<T>()
    if (size == 0) return list
    val reverseIterator = this.listIterator(this.size - 1)
    while (reverseIterator.hasPrevious()) {
        if (predicate(reverseIterator.previous())) break
    }
    while (reverseIterator.hasPrevious()) {
        list.add(reverseIterator.previous())
    }
    return list.reversed()
}