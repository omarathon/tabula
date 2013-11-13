package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.{AcademicYear, PersistenceTestBase, Fixtures}
import org.junit.Before
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpointState, MonitoringCheckpoint, MonitoringPointSet}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._

class MonitoringPointDaoTest extends PersistenceTestBase {
	val thisAcademicYear = AcademicYear(2013)

	val monitoringPointDao = new MonitoringPointDaoImpl
	val routeDao = new RouteDaoImpl
	val memberDao = new MemberDaoImpl

	val route1 = Fixtures.route("g553")
	val route2 = Fixtures.route("h553")
	val route3 = Fixtures.route("i553")

	val monitoringPoint1 = Fixtures.monitoringPoint("name1", 1, 1)
	val monitoringPoint2 = Fixtures.monitoringPoint("name1", 1, 1)
	val monitoringPoint3 = Fixtures.monitoringPoint("name1", 1, 2)

	val monitoringPointSet1 = new MonitoringPointSet
	monitoringPointSet1.academicYear = thisAcademicYear
	monitoringPointSet1.points = JArrayList(monitoringPoint1)
	monitoringPointSet1.route = route1
	monitoringPointSet1.year = 1
	monitoringPointSet1.createdDate = DateTime.now()

	monitoringPoint1.pointSet = monitoringPointSet1

	val monitoringPointSet2 = new MonitoringPointSet
	monitoringPointSet2.academicYear = thisAcademicYear
	monitoringPointSet2.route = route1
	monitoringPointSet2.year = 2
	monitoringPointSet2.createdDate = DateTime.now()

	val monitoringPointSet3 = new MonitoringPointSet
	monitoringPointSet3.academicYear = thisAcademicYear
	monitoringPointSet3.route = route2
	monitoringPointSet3.year = 1
	monitoringPointSet3.createdDate = DateTime.now()
	monitoringPointSet3.points = JArrayList(monitoringPoint2)

	monitoringPoint2.pointSet = monitoringPointSet3


	val monitoringPointSet4 = new MonitoringPointSet
	monitoringPointSet4.academicYear = thisAcademicYear
	monitoringPointSet4.route = route3
	monitoringPointSet4.year = null
	monitoringPointSet4.createdDate = DateTime.now()
	monitoringPointSet4.points = JArrayList(monitoringPoint3)

	monitoringPoint3.pointSet = monitoringPointSet4

	val monitoringPointSet5 = new MonitoringPointSet
	monitoringPointSet5.academicYear = thisAcademicYear.previous
	monitoringPointSet5.points = JArrayList(monitoringPoint1)
	monitoringPointSet5.route = route1
	monitoringPointSet5.year = 1
	monitoringPointSet5.createdDate = DateTime.now()

	@Before
	def setup() {
		monitoringPointDao.sessionFactory = sessionFactory
		routeDao.sessionFactory = sessionFactory
		memberDao.sessionFactory = sessionFactory
	}

	@Test def getCheckpointByMember() {
		transactional { tx =>
			val student1 = Fixtures.student("1234")
			student1.studentCourseDetails.get(0).route = route1
			val student2 = Fixtures.student("2345")
			student2.studentCourseDetails.get(0).route = route1

			routeDao.saveOrUpdate(route1)
			monitoringPointDao.saveOrUpdate(monitoringPointSet1)
			memberDao.saveOrUpdate(student1)
			memberDao.saveOrUpdate(student2)

			val checkpoint = new MonitoringCheckpoint
			checkpoint.point = monitoringPoint1
			checkpoint.studentCourseDetail = student1.studentCourseDetails.get(0)
			checkpoint.updatedBy = "foo"
			checkpoint.state = MonitoringCheckpointState.fromCode("attended")
			monitoringPointDao.saveOrUpdate(checkpoint)

			monitoringPointDao.getCheckpoint(monitoringPoint1, student1) should be (Option(checkpoint))
			monitoringPointDao.getCheckpoint(monitoringPoint1, student2) should be (None)

		}
	}

	@Test def getCheckpointByScjCode() {
		transactional { tx =>
			val student1 = Fixtures.student("1234")
			student1.studentCourseDetails.get(0).route = route1
			val student2 = Fixtures.student("2345")
			student2.studentCourseDetails.get(0).route = route1

			routeDao.saveOrUpdate(route1)
			monitoringPointDao.saveOrUpdate(monitoringPointSet1)
			memberDao.saveOrUpdate(student1)
			memberDao.saveOrUpdate(student2)

			val checkpoint = new MonitoringCheckpoint
			checkpoint.point = monitoringPoint1
			checkpoint.studentCourseDetail = student1.studentCourseDetails.get(0)
			checkpoint.updatedBy = "foo"
			checkpoint.state = MonitoringCheckpointState.fromCode("attended")
			monitoringPointDao.saveOrUpdate(checkpoint)

			monitoringPointDao.getCheckpoint(monitoringPoint1, student1) should be (Option(checkpoint))
			monitoringPointDao.getCheckpoint(monitoringPoint1, student2) should be (None)

		}
	}

