package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.AppContextTestBase

import org.junit.Test
import uk.ac.warwick.tabula.data.model.FileAttachment
import java.io.ByteArrayInputStream
import org.joda.time.DateTime
import javax.persistence.Entity
import org.hibernate.annotations.AccessType
import org.junit.Test
import org.junit.runner.RunWith
import uk.ac.warwick.spring.Wire
import org.springframework.stereotype.Repository
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.io.File
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.tabula.Fixtures

class MarkingWorkflowDaoTest extends AppContextTestBase {

	lazy val dao = Wire[MarkingWorkflowDao]

	@Test def crud = transactional { tx =>
		val dept = Fixtures.department("in")
		session.save(dept)
		session.flush
		
		val mw1 = Fixtures.markingWorkflow("mw1")
		mw1.department = dept
		
		val mw2 = Fixtures.markingWorkflow("mw2")
		mw2.department = dept
		
		val ass1 = Fixtures.assignment("ass1")
		val ass2 = Fixtures.assignment("ass2")
		val ass3 = Fixtures.assignment("ass3")
		
		session.save(ass1)
		session.save(ass2)
		session.save(mw1)
		session.save(mw2)
		session.flush
		
		dao.getAssignmentsUsingMarkingWorkflow(mw1) should be ('empty)
		dao.getAssignmentsUsingMarkingWorkflow(mw2) should be ('empty)
		
		ass1.markingWorkflow = mw1
		ass2.markingWorkflow = mw1
		
		session.save(ass1)
		session.save(ass2)
		
		dao.getAssignmentsUsingMarkingWorkflow(mw1).toSet should be (Seq(ass1, ass2).toSet)
		dao.getAssignmentsUsingMarkingWorkflow(mw2) should be ('empty)
		
		ass2.markingWorkflow = mw2
		
		session.save(ass2)
		
		dao.getAssignmentsUsingMarkingWorkflow(mw1) should be (Seq(ass1))
		dao.getAssignmentsUsingMarkingWorkflow(mw2) should be (Seq(ass2))
	}
	
}
