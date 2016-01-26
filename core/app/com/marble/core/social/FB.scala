package com.marble.core.social

import com.google.inject.Singleton
import com.google.inject.Inject
import com.marble.core.config.FacebookConfig
import com.ning.http.client.{AsyncHttpClientConfig, AsyncHttpClient}
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws._
import scala.concurrent.duration._
import play.api.Play.current
import scala.concurrent.{Await, Future}

case class FBUninitialized(message: String) extends Exception(message)
case class FBTokenInvalid(message: String) extends Exception(message)

object FB {
    val facebookServerUrl = "https://graph.facebook.com"
}

@Singleton
class FB @Inject() (fbConfig: FacebookConfig) {

    implicit val context = scala.concurrent.ExecutionContext.Implicits.global
    private val marbleFBClientId: String = fbConfig.fbId
    private val marbleFBClientSecret: String = fbConfig.fbSecret

    /*
        Creates new facebook api instance with a short lived token
     */
    def createFBInstance(token: String): FBInstance = {
        val longToken = WS.url(FB.facebookServerUrl + "/oauth/access_token?" +
            "client_id=" + marbleFBClientId +
            "&client_secret=" + marbleFBClientSecret +
            "&grant_type=fb_exchange_token" +
            "&fb_exchange_token=" + token)
            .get()
            .map { response =>
                val body = response.body
                try {
                    body.substring(body.indexOf('=') + 1, body.indexOf('&'))
                } catch {
                    case e: java.lang.StringIndexOutOfBoundsException => throw new FBTokenInvalid("Facebook token invalid.")
                }
            }
        val resp = Await.result(longToken, 5 seconds)
        if (resp.length > 10) {
            new FBInstance(resp)
        } else {
            throw new FBTokenInvalid("Facebook token invalid.")
        }
    }

}

class FBInstance(t: String) {
    val token: String = t
    implicit val context = scala.concurrent.ExecutionContext.Implicits.global

    def getBasicInfo: JsValue = {
        val res = WS.url(FB.facebookServerUrl + "/v2.3/me").withQueryString("access_token" -> token).get()
            .map { response =>
                response.json
            }
        Await.result(res, 5 seconds)
    }

    def getPictureUrl: String = {
        val res = WS.url(FB.facebookServerUrl + "/v2.3/me/picture").withQueryString("access_token" -> token, "redirect" -> "false", "width" -> "160", "height" -> "160").get()
            .map { response =>
                (response.json \ "data" \ "url").as[String]
            }
        Await.result(res, 5 seconds)
    }

}
