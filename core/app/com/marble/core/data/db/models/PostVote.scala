package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.marble.core.data.db.models.Enum.{ActionType, EntityType}
import play.api.Play.current
import play.api.db._

case class PostVote (
    postVoteId: Option[Int],
    postId: Int,
    userId: Int,
    value: Int,
    time: BigInt
)

object PostVote {

    private[models] val postVoteParser: RowParser[PostVote] = {
        get[Option[Int]]("post_vote_id") ~
            get[Int]("post_id") ~
            get[Int]("user_id") ~
            get[Int]("value") ~
            get[java.math.BigInteger]("time") map {
            case postVoteId ~ postId ~ userId ~ value ~ time =>
                PostVote(postVoteId, postId, userId, value, time)
        }
    }

    def findByPostAndUser(postId: Int, userId: Int): Option[PostVote] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM post_vote WHERE user_id = {user} AND post_id = {post} LIMIT 1").on('user -> userId, 'post -> postId)
                .as(postVoteParser.singleOpt)
        }
    }

    def votePost(postId: Int, userId: Int, value: Int): Boolean = {
        val post = Post.find(postId)
        val voteExists = PostVote.findByPostAndUser(postId, userId)
        if (post.isDefined && (value == -1 || value == 1) && voteExists.isEmpty) {
            DB.withConnection { implicit connection =>
                if (value == 1 && (userId != post.get.userId)) {
                    SQL("UPDATE user_info SET reputation = reputation + 20 WHERE user_id = {user}")
                        .on('user -> post.get.userId).executeUpdate()
                    Notification.create(postId, EntityType.Post, ActionType.Vote, userId)
                } else if (value == -1 && (userId != post.get.userId)) {
                    SQL("UPDATE user_info SET reputation = reputation - 20 WHERE user_id = {user}")
                        .on('user -> post.get.userId).executeUpdate()
                }
                val time = System.currentTimeMillis()
                SQL("INSERT INTO post_vote (post_id, user_id, value, time) VALUES ({post_id}, {user_id}, {value}, {time})")
                    .on('post_id -> postId, 'user_id -> userId, 'value -> value, 'time -> time).executeInsert()
                SQL("UPDATE post SET votes = votes + {value} WHERE post_id = {post_id}")
                    .on('value -> value, 'post_id -> postId).executeUpdate()
            }
            true
        } else if (voteExists.isDefined && (value == -1 || value == 1) && post.isDefined) {
            if (voteExists.get.value != value) {
                DB.withConnection { implicit connection =>
                    if (value == 1 && (userId != post.get.userId)) {
                        SQL("UPDATE user_info SET reputation = reputation + 40 WHERE user_id = {user}")
                            .on('user -> post.get.userId).executeUpdate()
                        Notification.create(postId, EntityType.Post, ActionType.Vote, userId)
                    } else if (value == -1 && (userId != post.get.userId)) {
                        SQL("UPDATE user_info SET reputation = reputation - 40 WHERE user_id = {user}")
                            .on('user -> post.get.userId).executeUpdate()
                        Notification.delete(postId, EntityType.Post, ActionType.Vote, userId)
                    }
                    val time = System.currentTimeMillis()
                    SQL("UPDATE post_vote SET value = {value}, time = {time} WHERE post_id = {post_id} AND user_id = {user_id}")
                        .on('value -> value, 'user_id -> userId, 'post_id -> postId, 'time -> time).executeUpdate()
                    SQL("UPDATE post SET votes = votes + {value} WHERE post_id = {post_id}")
                        .on('value -> 2 * value, 'post_id -> postId).executeUpdate()
                }
                true
            } else false
        } else false
    }

    def unVotePost(postId: Int, userId: Int): Unit = {
        val post = Post.find(postId)
        val vote = PostVote.findByPostAndUser(postId, userId)
        if (post.isDefined && vote.isDefined) {
            DB.withConnection { implicit connection =>
                SQL("DELETE FROM post_vote WHERE post_id = {post} AND user_id = {user}").on('post -> postId, 'user -> userId).executeUpdate()
            }
        }
    }

    def getPostVotesByUserAndPost(userId: Int, postIds: Seq[Int]): Seq[PostVote] = {
        DB.withConnection { implicit connection =>
            if (postIds.isEmpty)
                return List()
            SQL("SELECT * FROM post_vote WHERE post_id IN ({post_id}) AND user_id = {user_id}")
                .on('post_id -> postIds, 'user_id -> userId).as(postVoteParser *)
        }
    }

    def getPostVoteValue(postId: Int, userId: Int): Int = {
        val postVote = PostVote.findByPostAndUser(postId, userId)
        if (postVote.isDefined)
            postVote.get.value
        else 0
    }

}
