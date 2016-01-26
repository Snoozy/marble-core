package com.marble.core.web.controllers

import play.api.Play
import play.api.Play.current
import play.api.mvc._

class RemoteURLController extends Controller {

    private val CdnUrl = "https://static.themarble.co/"
    private lazy val staticPrefix = Play.current.configuration.getString("static.prefix").get

    lazy val desktopJs = js("desktop")
    lazy val desktopCss = css("desktop")

    lazy val mobileJs = js("mobile")
    lazy val mobileCss = css("mobile")

    private def js(platform: String) = {
        Play.isProd match {
            case true => "<script type=\"text/javascript\" src=\"" + CdnUrl + "js/bundle-" + platform + "-" + staticPrefix + ".js" + "\"></script>"
            case false =>
                val files = Play.getFile("public/" + platform + "/js")
                files.list.toList.sortWith(_ < _).map { s =>
                    if (!s.startsWith(".")) {
                        "<script type=\"text/javascript\" src=\"/assets/" + platform + "/js/" + s + "\"></script>"
                    } else
                        ""
                }.mkString("\n")
        }
    }

    private def css(platform: String) = {
        Play.isProd match {
            case true => "<link rel=\"stylesheet\" href=\"" + CdnUrl + "css/bundle-" + platform + "-" + staticPrefix + ".css" + "\" />"
            case false =>
                val files = Play.getFile("public/" + platform + "/css")
                files.list.map { s =>
                    if (!s.startsWith(".")) {
                        "<link rel=\"stylesheet\" href=\"/assets/" + platform + "/css/" + s + "\">"
                    } else
                        ""
                }.mkString("\n")
        }
    }

}
