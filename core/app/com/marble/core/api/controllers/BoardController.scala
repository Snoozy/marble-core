package com.marble.core.api.controllers

import com.google.inject.Inject
import com.marble.core.data.db.models.{Board, Post, User}
import com.marble.utils.play.Auth
import play.api.libs.json.Json
import play.api.mvc._
import com.marble.utils.play.errors._

/**
 * Handles everything with boards including:
 *      Describing
 *      Creating
 *      Getting Posts
 */

class BoardController @Inject() (auth: Auth) extends Controller {

    /**
     * Describes a specific board by id.
     *
     * @param board_id Id of board to be described.
     * @return
     */
    def describe(board_id: Int)  = auth.ApiAuthAction { implicit user => implicit request =>
        val board = Board.find(board_id)
        if (board.isDefined)
            Ok(Board.toJsonSingle(board.get, user))
        else
            BadRequest(Json.obj("error" -> "Board does not exist."))
    }

    /**
     * Creates a new board from the supplied parameters:
     *      name
     *      creator_id
     *      description
     *
     * @return Fully hydrated Json of the new board.
     */
    def create = auth.ApiAuthAction { implicit user => implicit request =>
        val body: AnyContent = request.body
        body.asFormUrlEncoded.map { form =>
            val name = form.get("name").map(_.head)
            val descr = form.get("description").map(_.head)
            val photo = form.get("photo").map(_.head.toInt)
            if (name.isDefined && Board.verifyName(name.get)) {
                var board_id: Option[Long] = None
                board_id = Board.create(name.get, descr, user.get.userId.get, photo = photo)
                if (board_id.isDefined) {
                    Board.addFollower(user.get.userId.get, board_id.get.toInt)
                    Ok(Board.toJsonSingle(Board.find(board_id.get.toInt).get, user, following = Option(true)))
                } else {
                    BadRequest(Json.obj("error" -> "Board creation failed."))
                }
            } else {
                BadRequest(BoardNameInvalid.toJson)
            }
        }.getOrElse(BadRequest(Json.obj("error" -> "Request Content-Type invalid.")))
    }

    /**
     * Gets a boards trending posts.
     *
     * @param board_id Board to get trending posts for.
     * @return Fully hydrated posts
     */
    def getBoardTrendingPosts(board_id: Int) = auth.ApiAuthAction { implicit user => implicit request =>
        val board = Board.find(board_id)
        if (board.isEmpty)
            BadRequest(Json.obj("error" -> "Board does not exist."))
        else {
            val after = request.getQueryString("after")
            val posts = {
                if (after.isDefined && after.get != "") {
                    Board.getFeedPaged(board_id, after.get.toInt, user.get.userId)
                } else {
                    Board.getFeed(board_id, user)
                }
            }
            Ok(Json.obj("posts" -> Post.toJsonWithUser(posts.reverse, user)))
        }
    }

    def followBoard(board_id: Int) = auth.ApiAuthAction { implicit user => implicit request =>
        val boardExists = Board.find(board_id)
        if (boardExists.isEmpty)
            BadRequest(Json.obj("error" -> "Board does not exist."))
        else {
            val success = Board.addFollower(user.get.userId.get, board_id)
            if (success)
                Ok(Json.obj("success" -> "Board successfully followed"))
            else
                BadRequest(Json.obj("error" -> "Unknown error."))
        }
    }

    def unfollowBoard(board_id: Int) = auth.ApiAuthAction { implicit user => implicit request =>
        val boardExists = Board.find(board_id)
        if (boardExists.isEmpty)
            BadRequest(Json.obj("error" -> "Board does not exist."))
        else {
            val success = Board.removeFollower(user.get.userId.get, board_id)
            if (success)
                Ok(Json.obj("success" -> "Board successfully followed."))
            else
                BadRequest(Json.obj("error" -> "Unknown error."))
        }
    }

    def getTrending = auth.ApiAuthAction { implicit user => implicit request =>
        val boards = Board.getRecommended(user.get.userId.get, limit = 8)
        Ok(Json.obj("trending" -> Board.toJsonSeq(boards, following = Some(false))))
    }

}
