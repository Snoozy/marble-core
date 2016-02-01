package com.marble.core.data.aws

import java.io.{InputStream, File}
import java.util.UUID
import javax.imageio.ImageIO
import com.sksamuel.scrimage.nio.JpegWriter
import play.api.libs.Files._
import java.net.URL
import org.apache.commons.io.FileUtils
import com.marble.core.data.db.models._

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest}
import com.amazonaws.{AmazonClientException, AmazonServiceException}
import play.api.Play
import com.sksamuel.scrimage._

object S3 {

    private final val bucketName = "cillo"

    def uploadImg(img: Image, profile: Boolean = false, file: Option[File] = None, original: Option[Image] = None, uuid: String = UUID.randomUUID().toString, format: String = "jpeg"): Option[Int] = {
        val aws_key = Play.current.configuration.getString("aws.key")
        val aws_secret = Play.current.configuration.getString("aws.secret")
        if (aws_key.isDefined && aws_secret.isDefined) {
            try {
                val key = "image/" + uuid
                val aws_creds = new BasicAWSCredentials(aws_key.get, aws_secret.get)
                val s3client = new AmazonS3Client(aws_creds)
                val metadata = new ObjectMetadata()
                metadata.setContentType("image/" + (if(format == "gif") format else "jpeg"))
                if (format != "gif") {
                    implicit val writer = JpegWriter.Default
                    val tempNormal = TemporaryFile()
                    if (original.isDefined) {
                            original.get.bound(2000, 2000).output(tempNormal.file)
                    } else {
                            img.bound(2000, 2000).output(tempNormal.file)
                    }
                    if (!profile) {
                        val tempMed = TemporaryFile()
                        img.bound(550, 550).output(tempMed.file)
                        val obj = new PutObjectRequest(bucketName, key + "_med", tempMed.file)
                        obj.setMetadata(metadata)
                        s3client.putObject(obj)
                    } else {
                        val tempThumb = TemporaryFile()
                        val tempProf = TemporaryFile()
                        img.cover(50, 50).output(tempThumb.file)
                        img.cover(200, 200).output(tempProf.file)
                        val temp = new PutObjectRequest(bucketName, key + "_small", tempThumb.file)
                        val prof = new PutObjectRequest(bucketName, key + "_small", tempProf.file)
                        temp.setMetadata(metadata)
                        temp.setMetadata(metadata)
                        s3client.putObject(temp)
                        s3client.putObject(prof)
                    }
                    val obj = new PutObjectRequest(bucketName, key, tempNormal.file)
                    obj.setMetadata(metadata)
                    s3client.putObject(obj)
                } else {
                    if (file.isDefined) {
                        val obj = new PutObjectRequest(bucketName, key, file.get)
                        obj.setMetadata(metadata)
                        s3client.putObject(obj)
                    } else {
                        throw new IllegalStateException("Image not supported.")
                    }
                }
                if (format == "gif") {
                    Media.create(1, uuid).map(_.toInt)
                } else {
                    Media.create(0, uuid).map(_.toInt)
                }
            } catch {
                case e: AmazonClientException => play.api.Logger.debug("Amazon Client Exception. Error: " + e.getMessage)
                    None
                case e: AmazonServiceException => play.api.Logger.debug("Amazon Service Exception. Error: " + e.getMessage)
                    None
                case _: Throwable => None
            }
        } else None
    }

    def upload(file: File, profile: Boolean = false, x: Option[Double] = None, y: Option[Double] = None,
               height: Option[Double] = None, width: Option[Double] = None): Option[Int] = {
        val reader = ImageIO.getImageReaders(ImageIO.createImageInputStream(file)).next()
        val format = reader.getFormatName
        if (x.isDefined && y.isDefined && width.isDefined && height.isDefined) {
            val temp = Image.fromFile(file)
            val imgWidth = temp.width
            val imgHeight = temp.height
            if (x.get > imgWidth || width.get > imgWidth || y.get > imgHeight || height.get > imgHeight) {
                uploadImg(Image.fromFile(file), profile = profile, format = reader.getFormatName)
            } else {
                val img = temp.trim(x.get.toInt, y.get.toInt, imgWidth - (width.get.toInt + x.get.toInt), imgHeight - (height.get.toInt + y.get.toInt))
                uploadImg(img, profile = profile, original = Some(temp))
            }
        } else {
            if (format != "gif") {
                uploadImg(Image.fromFile(file), profile = profile, format = format)
            } else {
                uploadImg(Image.fromFile(file), profile = profile, format = format, file = Some(file))
            }
        }
    }

    def uploadURL(url: String, profile: Boolean = false): Option[Int] = {
        try {
            val tempUrl = TemporaryFile("aoifjweoijf")
            val urlObj = new URL(url)
            FileUtils.copyURLToFile(urlObj, tempUrl.file)
            if (tempUrl.file.length() < 4000000) {
                upload(tempUrl.file, profile = profile)
            } else {
                None
            }
        } catch {
            case e: Exception =>
                play.api.Logger.debug(url + " failed")
                None
        }
    }

}
