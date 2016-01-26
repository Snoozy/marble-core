package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.marble.core.data.db.models.Enum.{ActionType, EntityType}
import play.api.Play.current
import play.api.db._

case class CommentVote (
    commentVoteId: Option[Int],
    commentId: Int,
    userId: Int,
    value: Int,
    time: BigInt
)

object CommentVote {

    private[models] val commentVoteParser: RowParser[CommentVote] = {
        get[Option[Int]]("comment_vote_id") ~
            get[Int]("comment_id") ~
            get[Int]("user_id") ~
            get[Int]("value") ~
            get[java.math.BigInteger]("time") map {
            case commentVoteId ~ commentId ~ userId ~ value ~ time =>
                CommentVote(commentVoteId, commentId, userId, value, time)
        }
    }

    def findByCommentAndUser(commentId: Int, userId: Int): Option[CommentVote] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM comment_vote WHERE user_id = {user} AND comment_id = {comment}").on('user -> userId, 'comment -> commentId)
                .as(commentVoteParser.singleOpt)
        }
    }

    def voteComment(commentId: Int, userId: Int, value: Int): Boolean = {
        val comment = Comment.find(commentId)
        val voteExists = CommentVote.findByCommentAndUser(commentId, userId)
        if (comment.isDefined && (value == -1 || value == 1) && voteExists.isEmpty) {
            DB.withConnection { implicit connection =>
                if (value == 1 && (userId != comment.get.userId)) {
                    SQL("UPDATE user_info SET reputation = reputation + 10 WHERE user_id = {user}")
                        .on('user -> comment.get.userId).executeUpdate()
                    Notification.create(commentId, EntityType.Comment, ActionType.Vote, userId)
                } else if (value == -1 && (userId != comment.get.userId)) {
                    SQL("UPDATE user_info SET reputation = reputation - 10 WHERE user_id = {user}")
                        .on('user -> comment.get.userId).executeUpdate()
                }
                val time = System.currentTimeMillis()
                SQL("INSERT INTO comment_vote (comment_id, user_id, value, time) VALUES ({comment_id}, {user_id}, {value}, {time})")
                    .on('comment_id -> commentId, 'user_id -> userId, 'value -> value, 'time -> time).executeInsert()
                SQL("UPDATE comment SET votes = votes + {value} WHERE comment_id = {comment_id}")
                    .on('value -> value, 'comment_id -> commentId).executeUpdate()
            }
            true
        } else if (voteExists.isDefined && (value == -1 || value == 1) && comment.isDefined) {
            if (voteExists.get.value != value) {
                DB.withConnection { implicit connection =>
                    if (value == 1 && (userId != comment.get.userId)) {
                        SQL("UPDATE user_info SET reputation = reputation + 20 WHERE user_id = {user}")
                            .on('user -> comment.get.userId).executeUpdate()
                        Notification.create(commentId, EntityType.Comment, ActionType.Vote, userId)
                    } else if (value == -1 && (userId != comment.get.userId)) {
                        SQL("UPDATE user_info SET reputation = reputation - 20 WHERE user_id = {user}")
                            .on('user -> comment.get.userId).executeUpdate()
                        Notification.delete(commentId, EntityType.Comment, ActionType.Vote, userId)
                    }
                    val time = System.currentTimeMillis()
                    SQL("UPDATE comment_vote SET value = {value}, time = {time} WHERE comment_id = {comment_id} AND user_id = {user_id}")
                        .on('value -> value, 'user_id -> userId, 'comment_id -> commentId, 'time -> time).executeUpdate()
                    SQL("UPDATE comment SET votes = votes + {value} WHERE comment_id = {comment_id}")
                        .on('value -> 2 * value, 'comment_id -> commentId).executeUpdate()
                }
                true
            } else false
        } else false
    }

    def unVoteComment(commentId: Int, userId: Int): Unit = {
        val comment = Comment.find(commentId)
        val vote = CommentVote.findByCommentAndUser(commentId, userId)
        if (comment.isDefined && vote.isDefined) {
            DB.withConnection { implicit connection =>
                SQL("DELETE FROM comment_vote WHERE user_id = {user} AND comment_id = {comment}").on('user -> userId, 'comment -> commentId).executeUpdate()
            }
        }
    }

    def getCommentVotesByUserAndPost(userId: Int, commentIds: Seq[Int]): Seq[CommentVote] = {
        DB.withConnection { implicit connection =>
            if (commentIds.isEmpty)
                return List()
            SQL("SELECT * FROM comment_vote WHERE post_id IN ({comment_id}) AND user_id = {user_id}")
                .on('comment -> commentIds, 'user_id -> userId).as(commentVoteParser *)
        }
    }

    def getCommentVoteValue(userId: Int, commentId: Int): Int = {
        val commentVote = CommentVote.findByCommentAndUser(commentId, userId)
        if (commentVote.isDefined)
            commentVote.get.value
        else 0
    }

}
