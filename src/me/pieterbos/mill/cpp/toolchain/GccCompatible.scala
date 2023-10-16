package me.pieterbos.mill.cpp.toolchain

import me.pieterbos.mill.cpp.options.{CppArchiveOptions, CppCompileOptions, CppExecutableOptions, CppOptimization, CppStandard}
import os.{Path, Shellable}

case class GccCompatible(compilerPath: Shellable, arPath: Shellable) extends CppToolchain {
  override def isValid: Boolean =
    check(compilerPath, "--version") &&
      check(arPath, "--version")

  override def compile(source: Path, outDir: Path, options: CppCompileOptions): Path = {
    val CppCompileOptions(includePaths, defines, includes, standard, optimization, extraOptions, extraEarlyOptions) = options

    val cmdOptions =
      extraEarlyOptions ++
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
        }) ++
        extraOptions

    val out = outDir / (source.baseName + ".o")
    os.proc(compilerPath, "-o", out, "-c", cmdOptions, source).call()
    out
  }

  override def archive(objects: Seq[Path], outDir: Path, name: String, options: CppArchiveOptions): Path = {
    val out = outDir / (name + ".a")
    os.proc(arPath, "rs", out, objects).call()
    out
  }

  override def linkExecutable(objects: Seq[Path], outDir: Path, name: String, options: CppExecutableOptions): Path = {
    val CppExecutableOptions(localSharedLibraries, systemLibraries, extraOptions, extraEarlyOptions) = options

    for(lib <- localSharedLibraries) {
      os.symlink(outDir / lib.baseName, lib)
    }

    val cmdOptions =
      extraEarlyOptions ++
        Seq("-L" + outDir.toString()) ++
        localSharedLibraries.map(p => "-l" + p.baseName) ++
        systemLibraries.map(l => "-l" + l) ++
        extraOptions

    val out = outDir / name
    os.proc(compilerPath, "-o", out, objects, cmdOptions).call()
    out
  }
}
