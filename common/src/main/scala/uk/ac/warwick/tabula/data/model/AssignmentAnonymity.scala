package uk.ac.warwick.tabula.data.model

import java.sql.Types

import org.hibernate.`type`.StandardBasicTypes
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.tabula.helpers.StringUtils._


sealed abstract class AssignmentAnonymity(val code: String, val description: String) {

	// For Spring
	def getCode: String = code
	def getDescription: String = description

	override def hashCode: Int = getClass.hashCode
	override def equals(other: Any): Boolean =
		other match {
			case that: AssignmentAnonymity => this.code.equals(that.code)// Reference equality
			case _ => false
		}
}

object AssignmentAnonymity {
	// you can't infer from state alone whether there's a request outstanding - use extension.awaitingReview()
	case object NameAndID extends AssignmentAnonymity("NameAndID", "Show name and University ID")
	case object IDOnly extends AssignmentAnonymity("IDOnly", "Show University ID only")
	case object FullyAnonymous extends AssignmentAnonymity("FullyAnonymous", "Fully anonymous")

	// Don't change this to a val https://warwick.slack.com/archives/C029QTGBN/p1493995125972397
	def values: Seq[AssignmentAnonymity] = Seq(NameAndID, IDOnly)

	def fromCode(code: String): AssignmentAnonymity =
		if (code == null || code.isEmptyOrWhitespace) null
		else values.find{_.code == code} match {
			case Some(method) => method
			case None => throw new IllegalArgumentException()
		}
}

class AssignmentAnonymityUserType extends AbstractBasicUserType[AssignmentAnonymity, String] {
	val basicType = StandardBasicTypes.STRING
	override def sqlTypes = Array(Types.VARCHAR)

	val nullValue = null
	val nullObject = null

	override def convertToObject(string: String): AssignmentAnonymity = AssignmentAnonymity.fromCode(string)
	override def convertToValue(aa: AssignmentAnonymity): String = aa.code
}

class AssignmentAnonymityConverter extends TwoWayConverter[String, AssignmentAnonymity] {
	override def convertRight(code: String): AssignmentAnonymity = AssignmentAnonymity.fromCode(code)
	override def convertLeft(aa: AssignmentAnonymity): String = (Option(aa) map { _.code }).orNull
}