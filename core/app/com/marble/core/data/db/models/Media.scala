package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db._

case class Media(
    mediaId: Option[Int],
    mediaType: Int,
    mediaName: String,
    mediaUrl: String
)

object Media {

    val BaseMediaURL = "https://static.themarble.co/image/"

    private[models] val mediaParser: RowParser[Media] = {
        get[Option[Int]]("media_id") ~
            get[Int]("media_type") ~
            get[String]("media_name") map {
            case mediaId ~ mediaType ~ mediaName =>
                Media(mediaId, mediaType, mediaName, BaseMediaURL + mediaName)
        }
    }

    def find(mediaId: Int): Option[Media] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM media WHERE media_id = {media}").on('media -> mediaId).as(mediaParser.singleOpt)
        }
    }

    def create(mediaType: Int, mediaName: String): Option[Long] = {
        DB.withConnection { implicit connection =>
            SQL("INSERT INTO media (media_type, media_name) VALUES ({media_type}, {media_name})")
                .on('media_type -> mediaType, 'media_name -> mediaName).executeInsert()
        }
    }

    def getAll: Seq[Media] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM media").as(mediaParser *)
        }
    }

}
