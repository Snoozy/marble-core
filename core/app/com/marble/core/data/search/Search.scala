package com.marble.core.data.search

import anorm.SqlParser._
import anorm._
import com.marble.core.data.db.models.{User, Board}
import play.api.Play.current
import play.api.db._
import com.marble.core.data.db.models.Board.boardParser
import com.marble.core.data.db.models.User.userParser


object Search {

    def boardSearch(query: String): Seq[Board] = {
        val newQuery = query.replace(" ", "")
        val natSearch = naturalBoardSearch(newQuery)
        if (natSearch.length < 5) {
            val ex = expandedBoardSearch(newQuery)
            if (ex.length < 1) {
                partialBoardSearch(newQuery)
            } else {
                ex
            }
        } else {
            natSearch
        }
    }

    def userSearch(query: String): Seq[User] = {
        val natSearch = naturalUserSearch(query)
        if (natSearch.length < 5) {
            val ex = expandedUserSearch(query)
            if (ex.length < 1) {
                partialUserSearch(query)
            } else {
                ex
            }
        } else {
            natSearch
        }
    }

    def autoCompleteBoard(query: String): Seq[Board] = {
        partialBoardSearch(query)
    }

    def autoCompleteUser(query: String): Seq[User] = {
        partialUserSearch(query)
    }

    private def partialBoardSearch(query: String): Seq[Board] = {
        DB.withConnection { implicit connection =>
            SQL("""SELECT * FROM board WHERE MATCH(name) AGAINST({q} IN BOOLEAN MODE)""").on('q -> (query + "*")).as(boardParser *)
        }
    }

    private def partialUserSearch(query: String): Seq[User] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM user WHERE MATCH(name) AGAINST({q} IN BOOLEAN MODE) OR MATCH(username) AGAINST({q} IN BOOLEAN MODE)").on('q -> (query + "*")).as(userParser *)
        }
    }

    /*
        Expanded board search with query expansion
     */
    private def expandedBoardSearch(query: String): Seq[Board] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM board WHERE MATCH(name) AGAINST({query} IN NATURAL LANGUAGE MODE WITH QUERY EXPANSION)")
                .on('query -> query).as(boardParser *)
        }
    }

    private def expandedUserSearch(query: String): Seq[User] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM user WHERE MATCH(name) AGAINST({q} IN NATURAL LANGUAGE MODE WITH QUERY EXPANSION) " +
                "OR MATCH(username) AGAINST({q} IN NATURAL LANGUAGE MODE WITH QUERY EXPANSION)").on('q -> (query + "*")).as(userParser *)
        }
    }

    /*
        Search with just natural language mode on
     */
    private def naturalBoardSearch(query: String): Seq[Board] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM board WHERE MATCH(name) AGAINST({query} IN NATURAL LANGUAGE MODE)")
                .on('query -> query).as(boardParser *)
        }
    }

    private def naturalUserSearch(query: String): Seq[User] = {
        DB.withConnection { implicit connection =>
            SQL("SELECT * FROM user WHERE MATCH(name) AGAINST({q} IN NATURAL LANGUAGE MODE) " +
                "OR MATCH(username) AGAINST({q} IN NATURAL LANGUAGE MODE)").on('q -> (query + "*")).as(userParser *)
        }
    }

}
