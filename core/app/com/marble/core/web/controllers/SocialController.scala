package com.marble.core.web.controllers

import com.google.inject.Inject
import com.marble.core.data.cache.Cache
import com.marble.core.data.cache.Session
import com.marble.core.data.db.models._
import com.marble.utils.play.Auth
import com.marble.core.social.FB
import com.marble.core.data.aws.S3
import play.api.mvc._

class SocialController @Inject() (auth: Auth, cache: Cache, fbSession: FB) extends Controller {

    def facebookAuth = auth.AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => Found("/")
            case None =>
                if (request.getQueryString("fb_token").isDefined) {
                    processFacebookAuth(request)
                } else {
                    NotFound("Oops.")
                }
        }
    }

    private def processFacebookAuth(request: Request[AnyContent]): Result = {
        val token = request.getQueryString("fb_token").get
        val fb = fbSession.createFBInstance(token)
        val info = fb.getBasicInfo
        val fbId = (info \ "id").as[String].toLong
        val userId = SocialUser.findFbUserId(fbId)
        if (userId.isDefined) {
            val user = User.find(userId.get)
            val admin = User.isUserAdmin(user.get.userId.get)
            val token = auth.getNewUserSessionId(user.get.userId.get)
            if (admin) {
                val sess = new Session(cache)(token)
                sess.set("admin", "true")
            }
            val next = request.getQueryString("next")
            if (next.isDefined) {
                Found(next.get).withCookies(Cookie("auth_token", token))
            } else {
                Found("/").withCookies(Cookie("auth_token", token))
            }
        } else {
            val fbEmail = (info \ "email").asOpt[String]
            if (fbEmail.isDefined) {
                val user = User.findByEmail(fbEmail.get)
                if (user.isDefined) {
                    val social = SocialUser.findFbUserId(fbId)
                    if (social.isEmpty) {
                        SocialUser.createFBUser(fbId, user.get.userId.get)
                    }
                    val admin = User.isUserAdmin(user.get.userId.get)
                    if (admin) {
                        user.get.session.get.set("admin", "true")
                    }
                    return Found("/").withCookies(auth.newSessionCookies(user.get.userId.get))
                }
            }
            val fbName = (info \ "name").as[String]
            val firstName = (info \ "first_name").as[String]
            val lastName = (info \ "last_name").as[String]
            val username = {
                if (fbEmail.isDefined)
                    User.genUsername(fbEmail.get)
                else
                    User.genUsername(fbName.replace(" ", ""))
            }
            val pic: Option[Int] = {
                val id = S3.uploadURL(fb.getPictureUrl, profile = true)
                if (id.isDefined) {
                        Some(id.get)
                } else None
            }
            val newUser = User.create(username, firstName, lastName, "", fbEmail.getOrElse(""), None, pic = pic)
            if (newUser.isDefined) {
                SocialUser.createFBUser(fbId, newUser.get.toInt)
                val sess_token = auth.getNewUserSessionId(User.find(newUser.get.toInt).get.userId.get)
                val sess = new Session(cache)(sess_token)
                sess.multiSet(Map("getting_started" -> "true", "fb_token" -> token))
                Found("/").withCookies(auth.newSessionCookies(sess_token))
            } else {
                InternalServerError("Oops.")
            }
        }
    }

}
