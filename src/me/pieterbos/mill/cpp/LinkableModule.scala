package me.pieterbos.mill.cpp

import mill._

import scala.collection.mutable.ArrayBuffer

import Util._

trait LinkableModule extends Module {
  def moduleDeps: Seq[LinkableModule]
  def systemLibraryDeps: T[Seq[String]]

  private def collectModuleDeps(acc: ArrayBuffer[LinkableModule]): Unit =
    if (!acc.contains(this)) {
      acc += this
      moduleDeps.foreach(_.collectModuleDeps(acc))
    }

  def transitiveModuleDeps: Seq[LinkableModule] = {
    val acc = ArrayBuffer[LinkableModule]()
    moduleDeps.foreach(_.collectModuleDeps(acc))
    acc.toSeq.distinctKeepLast
  }

  def staticObjects: T[Seq[PathRef]]
  def dynamicObjects: T[Seq[PathRef]]
  def exportIncludePaths: T[Seq[PathRef]]

  def transitiveStaticObjects: T[Seq[PathRef]] = T {
    (staticObjects() ++ T.traverse(transitiveModuleDeps)(_.staticObjects)().flatten).distinctKeepLast
  }

  def transitiveDynamicObjects: T[Seq[PathRef]] = T {
    T.traverse(transitiveModuleDeps)(_.dynamicObjects)().flatten.distinctKeepLast
  }

  def transitiveSystemLibraryDeps: T[Seq[String]] = T {
    (systemLibraryDeps() ++ T.traverse(transitiveModuleDeps)(_.systemLibraryDeps)().flatten).distinctKeepLast
  }
}

trait SharedLibraryModule extends LinkableModule {
  def name: T[String]
  def linkObject: T[PathRef]

  override def staticObjects: T[Seq[PathRef]] = T { Seq.empty[PathRef] }
  override def dynamicObjects: T[Seq[PathRef]] = T { Seq(linkObject()) }
}

trait StaticLibraryModule extends LinkableModule {
  def objects: T[Seq[PathRef]]

  override def staticObjects: T[Seq[PathRef]] = T { objects() }
  override def dynamicObjects: T[Seq[PathRef]] = T { Seq.empty[PathRef] }
}