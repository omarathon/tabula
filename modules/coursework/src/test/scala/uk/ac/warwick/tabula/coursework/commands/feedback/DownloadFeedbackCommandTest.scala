package uk.ac.warwick.tabula.coursework.commands.feedback

import org.hibernate.annotations.AccessType
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.BindException
import javax.persistence.Entity
import javax.persistence.NamedQueries
import uk.ac.warwick.tabula.AppContextTestBase
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.data.model.Feedback


class DownloadFeedbackCommandTest extends AppContextTestBase {

	@Transactional
	@Test def applyCommand = withUser("custard") {
		val feedback = session.load(classOf[Feedback], MyFixtures().feedback.id).asInstanceOf[Feedback]

		feedback.assignment.feedbacks.size should be (1)
		session.isDirty should be (false)

		// check that we can dirty and un-dirty it harmlessly
		feedback.assignment.fileExtensions = Seq(".doc")
		session.isDirty should be (true)
		feedback.assignment.fileExtensions = Seq()
		session.isDirty should be (false)

		val command = new DownloadFeedbackCommand(feedback.assignment.module, feedback.assignment, feedback)
		command.filename = "0123456-feedback.doc"

		val errors = new BindException(command, "command")
		withClue(errors) { errors.hasErrors should be (false) }
		command.apply should be (None)
		session.isDirty should be (false) // BECAUSE THIS IS A READ-OP
}

	case class MyFixtures() {
		val department = Fixtures.department(code="ls", name="Life Sciences")
		val module = Fixtures.module(code="ls101")
		val assignment = new Assignment
		val feedback = new Feedback("0123456")

		department.postLoad // force legacy settings
		module.department = department
		assignment.module = module
		assignment.addFeedback(feedback)

		session.save(department)
		session.save(module)
		session.save(assignment)
		session.flush
		session.clear
	}
}