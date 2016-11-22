import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys

/**
 * Please use plain text editor to edit this file instead of NetBeans (To be supported)
 */
object Build extends sbt.Build {

  lazy val root = Project("SampleProject", file("."))
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++= Dependencies.basic)

  lazy val basicSettings = Seq(
    organization := "your.organization",
    version := "0.1.0",
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq("-unchecked", "-deprecation"),
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    resolvers ++= Seq(
      "Sonatype OSS Releases" at "https://oss.sonatype.org/content/repositories/releases",
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"))

  // scalariform code format settings
  SbtScalariform.scalariformSettings // enable scalariformSettings
  import scalariform.formatter.preferences._
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(RewriteArrowSymbols, false)
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(DoubleIndentClassDeclaration, true)
    .setPreference(IndentSpaces, 2)
}

object Dependencies {
  // ---- define dependencies libs
  var basic: Seq[ModuleID] = Seq(
   "io.spray" %%  "spray-can"     % "1.3.3" withSources() withJavadoc(),
   "io.spray" %%  "spray-routing" % "1.3.3" withSources() withJavadoc(),
   "io.spray" %%  "spray-json" % "1.3.2",
   "io.spray" %%  "spray-client" % "1.3.3",
   "io.spray" %%  "spray-testkit" % "1.3.3"  % "test",
   "com.typesafe.akka" %% "akka-actor" % "2.3.11",
   "com.typesafe.akka" %% "akka-remote" % "2.3.11",
   "com.typesafe.akka" %% "akka-contrib" % "2.3.11",
   "com.typesafe.akka" %% "akka-testkit" % "2.3.11",
   "org.scalatest" %% "scalatest" % "2.2.4" % "test",
   "commons-io" % "commons-io" % "2.4" % "test",
   "org.specs2"          %%  "specs2-core"   % "2.3.11" % "test",
   "org.scalaz"          %%  "scalaz-core"   % "7.1.0"
    )

}