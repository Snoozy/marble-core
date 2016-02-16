package com.marble.core.data.aws

import java.io.{FileInputStream, InputStream, File}
import java.util.UUID
import javax.imageio.ImageIO
import com.sksamuel.scrimage.nio.JpegWriter
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
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
                        val obj3 = new PutObjectRequest(bucketName, key + "_med", tempMed.file)
                        val resByte3 = DigestUtils.md5(new FileInputStream(tempMed.file))
                        val streamMD53 = new String(Base64.encodeBase64(resByte3))
                        val metadata3 = new ObjectMetadata()
                        metadata3.setContentType("image/" + (if(format == "gif") format else "jpeg"))
                        metadata3.setContentMD5(streamMD53)
                        obj3.setMetadata(metadata3)
                        s3client.putObject(obj3)
                    } else {
                        val tempThumb = TemporaryFile()
                        val tempProf = TemporaryFile()
                        img.cover(50, 50).output(tempThumb.file)
                        img.cover(200, 200).output(tempProf.file)
                        val metadata1 = new ObjectMetadata()
                        metadata1.setContentType("image/" + (if(format == "gif") format else "jpeg"))
                        val temp = new PutObjectRequest(bucketName, key + "_small", tempThumb.file)
                        val resByte1 = DigestUtils.md5(new FileInputStream(tempThumb.file))
                        val streamMD51 = new String(Base64.encodeBase64(resByte1))
                        metadata1.setContentMD5(streamMD51)

                        val prof = new PutObjectRequest(bucketName, key + "_prof", tempProf.file)
                        val resByte2 = DigestUtils.md5(new FileInputStream(tempProf.file))
                        val streamMD52 = new String(Base64.encodeBase64(resByte2))
                        val metadata2 = new ObjectMetadata()
                        metadata2.setContentType("image/" + (if(format == "gif") format else "jpeg"))
                        metadata2.setContentMD5(streamMD52)
                        temp.setMetadata(metadata1)
                        prof.setMetadata(metadata2)
                        s3client.putObject(temp)
                        s3client.putObject(prof)
                    }
                    val metadata = new ObjectMetadata()
                    metadata.setContentType("image/" + (if(format == "gif") format else "jpeg"))
                    val obj = new PutObjectRequest(bucketName, key, tempNormal.file)
                    val resByte = DigestUtils.md5(new FileInputStream(tempNormal.file))
                    val streamMD5 = new String(Base64.encodeBase64(resByte))
                    metadata.setContentMD5(streamMD5)
                    obj.setMetadata(metadata)
                    s3client.putObject(obj)
                } else {
                    if (file.isDefined) {
                        val obj = new PutObjectRequest(bucketName, key, file.get)
                        val metadata = new ObjectMetadata()
                        metadata.setContentType("image/" + (if(format == "gif") format else "jpeg"))
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
                case e: AmazonClientException => play.api.Logger.warn("Amazon Client Exception. Error: " + e.getMessage)
                    None
                case e: AmazonServiceException => play.api.Logger.warn("Amazon Service Exception. Error: " + e.getMessage)
                    None
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
            val tempUrl = TemporaryFile()
            val urlObj = new URL(url)
            FileUtils.copyURLToFile(urlObj, tempUrl.file)
            if (tempUrl.file.length() < 4000000) {
                upload(tempUrl.file, profile = profile)
            } else {
                None
            }
        } catch {
            case e: Exception =>
                play.api.Logger.warn(url + " failed")
                None
        }
    }

}
