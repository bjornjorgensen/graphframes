// Your sbt build file. Guides on how to write one can be found at
// http://www.scala-sbt.org/0.13/docs/index.html

import ReleaseTransformations._

val sparkVer = sys.props.getOrElse("spark.version", "3.5.3")
val sparkBranch = sparkVer.substring(0, 3)
val defaultScalaVer = sparkBranch match {
  case "3.5" => "2.12.18"
  case _ => throw new IllegalArgumentException(s"Unsupported Spark version: $sparkVer.")
}
val scalaVer = sys.props.getOrElse("scala.version", defaultScalaVer)
val defaultScalaTestVer = scalaVer match {
  case s if s.startsWith("2.12") || s.startsWith("2.13") => "3.0.8"
}

sparkVersion := sparkVer

ThisBuild / scalaVersion := scalaVer

name := "graphframes"

organization := "org.graphframes"

ThisBuild / version := (ThisBuild / version).value + s"-spark$sparkBranch"

ThisBuild / isSnapshot := (ThisBuild / version).value.contains("SNAPSHOT")

// All Spark Packages need a license
licenses := Seq("Apache-2.0" -> url("http://opensource.org/licenses/Apache-2.0"))

// Remove these lines
// spName := "graphframes/graphframes"
// spAppendScalaVersion := true
// sparkComponents ++= Seq("graphx", "sql", "mllib")

// Add assembly settings
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x if x.endsWith("module-info.class") => MergeStrategy.discard
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

// Add Spark dependencies explicitly
libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-graphx" % sparkVer % "provided",
  "org.apache.spark" %% "spark-sql" % sparkVer % "provided",
  "org.apache.spark" %% "spark-mllib" % sparkVer % "provided"
)

// Exclude Spark from the assembly jar
assembly / assemblyExcludedJars := {
  val cp = (assembly / fullClasspath).value
  cp filter {_.data.getName.startsWith("spark-")}
}

// Name the assembly jar consistently
assembly / assemblyJarName := s"${name.value}-assembly_${scalaVersion.value}-${version.value}.jar"

// uncomment and change the value below to change the directory where your zip artifact will be created
// spDistDirectory := target.value

// add any Spark Package dependencies using spDependencies.
// e.g. spDependencies += "databricks/spark-avro:0.1"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.16"

libraryDependencies += "org.scalatest" %% "scalatest" % defaultScalaTestVer % "test"

libraryDependencies += "com.github.zafarkhaja" % "java-semver" % "0.9.0" % "test" // MIT license

parallelExecution := false

scalacOptions ++= Seq("-deprecation", "-feature")

Compile / doc / scalacOptions ++= Seq(
  "-groups",
  "-implicits",
  "-skip-packages", Seq("org.apache.spark").mkString(":"))

Test / doc / scalacOptions ++= Seq("-groups", "-implicits")

// This fixes a class loader problem with scala.Tuple2 class, scala-2.11, Spark 2.x
Test / fork := true

// Java 11 specific options
javacOptions ++= Seq(
  "-source", "11",
  "-target", "11"
)

// Conditionally add Java module flags based on Java version
javaOptions in Test ++= {
  val default = Seq(
    "-Xmx2048m",
    "-XX:ReservedCodeCacheSize=384m",
    "-XX:MaxMetaspaceSize=384m"
  )
  
  // Add module flags only for Java 9+
  if (sys.props("java.specification.version").toDouble >= 9) {
    default ++ Seq(
      "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
      "--add-opens=java.base/java.lang=ALL-UNNAMED",
      "--add-opens=java.base/java.nio=ALL-UNNAMED",
      "--add-opens=java.base/java.util=ALL-UNNAMED",
      "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
      "--add-opens=java.base/java.math=ALL-UNNAMED"
    )
  } else default
}

// Add settings for modern Java versions
Global / excludeLintKeys += run / fork
Global / excludeLintKeys += run / javaOptions

// Add Java options depending on version
ThisBuild / javaOptions ++= {
  val majorVersion = System.getProperty("java.version").split("\\.")(0).toInt
  if (majorVersion >= 17) {
    // Settings for Java 17+
    Seq(
      "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
      "--add-opens=java.base/java.lang=ALL-UNNAMED",
      "--add-opens=java.base/java.nio=ALL-UNNAMED",
      "--add-opens=java.base/java.util=ALL-UNNAMED",
      "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
      "--add-opens=java.base/java.math=ALL-UNNAMED",
      "-Djdk.io.File.enableADS=true"
    )
  } else Seq.empty
}

// Update memory settings for modern JVMs
ThisBuild / fork := true
ThisBuild / Test / fork := true

concurrentRestrictions in Global := Seq(
  Tags.limitAll(1))

autoAPIMappings := true

coverageHighlighting := false

// We only use sbt-release to update version numbers.
releaseProcess := Seq[ReleaseStep](
  inquireVersions,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  setNextVersion,
  commitNextVersion
)

credentials += Credentials(Path.userHome / ".ivy2" / ".sbtcredentials")
