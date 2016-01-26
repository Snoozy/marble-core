package com.marble.core.api

import com.marble.core.api.apple.PushNotifications
import com.marble.core.api.controllers.EtcController
import com.marble.core.data.cache.Memcached
import com.marble.core.data.cache.Redis
import play.api.Play.current
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._
import play.api.Logger
import play.api.{Application, GlobalSettings, Play}
import play.filters.gzip.GzipFilter

import scala.concurrent.Future

/**
 * Global file for the application that handles http request rejection if and initializes the
 * memcached instance on application start.
 */

object Global extends WithFilters(new GzipFilter()) with GlobalSettings {

    override def onRouteRequest(request: RequestHeader): Option[Handler] = {
        super.onRouteRequest(request)
        /*
        val x = request.headers.get("X-Forwarded-Proto")
        val ua = request.headers.get("User-Agent")
        if (Play.isProd && (!x.isDefined || x.size == 0 || !x.get.contains("https")) && !(ua.isDefined && ua.get.startsWith("ELB-HealthChecker"))) {
            Some(EtcController.rejectHttp)
        } else if (ua.isDefined && ua.get.startsWith("ELB-HealthChecker")) {
            Some(EtcController.healthCheck)
        } else {
            super.onRouteRequest(request)
        }
        */
    }

    override def onError(request: RequestHeader, ex: Throwable) = {
        Future.successful(InternalServerError(Json.obj("error" -> "Unknown error.")))
    }

    override def onHandlerNotFound(request: RequestHeader) = {
        Future.successful(NotFound(Json.obj("error" -> "Route not found.")))
    }

}
