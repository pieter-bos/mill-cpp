package me.pieterbos.mill.cpp.toolchain

import me.pieterbos.mill.cpp.options.{CppOptions, CppOutput}
import os.Path

trait CppToolchain {
  def isValid: Boolean

  def compile(source: Path, outDir: Path, options: CppOptions, additionalOptions: Seq[String]): Path

  def link(objects: Seq[Path], outDir: Path, output: CppOutput, options: CppOptions, additionalOptions: Seq[String]): Path
}
