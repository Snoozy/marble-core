package com.marble.core.config

object MarbleConfig {

    // REDIRECTION CONFIG
    final val RedirectHttp = true
    final val RedirectMobile = false
    // should redirect themarble.co to www.themarble.co
    final val RedirectToWWW = true

    final val SuperUser = 1

    // 0 based. value of 0 means no comment replies
    final val MaxCommentDepth = 1

    // ETC CONFIG
    // should be responding correctly to elb health checks
    final val HealthCheck = true
}