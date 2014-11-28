package uk.ac.warwick.tabula.reports.web

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.RoutesUtils

object Routes {
	import RoutesUtils._
	private val context = "/reports"
	def home = context + "/"
	def departmentHome(department: Department) =
		context + "/%s" format encoded(department.code)
	def departmentAcademicYear(department: Department, academicYear: AcademicYear) =
		context + "/%s/%s" format(encoded(department.code), encoded(academicYear.startYear.toString))

	object Attendance {
		def home(department: Department, academicYear: AcademicYear) =
			context + "/%s/%s/attendance" format(encoded(department.code), encoded(academicYear.startYear.toString))
	}

	object SmallGroups {
		def home(department: Department, academicYear: AcademicYear) =
			context + "/%s/%s/groups" format(encoded(department.code), encoded(academicYear.startYear.toString))
		def unrecorded(department: Department, academicYear: AcademicYear) =
			context + "/%s/%s/groups/unrecorded" format(encoded(department.code), encoded(academicYear.startYear.toString))
		def missed(department: Department, academicYear: AcademicYear) =
			context + "/%s/%s/groups/missed" format(encoded(department.code), encoded(academicYear.startYear.toString))
	}

}
