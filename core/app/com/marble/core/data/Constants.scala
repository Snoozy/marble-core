package com.marble.core.data

import play.Play

object Constants {

    val MaxUsernameLength = 25
    val MaxNameLength = 25
    val DefaultPageLength = 20
    val MaxMediaSize = 3145728
    val BannedNames = List("admin", "marble")
    val BannedBoards = List("admin", "settings", "support", "boards", "logout", "login", "user", "signup", "search", "gettingstarted", "connect", "debug", "a", "legal")
    val HashSalt = "K06mylBiXH"

    val MaxLinkLength = 30

    val GettingStartedBoards = {
        if (Play.isDev) {
            Map[String, Seq[Int]]("adsf" -> Seq(1, 2, 1, 2, 1), "qwerty" -> Seq(1, 1, 1, 1, 1, 1, 1), "zxcv" -> Seq(1, 1, 1, 1, 1, 1))
        } else {
            Map[String, Seq[Int]]("Entertainment" -> Seq(9, 7, 15, 16, 17, 8), "Sports" -> Seq(1, 5, 6, 31, 32, 33),
                "Science" -> Seq(13, 29, 26, 27, 30), "News and Politics" -> Seq(14, 23, 25), "Educational" -> Seq(18, 19, 20, 21, 22))
        }
    }

    val TestBoards = {
        if (Play.isDev) {
            Seq[Int](1, 1, 1, 1, 1, 1, 1, 1, 1)
        } else {
            Seq[Int](9, 15, 16, 17, 8, 1, 5, 6, 31, 32, 33, 13, 29, 26, 27, 30, 14, 23, 25, 18, 19, 20, 21, 22)
        }
    }

    val FeaturedBoards = {
        if (Play.isDev) {
            Seq[Int](1,1,1,1)
        } else {
            Seq[Int](9, 23, 37, 6, 27, 31, 38)
        }
    }

    val FrontBoards = {
        if (Play.isDev) {
            Seq[Int](1)
        } else {
            Seq[Int](9, 37, 8)
        }
    }

}
