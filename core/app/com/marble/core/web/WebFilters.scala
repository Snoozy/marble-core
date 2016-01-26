package com.marble.core.web

import javax.inject.Inject
import play.api.http.HttpFilters
import play.filters.gzip.GzipFilter
import com.googlecode.htmlcompressor.compressor.HtmlCompressor
import com.mohiva.play.htmlcompressor.HTMLCompressorFilter
import play.api.{Configuration, Environment, Mode}


class WebFilters @Inject() (
    gzip: GzipFilter,
    htmlCompressorFilter: CustomHTMLCompressorFilter
) extends HttpFilters {
    val filters = Seq(gzip, htmlCompressorFilter)
}

class CustomHTMLCompressorFilter @Inject() (
    val configuration: Configuration, environment: Environment)
    extends HTMLCompressorFilter {

    override val compressor: HtmlCompressor = {
        val c = new HtmlCompressor()
        c.setPreserveLineBreaks(false)
        c.setCompressCss(true)
        c.setCompressJavaScript(true)
        c.setRemoveComments(true)
        c.setRemoveIntertagSpaces(true)
        c.setRemoveHttpProtocol(true)
        c.setRemoveHttpsProtocol(true)
        c
    }
}