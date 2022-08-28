package eu.reisihub.sync.db

import org.jetbrains.exposed.sql.Table

object WaitlistPersons : Table("waitlist_person") {
    val id = long("id")
    val firstname = varchar("firstname", 200)
    val lastname = varchar("lastname", 200)

    override val primaryKey = PrimaryKey(id)
}
