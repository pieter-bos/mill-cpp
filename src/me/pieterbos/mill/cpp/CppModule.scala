package me.pieterbos.mill.cpp

import me.pieterbos.mill.cpp.options.implicits._
import me.pieterbos.mill.cpp.options.{CppArchiveOptions, CppCompileOptions, CppExecutableOptions, CppOptimization, CppStandard}
import me.pieterbos.mill.cpp.toolchain.{CppToolchain, GccCompatible, Msvc}
import mill.util.Util
import mill.{Command, PathRef, T}

trait CppModule extends LinkableModule {
  override def moduleDeps: Seq[LinkableModule] = Nil
  override def systemLibraryDeps: T[Seq[String]] = T { Seq.empty[String] }

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

  override def exportIncludePaths: T[Seq[PathRef]] = T { includePaths() ++ generatedIncludePaths() }

  def depIncludePaths: T[Seq[PathRef]] = T { T.traverse(moduleDeps)(_.exportIncludePaths)().flatten }

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

  def compileOptions: T[Seq[String]] = T { Seq.empty[String] }
  def compileEarlyOptions: T[Seq[String]] = T { Seq.empty[String] }
  def archiveOptions: T[Seq[String]] = T { Seq.empty[String] }
  def archiveEarlyOptions: T[Seq[String]] = T { Seq.empty[String] }

  private def compileOptionsObj: T[CppCompileOptions] = T {
    CppCompileOptions(
      allIncludePaths().map(_.path),
      defines(),
      includes().map(_.path),
      standard(),
      optimization(),
      compileOptions(),
      compileEarlyOptions(),
    )
  }

  private def archiveOptionsObj: T[CppArchiveOptions] = T {
    CppArchiveOptions(

    )
  }

  def compileOnly: T[Seq[PathRef]] = T.persistent {
    for(source <- allSourceFiles()) yield {
      PathRef(toolchain.compile(source.path, T.dest, compileOptionsObj()))
    }
  }

  def name: T[String] = T { millSourcePath.baseName }

  def archive: T[PathRef] = T {
    PathRef(toolchain.archive(compileOnly().map(_.path), T.dest, name(), archiveOptionsObj()))
  }

  override def staticObjects: T[Seq[PathRef]] = T { Seq(archive()) }

  override def dynamicObjects: T[Seq[PathRef]] = T { Seq.empty[PathRef] }
}

trait CppExecutableModule extends CppModule {
  def executableOptions: T[CppExecutableOptions] = T {
    CppExecutableOptions(
      transitiveDynamicObjects().map(_.path),
      transitiveSystemLibraryDeps(),
      Nil,
      Nil,
    )
  }

  def compile: T[PathRef] = T {
    PathRef(toolchain.linkExecutable((compileOnly() ++ transitiveStaticObjects()).map(_.path), T.dest, name(), executableOptions()))
  }

  def run(args: String*): Command[Int] = T.command {
    os.proc(compile().path, args).call().exitCode
  }
}
