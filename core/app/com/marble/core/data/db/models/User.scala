package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.google.inject.Inject
import com.marble.core.data.cache.Session
import com.marble.core.data.db.models.Comment.commentParser
import com.marble.core.data.db.models.Post.postParser
import com.marble.utils.Etc.makeDigest
import play.api.Play.current
import com.marble.core.data.Constants
import org.apache.commons.lang3.RandomStringUtils
import play.api.db._
import play.api.libs.json._
import com.marble.core.data.cache.Cache

case class User @Inject() (
    userId: Option[Int],
    username: String,
    name: String,
    photo: String,
    photoId: Int,
    cache: Cache,
    token: Option[String] = None,
    session: Option[Session] = None
) {
    lazy val admin: Boolean = {
        if (token.isDefined) {
            new Session(cache)(token.get).get("admin").isDefined
        } else
            false
    }

    lazy val userInfo = UserInfo.find(userId.get).get

    val password = userInfo.password
    val email = userInfo.email
    val time = userInfo.time
    val reputation = userInfo.reputation
    val bio = userInfo.bio
    var inboxCount = userInfo.inboxCount

    def readInbox() = {
        DB.withConnection { implicit connection =>
            SQL("UPDATE user_info SET inbox_count = 0 WHERE user_id = {user_id}").on('user_id -> userId.get).executeUpdate()
            this.inboxCount = 0
        }
    }
}

object User {

    private val DefaultPhotoString = "default"
    private val ImageURLBase = "https://static.themarble.co/image/"

    @Inject val cache: Cache = null

    private[data] val userParser: RowParser[User] = {
        get[Option[Int]]("user_id") ~
            get[String]("username") ~
            get[String]("name") ~
            get[Option[Int]]("photo") ~
            get[Option[String]]("photo_name") map {
            case userId ~ username ~ name ~ photo ~ photoName =>
                if (photo.isDefined && photoName.isDefined) {
                    User(userId, username, name, ImageURLBase + photoName.get, photo.get, cache)
                } else {
                    User(userId, username, name, ImageURLBase + DefaultPhotoString, 0, cache)
                }
        }
    }

    def find(id: Int): Option[User] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM user WHERE user_id = {id}").on('id -> id).as(userParser.singleOpt)
        }
    }

    def addMail(id: Int): Unit = {
        DB.withConnection { implicit connection =>
            SQL("UPDATE user_info SET inbox_count = inbox_count + 1 WHERE user_id = {id}").on('id -> id).executeUpdate()
        }
    }

    def isUserAdmin(userId: Int): Boolean = {
        DB.withConnection { implicit connection =>
            val admin = SQL("SELECT * FROM admin WHERE user_id = {user}").on('user -> userId).as(scalar[Long].singleOpt)
            admin.isDefined
        }
    }

    def getAll: Seq[User] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM user").as(userParser *)
        }
    }

    def find(username: String): Option[User] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM user WHERE username = {username}").on('username -> username).as(userParser.singleOpt)
        }
    }

    def findByEmail(email: String): Option[User] = {
        DB.withConnection { implicit connection =>
            val user = SQL("SELECT user_id FROM user_info WHERE email = {email}").on('email -> email).as(scalar[Long].singleOpt)
            if (user.isDefined) {
                User.find(user.get.toInt)
            } else {
                None
            }
        }
    }

    def checkUsername(s: String): Boolean = find(s).isEmpty

    def checkEmail(s: String): Boolean = findByEmail(s).isEmpty

    /**
     * Generates valid username from valid email
     *
     * @param s Valid email
     * @return Valid username for user.
     */
    def genUsername(s: String, backup: String = ""): String = {
        val gen = genRawUsername(s)
        if (gen.isEmpty) {
            if (backup != "") {
                val b = genRawUsername(backup)
                if (b.isEmpty) {
                    genRawUsername(RandomStringUtils.random(14)).get
                } else
                    b.get
            } else
                genRawUsername(RandomStringUtils.random(14)).get
        } else
            gen.get
    }

    private def genRawUsername(s: String): Option[String] = {
        val raw = s.replace(" ", "")
        val i = {
            val t = raw.indexOf('@')
            if (t > 0 && t < (Constants.MaxUsernameLength + 1)) {
                t
            } else {
                Constants.MaxUsernameLength
            }
        }
        val parsed = raw.substring(0, i)
        if (!checkUsername(parsed)) {
            var count = 0
            val rand = "%04d".format(scala.util.Random.nextInt(1000))
            var check = parsed
            if (check.length > (Constants.MaxUsernameLength - 1)) {
                check = check.substring(0, Constants.MaxUsernameLength - 4)
            }
            check = check + rand
            while (!checkUsername(check)) {
                if (count > 4)
                    return None
                check = check.substring(0, Constants.MaxUsernameLength - 4) + "%04d".format(scala.util.Random.nextInt(1000))
                count += 1
            }
            Some(check)
        } else Some(parsed)
    }

    def create(username: String, name: String, password: String, email: String, bio: Option[String] = None, pic: Option[Int] = None): Option[Long] = {

        if (checkEmail(email)) {
            DB.withConnection { implicit connection =>
                val picName: Option[String] = {
                    if (pic.isDefined)
                        Some(Media.find(pic.get).get.mediaName)
                    else
                        None
                }
                val user: Option[Long] = SQL("INSERT INTO user (username, name, photo, photo_name) VALUES ({username}, {name}, {pic}, {picName})").on('username -> username, 'name -> name,
                        'pic -> pic, 'picName -> picName).executeInsert()
                UserInfo.create(user.get.toInt, password, email, bio)
                user
            }
        } else
            None
    }

    def update(userId: Int, name: String, username: String, bio: String, pic: Int) = {
        DB.withConnection { implicit connection =>
            val media = Media.find(pic)
            val mediaName: Option[String] = {
                if (media.isDefined) {
                    Some(media.get.mediaName)
                } else {
                    None
                }
            }
            SQL("UPDATE user SET name = {name}, username = {username}, photo = {photo}, photo_name = {photoName} WHERE user_id = {user}")
                .on('name -> name, 'photo -> pic, 'user -> userId, 'username -> username, 'photoName -> mediaName).executeUpdate()
            SQL("UPDATE user_info SET bio = {bio} WHERE user_id = {user}").on('bio -> bio, 'user -> userId).executeUpdate()
        }
    }

    def updatePassword(userId: Int, password: String) = {
        DB.withConnection { implicit connection =>
            val pass = makeDigest(password)
            SQL("UPDATE user_info SET password = {pass} WHERE user_id = {user}").on('pass -> pass, 'user -> userId).executeUpdate()
        }
    }

    def getPosts(userId: Int, limit: Int = Post.DefaultPageSize): Seq[Post] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM post WHERE user_id = {id} ORDER BY time DESC LIMIT {limit}")
                .on('id -> userId, 'limit -> limit).as(postParser *)
        }
    }

    def getPostsPaged(userId: Int, after: Int, limit: Int = Post.DefaultPageSize): Seq[Post] = {
        DB.withConnection { implicit connection =>
            val afterPost = Post.find(after)
            if (afterPost.isDefined) {
                val posts = SQL("SELECT * FROM post WHERE time < {time} AND user_id = {id} ORDER BY time DESC LIMIT {limit}")
                    .on('id -> userId, 'time -> afterPost.get.time, 'limit -> limit).as(postParser *)
                if (posts.length < limit)
                    posts
                else
                    posts.takeRight(limit)
            } else {
                Seq()
            }
        }
    }

    def getFeed(userId: Int, limit: Int = Post.DefaultPageSize, boardIds: Option[Seq[Int]] = None): Seq[Post] = {
        DB.withConnection { implicit connection =>
            val boards = {
                if (boardIds.isDefined)
                    boardIds.get
                else
                    User.getBoardIDs(userId)
            }
            if (boards.nonEmpty) {
                val blockedUsers = UserBlock.getBlockedUserIds(userId)
                if (blockedUsers.nonEmpty) {
                    SQL("SELECT * FROM post WHERE board_id IN ({board_ids}) AND user_id NOT IN ({blocked_ids}) ORDER BY time DESC LIMIT {limit}")
                        .on('board_ids -> boards, 'blocked_ids -> blockedUsers, 'limit -> limit).as(postParser *)
                } else {
                    SQL("SELECT * FROM post WHERE board_id IN ({board_ids}) ORDER BY time DESC LIMIT {limit}")
                        .on('board_ids -> boards, 'limit -> limit).as(postParser *)
                }
            } else {
                Seq[Post]()
            }
        }
    }

    def getFeedPaged(userId: Int, after: Int, limit: Int = Post.DefaultPageSize): Seq[Post] = {
        DB.withConnection { implicit connection =>
            val boardIds = User.getBoardIDs(userId)
            if (boardIds.isEmpty)
                Seq()
            else {
                val afterPost = Post.find(after)
                if (afterPost.isDefined) {
                    val posts = {
                        val blockedUsers = UserBlock.getBlockedUserIds(userId)
                        if (blockedUsers.nonEmpty) {
                            SQL("SELECT * FROM post WHERE time < {time} AND board_id IN ({board_ids}) AND user_id NOT IN ({blocked_ids}) ORDER BY time DESC LIMIT {limit}")
                                .on('board_ids -> boardIds, 'time -> afterPost.get.time, 'blocked_ids -> blockedUsers, 'limit -> limit).as(postParser *)
                        } else {
                            SQL("SELECT * FROM post WHERE time < {time} AND board_id IN ({board_ids}) ORDER BY time DESC LIMIT {limit}")
                                .on('board_ids -> boardIds, 'time -> afterPost.get.time, 'limit -> limit).as(postParser *)
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
    }

    def getComments(userId: Int, limit: Int = Post.DefaultPageSize): Seq[Comment] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM comment WHERE user_id = {user_id} ORDER BY time DESC LIMIT {limit}").on('user_id -> userId, 'limit -> limit).as(commentParser *)
        }
    }

    def getCommentsPaged(userId: Int, after: Int, limit: Int = Post.DefaultPageSize): Seq[Comment] = {
        DB.withConnection { implicit connection =>
            val afterCom = Comment.find(after)
            if (afterCom.isDefined) {
                val comments = SQL("SELECT * FROM comment WHERE time < {time} AND user_id = {id} ORDER BY time DESC LIMIT {limit}")
                    .on('id -> userId, 'time -> afterCom.get.time, 'limit -> limit).as(commentParser *)
                if (comments.length < limit)
                    comments
                else
                    comments.takeRight(limit)
            } else {
                Seq()
            }
        }
    }

    def userIsFollowing(userId: Int, boardId: Int): Boolean = {
        DB.withConnection { implicit connection =>
            val count = SQL("SELECT COUNT(*) FROM user_to_board WHERE user_id = {user_id} AND board_id = {board_id}")
                .on('user_id -> userId, 'board_id -> boardId).as(scalar[Long].single)
            count > 0L
        }
    }

    def getPostsCount(userId: Int): Int = {
        DB.withConnection { implicit connection =>
            SQL("SELECT COUNT(*) FROM post WHERE user_id = {user}").on('user -> userId).as(scalar[Long].single).toInt
        }
    }

    def toJsonByUserID(userId: Int, self: Option[User] = None, email: Boolean = false): JsValue = {
        val userExists = User.find(userId)
        if (userExists.isEmpty)
            Json.obj("error" -> "User does not exist.")
        else {
            val user = userExists.get
            toJson(user, self, email)
        }
    }

    def getBoardIDs(userId: Int): Seq[Int] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT board_id FROM user_to_board WHERE user_id = {id}").on('id -> userId).as(int("board_id") *)
        }
    }

    def getBoards(userId: Int): Seq[Board] = {
        DB.withConnection { implicit connection =>
            val boardIDs = User.getBoardIDs(userId)
            if (boardIDs.isEmpty)
                return List()
            else
                SQL("SELECT * FROM board WHERE board_id IN ({board_ids})").on('board_ids -> boardIDs).as(Board.boardParser *)
        }
    }

    def toJsonByUsername(username: String, self: Option[User] = None, email: Boolean = false): JsValue = {
        val userExists = User.find(username)
        if (userExists.isEmpty)
            Json.obj("error" -> "User does not exist.")
        else {
            val user = userExists.get
            toJson(user, self, email)
        }
    }

    def toJson(user: User, self: Option[User] = None, email: Boolean = false): JsValue = {
        var newJson = Json.obj()
        newJson = newJson.as[JsObject] +
            ("name" -> Json.toJson(user.name)) +
            ("username" -> Json.toJson(user.username)) +
            ("user_id" -> Json.toJson(user.userId)) +
            ("reputation" -> Json.toJson(user.reputation)) +
            ("photo" -> Json.toJson(user.photo)) +
            ("bio" -> Json.toJson(user.bio)) +
            ("board_count" -> Json.toJson(User.getBoardIDs(user.userId.get).size))
        if (email) {
            newJson = newJson.as[JsObject] + ("email" -> Json.toJson(user.email))
        }
        if (self.isDefined && user.userId.get == self.get.userId.get)
            newJson = newJson.as[JsObject] + ("self" -> Json.toJson(true))
        else
            newJson = newJson.as[JsObject] + ("self" -> Json.toJson(false))
        newJson
    }

}
