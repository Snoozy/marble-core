import play.sbt.PlayImport._
import play.sbt.PlayScala
import play.sbt.routes.RoutesKeys._
import play.twirl.sbt.Import.TwirlKeys
import sbt._
import sbt.Keys._

object Build extends Build {
    lazy val marble = Project(id = "marble", base = file(".")).settings(
        name := "marble",
        version := "0.1",
        scalaVersion := "2.11.6",
        libraryDependencies ++= Seq(
            jdbc,
            cache,
            ws,
            filters,
            specs2 % Test,
            "com.typesafe.play" %% "anorm" % "2.4.0",
            "mysql" % "mysql-connector-java" % "5.1.27",
            "org.apache.commons" % "commons-lang3" % "3.3.2",
            "com.googlecode.xmemcached" % "xmemcached" % "2.0.0",
            "com.amazonaws" % "aws-java-sdk-s3" % "1.10.49",
            "io.argonaut" %% "argonaut" % "6.0.4",
            "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.2",
            "javax.mail" % "mail" % "1.4.7",
            "io.fastjson" % "boon" % "0.31",
            "net.debasishg" %% "redisclient" % "2.13",
            "com.mohiva" %% "play-html-compressor" % "0.5.0" exclude("rhino", "js"),
            "com.yahoo.platform.yui" % "yuicompressor" % "2.4.7" exclude("rhino", "js"),
            "com.github.jreddit" % "jreddit" % "1.0.2",
            "com.notnoop.apns" % "apns" % "1.0.0.Beta6",
            "com.sksamuel.scrimage" %% "scrimage-canvas" % "1.4.2"
        ),
        resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
        doc in Compile <<= target.map(_ / "none"),
        routesGenerator := InjectedRoutesGenerator,
        TwirlKeys.templateImports += "com.marble.core.data.db.models._",
        unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )
    ).enablePlugins(PlayScala)
}
