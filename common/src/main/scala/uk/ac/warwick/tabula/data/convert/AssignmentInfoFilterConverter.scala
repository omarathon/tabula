package uk.ac.warwick.tabula.data.convert

import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.commands.cm2.assignments.{AssignmentInfoFilter, AssignmentInfoFilters}
import uk.ac.warwick.tabula.data.model.MarkingMethod
import uk.ac.warwick.tabula.data.model.markingworkflow.MarkingWorkflowType
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.system.TwoWayConverter

import scala.util.Try

class AssignmentInfoFilterConverter extends TwoWayConverter[String, AssignmentInfoFilter] {

	@Autowired var moduleAndDepartmentService: ModuleAndDepartmentService = _

	override def convertRight(source: String): AssignmentInfoFilter = source match {
		case r"Module\(([^\)]+)${moduleCode}\)" =>
			AssignmentInfoFilters.Module(moduleAndDepartmentService.getModuleByCode(sanitise(moduleCode)).getOrElse {
				moduleAndDepartmentService.getModuleById(moduleCode).orNull
			})
		case _ => Try(MarkingMethod.fromCode(source)).map(AssignmentInfoFilters.WorkflowType.apply).orElse(
			Try(MarkingWorkflowType.fromCode(source)).map(AssignmentInfoFilters.CM2WorkflowType.apply)
		).getOrElse(AssignmentInfoFilters.of(source))

	}

	override def convertLeft(source: AssignmentInfoFilter): String = Option(source).map {
		_.getName
	}.orNull

	private def sanitise(code: String): String = {
		if (code == null) throw new IllegalArgumentException
		else code.toLowerCase
	}

}
