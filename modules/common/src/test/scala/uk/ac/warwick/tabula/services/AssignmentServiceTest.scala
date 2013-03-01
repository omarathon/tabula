package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.AppContextTestBase
import org.junit.Test
import uk.ac.warwick.tabula.data.model.Assignment
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Module
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.Feedback
import uk.ac.warwick.tabula.data.model.UpstreamAssignment
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.MockUserLookup
import org.junit.Before
import uk.ac.warwick.tabula.data.model.UpstreamAssessmentGroup
import collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.forms.Extension
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.forms.AssessmentGroup
import uk.ac.warwick.tabula.data.model.MarkingWorkflow
import uk.ac.warwick.tabula.Fixtures

class AssignmentServiceTest extends AppContextTestBase {
	
	@Autowired var service:AssignmentServiceImpl =_
    @Autowired var modAndDeptService:ModuleAndDepartmentService =_
    var userLookup:MockUserLookup = _
    
    @Before def getUserLookup {
		// We can't just Autowire this because it has autowire-candidate="false"
		userLookup = beans.getBean("userLookupDelegate").asInstanceOf[MockUserLookup]
		userLookup.defaultFoundUser = true
	}
	
	@Transactional @Test def recentAssignment {
		val assignment = newDeepAssignment()
		val department = assignment.module.department

		session.save(department)
		session.save(assignment.module)
		service.save(assignment)

		service.recentAssignment(department).get should be (assignment)
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
		assignment.addDefaultFields
		service.save(assignment)
		
		assignment.fields.get(1)
		
		service.isFilterEnabled("notDeleted") should be (false)
		service.getAssignmentById(assignment.id) should be (Some(assignment))
		session.enableFilter("notDeleted")
		service.getAssignmentById(assignment.id) should be (None)
		
		service.getAssignmentByNameYearModule(assignment.name, assignment.academicYear, assignment.module) should be ('empty)
	}
	
	@Transactional @Test def findDuplicateAssignmentNames {
		val module = new Module
		session.save(module)
		
		service.getAssignmentByNameYearModule("Essay", new AcademicYear(2009), module) should be ('empty)
		
		val assignment = new Assignment
		assignment.name = "Essay"
		assignment.module = module
		assignment.academicYear = new AcademicYear(2009)
		service.save(assignment)
		session.flush()
		
		service.getAssignmentByNameYearModule("Essay", new AcademicYear(2009), module) should not be ()
		service.getAssignmentByNameYearModule("Essay", new AcademicYear(2008), module) should be ('empty)
		service.getAssignmentByNameYearModule("Blessay", new AcademicYear(2009), module) should be ('empty)
	}
	
	@Transactional @Test def getAssignmentsByNameTest {    
	    val compSciDept = modAndDeptService.getDepartmentByCode("cs")
	    compSciDept should be ('defined)
	    
	    compSciDept.foreach(dept => {    
	        service.getAssignmentsByName("Test", dept) should have size(2)
            service.getAssignmentsByName("Computing", dept) should have size(1)	        
	        service.getAssignmentsByName("Assignment", dept) should have size(3) 
            service.getAssignmentsByName("xxxx", dept) should have size(0)	        
	    })
    }

	/*
	 * getUsersForFeedback gets all the users associated with an assignment who:
	 *     1. have feedback associated with that assignment which has not been released
	 *     2. have a submission associated with that assignment which is not suspected plagiarised.
	 */
	@Transactional @Test def getUsersForFeedbackTest {
		val assignment = service.getAssignmentById("1");
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
			service.saveSubmission(submission)
			
			// now check one user who needs to get feedback for this assignment is returned
			val userPairs = service.getUsersForFeedback(assmt)
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
			val userPairs2 = service.getUsersForFeedback(assmt)
			userPairs2.size should be (0)

			// feedback was not released - expect 1
			feedback.released = false
			val userPairs3 = service.getUsersForFeedback(assmt)
			userPairs3.size should be (1)
			
			// the only person was suspected of plagiarism - expect 0
			submission.suspectPlagiarised = true
			val userPairs4 = service.getUsersForFeedback(assmt)
			userPairs4.size should be (0)					
		}
		
	}
	
	@Transactional @Test def updateUpstreamAssignment {
		val upstream = new UpstreamAssignment
		upstream.departmentCode = "ch"
		upstream.moduleCode = "ch101"
		upstream.sequence = "A01"
		upstream.assessmentGroup = "A"
		upstream.name = "Egg plants"
		
		service.save(upstream)
		
		val upstream2 = new UpstreamAssignment
        upstream2.departmentCode = "ch"
        upstream2.moduleCode = "ch101"
        upstream2.sequence = "A01"
        upstream2.assessmentGroup = "A"
        upstream2.name = "Greg's plants"
		
        service.save(upstream2)
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
		
		service.save(assignment1)
		service.save(assignment2)
		service.save(assignment3)
		
		session.save(myFeedback)
		session.save(otherFeedback)
		session.save(unreleasedFeedback)
		session.save(deletedFeedback)
		
		session.enableFilter("notDeleted")
		
		val assignments = service.getAssignmentsWithFeedback(ThisUser)
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
		
		service.save(assignment1)
		service.save(assignment2)
		service.save(assignment3)
		
		session.save(mySubmission)
		session.save(otherSubmission)
		session.save(deletedSubmission)
		
		session.enableFilter("notDeleted")
		
		val assignments = service.getAssignmentsWithSubmission(ThisUser)
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
		
		service.save(group)
		session.flush
		
		val ua = new UpstreamAssignment
		ua.departmentCode = "LA"
		ua.moduleCode = "LA155-10"
		ua.sequence = "A01"
		ua.assessmentGroup = "A"
		ua.name = "Egg plants"
		
		service.save(ua) should be (ua)
		
		service.getAssessmentGroups(ua, new AcademicYear(2010)) should be (Seq(group))
		service.getAssessmentGroups(ua, new AcademicYear(2011)) should be (Seq())
		service.getAssessmentGroups(new UpstreamAssignment, new AcademicYear(2010)) should be (Seq())
		
		session.clear
		
		val foundGroup = service.find(group)
		foundGroup should be ('defined)
		foundGroup.eq(group) should be (false)
		
		foundGroup.get.occurrence = "B"
			
		service.save(foundGroup.get)
		session.flush
		session.clear
		
		service.find(group) should be ('empty)
		service.find(foundGroup.get) should be ('defined)
	}
	
	@Test def upstreamAssignments = transactional { tx =>
		val ua1 = new UpstreamAssignment
		ua1.departmentCode = "CH"
		ua1.moduleCode = "CH101-10"
		ua1.sequence = "A01"
		ua1.assessmentGroup = "A"
		ua1.name = "Egg plants"
			
		val ua2 = new UpstreamAssignment
		ua2.departmentCode = "CH"
		ua2.moduleCode = "CH101-20"
		ua2.sequence = "A02"
		ua2.assessmentGroup = "A"
		ua2.name = "Egg plants"
			
		val ua3 = new UpstreamAssignment
		ua3.departmentCode = "LA"
		ua3.moduleCode = "LA101-10"
		ua3.sequence = "A01"
		ua3.assessmentGroup = "A"
		ua3.name = "Egg plants"
			
		val ua4 = new UpstreamAssignment
		ua4.departmentCode = "LA"
		ua4.moduleCode = "LA101-10"
		ua4.sequence = "A02"
		ua4.assessmentGroup = "A"
		ua4.name = "Egg plants NOT IN USE"
		
		service.save(ua1) should be (ua1)
		service.save(ua2) should be (ua2)
		service.save(ua3) should be (ua3)
		service.save(ua4) should be (ua4)
		
		session.flush
		
		service.getUpstreamAssignment(ua1.id) should be (Some(ua1))
		service.getUpstreamAssignment(ua4.id) should be (Some(ua4))
		service.getUpstreamAssignment("wibble") should be (None)
		
		service.getUpstreamAssignments(Fixtures.module("ch101")) should be (Seq(ua1, ua2))
		service.getUpstreamAssignments(Fixtures.module("la101")) should be (Seq(ua3))
		service.getUpstreamAssignments(Fixtures.module("cs101")) should be (Seq())
		
		service.getUpstreamAssignments(Fixtures.department("ch")) should be (Seq(ua1, ua2))
		service.getUpstreamAssignments(Fixtures.department("la")) should be (Seq(ua3))
		service.getUpstreamAssignments(Fixtures.department("cs")) should be (Seq())
	}
	
	@Test def assessmentGroups = transactional { tx =>
		val upstreamGroup = new UpstreamAssessmentGroup
		upstreamGroup.moduleCode = "ch101-10"
		upstreamGroup.occurrence = "A"
		upstreamGroup.assessmentGroup = "A"
		upstreamGroup.academicYear = new AcademicYear(2010)
		upstreamGroup.members.staticIncludeUsers.addAll(Seq("rob","kev","bib").asJava)
		
		service.save(upstreamGroup)
		
		val upstreamAssignment = new UpstreamAssignment
		upstreamAssignment.departmentCode = "ch"
		upstreamAssignment.moduleCode = "ch101-10"
		upstreamAssignment.sequence = "A01"
		upstreamAssignment.assessmentGroup = "A"
		upstreamAssignment.name = "Egg plants"
		
		service.save(upstreamAssignment) should be (upstreamAssignment)
		
		val assignment = newDeepAssignment("ch101")
		val department = assignment.module.department

		session.save(department)
		session.save(assignment.module)
		service.save(assignment)
		
		val group = new AssessmentGroup
		group.assignment = assignment
		group.upstreamAssignment = upstreamAssignment
		group.occurrence = "A"
			
		session.save(group)
		
		session.flush
		
		service.getAssessmentGroup(group.id) should be (Some(group))
		
		service.getAssessmentGroup(group.id) map { service.delete(_) }
		service.getAssessmentGroup(group.id) should be ('empty)
	}
	
	@Test def submissions = transactional { tx =>
		val assignment = newDeepAssignment()
		val department = assignment.module.department

		session.save(department)
		session.save(assignment.module)
		service.save(assignment)
		
		val submission = new Submission
		submission.universityId = "0070790"
		submission.userId = "abcdef"
		submission.suspectPlagiarised = false
		assignment.addSubmission(submission)
		service.saveSubmission(submission)
		
		session.flush
		session.clear
		
		service.getSubmission(submission.id) should be ('defined)
		service.getSubmission(submission.id).eq(submission) should be (false)
		
		service.getSubmissionByUniId(assignment, "0070790") should be ('defined)
		service.getSubmissionByUniId(assignment, "0070790").eq(submission) should be (false)
		
		service.getSubmissionByUniId(assignment, "0070790") map { service.delete(_) }
		
		session.flush
		session.clear
		
		service.getSubmissionByUniId(assignment, "0070790") should be ('empty)
	}
	
	@Test def extensions = transactional { tx =>
		val assignment = newDeepAssignment()
		val department = assignment.module.department

		session.save(department)
		session.save(assignment.module)
		service.save(assignment)
		
		val extension = new Extension
		extension.universityId = "0070790"
		extension.userId = "abcdef"
		extension.assignment = assignment
		assignment.extensions.add(extension)
		session.saveOrUpdate(extension)
		
		session.flush
		session.clear
		
		service.getExtensionById(extension.id) should be ('defined)
		service.getExtensionById(extension.id).eq(extension) should be (false)
		
		service.getExtensionById(extension.id) map { session.delete(_) }
		
		session.flush
		session.clear
		
		service.getExtensionById(extension.id) should be ('empty)
	}
	
	@Test def getEnrolledAssignments = transactional { tx =>
		val year = AcademicYear.guessByDate(DateTime.now)
		
		val assignment1 = newDeepAssignment("ch101")
		assignment1.academicYear = year
		
		val department1 = assignment1.module.department

		session.save(department1)
		session.save(assignment1.module)
		service.save(assignment1)
		
		val assignment2 = newDeepAssignment("ch101")
		assignment2.academicYear = year
		
		val department2 = assignment2.module.department

		session.save(department2)
		session.save(assignment2.module)
		service.save(assignment2)
		
		val up1 = new UpstreamAssignment
		up1.departmentCode = "ch"
		up1.moduleCode = "ch101"
		up1.sequence = "A01"
		up1.assessmentGroup = "A"
		up1.name = "Egg plants"
		
		val upstream1 = service.save(up1)
		
		val up2 = new UpstreamAssignment
        up2.departmentCode = "ch"
        up2.moduleCode = "ch101"
        up2.sequence = "A02"
        up2.assessmentGroup = "B"
        up2.name = "Greg's plants"
		
    val upstream2 = service.save(up2)
		
		val up3 = new UpstreamAssignment
        up3.departmentCode = "ch"
        up3.moduleCode = "ch101"
        up3.sequence = "A03"
        up3.assessmentGroup = "C"
        up3.name = "Steg's plants"
		
    val upstream3 = service.save(up3)
    
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
		
		service.save(upstreamAg1)
		service.save(upstreamAg2)
		service.save(upstreamAg3)
		
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
		ag1.upstreamAssignment = upstream1
		ag1.occurrence = "A"
		
		val ag2 = new AssessmentGroup
		ag2.assignment = assignment1
		ag2.upstreamAssignment = upstream2
		ag2.occurrence = "B"
		
		val ag3 = new AssessmentGroup
		ag3.assignment = assignment2
		ag3.upstreamAssignment = upstream3
		ag3.occurrence = "C"
		
		assignment1.assessmentGroups.add(ag1)
		assignment1.assessmentGroups.add(ag3)
		
		assignment2.assessmentGroups.add(ag2)
		
		service.save(assignment1)
		service.save(assignment2)
		
		session.flush
		
		withUser("manual1", "0000006") { service.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment1).toSet) }
		withUser("manual2", "0000007") { service.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment1, assignment2).toSet) }
		withUser("manual3", "0000008") { service.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment2).toSet) }
		withUser("manual4", "0000009") { service.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment2).toSet) }
		
		withUser("student1", "0000001") { service.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment1, assignment2).toSet) }
		withUser("student2", "0000002") { service.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment2).toSet) }
		withUser("student3", "0000003") { service.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq().toSet) }
		withUser("student4", "0000004") { service.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq().toSet) }
		withUser("student5", "0000005") { service.getEnrolledAssignments(currentUser.apparentUser).toSet should be (Seq(assignment2).toSet) }
	}
	
	@Test def getAssignmentWhereMarker = transactional { tx =>
		val department = new Department
		department.code = "in"
			
		session.save(department)
		
		val workflow1 = new MarkingWorkflow
		workflow1.name = "mw1"
		workflow1.department = department
		
		val workflow2 = new MarkingWorkflow
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
		
		service.save(assignment1)
		service.save(assignment2)
		service.save(assignment3)
		
		withUser("cuscav") { service.getAssignmentWhereMarker(currentUser.apparentUser).toSet should be (Seq(assignment1, assignment2).toSet) }
		withUser("cusebr") { service.getAssignmentWhereMarker(currentUser.apparentUser).toSet should be (Seq(assignment1, assignment2).toSet) }
		withUser("cuscao") { service.getAssignmentWhereMarker(currentUser.apparentUser).toSet should be (Seq(assignment1).toSet) }
		withUser("curef") { service.getAssignmentWhereMarker(currentUser.apparentUser).toSet should be (Seq(assignment2).toSet) }
		withUser("cusfal") { service.getAssignmentWhereMarker(currentUser.apparentUser).toSet should be (Seq(assignment2).toSet) }
		withUser("cusmab") { service.getAssignmentWhereMarker(currentUser.apparentUser).toSet should be (Seq().toSet) }
	}
}