package com.marble.utils.play.errors

object BoardNameInvalid extends Error {
    val message = "Board name invalid."
    val code = ErrorCode.BoardNameInvalid
}