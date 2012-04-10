package uk.ac.warwick.courses

trait ToString {
	def toStringMap:Map[String,Any]
	override def toString() = {
		getClass.getSimpleName + toStringMap.map{case(k,v) => k+"="+v}.mkString("[", ",", "]")
	} 
}