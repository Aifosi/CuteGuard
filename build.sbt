name := "CuteGuard"

val scala3Version = "3.4.2"
ThisBuild / scalaVersion := scala3Version

// Used for scala fix
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

ThisBuild / scalafixOnCompile := true
ThisBuild / scalafmtOnCompile := true

enablePlugins(JavaAppPackaging, DockerPlugin, AshScriptPlugin)

ThisBuild / publish / skip                      := true
ThisBuild / githubWorkflowJavaVersions          := Seq(JavaSpec.temurin("17"))
ThisBuild / crossScalaVersions                  := List(scala3Version)
ThisBuild / githubWorkflowIncludeClean          := false
ThisBuild / githubWorkflowTargetBranches        := Seq("master")
ThisBuild / githubWorkflowTargetPaths           := Paths.Include(List("**version.sbt"))
ThisBuild / githubWorkflowPublishTargetBranches := Seq(RefPredicate.Equals(Ref.Branch("master")))

ThisBuild / githubWorkflowPublishPreamble := Seq(
  WorkflowStep.Use(
    name = Some("Login to DockerHub"),
    ref = UseRef.Public("docker", "login-action", "v2"),
    params = Map(
      "username" -> "${{ secrets.DOCKERHUB_USERNAME }}",
      "password" -> "${{ secrets.DOCKERHUB_PASS }}",
    ),
  ),
)

ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    List("Docker / publish"),
    name = Some("Publish to docker hub"),
  ),
)

ThisBuild / githubWorkflowJobSetup ++= Seq(
  WorkflowStep.Sbt(
    List("+scalafmtCheckAll", "scalafmtSbtCheck"),
    name = Some("Scalafmt"),
  ),
)
Universal / javaOptions            ++= Seq(
  "-Dconfig.file=/opt/docker/conf/application.conf",
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
  apacheLang,
  catsEffect,
  circeCore,
  circeParser,
  catsRetry,
  fs2,
  fs2IO,
  jda,
  log4cats,
  logbackClassic,
  pureconfig,
  pureconfigCE,
)

lazy val apacheLang     = "org.apache.commons"     % "commons-lang3"          % "3.14.0"
lazy val catsEffect     = "org.typelevel"         %% "cats-effect"            % "3.5.0"
lazy val circeCore      = "io.circe"              %% "circe-core"             % "0.14.7"
lazy val circeParser    = "io.circe"              %% "circe-parser"           % circeCore.revision
lazy val catsRetry      = "com.github.cb372"      %% "cats-retry"             % "3.1.3"
lazy val fs2            = "co.fs2"                %% "fs2-core"               % "3.10.2"
lazy val fs2IO          = "co.fs2"                %% "fs2-io"                 % fs2.revision
lazy val jda            = "net.dv8tion"            % "JDA"                    % "5.0.0-beta.20"
lazy val log4cats       = "org.typelevel"         %% "log4cats-slf4j"         % "2.6.0"
lazy val logbackClassic = "ch.qos.logback"         % "logback-classic"        % "1.5.6"
lazy val pureconfig     = "com.github.pureconfig" %% "pureconfig-core"        % "0.17.7"
lazy val pureconfigCE   = pureconfig.organization %% "pureconfig-cats-effect" % pureconfig.revision
