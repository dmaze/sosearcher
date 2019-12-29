name := """sosearcher"""
organization := "org.dmaze"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).disablePlugins(PlayLayoutPlugin)

scalaVersion := "2.13.1"

enablePlugins(JavaAgent)
javaAgents += "org.aspectj" % "aspectjweaver" % "1.9.5" % "runtime"

libraryDependencies ++= Seq(
  ehcache,
  guice,
  ws,
  "com.typesafe.play" %% "play-slick" % "5.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "5.0.0",
  "io.kontainers" %% "micrometer-akka" % "0.10.2",
  "io.micrometer" % "micrometer-registry-jmx" % "1.3.2",
  "io.micrometer" % "micrometer-registry-prometheus" % "1.3.2",
  "net.codingwell" %% "scala-guice" % "4.2.6",
  "org.xerial" % "sqlite-jdbc" % "3.30.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "org.dmaze.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "org.dmaze.binders._"
