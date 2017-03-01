name := "Toolkit"

organization := "growin"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.1"

scalacOptions += "-feature"

libraryDependencies += "org.parboiled" %% "parboiled" % "2.1.3"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies += "com.flyobjectspace" %% "flyscala" % "2.2.0-SNAPSHOT"

libraryDependencies += "growin" %% "exonode" % "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "io.swave" %% "swave-core" % "0.7.0" //,
  //"io.swave" %% "swave-akka-compat"   % "0.7.0", // if required
  //"io.swave" %% "swave-scodec-compat" % "0.7.0", // if required
  //"io.swave" %% "swave-testkit"       % "0.7.0"  // if required
)

