package uk.ac.warwick.tabula.groups.web

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEventOccurrence.WeekNumber
import uk.ac.warwick.tabula.data.model.{Module, Department}
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, DepartmentSmallGroupSet, SmallGroupSet}
import uk.ac.warwick.tabula.web.RoutesUtils

/**
 * Generates URLs to various locations, to reduce the number of places where URLs
 * are hardcoded and repeated.
 *
 * For methods called "apply", you can leave out the "apply" and treat the object like a function.
 */
object Routes {
	import RoutesUtils._
	private val context = "/groups"
	def home = context + "/"
	def homeForYear(academicYear: AcademicYear) = context + "/%s" format encoded(academicYear.startYear.toString)

	object tutor {
		def mygroups = context + "/tutor"
		def mygroupsForYear(academicYear: AcademicYear) = context + "/tutor/%s" format encoded(academicYear.startYear.toString)
		def registerForWeek (event: SmallGroupEvent, week: WeekNumber): String = context + "/event/%s/register?week=%s" format (encoded(event.id), encoded(week.toString))
	}

	object admin {
		def apply(department: Department, year: AcademicYear): String = context + "/admin/department/%s/%s" format (encoded(department.code), year.startYear.toString)

		def module(module: Module, year: AcademicYear): String = apply(module.adminDepartment, year) + s"?moduleFilters=Module(${module.code})"

		def release(department: Department, year: AcademicYear) = context + s"/admin/department/${encoded(department.code)}/${year.startYear.toString}/groups/release"
		def selfsignup(department: Department, year: AcademicYear, action: String) =
			context + "/admin/department/%s/%s/groups/selfsignup/%s".format(encoded(department.code), encoded(year.startYear.toString), encoded(action))

		def create(module: Module) = context + "/admin/module/%s/groups/new" format encoded(module.code)
		def create(set: SmallGroupSet) = context + "/admin/module/%s/groups/new/%s" format (encoded(set.module.code), encoded(set.id))
		def createAddStudents(set: SmallGroupSet) = context + "/admin/module/%s/groups/new/%s/students" format (encoded(set.module.code), encoded(set.id))
		def createAddGroups(set: SmallGroupSet) = context + "/admin/module/%s/groups/new/%s/groups" format (encoded(set.module.code), encoded(set.id))
		def createAddEvents(set: SmallGroupSet) = context + "/admin/module/%s/groups/new/%s/events" format (encoded(set.module.code), encoded(set.id))
		def createAllocate(set: SmallGroupSet) = context + "/admin/module/%s/groups/new/%s/allocate" format (encoded(set.module.code), encoded(set.id))
		def createEditEvent(event: SmallGroupEvent) = context + "/admin/module/%s/groups/new/%s/events/%s/edit/%s" format (encoded(event.group.groupSet.module.code), encoded(event.group.groupSet.id), encoded(event.group.id), encoded(event.id))
		def edit(set: SmallGroupSet) = context + "/admin/module/%s/groups/edit/%s" format (encoded(set.module.code), encoded(set.id))
		def editAddStudents(set: SmallGroupSet) = context + "/admin/module/%s/groups/edit/%s/students" format (encoded(set.module.code), encoded(set.id))
		def editAddGroups(set: SmallGroupSet) = context + "/admin/module/%s/groups/edit/%s/groups" format (encoded(set.module.code), encoded(set.id))
		def editAddEvents(set: SmallGroupSet) = context + "/admin/module/%s/groups/edit/%s/events" format (encoded(set.module.code), encoded(set.id))
		def editAllocate(set: SmallGroupSet) = context + "/admin/module/%s/groups/edit/%s/allocate" format (encoded(set.module.code), encoded(set.id))
		def editEditEvent(event: SmallGroupEvent) = context + "/admin/module/%s/groups/edit/%s/events/%s/edit/%s" format (encoded(event.group.groupSet.module.code), encoded(event.group.groupSet.id), encoded(event.group.id), encoded(event.id))

		def copy(department: Department) = context + s"/admin/department/${encoded(department.code)}/groups/copy"
		def copy(module: Module) = context + s"/admin/module/${encoded(module.code)}/groups/copy"

		def departmentAttendance(department: Department, academicYear: AcademicYear) = context + s"/admin/department/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/attendance"
		def moduleAttendance(module: Module, academicYear: AcademicYear) = context + s"/admin/module/${encoded(module.code)}/${encoded(academicYear.startYear.toString)}/attendance"

		def registers(department: Department, academicYear: AcademicYear) = context + s"/admin/department/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/registers"

		def importSpreadsheet(department: Department, academicYear: AcademicYear) = context + s"/admin/department/${encoded(department.code)}/${encoded(academicYear.startYear.toString)}/import-spreadsheet"

		object reusable {
			def apply(department: Department, academicYear: AcademicYear) =
				context + "/admin/department/%s/%s/groups/reusable".format(encoded(department.code), encoded(academicYear.startYear.toString))
			def create(department: Department, academicYear: AcademicYear) =
				context + "/admin/department/%s/%s/groups/reusable/new".format(encoded(department.code), encoded(academicYear.startYear.toString))
			def create(set: DepartmentSmallGroupSet) =
				context + "/admin/department/%s/%s/groups/reusable/new/%s".format(encoded(set.department.code), encoded(set.academicYear.startYear.toString), encoded(set.id))
			def createAddStudents(set: DepartmentSmallGroupSet) =
				context + "/admin/department/%s/%s/groups/reusable/new/%s/students".format(encoded(set.department.code), encoded(set.academicYear.startYear.toString), encoded(set.id))
			def createAddGroups(set: DepartmentSmallGroupSet) =
				context + "/admin/department/%s/%s/groups/reusable/new/%s/groups".format(encoded(set.department.code), encoded(set.academicYear.startYear.toString), encoded(set.id))
			def createAllocate(set: DepartmentSmallGroupSet) =
				context + "/admin/department/%s/%s/groups/reusable/new/%s/allocate".format(encoded(set.department.code), encoded(set.academicYear.startYear.toString), encoded(set.id))
			def edit(set: DepartmentSmallGroupSet) =
				context + "/admin/department/%s/%s/groups/reusable/edit/%s".format(encoded(set.department.code), encoded(set.academicYear.startYear.toString), encoded(set.id))
			def editAddStudents(set: DepartmentSmallGroupSet) =
				context + "/admin/department/%s/%s/groups/reusable/edit/%s/students".format(encoded(set.department.code), encoded(set.academicYear.startYear.toString), encoded(set.id))
			def editAddGroups(set: DepartmentSmallGroupSet) =
				context + "/admin/department/%s/%s/groups/reusable/edit/%s/groups".format(encoded(set.department.code), encoded(set.academicYear.startYear.toString), encoded(set.id))
			def editAllocate(set: DepartmentSmallGroupSet) =
				context + "/admin/department/%s/%s/groups/reusable/edit/%s/allocate".format(encoded(set.department.code), encoded(set.academicYear.startYear.toString), encoded(set.id))
			def delete(set: DepartmentSmallGroupSet) =
				context + "/admin/department/%s/%s/groups/reusable/delete/%s".format(encoded(set.department.code), encoded(set.academicYear.startYear.toString), encoded(set.id))
		}
	}
}
