package uk.ac.warwick.tabula.data.model.groups

import uk.ac.warwick.tabula.data.model.AbstractBasicUserType
import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

sealed abstract class SmallGroupFormat(val code: String, val description: String) {
	// For Spring, the silly bum
	def getCode = code
	def getDescription = description
	
	override def toString = description
}

object SmallGroupFormat {
	case object Seminar extends SmallGroupFormat("seminar", "Seminar")
	case object Lab extends SmallGroupFormat("lab", "Lab")
	case object Tutorial extends SmallGroupFormat("tutorial", "Tutorial")
	case object Project extends SmallGroupFormat("project", "Project group")
	case object Example extends SmallGroupFormat("example", "Example Class")
	case object Lecture extends SmallGroupFormat("lecture", "Lecture")

	// lame manual collection. Keep in sync with the case objects above
	val members = Seq(Seminar, Lab, Tutorial, Project, Example, Lecture)

	def fromCode(code: String) =
		if (code == null) null
		else members.find{_.code == code} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}

	def fromDescription(description: String) =
		if (description == null) null
		else members.find{_.description == description} match {
			case Some(caseObject) => caseObject
			case None => throw new IllegalArgumentException()
		}
}

class SmallGroupFormatUserType extends AbstractBasicUserType[SmallGroupFormat, String] {

	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String) = SmallGroupFormat.fromCode(string)
	override def convertToValue(format: SmallGroupFormat) = format.code
}