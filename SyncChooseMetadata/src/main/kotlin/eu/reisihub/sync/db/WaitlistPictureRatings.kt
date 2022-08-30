package eu.reisihub.sync.db

import org.jetbrains.exposed.sql.Table


object WaitlistPictureRatings : Table("review_pictures_rating") {
    val person = (long("person") references WaitlistPersons.id)
    val folder = (char("folder", 36) references WaitlistPictureAccess.folder)
    val filename = varchar("filename", 128)
    val rating = decimal("rating", 2, 1)

    override val primaryKey: PrimaryKey = PrimaryKey(folder, person, filename)
}
