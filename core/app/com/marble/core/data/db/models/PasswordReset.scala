package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db._
import com.marble.utils.security.SecureRand
import com.marble.core.email

case class PasswordReset (
    resetId: Option[Int],
    userId: Int,
    token: String,
    time: Long
)

object PasswordReset {

    private[data] val passwordResetParser: RowParser[PasswordReset] = {
        get[Option[Int]]("reset_id") ~
            get[Int]("user_id") ~
            get[String]("token") ~
            get[Long]("time") map {
            case resetId ~ userId ~ token ~ time =>
                PasswordReset(resetId, userId, token, time)
        }
    }

    def find(id: Int): Option[PasswordReset] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM password_reset WHERE reset_id = {reset_id} LIMIT 1").on('reset_id -> id).as(passwordResetParser.singleOpt)
        }
    }

    def byToken(token: String): Option[PasswordReset] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM password_reset WHERE token = {token} LIMIT 1").on('token -> token).as(passwordResetParser.singleOpt)
        }
    }

    def byUser(userId: Int): Seq[PasswordReset] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM password_reset WHERE user_id = {user_id} LIMIT 1").on('user_id -> userId).as(passwordResetParser *)
        }
    }

    def newReset(userId: Int): String = {
        DB.withConnection { implicit connection =>
            val token = SecureRand.newSessionId()
            val time = System.currentTimeMillis()
            SQL("INSERT INTO password_reset (user_id, token, time) VALUES ({user}, {token}, {time})").on('user -> userId, 'token -> token, 'time -> time).executeInsert()
            token
        }
    }

    def deleteOld(userId: Int): Unit = {
        DB.withConnection { implicit connection =>
            SQL("DELETE FROM password_reset WHERE user_id = {user}").on('user -> userId).executeInsert()
        }
    }

}
