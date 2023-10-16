package me.pieterbos.mill.cpp.toolchain

import me.pieterbos.mill.cpp.options.{CppArchiveOptions, CppCompileOptions, CppExecutableOptions}
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

  def compile(source: Path, outDir: Path, options: CppCompileOptions): Path

  def archive(objects: Seq[Path], outDir: Path, name: String, options: CppArchiveOptions): Path

  def linkExecutable(objects: Seq[Path], outDir: Path, name: String, options: CppExecutableOptions): Path
}
