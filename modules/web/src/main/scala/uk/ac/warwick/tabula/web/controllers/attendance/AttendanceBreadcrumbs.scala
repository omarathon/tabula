package uk.ac.warwick.tabula.web.controllers.attendance

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.data.model
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.web.BreadCrumb

trait AttendanceBreadcrumbs {
	val Breadcrumbs = AttendanceBreadcrumbs
}

object AttendanceBreadcrumbs {
	abstract class Abstract extends BreadCrumb
	case class Standard(title: String, url: Option[String], override val tooltip: String) extends Abstract

	object Old {

		/**
		 * Special case breadcrumb for the department view page.
		 */
		case class ViewDepartment(department: model.Department) extends Abstract {
			val title: String = department.name
			val url = Some(Routes.old.department.view(department))
		}

		/**
		 * Special case breadcrumb for the department view points page.
		 */
		case class ViewDepartmentPoints(department: model.Department) extends Abstract {
			val title = "Monitoring points"
			val url = Some(Routes.old.department.viewPoints(department))
		}

		/**
		 * Special case breadcrumb for the department view points page.
		 */
		case class ViewDepartmentStudents(department: model.Department) extends Abstract {
			val title = "Students"
			val url = Some(Routes.old.department.viewStudents(department))
		}

		/**
		 * Special case breadcrumb for the department view agents page.
		 */
		case class ViewDepartmentAgents(department: model.Department, relationshipType: StudentRelationshipType) extends Abstract {
			val title: String = relationshipType.agentRole.capitalize + "s"
			val url = Some(Routes.old.department.viewAgents(department, relationshipType))
		}

		/**
		 * Special case breadcrumb for the department admin page.
		 */
		case class ManagingDepartment(department: model.Department) extends Abstract {
			val title = "Manage monitoring schemes"
			val url = Some(Routes.old.department.manage(department))
		}

		/**
		 * Special case breadcrumb for agent relationship page.
		 */
		case class Agent(relationshipType: model.StudentRelationshipType) extends Abstract {
			val title: String = relationshipType.studentRole.capitalize + "s"
			val url = Some(Routes.old.agent.view(relationshipType))
		}

		/**
		 * Special case breadcrumb for agent student profile page.
		 */
		case class AgentStudent(student: model.StudentMember, relationshipType: model.StudentRelationshipType) extends Abstract {
			val title: String = student.fullName.getOrElse("")
			val url = Some(Routes.old.agent.student(student, relationshipType))
		}

	}

	object Manage {

		case object Home extends Abstract {
			val title = "Manage"
			val url = Some(Routes.Manage.home)
		}

		case class Department(department: model.Department) extends Abstract {
			val title: String = department.name
			val url = Some(Routes.Manage.department(department))
		}

		case class DepartmentForYear(department: model.Department, academicYear: AcademicYear) extends Abstract {
			val title: String = academicYear.startYear.toString
			val url = Some(Routes.Manage.departmentForYear(department, academicYear))
		}

		case class EditPoints(department: model.Department, academicYear: AcademicYear) extends Abstract {
			val title = "Edit points"
			val url = Some(Routes.Manage.editPoints(department, academicYear))
		}

	}

	object View {

		case object Home extends Abstract {
			val title = "View and record"
			val url = Some(Routes.View.home)
		}

		case class Department(department: model.Department) extends Abstract {
			val title: String = department.name
			val url = Some(Routes.View.department(department))
		}

		case class DepartmentForYear(department: model.Department, academicYear: AcademicYear) extends Abstract {
			val title: String = academicYear.startYear.toString
			val url = Some(Routes.View.departmentForYear(department, academicYear))
		}

		case class Students(department: model.Department, academicYear: AcademicYear) extends Abstract {
			val title = "Students"
			val url = Some(Routes.View.students(department, academicYear))
		}

		case class Student(department: model.Department, academicYear: AcademicYear, student: StudentMember) extends Abstract {
			val title: String = student.fullName.getOrElse("")
			val url = Some(Routes.View.student(department, academicYear, student))
		}

		case class Points(department: model.Department, academicYear: AcademicYear) extends Abstract {
			val title = "Points"
			val url = Some(Routes.View.points(department, academicYear))
		}

		case class Agents(department: model.Department, academicYear: AcademicYear, relationshipType: StudentRelationshipType) extends Abstract {
			val title: String = relationshipType.agentRole.capitalize + "s"
			val url = Some(Routes.View.agents(department, academicYear, relationshipType))
		}

	}

	object Agent {

		case class Relationship(relationshipType: StudentRelationshipType) extends Abstract {
			val title: String = relationshipType.studentRole.capitalize + "s"
			val url = Some(Routes.Agent.relationship(relationshipType))
		}
		case class RelationshipForYear(relationshipType: StudentRelationshipType, academicYear: AcademicYear) extends Abstract {
			val title: String = academicYear.startYear.toString
			val url = Some(Routes.Agent.relationshipForYear(relationshipType, academicYear))
		}
		case class Student(relationshipType: StudentRelationshipType, academicYear: AcademicYear, student: StudentMember) extends Abstract {
			val title: String = student.fullName.getOrElse("")
			val url = Some(Routes.Agent.student(relationshipType, academicYear, student))
		}

	}

	object Profile {
		case class Years(student: StudentMember, isStudent: Boolean) extends Abstract {
			val title: String = isStudent match {
				case true => "My Monitoring Points"
				case false => student.fullName.getOrElse("")
			}
			val url = Some(Routes.Profile.years(student))
		}
		case class ProfileForYear(student: StudentMember, academicYear: AcademicYear) extends Abstract {
			val title: String = academicYear.startYear.toString
			val url = Some(Routes.Profile.profileForYear(student, academicYear))
		}
	}
}