package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import play.api.Play
import play.api.Play.current
import play.api.db._
import play.api.libs.json._

case class Moderator (
    moderatorId: Int,
    userId: Int,
    boardId: Int,
    time: Long
)

object Moderator {

    private[data] val moderatorParser: RowParser[Moderator] = {
        get[Option[Int]]("moderator_id") ~
            get[Int]("user_id") ~
            get[Int]("board_id") ~
            get[Long]("time") map {
            case moderatorId ~ userId ~ boardId ~ time =>
                Moderator(moderatorId.get, userId, boardId, time)
        }
    }

}
