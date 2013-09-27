package uk.ac.warwick.tabula.data.model


sealed abstract class DegreeType(val dbValue: String, val description: String)

object DegreeType {
	case object Undergraduate extends DegreeType("UG", "Undergraduate")
	case object Postgraduate extends DegreeType("PG", "Postgraduate")
	case object InService extends DegreeType("IS", "In-Service")
	case object PGCE extends DegreeType("PGCE", "PGCE")

	def fromCode(code: String) = code match {
	  	case Undergraduate.dbValue => Undergraduate
	  	case Postgraduate.dbValue => Postgraduate
	  	case InService.dbValue => InService
	  	case PGCE.dbValue => PGCE
	  	case null => null
	  	case _ => throw new IllegalArgumentException()
	}
}

class DegreeTypeUserType extends AbstractStringUserType[DegreeType] {
	override def convertToObject(string: String) = DegreeType.fromCode(string)
	override def convertToValue(degreeType: DegreeType) = degreeType.dbValue
}

