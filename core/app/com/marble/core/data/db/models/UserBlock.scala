package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db._

case class UserBlock (
    blockId: Option[Int],
    blockerId: Int,
    blockeeId: Int
)

object UserBlock {
    private[data] val userBlockParser: RowParser[UserBlock] = {
        get[Option[Int]]("block_id") ~
            get[Int]("blocker_id") ~
            get[Int]("blockee_id") map {
            case blockId ~ blockerId ~ blockeeId =>
                UserBlock(blockId, blockerId, blockeeId)
        }
    }

    def blockUser(blockerId: Int, blockeeId: Int): Unit = {
        DB.withConnection { implicit connection =>
            SQL("INSERT INTO user_block (blocker_id, blockee_id) VALUES ({blocker_id}, {blockee_id})").on('blocker_id -> blockerId, 'blockee_id -> blockeeId).executeInsert()
        }
    }

    def getBlockedUserIds(userId: Int): Seq[Int] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT blockee_id FROM user_block WHERE blocker_id = {blocker}").on('blocker -> userId).as(scalar[Int] *)
        }
    }
}
