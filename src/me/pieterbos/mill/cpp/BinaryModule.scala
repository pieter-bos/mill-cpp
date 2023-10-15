package me.pieterbos.mill.cpp

import mill._

sealed trait BinaryModule extends Module {
  def moduleDeps: Seq[LinkableModule]
  def systemLibraryDeps: T[Seq[String]]
}

trait ExecutableBinaryModule extends BinaryModule {

}

trait LinkableModule extends BinaryModule {
  def staticObjects: T[Seq[PathRef]]
  def dynamicObjects: T[Seq[PathRef]]
  def headers: T[Seq[PathRef]]
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