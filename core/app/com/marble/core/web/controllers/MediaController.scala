package com.marble.core.web.controllers

import com.marble.core.data.aws.S3
import com.marble.core.data.Constants
import play.api.libs.Files
import play.api.mvc._

object MediaController {

    final val MediaIdentifier = "^media-\\S.*$".r

    def uploadMedia(files: Seq[MultipartFormData.FilePart[Files.TemporaryFile]]): Seq[Int] = {
        val res = files.map { media =>
            val mediaFile = media.ref.file
            if (mediaFile.length() > Constants.MaxMediaSize)
                -1
            else {
                val id = S3.upload(mediaFile)
                if (id.isEmpty)
                    -1
                else {
                    id.get
                }
            }
        }

        if (!res.contains(-1)) {
            res
        } else {
            throw new MediaUploadException("Media failed to upload")
        }
    }

    case class MediaUploadException(message: String) extends RuntimeException(message)

}
