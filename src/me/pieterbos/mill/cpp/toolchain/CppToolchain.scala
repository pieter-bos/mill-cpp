package me.pieterbos.mill.cpp.toolchain

import me.pieterbos.mill.cpp.options.CppOptions
import os.{Path, Shellable}

import java.io.IOException

trait CppToolchain {
  def isValid: Boolean

  protected def check(cmd: Shellable*): Boolean =
    try {
      os.proc(cmd: _*).call(check = false).exitCode == 0
    } catch {
      case _: IOException => false
    }

  def compile(source: Path, outDir: Path, options: CppOptions, additionalOptions: Seq[String]): Path

  def linkStatic(objects: Seq[Path], outDir: Path, name: String, options: CppOptions, additionalOptions: Seq[String]): Path

  def linkExecutable(objects: Seq[Path], outDir: Path, name: String, options: CppOptions, additionalOptions: Seq[String]): Path
}
