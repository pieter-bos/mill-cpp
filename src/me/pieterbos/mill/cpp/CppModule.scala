package me.pieterbos.mill.cpp

import me.pieterbos.mill.cpp.options.implicits._
import me.pieterbos.mill.cpp.options.{CppOptimization, CppOptions, CppStandard}
import me.pieterbos.mill.cpp.toolchain.{CppToolchain, GccCompatible, Msvc}
import mill.util.Util
import mill.{Command, Module, PathRef, T}

import scala.collection.mutable.ArrayBuffer

trait CppModule extends Module {
  def moduleDeps: Seq[CppModule] = Nil

  private def collectModuleDeps(acc: ArrayBuffer[CppModule]): Unit =
    if(!acc.contains(this)) {
      acc += this
      moduleDeps.foreach(_.collectModuleDeps(acc))
    }

  def transitiveModuleDeps: Seq[CppModule] = {
    val acc = ArrayBuffer[CppModule]()
    moduleDeps.foreach(_.collectModuleDeps(acc))
    acc.toSeq.distinct
  }

  def preferredWindowsToolchains: Seq[CppToolchain] = Seq(
    Msvc("cl", "lib"),
    GccCompatible("cxx", "ar"),
    GccCompatible("g++", "ar"),
    GccCompatible("clang++", "ar"),
  )

  def preferredUnixToolchains: Seq[CppToolchain] = Seq(
    GccCompatible("cxx", "ar"),
    GccCompatible("g++", "ar"),
    GccCompatible("clang++", "ar"),
  )

  def windowsToolchain: CppToolchain =
    preferredWindowsToolchains.find(_.isValid).get

  def unixToolchain: CppToolchain =
    preferredUnixToolchains.find(_.isValid).get

  lazy val toolchain: CppToolchain =
    if (Util.windowsPlatform) windowsToolchain
    else unixToolchain

  def sources: T[Seq[PathRef]] = T.sources { millSourcePath / "src" }
  def includePaths: T[Seq[PathRef]] = T.sources { millSourcePath / "include" }

  def generatedSources: T[Seq[PathRef]] = T { Seq.empty[PathRef] }
  def generatedIncludePaths: T[Seq[PathRef]] = T { Seq.empty[PathRef] }

  def exportIncludePaths: T[Seq[PathRef]] = T { includePaths() ++ generatedIncludePaths() }

  def depIncludePaths: T[Seq[PathRef]] = T { T.traverse(transitiveModuleDeps)(_.exportIncludePaths)().flatten }

  def allSources: T[Seq[PathRef]] = T { sources() ++ generatedSources() }
  def allIncludePaths: T[Seq[PathRef]] = T { includePaths() ++ generatedIncludePaths() ++ depIncludePaths() }

  def allSourceFiles: T[Seq[PathRef]] = T {
    def isHiddenFile(path: os.Path): Boolean = path.last.startsWith(".")

    for {
      root <- allSources()
      if os.exists(root.path)
      path <- if(os.isDir(root.path)) os.walk(root.path) else Seq(root.path)
      if os.isFile(path)
      if !isHiddenFile(path)
      if Seq("c", "cc", "cxx", "cpp", "c++", "cppm").contains(path.ext.toLowerCase)
    } yield PathRef(path)
  }

  def standard: T[CppStandard] = T[CppStandard] { CppStandard.Default }

  def optimization: T[CppOptimization] = T[CppOptimization] { CppOptimization.Default }

  def defines: T[Seq[(String, String)]] = T { Seq[(String, String)]() }

  def includes: T[Seq[PathRef]] = T { Seq.empty[PathRef] }

  private def options: T[CppOptions] = T {
    CppOptions(
      allIncludePaths().map(_.path),
      defines(),
      includes().map(_.path),
      standard(),
      optimization()
    )
  }

  def additionalOptions: T[Seq[String]] = T { Seq.empty[String] }

  def compileOnly: T[Seq[PathRef]] = T.persistent {
    for(source <- allSourceFiles()) yield {
      PathRef(toolchain.compile(source.path, T.dest, options(), additionalOptions()))
    }
  }

  def depLinkStatic: T[Seq[PathRef]]= T { T.traverse(transitiveModuleDeps)(_.linkStatic)() }

  def name: T[String] = T { millSourcePath.baseName }

  def linkStatic: T[PathRef] = T {
    PathRef(toolchain.linkStatic(compileOnly().map(_.path), T.dest, name(), options(), additionalOptions()))
  }

  def compile: T[PathRef] = T {
    PathRef(toolchain.linkExecutable((compileOnly() ++ depLinkStatic()).map(_.path), T.dest, name(), options(), additionalOptions()))
  }

  def run(args: String*): Command[Int] = T.command {
    os.proc(compile().path, args).call().exitCode
  }
}
