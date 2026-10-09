/*
 * sbt
 * Copyright 2023, Scala center
 * Copyright 2011 - 2022, Lightbend, Inc.
 * Copyright 2008 - 2010, Mark Harrah
 * Licensed under Apache License 2.0 (see LICENSE)
 */

package sbt
package plugins

import hedgehog.*
import hedgehog.runner.*
import java.io.File

/** Tests for [[SemanticdbPlugin.targetRoots]], the inverse of [[SemanticdbPlugin.targetRootOptions]]. */
object SemanticdbPluginTargetRootsTest extends Properties:
  private val root = File("/out/meta")

  override def tests: List[Test] = List(
    example("Scala 2 options round-trip", roundTrip("2.13.18")),
    example("Scala 3 options round-trip", roundTrip("3.8.4")),
    example(
      "the joined -semanticdb-target form",
      SemanticdbPlugin.targetRoots(Seq("-semanticdb-target:/out/meta")) ==== Seq("/out/meta")
    ),
    example(
      "-semanticdb-target without a value names no target root",
      SemanticdbPlugin.targetRoots(Seq("-semanticdb-target")) ==== Seq.empty
    ),
    example(
      "other options name no target root",
      SemanticdbPlugin.targetRoots(Seq("-deprecation", "-Xsemanticdb")) ==== Seq.empty
    ),
  )

  private def roundTrip(scalaVersion: String): Result =
    val options = "-deprecation" +: SemanticdbPlugin.targetRootOptions(scalaVersion, root)
    SemanticdbPlugin.targetRoots(options) ==== Seq(root.toString)
end SemanticdbPluginTargetRootsTest
