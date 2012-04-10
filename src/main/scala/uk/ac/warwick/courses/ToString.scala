package uk.ac.warwick.courses

trait ToString {
	def toStringProps:Seq[Pair[String,Any]]
	override def toString() = {
		getClass.getSimpleName + toStringProps.map{case(k,v) => k+"="+v}.mkString("[", ",", "]")
	} 
}