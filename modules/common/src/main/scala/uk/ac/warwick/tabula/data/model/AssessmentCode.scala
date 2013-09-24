package uk.ac.warwick.tabula.data.model

import uk.ac.warwick.tabula.data.convert.ConvertibleConverter

/**
  * Defines the type of [[uk.ac.warwick.tabula.data.model.AssessmentComponent]]
  * such as assignment or exam or winter exam or... etc.
  *
  * While 'A' appears to be the only kind of assignment,
  * 'E' is not the only kind of exam. 
  */
case class AssessmentCode(val code: String) extends Convertible[String] {
	def value = code
}

object AssessmentCode {
	implicit val factory = { code:String => AssessmentCode(code) }

	// For convenience we have a value for the code for assignments,
	// but we don't have an exhaustive/unchanging list of possible codes
	// so can't define a set of case objects for all possible values.
	val Assignment = AssessmentCode("A")
}

// usertype for hib, converter for spring
class AssessmentCodeUserType extends ConvertibleStringUserType[AssessmentCode]
class AssessmentCodeConverter extends ConvertibleConverter[String, AssessmentCode]

