package uk.ac.warwick.tabula.services

import scala.collection.JavaConverters._
import org.springframework.transaction.annotation.Transactional
import org.junit.Before
import org.joda.time.DateTime
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.data.{AssignmentMembershipDaoImpl, DepartmentDaoImpl}
import uk.ac.warwick.userlookup.User

// scalastyle:off magic.number
class AssignmentServiceTest extends PersistenceTestBase {

	val assignmentService:AssignmentServiceImpl = new AssignmentServiceImpl
	val assignmentMembershipService:AssignmentMembershipServiceImpl =new AssignmentMembershipServiceImpl
	val feedbackService:FeedbackServiceImpl =new FeedbackServiceImpl
	val submissionService:SubmissionServiceImpl =new SubmissionServiceImpl
	val originalityReportService:OriginalityReportServiceImpl =new OriginalityReportServiceImpl
	val extensionService:ExtensionServiceImpl =new ExtensionServiceImpl

  val modAndDeptService:ModuleAndDepartmentService =new ModuleAndDepartmentService
  var userLookup:MockUserLookup = _

  @Before def setup {
		userLookup = new MockUserLookup()
		userLookup.defaultFoundUser = true
		assignmentService.sessionFactory = sessionFactory
		submissionService.sessionFactory = sessionFactory
		extensionService.sessionFactory = sessionFactory
		val deptDao = new DepartmentDaoImpl
		deptDao.sessionFactory = sessionFactory
		modAndDeptService.departmentDao = deptDao
		feedbackService.userLookup = userLookup
		val amDao = new AssignmentMembershipDaoImpl
		amDao.sessionFactory = sessionFactory
		assignmentMembershipService.dao = amDao
		assignmentMembershipService.userLookup = userLookup
	}

	@Transactional @Test def recentAssignment {
		val assignment = newDeepAssignment()
		val department = assignment.module.department

		session.save(department)
		session.save(assignment.module)
		assignmentService.save(assignment)

		assignmentService.recentAssignment(department).get should be (assignment)
	}

