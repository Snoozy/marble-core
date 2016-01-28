package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.marble.core.data.db.models.Post.postParser
import play.api.Play
import play.api.Play.current
import play.api.db._
import play.api.libs.json._

case class Board (
    boardId: Option[Int],
    name: String,
    time: Long,
    description: String,
    creatorId: Int,
    followers: Int,
    privacy: Int,
    photo: String,
    photoId: Int
)

object Board {

    private val DefaultPhotoString = "default_group"
    private val ImageURLBase = "https://static.themarble.co/image/"

    private[data] val boardParser: RowParser[Board] = {
        get[Option[Int]]("board_id") ~
            get[String]("name") ~
            get[Long]("time") ~
            get[String]("description") ~
            get[Int]("creator_id") ~
            get[Int]("followers") ~
            get[Int]("privacy") ~
            get[Option[Int]]("photo") map {
            case boardId ~ name ~ time ~ description ~ creatorId ~ followers ~ privacy ~ photo =>
                if (photo.isDefined) {
                    val p = Media.find(photo.get)
                    if (p.isDefined)
                        Board(boardId, name, time, description, creatorId, followers, privacy, ImageURLBase + p.get.mediaName, photo.get)
                    else
                        Board(boardId, name, time, description, creatorId, followers, privacy, ImageURLBase + DefaultPhotoString, 0)
                } else {
                    Board(boardId, name, time, description, creatorId, followers, privacy, ImageURLBase + DefaultPhotoString, 0)
                }

        }
    }

