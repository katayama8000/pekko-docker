// Builds a single self-contained "fat" jar so the Docker image
// only needs a JRE at runtime (no sbt, no dependency resolution).
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.3.0")
