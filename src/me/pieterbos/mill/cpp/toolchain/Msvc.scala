package me.pieterbos.mill.cpp.toolchain

import me.pieterbos.mill.cpp.options.{CppOptimization, CppOptions, CppOutput, CppStandard}
import os.{Path, Shellable}

case class Msvc(path: Shellable) extends CppToolchain {
  override def isValid: Boolean =
    os.proc(path).call().exitCode == 0

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
    os.proc(path, "/Fo", out, cmdOptions(options), additionalOptions, source).call().exitCode
    out
  }

  override def link(objects: Seq[Path], outDir: Path, output: CppOutput, options: CppOptions, additionalOptions: Seq[String]): Path = {
    val (outputFlags, name) = output match {
      case CppOutput.GenericObject(name) => Seq("/Fo") -> (name + ".obj")
      case CppOutput.SharedLibrary(name) => Seq("/Ld", "/Fe") -> (name + ".dll")
      case CppOutput.StaticLibrary(name) => ???
      case CppOutput.Executable(name) => Seq("/Fe") -> (name + ".exe")
    }

    val out = outDir / name
    os.proc(path, outputFlags, out, cmdOptions(options), additionalOptions, objects).call().exitCode
    out
  }
}
