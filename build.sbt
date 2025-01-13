// Your sbt build file. Guides on how to write one can be found at
// http://www.scala-sbt.org/0.13/docs/index.html

import ReleaseTransformations._

// Define versions
val sparkVer = sys.props.getOrElse("spark.version", "3.5.4")
val sparkBranch = sparkVer.substring(0, 3)
val defaultScalaVer = sparkBranch match {
  case "3.5" => "2.12.18"
  case _ => throw new IllegalArgumentException(s"Unsupported Spark version: $sparkVer.")
}
val scalaVer = sys.props.getOrElse("scala.version", defaultScalaVer)
val defaultScalaTestVer = scalaVer match {
  case s if s.startsWith("2.12") || s.startsWith("2.13") => "3.0.8"
}

// Basic settings
ThisBuild / version := {
  val baseVersion = (ThisBuild / version).value
  s"$baseVersion-spark$sparkBranch"
}
ThisBuild / scalaVersion := scalaVer
ThisBuild / crossScalaVersions := Seq("2.12.18", "2.13.8")
ThisBuild / isSnapshot := (ThisBuild / version).value.contains("SNAPSHOT")

// Project info
name := "graphframes"
organization := "org.graphframes"
licenses := Seq("Apache-2.0" -> url("http://opensource.org/licenses/Apache-2.0"))

// Dependencies
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-graphx" % sparkVer % "provided",
  "org.apache.spark" %% "spark-sql" % sparkVer % "provided",
  "org.apache.spark" %% "spark-mllib" % sparkVer % "provided",
  "org.slf4j" % "slf4j-api" % "1.7.16",
  "org.scalatest" %% "scalatest" % defaultScalaTestVer % "test",
  "com.github.zafarkhaja" % "java-semver" % "0.9.0" % "test" // MIT license
)

// Assembly settings
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x if x.endsWith("module-info.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

assembly / assemblyExcludedJars := {
  val cp = (assembly / fullClasspath).value
  cp filter {_.data.getName.startsWith("spark-")}
}

assembly / assemblyJarName := s"${name.value}-assembly_${scalaVersion.value}-${version.value}.jar"

// Compiler settings
scalacOptions ++= Seq("-deprecation", "-feature")
Compile / doc / scalacOptions ++= Seq(
  "-groups",
  "-implicits",
  "-skip-packages", Seq("org.apache.spark").mkString(":"))
Test / doc / scalacOptions ++= Seq("-groups", "-implicits")

// Test settings
Test / fork := true
parallelExecution := false

// Java settings
javacOptions ++= {
  val majorVersion = System.getProperty("java.version").split("\\.")(0).toInt
  if (majorVersion >= 17) {
    Seq(
      "-source", "17",
      "-target", "17",
      "--release", "17"
    )
  } else {
    Seq(
      "-source", "11",
      "-target", "11"
    )
  }
}

// Add Java module flags based on version
ThisBuild / javaOptions ++= {
  val majorVersion = System.getProperty("java.version").split("\\.")(0).toInt
  if (majorVersion >= 9) {
    Seq(
      "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
      "--add-opens=java.base/java.lang=ALL-UNNAMED",
      "--add-opens=java.base/java.nio=ALL-UNNAMED",
      "--add-opens=java.base/java.util=ALL-UNNAMED",
      "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
      "--add-opens=java.base/java.math=ALL-UNNAMED"
    )
  } else Seq.empty
}

// Modern JVM settings
Global / excludeLintKeys ++= Set(
  run / fork,
  run / javaOptions
)
ThisBuild / fork := true
ThisBuild / Test / fork := true

// Other settings
concurrentRestrictions in Global := Seq(Tags.limitAll(1))
autoAPIMappings := true
coverageHighlighting := false

// Release settings
releaseProcess := Seq[ReleaseStep](
  inquireVersions,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion
)

credentials += Credentials(Path.userHome / ".ivy2" / ".sbtcredentials")
