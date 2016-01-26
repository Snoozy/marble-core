package com.marble.core.api.controllers

import com.google.inject.Inject
import com.marble.core.data.db.models._
import com.marble.utils.play.Auth
import com.marble.utils.play.Auth
import play.api.libs.json.{JsObject, JsValue, Json}
import com.marble.core.data.Constants
import play.api.mvc._
import com.marble.utils.play.errors._

/**
 * Handles User API requests including:
 *      Describing
 *      Describing the currently logged in user
 *      Creating user
 *      Get a users boards
 *      Get a users comments
 *      Get a users posts by time
 *      Get a users feed
 */

class UserController @Inject() (auth: Auth) extends Controller {

    /**
     * Describes the user given by the querystring that can either be a userId or username.
     *
     * @return Json of the user.
     */
    def describe = auth.ApiAuthAction { implicit user => implicit request =>
        val query = request.queryString.map {
            case (k, v) => k -> v.mkString
        }
        if (query.contains("userId")) {
            try {
                Ok(User.toJsonByUserID(query.get("userId").get.toInt, self = user))
            } catch {
                case e: java.lang.NumberFormatException => BadRequest(Json.obj("error" -> "Invalid request format."))
            }
        } else if (query.contains("username")) {
            Ok(User.toJsonByUsername(query.get("username").get, self = user))
        } else {
            BadRequest(Json.obj("error" -> "Invalid request format."))
        }
    }

    /**
     * Describes the currently logged in user.
     *
     * @return Json of the user.
     */
    def describeSelf = auth.ApiAuthAction { implicit user => implicit request =>
        Ok(User.toJson(user.get, self = user))
    }

    /**
     * Creates a new user with the supplied fields:
     *      name
     *      username
     *      password
     *      email
     *      bio
     *
     * @return Json of the newly created user.
     */
    def create = auth.AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => BadRequest(Json.obj("error" -> "User must be logged out."))
            case None =>
                val body: AnyContent = request.body
                body.asFormUrlEncoded.map { form =>
                    val username = form.get("username").map(_.head)
                    val name = form.get("name").map(_.head)
                    val password = form.get("password").map(_.head)
                    val email = form.get("email").map(_.head)
                    val bio = form.get("bio").map(_.head)
                    if (username.isDefined && username.get.length < Constants.MaxUsernameLength && name.isDefined && name.get.length < Constants.MaxNameLength
                        && password.isDefined && email.isDefined) {
                        val usernameExists = User.find(username.get)
                        if (usernameExists.isDefined) {
                            BadRequest(UsernameTaken.toJson)
                        } else {
                            val newUser = User.create(username.get, name.get, password.get, email.get, bio)
                            if (newUser.isDefined) {
                                val auth_token = auth.getNewUserSessionId(newUser.get.toInt)
                                Ok(Json.obj("user" -> User.toJsonByUserID(newUser.get.toInt),
                                    "auth_token" -> Json.toJson(auth_token)))
                            }
                            else
                                BadRequest(Json.obj("error" -> "User creation failed."))
                        }
                    } else {
                        BadRequest(Json.obj("error" -> "Request format invalid."))
                    }
                }.getOrElse(BadRequest(Json.obj("error" -> "Request format invalid.")))
        }
    }

    /**
     * Get the boards of a user.
     *
     * @param userId userId of entity to get boards of
     * @return Json of fully hydrated boards.
     */
    def getBoards(userId: Int) = auth.ApiAuthAction { implicit user => implicit request =>
        val userExists = User.find(userId)
        if (userExists.isEmpty)
            BadRequest(Json.obj("error" -> "User does not exist."))
        else {
            val describingUser = userExists.get
            val json: JsValue = Json.obj(
                "boards" -> Board.toJsonSeq(User.getBoards(describingUser.userId.get), Some(true))
            )
            Ok(json)
        }
    }

    /**
     * Gets the comments of a specific user by time.
     *
     * @param userId UserId of entity to get comments for.
     * @return Json for a list of fully hydrated comments.
     */
    def getComments(userId: Int) = auth.ApiAuthAction { implicit user => implicit request =>
        val userExists = User.find(userId)
        if (userExists.isEmpty)
            BadRequest(Json.obj("error" -> "User does not exist."))
        else {
            val after = request.getQueryString("after")
            val describingUser = userExists.get
            val comments = {
                if (after.isDefined) {
                    User.getCommentsPaged(describingUser.userId.get, after.get.toInt)
                } else {
                    User.getComments(describingUser.userId.get)
                }
            }
            Ok(Json.obj("comments" -> Comment.toJsonSeqWithUser(comments.reverse, user)))
        }
    }

    /**
     * Gets the posts of a specific user by time.
     *
     * @param userId UserId of the entity to get posts for.
     * @return Fully hydrated posts for this user.
     */
    def getPosts(userId: Int) = auth.ApiAuthAction { implicit user => implicit request =>
        val userExists = User.find(userId)
        if (userExists.isEmpty)
            BadRequest(Json.obj("error" -> "User does not exist."))
        else {
            val after = request.getQueryString("after")
            val describingUser = userExists.get
            val posts = {
                if (after.isDefined && after.get != "") {
                    User.getPostsPaged(describingUser.userId.get, after.get.toInt)
                } else {
                    User.getPosts(describingUser.userId.get)
                }
            }
            Ok(Json.obj("posts" -> Post.toJsonWithUser(posts.reverse, user, following = Option(true))))
        }
    }

    /**
     * Gets the feed for the currently logged in user.
     *
     * @return Fully hydrated posts for this users feed.
     */
    def getFeed = auth.ApiAuthAction { implicit user => implicit request =>
        val afterPost = request.getQueryString("after")
        val posts = {
            if (afterPost.isDefined && afterPost.get != "")
                User.getFeedPaged(user.get.userId.get, afterPost.get.toInt)
            else
                User.getFeed(user.get.userId.get)
        }
        Ok(Json.obj("posts" -> Post.toJsonWithUser(posts.reverse, user, following = Option(true))))
    }
}
