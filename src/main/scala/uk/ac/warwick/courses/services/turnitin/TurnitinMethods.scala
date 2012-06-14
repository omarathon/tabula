package uk.ac.warwick.courses.services.turnitin

import TurnitinDates._
import TurnitinMethods._
import scala.xml.Elem
import scala.xml.NodeSeq
import java.io.File

trait Response {
	def successful:Boolean
}
abstract class SuccessResponse extends Response { def successful = true }
abstract class FailureResponse extends Response { def successful = false }
case class Created(id:String) extends SuccessResponse
case class AlreadyExists() extends FailureResponse
case class NotFound() extends FailureResponse
case class Failed(reason:String) extends FailureResponse

trait TurnitinMethods {

	def doRequest(functionId: String, pdata: Option[File], params: Pair[String, String]*) : TurnitinResponse
	
	/**
	 * Create a new Class by this name. If there's already a Class by this name,
	 * it will still return success but the ID will be that of the existing Class.
	 * 
	 * Classes are set to expire in 5 years.
	 */
	def createClass(className:String) = {
		val response = doRequest(CreateClassFunction, None,
				"ctl" -> className, 
				"ced" -> yearsFromNow(5))

		if (response.success) Created(response.classId getOrElse "")
		else Failed(response.message)
	}
	
	/**
	 * Create a new Assignment in this class. If there's already an Assignment by this name,
	 * it will return success with the ID of the existing Assignment.
	 */
	def createAssignment(className:String, assignmentName:String, update:Boolean=false) = {
		var params = List("ctl" -> className, 
				"assign" -> assignmentName,
				"dtstart" -> monthsFromNow(0), //The start date for this assignment must occur on or after today.
				"dtdue" -> monthsFromNow(6))
		if (update) params = ("fcmd" -> "3") :: params
		val response = doRequest(CreateAssignmentFunction, None, params:_*)
		
		if (response.code == 419) AlreadyExists()
		else if (response.success) Created(response.assignmentId getOrElse "")
		else Failed(response.message)
	}
	
	def deleteAssignment(className:String, assignmentName:String) = {
		doRequest(CreateAssignmentFunction, None, 
				"ctl" -> className,
				"assign" -> assignmentName, 
				"fcmd" -> "6")
		// 411 -> it didn't exist
	}
	
	def submitPaper(className:String, assignmentName:String, paperTitle:String, file:File, authorFirstName:String, authorLastName:String) = { 
		val response = doRequest(SubmitPaperFunction, Some(file),
				"ctl" -> className,
				"assign" -> assignmentName,
				"ptl" -> paperTitle,
				"ptype" -> "2",
				"pfn" -> authorFirstName,
				"pln" -> authorLastName)
				
		if (response.success) Created(response.objectId getOrElse "")
		else Failed(response.message)
	}
	
	def getReport(paperId:String) = {
		val response = doRequest(GenerateReportFunction, None,
				"oid" -> paperId)
				
		if (response.success) response.xml
		else Failed(response.message)
	}
	
	//def listSubmissions
		
}


object TurnitinMethods {
	// API numbers for 
	private val CreateClassFunction = "2"
	private val CreateAssignmentFunction = "4"
	private val SubmitPaperFunction = "5"
	private val GenerateReportFunction = "6"
}