package uk.ac.warwick.tabula.coursework.commands.departments

<<<<<<< HEAD
=======
import collection.JavaConversions._
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.data.model.Assignment
>>>>>>> TAB-530 First stab at spreadsheet, also includes mock ReportWorld object
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.data.model.UserGroup
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Module
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.model.AuditEvent
import uk.ac.warwick.tabula.services.{AssignmentMembershipService, AuditEventQueryMethods}
import collection.JavaConversions._
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.userlookup.User


// reusable environment for marking workflow tests
trait ReportWorld extends AppContextTestBase with Mockito {
	
	val department = new Department
	department.code = "IN"
	department.name = "Test Department"
		
	val moduleOne = new Module("IN101", department)
	moduleOne.name = "Module One"
	val moduleTwo = new Module("IN102", department)
	moduleTwo.name = "Module Two"

	department.modules = List(moduleOne, moduleTwo)
	
	var auditEvents: List[AuditEvent] = List()
	
	val assignmentOne = addAssignment("1001", "test one", dateTime(2013, 3, 10), 10, 5, moduleOne)
	val assignmentTwo = addAssignment("1002", "test two", dateTime(2013, 4, 10), 29, 5, moduleOne)
	val assignmentThree = addAssignment("1003", "test three", dateTime(2013, 5, 10), 13, 5, moduleOne)
	
	val assignmentFour = addAssignment("1004", "test four", dateTime(2013, 5, 31), 35, 5, moduleTwo)
	val assignmentFive = addAssignment("1005", "test five", dateTime(2013, 8, 23), 100, 50, moduleTwo)
	val assignmentSix = addAssignment("1006", "test six", dateTime(2013, 7, 1), 73, 3, moduleTwo)
	
	
	createPublishEvent(assignmentOne, 15, studentData(1, 10)) 	// all on time
	createPublishEvent(assignmentTwo, 35, studentData(1, 29))		// all late
	createPublishEvent(assignmentThree, 10, studentData(1, 4))	// for test three - these on time
	createPublishEvent(assignmentThree, 35, studentData(5, 13))	// ... and these late

	createPublishEvent(assignmentFour, 30, studentData(1, 35))	// on time for the 7 (that is, 35 % 5) that have submitted late, late for the rest
	createPublishEvent(assignmentFive, 31, studentData(1, 100))	// on time for the 2 (100 % 50) that have submitted late, late for the rest
	createPublishEvent(assignmentSix, 15, studentData(1, 23))		// on time
	createPublishEvent(assignmentSix, 20, studentData(24, 65))	// on time
	createPublishEvent(assignmentSix, 29, studentData(66, 73))	// late

	var auditEventQueryMethods = mock[AuditEventQueryMethods]
	auditEventQueryMethods.submissionForStudent(any[Assignment], any[User]) answers {argsObj => {
		val args = argsObj.asInstanceOf[Array[_]]
		val assignment = args(0).asInstanceOf[Assignment]
		val user = args(1).asInstanceOf[User]
		auditEvents.filter(event => {event.userId == user.getUserId && event.assignmentId.get == assignment.id})
	}}

	auditEventQueryMethods.publishFeedbackForStudent(any[Assignment], any[User]) answers {argsObj => {
		val args = argsObj.asInstanceOf[Array[_]]
		val assignment = args(0).asInstanceOf[Assignment]
		val user = args(1).asInstanceOf[User]
		auditEvents.filter(event => {event.students.contains(user.getWarwickId) && event.assignmentId.get == assignment.id})
	}}

	var assignmentMembershipService = mock[AssignmentMembershipService]
	assignmentMembershipService.determineMembershipUsers(any[Assignment]) answers { assignmentObj =>
		val assignment = assignmentObj.asInstanceOf[Assignment]
		val studentIds = assignment.members.includeUsers
		val users = studentIds.map{userId =>
			val userOne = new User(userId)
			userOne.setWarwickId(userId)
			userOne
		}.toList
		users
	}


	def studentData(start:Int, end:Int) = (start to end).map(idFormat(_)).toList 
	
	def createPublishEvent(assignment: Assignment, daysAfter: Int, students: List[String]) = {
		val date = assignment.closeDate.plusDays(daysAfter)
		val event = AuditEvent(
				eventId="event", eventType="PublishFeedback", userId="cuslat", eventDate=date,
				eventStage="before", data="""{"assignment": """ + assignment.id + """,
											  "students" : """ + studentsData(students) + """  }"""
			)
			
		event.related = Seq(event)
		event.parsedData = Some(json.readValue(event.data, classOf[Map[String, Any]]))
			
		auditEvents = event :: auditEvents
	}
	
	def studentsData(students: List[String]) = students.addString(new StringBuilder(), """["""", """","""", """"]""")	
	
	
	val extension = new Extension(idFormat(3))
	extension.approved = true
	extension.expiryDate = assignmentSix.closeDate.plusDays(2)
	assignmentSix.extensions = Seq(extension)
	
	def addAssignment(id: String, name: String, closeDate: DateTime, numberOfStudents: Int, lateModNumber: Int, module: Module) = {
		val assignment = new Assignment(module)
		assignment.id = id
		assignment.name = name
		assignment.closeDate = closeDate
		assignment.collectSubmissions = true
		assignment.module = module

		for (i <- 1 to numberOfStudents) {
			generateSubmission(assignment, i, lateModNumber)
			addFeedback(assignment)
		}
		
		val userIds = (1 to numberOfStudents).map(idFormat(_)) 
		assignment.members = makeUserGroup(userIds)
		module.assignments = module.assignments + assignment

		assignment
	}
	
	
	def addFeedback(assignment:Assignment){
		withFakeTime(dateTime(2013, 3, 13)) {	
			val feedback = assignment.submissions.map{s=>
				val newFeedback = new Feedback
				newFeedback.assignment = assignment
				newFeedback.universityId = s.universityId
				newFeedback.released = false
				newFeedback
			}
			assignment.feedbacks = feedback
		}
	}

	
	def idFormat(i:Int) = "1" + ("%06d" format i)

	def generateSubmission(assignment:Assignment, num: Int, lateModNumber: Int) {
		val submissionDate = if(num % lateModNumber == 0){
			assignment.closeDate.plusDays(1)
		} else {
			assignment.closeDate.minusDays(1)
		}
		withFakeTime(submissionDate) {
			val submission = new Submission()
			submission.assignment = assignment
			submission.universityId = idFormat(num)
			submission.submittedDate = submissionDate
			assignment.submissions.add(submission)
			
			val event = AuditEvent(
				eventId="event", eventType="SubmitAssignment", userId=idFormat(num), eventDate=DateTime.now,
				eventStage="before", data="""{"assignment": """ + assignment.id + """}"""
			)
			
			event.related = Seq(event)
			event.parsedData = Some(json.readValue(event.data, classOf[Map[String, Any]]))
		
			auditEvents = event :: auditEvents
			
			
		}	
	}

	def makeUserGroup(users: Seq[String]): UserGroup = {
		val ug = new UserGroup
		ug.includeUsers = users
		ug
	}
}