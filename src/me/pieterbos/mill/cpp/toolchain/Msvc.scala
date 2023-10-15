package me.pieterbos.mill.cpp.toolchain

import me.pieterbos.mill.cpp.options.{CppOptimization, CppOptions, CppStandard}
import os.{Path, Shellable}

case class Msvc(clPath: Shellable, libPath: Shellable) extends CppToolchain {
  override def isValid: Boolean =
    check(clPath) &&
      check(libPath)

  private def cmdOptions(options: CppOptions): Seq[String] = {
    val CppOptions(includePaths, defines, includes, standard, optimization) = options

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
      })
  }

  override def compile(source: Path, outDir: Path, options: CppOptions, additionalOptions: Seq[String]): Path = {
    val out = outDir / (source.baseName + ".obj")
    os.proc(clPath, "/Fo", out, cmdOptions(options), additionalOptions, source).call()
    out
  }

  override def linkStatic(objects: Seq[Path], outDir: Path, name: String, options: CppOptions, additionalOptions: Seq[String]): Path = {
    val out = outDir / (name + ".lib")
    os.proc(libPath, "/OUT", out, objects).call()
    out
  }

  override def linkExecutable(objects: Seq[Path], outDir: Path, name: String, options: CppOptions, additionalOptions: Seq[String]): Path = {
    val out = outDir / (name + ".exe")
    os.proc(clPath, "/Fe", out, cmdOptions(options), additionalOptions, objects).call()
    out
  }
}
