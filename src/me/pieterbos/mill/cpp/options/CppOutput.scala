package me.pieterbos.mill.cpp.options

import upickle.default.{macroRW, ReadWriter => RW}

sealed trait CppOutput

object CppOutput {
  implicit val rw: RW[CppOutput] = RW.merge(
    macroRW[GenericObject],
    macroRW[SharedLibrary],
    macroRW[StaticLibrary],
    macroRW[Executable],
  )

  case class GenericObject(name: String) extends CppOutput
  case class SharedLibrary(name: String) extends CppOutput
  case class StaticLibrary(name: String) extends CppOutput
  case class Executable(name: String) extends CppOutput
}
