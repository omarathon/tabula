package uk.ac.warwick.courses.services.turnitin

import scala.xml.Elem

/** Response from the API. 
  */
class TurnitinResponse(
	val code: Int,
	diagnostic: Option[String] = None,
	val xml: Option[Elem] = None) {
	def success = code <= 100
	def error = !success
	
	lazy val assignmentId = xml map { _ \\ "assignmentid" text } filterNot emptyString
	lazy val classId = xml map { _ \\ "classid" text } filterNot emptyString
	lazy val userId = xml map { _ \\ "userid" text } filterNot emptyString
	lazy val objectId = xml map { _ \\ "objectID" text } filterNot emptyString
	
	/**
	 * Convert an <object> list into a list of TurnitinSubmissionInfo items.
	 * 
	 * It is assumed that the student's Uni ID was used as the first name, so
	 * it is used as the firstname element.
	 */
	lazy val submissionsList = {
		xml map { 
			_ \\ "object" map { obj =>
				TurnitinSubmissionInfo(
					objectId = (obj \\ "objectID" text),
					title = (obj \\ "title" text),
					universityId = (obj \\ "firstname" text),
					similarityScore = (obj \\ "similarityScore" text).toInt,
					overlap = parseInt(obj \\ "overlap" text),
					webOverlap = parseInt(obj \\ "web_overlap" text),
					publicationOverlap = parseInt(obj \\ "publication_overlap" text),
					studentPaperOverlap = parseInt(obj \\ "student_paper_overlap" text)
				)
			}
		} getOrElse Nil
	}
	
	def message = TurnitinResponse.responseCodes.getOrElse(code, unknownMessage)
	private def unknownMessage = if (success) "Unknown code" else "Unknown error"
	private def emptyString(s: String) = s.trim.isEmpty
	private def parseInt(text:String) = if (text.isEmpty) None else Some(text.toInt)
}

object TurnitinResponse {

	val StatusRegex = "(?s).+rcode&gt;(\\d+)&lt;.+".r

	/** Response in diagnostic mode is all HTML but we can
	  * regex out the status code at least.
	  */
	def fromDiagnostic(string: String) = {
		val code = string match {
			case StatusRegex(status) => status.toInt
			case _ => 1
		}
		new TurnitinResponse(code, diagnostic = Some(string))
	}

	def fromXml(xml: Elem) = {
		new TurnitinResponse((xml \\ "rcode").text.toInt, xml = Some(xml))
	}

