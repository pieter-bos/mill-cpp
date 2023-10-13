import mill._
import scalalib._
import publish._

object main extends RootModule with ScalaModule with PublishModule {
  def scalaVersion = "2.13.12"
  def publishVersion = "0.0.1"
  def artifactName = T { "me.pieterbos.mill.cpp" }

  def pomSettings = PomSettings(
    description = "",
    organization = "me.pieterbos",
    url = "https://github.com/pieter-bos/mill-cpp",
    licenses = Seq(License.`MPL-2.0`),
    versionControl = VersionControl.github("pieter-bos", "mill-cpp"),
    developers = Seq(
      Developer("pieter-bos", "Pieter Bos", "https://pieterbos.me/"),
    ),
  )

  def ivyDeps = Agg(
    ivy"com.lihaoyi::mill-main-api:0.11.0",
    ivy"com.lihaoyi::mill-main:0.11.0",
  )
}