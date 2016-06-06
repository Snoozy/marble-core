package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.marble.core.data.db.models.Enum.{ActionType, EntityType}
import com.marble.utils.EncodeDecode
import play.api.Play.current
import com.marble.core.api.apple.PNController
import com.marble.core.config.MarbleConfig
import play.api.Logger
import play.api.db._
import play.api.libs.json.{JsObject, JsValue, Json}

case class Comment (
    commentId: Option[Int],
    postId: Int,
    userId: Int,
    parentId: Option[Int],
    data: String,
    time: Long,
    votes: Int,
    status: Int,
    media: Seq[Int]
) {
    lazy val root: Boolean = parentId.isEmpty
}

object Comment {

    private[models] val commentParser: RowParser[Comment] = {
        get[Option[Int]]("comment_id") ~
            get[Int]("post_id") ~
            get[Int]("user_id") ~
            get[Option[Int]]("parent_id") ~
            get[String]("data") ~
            get[Long]("time") ~
            get[Int]("votes") ~
            get[Int]("status") ~
            get[String]("media") map {
            case commentId ~ postId ~ userId ~ parentId ~ data ~ time ~ votes ~ status ~ media =>
                val media_ids = media.split("~").filter(_ != "").map(_.toInt)
                Comment(commentId, postId, userId, parentId, data, time, votes, status, media_ids)
        }
    }

//    def parseCommentParents(): Unit = {
//        DB.withConnection { implicit conn =>
//            val comments = SQL("SELECT * FROM comment").as(commentParser *)
//            comments.foreach { comment =>
//                val path = comment.path.split("/")
//                if (path.length > 1) {
//                    val parent = path(1)
//                    SQL("UPDATE comment SET parent_id = {parent} WHERE comment_id = {id}").on('parent -> EncodeDecode.decodeNum(parent), 'id -> comment.commentId.get).executeUpdate()
//                }
//            }
//        }
//    }

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
            val exists = SQL("SELECT comment_id FROM comment WHERE parent_id = {parent} AND user_id = {user_id} LIMIT 2")
                .on('parent -> comment.get.commentId.get, 'user_id -> userId).as(scalar[Int] *)
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
            SQL("SELECT user_id FROM comment WHERE parent_id = {parent} ORDER BY time DESC LIMIT 1").on('parent -> comment.get.parentId.get).as(scalar[Int].singleOpt)
        }
    }

    def create(postId: Int, userId: Int, data: String, parentId: Option[Int], media: Option[Seq[Int]] = None, notif: Boolean = true): Option[Long] = {
        val parent: Option[Comment] = if (parentId.isDefined) Comment.find(parentId.get) else None

        if (parent.isDefined && !parent.get.root) {
            // recurse if parent is not a root node
            create(postId, userId, data, parent.get.parentId, media, notif)
        } else {
            if (notif) {
                val post = Post.find(postId)
                if (post.isDefined) {
                    val board = Board.find(post.get.boardId)
                    PNController.sendNotification(MarbleConfig.SuperUser, "New comment on " + board.get.name)
                }
            }

            val mediaString = {
                if (media.isDefined && media.get.nonEmpty) {
                    media.get.mkString("~")
                } else {
                    ""
                }
            }

            val time = System.currentTimeMillis()

            DB.withConnection { implicit connection =>
                val ret: Option[Long] = {
                    if (parent.isEmpty) {
                        SQL("INSERT INTO comment (post_id, user_id, data, time, votes, media) VALUES ({post_id}, {user_id}, {data}," +
                            "{time}, 0, {media})").on('post_id -> postId, 'user_id -> userId, 'data -> data, 'time -> time, 'media -> mediaString).executeInsert()
                    } else {
                        SQL("INSERT INTO comment (post_id, user_id, parent_id, data, time, votes, media) VALUES ({post_id}, {user_id}, {parent}, {data}," +
                            "{time}, 0, {media})").on('post_id -> postId, 'user_id -> userId, 'parent -> parentId.get, 'data -> data, 'time -> time, 'media -> mediaString).executeInsert()
                    }
                }
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
