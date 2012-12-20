package uk.ac.warwick.tabula.data.model

import scala.reflect.BeanProperty
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

sealed abstract class DegreeType(val dbValue: String, @BeanProperty val description: String)
case object Undergraduate extends DegreeType("UG", "Undergraduate")
case object Postgraduate extends DegreeType("PG", "Postgraduate")
case object InService extends DegreeType("IS", "In-Service")
case object PGCE extends DegreeType("PGCE", "PGCE")

object DegreeType {
	def fromCode(code: String) = code match {
	  	case Undergraduate.dbValue => Undergraduate
	  	case Postgraduate.dbValue => Postgraduate
	  	case InService.dbValue => InService
	  	case PGCE.dbValue => PGCE
	  	case null => null
	  	case _ => throw new IllegalArgumentException()
	}
}

class DegreeTypeUserType extends AbstractBasicUserType[DegreeType, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = DegreeType.fromCode(string)
	
	override def convertToValue(degreeType: DegreeType) = degreeType.dbValue

}