package com.marble.core.config

object MarbleConfig {

    // REDIRECTION CONFIG
    final val RedirectHttp = true
    final val RedirectMobile = true
    // should redirect marble.co to www.marble.co
    final val RedirectToWWW = true

    // ETC CONFIG
    // should be responding correctly to elb health checks
    final val HealthCheck = true

}
