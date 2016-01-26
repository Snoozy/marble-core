package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db._

case class AppleDeviceToken(
    deviceTokenId: Int,
    userId: Int,
    token: String
)

object AppleDeviceToken {

    private[data] val appleDeviceTokenParser: RowParser[AppleDeviceToken] = {
        get[Option[Int]]("device_token_id") ~
            get[Int]("user_id") ~
            get[String]("token_id") map {
            case deviceTokenId ~ userId ~ token =>
                AppleDeviceToken(deviceTokenId.get, userId, token)
        }
    }

    def createToken(userId: Int, token: String) = {
        DB.withConnection { implicit connection =>
            SQL("INSERT INTO apple_device_token (user_id, token) VALUES ({user}, {token})")
                .on('user -> userId, 'token -> token).executeInsert()
        }
    }

    def getDeviceTokens(userId: Int): Seq[String] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT token FROM apple_device_token WHERE user_id = {user}")
                .on('user -> userId).as(scalar[String] *)
        }
    }

    def deleteToken(token: String): Unit = {
        DB.withConnection { implicit connection =>
            SQL("DELETE FROM apple_device_token WHERE token = {token}").on('token -> token).executeUpdate()
        }
    }

}
