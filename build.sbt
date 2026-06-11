ThisBuild / scalaVersion := "2.13.18"
ThisBuild / organization := "com.example"
ThisBuild / version      := "0.1.0"

lazy val pekkoVersion     = "1.1.3"
lazy val pekkoHttpVersion = "1.1.0"

lazy val root = (project in file("."))
  .settings(
    name := "pekko-docker",
    libraryDependencies ++= Seq(
      // Pekko actors (typed) — the core actor model
      "org.apache.pekko" %% "pekko-actor-typed"      % pekkoVersion,
      // Pekko streams — required transitively by Pekko HTTP
      "org.apache.pekko" %% "pekko-stream"           % pekkoVersion,
      // SLF4J logger implementation used in application.conf
      "org.apache.pekko" %% "pekko-slf4j"            % pekkoVersion,
      // Pekko HTTP — REST layer in front of the actor
      "org.apache.pekko" %% "pekko-http"             % pekkoHttpVersion,
      // JSON (un)marshalling via spray-json
      "org.apache.pekko" %% "pekko-http-spray-json"  % pekkoHttpVersion,
      // Logging backend for Pekko's SLF4J logger
      "ch.qos.logback"    % "logback-classic"        % "1.5.12"
    )
  )

// Pekko ships a `reference.conf` inside every jar; these must be CONCATENATED,
// not overwritten, or the actor system fails to start in the fat jar.
assembly / assemblyMergeStrategy := {
  case "reference.conf"                     => MergeStrategy.concat
  case "application.conf"                   => MergeStrategy.concat
  case PathList("META-INF", "services", _*) => MergeStrategy.concat
  case PathList("META-INF", _*)             => MergeStrategy.discard
  case "module-info.class"                  => MergeStrategy.discard
  case _                                    => MergeStrategy.first
}
