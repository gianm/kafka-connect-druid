scalaVersion := "2.10.6"

crossScalaVersions := Seq("2.10.6", "2.11.8")

organization := "io.imply"

licenses := Seq("Apache License, Version 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))

homepage := Some(url("https://github.com/implydata/kafka-connect-druid"))

publishMavenStyle := true

publishTo := Some("releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2/")

pomIncludeRepository := { _ => false }

pomExtra := (
  <scm>
    <url>https://github.com/implydata/kafka-connect-druid.git</url>
    <connection>scm:git:git@github.com:implydata/kafka-connect-druid.git</connection>
  </scm>
    <developers>
      <developer>
        <name>Gian Merlino</name>
        <organization>Imply Data</organization>
        <organizationUrl>http://imply.io/</organizationUrl>
      </developer>
    </developers>)

// Target Java 7
scalacOptions += "-target:jvm-1.7"
javacOptions in compile ++= Seq("-source", "1.7", "-target", "1.7")

val tranquilityVersion = "0.7.4"
val kafkaVersion = "0.9.0.1"

libraryDependencies ++= Seq(
  "io.druid" %% "tranquility-core" % "0.7.4"
    exclude("com.amazonaws", "aws-java-sdk-core")
    exclude("org.apache.httpcomponents", "httpclient")
    exclude("org.apache.commons", "commons-dbcp2")
    exclude("io.airlift", "airline"),
  "org.apache.kafka" % "connect-api" % kafkaVersion
)

// Testing
libraryDependencies ++= Seq(
  "junit" % "junit" % "4.12" % "test",
  "org.scalatest" %% "scalatest" % "2.2.5" % "test"
)

// Packaging
assemblyMergeStrategy in assembly := {
  case PathList("org", "eclipse", "aether", xs@_*) => MergeStrategy.first
  case PathList("META-INF", "sisu", xs@_*) => MergeStrategy.discard
  case "META-INF/services/io.druid.initialization.DruidModule" => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
