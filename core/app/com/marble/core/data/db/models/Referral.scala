package com.marble.core.data.db.models

import anorm.SqlParser._
import anorm._
import com.marble.utils.Hash
import play.api.Play.current
import com.marble.core.data.Constants
import play.api.db._
import com.marble.core.data.Constants
import play.api.libs.json._


case class Referral(
    referralId: Option[Int],
    code: String,
    userId: Int
)

object Referral {

    private val hasher = Hash(Constants.HashSalt)

    private[data] val referralParser: RowParser[Referral] = {
        get[Option[Int]]("referral_id") ~
            get[Int]("user_id") map {
            case referralId ~ userId =>
                val code = hasher.encode(referralId.get)
                Referral(referralId, code, userId)
        }
    }

    def find(referralId: Int): Option[Referral] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM referral WHERE referral_id = {ref}").on('ref -> referralId).as(referralParser.singleOpt)
        }
    }

    def findByUser(userId: Int): Option[Referral] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM referral WHERE user_id = {user}").on('user -> userId).as(referralParser.singleOpt)
        }
    }

    def findByHash(hash: String): Option[Referral] = {
        val user = hasher.decode(hash).head
        findByUser(user.toInt)
    }

    def create(userId: Int): Option[Referral] = {
        val exists = Referral.findByUser(userId)
        if (exists.isDefined) {
            exists
        } else {
            DB.withConnection { implicit connection =>
                val id = SQL("INSERT INTO referral (user_id) VALUES ({user})").on('user -> userId).executeInsert(scalar[Long].singleOpt)
                if (id.isDefined) {
                    find(id.get.toInt)
                } else {
                    None
                }
            }
        }
    }

}
