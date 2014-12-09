package uk.ac.warwick.tabula.data.model

import org.hibernate.`type`.StandardBasicTypes
import java.sql.Types

sealed abstract class FeedbackForSitsStatus(val code: String, val description: String) {
	// for Spring
	def getCode = code
	def getDescription = description

	override def toString = description
}

object FeedbackForSitsStatus {
	case object UploadNotAttempted extends FeedbackForSitsStatus("uploadNotAttempted", "Upload not yet attempted")
	case object Failed extends FeedbackForSitsStatus("failed", "Failed")
	case object Successful extends FeedbackForSitsStatus("successful", "Successful")

	// manual collection - keep in sync with the case objects above
	val members = Seq(UploadNotAttempted, Successful, Failed)

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

class FeedbackForSitsStatusUserType extends AbstractStringUserType[FeedbackForSitsStatus] {

	override def sqlTypes = Array(Types.VARCHAR)
	override def convertToObject(string: String) = FeedbackForSitsStatus.fromCode(string)
	override def convertToValue(format: FeedbackForSitsStatus) = format.code
}
