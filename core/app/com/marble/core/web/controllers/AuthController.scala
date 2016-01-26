package com.marble.core.web.controllers

import com.google.inject.Inject
import com.marble.core.data.db.models._
import com.marble.core.web.views.html.desktop.core
import com.marble.utils.play.Auth
import com.marble.utils.play.{Auth, EmailDoesNotExist}
import play.api.mvc._
import com.marble.core.email._
import play.api.libs.json.Json
import scala.concurrent.ExecutionContext.Implicits.global
import com.marble.utils.Etc
import com.marble.core.data.cache.Session
import com.marble.core.data.cache.Cache

class AuthController @Inject() (auth: Auth, cache: Cache) extends Controller {


    def cleanLoginPage = auth.AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => Redirect("/")
            case None =>
                val next = request.getQueryString("next")
                Ok(core.login(next = next))
        }
    }

    def attemptLogin = auth.AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => Redirect("/")
            case None => processLogin(request)
        }
    }

    def logout = auth.AuthAction { implicit user => implicit request =>
        if (user.isDefined)
            auth.logOutSession(user.get.token.get)
        Redirect("/").discardingCookies(auth.deleteSessionCookies)
    }

    def changePassword = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest("User is not authenticated.")
            case Some(_) =>
                val body = request.body
                body.asFormUrlEncoded.map { form =>
                    val currentPass = form.get("current").map(_.head)
                    val newPass = form.get("new").map(_.head)
                    if (currentPass.isDefined && newPass.isDefined) {
                        val check = Etc.checkPass(currentPass.get, user.get.password)
                        if (check && Etc.checkPasswordValidity(newPass.get)) {
                            User.updatePassword(user.get.userId.get, newPass.get)
                            Ok(Json.obj("success" -> "Success"))
                        } else {
                            BadRequest(Json.obj("error" -> "Password is invalid."))
                        }
                    } else {
                        BadRequest(Json.obj("error" -> "Request needs to contain current and new password."))
                    }
                }.getOrElse(BadRequest(Json.obj("error" -> "Request format invalid.")))
        }
    }

    def resetPasswordPage = auth.AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => Redirect("/")
            case None =>
                Ok(core.password_reset(None))
        }
    }

    def resetPasswordPost = auth.AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => Redirect("/")
            case None =>
                val body = request.body
                body.asFormUrlEncoded.map { form =>
                    val email = form.get("email").map(_.head)
                    if (email.isDefined && email.get != "") {
                        val user = User.findByEmail(email.get)
                        if (user.isDefined) {
                            val token = PasswordReset.newReset(user.get.userId.get)
                            sendPasswordResetEmail(user.get.email, Etc.parseFirstName(user.get.name), token)
                            Ok(core.password_reset(None, success = true))
                        } else {
                            Ok(core.password_reset(Some("Email does not exist")))
                        }
                    } else {
                        Ok(core.password_reset(Some("Email required.")))
                    }
                }.getOrElse(BadRequest("Request format incorrect."))
        }
    }

    def sendPasswordResetEmail(email: String, firstName: String, token: String) = {
        val sendEmail = Email(
            subject = "marble Password Reset",
            from = EmailAddress("marble", "reset@marble.co"),
            text = "asdf",
            htmlText = com.marble.core.web.views.html.email.password_reset("https://www.marble.co/password/reset?token=" + token, firstName).toString()
        ).to(firstName, email)
        AsyncMailer.sendEmail(sendEmail)
    }

    def resetPasswordAuth = auth.AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => Redirect("/")
            case None =>
                val token = request.getQueryString("token")
                if (token.isDefined) {
                    val reset = PasswordReset.byToken(token.get)
                    if (reset.isDefined) {
                        Ok(core.password_reset(None, token = Some(token.get)))
                    } else {
                        Redirect("/")
                    }
                } else {
                    Redirect("/")
                }
        }
    }

    def resetPasswordAttempt = auth.AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => Redirect("/")
            case None =>
                request.body.asFormUrlEncoded.map { form =>
                    val token = form.get("token").map(_.head)
                    if (token.isDefined) {
                        val reset = PasswordReset.byToken(token.get)
                        val password = form.get("password").map(_.head)
                        if (reset.isDefined && password.isDefined) {
                            User.updatePassword(reset.get.userId, password.get)
                            Redirect("/").withCookies(Cookie("auth_token", auth.getNewUserSessionId(reset.get.userId)))
                        } else {
                            Redirect("/")
                        }
                    } else {
                        Redirect("/")
                    }
                }.getOrElse(Redirect("/"))
        }
    }

    def setPassword = auth.AuthAction { implicit user => implicit request =>
        user match {
            case None => BadRequest("User is not authenticated.")
            case Some(_) =>
                val body = request.body
                body.asFormUrlEncoded.map { form =>
                    val newPass = form.get("new").map(_.head)
                    if (newPass.isDefined) {
                        if (SocialUser.userIsSocial(user.get.userId.get) && Etc.checkPasswordValidity(newPass.get) && user.get.password == "") {
                            User.updatePassword(user.get.userId.get, newPass.get)
                            Ok("Success.")
                        } else {
                            BadRequest("Password invalid.")
                        }
                    } else {
                        BadRequest("Request needs to contain new password.")
                    }
                }.getOrElse(BadRequest("Request format invalid."))
        }
    }

    private def processLogin(request: Request[AnyContent])(implicit r: RequestHeader): Result = {
        val body: AnyContent = request.body
        body.asFormUrlEncoded.map { form =>
            val email = form.get("email").map(_.head)
            val password = form.get("password").map(_.head)
            if (email.isDefined && password.isDefined && email.get.trim.length > 0 && password.get.trim.length > 0) {
                try {
                    val token = auth.logInSession(email.get, password.get)
                    if (token.isEmpty) {
                        Ok(core.login(error = true, errorMessage = "Hmm, wrong password. Try again!", email = email.get))
                    }
                    else {
                        val admin = User.isUserAdmin(User.findByEmail(email.get).get.userId.get)
                        if (admin) {
                            val sess = new Session(cache)(token.get)
                            sess.set("admin", "true")
                        }
                        val next = request.getQueryString("next")
                        if (next.isDefined) {
                            Redirect(next.get).withCookies(Cookie("auth_token", token.get))
                        } else {
                            Redirect("/").withCookies(Cookie("auth_token", token.get))
                        }
                    }
                } catch {
                    case e: EmailDoesNotExist =>
                        Ok(core.login(error = true, errorMessage = "Hmm, seems like that email does not exist.", email = email.get))
                }
            } else {
                Ok(core.login(error = true, errorMessage = "You need to fill in your email and password."))
            }
        }.getOrElse(Ok(core.login(error = true, errorMessage = "Something went wrong... Please try again.")))
    }

}
