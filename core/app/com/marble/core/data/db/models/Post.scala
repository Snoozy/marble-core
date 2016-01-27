package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.marble.core.data.db.models.Comment.commentParser
import com.marble.core.data.db.models.Enum.EntityType
import com.marble.utils.Etc.{bool2int, int2bool}
import com.marble.core.web.views.html.desktop.components
import com.marble.core.data.Constants
import play.api.Play.current
import play.api.db._
import play.api.libs.json._

import scala.util.Random

case class Post (
    postId: Option[Int],
    userId: Int,
    data: String,
    boardId: Int,
    repostId: Option[Int],
    votes: Int,
    commentCount: Int,
    time: Long,
    media: Seq[Int]
)

object Post {

    val DefaultPageSize = 10

    private[models] val postParser: RowParser[Post] = {
        get[Option[Int]]("post_id") ~
            get[Int]("user_id") ~
            get[String]("data") ~
            get[Int]("board_id") ~
            get[Option[Int]]("repost_id") ~
            get[Int]("votes") ~
            get[Int]("comment_count") ~
            get[Long]("time") ~
            get[String]("media") map {
            case postId ~ userId ~ data ~ boardId ~ repostId ~ votes ~ commentCount ~ time ~ media =>
                val media_ids = media.split("~").filter(_ != "").map(_.toInt)
                Post(postId, userId, data, boardId, repostId, votes, commentCount, time, media_ids)
        }
    }

