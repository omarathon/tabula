package uk.ac.warwick.courses.services.turnitin

sealed abstract class FunctionId(val id:Int, val description:String)
case class CreateAssignmentFunction() extends FunctionId(4, "Create assignment")
case class SubmitPaperFunction() extends FunctionId(5, "Submit paper")
case class GenerateReportFunction() extends FunctionId(6, "Generate report")

class Turnitin {
	val url = "https://submit.ac.uk/api.asp"
	def aid = 299
	def said = 8432
	var diagnostic = true
	
	
	
	def doRequest(f:FunctionId) {
		
	}
}