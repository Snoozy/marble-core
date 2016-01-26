package com.marble.core.web.controllers

import com.google.inject.Inject
import com.marble.core.data.db.models._
import com.marble.utils.play.Auth
import com.marble.core.web.views.html.desktop.core
import play.api.mvc._
import com.marble.core.web.views.html.desktop.components
import play.api.libs.json.Json

class MessageController @Inject() (auth: Auth) extends Controller {

    def inbox = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None =>
                Found("/login")
            case Some(_) =>
                val conversations = Conversation.byUser(user.get.userId.get)
                user.get.readInbox()
                Ok(core.inbox(conversations, user.get))
        }
    }

    def send(id: Int) = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None =>
                BadRequest(Json.obj("error" -> "User must be authenticated"))
            case Some(u) =>
                val receiver = User.find(id)
                if (receiver.isDefined) {
                    request.body.asFormUrlEncoded.map { form =>
                        val content = form.get("content").map(_.head)
                        if (content.isDefined) {
                            val msg = Message.create(u.userId.get, receiver.get.userId.get, content.get)
                            if (msg.isDefined) {
                                User.addMail(receiver.get.userId.get)
                                Ok(Json.obj("item_html" -> components.message(content.get, self = true, msg.get.toInt).toString(), "preview" -> (if (content.get.length > 50) {
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

    def getPaged(conversationId: Int) = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None =>
                BadRequest(Json.obj("error" -> "User must be authenticated"))
            case Some(u) =>
                val conversation = Conversation.find(conversationId)
                if (conversation.isDefined && (conversation.get.user1Id == u.userId.get || conversation.get.user2Id == u.userId.get)) {
                    val before = request.getQueryString("before").map(_.toInt)
                    val messages = Message.getPagedBefore(conversationId, before.get).reverse
                    if (messages.nonEmpty) {
                        val res = messages.map { m =>
                            components.message(m.content, u.userId.get == m.userId, m.messageId.get).toString()
                        }.mkString("")
                        Ok(Json.obj("item_html" -> res))
                    } else {
                        Ok(Json.obj("done" -> true))
                    }
                } else {
                    BadRequest(Json.obj("error" -> "User does not have permission"))
                }
        }
    }

    def getConversation(conversationId: Int) = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None =>
                BadRequest(Json.obj("error" -> "User must be authenticated"))
            case Some(u) =>
                val conversation = Conversation.find(conversationId)
                if (conversation.isDefined && (conversation.get.user1Id == u.userId.get || conversation.get.user2Id == u.userId.get)) {
                    val messages = Message.byConversation(conversation.get.conversationId.get)
                    val res = {
                        if (conversation.get.user1Id == u.userId.get) {
                            components.messages(messages, conversation.get.conversationId.get,
                                User.find(conversation.get.user1Id).get, User.find(conversation.get.user2Id).get)
                        } else {
                            components.messages(messages, conversation.get.conversationId.get,
                                User.find(conversation.get.user2Id).get, User.find(conversation.get.user1Id).get)
                        }
                    }
                    Conversation.read(conversation.get, u.userId.get)
                    Ok(Json.obj("item_html" -> res.toString(), "preview" -> (if (messages.last.content.length > 50) {
                        messages.last.content.substring(0, 50)
                    } else {
                        messages.last.content
                    })))
                } else {
                    BadRequest(Json.obj("error" -> "Entity does not exist"))
                }
        }
    }

    def poll(conversationId: Int) = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None =>
                BadRequest(Json.obj("error" -> "User must be authenticated"))
            case Some(u) =>
                val conversation = Conversation.find(conversationId)
                if (conversation.isDefined && (conversation.get.user1Id == u.userId.get || conversation.get.user2Id == u.userId.get)) {
                    val after = request.getQueryString("after").map(_.toInt)
                    val messages = Message.getPagedAfter(conversationId, after.get).reverse
                    if (messages.nonEmpty) {
                        val res = messages.map { m =>
                            components.message(m.content, u.userId.get == m.userId, m.messageId.get).toString()
                        }.mkString("")
                        Ok(Json.obj("item_html" -> res, "preview" -> (if (messages.last.content.length > 50) {
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
    }

}
