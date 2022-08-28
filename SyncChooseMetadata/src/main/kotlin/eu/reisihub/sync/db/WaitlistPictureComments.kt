package eu.reisihub.sync.db

import org.jetbrains.exposed.sql.Table

object WaitlistPictureComments : Table("review_pictures_comment") {
    val person = (long("person") references WaitlistPictureAccess.person)
    val folder = (char("folder", 36) references WaitlistPictureAccess.folder)
    val filename = varchar("filename", 128)
    val comment = text("comment")

    override val primaryKey: PrimaryKey = PrimaryKey(folder, person, filename)
}
