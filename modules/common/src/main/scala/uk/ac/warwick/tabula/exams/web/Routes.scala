package uk.ac.warwick.tabula.exams.web

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	import uk.ac.warwick.tabula.web.RoutesUtils._
	private val context = "/exams"
	def home = context + "/"

	object admin {
		def department(department: Department, academicYear: AcademicYear) =
			context + "/admin/department/%s/%s" format(encoded(department.code), encoded(academicYear.startYear.toString))

		object module {
			def apply(module: Module, academicYear: AcademicYear) = department(module.adminDepartment, academicYear) + "#module-" + encoded(module.code)
		}

		object exam {
			def apply(exam: Exam) =
				context + "/exams/admin/module/%s/%s/exams/%s" format(
					encoded(exam.module.code),
					encoded(exam.academicYear.startYear.toString),
					encoded(exam.id)
				)
		}

	}

}
