package com.marble.core.api

import javax.inject.Inject
import play.api.http.HttpFilters
import play.filters.gzip.GzipFilter

class ApiFilters @Inject() (
    gzip: GzipFilter
) extends HttpFilters {
    val filters = Seq(gzip)
}