package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.marble.core.data.cache.Session
import com.marble.utils.Etc.makeDigest
import play.api.Play.current
import com.marble.core.data.Constants
import org.apache.commons.lang3.RandomStringUtils
import play.api.db._
import play.api.libs.json._

case class UserInfo(
    userId: Int,
    password: String,
    email: String,
    time: Long,
    reputation: Int,
    inboxCount: Int,
    bio: String
)

object UserInfo {

    private[models] val userInfoParser: RowParser[UserInfo] = {
        get[Option[Long]]("user_id") ~
            get[String]("password") ~
            get[String]("email") ~
            get[Long]("time") ~
            get[Int]("reputation") ~
            get[Int]("inbox_count") ~
            get[String]("bio") map {
            case userId ~ password ~ email ~ time ~ reputation ~ inboxCount ~ bio =>
                UserInfo(userId.get.toInt, password, email, time, reputation, inboxCount, bio)
        }
    }

    def create(userId: Int, password: String, email: String, bio: Option[String] = None) = {
        DB.withConnection { implicit connection =>
            val time = System.currentTimeMillis()

            val pass = {
                if (password != "")
                    makeDigest(password)
                else password
            }

            SQL("INSERT INTO user_info (user_id, password, email, time, reputation, bio) VALUES ({user_id}, {password}, {email}, {time}, 0, {bio})")
                .on('user_id -> userId, 'password -> pass, 'email -> email, 'time -> time, 'bio -> bio.getOrElse("")).executeInsert()
        }
    }

    def find(userId: Int) = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM user_info WHERE user_id = {user_id}").on('user_id -> userId).as(userInfoParser.singleOpt)
        }
    }

    def setPhoto(userId: Int) = {
        DB.withConnection { implicit connection =>
            val id = SQL("SELECT photo FROM user WHERE user_id = {user}").on('user -> userId).as(scalar[Int].singleOpt)
            if (id.isDefined && id.get != 0) {
                val media = Media.find(id.get)
                SQL("UPDATE user SET photo_name = {photoName} WHERE user_id = {user}")
                    .on('photoName -> media.get.mediaName, 'user -> userId).executeUpdate()
            }
        }
    }

    private def createRaw(userId: Int, password: String, email: String, bio: String, time: Long) = {
        DB.withConnection { implicit connection =>

            SQL("INSERT INTO user_info (user_id, password, email, time, reputation, bio) VALUES ({user_id}, {password}, {email}, {time}, 0, {bio})")
                .on('user_id -> userId, 'password -> password, 'email -> email, 'time -> time, 'bio -> bio).executeInsert()
        }
    }

}
