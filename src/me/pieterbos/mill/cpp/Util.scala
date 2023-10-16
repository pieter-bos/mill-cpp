package me.pieterbos.mill.cpp

object Util {
  implicit class SeqHelpers[T](xs: Seq[T]) {
    def distinctKeepFirst: Seq[T] = {
      var result = Seq.empty[T]
      for(x <- xs) {
        if(!result.contains(x)) result :+= x
      }
      result
    }

    def distinctKeepLast: Seq[T] = xs.reverse.distinctKeepFirst.reverse
  }
}
