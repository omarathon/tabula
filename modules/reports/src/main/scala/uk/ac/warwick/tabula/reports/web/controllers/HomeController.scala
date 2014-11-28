package uk.ac.warwick.tabula.reports.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.commands.CurrentSITSAcademicYear
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.AutowiringModuleAndDepartmentServiceComponent
import collection.JavaConverters._

/**
 * Displays the Reports home screen.
 */
@Controller
@RequestMapping(Array("/"))
class HomeController extends ReportsController with AutowiringModuleAndDepartmentServiceComponent with CurrentSITSAcademicYear {

	@RequestMapping
	def home = {
		val deptPermissions = moduleAndDepartmentService.departmentsWithPermission(user, Permissions.Department.Reports)
		Mav("home",
			"departments" -> deptPermissions.flatMap(d => Seq(d) ++ d.children.asScala.toSeq.sortBy(_.name)),
			"academicYears" -> Seq(academicYear.previous, academicYear)
		)
	}

}