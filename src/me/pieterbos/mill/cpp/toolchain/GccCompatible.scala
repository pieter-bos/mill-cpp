package me.pieterbos.mill.cpp.toolchain

import me.pieterbos.mill.cpp.options.{CppOptimization, CppOptions, CppStandard}
import os.{Path, Shellable}

case class GccCompatible(compilerPath: Shellable, arPath: Shellable) extends CppToolchain {
  override def isValid: Boolean =
    check(compilerPath, "--version") &&
      check(arPath, "--version")

  private def cmdOptions(options: CppOptions): Seq[String] = {
    val CppOptions(includePaths, defines, includes, standard, optimization) = options

    includePaths.map(p => s"-I${p.toString()}") ++
      defines.map { case (m, value) => s"-D$m=$value" } ++
      includes.flatMap(p => Seq("-include", p.toString())) ++
      (standard match {
        case CppStandard.Default => Nil
        case CppStandard.Cpp14 => Seq("-std=c++14")
        case CppStandard.Cpp17 => Seq("-std=c++17")
        case CppStandard.Cpp20 => Seq("-std=c++20")
        case CppStandard.C11 => Seq("-std=c11")
        case CppStandard.C17 => Seq("-std=c17")
      }) ++
      (optimization match {
        case CppOptimization.Default => Nil
        case CppOptimization.None => Seq("-O0")
        case CppOptimization.Small => Seq("-Os")
        case CppOptimization.Smallest => Seq("-Oz")
        case CppOptimization.Fast => Seq("-O2")
        case CppOptimization.Fastest => Seq("-O3")
      })
  }

  override def compile(source: Path, outDir: Path, options: CppOptions, additionalOptions: Seq[String]): Path = {
    val out = outDir / (source.baseName + ".o")
    os.proc(compilerPath, "-o", out, "-c", cmdOptions(options), additionalOptions, source).call()
    out
  }

  override def linkStatic(objects: Seq[Path], outDir: Path, name: String, options: CppOptions, additionalOptions: Seq[String]): Path = {
    val out = outDir / (name + ".a")
    os.proc(arPath, "rs", out, objects).call()
    out
  }

  override def linkExecutable(objects: Seq[Path], outDir: Path, name: String, options: CppOptions, additionalOptions: Seq[String]): Path = {
    val out = outDir / name
    os.proc(compilerPath, "-o", out, cmdOptions(options), objects, additionalOptions).call()
    out
  }
}
