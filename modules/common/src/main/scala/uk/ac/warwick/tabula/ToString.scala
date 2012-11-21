package uk.ac.warwick.tabula

/**
 * Trait which defines a basic `toString` method for you.
 * All you have to do is return a list of interesting properties
 * from `toStringProps`, and it will stringify that along with
 * the class name.
 */
trait ToString {
	def toStringProps: Seq[Pair[String, Any]]
	override def toString() = {
		getClass.getSimpleName + toStringProps.map { case (k, v) => k + "=" + v }.mkString("[", ",", "]")
	}
}