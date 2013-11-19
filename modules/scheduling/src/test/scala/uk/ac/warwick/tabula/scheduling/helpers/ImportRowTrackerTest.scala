package uk.ac.warwick.tabula.scheduling.helpers

import scala.collection.mutable.HashSet
import uk.ac.warwick.tabula.{Fixtures, PersistenceTestBase}
import uk.ac.warwick.tabula.data.model.StudentCourseYearKey
import uk.ac.warwick.tabula.data.MemberDaoImpl
import uk.ac.warwick.tabula.data.StudentCourseDetailsDao
import uk.ac.warwick.tabula.data.StudentCourseDetailsDaoImpl
import uk.ac.warwick.tabula.data.StudentCourseYearDetailsDaoImpl

class ImportRowTrackerTest extends PersistenceTestBase {


	@Test
	def importRowTrackerTest  = transactional { tx =>
		val memberDao = new MemberDaoImpl
		memberDao.sessionFactory = sessionFactory

		val scdDao = new StudentCourseDetailsDaoImpl
		scdDao.sessionFactory = sessionFactory

		val scydDao = new StudentCourseYearDetailsDaoImpl
		scydDao.sessionFactory = sessionFactory

		val stu1 = Fixtures.student("1000001")
		val stu2 = Fixtures.student("1000002")
		val stu3 = Fixtures.student("1000003")

		session.save(stu1)
		session.save(stu2)
		session.save(stu3)

		session.flush
		session.clear

		val scyd = stu1.mostSignificantCourse.latestStudentCourseYearDetails
		val key = new StudentCourseYearKey(scyd.studentCourseDetails.scjCode, scyd.sceSequenceNumber)
		val scydId2 = stu2.mostSignificantCourse.latestStudentCourseYearDetails.id
		val scydId3 = stu3.mostSignificantCourse.latestStudentCourseYearDetails.id

		var tracker = new ImportRowTracker
		tracker.memberDao = memberDao
		tracker.studentCourseDetailsDao = scdDao
		tracker.studentCourseYearDetailsDao = scydDao

		// lets make out we've only see the first student
		tracker.universityIdsSeen = HashSet(stu1.universityId)
		tracker.scjCodesSeen = HashSet(stu1.mostSignificantCourse.scjCode)
		tracker.studentCourseYearDetailsSeen = HashSet(key)

		tracker.newStaleUniversityIds.toSet should be (HashSet("1000002", "1000003"))
		tracker.newStaleScjCodes.toSet should be (HashSet("1000002/1", "1000003/1"))
		tracker.newStaleScydIds.toSet should be (HashSet(scydId2, scydId3))
	}
}
