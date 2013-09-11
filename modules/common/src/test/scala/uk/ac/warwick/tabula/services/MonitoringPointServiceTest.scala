package uk.ac.warwick.tabula.services

import uk.ac.warwick.tabula.data.{MonitoringPointDaoComponent, MonitoringPointDao}
import uk.ac.warwick.tabula.{TestBase, Fixtures, Mockito}
import uk.ac.warwick.tabula.data.model.{StudentMember, MemberUserType}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpoint, MonitoringPointSet}
import uk.ac.warwick.tabula.JavaImports.JArrayList

class MonitoringPointServiceTest extends TestBase with Mockito {
	trait ServiceTestSupport extends MonitoringPointDaoComponent {
		val monitoringPointDao = mock[MonitoringPointDao]
	}

	trait CheckpointFixture {
		val service = new AbstractMonitoringPointService with ServiceTestSupport
		val uniId1 = "1234"
		val member1 = Fixtures.member(MemberUserType.Student, uniId1).asInstanceOf[StudentMember]
		val uniId2 = "2345"
		val member2 = Fixtures.member(MemberUserType.Student, uniId2).asInstanceOf[StudentMember]
		val point1 = Fixtures.monitoringPoint("point1", defaultValue = false, 2)
		val point2 = Fixtures.monitoringPoint("point2", defaultValue = true, 4)
		val pointSet = new MonitoringPointSet
		pointSet.points = JArrayList(point1, point2)
		val trueCheckpoint = new MonitoringCheckpoint
		trueCheckpoint.checked = true
		val falseCheckpoint = new MonitoringCheckpoint
		falseCheckpoint.checked = false
	}

	@Test
	def checkedForWeekTestNoCheckpointsBeforePoints() {
		new CheckpointFixture {
			// With no checkpoint, before a point is valid there should be None (neither true or false)
			service.monitoringPointDao.getCheckpoint(point1, member1) returns None
			service.monitoringPointDao.getCheckpoint(point1, member2) returns None
			service.monitoringPointDao.getCheckpoint(point2, member1) returns None
			service.monitoringPointDao.getCheckpoint(point2, member2) returns None
			val noCheckpointsBeforePoints = service.getCheckedForWeek(Seq(member1, member2), pointSet, 1)
			noCheckpointsBeforePoints(member1).exists{
				case (_, Some(_)) => true
				case _ => false
			} should be (right = false)
			noCheckpointsBeforePoints(member2).exists{
				case (_, Some(_)) => true
				case _ => false
			} should be (right = false)
		}
	}

	@Test
	def checkedForWeekTestNoCheckpointsAfterPoints() {
		new CheckpointFixture {
			// With no checkpoint, after a point is valid, should return the point's default value
			service.monitoringPointDao.getCheckpoint(point1, member1) returns None
			service.monitoringPointDao.getCheckpoint(point1, member2) returns None
			service.monitoringPointDao.getCheckpoint(point2, member1) returns None
			service.monitoringPointDao.getCheckpoint(point2, member2) returns None
			val noCheckpointsAfterPoints = service.getCheckedForWeek(Seq(member1, member2), pointSet, 6)
			noCheckpointsAfterPoints(member1)(point1) should be (Option(point1.defaultValue))
			noCheckpointsAfterPoints(member2)(point1) should be (Option(point1.defaultValue))
			noCheckpointsAfterPoints(member1)(point2) should be (Option(point2.defaultValue))
			noCheckpointsAfterPoints(member2)(point2) should be (Option(point2.defaultValue))
		}
	}

	@Test
	def checkedForWeekTestWithCheckpoints() {
		new CheckpointFixture {
			// With checkpoints, should return the checkpoint's value regardless of the week
			service.monitoringPointDao.getCheckpoint(point1, member1) returns Option(trueCheckpoint)
			service.monitoringPointDao.getCheckpoint(point1, member2) returns Option(falseCheckpoint)
			service.monitoringPointDao.getCheckpoint(point2, member1) returns Option(trueCheckpoint)
			service.monitoringPointDao.getCheckpoint(point2, member2) returns Option(falseCheckpoint)

			val withCheckpointsBeforePoints = service.getCheckedForWeek(Seq(member1, member2), pointSet, 1)
			withCheckpointsBeforePoints(member1)(point1) should be (Option(trueCheckpoint.checked))
			withCheckpointsBeforePoints(member2)(point1) should be (Option(falseCheckpoint.checked))
			withCheckpointsBeforePoints(member1)(point2) should be (Option(trueCheckpoint.checked))
			withCheckpointsBeforePoints(member2)(point2) should be (Option(falseCheckpoint.checked))

			val withCheckpointsAfterPoints = service.getCheckedForWeek(Seq(member1, member2), pointSet, 6)
			withCheckpointsAfterPoints(member1)(point1) should be (Option(trueCheckpoint.checked))
			withCheckpointsAfterPoints(member2)(point1) should be (Option(falseCheckpoint.checked))
			withCheckpointsAfterPoints(member1)(point2) should be (Option(trueCheckpoint.checked))
			withCheckpointsAfterPoints(member2)(point2) should be (Option(falseCheckpoint.checked))
		}
	}
}
