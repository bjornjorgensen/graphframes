// You may use this file to add plugin dependencies for sbt.
resolvers += "Spark Packages repo" at "https://repos.spark-packages.org/"

addSbtPlugin("org.spark-packages" % "sbt-spark-package" % "0.2.7-asl")

// scalacOptions in (Compile,doc) := Seq("-groups", "-implicits")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.9")

addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")
