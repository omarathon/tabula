package uk.ac.warwick.tabula.helpers

object SeqUtils {
  implicit class SeqImprovements[T](i: Seq[T]) {
    def mkStringOrEmpty(start: String, sep: String, end: String): String =
      if (i.isEmpty) ""
      else i.mkString(start, sep, end)
  }
}
