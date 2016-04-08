package uk.ac.warwick.tabula.commands.attendance.view

import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.{Fixtures, Mockito, TestBase}

class FiltersCheckpointMapChangesTest extends TestBase with Mockito {

	val existingStudent = Fixtures.student("1234")
	val newStudent = Fixtures.student("2345")
	val existingPoint1 = Fixtures.attendanceMonitoringPoint(null)
	val existingPoint2 = Fixtures.attendanceMonitoringPoint(null)
	val existingPoint3 = Fixtures.attendanceMonitoringPoint(null)
	val existingPoint4 = Fixtures.attendanceMonitoringPoint(null)
	val newPoint = Fixtures.attendanceMonitoringPoint(null)

	@Test
	def checkChanges(): Unit  = {
		val checker = new FiltersCheckpointMapChanges{}
		val existingCheckpoints = Map(
			existingStudent -> Map(
				existingPoint1 -> AttendanceState.Attended,
				existingPoint2 -> AttendanceState.Attended,
				existingPoint3 -> null,
				existingPoint4 -> AttendanceState.Attended
			)
		)
		val newCheckpoints = Map(
			newStudent -> Map(
				existingPoint1 -> AttendanceState.Attended,
				existingPoint2 -> AttendanceState.Attended
			),
			existingStudent -> Map(
				newPoint -> AttendanceState.Attended,
				existingPoint1 -> AttendanceState.Attended, // Unchanged
				existingPoint2 -> AttendanceState.MissedAuthorised, // Changed from state to state
				existingPoint3 -> AttendanceState.MissedAuthorised // Changed from null to state
				// existingPoint4 changed from state to null
			)
		)
		val changedMap = checker.filterCheckpointMapForChanges(newCheckpoints, existingCheckpoints)
		val newStudentMap = changedMap(newStudent)
		newStudentMap.values.size should be (2)
		newStudentMap.values.forall(_ == AttendanceState.Attended) should be {true}
		val existingStudentMap = changedMap(existingStudent)
		existingStudentMap(newPoint) should be (AttendanceState.Attended)
		existingStudentMap.get(existingPoint1) should be (None) // Unchanged
		existingStudentMap(existingPoint2) should be (AttendanceState.MissedAuthorised)
		existingStudentMap(existingPoint3) should be (AttendanceState.MissedAuthorised)

	}

}
