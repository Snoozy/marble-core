package com.marble.core.web.controllers

import java.util.regex.Pattern
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.google.inject.Inject
import com.marble.core.config.RedisConfig
import scala.concurrent.ExecutionContext.Implicits.global
import com.marble.core.email.{AsyncMailer, EmailAddress, Email}
import play.api.Play.current
import com.amazonaws.services.s3.model.{DeleteObjectRequest, S3ObjectSummary}
import com.marble.core.data.cache.Redis
import play.api.{Logger, Play}
import play.api.mvc._
import com.marble.utils.play.Auth
import com.marble.utils.reddit.Reddit
import com.marble.core.data.db.models._
import scala.collection.JavaConversions._
import org.apache.commons.lang3.StringEscapeUtils._
import com.marble.core.data.aws.S3._
import scala.util.Random
import com.marble.core.data.cache.Cache

class EtcController @Inject() (auth: Auth, cache: Cache) extends Controller {

    val subreddits = Map[String, List[String]]("worldnews" -> List("worldnews", "news"), "earthpics" -> List("earthporn"),
        "nba" -> List("nba"), "programming" -> List("programming", "programmerhumor"), "soccer" -> List("soccer"), "politics" -> List("politics"),
        "tech" -> List("technology"), "sports" -> List("sports"), "funny" -> List("funny"), "food" -> List("food", "foodporn"), "music" -> List("music"))
    val users = Vector(2, 3, 4, 8, 13, 14, 12, 10)

    def debug = Action { implicit request =>
        Ok("asdf")
    }

    def refresh = AuthAction { implicit user => implicit request =>
        if ((user.isDefined && user.get.admin) || Play.isDev) {
            cache.delete("gettingStarted_cache")
            cache.delete("welcome_cache")
            Ok("done.")
        } else {
            Found("/")
        }
    }

    def reddit = AuthAction { implicit user => implicit request =>
        subreddits.foreach {
            case (key, value) =>
                value.foreach { s =>
                    val board = Board.find(key)
                    if (board.isDefined) {
                        if (s == "earthporn" || s == "foodporn") {
                            val subms = Reddit.getSubredditPosts(s)
                            subms.foreach { p =>
                                val userId = users(Random.nextInt(users.size))
                                val time = System.currentTimeMillis() - (Random.nextInt(48) * 1800000)
                                if ((!p.getUrl.contains("gallery") && !p.getUrl.contains("/a/")) && p.getUrl.contains("imgur")) {
                                    val url = {
                                        if (!p.getUrl.contains("i.imgur.com")) {
                                            p.getURL + ".jpg"
                                        } else {
                                            p.getURL.replace(".gifv", ".gif")
                                        }
                                    }
                                    val testTitle = unescapeHtml4(p.getTitle.replace("[OC]", "").substring(0, p.getTitle.indexOf("[")))
                                    val title = {
                                        if (testTitle.length < 100) {
                                            Some(testTitle)
                                        } else {
                                            None
                                        }
                                    }
                                    val data = {
                                        if (testTitle.length < 100) {
                                            ""
                                        } else {
                                            testTitle
                                        }
                                    }
                                    val id = uploadURL(url)
                                    if (id.isDefined) {
                                        Post.createMediaPost(userId, title, data, board.get.boardId.get, Seq(id.get), time = time)
                                    }
                                }
                            }
                        } else {
                            val subms = Reddit.getSubredditPosts(s)
                            subms.foreach { p =>
                                val userId = users(Random.nextInt(users.size))
                                val time = System.currentTimeMillis() - (Random.nextInt(13) * 1800000)
                                if (p.getUrl.contains("imgur") && (!p.getUrl.contains("gallery") && !p.getUrl.contains("/a/"))) {
                                    val url = {
                                        if (!p.getUrl.contains("i.imgur.com") && !p.getUrl.contains("gifv")) {
                                            p.getURL + ".jpg"
                                        } else {
                                            p.getURL.replace(".gifv", ".gif")
                                        }
                                    }
                                    val testTitle = unescapeHtml4(p.getTitle)
                                    val title = {
                                        if (testTitle.length < 100) {
                                            Some(testTitle)
                                        } else {
                                            None
                                        }
                                    }
                                    val data = {
                                        if (testTitle.length < 100) {
                                            ""
                                        } else {
                                            testTitle
                                        }
                                    }
                                    val id = uploadURL(url)
                                    if (id.isDefined) {
                                        Post.createMediaPost(userId, title, data, board.get.boardId.get, Seq(id.get), time = time)
                                    }
                                } else {
                                    val title = {
                                        if (p.getTitle.length < 100) {
                                            Some(unescapeHtml4(p.getTitle))
                                        } else {
                                            None
                                        }
                                    }
                                    val data = {
                                        if (p.getTitle.length < 100) {
                                            p.getURL
                                        } else {
                                            unescapeHtml4(p.getTitle) + "\n\n" + p.getURL
                                        }
                                    }
                                    Post.createSimplePost(userId, title, data, board.get.boardId.get, time = time)
                                }
                            }
                        }
                    }
                }
        }
        Ok("done")
    }

    def cleanS3 = AuthAction { implicit user => implicit request =>
        /* TODO WHEN S3 BUCKET IS FILLING UP
        if (user.isDefined && user.get.admin) {
            val aws_key = Play.current.configuration.getString("aws.key")
            val aws_secret = Play.current.configuration.getString("aws.secret")
            val aws_creds = new BasicAWSCredentials(aws_key.get, aws_secret.get)
            val s3client = new AmazonS3Client(aws_creds)
            val list = s3client.listObjects("marble-static", "image")
            do {
                val summaries = list.getObjectSummaries
                var count = 0
                for (summary: S3ObjectSummary <- summaries; if count < 20) {
                    count += 1
                    val key = summary.getKey
                    play.api.Logger.debug(key)
                }
            } while (list.isTruncated)
            s3client.deleteObject(new DeleteObjectRequest("marble-static", "453987984735"))
            Ok("Done.")
        } else {
            Found("/")
        }
        */
        Found("/")
    }

}

object EtcController extends Controller {

    def redirectHttp = Action { implicit request =>
        MovedPermanently("https://" + request.host + request.uri)
    }

    def healthCheck = Action {
        Ok("Healthy")
    }

}
