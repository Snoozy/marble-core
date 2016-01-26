package com.marble.core.web.controllers

import com.google.inject.Inject
import com.marble.core.data.aws.S3
import com.marble.core.data.db.models._
import com.marble.utils.play.Auth
import play.api.mvc._
import com.sksamuel.scrimage._


class SettingsController @Inject() (auth: Auth) extends Controller {

    def userSettingsPage = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None => Found("/login")
            case Some(_) => Ok(com.marble.core.web.views.html.desktop.core.settings(user.get))
        }
    }

    def boardSettingsPage(name: String) = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None => Found("/login")
            case Some(_) =>
                val board = Board.find(name)
                if (board.isDefined) {
                    if (board.get.creatorId == user.get.userId.get || user.get.admin) {
                        Ok(com.marble.core.web.views.html.desktop.core.board_settings(user.get, board.get))
                    } else {
                        NotFound("Permission denied.")
                    }
                } else {
                    NotFound("Board not found.")
                }
        }
    }

    def boardSettingsChange(name: String) = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None => Found("/login")
            case Some(_) =>
                val board = Board.find(name)
                if (board.isDefined && (user.get.userId.get == board.get.creatorId || user.get.admin)) {
                    val body = request.body.asMultipartFormData
                    if (body.isDefined) {
                        val res = body.get.asFormUrlEncoded
                        val picID: Int = {
                            val file = body.get.file("picture")
                            if (file.isDefined) {
                                val pic = file.get.ref.file
                                if (pic.length() < 3145728) {
                                    val xForm = res.get("picture-x").map(_.head)
                                    val yForm = res.get("picture-y").map(_.head)
                                    val widthForm = res.get("picture-width").map(_.head)
                                    val heightForm = res.get("picture-height").map(_.head)
                                    val id = {
                                        if (xForm.isDefined && yForm.isDefined && widthForm.isDefined && heightForm.isDefined) {
                                            S3.upload(pic, profile = true, x = xForm.map(_.toDouble), y = yForm.map(_.toDouble), width = widthForm.map(_.toDouble), height = heightForm.map(_.toDouble))
                                        } else {
                                            S3.upload(pic, profile = true)
                                        }
                                    }
                                    if (id.isDefined) {
                                        id.get
                                    } else
                                        board.get.photoId
                                } else
                                    board.get.photoId
                            } else {
                                board.get.photoId
                            }
                        }
                        val desc = res.get("desc").map(_.head).getOrElse(user.get.name)
                        Board.update(board.get.boardId.get, desc, picID)
                    }
                    Found("/" + board.get.name)
                } else {
                    NotFound("Board not found.")
                }
        }
    }

    def userSettingsChange = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None => Found("/login")
            case Some(_) =>
                val body = request.body.asMultipartFormData
                if (body.isDefined) {
                    val res = body.get.asFormUrlEncoded
                    val picID: Int = {
                        val file = body.get.file("picture")
                        if (file.isDefined) {
                            val pic = file.get.ref.file
                            if (pic.length() < 3145728) {
                                val xForm = res.get("picture-x").map(_.head)
                                val yForm = res.get("picture-y").map(_.head)
                                val widthForm = res.get("picture-width").map(_.head)
                                val heightForm = res.get("picture-height").map(_.head)
                                val id = {
                                    if (xForm.isDefined && yForm.isDefined && widthForm.isDefined && heightForm.isDefined) {
                                        S3.upload(pic, profile = true, x = xForm.map(_.toDouble), y = yForm.map(_.toDouble), width = widthForm.map(_.toDouble), height = heightForm.map(_.toDouble))
                                    } else {
                                        S3.upload(pic, profile = true)
                                    }
                                }
                                if (id.isDefined) {
                                    id.get
                                } else
                                    user.get.photoId
                            } else
                                user.get.photoId
                        } else {
                            play.api.Logger.debug("file not found.")
                            user.get.photoId
                        }
                    }
                    val name = res.get("name").map(_.head).getOrElse(user.get.name)
                    val username = res.get("username").map(_.head).getOrElse(user.get.name)
                    val bio = res.get("bio").map(_.head).getOrElse(user.get.name)
                    User.update(user.get.userId.get, name, username, bio, picID)
                    Found("/user/" + username)
                } else {
                    Found("/user/" + user.get.username)
                }
        }
    }

}
