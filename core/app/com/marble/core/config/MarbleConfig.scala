package com.marble.core.config

object MarbleConfig {

    // REDIRECTION CONFIG
    final val RedirectHttp = true
    final val RedirectMobile = false
    // should redirect themarble.co to www.themarble.co
    final val RedirectToWWW = true

    final val SuperUser = 1

    // ETC CONFIG
    // should be responding correctly to elb health checks
    final val HealthCheck = true
}