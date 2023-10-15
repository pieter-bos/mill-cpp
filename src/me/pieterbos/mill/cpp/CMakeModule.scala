package me.pieterbos.mill.cpp

import mill._
import cmake.implicits._
import mill.api.Ctx

trait CMakeModule extends LinkableModule {
  final override def moduleDeps: Seq[LinkableModule] = Nil

  def root: T[PathRef]
  def targets: T[Seq[String]]

  def cMakeSetupBuild: T[os.Path] = T {
    val apiDir = T.dest / ".cmake" / "api" / "v1"
    os.makeDir.all(apiDir / "query")
    os.write(apiDir / "query" / "codemodel-v2", "")
    os.proc("cmake", "-B", T.dest, "-S", root().path).call(cwd = T.dest)
    T.dest
  }

  def queryCMake: T[PathRef] = T {
    PathRef(cMakeSetupBuild() / ".cmake" / "api" / "v1" / "reply")
  }

  private def jobs: T[Int] = T {
    T.ctx() match {
      case ctx: Ctx.Jobs => ctx.jobs
      case _ => 1
    }
  }

  def cMakeBuild: T[PathRef] = T {
    val build = cMakeSetupBuild()
    os.proc("make", "-j", jobs(), "all").call(cwd = build)
    PathRef(build)
  }

  def cMakeIndex: T[cmake.Index] = T {
    val out = os.list(queryCMake().path).find(_.baseName.startsWith("index-")).get
    upickle.default.read[cmake.Index](out.toIO, trace = true)
  }

  def cMakeCodeModel: T[cmake.CodeModel] = T {
    val fileName = cMakeIndex().objects.find(_.kind == "codemodel").get.jsonFile
    val path = queryCMake().path / fileName
    upickle.default.read[cmake.CodeModel](path.toIO, trace = true)
  }

  def allCMakeTargets: T[Seq[cmake.Target]] = T {
    val pathStrings = cMakeCodeModel().configurations.flatMap(_.targets.map(_.jsonFile))
    val paths = pathStrings.map(p => queryCMake().path / p)
    paths.map(p => upickle.default.read[cmake.Target](p.toIO, trace = true))
  }

  def cMakeTargets: T[Seq[cmake.Target]] = T {
    val targets = this.targets()
    allCMakeTargets().filter(t => targets.contains(t.name))
  }

  def cMakeTargetsAndDeps: T[Seq[cmake.Target]] = T {
    val targets = allCMakeTargets()
    cMakeTargets().flatMap(t => t +: t.dependencies.map(dep => targets.find(_.id == dep.id).get)).distinctBy(_.id)
  }

  override def systemLibraryDeps: T[Seq[String]] = T { Seq.empty[String] }

  override def staticObjects: T[Seq[PathRef]] = T {
    cMakeTargetsAndDeps()
      .filter(t => Seq("STATIC_LIBRARY", "OBJECT_LIBRARY").contains(t.`type`))
      .flatMap(_.artifacts)
      .map(p => PathRef(cMakeBuild().path / os.RelPath(p.path)))
  }

  override def dynamicObjects: T[Seq[PathRef]] = T {
    cMakeTargetsAndDeps()
      .filter(t => Seq("SHARED_LIBRARY").contains(t.`type`))
      .flatMap(_.artifacts)
      .map(p => PathRef(cMakeBuild().path / os.RelPath(p.path)))
  }

  override def headers: T[Seq[PathRef]] = T {
    /* TODO: this is most likely the include paths available to the compilation of the targets, and not in any sense
             the "export" of the target. There's a good chance the exports are included there. */
    cMakeTargets()
      .flatMap(_.compileGroups)
      .flatMap(_.includes)
      .map(p => PathRef(os.Path(p.path)))
  }
}
