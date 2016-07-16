package com.marble.core.web.controllers

import com.google.inject.Inject
import com.marble.core.data.cache._
import com.marble.core.data.cache
import com.marble.core.data.db.models._
import com.marble.core.email.MailTemplates
import com.marble.utils.Etc
import com.marble.utils.play.Auth
import play.Play
import play.api.mvc._

class RegisterController @Inject() (auth: Auth, c: Cache) extends Controller {

    def cleanRegisterPage = auth.AuthAction { implicit user => implicit request =>
        Ok(com.marble.core.web.views.html.desktop.core.register())
    }

    def attemptRegister = auth.AuthAction { implicit user => implicit request =>
        user match {
            case Some(_) => Redirect("/")
            case None =>
                processRegister(request)
        }
    }

    private def processRegister(request: Request[AnyContent]): Result = {
        val body: AnyContent = request.body
        body.asFormUrlEncoded.map { form =>
            val email = form.get("email").map(_.head)
            val fname = form.get("first_name").map(_.head)
            val lname = form.get("last_name").map(_.head)
            val password = form.get("password").map(_.head)
            if (email.isDefined && fname.isDefined && lname.isDefined && password.isDefined) {
                val userExists = User.findByEmail(email.get)
                if (userExists.isDefined && Etc.checkPass(password.get, userExists.get.password))
                    Found("/").withCookies(auth.newSessionCookies(userExists.get.userId.get))
                val newUser = User.register(fname.get, lname.get, password.get, email.get)
                if (newUser.isDefined) {
                    val token = auth.getNewUserSessionId(User.find(newUser.get.toInt).getOrElse(return BadRequest("Error.")).userId.get)
                    val sess = new cache.Session(c)(token)
                    sess.set("getting_started", "true")
                    if (Play.isProd) {
                        MailTemplates.sendWelcomeEmail(fname.get, email.get)
                    }
                    Found("/").withCookies(auth.newSessionCookies(token))
                }
                else
                    BadRequest("Error: user creation failed.")
            } else
                BadRequest("Error: request format invalid.")
        }.getOrElse(return BadRequest("Error."))
    }


}
