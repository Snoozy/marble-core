package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import play.api.Play.current
import play.api.db._
import play.api.libs.json.JsValue
import play.api.libs.json.Json

case class Message (
    messageId: Option[Int],
    conversationId: Int,
    userId: Int,
    content: String,
    time: Long
)

object Message {

    private[data] val messageParser: RowParser[Message] = {
        get[Option[Int]]("message_id") ~
            get[Int]("conversation_id") ~
            get[Int]("user_id") ~
            get[String]("content") ~
            get[Long]("time") map {
            case messageId ~ conversationId ~ userId ~ content ~ time =>
                Message(messageId, conversationId, userId, content, time)
        }
    }
    
    def find(id: Int): Option[Message] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM message WHERE message_id = {message_id} LIMIT 1").on('message_id -> id).as(messageParser.singleOpt)
        }
    }
    
    def byConversation(id: Int, limit: Int = 30): Seq[Message] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM message WHERE conversation_id = {conversation_id} ORDER BY time DESC LIMIT {limit}").on('conversation_id -> id, 'limit -> limit).as(messageParser *).reverse
        }
    }

    def getPagedBefore(conversationId: Int, before: Int, limit: Int = 20): Seq[Message] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM message WHERE conversation_id = {conversation_id} AND message_id < {before} ORDER BY message_id DESC LIMIT {limit}")
                .on('conversation_id -> conversationId, 'before -> before, 'limit -> limit).as(messageParser *)
        }
    }

    def getPagedAfter(conversationId: Int, after: Int): Seq[Message] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM message WHERE conversation_id = {conversation_id} AND message_id > {after} ORDER BY message_id DESC")
                .on('conversation_id -> conversationId, 'after -> after).as(messageParser *)
        }
    }
    
    def create(sender: Int, receiver: Int, content: String): Option[Long] = {
        DB.withConnection { implicit connection =>
            val time = System.currentTimeMillis()
            val conversation = Conversation.update(sender, receiver, content)
            if (conversation.isDefined) {
                SQL("INSERT INTO message (conversation_id, user_id, content, time) VALUES ({conversation_id}, {sender}, {content}, {time})")
                    .on('conversation_id -> conversation.get, 'sender -> sender, 'content -> content, 'time -> time).executeInsert()
            } else {
                None
            }
        }
    }

    def toJsonSeq(msgs: Seq[Message]): JsValue = {
        Json.toJson(msgs.map{m => toJsonSingle(m)})
    }

    def toJsonSingle(msg: Message): JsValue = {
        Json.obj(
            "message_id" -> msg.messageId.get,
            "conversation_id" -> msg.conversationId,
            "user_id" -> msg.userId,
            "content" -> msg.content,
            "time" -> msg.time
        )
    }

}
