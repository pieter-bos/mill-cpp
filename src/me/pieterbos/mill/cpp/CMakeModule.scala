package me.pieterbos.mill.cpp

import mill._
import cmake.implicits._
import mill.api.Ctx
import mill.api.Result

import scala.collection.mutable.ArrayBuffer

trait CMakeModule extends Module {
  def root: T[PathRef]

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

  def jobs: T[Int] = T {
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

  trait CMakeLibrary extends LinkableModule {
    final override def moduleDeps: Seq[LinkableModule] = Nil
    def target: T[String]

    def cMakeTarget: T[cmake.Target] = T {
      allCMakeTargets().find(_.name == target()).get
    }

    def cMakeTargetAndDepsUnordered: T[Seq[cmake.Target]] = T {
      val target = cMakeTarget()
      val targets = allCMakeTargets()
      target +: target.dependencies.map(dep => targets.find(_.id == dep.id).get)
    }

    def cMakeTargetAndDeps: T[Seq[cmake.Target]] = T {
      /* The dependencies of a target are already the transitive closure of its dependencies, but they are not ordered
       * in a defined way. The linker needs that static libraries do not occur earlier than other objects that need it,
       * so we re-derive a proper order.
       */
      val pile = ArrayBuffer(cMakeTargetAndDepsUnordered(): _*)

      def inner(): Result[Seq[cmake.Target]] = {
        var result = Seq.empty[cmake.Target]

        while(pile.nonEmpty) {
          // find a dependency s.t.
          pile.find { candidate =>
            // there is no target that depends on it
            !pile.exists(_.dependencies.exists(_.id == candidate.id))
          } match {
            case None => return Result.Failure("CMake configuration has cyclic dependencies")
            case Some(target) =>
              pile -= target
              result :+= target
          }
        }

        Result.Success(result)
      }

      inner()
    }

    override def systemLibraryDeps: T[Seq[String]] = T { Seq.empty[String] }

    override def staticObjects: T[Seq[PathRef]] = T {
      cMakeTargetAndDeps()
        .filter(t => Seq("STATIC_LIBRARY", "OBJECT_LIBRARY").contains(t.`type`))
        .flatMap(_.artifacts)
        .map(p => PathRef(cMakeBuild().path / os.RelPath(p.path)))
    }

    override def dynamicObjects: T[Seq[PathRef]] = T {
      cMakeTargetAndDeps()
        .filter(t => Seq("SHARED_LIBRARY").contains(t.`type`))
        .flatMap(_.artifacts)
        .map(p => PathRef(cMakeBuild().path / os.RelPath(p.path)))
    }

    override def exportIncludePaths: T[Seq[PathRef]] = T {
      /* TODO: this is most likely the include paths available to the compilation of the targets, and not in any sense
               the "export" of the target. There's a good chance the exports are included there. */
      cMakeTarget()
        .compileGroups
        .flatMap(_.includes)
        .map(p => PathRef(os.Path(p.path)))
    }
  }

  trait CMakeExecutable extends Module {
    def target: T[String]

    def cMakeTarget: T[cmake.Target] = T {
      allCMakeTargets().find(_.name == target()).get
    }

    def executable: T[PathRef] = T {
      PathRef(cMakeBuild().path / os.RelPath(cMakeTarget().artifacts.head.path))
    }

    def run(args: String*): Command[Int] = T.command {
      os.proc(executable().path, args).call(check = false).exitCode
    }
  }
}
