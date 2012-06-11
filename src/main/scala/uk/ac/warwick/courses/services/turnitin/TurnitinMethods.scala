package uk.ac.warwick.courses.services.turnitin

import TurnitinDates._
import TurnitinMethods._

trait TurnitinMethods {

	private[turnitin] def doRequest(functionId: String, params: Pair[String, String]*)
	
	/**
	 * Create a new Class by this name. If there's already a Class by this name,
	 * it will still return success but the ID will be that of the existing Class.
	 * 
	 * Classes are set to expire in 5 years.
	 */
	def createClass(className:String) = 
		doRequest(CreateClassFunction, 
				"ctl" -> className, 
				"ced" -> yearsFromNow(5))
	
	/**
	 * Create a new Assignment in this class. If there's already an Assignment by this name,
	 * it will return success with the ID of the existing Assignment.
	 */
	def createAssignment(className:String, assignmentName:String) = {
		doRequest(CreateAssignmentFunction, 
				"ctl" -> className, 
				"assign" -> assignmentName,
				"dtstart" -> monthsFromNow(-6), 
				"dtend" -> monthsFromNow(6))
	}
	
	def submitPaper() = 
		doRequest(SubmitPaperFunction)
		
	
}


object TurnitinMethods {
	// API numbers for 
	private val CreateClassFunction = "2"
	private val CreateAssignmentFunction = "4"
	private val SubmitPaperFunction = "5"
	private val GenerateReportFunction = "6"
}