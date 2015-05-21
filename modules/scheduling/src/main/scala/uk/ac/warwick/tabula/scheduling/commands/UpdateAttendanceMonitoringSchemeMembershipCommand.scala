package uk.ac.warwick.tabula.scheduling.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import uk.ac.warwick.tabula.{AutowiringFeaturesComponent, FeaturesComponent}
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data.model.{StudentMember, Module, SitsStatus, ModeOfAttendance, Route, CourseType}
import org.hibernate.criterion.Order._
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.helpers.Logging

object UpdateAttendanceMonitoringSchemeMembershipCommand {
	def apply() =
		new UpdateAttendanceMonitoringSchemeMembershipCommandInternal
			with ComposableCommand[Seq[AttendanceMonitoringScheme]]
			with AutowiringFeaturesComponent
			with AutowiringProfileServiceComponent
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringDeserializesFilterImpl
			with UpdateAttendanceMonitoringSchemeMembershipDescription
			with UpdateAttendanceMonitoringSchemeMembershipPermissions
			with UpdateAttendanceMonitoringSchemeMembershipCommandState
}

class UpdateAttendanceMonitoringSchemeMembershipCommandInternal extends CommandInternal[Seq[AttendanceMonitoringScheme]] with Logging {

	self: FeaturesComponent with AttendanceMonitoringServiceComponent with UpdateAttendanceMonitoringSchemeMembershipCommandState with TaskBenchmarking =>

	override def applyInternal() = {
		if (features.attendanceMonitoringAcademicYear2014) {

			val schemesToUpdate = transactional(readOnly = true) {
				attendanceMonitoringService.listSchemesForMembershipUpdate
			}

			logger.info(s"${schemesToUpdate.size} schemes need membership updating")

			val studentsToUpdate = schemesToUpdate.flatMap { scheme => benchmark(s"Update scheme ${scheme.id} in transaction") { transactional() {
				deserializeFilter(scheme.memberQuery)
				val staticStudentIds = benchmarkTask("profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments") {
					profileService.findAllUniversityIdsByRestrictionsInAffiliatedDepartments(
						department = scheme.department,
						restrictions = buildRestrictions(scheme.academicYear),
						orders = buildOrders()
					)
				}
				scheme.members.staticUserIds = staticStudentIds

				attendanceMonitoringService.saveOrUpdate(scheme)

				scheme.members.members.map((_, (scheme.department, scheme.academicYear)))

			}}
			}.groupBy(_._1).map { case (universityId, groupedStudentData) => universityId -> groupedStudentData.map(_._2).distinct}

			logger.info(s"Updating ${studentsToUpdate.size} student checkpoint totals")

			benchmark("updateCheckpointTotals") {
				val studentMembers = transactional(readOnly = true){
					profileService.getAllMembersWithUniversityIds(studentsToUpdate.keys.toSeq).flatMap {
						case student: StudentMember => Option(student)
						case _ => None
					}
				}

				studentMembers.foreach(student => benchmark(s"updateCheckpointTotal for ${student.universityId} in transaction") { transactional() {
					val studentDeptAndYears = studentsToUpdate(student.universityId)
					studentDeptAndYears.foreach { case (dept, academicYear) =>
						attendanceMonitoringService.updateCheckpointTotalsAsync(Seq(student), dept, academicYear)
					}
				}})
			}

			benchmark("resetCheckpointTotals") {
				schemesToUpdate.groupBy(s => (s.department, s.academicYear)).foreach{case((dept, academicYear), _) =>
					transactional() {
						attendanceMonitoringService.resetTotalsForStudentsNotInAScheme(dept, academicYear)
					}
				}
			}

			schemesToUpdate
		} else {
			Seq()
		}
	}

}

trait UpdateAttendanceMonitoringSchemeMembershipPermissions extends RequiresPermissionsChecking {

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.UpdateMembership)
	}

}

trait UpdateAttendanceMonitoringSchemeMembershipDescription extends Describable[Seq[AttendanceMonitoringScheme]] {

	override lazy val eventName = "UpdateAttendanceMonitoringSchemeMembership"

	override def describe(d: Description) {

	}
}

trait UpdateAttendanceMonitoringSchemeMembershipCommandState extends FiltersStudents with DeserializesFilter {
	val department = null // Needs to be defined, but never actually used
	val defaultOrder = Seq(asc("lastName"), asc("firstName"))
	var sortOrder: JList[Order] = JArrayList() // Never used

	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()

}
