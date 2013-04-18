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

class FeedbackDaoTest extends AppContextTestBase {

	lazy val dao = Wire[FeedbackDao]

	@Test def crud = transactional { tx =>
		val f1 = Fixtures.feedback("0205225")
		val f2 = Fixtures.feedback("0205225")
		val f3 = Fixtures.feedback("0205226")
		
		val ass1 = Fixtures.assignment("ass1")
		val ass2 = Fixtures.assignment("ass2")
		
		session.save(ass1)
		session.save(ass2)
		session.flush
		
		ass1.feedbacks.add(f1)
		f1.assignment = ass1
		ass1.feedbacks.add(f3)
		f3.assignment = ass1
		ass2.feedbacks.add(f2)
		f2.assignment = ass2
		
		session.saveOrUpdate(ass1)
		session.saveOrUpdate(ass2)
		session.saveOrUpdate(f1)
		session.saveOrUpdate(f2)
		session.saveOrUpdate(f3)
		session.flush
		
		val mf1 = Fixtures.markerFeedback(f1)
		val mf2 = Fixtures.markerFeedback(f2)
		
		f1.firstMarkerFeedback = mf1
		f2.firstMarkerFeedback = mf2
		
		session.saveOrUpdate(f1)
		session.saveOrUpdate(mf1)
		session.saveOrUpdate(f2)
		session.saveOrUpdate(mf2)
		session.flush
		
		val mf3 = Fixtures.markerFeedback(f3)
		
		f3.firstMarkerFeedback = mf3
		
		session.saveOrUpdate(f3)
		session.saveOrUpdate(mf3)
		session.flush
		session.clear
		
		dao.getFeedback(f1.id) should be (Some(f1))
		dao.getFeedback(f2.id) should be (Some(f2))
		dao.getFeedback(f3.id) should be (Some(f3))
		dao.getFeedback("blah") should be (None)
		
		dao.getMarkerFeedback(mf1.id) map { _.feedback } should be (Some(f1))
		dao.getMarkerFeedback(mf2.id) map { _.feedback } should be (Some(f2))
		dao.getMarkerFeedback(mf3.id) map { _.feedback } should be (Some(f3))
		dao.getMarkerFeedback("blah") should be (None)
		
		dao.getFeedbackByUniId(ass1, "0205225") should be (Some(f1))
		dao.getFeedbackByUniId(ass2, "0205225") should be (Some(f2))
		dao.getFeedbackByUniId(ass1, "0205226") should be (Some(f3))
		dao.getFeedbackByUniId(ass2, "0205226") should be (None)
		session.flush
		
		dao.delete(dao.getFeedback(f1.id).get)
		session.flush
		
		dao.getFeedbackByUniId(ass1, "0205225") should be (None)
		dao.getFeedback(f1.id) should be (None)
		dao.getMarkerFeedback(mf1.id) should be (None)
	}
	
}