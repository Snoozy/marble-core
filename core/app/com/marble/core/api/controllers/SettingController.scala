package com.marble.core.api.controllers

import com.google.inject.Inject
import com.marble.core.data.db.models._
import com.marble.utils.play.Auth
import play.api.libs.json.Json
import play.api.mvc._
import com.marble.utils.Etc

class SettingController @Inject() (auth: Auth) extends Controller{

    def updateSelf = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User must be authenticated."))
            case Some(_) =>
                val body: AnyContent = request.body
                body.asFormUrlEncoded.map { form =>
                    val name = form.get("name").map(_.head).getOrElse(user.get.name)
                    val username = form.get("username").map(_.head).getOrElse(user.get.username)
                    val bio = form.get("bio").map(_.head).getOrElse(user.get.bio)
                    val pic = {
                        try {
                            form.get("photo").map(_.head).getOrElse(user.get.photoId + "").toInt
                        } catch {
                            case e: java.lang.NumberFormatException => user.get.photoId
                        }
                    }
                    if (User.update(user.get.userId.get, name, username, bio, pic) > 0) {
                        Ok(User.toJsonByUserID(user.get.userId.get, self = user))
                    } else {
                        BadRequest(Json.obj("error" -> "Error updating user information."))
                    }
                }.getOrElse(BadRequest(Json.obj("error" -> "Request content type invalid.")))
        }
    }

    def updatePassword = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User must be authenticated."))
            case Some(_) =>
                val body: AnyContent = request.body
                body.asFormUrlEncoded.map { form =>
                    val currPassword = form.get("current").map(_.head)
                    val newPassword = form.get("new").map(_.head)
                    if (currPassword.isDefined && newPassword.isDefined) {
                        if (Etc.checkPass(currPassword.get, user.get.password)) {
                            if (Etc.checkPasswordValidity(newPassword.get)) {
                                User.updatePassword(user.get.userId.get, newPassword.get)
                                Ok(Json.obj("success" -> "Password successfully changed."))
                            } else {
                                BadRequest(Json.obj("error" -> "Password format is not valid."))
                            }
                        } else {
                            BadRequest(Json.obj("error" -> "Current password incorrect."))
                        }
                    } else {
                        BadRequest(Json.obj("error" -> "Current and new password need to be supplied."))
                    }
                }.getOrElse(BadRequest(Json.obj("error" -> "Request format invalid.")))
        }
    }

    def updateBoard(board_id: Int) = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User must be authenticated."))
            case Some(_) =>
                val board = Board.find(board_id)
                if (board.isDefined && board.get.creatorId == user.get.userId.get) {
                    val body: AnyContent = request.body
                    body.asFormUrlEncoded.map { form =>
                        val descr = form.get("description").map(_.head).getOrElse(board.get.description)
                        var pic = 1
                        try {
                            pic = form.get("picture").map(_.head).getOrElse(board.get.photoId + "").toInt
                        } catch {
                            case e: java.lang.NumberFormatException =>
                        }
                        if (Board.update(board_id, descr, pic) > 0) {
                            Ok(Board.toJsonSingle(Board.find(board_id).get, user, following = Option(true)))
                        } else {
                            BadRequest(Json.obj("error" -> "Error updating board information."))
                        }
                    }.getOrElse(BadRequest(Json.obj("error" -> "Request content type invalid.")))
                } else {
                    BadRequest(Json.obj("error" -> "Invalid request."))
                }
        }
    }

}