	@Test def findPointSetsForStudents() {
		transactional { tx =>
			routeDao.saveOrUpdate(route1)
			routeDao.saveOrUpdate(route2)
			routeDao.saveOrUpdate(route3)

			monitoringPointDao.saveOrUpdate(monitoringPointSet1)
			monitoringPointDao.saveOrUpdate(monitoringPointSet2)
			monitoringPointDao.saveOrUpdate(monitoringPointSet3)
			monitoringPointDao.saveOrUpdate(monitoringPointSet4)
			monitoringPointDao.saveOrUpdate(monitoringPointSet5)

			val studentInRoute1Year1 = Fixtures.student("student1")
			studentInRoute1Year1.studentCourseDetails.get(0).route = route1
			studentInRoute1Year1.studentCourseDetails.get(0).attachStudentCourseYearDetails(
				Fixtures.studentCourseYearDetails(thisAcademicYear, null, 1, studentInRoute1Year1.studentCourseDetails.get(0))
			)
			memberDao.saveOrUpdate(studentInRoute1Year1)

			val studentInRoute1Year2 = Fixtures.student("student2")
			studentInRoute1Year2.studentCourseDetails.get(0).route = route1
			studentInRoute1Year2.studentCourseDetails.get(0).attachStudentCourseYearDetails(
				Fixtures.studentCourseYearDetails(thisAcademicYear, null, 2, studentInRoute1Year2.studentCourseDetails.get(0))
			)
			memberDao.saveOrUpdate(studentInRoute1Year2)

			val studentInRoute2Year1 = Fixtures.student("student3")
			studentInRoute2Year1.studentCourseDetails.get(0).route = route2
			studentInRoute2Year1.studentCourseDetails.get(0).attachStudentCourseYearDetails(
				Fixtures.studentCourseYearDetails(thisAcademicYear, null, 1, studentInRoute2Year1.studentCourseDetails.get(0))
			)
			memberDao.saveOrUpdate(studentInRoute2Year1)

			val studentInRoute3Year1 = Fixtures.student("student4")
			studentInRoute3Year1.studentCourseDetails.get(0).route = route3
			studentInRoute3Year1.studentCourseDetails.get(0).attachStudentCourseYearDetails(
				Fixtures.studentCourseYearDetails(thisAcademicYear, null, 1, studentInRoute3Year1.studentCourseDetails.get(0))
			)
			memberDao.saveOrUpdate(studentInRoute3Year1)

			val studentInDifferentAcademicYear = Fixtures.student("student5")
			studentInDifferentAcademicYear.studentCourseDetails.get(0).route = route1
			studentInDifferentAcademicYear.studentCourseDetails.get(0).attachStudentCourseYearDetails(
				Fixtures.studentCourseYearDetails(thisAcademicYear.previous, null, 1, studentInDifferentAcademicYear.studentCourseDetails.get(0))
			)
			memberDao.saveOrUpdate(studentInDifferentAcademicYear)

			val result = monitoringPointDao.findPointSetsForStudents(Seq(
				studentInRoute1Year1,
				studentInRoute1Year2,
				studentInRoute2Year1,
				studentInRoute3Year1,
				studentInDifferentAcademicYear
			), thisAcademicYear)

			result.size should be (4)
		}
	}

	@Test def findSimilarPointsForMembers() {
		transactional { tx =>
			routeDao.saveOrUpdate(route1)
			routeDao.saveOrUpdate(route2)
			routeDao.saveOrUpdate(route3)

			monitoringPointDao.saveOrUpdate(monitoringPointSet1)
			monitoringPointDao.saveOrUpdate(monitoringPointSet2)
			monitoringPointDao.saveOrUpdate(monitoringPointSet3)
			monitoringPointDao.saveOrUpdate(monitoringPointSet4)
			monitoringPointDao.saveOrUpdate(monitoringPointSet5)

			val studentInRoute1Year1 = Fixtures.student("student1")
			studentInRoute1Year1.studentCourseDetails.get(0).route = route1
			studentInRoute1Year1.studentCourseDetails.get(0).attachStudentCourseYearDetails(
				Fixtures.studentCourseYearDetails(thisAcademicYear, null, 1, studentInRoute1Year1.studentCourseDetails.get(0))
			)
			memberDao.saveOrUpdate(studentInRoute1Year1)

			val studentInRoute1Year2 = Fixtures.student("student2")
			studentInRoute1Year2.studentCourseDetails.get(0).route = route1
			studentInRoute1Year2.studentCourseDetails.get(0).attachStudentCourseYearDetails(
				Fixtures.studentCourseYearDetails(thisAcademicYear, null, 2, studentInRoute1Year2.studentCourseDetails.get(0))
			)
			memberDao.saveOrUpdate(studentInRoute1Year2)

			val studentInRoute2Year1 = Fixtures.student("student3")
			studentInRoute2Year1.studentCourseDetails.get(0).route = route2
			studentInRoute2Year1.studentCourseDetails.get(0).attachStudentCourseYearDetails(
				Fixtures.studentCourseYearDetails(thisAcademicYear, null, 1, studentInRoute2Year1.studentCourseDetails.get(0))
			)
			memberDao.saveOrUpdate(studentInRoute2Year1)

			val studentInRoute3Year1 = Fixtures.student("student4")
			studentInRoute3Year1.studentCourseDetails.get(0).route = route3
			studentInRoute3Year1.studentCourseDetails.get(0).attachStudentCourseYearDetails(
				Fixtures.studentCourseYearDetails(thisAcademicYear, null, 1, studentInRoute3Year1.studentCourseDetails.get(0))
			)
			memberDao.saveOrUpdate(studentInRoute3Year1)

			val studentInDifferentAcademicYear = Fixtures.student("student5")
			studentInDifferentAcademicYear.studentCourseDetails.get(0).route = route1
			studentInDifferentAcademicYear.studentCourseDetails.get(0).attachStudentCourseYearDetails(
				Fixtures.studentCourseYearDetails(thisAcademicYear.previous, null, 1, studentInDifferentAcademicYear.studentCourseDetails.get(0))
			)
			memberDao.saveOrUpdate(studentInDifferentAcademicYear)

			val result = monitoringPointDao.findSimilarPointsForMembers(monitoringPoint1, Seq(
				studentInRoute1Year1,
				studentInRoute1Year2,
				studentInRoute2Year1,
				studentInRoute3Year1,
				studentInDifferentAcademicYear
			))

			result.size should be (2)
		}
	}
}