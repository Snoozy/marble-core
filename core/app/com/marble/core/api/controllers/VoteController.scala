package com.marble.core.api.controllers

import com.google.inject.Inject
import com.marble.core.data.db.models._
import com.marble.utils.play.Auth
import play.api.libs.json.Json
import play.api.mvc._

/**
 * Handles all voting both on posts and comments.
 */

class VoteController @Inject() (auth: Auth) extends Controller {

    /**
     * Upvotes a specific comment by id.
     *
     * @param commentId Id of the comment to be upvoted.
     * @return The success/error of the upvote.
     */
    def upvoteComment(commentId: Int) = voteComment(commentId, 1)

    /**
     * Downvotes a specific comment by id.
     *
     * @param commentId Id of the comment to be downvoted.
     * @return The success/error of the downvote.
     */
    def downvoteComment(commentId: Int) = voteComment(commentId, -1)

    /**
     * Upvotes a specific post by id.
     *
     * @param postId Id of the post to be upvoted.
     * @return The success or error of the upvote.
     */
    def upvotePost(postId: Int) = votePost(postId, 1)

    /**
     * Downvotes a specific post.
     *
     * @param postId Id of the post to be downvoted.
     * @return The success or error of the downvote.
     */
    def downvotePost(postId: Int) = votePost(postId, -1)

    def unVotePost(postId: Int) = auth.ApiAuthAction { implicit user => implicit request =>
        PostVote.unVotePost(postId, user.get.userId.get)
        Ok(Json.obj("success" -> "Post unvote successful."))
    }

    def unVoteComment(commentId: Int) = auth.ApiAuthAction { implicit user => implicit request =>
        CommentVote.unVoteComment(commentId, user.get.userId.get)
        Ok(Json.obj("success" -> "Comment unvote successful."))
    }

    /**
     * Votes on a comment.
     *
     * @param commentId Id of comment to be voted on.
     * @param value Value of the vote.
     * @return The success or error of the vote.
     */
    private def voteComment(commentId: Int, value: Int) = auth.ApiAuthAction { implicit user => implicit request =>
        val commentExists = Comment.find(commentId)
        if (commentExists.isDefined) {
            if (CommentVote.voteComment(commentExists.get.commentId.get, user.get.userId.get, value))
                Ok(Json.obj("success" -> "Comment vote successful."))
            else
                BadRequest(Json.obj("error" -> "Request format invalid. Code: 200"))
        } else
            BadRequest(Json.obj("error" -> "Comment does not exist."))
    }

    /**
     * Votes on a post.
     *
     * @param postId Id of the post to be voted on.
     * @param value Value of the vote.
     * @return The success or error of the vote.
     */
    private def votePost(postId: Int, value: Int) = auth.ApiAuthAction { implicit user => implicit request =>
        val postExists = Post.find(postId)
        if (postExists.isDefined) {
            if (PostVote.votePost(postExists.get.postId.get, user.get.userId.get, value))
                Ok(Json.obj("success" -> "Post vote successful."))
            else
                BadRequest(Json.obj("error" -> "Request format invalid."))
        } else
            BadRequest(Json.obj("error" -> "Vote does not exist."))
    }
}
