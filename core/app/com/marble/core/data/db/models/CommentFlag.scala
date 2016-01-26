package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db._

case class CommentFlag (
    commentFlagId: Option[Int],
    commentId: Int,
    userId: Int
)

object CommentFlag {
    private[data] val commentFlagParser: RowParser[CommentFlag] = {
        get[Option[Int]]("comment_flag_id") ~
            get[Int]("comment_id") ~
            get[Int]("user_id") map {
            case commentFlagId ~ commentId ~ userId =>
                CommentFlag(commentFlagId, commentId, userId)
        }
    }

    def createCommentFlag(userId: Int, commentId: Int): Unit = {
        DB.withConnection { implicit connection =>
            SQL("INSERT INTO comment_flag (user_id, comment_id) VALUES ({user_id}, {comment_id})").on('user_id -> userId, 'comment_id -> commentId).executeInsert()
        }
    }
}
