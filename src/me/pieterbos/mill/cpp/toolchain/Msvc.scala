package me.pieterbos.mill.cpp.toolchain

import me.pieterbos.mill.cpp.options.{CppArchiveOptions, CppCompileOptions, CppExecutableOptions, CppOptimization, CppStandard}
import os.{Path, Shellable}

case class Msvc(clPath: Shellable, libPath: Shellable) extends CppToolchain {
  override def isValid: Boolean =
    check(clPath) &&
      check(libPath)

  override def compile(source: Path, outDir: Path, options: CppCompileOptions): Path = {
    val CppCompileOptions(includePaths, defines, includes, standard, optimization, extraOptions, extraEarlyOptions) = options

    val cmdOptions =
      extraOptions ++
      includePaths.map(p => s"/I${p.toString()}") ++
        defines.map { case (m, value) => s"/D$m=$value" } ++
        includes.map(p => s"/FI${p.toString()}") ++
        (standard match {
          case CppStandard.Default => Nil
          case CppStandard.Cpp14 => Seq("/std:c++14")
          case CppStandard.Cpp17 => Seq("/std:c++17")
          case CppStandard.Cpp20 => Seq("/std:c++20")
          case CppStandard.C11 => Seq("/std:c11")
          case CppStandard.C17 => Seq("/std:c17")
        }) ++
        (optimization match {
          case CppOptimization.Default => Nil
          case CppOptimization.None => Seq("/Od")
          case CppOptimization.Small => Seq("/O1")
          case CppOptimization.Smallest => Seq("/O1")
          case CppOptimization.Fast => Seq("/O2")
          case CppOptimization.Fastest => Seq("/O2")
        }) ++
        extraEarlyOptions

    val out = outDir / (source.baseName + ".obj")
    os.proc(clPath, "/Fo", out, cmdOptions, source).call()
    out
  }

  override def archive(objects: Seq[Path], outDir: Path, name: String, options: CppArchiveOptions): Path = {
    val out = outDir / (name + ".lib")
    os.proc(libPath, "/OUT", out, objects).call()
    out
  }

  override def linkExecutable(objects: Seq[Path], outDir: Path, name: String, options: CppExecutableOptions): Path = {
    val out = outDir / (name + ".exe")
    os.proc(clPath, "/Fe", out, objects).call()
    out
  }
}
