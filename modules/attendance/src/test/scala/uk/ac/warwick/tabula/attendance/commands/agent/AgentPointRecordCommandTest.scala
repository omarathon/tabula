package uk.ac.warwick.tabula.attendance.commands.agent

import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPointStyle, AttendanceMonitoringPoint, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.data.model.{MemberStudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.services.{AttendanceMonitoringService, AttendanceMonitoringServiceComponent, RelationshipService, RelationshipServiceComponent, TermService, TermServiceComponent}
import uk.ac.warwick.tabula.{AcademicYear, Fixtures, Mockito, TestBase}

class AgentPointRecordCommandTest extends TestBase with Mockito {

	trait Fixture {
		val thisRelationshipType = new StudentRelationshipType
		thisRelationshipType.id = "123"
		val student1 = Fixtures.student("2345")
		val student1rel = new MemberStudentRelationship
		student1rel.studentMember = student1
		val student2 = Fixtures.student("3456")
		val student2rel = new MemberStudentRelationship
		student2rel.studentMember = student2
		val scheme1 = new AttendanceMonitoringScheme
		scheme1.pointStyle = AttendanceMonitoringPointStyle.Week
		val scheme1point1 = Fixtures.attendanceMonitoringPoint(scheme1, "point1", 1, 2)
		val scheme1point2 = Fixtures.attendanceMonitoringPoint(scheme1, "point2", 1, 2)
		val scheme2 = new AttendanceMonitoringScheme
		scheme2.pointStyle = AttendanceMonitoringPointStyle.Week
		val scheme2point1 = Fixtures.attendanceMonitoringPoint(scheme2, "point1", 1, 2)
		val scheme2point2 = Fixtures.attendanceMonitoringPoint(scheme2, "point2", 1, 2)
	}

	trait StateTestSupport extends AttendanceMonitoringServiceComponent with TermServiceComponent with RelationshipServiceComponent {
		var relationshipType: StudentRelationshipType = null
		val academicYear = AcademicYear(2014)
		var templatePoint: AttendanceMonitoringPoint = null
		val user = null
		val member = Fixtures.staff("1234")
		val attendanceMonitoringService = smartMock[AttendanceMonitoringService]
		val termService = smartMock[TermService]
		val relationshipService = smartMock[RelationshipService]
	}

	trait StateFixture extends Fixture {
		val state = new AgentPointRecordCommandState with StateTestSupport
		state.relationshipType = thisRelationshipType
		state.relationshipService.listStudentRelationshipsWithMember(state.relationshipType, state.member) returns Seq(student1rel, student2rel)
		state.attendanceMonitoringService.listStudentsPoints(student1, None, state.academicYear) returns Seq(scheme1point1, scheme1point2, scheme2point1, scheme2point2)
		state.attendanceMonitoringService.listStudentsPoints(student2, None, state.academicYear) returns Seq(scheme2point1, scheme2point2)
	}

	@Test
	def studentPointMap() {
		new StateFixture {
			state.templatePoint = scheme1point1
			val result = state.studentPointMap
			result(student1) should be (Seq(scheme1point1, scheme2point1))
			result(student2) should be (Seq(scheme2point1))
		}
	}

}
