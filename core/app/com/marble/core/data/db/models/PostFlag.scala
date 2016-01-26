package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db._

case class PostFlag (
    postFlagId: Option[Int],
    postId: Int,
    userId: Int
)

object PostFlag {
    private[data] val postFlagParser: RowParser[PostFlag] = {
        get[Option[Int]]("post_flag_id") ~
            get[Int]("post_id") ~
            get[Int]("user_id") map {
            case postFlagId ~ postId ~ userId =>
                PostFlag(postFlagId, postId, userId)
        }
    }

    def createPostFlag(userId: Int, postId: Int): Unit = {
        DB.withConnection { implicit connection =>
            SQL("INSERT INTO post_flag (user_id, post_id) VALUES ({user_id}, {post_id})").on('user_id -> userId, 'post_id -> postId).executeInsert()
        }
    }
}
