package com.marble.core.web.controllers

import com.google.inject.Inject
import com.marble.core.data.db.models._
import com.marble.core.web.views.html.desktop.core
import com.marble.utils.play.Auth
import java.util.regex.Pattern
import com.marble.core.data.Constants
import play.api.mvc._
import play.api.libs.json.Json

class BoardController @Inject() (auth: Auth) extends Controller {

    def boardPage(name: String) = AuthAction { implicit user => implicit request =>
        val board = Board.find(name)
        if (board.isDefined) {
            if (name != board.get.name) {
                MovedPermanently("/" + board.get.name)
            } else {
                val posts = Board.getFeed(board.get.boardId.get, user)
                val followingBool = {
                    if (user.isDefined)
                        User.userIsFollowing(user.get.userId.get, board.get.boardId.get)
                    else
                        false
                }
                Ok(core.board(board.get, user, posts)(following = followingBool))
            }
        } else {
            NotFound("Board not found.")
        }
    }

    def followBoard(boardId: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val groupExists = Board.find(boardId)
                if (groupExists.isDefined) {
                    val followExists = User.userIsFollowing(user.get.userId.get, boardId)
                    if (followExists)
                        Ok(Json.obj("success" -> "User is following group."))
                    else{
                        if (Board.addFollower(user.get.userId.get, boardId)) {
                            Ok(Json.obj("success" -> "Add follower successful."))
                        } else {
                            BadRequest(Json.obj("error" -> "Unknown error following group."))
                        }
                    }
                } else {
                    BadRequest(Json.obj("error" -> "Group does not exist."))
                }
        }
    }

    def unfollowBoard(boardId: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User authentication required."))
            case Some(_) =>
                val groupExists = Board.find(boardId)
                if (groupExists.isDefined) {
                    val followExists = User.userIsFollowing(user.get.userId.get, boardId)
                    if (!followExists)
                        Ok(Json.obj("success" -> "User is not following group."))
                    else{
                        if (Board.removeFollower(user.get.userId.get, boardId)) {
                            Ok(Json.obj("success" -> "Remove follower successful."))
                        } else {
                            BadRequest(Json.obj("error" -> "Unknown error unfollowing group."))
                        }
                    }
                } else {
                    BadRequest(Json.obj("error" -> "Group does not exist."))
                }
        }
    }

    def createBoardPage = AuthAction { implicit user => implicit request =>
        user match {
            case None => Redirect("/")
            case Some(_) => Ok(core.create_board(user.get))
        }
    }

    val whiteSpace = Pattern.compile("\\s")

    def attemptCreateBoard = AuthAction { implicit user => implicit request =>
        user match {
            case None => Redirect("/")
            case Some(_) =>
                val body: AnyContent = request.body
                body.asFormUrlEncoded.map { form =>
                    val board_name = form.get("name").map(_.head)
                    val board_descr = form.get("description").map(_.head)
                    val board_type = form.get("board-type").map(_.head).getOrElse("public")
                    if (board_name.isDefined) {
                        if (!Constants.BannedBoards.contains(board_name.get)) {
                            val nameMatcher = whiteSpace.matcher(board_name.get)
                            if (!nameMatcher.find()) {
                                val boardExists = Board.find(board_name.get)
                                if (boardExists.isEmpty) {
                                    val privacy = 0
                                    val boardId = Board.create(board_name.get, board_descr, user.get.userId.get, privacy = privacy)
                                    if (boardId.isDefined) {
                                        val newBoard = Board.find(boardId.get.toInt)
                                        Board.addFollower(user.get.userId.get, newBoard.get.boardId.get)
                                        Redirect("/" + newBoard.get.name)
                                    } else {
                                        Ok(core.create_board(user.get))
                                    }
                                } else {
                                    Ok(core.create_board(user.get, "Board name already exists.", board_name.get, board_descr.getOrElse("")))
                                }
                            } else {
                                Ok(core.create_board(user.get, "Board name may not have spaces", board_name.get, board_descr.getOrElse("")))
                            }
                        } else {
                            Ok(core.create_board(user.get, "Board may not have that name", board_name.get, board_descr.getOrElse("")))
                        }
                    } else {
                        Ok(core.create_board(user.get, "Board needs a name.", "", board_descr.getOrElse("")))
                    }
                }.getOrElse(Ok(core.create_board(user.get, "Unknown error. Please try again later.")))
        }
    }

    def deleteBoard(boardName: String) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User must be authenticated."))
            case Some(_) =>
                if (user.get.admin) {
                    val board = Board.find(boardName)
                    if (board.isDefined) {
                        Board.delete(board.get.boardId.get)
                        Ok(Json.obj("success" -> "Success"))
                    } else {
                        BadRequest(Json.obj("error" -> "Board does not exist."))
                    }
                } else {
                    BadRequest(Json.obj("error" -> "User is not authorized for this action."))
                }
        }
    }

}
