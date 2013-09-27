package uk.ac.warwick.tabula.data

import uk.ac.warwick.tabula.{PersistenceTestBase, Fixtures}
import org.junit.Before
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpointState, MonitoringCheckpoint, MonitoringPointSet}
import org.joda.time.DateTime
import uk.ac.warwick.tabula.JavaImports._

class MonitoringPointDaoTest extends PersistenceTestBase {

	val monitoringPointDao = new MonitoringPointDaoImpl
	val routeDao = new RouteDaoImpl
	val memberDao = new MemberDaoImpl

	val route = Fixtures.route("g503")

	val monitoringPoint = Fixtures.monitoringPoint()

	val monitoringPointSet = new MonitoringPointSet
	monitoringPointSet.points = JArrayList(monitoringPoint)
	monitoringPointSet.route = route
	monitoringPointSet.createdDate = DateTime.now()

	monitoringPoint.pointSet = monitoringPointSet

	@Before
	def setup() {
		monitoringPointDao.sessionFactory = sessionFactory
		routeDao.sessionFactory = sessionFactory
		memberDao.sessionFactory = sessionFactory
	}

	@Test def getCheckpointByMember {
		transactional { tx =>
			val student1 = Fixtures.student("1234")
			student1.studentCourseDetails.get(0).route = route
			val student2 = Fixtures.student("2345")
			student2.studentCourseDetails.get(0).route = route

			routeDao.saveOrUpdate(route)
			monitoringPointDao.saveOrUpdate(monitoringPointSet)
			memberDao.saveOrUpdate(student1)
			memberDao.saveOrUpdate(student2)

			val checkpoint = new MonitoringCheckpoint
			checkpoint.point = monitoringPoint
			checkpoint.studentCourseDetail = student1.studentCourseDetails.get(0)
			checkpoint.updatedBy = "foo"
			checkpoint.state = MonitoringCheckpointState.fromCode("attended")
			monitoringPointDao.saveOrUpdate(checkpoint)

			monitoringPointDao.getCheckpoint(monitoringPoint, student1) should be (Option(checkpoint))
			monitoringPointDao.getCheckpoint(monitoringPoint, student2) should be (None)

		}
	}

	@Test def getCheckpointByScjCode {
		transactional { tx =>
			val student1 = Fixtures.student("1234")
			student1.studentCourseDetails.get(0).route = route
			val student2 = Fixtures.student("2345")
			student2.studentCourseDetails.get(0).route = route

			routeDao.saveOrUpdate(route)
			monitoringPointDao.saveOrUpdate(monitoringPointSet)
			memberDao.saveOrUpdate(student1)
			memberDao.saveOrUpdate(student2)

			val checkpoint = new MonitoringCheckpoint
			checkpoint.point = monitoringPoint
			checkpoint.studentCourseDetail = student1.studentCourseDetails.get(0)
			checkpoint.updatedBy = "foo"
			checkpoint.state = MonitoringCheckpointState.fromCode("attended")
			monitoringPointDao.saveOrUpdate(checkpoint)

			monitoringPointDao.getCheckpoint(monitoringPoint, student1.mostSignificantCourseDetails.get.scjCode) should be (Option(checkpoint))
			monitoringPointDao.getCheckpoint(monitoringPoint, student2.mostSignificantCourseDetails.get.scjCode) should be (None)

		}
	}

	@Test def getMonitoringPointSetWithoutYear() {
		transactional { tx =>
			routeDao.saveOrUpdate(route)
			session.save(monitoringPointSet)
			monitoringPointDao.findMonitoringPointSet(route, Some(1)) should be (None)
			monitoringPointDao.findMonitoringPointSet(route, None) should be (Some(monitoringPointSet))
		}
	}

	@Test def getMonitoringPointSetWithYear {
		transactional { tx =>
			routeDao.saveOrUpdate(route)
			monitoringPointSet.year = 2
			session.save(monitoringPointSet)
			monitoringPointDao.findMonitoringPointSet(route, Some(2)) should be (Some(monitoringPointSet))
			monitoringPointDao.findMonitoringPointSet(route, Some(1)) should be (None)
			monitoringPointDao.findMonitoringPointSet(route, None) should be (None)
		}
	}


}