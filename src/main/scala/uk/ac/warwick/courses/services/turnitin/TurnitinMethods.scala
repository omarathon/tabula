package uk.ac.warwick.courses.services.turnitin

import java.io.File

import TurnitinDates._
import TurnitinMethods._

trait Response {
	def successful:Boolean
}
abstract class SuccessResponse extends Response { def successful = true }
abstract class FailureResponse extends Response { def successful = false }
case class Created(id:String) extends SuccessResponse
case class Deleted() extends SuccessResponse
case class GotSubmissions(list:Seq[TurnitinSubmissionInfo]) extends SuccessResponse
case class AlreadyExists() extends FailureResponse
//case class NotFound() extends FailureResponse
case class ClassNotFound() extends FailureResponse
case class AssignmentNotFound() extends FailureResponse
case class SubmissionNotFound() extends FailureResponse
case class Failed(code:Int, reason:String) extends FailureResponse

trait TurnitinMethods extends TurnitinAPI {

	def doRequest(functionId: String, pdata: Option[FileData], params: Pair[String, String]*) : TurnitinResponse
	
	/**
	 * Create a new Class by this name. If there's already a Class by this name,
	 * it will still return success but the ID will be that of the existing Class.
	 * 
	 * Classes are set to expire in 5 years.
	 */
	def createClass(className:String): Response = {
		val response = doRequest(CreateClassFunction, None,
				"ctl" -> className, 
				"ced" -> yearsFromNow(5))

		if (response.success && response.classId.isDefined) Created(response.classId.get)
		else Failed(response.code, response.message)
	}
	
	/**
	 * Create a new Assignment in this class. If there's already an Assignment by this name,
	 * it will return success with the ID of the existing Assignment.
	 */
	def createAssignment(className:String, assignmentName:String, update:Boolean=false): Response = {
		var params = List("ctl" -> className, 
				"assign" -> assignmentName,
				"dtstart" -> monthsFromNow(0), //The start date for this assignment must occur on or after today.
				"dtdue" -> monthsFromNow(6))
		if (update) params = ("fcmd" -> "3") :: params
		val response = doRequest(CreateAssignmentFunction, None, params:_*)
		
		if (response.code == 419) AlreadyExists()
		else if (response.code == 206) ClassNotFound()
		else if (response.success) Created(response.assignmentId getOrElse "")
		else Failed(response.code, response.message)
	}
	
	def deleteAssignment(className:String, assignmentName:String): Response = {
		val response = doRequest(CreateAssignmentFunction, None, 
				"ctl" -> className,
				"assign" -> assignmentName, 
				"fcmd" -> "6")
		Failed(response.code, response.message)
		// 411 -> it didn't exist
	}
	
	def submitPaper(className:String, assignmentName:String, paperTitle:String, file:File, authorFirstName:String, authorLastName:String): Response = { 
		val response = doRequest(SubmitPaperFunction, Some(FileData(file, paperTitle)),
				"ctl" -> className,
				"assign" -> assignmentName,
				"ptl" -> paperTitle,
				"ptype" -> "2",
				"pfn" -> authorFirstName,
				"pln" -> authorLastName)
				
		if (response.success) Created(response.objectId getOrElse "")
		else Failed(response.code, response.message)
	}
	
	def deleteSubmission(className:String, assignmentName:String, oid:String): Response = {
		val response = doRequest(DeletePaperFunction, None,
				"ctl" -> className,
				"assign" -> assignmentName,
				"oid" -> oid )
		if (response.success) Deleted()
		else Failed(response.code, response.message)
	}
	
	def getReport(paperId:String): Response = {
		val response = doRequest(GenerateReportFunction, None,
				"oid" -> paperId)
		Failed(response.code, response.message)
	}
	
	def listSubmissions(className:String, assignmentName:String): Response = {
		val response = doRequest(ListSubmissionsFunction, None, 
				"ctl" -> className,
				"assign" -> assignmentName, 
				"fcmd" -> "2")
		if (response.success) GotSubmissions(response.submissionsList)
		else Failed(response.code, response.message)
	}
	
	def resolveError(response:TurnitinResponse): Response = response.code match {
		case 419 => AlreadyExists()
		case _ => Failed(response.code, response.message)
	}
		
}


object TurnitinMethods {
	// Values for the "fid" parameter of an API call 
	private val CreateClassFunction = "2"
	private val CreateAssignmentFunction = "4"
	private val SubmitPaperFunction = "5"
	private val GenerateReportFunction = "6"
	private val DeletePaperFunction = "8"
	private val ListSubmissionsFunction = "10"
}