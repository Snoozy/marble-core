package com.marble.core.web

import com.marble.core.config.MarbleConfig
import com.marble.core.web.controllers.EtcController
import play.api.Play.current
import play.api.mvc.Results._
import play.api.mvc._
import play.api.{Application, GlobalSettings, Play}
import com.marble.core.social.FB
import scala.concurrent.ExecutionContext.Implicits.global
import com.marble.utils.UAgentInfo
import scala.concurrent.Future

object Global extends WithFilters() with GlobalSettings {

    override def doFilter(action: EssentialAction): EssentialAction = super.doFilter(EssentialAction { request =>
        action.apply(request).map(_.withHeaders(
            "Strict-Transport-Security" -> "max-age=31536000; includeSubdomains; preload"
        ))
    })

    override def onRouteRequest(request: RequestHeader): Option[Handler] = {
        val x = request.headers.get("X-Forwarded-Proto")
        val ua = request.headers.get("User-Agent")
        val host = request.host
        if (ua.isDefined && ua.get.startsWith("ELB-HealthChecker") && MarbleConfig.HealthCheck) {
            Some(EtcController.healthCheck)
        } else if (Play.isProd && MarbleConfig.RedirectHttp && (x.isEmpty || !x.get.contains("https")) ) {
            Some(EtcController.redirectHttp)
        } else if (host == "marble.co" && MarbleConfig.RedirectToWWW) {
            Some(Action{MovedPermanently("https://www.themarble.co" + request.uri)})
        } else if (ua.isDefined && MarbleConfig.RedirectMobile) {
            val uaInfo = new UAgentInfo(ua.get, request.headers.get("Accept").getOrElse(""))
            if (uaInfo.isMobilePhone) {
                if (!host.startsWith("m.")) {
                    Some(Action{MovedPermanently("https://m.themarble.co" + request.uri)})
                } else {
                    super.onRouteRequest(request)
                }
            } else {
                super.onRouteRequest(request)
            }
        } else {
            super.onRouteRequest(request)
        }
    }

    override def onError(request: RequestHeader, ex: Throwable) = {
        if (Play.isProd) {
            Future.successful(InternalServerError("Oops. Something broke..."))
        } else {
            super.onError(request, ex)
        }
    }

}

case class StaticPrefixMissing(message: String) extends Exception(message)
