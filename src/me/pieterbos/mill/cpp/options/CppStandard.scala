package me.pieterbos.mill.cpp.options

import upickle.default.{macroRW, ReadWriter => RW}

sealed trait CppStandard

object CppStandard {
  implicit val rw: RW[CppStandard] = RW.merge(
    macroRW[Default.type],
    macroRW[Cpp14.type],
    macroRW[Cpp17.type],
    macroRW[Cpp20.type],
    macroRW[C11.type],
    macroRW[C17.type],
  )

  case object Default extends CppStandard
  case object Cpp14 extends CppStandard
  case object Cpp17 extends CppStandard
  case object Cpp20 extends CppStandard
  case object C11 extends CppStandard
  case object C17 extends CppStandard
}
