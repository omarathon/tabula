package uk.ac.warwick.tabula.attendance.web

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringScheme}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.web.RoutesUtils

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	import RoutesUtils._
	private val context = "/attendance"
	def home: String = context + "/"

	object old {

		object department {
			def view(department: Department): String = context + "/%s" format encoded(department.code)

			def viewPoints(department: Department): String = context + "/view/%s/2013/points" format encoded(department.code)

			def viewStudents(department: Department): String = context + "/view/%s/2013/students" format encoded(department.code)

			def viewStudent(department: Department, student: StudentMember): String =
				context + "/view/%s/2013/students/%s" format(encoded(department.code), encoded(student.universityId))

			def viewAgents(department: Department, relationshipType: StudentRelationshipType): String =
				context + "/view/%s/2013/agents/%s" format(encoded(department.code), encoded(relationshipType.urlPart))

			def manage(department: Department): String = context + "/manage/%s/2013" format encoded(department.code)
		}

		object admin {
			def departmentPermissions(department: Department): String = context + "/admin/department/%s/permissions" format encoded(department.code)
		}

		object profile {
			def apply(): String = context + "/profile"

			def apply(student: StudentMember): String =
				context + "/profile/%s/2013" format encoded(student.universityId)
		}

		object agent {
			def view(relationshipType: StudentRelationshipType): String = context + "/agent/%s/2013" format encoded(relationshipType.urlPart)

			def student(student: StudentMember, relationshipType: StudentRelationshipType): String =
				context + "/agent/%s/2013/%s" format(encoded(relationshipType.urlPart), encoded(student.universityId))
		}

	}

	object Manage {
		def home: String = context + "/manage"
		def department(department: Department): String = context + "/manage/%s" format encoded(department.code)
		def departmentForYear(department: Department, academicYear: AcademicYear): String =
			context + "/manage/%s/%s" format(encoded(department.code), encoded(academicYear.startYear.toString))

		def addStudentsToScheme(scheme: AttendanceMonitoringScheme): String =
			context + "/manage/%s/%s/new/%s/students" format(
				encoded(scheme.department.code), encoded(scheme.academicYear.startYear.toString), encoded(scheme.id)
			)

		def addPointsToNewScheme(scheme: AttendanceMonitoringScheme): String =
			context + "/manage/%s/%s/new/%s/points" format(
				encoded(scheme.department.code), encoded(scheme.academicYear.startYear.toString), encoded(scheme.id)
			)

		def editScheme(scheme: AttendanceMonitoringScheme): String =
			context + "/manage/%s/%s/%s/edit" format(
				encoded(scheme.department.code), encoded(scheme.academicYear.startYear.toString), encoded(scheme.id)
		)

		def editSchemeStudents(scheme: AttendanceMonitoringScheme): String =
			context + "/manage/%s/%s/%s/edit/students" format(
				encoded(scheme.department.code), encoded(scheme.academicYear.startYear.toString), encoded(scheme.id)
		)

		def editSchemePoints(scheme: AttendanceMonitoringScheme): String =
			context + "/manage/%s/%s/%s/edit/points" format(
				encoded(scheme.department.code), encoded(scheme.academicYear.startYear.toString), encoded(scheme.id)
		)

		def editPoints(department: Department, academicYear: AcademicYear): String =
			context + "/manage/%s/%s/editpoints" format(
				encoded(department.code), encoded(academicYear.startYear.toString)
			)

		def addPointsToExistingSchemes(department: Department, academicYear: AcademicYear): String =
			context + "/manage/%s/%s/addpoints" format(
				encoded(department.code), encoded(academicYear.startYear.toString)
			)

	}

	object Note {
		def view(academicYear: AcademicYear, student: StudentMember, point: AttendanceMonitoringPoint): String =
			context + "/note/%s/%s/%s" format(encoded(academicYear.startYear.toString), encoded(student.universityId), encoded(point.id))

	}

	object View {
		def home: String = context + "/view"
		def department(department: Department): String = context + "/view/%s" format encoded(department.code)
		def departmentForYear(department: Department, academicYear: AcademicYear): String =
			context + "/view/%s/%s" format(encoded(department.code), encoded(academicYear.startYear.toString))
		def students(department: Department, academicYear: AcademicYear): String =
			context + "/view/%s/%s/students" format(encoded(department.code), encoded(academicYear.startYear.toString))
		def studentsUnrecorded(department: Department, academicYear: AcademicYear): String =
			context + "/view/%s/%s/students?hasBeenFiltered=true&otherCriteria=Unrecorded" format(encoded(department.code), encoded(academicYear.startYear.toString))
		def student(department: Department, academicYear: AcademicYear, student: StudentMember): String =
			context + "/view/%s/%s/students/%s" format(
				encoded(department.code),
				encoded(academicYear.startYear.toString),
				encoded(student.universityId)
			)
		def points(department: Department, academicYear: AcademicYear): String =
			context + "/view/%s/%s/points" format(encoded(department.code), encoded(academicYear.startYear.toString))
		def pointsUnrecorded(department: Department, academicYear: AcademicYear): String =
			context + "/view/%s/%s/points?hasBeenFiltered=true&otherCriteria=Unrecorded" format(encoded(department.code), encoded(academicYear.startYear.toString))
		def pointRecordUpload(department: Department, academicYear: AcademicYear, templatePoint: AttendanceMonitoringPoint, args: String = ""): String =
			context + "/view/%s/%s/points/%s/record/upload%s" format(encoded(department.code), encoded(academicYear.startYear.toString), encoded(templatePoint.id), if (args.length > 0) "?" + args else "")
		def agents(department: Department, academicYear: AcademicYear, relationshipType: StudentRelationshipType): String =
			context + "/view/%s/%s/agents/%s" format(
				encoded(department.code),
				encoded(academicYear.startYear.toString),
				encoded(relationshipType.urlPart)
			)
	}

	object Agent {
		def home: String = context + "/agent"
		def relationshipForYear(relationshipType: StudentRelationshipType, academicYear: AcademicYear): String =
			context + "/agent/%s/%s" format(encoded(relationshipType.urlPart), encoded(academicYear.startYear.toString))
		def student(relationshipType: StudentRelationshipType, academicYear: AcademicYear, student: StudentMember): String =
			context + "/agent/%s/%s/%s" format(encoded(relationshipType.urlPart), encoded(academicYear.startYear.toString), encoded(student.universityId))
		def pointRecordUpload(relationshipType: StudentRelationshipType, academicYear: AcademicYear, templatePoint: AttendanceMonitoringPoint): String =
			context + "/agent/%s/%s/point/%s/upload" format(encoded(relationshipType.urlPart), encoded(academicYear.startYear.toString), encoded(templatePoint.id))
	}

	object Profile {
		def home: String = context + "/profile"
		def years(student: StudentMember): String = context + "/profile/%s" format encoded(student.universityId)
		def profileForYear(student: StudentMember, academicYear: AcademicYear): String =
			context + "/profile/%s/%s" format(encoded(student.universityId), encoded(academicYear.startYear.toString))
	}
}
