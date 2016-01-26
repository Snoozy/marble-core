package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db._
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import scala.concurrent.duration._

case class Conversation (
    conversationId: Option[Int],
    user1Id: Int,
    user2Id: Int,
    created: Long,
    updated: Long,
    read: Int,
    lastUser: Int,                        
    preview: String
)

object Conversation {

    private[data] val conversationParser: RowParser[Conversation] = {
        get[Option[Int]]("conversation_id") ~
            get[Int]("user1_id") ~
            get[Int]("user2_id") ~
            get[Long]("created_time") ~
            get[Long]("updated_time") ~
            get[Int]("read") ~
            get[Int]("last_user") ~
            get[String]("preview") map {
            case conversationId ~ user1Id ~ user2Id ~ created ~ updated ~ read ~ lastUser ~ preview =>
                Conversation(conversationId, user1Id, user2Id, created, updated, read, lastUser, preview)
        }
    }

    def find(conversationId: Int): Option[Conversation] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM conversation WHERE conversation_id = {id}").on('id -> conversationId).as(conversationParser.singleOpt)
        }
    }

    def byUsers(user1: Int, user2: Int): Option[Conversation] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM conversation WHERE user1_id = {user1} AND user2_id = {user2} OR user1_id = {user2} AND user2_id = {user1} LIMIT 1")
                .on('user1 -> user1, 'user2 -> user2).as(conversationParser.singleOpt)
        }
    }

    def byUser(userId: Int, limit: Int = 15): Seq[Conversation] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM conversation WHERE user1_id = {user} OR user2_id = {user} ORDER BY updated_time DESC LIMIT {limit}")
                .on('user -> userId, 'limit -> limit).as(conversationParser *)
        }
    }

    def read(conversation: Conversation, userId: Int): Unit = {
        Akka.system.scheduler.scheduleOnce(10.milliseconds) {
            DB.withConnection { implicit connection =>
                if ((conversation.user1Id == userId && conversation.lastUser != 1) || (conversation.user2Id == userId && conversation.lastUser != 2)) {
                    SQL("UPDATE conversation SET `read` = 1 WHERE conversation_id = {conversation_id}").on('conversation_id -> conversation.conversationId.get).executeUpdate()
                }
            }
        }
    }

    def update(sender: Int, receiver: Int, preview: String): Option[Int] = {
        DB.withConnection { implicit connection =>
            val time = System.currentTimeMillis()
            val conversation = Conversation.byUsers(sender, receiver)
            val parsed = {
                if (preview.length > 295) {
                    preview.substring(0, 295)
                } else
                    preview
            }
            if (conversation.isDefined) {
                val last = {
                    if (sender == conversation.get.user1Id)
                        1
                    else
                        2
                }
                SQL("UPDATE conversation SET `read` = 0, updated_time = {time}, preview = {preview}, last_user = {last} WHERE conversation_id = {conversation_id}")
                    .on('conversation_id -> conversation.get.conversationId.get, 'time -> time, 'preview -> parsed, 'last -> last).executeUpdate()
                conversation.get.conversationId
            } else {
                SQL("INSERT INTO conversation (user1_id, user2_id, created_time, updated_time, preview, last_user) VALUES ({user1_id}, {user2_id}, {time}, {time}, {preview}, 1)")
                    .on('user1_id -> sender, 'user2_id -> receiver, 'time -> time, 'preview -> parsed).executeInsert(scalar[Long].singleOpt).map(_.toInt)
            }
        }
    }

    def toJsonSeq(conversations: Seq[Conversation], self: Int): JsValue = {
        Json.toJson(conversations.map{c => toJsonSingle(c, self)})
    }

    def toJsonSingle(c: Conversation, self: Int): JsValue = {
        val user = {
            if (self == c.user1Id)
                User.find(c.user2Id)
            else
                User.find(c.user1Id)
        }
        Json.obj(
            "conversation_id" -> c.conversationId.get,
            "user" -> User.toJson(user.get),
            "create_time" -> c.created,
            "update_time" -> c.updated,
            "read" -> (c.read != 0),
            "last_user" -> c.lastUser,
            "preview" -> c.preview
        )
    }

}
