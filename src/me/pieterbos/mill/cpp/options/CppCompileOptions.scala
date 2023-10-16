package me.pieterbos.mill.cpp.options

import os.Path
import upickle.default.{macroRW, ReadWriter => RW}
import mill._

object CppCompileOptions {
  val rw: RW[CppCompileOptions] = macroRW
}

case class CppCompileOptions(
  includePaths: Seq[Path] = Nil,
  defines: Seq[(String, String)] = Nil,
  includes: Seq[Path] = Nil,
  standard: CppStandard = CppStandard.Default,
  optimization: CppOptimization = CppOptimization.Default,
  extraOptions: Seq[String] = Nil,
  extraEarlyOptions: Seq[String] = Nil,
)

object CppArchiveOptions {
  val rw: RW[CppArchiveOptions] = macroRW
}

case class CppArchiveOptions()

object CppExecutableOptions {
  val rw: RW[CppExecutableOptions] = macroRW
}

case class CppExecutableOptions(
  localSharedLibraries: Seq[os.Path],
  systemLibraries: Seq[String],
  extraOptions: Seq[String] = Nil,
  extraEarlyOptions: Seq[String] = Nil,
)