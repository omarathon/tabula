package uk.ac.warwick.tabula.commands.reports.profiles

import org.hibernate.criterion.Order
import org.hibernate.criterion.Order.asc
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.jobs.reports.ProfileExportCSVJob
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.jobs.{AutowiringJobServiceComponent, JobInstance, JobServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, AutowiringSecurityServiceComponent, ProfileServiceComponent, SecurityServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser, JavaImports}

import scala.collection.JavaConverters._


object ProfileExportCSVCommand {
	def apply(department: Department, academicYear: AcademicYear, user: CurrentUser) =
		new ProfileExportCSVCommandInternal(department, academicYear, user)
			with AutowiringJobServiceComponent
			with AutowiringProfileServiceComponent
			with AutowiringSecurityServiceComponent
			with ComposableCommand[JobInstance]
			//			with ProfileExportCSVValidation
			with Unaudited
			with ProfileExportCSVPermissions
			with ProfileExportCSVCommandState
}


class ProfileExportCSVCommandInternal(val department: Department, val academicYear: AcademicYear, val user: CurrentUser)
	extends CommandInternal[JobInstance] {

	self: JobServiceComponent with ProfileExportCSVCommandState with ProfileServiceComponent with SecurityServiceComponent with TaskBenchmarking =>

	override def applyInternal(): JobInstance = {
		//		val allStudents: Seq[AttendanceMonitoringStudentData] = profileService.findAllStudentDataByRestrictionsInAffiliatedDepartments(
		//			department = department,
		//			restrictions = buildRestrictions(academicYear),
		//			academicYear
		//		)

		val allStudents: Seq[AttendanceMonitoringStudentData] = if (searchSingle || searchMulti) {
			val members = {
				if (searchSingle) profileService.getAllMembersWithUserId(singleSearch)
				else profileService.getAllMembersWithUniversityIds(multiSearchEntries)
			}
			val students = members.flatMap {
				case student: StudentMember => Some(student)
				case _ => None
			}
			val (validStudents, noPermission) = students.partition(s => securityService.can(user, Permissions.Department.Reports, s))

			notFoundIds = multiSearchEntries.diff(students.map(_.universityId))
			noPermissionIds = noPermission.map(_.universityId)

			validStudents.map(s => {
				val scd = Option(s.mostSignificantCourse)
				AttendanceMonitoringStudentData(
					s.firstName,
					s.lastName,
					s.universityId,
					s.userId,
					null,
					null,
					scd.map(_.currentRoute.code).getOrElse(""),
					scd.map(_.currentRoute.name).getOrElse(""),
					scd.map(_.latestStudentCourseYearDetails.yearOfStudy.toString).getOrElse(""),
					scd.map(_.sprCode).getOrElse(""),
					scd.map(_.latestStudentCourseYearDetails).exists { scyd => Option(scyd.casUsed).contains(true) || Option(scyd.tier4Visa).contains(true) }
				)
			})
		} else if (hasBeenFiltered) {
			benchmarkTask("profileService.findAllStudentDataByRestrictionsInAffiliatedDepartments") {
				profileService.findAllStudentDataByRestrictionsInAffiliatedDepartments(
					department = department,
					restrictions = buildRestrictions(academicYear),
					academicYear
				)
			}
		} else {
			Seq()
		}

		jobService.add(Option(user), ProfileExportCSVJob(allStudents.map(_.universityId), academicYear))
	}

}

//trait ProfileExportCSVValidation extends SelfValidating {
//
//	self: ProfileExportCSVCommandState with ProfileServiceComponent =>
//
//	override def validate(errors: Errors) {
////		if (students.isEmpty) {
////			errors.rejectValue("students", "reports.profiles.export.noStudents")
////		} else {
////			val memberMap = profileService.getAllMembersWithUniversityIds(students.asScala).groupBy(_.universityId).mapValues(_.head)
////			val notMembers = students.asScala.filter(uniId => memberMap.get(uniId).isEmpty)
////			if (notMembers.nonEmpty) {
////				errors.rejectValue("students", "reports.profiles.export.notMembers", Array(notMembers.mkString(", ")), "")
////			}
////			val outsideDepartment = memberMap.values.toSeq.filterNot(member => member.affiliatedDepartments.flatMap(_.subDepartmentsContaining(member)).contains(department))
////			if (outsideDepartment.nonEmpty) {
////				errors.rejectValue("students", "reports.profiles.export.outsideDepartment", Array(outsideDepartment.map(m => s"${m.fullName.getOrElse("")} (${m.universityId})").mkString(", ")), "")
////			}
////		}
//	}
//
//}

trait ProfileExportCSVPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ProfileExportCSVCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.Reports, department)
	}

}

trait ProfileExportCSVCommandState extends FiltersStudents {
	def department: Department

	def academicYear: AcademicYear

	var studentsPerPage = FiltersStudents.DefaultStudentsPerPage
	val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm

	var noPermissionIds: Seq[String] = Seq()
	var notFoundIds: Seq[String] = Seq()
	// Bind variables

	var page = 1
	var sortOrder: JList[Order] = JArrayList()
	var hasBeenFiltered = false
	var searchSingle = false
	var searchMulti = false
	var singleSearch: String = _
	var multiSearch: String = _

	def multiSearchEntries: Seq[String] =
		if (multiSearch == null) Nil
		else multiSearch split "(\\s|[^A-Za-z\\d\\-_\\.])+" map (_.trim) filterNot (_.isEmpty)

	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	var courses: JList[Course] = JArrayList()
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var levelCodes: JList[String] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()
	var hallsOfResidence: JList[String] = JArrayList()
}