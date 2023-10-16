package me.pieterbos.mill.cpp.options

import upickle.default.{ReadWriter => RW}

object implicits {
  implicit val optionsRw: RW[CppCompileOptions] = CppCompileOptions.rw
  implicit val archiveOptionsRw: RW[CppArchiveOptions] = CppArchiveOptions.rw
  implicit val linkExecutableOptionsRw: RW[CppExecutableOptions] = CppExecutableOptions.rw

  implicit val optimizationRw: RW[CppOptimization] = CppOptimization.rw
  implicit val standardRw: RW[CppStandard] = CppStandard.rw
}
