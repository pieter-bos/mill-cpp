package me.pieterbos.mill.cpp.options

import upickle.default.{ReadWriter => RW}

object implicits {
  implicit val optionsRw: RW[CppOptions] = CppOptions.rw

  implicit val optimizationRw: RW[CppOptimization] = CppOptimization.rw
  implicit val standardRw: RW[CppStandard] = CppStandard.rw
}
