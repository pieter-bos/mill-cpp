# `mill-cpp`: C and C++ plugin for mill
C and C++ language support for the [Mill build tool](https://mill-build.com).

Build projects from source:

```scala
object main extends CppModule {
  def standard = T[options.CppStandard] { options.CppStandard.Cpp20 }
  def sources = T.sources(millSourcePath / "src")
  def includePaths = T.sources(millSourcePath / "include")
}

// $ mill main.archive
//  => ar: creating out/main/archive.dest/main.a
```

Link them into executables:

```scala
object main extends CppExecutableModule {
  // ...
  
  def name = T { "test" }
}
// $ mill main.compile
// $ ./out/main/compile.dest/test
//  => Hello, mill!
```

Import local CMake configurations:

```scala
object proto extends CMakeModule {
  def root = T.source(os.Path("/path/to/protobuf"))

  object libprotobuf extends CMakeLibrary {
    def target = T { "libprotobuf" }
  }
}
```

And use their targets as libraries or utilities:

```scala
object proto extends CMakeModule {
  // ...

  object protoc extends CMakeExecutable {
    def target = T { "protoc" }
  }

  def proto = T.source(millSourcePath / "message.proto")

  def generate = T {
    os.proc(protoc.executable().path, "-I=" + proto().path.toString,  "--cpp_out=" + T.dest.toString, proto().path).call()
    T.dest
  }

  object messageProto extends CppModule {
    def standard = T[options.CppStandard] { options.CppStandard.Cpp20 }
    def moduleDeps = Seq(libprotobuf)
    def sources = T.sources(generate())
    def includePaths = T.sources(generate())
  }
}
```

## Quickstart
Import the `mill-cpp` plugin in your `build.sc`:

```scala
import ivy.`me.pieterbos::mill-cpp::0.0.1`

import mill._, me.pieterbos.mill.cpp._
```

A `CppModule` will compile both C and C++ sources. The default values for some `CppModule` tasks you may want to override are:

```scala
object main extends CppExecutableModule {
  def moduleDeps: Seq[LinkableModule] = Nil
  def systemLibraryDeps: T[Seq[String]] = T { Seq.empty[String] }

  def sources: T[Seq[PathRef]] = T.sources { millSourcePath / "src" }
  
  // The exported include paths from *direct* module dependencies is also available during compilation
  def includePaths: T[Seq[PathRef]] = T.sources { millSourcePath / "include" }
  
  // You may want to hide internal headers from the export paths for dependents
  override def exportIncludePaths: T[Seq[PathRef]] = T { includePaths() ++ generatedIncludePaths() }
  
  def standard: T[CppStandard] = T[CppStandard] { CppStandard.Default } // e.g. -std=c++20 
  def optimization: T[CppOptimization] = T[CppOptimization] { CppOptimization.Default } // e.g. -O2
  def defines: T[Seq[(String, String)]] = T { Seq[(String, String)]() } // e.g. -DWITH_SPECIAL_FEATURE=1
  def includes: T[Seq[PathRef]] = T { Seq.empty[PathRef] } // explicitly pre-process an additional file

  def compileOptions: T[Seq[String]] = T { Seq.empty[String] } // additional compile options

  def name: T[String] = T { millSourcePath.baseName }
}
```