	/**
	 * The Hibernate filter that adds deleted != 0
	 */
	@Transactional @Test def notDeletedFilter {
		val module = new Module
		session.save(module)
		val assignment = new Assignment
		assignment.name = "Essay"
		assignment.module = module
		assignment.academicYear = new AcademicYear(2009)
		assignment.markDeleted()
		assignment.addDefaultFields()
		assignmentService.save(assignment)

		assignment.fields.get(1)

		assignmentService.isFilterEnabled("notDeleted") should be (false)
		assignmentService.getAssignmentById(assignment.id) should be (Some(assignment))
		session.enableFilter("notDeleted")
		assignmentService.getAssignmentById(assignment.id) should be (None)

		assignmentService.getAssignmentByNameYearModule(assignment.name, assignment.academicYear, assignment.module) should be ('empty)
	}

	@Transactional @Test def findDuplicateAssignmentNames {
		val module = new Module
		session.save(module)

		assignmentService.getAssignmentByNameYearModule("Essay", new AcademicYear(2009), module) should be ('empty)

		val assignment = new Assignment
		assignment.name = "Essay"
		assignment.module = module
		assignment.academicYear = new AcademicYear(2009)
		assignmentService.save(assignment)
		session.flush()

		assignmentService.getAssignmentByNameYearModule("Essay", new AcademicYear(2009), module) should not be ()
		assignmentService.getAssignmentByNameYearModule("Essay", new AcademicYear(2008), module) should be ('empty)
		assignmentService.getAssignmentByNameYearModule("Blessay", new AcademicYear(2009), module) should be ('empty)
	}

	@Transactional @Test def getAssignmentsByNameTest {
	    val compSciDept = modAndDeptService.getDepartmentByCode("cs")
	    compSciDept should be ('defined)

	    compSciDept.foreach(dept => {
	        assignmentService.getAssignmentsByName("Test", dept) should have size(2)
            assignmentService.getAssignmentsByName("Computing", dept) should have size(1)
	        assignmentService.getAssignmentsByName("Assignment", dept) should have size(3)
            assignmentService.getAssignmentsByName("xxxx", dept) should have size(0)
	    })
    }

	/*
	 * getUsersForFeedback gets all the users associated with an assignment who:
	 *     1. have feedback associated with that assignment which has not been released
	 *     2. have a submission associated with that assignment which is not suspected plagiarised.
	 */
	@Transactional @Test def getUsersForFeedbackTest {
		val assignment = assignmentService.getAssignmentById("1");
		assignment should be('defined)

		assignment.foreach { assmt =>
			// create a feedback for the assignment, not yet released
			val feedback = new Feedback
			feedback.universityId = "0070790"
			feedback.actualMark = Some(41)
			feedback.released = false
			assmt.addFeedback(feedback)
			session.save(feedback)

			// create a submission for the assignment, not plagiarised
			val submission = new Submission

			submission.universityId = "0070790"
			submission.userId = "abcdef"
			submission.suspectPlagiarised = false
			assmt.addSubmission(submission)
			submissionService.saveSubmission(submission)

			// now check one user who needs to get feedback for this assignment is returned
			val userPairs = feedbackService.getUsersForFeedback(assmt)
			userPairs.size should be (1)

			// and check it's the right one
			for (userPair <- userPairs) {
				val studentId = userPair._1
				val user = userPair._2

				studentId should equal ("0070790")
				user.getWarwickId() should equal ("0070790")
			}

			// suppose the feedback was already released - would expect to get no users back
			feedback.released = true
			val userPairs2 = feedbackService.getUsersForFeedback(assmt)
			userPairs2.size should be (0)

			// feedback was not released - expect 1
			feedback.released = false
			val userPairs3 = feedbackService.getUsersForFeedback(assmt)
			userPairs3.size should be (1)

			// the only person was suspected of plagiarism - expect 0
			submission.suspectPlagiarised = true
			val userPairs4 = feedbackService.getUsersForFeedback(assmt)
			userPairs4.size should be (0)
		}

	}

	@Transactional @Test def updateAssessmentComponent {
		val upstream = new AssessmentComponent
		upstream.departmentCode = "ch"
		upstream.moduleCode = "ch101"
		upstream.sequence = "A01"
		upstream.assessmentGroup = "A"
		upstream.assessmentType = AssessmentType.Assignment
		upstream.name = "Egg plants"

		assignmentMembershipService.save(upstream)

		val upstream2 = new AssessmentComponent
		upstream2.departmentCode = "ch"
		upstream2.moduleCode = "ch101"
		upstream2.sequence = "A01"
		upstream2.assessmentGroup = "A"
		upstream2.assessmentType = AssessmentType.Assignment
		upstream2.name = "Greg's plants"

		assignmentMembershipService.save(upstream2)
	}

	@Transactional @Test def findAssignmentsWithFeedback {
		val ThisUser = 	"1234567"
		val OtherUser = "1234568"

		val myFeedback = new Feedback
		myFeedback.universityId = ThisUser
		myFeedback.released = true

		val otherFeedback = new Feedback
		otherFeedback.universityId = OtherUser
		otherFeedback.released = true

		val unreleasedFeedback = new Feedback
		unreleasedFeedback.universityId = ThisUser

		val deletedFeedback = new Feedback
		deletedFeedback.universityId = ThisUser
		deletedFeedback.released = true

		val assignment1 = new Assignment
		val assignment2 = new Assignment
		val assignment3 = new Assignment
		assignment3.markDeleted

		assignment1.addFeedback(myFeedback)
		assignment1.addFeedback(otherFeedback)
		assignment2.addFeedback(unreleasedFeedback)
		assignment3.addFeedback(deletedFeedback)

		assignmentService.save(assignment1)
		assignmentService.save(assignment2)
		assignmentService.save(assignment3)

		session.save(myFeedback)
		session.save(otherFeedback)
		session.save(unreleasedFeedback)
		session.save(deletedFeedback)

		session.enableFilter("notDeleted")

		val assignments = assignmentService.getAssignmentsWithFeedback(ThisUser)
		assignments.size should be (1)
		assignments(0) should be (assignment1)
	}

	@Transactional @Test def findAssignmentsWithSubmission {
		val ThisUser = 	"1234567"
		val OtherUser = "1234568"

		val mySubmission = new Submission
		mySubmission.universityId = ThisUser
		mySubmission.userId = "not-used"

		val otherSubmission = new Submission
		otherSubmission.universityId = OtherUser
		otherSubmission.userId = "not-used"

		val deletedSubmission = new Submission
		deletedSubmission.universityId = ThisUser
		deletedSubmission.userId = "not-used"

		val assignment1 = new Assignment
		val assignment2 = new Assignment
		val assignment3 = new Assignment
		assignment3.markDeleted

		assignment1.addSubmission(mySubmission)
		assignment1.addSubmission(otherSubmission)
		assignment3.addSubmission(deletedSubmission)

		assignmentService.save(assignment1)
		assignmentService.save(assignment2)
		assignmentService.save(assignment3)

		submissionService.saveSubmission(mySubmission)
		submissionService.saveSubmission(otherSubmission)
		submissionService.saveSubmission(deletedSubmission)

		session.enableFilter("notDeleted")

		val assignments = assignmentService.getAssignmentsWithSubmission(ThisUser)
		assignments.size should be (1)
		assignments(0) should be (assignment1)
	}

	@Test def upstreamAssessmentGroups = transactional { tx =>
		val group = new UpstreamAssessmentGroup
		group.moduleCode = "LA155-10"
		group.occurrence = "A"
		group.assessmentGroup = "A"
		group.academicYear = new AcademicYear(2010)
		group.members.staticIncludeUsers.addAll(Seq("rob","kev","bib").asJava)

		assignmentMembershipService.save(group)
		session.flush

		val ua = new AssessmentComponent
		ua.departmentCode = "LA"
		ua.moduleCode = "LA155-10"
		ua.sequence = "A01"
		ua.assessmentGroup = "A"
		ua.assessmentType = AssessmentType.Assignment
		ua.name = "Egg plants"

		assignmentMembershipService.save(ua) should be (ua)

		assignmentMembershipService.getUpstreamAssessmentGroups(ua, new AcademicYear(2010)) should be (Seq(group))
		assignmentMembershipService.getUpstreamAssessmentGroups(ua, new AcademicYear(2011)) should be (Seq())
		assignmentMembershipService.getUpstreamAssessmentGroups(new AssessmentComponent, new AcademicYear(2010)) should be (Seq())

		session.clear

		val foundGroup = assignmentMembershipService.find(group)
		foundGroup should be ('defined)
		foundGroup.eq(Some(group)) should be (false)

		foundGroup.get.occurrence = "B"

		assignmentMembershipService.save(foundGroup.get)
		session.flush
		session.clear

		assignmentMembershipService.find(group) should be ('empty)
		assignmentMembershipService.find(foundGroup.get) should be ('defined)
	}

	@Test def upstreamAssignments = transactional { tx =>
		val ua1 = new AssessmentComponent
		ua1.departmentCode = "CH"
		ua1.moduleCode = "CH101-10"
		ua1.sequence = "A01"
		ua1.assessmentGroup = "A"
		ua1.assessmentType = AssessmentType.Assignment
		ua1.name = "Egg plants"

		val ua2 = new AssessmentComponent
		ua2.departmentCode = "CH"
		ua2.moduleCode = "CH101-20"
		ua2.sequence = "A02"
		ua2.assessmentGroup = "A"
		ua2.assessmentType = AssessmentType.Assignment
		ua2.name = "Egg plants"

		val ua3 = new AssessmentComponent
		ua3.departmentCode = "LA"
		ua3.moduleCode = "LA101-10"
		ua3.sequence = "A01"
		ua3.assessmentGroup = "A"
		ua3.assessmentType = AssessmentType.Assignment
		ua3.name = "Egg plants"

		val ua4 = new AssessmentComponent
		ua4.departmentCode = "LA"
		ua4.moduleCode = "LA101-10"
		ua4.sequence = "A02"
		ua4.assessmentGroup = "A"
		ua4.assessmentType = AssessmentType.Assignment
		ua4.name = "Egg plants NOT IN USE"

		assignmentMembershipService.save(ua1) should be (ua1)
		assignmentMembershipService.save(ua2) should be (ua2)
		assignmentMembershipService.save(ua3) should be (ua3)
		assignmentMembershipService.save(ua4) should be (ua4)

		session.flush

		assignmentMembershipService.getAssessmentComponent(ua1.id) should be (Some(ua1))
		assignmentMembershipService.getAssessmentComponent(ua4.id) should be (Some(ua4))
		assignmentMembershipService.getAssessmentComponent("wibble") should be (None)

		assignmentMembershipService.getAssessmentComponents(Fixtures.module("ch101")) should be (Seq(ua1, ua2))
		assignmentMembershipService.getAssessmentComponents(Fixtures.module("la101")) should be (Seq(ua3))
		assignmentMembershipService.getAssessmentComponents(Fixtures.module("cs101")) should be (Seq())

		assignmentMembershipService.getAssessmentComponents(Fixtures.department("ch")) should be (Seq(ua1, ua2))
		assignmentMembershipService.getAssessmentComponents(Fixtures.department("la")) should be (Seq(ua3))
		assignmentMembershipService.getAssessmentComponents(Fixtures.department("cs")) should be (Seq())
	}

	@Test def assessmentGroups = transactional { tx =>
		val upstreamGroup = new UpstreamAssessmentGroup
		upstreamGroup.moduleCode = "ch101-10"
		upstreamGroup.occurrence = "A"
		upstreamGroup.assessmentGroup = "A"
		upstreamGroup.academicYear = new AcademicYear(2010)
		upstreamGroup.members.staticIncludeUsers.addAll(Seq("rob","kev","bib").asJava)

		assignmentMembershipService.save(upstreamGroup)

		val upstreamAssignment = new AssessmentComponent
		upstreamAssignment.departmentCode = "ch"
		upstreamAssignment.moduleCode = "ch101-10"
		upstreamAssignment.sequence = "A01"
		upstreamAssignment.assessmentGroup = "A"
		upstreamAssignment.assessmentType = AssessmentType.Assignment
		upstreamAssignment.name = "Egg plants"

		assignmentMembershipService.save(upstreamAssignment) should be (upstreamAssignment)

		val assignment = newDeepAssignment("ch101")
		val department = assignment.module.department

		session.save(department)
		session.save(assignment.module)
		assignmentService.save(assignment)

		val group = new AssessmentGroup
		group.assignment = assignment
		group.assessmentComponent = upstreamAssignment
		group.occurrence = "A"

		session.save(group)

		session.flush

		assignmentMembershipService.getAssessmentGroup(group.id) should be (Some(group))

		assignmentMembershipService.getAssessmentGroup(group.id) map { assignmentMembershipService.delete(_) }
		assignmentMembershipService.getAssessmentGroup(group.id) should be ('empty)
	}

	@Test def submissions = transactional { tx =>
		val assignment = newDeepAssignment()
		val department = assignment.module.department

		session.save(department)
		session.save(assignment.module)
		assignmentService.save(assignment)

		val submission = new Submission
		submission.universityId = "0070790"
		submission.userId = "abcdef"
		submission.suspectPlagiarised = false
		assignment.addSubmission(submission)
		submissionService.saveSubmission(submission)

		session.flush
		session.clear

		submissionService.getSubmission(submission.id) should be ('defined)
		submissionService.getSubmission(submission.id).eq(Some(submission)) should be (false)

		submissionService.getSubmissionByUniId(assignment, "0070790") should be ('defined)
		submissionService.getSubmissionByUniId(assignment, "0070790").eq(Some(submission)) should be (false)

		submissionService.getSubmissionByUniId(assignment, "0070790") map { submissionService.delete(_) }

		session.flush
		session.clear

		submissionService.getSubmissionByUniId(assignment, "0070790") should be ('empty)
	}

	@Test def extensions = transactional { tx =>
		val assignment = newDeepAssignment()
		val department = assignment.module.department

		session.save(department)
		session.save(assignment.module)
		assignmentService.save(assignment)

		val extension = new Extension
		extension.universityId = "0070790"
		extension.userId = "abcdef"
		extension.assignment = assignment
		assignment.extensions.add(extension)
		session.saveOrUpdate(extension)

		session.flush
		session.clear

		extensionService.getExtensionById(extension.id) should be ('defined)
		extensionService.getExtensionById(extension.id).eq(Some(extension)) should be (false)

		extensionService.getExtensionById(extension.id) map { session.delete(_) }

		session.flush
		session.clear

		extensionService.getExtensionById(extension.id) should be ('empty)
	}
	
	trait AssignmentMembershipFixture {
		val student1 = new User("student1") { setWarwickId("0000001"); setFoundUser(true); setVerified(true); }
		val student2 = new User("student2") { setWarwickId("0000002"); setFoundUser(true); setVerified(true); }
		val student3 = new User("student3") { setWarwickId("0000003"); setFoundUser(true); setVerified(true); }
		val student4 = new User("student4") { setWarwickId("0000004"); setFoundUser(true); setVerified(true); }
		val student5 = new User("student5") { setWarwickId("0000005"); setFoundUser(true); setVerified(true); }
		val manual1 = new User("manual1") { setWarwickId("0000006"); setFoundUser(true); setVerified(true); }
		val manual2 = new User("manual2") { setWarwickId("0000007"); setFoundUser(true); setVerified(true); }
		val manual3 = new User("manual3") { setWarwickId("0000008"); setFoundUser(true); setVerified(true); }
		val manual4 = new User("manual4") { setWarwickId("0000009"); setFoundUser(true); setVerified(true); }
		val manual5 = new User("manual5") { setWarwickId("0000010"); setFoundUser(true); setVerified(true); }
		val manual10 = new User("manual10") { setWarwickId("0000015"); setFoundUser(true); setVerified(true); }
		
		userLookup.registerUserObjects(student1, student2, student3, student4, student5, manual1, manual2, manual3, manual4, manual5, manual10)
		
		val year = AcademicYear.guessByDate(DateTime.now)

		val assignment1 = newDeepAssignment("ch101")
		assignment1.academicYear = year
		assignment1.assignmentMembershipService = assignmentMembershipService
		assignment1.members.userLookup = userLookup

		val department1 = assignment1.module.department

		session.save(department1)
		session.save(assignment1.module)
		assignmentService.save(assignment1)

		val assignment2 = newDeepAssignment("ch101")
		assignment2.module = assignment1.module
		assignment2.academicYear = year
		assignment2.assignmentMembershipService = assignmentMembershipService
		assignment2.members.userLookup = userLookup

		val department2 = assignment2.module.department

		session.save(department2)
		assignmentService.save(assignment2)

		val up1 = new AssessmentComponent
		up1.departmentCode = "ch"
		up1.moduleCode = "ch101"
		up1.sequence = "A01"
		up1.assessmentGroup = "A"
		up1.assessmentType = AssessmentType.Assignment
		up1.name = "Egg plants"

		val upstream1 = assignmentMembershipService.save(up1)

		val up2 = new AssessmentComponent
		up2.departmentCode = "ch"
		up2.moduleCode = "ch101"
		up2.sequence = "A02"
		up2.assessmentGroup = "B"
		up2.assessmentType = AssessmentType.Assignment
		up2.name = "Greg's plants"

		val upstream2 = assignmentMembershipService.save(up2)

		val up3 = new AssessmentComponent
		up3.departmentCode = "ch"
		up3.moduleCode = "ch101"
		up3.sequence = "A03"
		up3.assessmentGroup = "C"
		up3.assessmentType = AssessmentType.Assignment
		up3.name = "Steg's plants"

    val upstream3 = assignmentMembershipService.save(up3)

    session.flush

    val upstreamAg1 = new UpstreamAssessmentGroup
    upstreamAg1.moduleCode = "ch101"
    upstreamAg1.assessmentGroup = "A"
    upstreamAg1.academicYear = year
    upstreamAg1.occurrence = "A"

    val upstreamAg2 = new UpstreamAssessmentGroup
    upstreamAg2.moduleCode = "ch101"
    upstreamAg2.assessmentGroup = "B"
    upstreamAg2.academicYear = year
    upstreamAg2.occurrence = "B"

    val upstreamAg3 = new UpstreamAssessmentGroup
    upstreamAg3.moduleCode = "ch101"
    upstreamAg3.assessmentGroup = "C"
    upstreamAg3.academicYear = year
    upstreamAg3.occurrence = "C"

    upstreamAg1.members.staticIncludeUsers.add("0000001")
		upstreamAg1.members.staticIncludeUsers.add("0000002")

		upstreamAg2.members.staticIncludeUsers.add("0000002")
		upstreamAg2.members.staticIncludeUsers.add("0000003")

		upstreamAg3.members.staticIncludeUsers.add("0000001")
		upstreamAg3.members.staticIncludeUsers.add("0000002")
		upstreamAg3.members.staticIncludeUsers.add("0000003")
		upstreamAg3.members.staticIncludeUsers.add("0000004")
		upstreamAg3.members.staticIncludeUsers.add("0000005")

		assignmentMembershipService.save(upstreamAg1)
		assignmentMembershipService.save(upstreamAg2)
		assignmentMembershipService.save(upstreamAg3)

		assignment1.members.addUser("manual1")
		assignment1.members.addUser("manual2")
		assignment1.members.addUser("manual3")

		assignment2.members.addUser("manual2")
		assignment2.members.addUser("manual3")
		assignment2.members.addUser("manual4")

		assignment1.members.excludeUser("student2")
		assignment1.members.excludeUser("student3")
		assignment1.members.excludeUser("manual3") // both inc and exc, but exc should take priority

		assignment2.members.excludeUser("student4")
		assignment2.members.excludeUser("student3")

		val ag1 = new AssessmentGroup
		ag1.assignment = assignment1
		ag1.assessmentComponent = upstream1
		ag1.occurrence = "A"

		val ag2 = new AssessmentGroup
		ag2.assignment = assignment1
		ag2.assessmentComponent = upstream2
		ag2.occurrence = "B"

		val ag3 = new AssessmentGroup
		ag3.assignment = assignment2
		ag3.assessmentComponent = upstream3
		ag3.occurrence = "C"

		assignment1.assessmentGroups.add(ag1)
		assignment1.assessmentGroups.add(ag3)

		assignment2.assessmentGroups.add(ag2)

		assignmentService.save(assignment1)
		assignmentService.save(assignment2)

		session.flush
		
		val universityIdGroup = UserGroup.ofUniversityIds
		universityIdGroup.userLookup = userLookup
		universityIdGroup.addUser("0000001")
		universityIdGroup.addUser("0000010")
		universityIdGroup.addUser("0000015")
		universityIdGroup.excludeUser("0000009")
		
		val userIdGroup = UserGroup.ofUsercodes
		userIdGroup.userLookup = userLookup
		userIdGroup.addUser("student1")
		userIdGroup.addUser("manual5")
		userIdGroup.addUser("manual10")
		userIdGroup.excludeUser("manual4")
	}

	@Test def getEnrolledAssignments { transactional { tx =>
		new AssignmentMembershipFixture() {
			val ams = assignmentMembershipService
	
			withUser("manual1", "0000006") { ams.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment1).toSet) }
			withUser("manual2", "0000007") { ams.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment1, assignment2).toSet) }
			withUser("manual3", "0000008") { ams.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment2).toSet) }
			withUser("manual4", "0000009") { ams.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment2).toSet) }
	
			withUser("student1", "0000001") { ams.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment1, assignment2).toSet) }
			withUser("student2", "0000002") { ams.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment2).toSet) }
			withUser("student3", "0000003") { ams.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq().toSet) }
			withUser("student4", "0000004") { ams.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq().toSet) }
			withUser("student5", "0000005") { ams.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment2).toSet) }
		}
	}}

	@Test def getAssignmentWhereMarker = transactional { tx =>
		val department = new Department
		department.code = "in"

		session.save(department)

		val workflow1 = new StudentsChooseMarkerWorkflow

		workflow1.name = "mw1"
		workflow1.department = department

		val workflow2 = new SeenSecondMarkingWorkflow
		workflow2.name = "mw2"
		workflow2.department = department

		workflow1.firstMarkers.addUser("cuscav")
		workflow1.firstMarkers.addUser("cusebr")
		workflow1.firstMarkers.addUser("cuscao")

		workflow2.firstMarkers.addUser("cuscav")
		workflow2.firstMarkers.addUser("curef")
		workflow2.secondMarkers.addUser("cusfal")
		workflow2.secondMarkers.addUser("cusebr")

		val assignment1 = new Assignment
		val assignment2 = new Assignment
		val assignment3 = new Assignment
		assignment3.markDeleted

		assignment1.markingWorkflow = workflow1
		assignment2.markingWorkflow = workflow2
		assignment3.markingWorkflow = workflow1

		session.save(workflow1)
		session.save(workflow2)

		assignmentService.save(assignment1)
		assignmentService.save(assignment2)
		assignmentService.save(assignment3)

		withUser("cuscav") { assignmentService.getAssignmentWhereMarker(currentUser.apparentUser).toSet should be (Seq(assignment1, assignment2).toSet) }
		withUser("cusebr") { assignmentService.getAssignmentWhereMarker(currentUser.apparentUser).toSet should be (Seq(assignment1, assignment2).toSet) }
		withUser("cuscao") { assignmentService.getAssignmentWhereMarker(currentUser.apparentUser).toSet should be (Seq(assignment1).toSet) }
		withUser("curef") { assignmentService.getAssignmentWhereMarker(currentUser.apparentUser).toSet should be (Seq(assignment2).toSet) }
		withUser("cusfal") { assignmentService.getAssignmentWhereMarker(currentUser.apparentUser).toSet should be (Seq(assignment2).toSet) }
		withUser("cusmab") { assignmentService.getAssignmentWhereMarker(currentUser.apparentUser).toSet should be (Seq().toSet) }
	}
	
	@Test def determineMembership() { transactional { tx =>
		new AssignmentMembershipFixture() {
			// Assignment1:
			// INC: manual1/0000006, manual2/0000007, manual3/0000008 (also excluded, so excluded takes priority)
			// EXC: student2/0000002, student3/0000003, manual3/0000008
			// Ass Groups: ag1 (0000001, 0000002), ag3 (0000001, 0000002, 0000003, 0000004, 0000005)
			// Actual membership: manual1/0000006, manual2/0000007, 0000001, 0000004, 0000005
			
			assignmentMembershipService.determineMembershipUsers(assignment1).map { _.getWarwickId }.toSet should be (Set(
					"0000001", "0000004", "0000005", "0000006", "0000007"
			))
			
			// Assignment2:
			// INC: manual2/0000007, manual3/0000008, manual4/0000009
			// EXC: student4/0000004, student3/0000003
			// Ass Groups: ag2 (0000002, 0000003)
			// Actual membership: manual2/0000007, manual3/0000008, manual4/0000009, 0000002
			
			assignmentMembershipService.determineMembershipUsers(assignment2).map { _.getWarwickId }.toSet should be (Set(
					"0000007", "0000008", "0000009", "0000002"
			))
			
			assignmentMembershipService.determineMembershipUsers(Seq(upstreamAg1), None).map { _.getWarwickId }.toSet should be (Set(
					"0000001", "0000002"
			))
			assignmentMembershipService.determineMembershipUsers(Seq(upstreamAg2), None).map { _.getWarwickId }.toSet should be (Set(
					"0000002", "0000003"
			))
			assignmentMembershipService.determineMembershipUsers(Seq(upstreamAg3), None).map { _.getWarwickId }.toSet should be (Set(
					"0000001", "0000002", "0000003", "0000004", "0000005"
			))
			assignmentMembershipService.determineMembershipUsers(Seq(upstreamAg1, upstreamAg2, upstreamAg3), None).map { _.getWarwickId }.toSet should be (Set(
					"0000001", "0000002", "0000003", "0000004", "0000005"
			))
			
			// UniversityID Group: 0000001, 0000010, 0000015
			
			assignmentMembershipService.determineMembershipUsers(Seq(upstreamAg1, upstreamAg2, upstreamAg3), Some(universityIdGroup)).map { _.getWarwickId }.toSet should be (Set(
					"0000001", "0000002", "0000003", "0000004", "0000005", "0000010", "0000015"
			))
			
			// UserID Group: student1/0000001, manual5/0000010, manual10/0000015
			
			assignmentMembershipService.determineMembershipUsers(Seq(upstreamAg1, upstreamAg2, upstreamAg3), Some(userIdGroup)).map { _.getWarwickId }.toSet should be (Set(
					"0000001", "0000002", "0000003", "0000004", "0000005", "0000010", "0000015"
			))
		}
	}}
	
	@Test def countMembershipWithUniversityIdGroup() { transactional { tx =>
		new AssignmentMembershipFixture() {		
			assignmentMembershipService.countMembershipWithUniversityIdGroup(Seq(upstreamAg1), None) should be (2)
			assignmentMembershipService.countMembershipWithUniversityIdGroup(Seq(upstreamAg2), None) should be (2)
			assignmentMembershipService.countMembershipWithUniversityIdGroup(Seq(upstreamAg3), None) should be (5)
			assignmentMembershipService.countMembershipWithUniversityIdGroup(Seq(upstreamAg1, upstreamAg2, upstreamAg3), None) should be (5)
			
			assignmentMembershipService.countMembershipWithUniversityIdGroup(Seq(upstreamAg1, upstreamAg2, upstreamAg3), Some(universityIdGroup)) should be (7)
			
			// UserID Group should fall back to using the other strategy, but get the same result
			assignmentMembershipService.countMembershipWithUniversityIdGroup(Seq(upstreamAg1, upstreamAg2, upstreamAg3), Some(userIdGroup)) should be (7)
		}
	}}
}
