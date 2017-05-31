package uk.ac.warwick.tabula.web.controllers.exams.exams

import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController

abstract class AbstractExamsHomeController extends ExamsController with TaskBenchmarking
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent {

	@Autowired var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]
	@Autowired var assessmentService: AssessmentService = Wire[AssessmentService]
	@Autowired var examMembershipService: AssessmentMembershipService = _
	@Autowired var feedbackService: FeedbackService = _

	@RequestMapping
	def examsHome(@RequestParam(required = false) marked: Integer, @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]): Mav = {
		val academicYear = activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))

		val ownedDepartments = benchmarkTask("Get owned departments") {
			moduleAndDepartmentService.departmentsWithPermission(user, Permissions.Module.ManageAssignments)
		}

		val ownedModules = benchmarkTask("Get owned modules") {
			moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.ManageAssignments)
		}

		val examsForMarking = benchmarkTask("Get exams for marking") {
			assessmentService.getExamsWhereMarker(user.apparentUser).filter(_.academicYear == academicYear).sortBy(_.module.code).map(exam => {
				val requiresMarking = examMembershipService.determineMembershipUsersWithOrderForMarker(exam, user.apparentUser)
				val feedbacks = feedbackService.getExamFeedbackMap(exam, requiresMarking.map(_._1)).values.toSeq.filter(_.latestMark.isDefined)
				exam -> (feedbacks, requiresMarking)
			})
		}.toMap

		Mav("exams/exams/home/view",
			"academicYear" -> academicYear,
			"currentAcademicYear" -> academicYear,
			"examsForMarking" -> examsForMarking,
			"ownedDepartments" -> ownedDepartments,
			"ownedModuleDepartments" -> ownedModules.map { _.adminDepartment },
			"marked" -> marked
		).secondCrumbs(academicYearBreadcrumbs(academicYear)(Routes.exams.Exams.home): _*)
	}
}

@Controller
@RequestMapping(Array("/exams/exams"))
class ExamsHomeController extends AbstractExamsHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] =
		retrieveActiveAcademicYear(None)

}

@Controller
@RequestMapping(Array("/exams/exams/{academicYear:\\d{4}}"))
class ExamsHomeForYearController extends AbstractExamsHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
		retrieveActiveAcademicYear(Option(academicYear))

}
