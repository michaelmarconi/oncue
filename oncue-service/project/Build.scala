import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "oncue-service"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    javaCore,
    javaJdbc,
    javaEbean,
    "oncue" % "oncue-common" % "1.0-SNAPSHOT",
    "oncue" % "oncue-queuemanager" % "1.0-SNAPSHOT",
    "oncue" % "oncue-scheduler" % "1.0-SNAPSHOT",
    "oncue" % "oncue-timedjobs" % "1.0-SNAPSHOT"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
	resolvers += "Local Maven Repository" at "file:///"+Path.userHome+"/.m2/repository"    
  )

}
