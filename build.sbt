name := "Toolkit"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies += "org.parboiled" %% "parboiled" % "2.1.3"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq("com.flyobjectspace" %% "flyscala" % "2.2.0-SNAPSHOT")

libraryDependencies += "growin" %% "exonode" % "0.1-SNAPSHOT"