    def find(id: Int):Option[Board] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM board WHERE board_id = {id} LIMIT 1").on('id -> id).as(boardParser.singleOpt)
        }
    }

    def find(name: String): Option[Board] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM board WHERE name = {name}").on('name -> name).as(boardParser.singleOpt)
        }
    }

    def create(name: String, desc: Option[String], creatorId: Int, privacy: Int = 0, photo: Option[Int] = None): Option[Long] = {
        val time = System.currentTimeMillis()

        DB.withConnection { implicit connection =>
            SQL("INSERT INTO `board` (name, description, creator_id, followers, privacy, time, photo) VALUES " +
                "({name}, {desc}, {creator_id}, {followers}, {privacy}, {time}, {photo})").on('name -> name,
                    'desc -> desc.getOrElse(""), 'creator_id -> creatorId, 'followers -> 0, 'privacy -> privacy,
                    'time -> time, 'photo -> photo).executeInsert()
        }
    }

    def delete(boardId: Int) = {
        DB.withConnection { implicit connection =>
            val postIds = Board.getAllPostIds(boardId).map(_.toInt)
            postIds.foreach { p =>
                Post.deletePost(p)
            }
            SQL("DELETE FROM user_to_board WHERE board_id = {board}").on('board -> boardId).executeUpdate()
            SQL("DELETE FROM board WHERE board_id = {board}").on('board -> boardId).executeUpdate()
        }
    }

    def getAllPostIds(boardId: Int): Seq[Long] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT post_id FROM post WHERE board_id = {board}").on('board -> boardId).as(scalar[Long] *)
        }
    }

    def getRecommended(userId: Int, limit: Int = 3): Seq[Board] = {
        DB.withConnection { implicit connection =>
            val boardIds = User.getBoardIDs(userId)
            if (boardIds.nonEmpty) {
                SQL("SELECT * FROM board WHERE board_id NOT IN ({board_ids}) ORDER BY followers DESC LIMIT {limit}")
                    .on('board_ids -> boardIds, 'limit -> limit).as(boardParser *)
            } else {
                SQL("SELECT * FROM board ORDER BY followers DESC LIMIT {limit}")
                    .on('board_ids -> boardIds, 'limit -> limit).as(boardParser *)
            }
        }
    }

    def verifyName(name: String): Boolean = {
        !name.contains(' ')
    }

    def getAll: Seq[Board] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM board").as(boardParser *)
        }
    }

    def update(boardId: Int, desc: String, pic: Int) = {
        DB.withConnection { implicit connection =>
            SQL("UPDATE `board` SET description = {desc}, photo = {pic} WHERE board_id = {board}")
                .on('desc -> desc, 'pic -> pic, 'board -> boardId).executeUpdate()
        }
    }

    def getPostsCount(boardId: Int): Int = {
        DB.withConnection { implicit connection =>
            SQL("SELECT COUNT(*) FROM post WHERE board_id = {board}").on('board -> boardId).as(scalar[Long].single).toInt
        }
    }

    def addFollower(userId: Int, boardId: Int): Boolean = {
        val time = System.currentTimeMillis()

        DB.withConnection { implicit connection =>
            SQL("UPDATE `board` SET followers = followers + 1 WHERE board_id = {board_id}")
                .on('board_id -> boardId).executeUpdate()
            SQL("INSERT INTO user_to_board (user_id, board_id, time) VALUES ({user_id}, {board_id}, {time})")
                .on('user_id -> userId, 'board_id -> boardId, 'time -> time).executeInsert()
        }
        true
    }

    def removeFollower(userId: Int, boardId: Int): Boolean = {
        DB.withConnection { implicit connection =>
            SQL("DELETE FROM user_to_board WHERE user_id = {user_id} AND board_id = {board_id}")
                .on('user_id -> userId, 'board_id -> boardId).executeUpdate()
            SQL("UPDATE `board` SET followers = followers - 1 WHERE board_id = {board_id}")
                .on('board_id -> boardId).executeUpdate()
        }
        true
    }

    val artificialTrending: Seq[Int] = {
        if (Play.isProd) {
            Vector[Int](9, 23, 37, 6, 27, 31)
        } else {
            Seq(13)
        }
    }

    def getTrendingBoards(limit: Int = 10): Seq[Board] = {
        DB.withConnection { implicit connection =>
            val res = SQL("SELECT * FROM board WHERE privacy = 0 ORDER BY followers DESC LIMIT {limit}").on('limit -> limit).as(boardParser *)
            if (artificialTrending.nonEmpty) {
                val art: Seq[Board] = artificialTrending.flatMap(b => Board.find(b))
                (art ++ res.take(limit)).distinct
            } else {
                res.take(limit)
            }
        }
    }

    def getFeed(boardId: Int, user: Option[User], limit: Int = Post.DefaultPageSize): Seq[Post] = {
        DB.withConnection { implicit connection =>
            val blockedUsers = {
                if (user.isDefined) {
                    UserBlock.getBlockedUserIds(user.get.userId.get)
                } else {
                    Seq()
                }
            }
            if (blockedUsers.nonEmpty) {
                SQL("SELECT * FROM post WHERE board_id = {board_id} AND user_id NOT IN ({blocked_users}) ORDER BY time DESC LIMIT {limit}")
                    .on('board_id -> boardId, 'blocked_users -> blockedUsers, 'limit -> limit).as(postParser *)
            } else {
                SQL("SELECT * FROM post WHERE board_id = {board_id} ORDER BY time DESC LIMIT {limit}")
                    .on('board_id -> boardId, 'limit -> limit).as(postParser *)
            }
        }
    }

    def getFeedPaged(boardId: Int, after: Int, userId: Option[Int], limit: Int = Post.DefaultPageSize): Seq[Post] = {
        DB.withConnection { implicit connection =>
            val afterPost = Post.find(after)
            if (afterPost.isDefined) {
                val blockedUsers = {
                    if (userId.isDefined) {
                        UserBlock.getBlockedUserIds(userId.get)
                    } else {
                        Seq()
                    }
                }
                val posts = {
                    if (blockedUsers.nonEmpty) {
                        SQL("SELECT * FROM post WHERE time < {time} AND board_id = {board_id} AND user_id NOT IN ({blocked_ids}) ORDER BY time DESC LIMIT {limit}")
                            .on('board_id -> boardId, 'time -> afterPost.get.time, 'blocked_ids -> blockedUsers, 'limit -> limit).as(postParser *)
                    } else {
                        SQL("SELECT * FROM post WHERE time < {time} AND board_id = {board_id} ORDER BY time DESC LIMIT {limit}")
                            .on('board_id -> boardId, 'time -> afterPost.get.time, 'limit -> limit).as(postParser *)
                    }
                }
                if (posts.length < limit)
                    posts
                else
                    posts.takeRight(limit)
            } else {
                Seq()
            }
        }
    }

    def getTopPosts(boardId: Int, limit: Int = 10, time: Int = 604800000): Seq[Post] = {
        DB.withConnection { implicit connection =>
            val t = System.currentTimeMillis() - time
            SQL("SELECT * FROM post WHERE board_id = {board_id} ORDER BY votes DESC LIMIT {limit}").on('board_id -> boardId, 'limit -> limit, 'time -> t).as(postParser *)
        }
    }

    def toJsonSeq(boards: Seq[Board], following: Option[Boolean] = None, user: Option[User] = None): JsValue = {
        var json = Json.arr()
        boards.reverse.foreach { board =>
            json = json.+:(toJsonSingle(board, user, following = following))
        }
        json
    }

    def toJsonSingle(board: Board, user: Option[User], following: Option[Boolean] = None): JsValue = {
        if (following.isDefined) {
            toJson(board, following.get)
        } else if (user.isDefined) {
            val f = User.userIsFollowing(user.get.userId.get, board.boardId.get)
            toJson(board, f)
        } else {
            toJson(board)
        }

    }

    private def toJson(board: Board, following: Boolean = false): JsValue = {
        Json.obj(
            "name" -> board.name,
            "followers" -> board.followers,
            "board_id" -> board.boardId.get,
            "creator_id" -> board.creatorId,
            "photo" -> board.photo,
            "description" -> board.description,
            "following" -> following
        )
    }

}
