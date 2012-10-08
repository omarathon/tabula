package uk.ac.warwick.courses.services.turnitin


import collection.JavaConverters._
import java.io.File

import TurnitinDates._
import TurnitinMethods._

import dispatch.Request

import uk.ac.warwick.courses.helpers.Logging
import uk.ac.warwick.util.web._

trait Response {
	def successful: Boolean
}
abstract class SuccessResponse extends Response { def successful = true }
abstract class FailureResponse extends Response { def successful = false }
case class Created(id: String) extends SuccessResponse
case class Deleted() extends SuccessResponse
case class Done() extends SuccessResponse
case class GotSubmissions(list: Seq[TurnitinSubmissionInfo]) extends SuccessResponse
case class AlreadyExists() extends FailureResponse
case class DuplicateClass() extends FailureResponse
case class ClassNotFound() extends FailureResponse
case class AssignmentNotFound() extends FailureResponse
case class SubmissionNotFound() extends FailureResponse
case class Failed(code: Int, reason: String) extends FailureResponse

/**
 * Contains the main API methods that can be called from Session.
 * 
 * About IDs
 * ---------
 * 
 * In Turnitin, classes and assignments can have two kinds of ID. They always have an internally
 * generated ID which is returned in a response, but it's also possible to pass your own external ID
 * when creating the object. The API will never expose this back to you, but it is stored with the object
 * so you can later refer to it by that ID.
 * 
 * I think when we use a ClassId or AssignmentId, we should use our own values.
 * 
 * About creating objects
 * ----------------------
 * 
 * For many of the APIs, if you make a request as a user who doesn't already exist, they will be created
 * (though the login step will do this anyway). If you create an assignment referring to a class ID that
 * doesn't exist, the class will be created implicitly. the same goes for the paper submission, which can
 * implicitly create both a class and an assignment for you. So you can avoid a few API calls by skipping
 * the step of making those things explicitly.
 * 
 */
trait TurnitinMethods { self: Session =>

	// Create a session in turnitin and return a session ID.
	// This is only called by Turnitin service, and once you have a Session
	// you are already logged in and can just use the other methods.
	def login() = {
		if (sessionId != null) {
			throw new UnsupportedOperationException("This should only be called by Turnitin service on an anonymous Session without an ID")
		}

		val response = doRequestAdvanced(LoginFunction, None,
			"fcmd" -> "2",
			"utp" -> "2",
			"uem" -> userEmail,
			"ufn" -> userFirstName,
			"uln" -> userLastName,
			"create_session" -> "1") { request =>
				request >:+ { (headers, request) =>
					logger.debug("Login request")
					logger.debug(headers)
					request <> { (node) => TurnitinResponse.fromXml(node) }
				}
			}
		if (logger.isDebugEnabled) {
			logger.debug("Login %s : %s" format (userEmail, response))
		}
		if (response.success) Created(response.sessionId.getOrElse(""))
		else Failed(response.code, response.message)
	}

	/**
	 * Returns a URL to the Turnitin home page hat initiates a session as this user.
	 *
	 * The Session ID is contained within the URL so it is unique to the user and shouldn't
	 * be shared.
	 *
	 * Note that Session uses the same session so calling logout() will invalidate any
	 * link generated using this.
	 */
	def getLoginLink(): Option[Uri] = {
		 doRequest(LoginFunction, None,
			"fcmd" -> "1",
			"utp" -> "2",
			"uem" -> userEmail,
			"ufn" -> userFirstName,
			"uln" -> userLastName)
			.redirectUrl.map { path =>
			 
			UriBuilder.parse(apiEndpoint).setPath(path).toUri
		 }
	}
	
	/**
	 * There is an API call fid=6 to get a report link, but at the moment it doesn't
	 * work, so this generates a link to the originality report viewer for a document.
	 */
	def getDocumentViewerLink(document: DocumentId): Uri = {
		UriBuilder.parse(apiEndpoint)
    		.setPath("/dv")
    		.setQuery("")
    		.addQueryParameter("o", document.value)
    		.addQueryParameter("session-id", sessionId)
    		.addQueryParameter("s", "1") // s=1 -> Originality report
    		.toUri
	}

