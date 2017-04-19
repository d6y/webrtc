name := "webrtc-app"
organization := "underscore.io"
scalaVersion in ThisBuild := "2.12.2"

lazy val root = project.in(file(".")).
  aggregate(webrtcJS, webrtcJVM).
  settings(
    publish := {},
    publishLocal := {}
  )

lazy val webrtc = crossProject.in(file(".")).
  settings(
    name := "webrtc",
    version := "1.0.0",
    scalacOptions := Seq(
        "-deprecation",
        "-encoding", "UTF-8",
        "-unchecked",
        "-deprecation",
        "-feature",
        "-language:implicitConversions",
        "-language:postfixOps",
        "-Ywarn-dead-code",
        "-Ywarn-value-discard",
        "-Xlint",
        "-Xfatal-warnings"
    ),
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % "1.13.4" % "test",
      "org.scalatest"  %%% "scalatest"  % "3.0.1"  % "test"
    )
  ).
  jvmSettings(
    libraryDependencies ++= Seq(
      "com.spinoco"  %% "fs2-http"      % "0.1.7",
      "org.scala-js" %% "scalajs-stubs" % scalaJSVersion % "provided"
    ),
    mainClass := Some("io.underscore.webrtc.Main")
  ).
  jsSettings(
  )

lazy val webrtcJVM = webrtc.jvm
lazy val webrtcJS = webrtc.js