    def find(id: Int): Option[Post] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM post WHERE post_id = {id}").on('id -> id).as(postParser.singleOpt)
        }
    }

    def getAll: Seq[Post] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM post").as(postParser *)
        }
    }

    def createSimplePost(userId: Int, data: String, boardId: Int, repostId: Option[Int] = None, time: Long = System.currentTimeMillis()): Option[Long] = {
        // checking that data is a number if post is a repost
        if (repostId.isDefined) {
            val repostExists = Post.find(repostId.get)
            if (repostExists.isEmpty)
                return None
        }

        DB.withConnection { implicit connection =>
            val id: Option[Long] = SQL("INSERT INTO post (user_id, data, board_id, repost_id, votes, time, comment_count) VALUES ({user_id}, {data}," +
                " {board_id}, {repost_id}, 0, {time}, 0)").on('user_id -> userId, 'data -> data,
                    'board_id -> boardId, 'repost_id -> repostId, 'time -> time).executeInsert()
            if (id.isDefined) {
                Notification.addListener(id.get.toInt, EntityType.Post, userId)
            }
            id
        }
    }

    def createMediaPost(userId: Int, data: String, boardId: Int, mediaIds: Seq[Int], time: Long = System.currentTimeMillis()): Option[Long] = {

        mediaIds.foreach(id => if (Media.find(id).isEmpty) return None)

        val mediaString = mediaIds.mkString("~")

        DB.withConnection { implicit connection =>
            val id: Option[Long] = SQL("INSERT INTO post (user_id, data, board_id, votes, time, comment_count, media) values ({user_id}, {data}," +
                " {board_id}, 0, {time}, 0, {media})").on('user_id -> userId, 'data -> data,
                    'board_id -> boardId, 'time -> time, 'media -> mediaString).executeInsert()
            if (id.isDefined) {
                Notification.addListener(id.get.toInt, EntityType.Post, userId)
            }
            id
        }
    }

    def deletePost(postId: Int): Boolean = {
        DB.withConnection { implicit connection =>
            val commentIds: Seq[Int] = SQL("SELECT comment_id FROM comment WHERE post_id = {post_id}").on('post_id -> postId).as(scalar[Int].*)
            if (commentIds.nonEmpty) {
                SQL("DELETE FROM comment_vote WHERE comment_id IN ({ids})").on('ids -> commentIds).executeUpdate()
            }
            val reposts = SQL("SELECT post_id FROM post WHERE repost_id = {post_id}").on('post_id -> postId).as(scalar[Int].*)
            reposts.foreach(deletePost)
            SQL("DELETE FROM comment WHERE post_id = {post_id}").on('post_id -> postId).executeUpdate()
            SQL("DELETE FROM post_vote WHERE post_id = {post_id}").on('post_id -> postId).executeUpdate()
            SQL("DELETE FROM post WHERE post_id = {post_id}").on('post_id -> postId).executeUpdate()
        }
    }

    def mostRecentVoter(postId: Int): Option[Int] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT user_id FROM post_vote WHERE post_id = {post_id} ORDER BY time DESC LIMIT 1").on('post_id -> postId).as(scalar[Int].singleOpt)
        }
    }
    
    def mostRecentReplier(postId: Int): Option[Int] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT user_id FROM comment WHERE post_id = {post_id} ORDER BY time DESC LIMIT 1").on('post_id -> postId).as(scalar[Int].singleOpt)
        }
    }

    def userHasReposted(userId: Int, postId: Int): Boolean = {
        DB.withConnection { implicit connection =>
            val post = SQL("SELECT * FROM post WHERE repost_id = {post} AND user_id = {user}")
                .on('post -> postId, 'user -> userId).as(postParser *)
            post.nonEmpty
        }
    }

    def getRootComments(postId: Int, limit: Int = 5): Seq[Comment] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM comment WHERE post_id = {post} AND path = '' ORDER BY votes LIMIT {LIMIT}")
                .on('post -> postId, 'limit -> limit).as(commentParser *)
        }
    }

    def toJson(posts: Seq[Post]): JsValue = {
        toJsonWithUser(posts, None)
    }

    def toJsonSingle(post: Post, user: Option[User], following: Option[Boolean] = None): JsValue = {
        var newPost = Json.obj(
            "post_id" -> Json.toJson(post.postId)
        )

        if (post.repostId.isDefined) {
            val repostedPost = Post.find(post.repostId.get)
            if (repostedPost.isDefined) {
                val board = Board.find(post.boardId)
                val poster = User.find(repostedPost.get.userId)
                val reposter = User.find(post.userId)
                val repostBoard = Board.find(repostedPost.get.boardId)
                if (board.isDefined && poster.isDefined && reposter.isDefined && repostBoard.isDefined) {
                    newPost = newPost.as[JsObject] +
                        ("repost" -> Post.toJsonSingle(repostedPost.get, user)) +
                        ("content" -> Json.toJson(post.data)) +
                        ("board" -> Board.toJsonSingle(board.get, user, following = following)) +
                        ("user" -> User.toJson(reposter.get, self = user)) +
                        ("time" -> Json.toJson(post.time)) +
                        ("votes" -> Json.toJson(post.votes)) +
                        ("comment_count" -> Json.toJson(post.commentCount))
                }
            }
        } else {
            val board = Board.find(post.boardId)
            val poster = User.find(post.userId)
            if (board.isDefined && poster.isDefined) {
                newPost = newPost.as[JsObject] +
                    ("content" -> Json.toJson(post.data)) +
                    ("board" -> Board.toJsonSingle(board.get, user, following = following)) +
                    ("user" -> User.toJson(poster.get, self = user)) +
                    ("time" -> Json.toJson(post.time)) +
                    ("votes" -> Json.toJson(post.votes)) +
                    ("comment_count" -> Json.toJson(post.commentCount))
            }
        }

        if (user.isDefined) {
            newPost = newPost.as[JsObject] + ("vote_value" -> Json.toJson(PostVote.getPostVoteValue(post.postId.get, user.get.userId.get)))
        }
        if (post.media.nonEmpty) {
            val mediaArr = post.media.map { id =>
                val media = Media.find(id)
                if (media.isDefined) {
                    Some(Json.toJson(Media.BaseMediaURL + media.get.mediaName + (if(media.get.mediaType != 1){"_med"}else{""})))
                } else {
                    None
                }
            }.filter(_.isDefined)
            newPost = newPost.as[JsObject] + ("media" -> Json.toJson(mediaArr))
        }
        newPost
    }

    def getTrendingPosts: Seq[Post] = {
        Random.shuffle(Constants.FrontBoards.flatMap { b =>
            val board = Board.find(b)
            if (board.isDefined) {
                Board.getTopPosts(board.get.boardId.get)
            } else {
                Seq()
            }
        })
    }

    def toJsonWithUser(posts: Seq[Post], user: Option[User], following: Option[Boolean] = None): JsValue = {
        var json = Json.arr()
        posts.foreach { post =>
            json = json.+:(toJsonSingle(post, user, following = following))
        }
        json
    }

    def toHTMLWIthUser(posts: Seq[Post], user: Option[User]): String = {
        posts.map { post =>
            components.post(post, user)()
        }.mkString("")
    }

}
