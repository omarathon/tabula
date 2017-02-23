package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.BreadCrumb
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments.AssignmentBreadcrumbs.Assignment.AssignmentBreadcrumbIdentifier


abstract class AbstractAssignmentController extends CourseworkController {

	protected def breadcrumbsStaff(assignment: Assignment, activeIdentifier: AssignmentBreadcrumbIdentifier): Seq[BreadCrumb] = Seq(
		AssignmentBreadcrumbs.Assignment.AssignmentManagement(assignment).setActive(activeIdentifier)
	)

}
