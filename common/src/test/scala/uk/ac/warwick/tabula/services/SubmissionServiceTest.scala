package uk.ac.warwick.tabula.services

import org.joda.time.DateTime
import org.junit.Before
import org.springframework.transaction.annotation.Transactional
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model._

class SubmissionServiceTest extends PersistenceTestBase {

	val assessmentService = new AssessmentServiceImpl
	val submissionService = new SubmissionServiceImpl

  @Before def setup() {
		submissionService.sessionFactory = sessionFactory
	}

	@Transactional
	@Test def submissionsBetweenDates() {
		val universityId = "1234"

		val startDate = new DateTime(2018, 3, 1, 0, 0, 0)
		val endDate = new DateTime(2018, 3, 8, 0, 0, 0)

		val submissionBefore = new Submission
		submissionBefore._universityId = universityId
		submissionBefore.usercode = universityId
		submissionBefore.submittedDate = startDate.minusDays(1)

		val submissionOnStartDate = new Submission
		submissionOnStartDate._universityId = universityId
		submissionOnStartDate.usercode = universityId
		submissionOnStartDate.submittedDate = startDate

		val submissionInside = new Submission
		submissionInside._universityId = universityId
		submissionInside.usercode = universityId
		submissionInside.submittedDate = startDate.plusDays(1)

		val submissionOnEndDate = new Submission
		submissionOnEndDate._universityId = universityId
		submissionOnEndDate.usercode = universityId
		submissionOnEndDate.submittedDate = endDate

		val submissionAfter = new Submission
		submissionAfter._universityId = universityId
		submissionAfter.usercode = universityId
		submissionAfter.submittedDate = endDate.plusDays(1)

		val assignment1 = new Assignment
		val assignment2 = new Assignment
		val assignment3 = new Assignment
		val assignment4 = new Assignment
		val assignment5 = new Assignment
		assignment1.addSubmission(submissionBefore)
		assignment2.addSubmission(submissionOnStartDate)
		assignment3.addSubmission(submissionInside)
		assignment4.addSubmission(submissionOnEndDate)
		assignment5.addSubmission(submissionAfter)

		session.save(assignment1)
		session.save(assignment2)
		session.save(assignment3)
		session.save(assignment4)
		session.save(assignment5)

		session.save(submissionBefore)
		session.save(submissionOnStartDate)
		session.save(submissionInside)
		session.save(submissionOnEndDate)
		session.save(submissionAfter)

		val result = submissionService.getSubmissionsBetweenDates(universityId, startDate, endDate)
		result.size should be (3)
		result.contains(submissionOnStartDate) should be (true)
		result.contains(submissionInside) should be (true)
		result.contains(submissionOnEndDate) should be (true)

	}
}