package com.marble.utils.play.errors

object ErrorCode {

    sealed trait Code { def id: Int}

    case object UserNotAuthenticated extends Code { val id = 10 }

    case object PasswordInvalid extends Code { val id = 20 }

    case object UsernameTaken extends Code { val id = 30 }

}
