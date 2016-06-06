package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.marble.core.data.db.models.Comment.commentParser
import play.api.Play.current
import play.api.db._
import play.api.libs.json.{JsValue, _}

case class CommentTree(roots: Seq[CommentTreeThread], postId: Int)

case class CommentTreeThread(root: Comment, children: Seq[Comment])

object CommentTree {

    def getPostCommentsTop(postId: Int): CommentTree = {
        DB.withConnection { implicit connection =>
            val comments = SQL("SELECT * FROM comment WHERE post_id = {id} AND status = 0").on('id -> postId).as(commentParser *)
            commentsByTop(comments)
        }
    }

    def getPostCommentsTop(post: Post): CommentTree = {
        if (post.repostId.isDefined) {
            getPostCommentsTop(post.repostId.get)
        } else {
            getPostCommentsTop(post.postId.get)
        }
    }

    def getCommentTreeThread(comment: Comment): CommentTree = {
        if (comment.root) {
            DB.withConnection { implicit connection =>
                val comments = SQL("SELECT * FROM comment WHERE parent_id = {parent} AND status = 0").on('parent -> comment.commentId.get).as(commentParser *)
                CommentTree(Seq(CommentTreeThread(comment, comments)), comment.postId)
            }
        } else {
            CommentTree(Seq(), comment.postId)
        }
    }

    def getTopRootComments(postId: Int): Seq[Comment] = {
        DB.withConnection { implicit connection =>
            sortTop(SQL("SELECT * FROM comment WHERE post_id = {id} AND parent_id is NULL AND status = 0 ORDER BY votes desc").on('id -> postId).as(commentParser *))
        }
    }

    def getTopRootComments(post: Post): Seq[Comment] = {
        getTopRootComments(post.repostId.getOrElse(post.postId.get))
    }

    def getCommentNumChildren(commentId: Int): Int = {
        val comment = Comment.find(commentId, status = None)
        if (comment.isDefined && comment.get.root) {
            DB.withConnection { implicit connection =>
                SQL("SELECT COUNT(*) FROM comment WHERE parent_id = {parent}").on('parent -> comment.get.commentId.get).as(scalar[Long].single).toInt
            }
        } else 0
    }

    private def sortTop(comments: Seq[Comment]): Seq[Comment] = {
        val ret = comments.sortBy(- _.votes)
        ret
    }

    private def commentsByTop(comments: Seq[Comment]): CommentTree = {
        if (comments.isEmpty) {
            CommentTree(Seq(), 0)
        } else {
            val filtered = comments.par.filter(_.root)
            val rootComments = sortTop(comments.par.filter(_.root).toVector)
            val roots = rootComments.map { root =>
                val children = sortTop(comments.par.filter((c: Comment) => c.parentId.isDefined && c.parentId.get == root.commentId.get).toVector)
                CommentTreeThread(root, children)
            }
            CommentTree(roots, comments.head.postId)
        }
    }

    def commentTreeJson(tree: CommentTree, user: Option[User], blockedUserIds: Option[Seq[Int]] = None): JsValue = {
        var json = Json.arr()
        val blocked: Seq[Int] = {
            if (blockedUserIds.isEmpty && user.isDefined) {
                UserBlock.getBlockedUserIds(user.get.userId.get)
            } else {
                Seq()
            }
        }
        tree.roots.reverse.foreach { node =>
            val comment = node.root
            val children = Json.arr()
            node.children.foreach { child =>
                json = json.+:(Comment.toJson(child, user = user))
            }
            var newComment: JsValue = Comment.toJson(comment, user = user).as[JsObject] + ("children" -> children)
            if (user.isDefined)
                newComment = newComment.as[JsObject] + ("vote_value" -> Json.toJson(CommentVote.getCommentVoteValue(user.get.userId.get, comment.commentId.get)))
            else
                newComment = newComment.as[JsObject] + ("vote_value" -> Json.toJson(0))
            if (blocked.nonEmpty && blocked.contains(comment.userId)) {
                newComment = newComment.as[JsObject] + ("blocked" -> Json.toJson(1))
            }
            json = json.+:(newComment) // Adds it to json array
        }
        json
    }

}
