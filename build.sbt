name := "siren-scala-shapeless"

organization := "pl.immutables"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless"   % "2.3.2",
  "com.yetu"    %% "siren-scala" % "0.5.1",
  "org.specs2"  %% "specs2-core" % "3.8.5" % "test"
)

resolvers += "yetu Bintray Repo" at "http://dl.bintray.com/yetu/maven/"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
