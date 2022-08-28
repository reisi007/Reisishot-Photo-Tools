package eu.reisihub.sync.db

import org.jetbrains.exposed.sql.Table

object WaitlistPictureAccess : Table("review_pictures_access") {
    val person = (long("person") references WaitlistPersons.id)
    val folder = char("folder", 36)

    override val primaryKey: PrimaryKey = PrimaryKey(person, folder)

}
