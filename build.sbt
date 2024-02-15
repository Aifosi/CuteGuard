version := "1.0.0"

name := "CuteGuard"

val scala3Version = "3.4.0"
scalaVersion := scala3Version

// Used for scala fix
semanticdbEnabled := true
semanticdbVersion := scalafixSemanticdb.revision

scalafixOnCompile := true
scalafmtOnCompile := true

enablePlugins(JavaAppPackaging, DockerPlugin, AshScriptPlugin)

publish / skip                      := true
githubWorkflowJavaVersions          := Seq(JavaSpec.temurin("17"))
crossScalaVersions                  := List(scala3Version)
githubWorkflowIncludeClean          := false
githubWorkflowTargetBranches        := Seq("master")
githubWorkflowTargetPaths           := Paths.Include(List("**version.sbt"))
githubWorkflowPublishTargetBranches := Seq(RefPredicate.Equals(Ref.Branch("master")))

githubWorkflowPublishPreamble := Seq(
  WorkflowStep.Use(
    name = Some("Login to DockerHub"),
    ref = UseRef.Public("docker", "login-action", "v2"),
    params = Map(
      "username" -> "${{ secrets.DOCKERHUB_USERNAME }}",
      "password" -> "${{ secrets.DOCKERHUB_PASS }}",
    ),
  ),
)

githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("Docker / publish"),
    name = Some("Publish to docker hub"),
  ),
)

githubWorkflowJobSetup ++= Seq(
  WorkflowStep.Sbt(
    List("+scalafmtCheckAll", "scalafmtSbtCheck"),
    name = Some("Scalafmt"),
  ),
)

Docker / dockerRepository := Some("aifosi")
dockerUpdateLatest        := true
dockerBaseImage           := "openjdk:17-jdk"
publish / skip            := false
dockerBuildOptions        += "--platform=linux/amd64"

javacOptions  ++= Seq("-Xlint", "-encoding", "UTF-8")
scalacOptions ++= Seq(
  "-explain",                      // Explain errors in more detail.
  "-explain-types",                // Explain type errors in more detail.
  "-indent",                       // Allow significant indentation.
  "-new-syntax",                   // Require `then` and `do` in control expressions.
  "-feature",                      // Emit warning and location for usages of features that should be imported explicitly.
  "-source:future",                // better-monadic-for
  "-language:higherKinds",         // Allow higher-kinded types
  "-language:implicitConversions", // Allow implicit conversions
  "-deprecation",                  // Emit warning and location for usages of deprecated APIs.
  "-Wunused:all",                  // Emit warnings for unused imports, local definitions, explicit parameters implicit, parameters method, parameters
  "-Xcheck-macros",
)

libraryDependencies ++= Seq(
  catsEffect,
  circeCore,
  circeParser,
  catsRetry,
  doobieCore,
  doobiePostgres,
  flyway,
  fs2,
  fs2Kafka,
  jda,
  log4cats,
  logbackClassic,
  postgres,
  pureconfig,
  pureconfigCE,
)

lazy val catsEffect     = "org.typelevel"         %% "cats-effect"            % "3.5.0"
lazy val circeCore      = "io.circe"              %% "circe-core"             % "0.14.5"
lazy val circeParser    = "io.circe"              %% "circe-parser"           % circeCore.revision
lazy val catsRetry      = "com.github.cb372"      %% "cats-retry"             % "3.1.0"
lazy val doobieCore     = "org.tpolecat"          %% "doobie-core"            % "1.0.0-RC2"
lazy val doobiePostgres = doobieCore.organization %% "doobie-postgres"        % doobieCore.revision
lazy val flyway         = "org.flywaydb"           % "flyway-core"            % "9.16.0"
lazy val fs2            = "co.fs2"                %% "fs2-core"               % "3.7.0"
lazy val fs2Kafka       = "com.github.fd4s"       %% "fs2-kafka"              % "2.5.0"
lazy val jda            = "net.dv8tion"            % "JDA"                    % "5.0.0-beta.20"
lazy val log4cats       = "org.typelevel"         %% "log4cats-slf4j"         % "2.5.0"
lazy val logbackClassic = "ch.qos.logback"         % "logback-classic"        % "1.4.7"
lazy val postgres       = "org.postgresql"         % "postgresql"             % "42.5.4"
lazy val pureconfig     = "com.github.pureconfig" %% "pureconfig-core"        % "0.17.2"
lazy val pureconfigCE   = pureconfig.organization %% "pureconfig-cats-effect" % pureconfig.revision
