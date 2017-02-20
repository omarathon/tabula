package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.BreadCrumb
import uk.ac.warwick.tabula.web.Breadcrumbs.Active

trait AssignmentBreadcrumbs {
	val Breadcrumbs = AssignmentBreadcrumbs
}

object AssignmentBreadcrumbs {

	object Assignment {

		sealed abstract class AssignmentBreadcrumbIdentifier(id: String)
		case object AssignmentManagementIdentifier extends AssignmentBreadcrumbIdentifier("assignment")

		abstract class AssignmentBreadcrumb extends BreadCrumb {
			def identifier: AssignmentBreadcrumbIdentifier
			def setActive(activeIdentifier: AssignmentBreadcrumbIdentifier): BreadCrumb = {
				if (this.identifier == activeIdentifier) {
					Active(this.title, this.url, this.tooltip)
				} else {
					this
				}
			}
		}


		case class AssignmentManagement(assignment: Assignment) extends AssignmentBreadcrumb {
			val identifier = AssignmentManagementIdentifier
			val title: String = "Assignment Management"
			val url = Some(Routes.admin.assignment.createAssignmentDetails(assignment.module))
		}

	}

}