	/** From the API documentation. */
	val responseCodes = Map(
		1 -> "General Success State, no errors",
		11 -> "successful",
		21 -> "successful, do not sent to login",
		31 -> "successful, do not send to login",
		40 -> "successful",
		51 -> "successful",
		61 -> "successful",
		70 -> "successful",
		71 -> "successful",
		72 -> "successful",
		73 -> "successful",
		74 -> "successful",
		75 -> "successful",
		100 -> "Primary account ID missing from URL",
		101 -> "No HTTPS - the URL was not transmitted via HTTPS",
		102 -> "GMT missing from URL",
		103 -> "GMT malformed - the date/time in the URL is bad",
		104 -> "Email missing from URL",
		105 -> "Email address is not between 5-75 characters",
		106 -> "Email address contains whitespace",
		107 -> "Email address in URL is malformed",
		108 -> "Password in URL contained whitespace",
		109 -> "Diagnostic value in URL is bad",
		110 -> "MD5 missing from URL",
		111 -> "fcmd missing from URL",
		112 -> "User first name missing from URL or incorrect length",
		113 -> "User last name missing from URL or incorrect length",
		114 -> "Class title missing from URL, or not between 5-50 characters",
		115 -> "Password is not between 6-12 characters",
		116 -> "fid missing from URL, or does not reference existing function",
		117 -> "User type missing from URL or is not valid",
		118 -> "encrypt value in URL is bad or missing",
		119 -> "Paper author’s first name missing or incorrect length",
		120 -> "Paper author’s last name missing or incorrect length",
		121 -> "Paper title missing ",
		122 -> "Paper type missing or invalid",
		123 -> "Assignment title missing or incorrect length",
		124 -> "ObjectID missing",
		125 -> "Password is required for fid 6",
		126 -> "Date start and date due is required for function",
		200 -> "Primary account ID missing from Turnitin database",
		201 -> "IP address missing from Turnitin database",
		202 -> "Primary account entry not active in Turnitin database",
		203 -> "MD5 key missing from Turnitin database",
		204 -> "Class does not exist in Turnitin database",
		205 -> "Database error verifying class",
		206 -> "Assignment does not exist in Turnitin database",
		207 -> "Database error verifying assignment",
		208 -> "User is not enrolled in class",
		209 -> "Database error verifying user’s enrollment",
		210 -> "User password does not match user email",
		211 -> "Database error verifying objectID",
		212 -> "objectID does not exist for this user",
		213 -> "objectID does not belong to this user ",
		214 -> "Filename does not exist for this paper",
		215 -> "This primary account ID is not authorized to use this product",
		300 -> "IP address not authenticated. IP address in database does not match URL originating IP address",
		301 -> "Date/time expired – GMT timestamp used in MD5 calculation is off",
		302 -> "MD5 not authenticated - the MD5 in the URL does not match the MD5 calculated",
		400 -> "Creating new user failed",
		401 -> "Unauthorized access - user exists, but does not belong to correct primary account ID or sub-account ID, do not execute function",
		402 -> "User is not an instructor, must be an instructor to run fid 2",
		403 -> "User is not a student, must be a student to run fid 3",
		404 -> "FCMD is not valid",
		405 -> "Class title is not unique",
		406 -> "Creating new class failed in fid 2",
		407 -> "Student failed to join or log in to a class in fid 3",
		408 -> "Attempt to join new user (instructor) to account failedTurnitin API Guide 2.5",
		409 -> "Function requires POST request",
		410 -> "Function requires GET request",
		411 -> "Creating new Assignment failed in fid 4",
		412 -> "Assignment title is not unique",
		413 -> "Error while trying to save the paper",
		414 -> "Originality report not generated yet in fid 6, fcmd 1",
		415 -> "Originality score not available yet in fid 6, fcmd 2",
		416 -> "Error checking if submission existed for user",
		417 -> "Error trying to change user password",
		418 -> "Error trying to delete submission",
		419 -> "Assignment already exists for this class and instructor",
		420 -> "Error trying to view/download original paper",
		421 -> "Error trying to retrieve paper submission info for assignment",
		1000 -> "The due date for this assignment has passed.  Please see your instructor to request a late submission.",
		1001 -> "You may not submit a paper to this assignment until the assignment start date",
		1002 -> "You may not submit a paper to this assignment because the Plagiarism Prevention product is unavailable",
		1003 -> "You have reached the maximum limit of 10 papers for the InSite demo account",
		1004 -> "Paper author’s first name missing or incorrect length",
		1005 -> "Paper author’s last name missing or incorrect length",
		1006 -> "Paper title missing",
		1007 -> "The file you have uploaded is too big",
		1008 -> "No file uploaded",
		1009 -> "Invalid file type!  Valid file types are MS Word, Acrobat PDF, Postscript, Text, HTML, WordPerfect (WPD) and Rich Text Format.",
		1010 -> "You must submit more than 100 characters of non-whitespace",
		1011 -> "It appears as though the copying and pasting of your document resulted in spaces getting introduced between each letter.  Please try again or contact our helpdesk if the problem persists.",
		1012 -> "Paper length exceeds limits",
		1013 -> "You must submit more than 20 words of text",
		1014 -> "You must select an enrolled student as the author of this paper",
		1015 -> "You have already submitted a paper to this assignment.  Please contact your instructor to request a resubmission",
		1016 -> "This student has already submitted a paper to this assignment. Please delete the original paper before submitting a new one.",
		2025 -> "The class name must be between 5-200 characters",
		2026 -> "The class enrollment password must be between 4-12 characters",
		2027 -> "There was an error processing your request",
		2035 -> "There was an error processing your request",
		2036 -> "There was an error processing your request",
		2300 -> "You have entered an invalid date!",
		2301 -> "You must select a rubric set to user with remediation",
		2302 -> "The assignment title must be between 2-100 characters",
		2303 -> "The assignment must have a point value between 0 and 1000",
		2304 -> "The assignment instructions must be less than 8000 characters",
		2305 -> "The start date for this assignment must occur on or after today",
		2306 -> "The due date for this assignment must occur on or after the start date",
		2307 -> "The due date for this assignment must occur within 6 months of the start date",
		2315 -> "There was an error processing your request",
		2316 -> "There was an error processing your request",
		2317 -> "There was an error processing your request",
		2318 -> "You cannot change the search targets because papers have already been submitted to this assignment. You must create a new assignment if you would like to change the search targets",
		2319 -> "There was an error processing your request",
		2320 -> "There was an error processing your request")

}