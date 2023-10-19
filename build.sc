import mill._
import scalalib._
import publish._

object main extends RootModule with ScalaModule with PublishModule {
  def scalaMajor = T { "2.13" }
  def scalaMinor = T { "12" }
  def scalaVersion = T { s"${scalaMajor()}.${scalaMinor()}" }

  def millMajor = T { "0.11" }
  def millMinor = T { "0" }
  def millVersion = T { s"${millMajor()}.${millMinor()}" }

  def publishVersion = T { "0.0.1" }
  def artifactName = T { "mill-cpp" }
  def artifactSuffix = T { s"_mill${millMajor()}_${scalaMajor()}" }

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
    ivy"com.lihaoyi::mill-main-api:${millVersion()}",
    ivy"com.lihaoyi::mill-main:${millVersion()}",
  )
}