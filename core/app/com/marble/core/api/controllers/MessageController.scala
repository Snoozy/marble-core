package com.marble.core.api.controllers

import com.google.inject.Inject
import com.marble.core.data.db.models._
import com.marble.utils.play.Auth
import play.api.mvc._
import play.api.libs.json.Json

class MessageController @Inject() (auth: Auth) extends Controller {

    def getMessages(id: Int) = auth.ApiAuthAction { implicit user => implicit request =>
        val conversation = Conversation.find(id)
        if (conversation.isDefined && (conversation.get.user1Id == user.get.userId.get || conversation.get.user2Id == user.get.userId.get)) {
            val messages = Message.byConversation(id)
            Conversation.read(conversation.get, user.get.userId.get)
            Ok(Json.obj("messages" -> Message.toJsonSeq(messages)))
        } else {
            BadRequest(Json.obj("error" -> "Entity does not exist"))
        }
    }

    def readInbox = auth.ApiAuthAction { implicit user => implicit request =>
        user.get.readInbox()
        Ok(Json.obj("success" -> "success"))
    }

    def getMessagesByUser(id: Int) = auth.ApiAuthAction { implicit user => implicit request =>
        val other = User.find(id)
        if (other.isDefined) {
            val conversation = Conversation.byUsers(user.get.userId.get, other.get.userId.get)
            if (conversation.isDefined && (conversation.get.user1Id == user.get.userId.get || conversation.get.user2Id == user.get.userId.get)) {
                val messages = Message.byConversation(conversation.get.conversationId.get)
                Conversation.read(conversation.get, user.get.userId.get)
                Ok(Json.obj("messages" -> Message.toJsonSeq(messages)))
            } else {
                Ok(Json.obj("messages" -> "[]"))
            }
        } else {
            BadRequest(Json.obj("error" -> "Entity does not exist."))
        }
    }

    def getConversations = auth.ApiAuthAction { implicit user => implicit request =>
        val convos = Conversation.byUser(user.get.userId.get)
        Ok(Json.obj("conversations" -> Conversation.toJsonSeq(convos, user.get.userId.get), "inbox_count" -> user.get.inboxCount))
    }

    def getPaged(conversationId: Int) = auth.ApiAuthAction { implicit user => implicit request =>
        val conversation = Conversation.find(conversationId)
        if (conversation.isDefined && (conversation.get.user1Id == user.get.userId.get || conversation.get.user2Id == user.get.userId.get)) {
            val before = request.getQueryString("before").map(_.toInt)
            val messages = Message.getPagedBefore(conversationId, before.get).reverse
            if (messages.nonEmpty) {
                Ok(Json.obj("messages" -> Message.toJsonSeq(messages)))
            } else {
                Ok(Json.obj("done" -> true))
            }
        } else {
            BadRequest(Json.obj("error" -> "Entity does not exist"))
        }
    }

    def poll(conversationId: Int) = auth.ApiAuthAction { implicit user => implicit request =>
        val conversation = Conversation.find(conversationId)
        if (conversation.isDefined && (conversation.get.user1Id == user.get.userId.get || conversation.get.user2Id == user.get.userId.get)) {
            val after = request.getQueryString("after").map(_.toInt)
            val messages = Message.getPagedAfter(conversationId, after.get).reverse
            if (messages.nonEmpty) {
                Ok(Json.obj("messages" -> Message.toJsonSeq(messages), "preview" -> (if (messages.last.content.length > 50) {
                    messages.last.content.substring(0, 50)
                } else {
                    messages.last.content
                })))
            } else {
                Ok(Json.obj("status" -> "emtpy"))
            }
        } else {
            BadRequest(Json.obj("error" -> "Entity does not exist."))
        }
    }

    def send(id: Int) = auth.ApiAuthAction { implicit user => implicit request =>
        val receiver = User.find(id)
        if (receiver.isDefined) {
            request.body.asFormUrlEncoded.map { form =>
                val content = form.get("content").map(_.head)
                if (content.isDefined) {
                    val msg = Message.create(user.get.userId.get, receiver.get.userId.get, content.get)
                    if (msg.isDefined) {
                        User.addMail(receiver.get.userId.get)
                        Ok(Json.obj("message" -> Message.toJsonSingle(Message.find(msg.get.toInt).get), "preview" -> (if (content.get.length > 50) {
                            content.get.substring(0, 50)
                        } else {
                            content.get
                        })))
                    }
                    else
                        BadRequest(Json.obj("error" -> "Error creating message."))
                } else {
                    BadRequest(Json.obj("error" -> "Content does not exist."))
                }
            }.getOrElse(BadRequest(Json.obj("error" -> "Request format invalid.")))
        } else {
            BadRequest(Json.obj("error" -> "Recipient does not exist."))
        }

    }

}