	/**
	 * Ends this session in Turnitin. getLoginLink generates a link for the same session
	 *
	 */
	def logout() = {
		val response = doRequest(LogoutFunction, None,
			"utp" -> "2",
			"fcmd" -> "2",
			"uem" -> userEmail,
			"ufn" -> userFirstName,
			"uln" -> userLastName,
			"create_session" -> "1")
		logger.debug(response)
	}

	/**
	 * Gets/creates this user and returns the userid
	 * if successful.
	 */
	def getUserId(): Option[String] = {
		doRequest(LoginFunction, None,
			"fcmd" -> "2",
			"utp" -> "2",
			"uem" -> userEmail,
			"ufn" -> userFirstName,
			"uln" -> userLastName)
			.userId
	}
	
	/**
	 * Ensures that the session has a userid for the user who is logged in.
	 */
	def acquireUserId() {
		if (userId == "") {
		    userId = getUserId.getOrElse(throw new IllegalStateException("Failed to get userid"))
		}
	}

	/**
	 * Adds the current user as a tutor on the class with this ID.
	 *
	 * If the person is already enrolled on this class, it still returns success,
	 * so it's fine to call this eagerly.
	 * 
	 * If the class doesn't exist, it is created with the given external ID.
	 *
	 * The className is not used to find the class, but it is required in the
	 * request - the class's name in Turnitin will be updated with this name.
	 */
	def addTutor(classId: ClassId, className: ClassName): Response = {
		val response = doRequest(CreateClassFunction, None,
			"utp" -> "2",
			"fcmd" -> "2",
			"cid" -> classId.value,
			"ctl" -> className.value)
		for (d <- response.diagnostic) logger.debug(d)
		if (response.success) Done()
		else if (response.code == 220) DuplicateClass() // shouldn't happen as we're using IDs.
		else Failed(response.code, response.message)
	}

	/**
	 * Create a new Class with this external ID and name. If successful, it returns
	 * Created() with the internal ID of the class (which will be different to the external ID you provided,
	 * but you can later refer to the class using either as ClassId). The classId returned from responses
	 * is always the internally-generated ID, even if you specified your own ID (though that is stored, invisibly).
	 * 
	 * If the given ClassId already exists, that class is returned and no new class is created.
	 *
	 * Classes are set to expire in 5 years.
	 */
	def createClass(classId: ClassId, className: ClassName): Response = {
		val response = doRequest(CreateClassFunction, None,
			"cid" -> classId.value,
			"ctl" -> className.value,
			"ced" -> yearsFromNow(5))

		if (response.success && response.classId.isDefined) Created(response.classId.get)
		else Failed(response.code, response.message)
	}
	
	/**
	 * Create a new Assignment in this class, using the given AssignmentId as the externally-provided ID.
	 * 
	 * You can pass in a ClassId that doesn't exist, and it will create the class (and add the current user
	 * as a class instructor). 
	 * 
	 * If the requested AssignmentId already exists, AlreadyExists() is returned.
	 */
	def createAssignment(classId: ClassId, className: ClassName, assignmentId: AssignmentId, assignmentName: AssignmentName): Response = {
		val params = commonAssignmentParams(classId, className, assignmentId, assignmentName)
		val response = doRequest(CreateAssignmentFunction, None, params: _*)
		resolveAssignmentResponse(response)
	}
	
	/**
	 * Update an existing assignment in this class.
	 */
	def updateAssignment(classId: ClassId, className: ClassName, assignmentId: AssignmentId, assignmentName: AssignmentName): Response = {
		val params = commonAssignmentParams(classId, className, assignmentId, assignmentName) ++ Seq("fcmd" -> "3")
        val response = doRequest(CreateAssignmentFunction, None, params: _*)
        resolveAssignmentResponse(response)
	}

	private def commonAssignmentParams(classId: ClassId, className: ClassName, assignmentId: AssignmentId, assignmentName: AssignmentName) = List(
		"cid" -> classId.value,
		"ctl" -> className.value, // used if class doesn't exist, to create the class.
		"assignid" -> assignmentId.value,
		"assign" -> assignmentName.value, // used if the assignment doesn't exist, to name the assignment.
		"dtstart" -> monthsFromNow(0), //The start date for this assignment must occur on or after today.
		"dtdue" -> monthsFromNow(6))

