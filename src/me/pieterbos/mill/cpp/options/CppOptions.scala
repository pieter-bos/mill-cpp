package me.pieterbos.mill.cpp.options

import os.Path
import upickle.default.{macroRW, ReadWriter => RW}
import mill._

object CppOptions {
  val rw: RW[CppOptions] = macroRW
}

case class CppOptions(
  includePaths: Seq[Path] = Nil,
  defines: Seq[(String, String)] = Nil,
  includes: Seq[Path] = Nil,
  standard: CppStandard = CppStandard.Default,
  optimization: CppOptimization = CppOptimization.Default,
)
