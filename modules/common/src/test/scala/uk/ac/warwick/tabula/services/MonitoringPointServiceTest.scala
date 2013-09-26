package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.{MonitoringPointDaoComponent, MonitoringPointDao}
import uk.ac.warwick.tabula.{TestBase, Fixtures, Mockito}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpointState, MonitoringPointSet}
import uk.ac.warwick.tabula.JavaImports.JArrayList

class MonitoringPointServiceTest extends TestBase with Mockito {
	trait ServiceTestSupport extends MonitoringPointDaoComponent {
		val monitoringPointDao = mock[MonitoringPointDao]
	}

	trait CheckpointFixture {
		val service = new AbstractMonitoringPointService with ServiceTestSupport
		val uniId1 = "1234"
		val member1 = Fixtures.student(uniId1)
		val uniId2 = "2345"
		val member2 = Fixtures.student(uniId2)
		val point1 = Fixtures.monitoringPoint("point1", 2, 2)
		val point2 = Fixtures.monitoringPoint("point2", 4, 4)
		val pointSet = new MonitoringPointSet
		pointSet.points = JArrayList(point1, point2)
		val passedCheckpoint = Fixtures.monitoringCheckpoint(point1, member1.mostSignificantCourseDetails.get, MonitoringCheckpointState.fromCode("attended"))
		val missedCheckpoint = Fixtures.monitoringCheckpoint(point2, member1.mostSignificantCourseDetails.get, MonitoringCheckpointState.fromCode("unauthorised"))
	}

	@Test
	def testGetChecked() {
		new CheckpointFixture {
			service.monitoringPointDao.getCheckpoint(point1, member1) returns Option(passedCheckpoint)
			service.monitoringPointDao.getCheckpoint(point2, member1) returns Option(missedCheckpoint)
			service.monitoringPointDao.getCheckpoint(point1, member2) returns None
			service.monitoringPointDao.getCheckpoint(point2, member2) returns None
			val result = service.getChecked(Seq(member1, member2), pointSet)
			result(member1).keys.size should be (2)
			result(member2).keys.size should be (2)
		}
	}

	@Test
	def testDeleteCheckpointExists() {
		new CheckpointFixture {
			service.monitoringPointDao.getCheckpoint(point1, member1.mostSignificantCourseDetails.get.scjCode) returns Option(passedCheckpoint)
			service.deleteCheckpoint(member1.mostSignificantCourseDetails.get.scjCode, point1)
			there was one (service.monitoringPointDao).deleteCheckpoint(passedCheckpoint)
			there was no (service.monitoringPointDao).deleteCheckpoint(missedCheckpoint)
		}
	}

	@Test
	def testDeleteCheckpointNone() {
		new CheckpointFixture {
			service.monitoringPointDao.getCheckpoint(point1, member2.mostSignificantCourseDetails.get.scjCode) returns None
			service.deleteCheckpoint(member2.mostSignificantCourseDetails.get.scjCode, point1)
			there was no (service.monitoringPointDao).deleteCheckpoint(passedCheckpoint)
			there was no (service.monitoringPointDao).deleteCheckpoint(missedCheckpoint)
		}
	}

}
