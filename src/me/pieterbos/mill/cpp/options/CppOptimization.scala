package me.pieterbos.mill.cpp.options

import upickle.default.{macroRW, ReadWriter => RW}

sealed trait CppOptimization

object CppOptimization {
  implicit val rw: RW[CppOptimization] = RW.merge(
    macroRW[Default.type],
    macroRW[None.type],
    macroRW[Small.type],
    macroRW[Smallest.type],
    macroRW[Fast.type],
    macroRW[Fastest.type],
  )

  object Default extends CppOptimization
  object None extends CppOptimization
  object Small extends CppOptimization
  object Smallest extends CppOptimization
  object Fast extends CppOptimization
  object Fastest extends CppOptimization
}