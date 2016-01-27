package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.marble.core.data.db.models.Enum._
import com.marble.utils.Etc
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent.Akka
import play.api.libs.json.JsValue
import scala.concurrent.duration._
import play.api.db._
import play.api.libs.json.Json

case class Notification (
    notificationId: Option[Int],
    entityId: Int,
    entityType: EntityType,
    actionType: ActionType,
    titleUser: Int,
    userId: Int,
    count: Int,
    read: Boolean,
    time: Long
)

object Notification {

    private[data] val notificationParser: RowParser[Notification] = {
        get[Option[Int]]("notification_id") ~
            get[Int]("entity_id") ~
            get[Int]("entity_type") ~
            get[Int]("title_user") ~
            get[Int]("action_type") ~
            get[Int]("count") ~
            get[Int]("user_id") ~
            get[Int]("read") ~
            get[Long]("time") map {
            case notificationId ~ entityId ~ entityType ~ titleUser ~ actionType ~ count ~ userId ~ read ~ time =>
                Notification(notificationId, entityId, EntityType.fromInt(entityType).get, ActionType.fromInt(actionType).get, titleUser, userId, count, read != 0, time)
        }
    }

    def addListener(entityId: Int, entityType: EntityType, userId: Int): Unit = {
        Akka.system.scheduler.scheduleOnce(10.milliseconds) {
            DB.withConnection { implicit connection =>
                SQL("INSERT INTO notification_listener (entity_id, entity_type, user_id) VALUES ({entity_id}, {entity_type}, {user_id})")
                    .on('entity_id -> entityId, 'entity_type -> entityType.id, 'user_id -> userId).executeInsert()
            }
        }
    }

    /**
     * Creates a notification for the given parameters. Will not change anything if user has already created a notification of the same type.
     *
     * @param entityId Id of entity for notification
     * @param entityType Type of entity for notification
     * @param actionType Type of the action for notification
     * @param userId User that may be used in the title of the notification
     */
    def create(entityId: Int, entityType: EntityType, actionType: ActionType, userId: Int): Unit = {
        Akka.system.scheduler.scheduleOnce(10.milliseconds) {
            DB.withConnection { implicit connection =>
                val time = System.currentTimeMillis()
                val oneday = time - 86400000
                val exists = SQL("SELECT * FROM notification WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type} AND time > {oneday} LIMIT 1")
                    .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id, 'oneday -> oneday).as(notificationParser.singleOpt)
                if (exists.isDefined) {
                    if (actionType == ActionType.Vote || userHasReplied(entityId, entityType, userId)) {
                        if (exists.get.count < 101) {
                            SQL("UPDATE notification SET count = count + 1, title_user = {user}, time = {time}, `read` = 0 WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type} AND time > {oneday} LIMIT 1")
                                .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id, 'user -> userId, 'time -> time, 'oneday -> oneday).executeUpdate()
                        } else {
                            SQL("UPDATE notification SET title_user = {user}, time = {time}, `read` = 0 WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type} AND time > {oneday} LIMIT 1")
                                .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id, 'user -> userId, 'time -> time, 'oneday -> oneday).executeUpdate()
                        }
                    }
                } else {
                    val time = System.currentTimeMillis()
                    val listeners = SQL("SELECT user_id FROM notification_listener WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND user_id != {user}")
                        .on('entity_id -> entityId, 'entity_type -> entityType.id, 'user -> userId).as(scalar[Int] *)
                    listeners.par map { l =>
                        SQL("INSERT INTO notification (entity_id, entity_type, title_user, action_type, count, user_id, time) VALUES ({entity_id}, {entity_type}, {title_user}," +
                            " {action_type}, 0, {user_id}, {time})")
                            .on('entity_id -> entityId, 'entity_type -> entityType.id, 'title_user -> userId, 'action_type -> actionType.id, 'user_id -> l, 'time -> time).executeInsert()
                    }
                }
            }
        }
    }

    def userHasReplied(entityId: Int, entityType: EntityType, userId: Int): Boolean = {
        entityType match {
            case EntityType.Post =>
                DB.withConnection { implicit connection =>
                    val exists = SQL("SELECT comment_id FROM comment WHERE post_id = {post_id} AND user_id = {user_id} LIMIT 2")
                        .on('post_id -> entityId, 'user_id -> userId).as(scalar[Int] *)
                    exists.length > 1
                }
            case EntityType.Comment =>
                Comment.userHasCommented(userId, entityId)
        }
    }

    /**
     * Deletes notifications from a specific user on an entity. Used for when user changes vote/deletes reply. Will remove notification from listeners.
     *
     * @param entityId Entity id
     * @param entityType Entity type
     * @param actionType Action type
     * @param userId User's id
     */
    def delete(entityId: Int, entityType: EntityType, actionType: ActionType, userId: Int): Unit = {
        Akka.system.scheduler.scheduleOnce(10.milliseconds) {
            DB.withConnection { implicit connection =>
                val exists = SQL("SELECT * FROM notification WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type} LIMIT 1")
                    .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id).as(notificationParser.singleOpt)
                if (exists.isDefined) {
                    if (exists.get.titleUser == userId && exists.get.count > 0) {
                        entityType match {
                            case EntityType.Comment =>
                                actionType match {
                                    case ActionType.Vote =>
                                        val recent = Comment.mostRecentVoter(entityId)
                                        SQL("UPDATE notification SET title_user = {title_user}, count = count - 1 WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type}")
                                            .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id, 'title_user -> recent.get).executeUpdate() > 0
                                    case ActionType.Reply =>
                                        val recent = Comment.mostRecentReplier(entityId)
                                        SQL("UPDATE notification SET title_user = {title_user}, count = count - 1 WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type}")
                                            .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id, 'title_user -> recent.get).executeUpdate() > 0
                                }
                            case EntityType.Post =>
                                actionType match {
                                    case ActionType.Vote =>
                                        val recent = Post.mostRecentVoter(entityId)
                                        SQL("UPDATE notification SET title_user = {title_user}, count = count - 1 WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type}")
                                            .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id, 'title_user -> recent.get).executeUpdate() > 0
                                    case ActionType.Reply =>
                                        val recent = Post.mostRecentReplier(entityId)
                                        SQL("UPDATE notification SET title_user = {title_user}, count = count - 1 WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type}")
                                            .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id, 'title_user -> recent.get).executeUpdate() > 0
                                }
                        }
                    } else if (exists.get.count > 0) {
                        SQL("UPDATE notification SET count = count - 1 WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type}")
                            .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id).executeUpdate() > 0
                    } else {
                        SQL("DELETE FROM notification WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND action_type = {action_type}")
                            .on('entity_id -> entityId, 'entity_type -> entityType.id, 'action_type -> actionType.id).executeUpdate() > 0
                    }
                } else {
                    false
                }
            }
        }
    }

    /**
     * Removes a user's listener on an entity. For when a user their own comment, and does not want notifications anymore.
     *
     * @param entityId Entity's id
     * @param entityType Entity's type
     * @param userId User's id
     */
    def removeListener(entityId: Int, entityType: EntityType, userId: Int): Unit = {
        Akka.system.scheduler.scheduleOnce(10.milliseconds) {
            DB.withConnection { implicit connection =>
                SQL("DELETE FROM notification_listener WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND user_id = {user}")
                    .on('entity_id -> entityId, 'entity_type -> entityType.id, 'user -> userId).executeUpdate()
                SQL("DELETE FROM notification WHERE entity_id = {entity_id} AND entity_type = {entity_type} AND user_id = {user}")
                    .on('entity_id -> entityId, 'entity_type -> entityType.id, 'user -> userId).executeUpdate()
            }
        }
    }

    /**
     * Marks a user's notifications as read.
     *
     * @param userId User id to mark notifications as read
     * @return Boolean for operation success
     */
    def read(userId: Int): Boolean = {
        DB.withConnection { implicit connection =>
            SQL("UPDATE notification SET `read` = 1 WHERE user_id = {user_id}").on('user_id -> userId).executeUpdate()
            true
        }
    }

    /**
     * Gets the details of a notification.
     *
     * @param notification Notification to get the details of
     * @return String, string tuple where first is the url redirect of the notification and second is the description of the notification
     */
    def getDetails(notification: Notification): (String, String) = {
        notification.entityType match {
            case EntityType.Post =>
                val post = Post.find(notification.entityId)
                val board = Board.find(post.get.boardId)
                val descr = {
                    Etc.preview(post.get.data, 50)
                }
                ("/" + board.get.name + "/posts/" + post.get.postId.get, descr)
            case EntityType.Comment =>
                val comment = Comment.find(notification.entityId, status = None)
                val board = Board.find(Post.find(comment.get.postId).get.boardId)
                val descr = Etc.preview(comment.get.data, 50)
                ("/" + board.get.name + "/comments/" + comment.get.commentId.get, descr)
        }
    }

    /**
     * Gets and formats a notification's preview.
     *
     * @param notification Notification to get preview for
     * @return String of preview for notification
     */
    def getPreview(notification: Notification): String = {
        notification.entityType match {
            case EntityType.Post =>
                val post = Post.find(notification.entityId)
                Etc.preview(post.get.data, 50)
            case EntityType.Comment =>
                val comment = Comment.find(notification.entityId, status = None)
                Etc.preview(comment.get.data, 50)
        }
    }

    def getNotifications(userId: Int, limit: Int = 15): Seq[Notification] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM notification WHERE user_id = {user_id} ORDER BY time DESC LIMIT {limit}").on('user_id -> userId, 'limit -> limit).as(notificationParser *)
        }
    }

    /**
     * Change sequence of notifications to json format
     *
     * @param notifications Seq of notifications to be converted
     * @return JsValue of converted json
     */
    def toJsonSeq(notifications: Seq[Notification]): JsValue = {
        var json = Json.arr()
        notifications.reverse.foreach { n =>
            json = json.+:(toJson(n))
        }
        json
    }

    def toJson(notification: Notification): JsValue = {
        val json = Json.obj(
            "notification_id" -> notification.notificationId,
            "count" -> notification.count,
            "title_user" -> User.toJsonByUserID(notification.titleUser),
            "time" -> notification.time,
            if(notification.entityType == EntityType.Post) {
                "post_id" -> notification.entityId
            } else {
                "comment_id" -> notification.entityId
            },
            "read" -> notification.read,
            "preview" -> Notification.getPreview(notification),
            "action_type" -> (if (notification.actionType == ActionType.Reply) {"reply"} else {"vote"})
        )
        if (notification.entityType == EntityType.Comment) {
            val comment = Comment.find(notification.entityId)
            if (comment.isDefined)
                json.+("post_id" -> Json.toJson(comment.get.postId))
            else
                json.+("post_id" -> Json.toJson(0))
        } else
            json
    }

}
