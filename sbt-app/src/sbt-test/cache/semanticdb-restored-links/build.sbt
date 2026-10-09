import java.nio.file.{ Files, Path }
import complete.DefaultParsers.*
import sbt.internal.util.CacheEventSummary

Global / localCacheDirectory := baseDirectory.value / "diskcache"

ThisBuild / scalaVersion := "3.8.4"
ThisBuild / semanticdbEnabled := true

// Like sbt-scapegoat's: compiles Compile's sources with Compile's options, so it writes SemanticDB
// into Compile's target root while its own semanticdbTargetRoot points elsewhere.
lazy val Custom = config("custom").extend(Compile)

lazy val root = project
  .in(file("."))
  .configs(Custom)
  .settings(
    inConfig(Custom)(Defaults.compileSettings),
    Custom / sources := (Compile / sources).value,
    Custom / scalacOptions := (Compile / scalacOptions).value :+ "-deprecation",
  )

def semanticdbDir: Def.Initialize[Path] =
  Def.setting((Compile / semanticdbTargetRoot).value.toPath.resolve("META-INF/semanticdb"))

lazy val checkHit = taskKey[Unit]("asserts the previous command was a pure cache hit")

checkHit := Def.uncached {
  val config = Def.cacheConfiguration.value
  val prev = config.cacheEventLog.previous match
    case s: CacheEventSummary.Data => s
    case _                         => sys.error("empty event log")
  streams.value.log.info(s"hitCount=${prev.hitCount} missCount=${prev.missCount}")
  assert(prev.missCount == 0, s"expected a pure cache hit but missCount=${prev.missCount}")
}

lazy val checkSemanticdb =
  inputKey[Unit]("asserts the SemanticDB files are regular files and A's holds the given member")

checkSemanticdb := {
  val member = spaceDelimited("<member>").parsed.headOption
  val dir = semanticdbDir.value.resolve("src/main/scala")
  def file(name: String): Path = dir.resolve(s"$name.scala.semanticdb")
  Seq("A", "B").foreach: name =>
    assert(Files.isRegularFile(file(name)), s"${file(name)} is missing")
    assert(!Files.isSymbolicLink(file(name)), s"${file(name)} is still a link into the disk cache")
  member.foreach: m =>
    val a = new String(Files.readAllBytes(file("A")), "ISO-8859-1")
    assert(a.contains(m), s"${file("A")} was not regenerated: no $m")
}
