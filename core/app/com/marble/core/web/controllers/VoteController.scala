package com.marble.core.web.controllers

import com.marble.core.data.db.models._
import com.marble.utils.play.Auth.AuthAction
import play.api.libs.json.Json
import play.api.mvc._

class VoteController extends Controller {

    def upvotePost(postId: Int) = votePost(postId, 1)

    def downvotePost(postId: Int) = votePost(postId, -1)

    def upvoteComment(commentId: Int) = voteComment(commentId, 1)

    def downvoteComment(commentId: Int) = voteComment(commentId, -1)

    private def votePost(postId: Int, value: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User must be authenticated"))
            case Some(_) =>
                val postExists = Post.find(postId)
                if (postExists.isDefined && PostVote.votePost(postExists.get.postId.get, user.get.userId.get, value)) {
                    Ok(Json.obj("success" -> "Yea!"))
                } else {
                    BadRequest(Json.obj("error" -> "Vote error."))
                }
        }
    }

    private def voteComment(commentId: Int, value: Int) = AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest(Json.obj("error" -> "User must be authenticated"))
            case Some(_) =>
                val commExists = Comment.find(commentId)
                if (commExists.isDefined && CommentVote.voteComment(commExists.get.commentId.get, user.get.userId.get, value)) {
                    Ok(Json.obj("success" -> "Yea!"))
                } else {
                    BadRequest(Json.obj("error" -> "Vote error."))
                }
        }
    }

}
