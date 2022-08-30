package eu.reisihub.sync.data


class Person(private val firstname: String, private val lastname: String) {
    override fun toString(): String {
        return "$firstname $lastname"
    }
}
