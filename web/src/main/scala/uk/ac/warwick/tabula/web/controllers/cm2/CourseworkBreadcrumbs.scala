package uk.ac.warwick.tabula.web.controllers.cm2

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.web.BreadCrumb

trait CourseworkBreadcrumbs {
	val Breadcrumbs = CourseworkBreadcrumbs
}

object CourseworkBreadcrumbs {
	def department(department: model.Department, academicYear: Option[AcademicYear], active: Boolean = false): Seq[BreadCrumb] =
		Seq(Department(department, academicYear, active))

	def module(module: model.Module, academicYear: AcademicYear, active: Boolean = false): Seq[BreadCrumb] =
		department(module.adminDepartment, Some(academicYear)) ++ Seq(Year(module.adminDepartment, academicYear), Module(module, academicYear, active))

	def assignment(assignment: model.Assignment, active: Boolean = false): Seq[BreadCrumb] =
		module(assignment.module, assignment.academicYear) :+ Assignment(assignment, active)

	private[CourseworkBreadcrumbs] case class Standard(title: String, url: Option[String], override val tooltip: String) extends BreadCrumb

	private[CourseworkBreadcrumbs] case class Department(dept: model.Department, academicYear: Option[AcademicYear], override val active: Boolean) extends BreadCrumb {
		val title: String = dept.name
		val url: Option[String] = academicYear match {
			case Some(year) => Some(Routes.admin.department(dept, year))
			case _ => Some(Routes.admin.department(dept))
		}
	}

	private[CourseworkBreadcrumbs] case class Year(dept: model.Department, academicYear: AcademicYear) extends BreadCrumb {
		val title: String = academicYear.toString
		val url: Option[String] = Some(Routes.admin.department(dept, academicYear))
	}

	private[CourseworkBreadcrumbs] case class Module(module: model.Module, academicYear: AcademicYear, override val active: Boolean = false) extends BreadCrumb {
		val title: String = s"${module.code.toUpperCase} ${module.name}"
		val url: Option[String] = Some(Routes.admin.moduleWithinDepartment(module, academicYear))
	}

	private[CourseworkBreadcrumbs] case class Assignment(assignment: model.Assignment, override val active: Boolean = false) extends BreadCrumb {
		val title: String = assignment.name
		val url: Option[String] = Some(Routes.admin.assignment.submissionsandfeedback(assignment))
	}
}