name := "Toolkit"

organization := "growin"

version := "1.3-SNAPSHOT"

scalaVersion := "2.12.1"

scalacOptions ++= Seq("-feature", "-deprecation")

libraryDependencies += "org.parboiled" %% "parboiled" % "2.1.4"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "com.flyobjectspace" %% "flyscala" % "2.2.0-SNAPSHOT"

libraryDependencies += "growin" %% "exonode" % "1.3-SNAPSHOT"

libraryDependencies += "io.swave" %% "swave-core" % "0.7.0"

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.24"