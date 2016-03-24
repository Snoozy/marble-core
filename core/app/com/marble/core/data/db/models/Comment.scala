package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.marble.core.data.db.models.Enum.{ActionType, EntityType}
import com.marble.utils.EncodeDecode
import play.api.Play.current
import com.marble.core.api.apple.PNController
import com.marble.core.config.MarbleConfig
import play.api.db._
import play.api.libs.json.{JsObject, JsValue, Json}

case class Comment (
    commentId: Option[Int],
    postId: Int,
    userId: Int,
    data: String,
    time: Long,
    path: String,
    votes: Int,
    status: Int
)

object Comment {

    private[models] val commentParser: RowParser[Comment] = {
        get[Option[Int]]("comment_id") ~
            get[Int]("post_id") ~
            get[Int]("user_id") ~
            get[String]("data") ~
            get[Long]("time") ~
            get[String]("path") ~
            get[Int]("votes") ~
            get[Int]("status") map {
            case commentId ~ postId ~ userId ~ data ~ time ~ path ~ votes ~ status =>
                Comment(commentId, postId, userId ,data, time, path, votes, status)
        }
    }

    def find(id: Int, status: Option[Int] = Some(0)): Option[Comment] = {
        DB.withConnection { implicit connection =>
            if (status.isDefined) {
                SQL("SELECT * FROM comment WHERE comment_id = {id} AND status = {status}").on('id -> id, 'status -> status.get).as(commentParser.singleOpt)
            } else {
                SQL("SELECT * FROM comment WHERE comment_id = {id}").on('id -> id).as(commentParser.singleOpt)
            }
        }
    }

    def delete(id: Int): Boolean = {
        DB.withConnection { implicit connection =>
            val comment = Comment.find(id)
            if (comment.isDefined) {
                val res = SQL("UPDATE comment SET status = 1 WHERE comment_id = {id}").on('id -> id).executeUpdate()
                if (res > 0)
                    Notification.removeListener(id, EntityType.Comment, comment.get.userId)
                res > 0
            } else
                false
        }
    }

    def userHasCommented(userId: Int, commentId: Int): Boolean = {
        DB.withConnection { implicit connection =>
            val comment = Comment.find(commentId, status = None)
            val path = comment + "/" + EncodeDecode.encodeNum(commentId)
            val exists = SQL("SELECT comment_id FROM comment WHERE path = {path} AND user_id = {user_id} LIMIT 2").on('path -> (path + "%"), 'user_id -> userId).as(scalar[Int] *)
            exists.length > 1
        }
    }

    def mostRecentVoter(commentId: Int): Option[Int] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT user_id FROM comment_vote WHERE comment_id = {comment_id} ORDER BY time DESC LIMIT 1").on('comment_id -> commentId).as(scalar[Int].singleOpt)
        }
    }

    def mostRecentReplier(commentId: Int): Option[Int] = {
        DB.withConnection { implicit connection =>
            val comment = Comment.find(commentId)
            val path = comment.get.path + "/" + EncodeDecode.encodeNum(commentId)
            SQL("SELECT user_id FROM comment WHERE path = {path} ORDER BY time DESC LIMIT 1").on('path -> path).as(scalar[Int].singleOpt)
        }
    }

    def create(postId: Int, userId: Int, data: String, parentId: Option[Int]): Option[Long] = {
        val path = parentId match {
            case None => ""
            case Some(_) =>
                val parent = Comment.find(parentId.get, status = None).getOrElse(return None)
                parent.path + "/" + EncodeDecode.encodeNum(parent.commentId.get)
        }

        val post = Post.find(postId)
        if (post.isDefined) {
            val board = Board.find(post.get.boardId)
            PNController.sendNotification(MarbleConfig.SuperUser, "New comment on " + board.get.name)
        }

        val time = System.currentTimeMillis()

        DB.withConnection { implicit connection =>
            val ret: Option[Long] = SQL("INSERT INTO comment (post_id, user_id, data, path, time, votes) VALUES ({post_id}, {user_id}, {data}," +
                "{path}, {time}, 0)").on('post_id -> postId, 'user_id -> userId, 'path -> path, 'data -> data, 'time -> time).executeInsert()
            if (ret.isDefined) {
                SQL("UPDATE post SET comment_count = comment_count + 1 WHERE post_id = {post_id}")
                    .on('post_id -> postId).executeUpdate()
                Notification.addListener(ret.get.toInt, EntityType.Comment, userId)
                Notification.create(postId, EntityType.Post, ActionType.Reply, userId)
                if (parentId.isDefined) {
                    Notification.create(parentId.get, EntityType.Comment, ActionType.Reply, userId)
                }
            }
            ret
        }
    }

    def toJson(comment: Comment, user: Option[User] = None): JsValue = {
        Json.obj(
            "comment_id" -> comment.commentId.get,
            "user" -> User.toJsonByUserID(comment.userId, self = user),
            "content" -> (if(comment.status != 1) Json.toJson(comment.data) else Json.toJson("")),
            "time" -> Json.toJson(comment.time),
            "votes" -> Json.toJson(comment.votes),
            "deleted" -> Json.toJson(comment.status == 1)
        )
    }

    def toJsonSeqWithUser(comments: Seq[Comment], user: Option[User]): JsValue = {
        var json = Json.arr()
        comments.foreach { comment =>
           json = json.+:(toJson(comment, user).as[JsObject] + ("post" -> Post.toJsonSingle(Post.find(comment.postId).get, user)))
        }
        json
    }

}
