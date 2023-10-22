import mill._
import mill.api.{Logger, Result}
import scalalib._
import publish._

import scala.reflect.runtime.{universe => runtimeUniverse}

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
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("pieter-bos", "mill-cpp"),
    developers = Seq(
      Developer("pieter-bos", "Pieter Bos", "https://pieterbos.me/"),
    ),
  )

  def ivyDeps = Agg(
    ivy"com.lihaoyi::mill-main-api:${millVersion()}",
    ivy"com.lihaoyi::mill-main:${millVersion()}",
  )

  def requireGitVersionTag: T[Unit] = T {
    val noUncommittedChanges = os.proc("git", "diff-index", "HEAD").call(cwd = millSourcePath).out.text().isBlank
    val currentTags = os.proc("git", "tag", "--points-at", "HEAD").call(cwd = millSourcePath).out.text().strip().split('\n').toSeq
    val expectedTag = s"v${publishVersion()}"

    if(!noUncommittedChanges)
      Result.Failure("There are uncommitted changes.")
    else if(currentTags != Seq(expectedTag))
      Result.Failure(s"The current commit has tags: ${currentTags.mkString("[", ",", "]")}, but is expected to only have tag $expectedTag")
    else
      Result.Success(())
  }

  case class SonatypeHelper(workspace: os.Path, env: Map[String, String], logger: Logger)
      extends SonatypePublisher("", "", "", false, Nil, 0, 0, logger, workspace, env, 0, false)
  {
    private val mirror = runtimeUniverse.runtimeMirror(getClass.getClassLoader)
    private val instanceMirror = mirror.reflect[SonatypePublisher](this)

    private def call[R](name: String, args: Any*): R =
      instanceMirror.reflectMethod(
        mirror.typeOf[SonatypePublisher]
          .decl(runtimeUniverse.TermName(name))
          .asMethod
      ).apply(args: _*).asInstanceOf[R]

    def md5hex(bytes: Array[Byte]): Array[Byte] = call("md5hex", bytes)
    def sha1hex(bytes: Array[Byte]): Array[Byte] = call("sha1hex", bytes)
    def gpgSigned(file: os.Path, args: Seq[String]): os.Path = call("gpgSigned", file, args)
  }

  def publishSonatypeCentralZip: T[PathRef] = T {
    val pom = pomSettings()
    val componentDir = T.dest / "component"
    val namespaceDir = pom.organization.split('.').foldLeft(componentDir)(_ / _)
    val dest = namespaceDir / artifactId() / publishVersion()
    os.makeDir.all(dest)

    val helper = SonatypeHelper(dest, T.env, T.log)

    val artifacts = publishArtifacts().payload

    for((data, name) <- artifacts) {
      val content = os.read.bytes(data.path)
      os.write(dest / name, content)
      helper.gpgSigned(dest / name, PublishModule.defaultGpgArgs)
      os.write(dest / (name + ".md5"), helper.md5hex(content))
      os.write(dest / (name + ".sha1"), helper.sha1hex(content))
    }

    val toArchive =
      os.list(componentDir)
        .map(_.relativeTo(componentDir))

    val out = T.dest / "component.zip"

    os.proc("tar", "--create", "--file", out, "--directory=" + componentDir.toString(), toArchive).call(cwd = T.dest)

    PathRef(out)
  }

  def publishSonatypeCentral(): Command[Unit] = T.command {
    requireGitVersionTag()
    T.log.info(s"Deployment name: ${artifactId()}")
    T.log.info(s"Upload file: ${publishSonatypeCentralZip().path}")
    ()
  }
}