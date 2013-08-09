import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "oncue-service"
  val appVersion      = "0.9.3-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    javaCore,
    javaJdbc,
    javaEbean
  )

  def customLessEntryPoints(base: File): PathFinder = (
    (base / "app" / "assets" / "stylesheets" / "bootstrap" * "bootstrap.less") +++
    (base / "app" / "assets" / "stylesheets" / "bootstrap" * "responsive.less") +++
    (base / "app" / "assets" / "stylesheets" * "*.less"))

  val main = play.Project(appName, appVersion, appDependencies).settings(
  	lessEntryPoints <<= baseDirectory(customLessEntryPoints)
  )
}
