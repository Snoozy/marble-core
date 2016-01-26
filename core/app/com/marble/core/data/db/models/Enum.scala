package com.marble.core.data.db.models

object Enum {

    sealed trait EntityType { def id: Int}

    object EntityType {
        def fromInt(id: Int): Option[EntityType] = {
            id match {
                case 0 => Some(Post)
                case 1 => Some(Comment)
                case _ => None
            }
        }
        case object Post extends EntityType { val id = 0 }
        case object Comment extends EntityType { val id = 1 }
    }

    sealed trait ActionType { def id: Int}

    object ActionType {
        def fromInt(id: Int): Option[ActionType] = {
            id match {
                case 0 => Some(Vote)
                case 1 => Some(Reply)
                case _ => None
            }
        }
        case object Vote extends ActionType { val id = 0 }
        case object Reply extends ActionType { val id = 1 }
    }

}
