package me.pieterbos.mill.cpp.cmake

import upickle.default._

object implicits {
  implicit val rwPaths: ReadWriter[Paths] = macroRW
  implicit val rwPathObj: ReadWriter[PathObj] = macroRW
  implicit val rwIdObj: ReadWriter[IdObj] = macroRW

  implicit val rwIndexObject: ReadWriter[IndexObject] = macroRW
  implicit val rwIndex: ReadWriter[Index] = macroRW

  implicit val rwTargetHeader: ReadWriter[TargetHeader] = macroRW
  implicit val rwDirectory: ReadWriter[DirectoryHeader] = macroRW
  implicit val rwProject: ReadWriter[ProjectHeader] = macroRW
  implicit val rwConfiguration: ReadWriter[Configuration] = macroRW
  implicit val rwCodeModel: ReadWriter[CodeModel] = macroRW

  implicit val rwCompileGroup: ReadWriter[CompileGroup] = macroRW
  implicit val rwTarget: ReadWriter[Target] = macroRW
}

case class Paths(source: String, build: String)
case class PathObj(path: String)
case class IdObj(id: String)

case class Index(objects: Seq[IndexObject])
case class IndexObject(kind: String, jsonFile: String)

case class CodeModel(configurations: Seq[Configuration])
case class Configuration(name: String, projects: Seq[ProjectHeader], directories: Seq[DirectoryHeader], targets: Seq[TargetHeader])
case class ProjectHeader(parentIndex: Int = -1, childIndexes: Seq[Int] = Nil, directoryIndexes: Seq[Int], targetIndexes: Seq[Int] = Nil)
case class DirectoryHeader(source: String, build: String, parentIndex: Int = -1, childIndexes: Seq[Int] = Nil, projectIndex: Int, targetIndexes: Seq[Int] = Nil, jsonFile: String)
case class TargetHeader(name: String, directoryIndex: Int, projectIndex: Int, jsonFile: String)

case class Target(id: String, name: String, `type`: String, paths: Paths, artifacts: Seq[PathObj] = Nil, dependencies: Seq[IdObj] = Nil, compileGroups: Seq[CompileGroup] = Nil)
case class CompileGroup(includes: Seq[PathObj])