	private def resolveAssignmentResponse(response: TurnitinResponse) = {
		if (response.code == 419) AlreadyExists()
		else if (response.code == 206) ClassNotFound()
		else if (response.success) Created(response.assignmentId getOrElse "")
		else Failed(response.code, response.message)
	}

	def deleteAssignment(classId: ClassId, assignmentId: AssignmentId): Response = {
		val response = doRequest(CreateAssignmentFunction, None,
			"cid" -> classId.value,
			"assignid" -> assignmentId.value,
			"fcmd" -> "6")
		Failed(response.code, response.message)
		// 411 -> it didn't exist
	}

	/**
	 * Submit a paper to this ClassId and AssignmentId.
	 * 
	 * If this AssignmentId doesn't exist, the assignment is created using the
	 * given name. 
	 */
	def submitPaper(
			classId: ClassId, // what
			className: ClassName, // a
			assignmentId: AssignmentId, // lot 
			assignmentName:AssignmentName, // of
			paperTitle: String, // arguments.
			file: File, 
			authorFirstName: String, 
			authorLastName: String): Response = {
		
		val response = doRequest(SubmitPaperFunction, Some(FileData(file, paperTitle)),
			"cid" -> classId.value,
			"ctl" -> className.value,
			"assignid" -> assignmentId.value,
			"assign" -> assignmentName.value,
			"ptl" -> paperTitle,
			"ptype" -> "2",
			"pfn" -> authorFirstName,
			"pln" -> authorLastName)

		if (response.success) Created(response.objectId getOrElse "")
		else resolveError(response)
	}

	/**
	 * Delete a submission document from Turnitin.
	 * 
	 * Doesn't remove it from any central repository, so you will still get plagiarism matches from deleted documents.
	 */
	def deleteSubmission(classId: ClassId, assignmentId: AssignmentId, oid: DocumentId): Response = {
		val response = doRequest(DeletePaperFunction, None,
			"cid" -> classId.value,
			"assignid" -> assignmentId.value,
			"oid" -> oid.value)
		if (response.success) Deleted()
		else Failed(response.code, response.message)
	}

	/**
	 * fcmd 1 should generate a redirect to a URL to view the report for
	 * this document. At the moment it throws an error so am using a hacky method (see getDocumentViewerLink)
	 */
	def getReport(paperId: DocumentId) = {
		val response = doRequest(GenerateReportFunction, None, 
			"oid" -> paperId.value,
			"fcmd" -> "1")
		println(response)
		response
//		val params = calculateParameters(GenerateReportFunction,
//			"oid" -> paperId.value,
//			"fcmd" -> "1")
//		UriBuilder.parse(apiEndpoint).addQueryParameters(params.toMap.asJava).toUri()
//		Failed(response.code, response.message)
	}

	/**
	 * List the submissions made to this assignment in this class.
	 * 
	 * Unlike some others, this command will not create the class or the assignment implicitly -
	 * they must already exist. You will get ClassNotFound or AssignmentNotFound if either are
	 * missing. The API documentation claims otherwise.
	 */
	def listSubmissions(classId: ClassId, assignmentId: AssignmentId): Response = {
		val response = doRequest(ListSubmissionsFunction, None,
			"cid" -> classId.value,
			"ctl" -> "Class name", // required but ignored.
			"assignid" -> assignmentId.value,
			"assign" -> "Assignment name", //required but ignored.
			"tem" -> userEmail,
			"fcmd" -> "2")
		if (response.success) GotSubmissions(response.submissionsList)
		else resolveError(response)
	}

	def resolveError(response: TurnitinResponse): Response = response.code match {
		case 419 => AlreadyExists()
		case 204 => ClassNotFound()
		case 206 => AssignmentNotFound()
		case _ => Failed(response.code, response.message)
	}

}

object TurnitinMethods {
	// Values for the "fid" parameter of an API call 
	private val LoginFunction = "1"
	private val CreateClassFunction = "2"
	private val CreateAssignmentFunction = "4"
	private val SubmitPaperFunction = "5"
	private val GenerateReportFunction = "6"
	private val DeletePaperFunction = "8"
	private val ListSubmissionsFunction = "10"
	private val LogoutFunction = "18"
	private val ListEnrollmentFunction = "19"
}