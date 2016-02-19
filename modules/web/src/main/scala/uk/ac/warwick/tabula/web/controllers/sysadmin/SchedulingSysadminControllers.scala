package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.joda.time.DateTime
import org.quartz.Scheduler
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.commands.scheduling.imports.ImportProfilesCommand
import uk.ac.warwick.tabula.commands.{Command, Description, ReadOnly}
import uk.ac.warwick.tabula.data.model.{StaffMember, StudentMember}
import uk.ac.warwick.tabula.helpers.SchedulingHelpers._
import uk.ac.warwick.tabula.jobs.scheduling.ImportMembersJob
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.elasticsearch.{AuditEventIndexService, ElasticsearchIndexingResult, NotificationIndexService, ProfileIndexService}
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.services.scheduling.jobs.{ProcessScheduledNotificationsJob, ImportAcademicDataJob, ImportAssignmentsJob, ImportProfilesJob}
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, ProfileService}
import uk.ac.warwick.tabula.validators.WithinYears
import uk.ac.warwick.tabula.web.Routes

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class ReindexAuditEventsCommand extends Command[ElasticsearchIndexingResult] with ReadOnly {
	PermissionCheck(Permissions.ImportSystemData)

	var indexer = Wire[AuditEventIndexService]

	@WithinYears(maxPast = 20) @DateTimeFormat(pattern = DateFormats.DateTimePicker)
	var from: DateTime = _

	def applyInternal() = {
		Await.result(indexer.indexFrom(from), Duration.Inf)
	}

	override def describe(d: Description) = d.property("from" -> from)
	override def describeResult(d: Description, result: ElasticsearchIndexingResult) =
		d.properties(
			"successful" -> result.successful,
			"failed" -> result.failed,
			"timeTaken" -> result.timeTaken,
			"maxUpdatedDate" -> result.maxUpdatedDate
		)
}

class ReindexNotificationsCommand extends Command[ElasticsearchIndexingResult] with ReadOnly {
	PermissionCheck(Permissions.ImportSystemData)

	var indexer = Wire[NotificationIndexService]

	@WithinYears(maxPast = 20) @DateTimeFormat(pattern = DateFormats.DateTimePicker)
	var from: DateTime = _

	def applyInternal() = {
		Await.result(indexer.indexFrom(from), Duration.Inf)
	}

	def describe(d: Description) = d.property("from" -> from)
	override def describeResult(d: Description, result: ElasticsearchIndexingResult) =
		d.properties(
			"successful" -> result.successful,
			"failed" -> result.failed,
			"timeTaken" -> result.timeTaken,
			"maxUpdatedDate" -> result.maxUpdatedDate
		)
}

class ReindexProfilesCommand extends Command[ElasticsearchIndexingResult] with ReadOnly {
	PermissionCheck(Permissions.ImportSystemData)

	var indexer = Wire[ProfileIndexService]
	var mdService = Wire[ModuleAndDepartmentService]

	@WithinYears(maxPast = 20) @DateTimeFormat(pattern = DateFormats.DateTimePicker)
	var from: DateTime = _
	var deptCode: String = _

	def applyInternal() = {
		Await.result(
			mdService.getDepartmentByCode(deptCode) match {
				case None => indexer.indexFrom(from)
				case Some(department) => indexer.indexByDateAndDepartment(from, department)
			},
			Duration.Inf
		)
	}

	def describe(d: Description) = d.property("from" -> from).property("deptCode" -> deptCode)
	override def describeResult(d: Description, result: ElasticsearchIndexingResult) =
		d.properties(
			"successful" -> result.successful,
			"failed" -> result.failed,
			"timeTaken" -> result.timeTaken,
			"maxUpdatedDate" -> result.maxUpdatedDate
		)
}

@Controller
@RequestMapping(Array("/sysadmin/index/run-notifications"))
class SysadminNotificationsAuditController extends BaseSysadminController {
	@ModelAttribute("reindexForm") def reindexForm = new ReindexNotificationsCommand

	@RequestMapping(method = Array(POST))
	def reindex(form: ReindexNotificationsCommand) = {
		form.apply()
		redirectToHome
	}
}

@Controller
@RequestMapping(Array("/sysadmin/index/run-audit"))
class SysadminIndexAuditController extends BaseSysadminController {
	@ModelAttribute("reindexForm") def reindexForm = new ReindexAuditEventsCommand

	@RequestMapping(method = Array(POST))
	def reindex(form: ReindexAuditEventsCommand) = {
		form.apply()
		redirectToHome
	}
}

@Controller
@RequestMapping(Array("/sysadmin/index/run-profiles"))
class SysadminIndexProfilesController extends BaseSysadminController {
	@ModelAttribute("reindexForm") def reindexForm = new ReindexProfilesCommand

	@RequestMapping(method = Array(POST))
	def reindex(form: ReindexProfilesCommand) = {
		form.apply()
		redirectToHome
	}
}

@Controller
@RequestMapping(Array("/sysadmin/import"))
class SchedulingSysadminController extends BaseSysadminController {

	var scheduler = Wire[Scheduler]

	@RequestMapping(method = Array(POST))
	def importModules = {
		scheduler.scheduleNow[ImportAcademicDataJob]()
		redirectToHome
	}

}

@Controller
@RequestMapping(Array("/sysadmin/import-department"))
class ImportDeptModulesController extends BaseSysadminController {

	var scheduler = Wire[Scheduler]

	@RequestMapping(method = Array(POST))
	def importModules(@RequestParam deptCode: String) = {
		scheduler.scheduleNow[ImportAcademicDataJob]("departmentCodes" -> deptCode)
		redirectToHome
	}
}

@Controller
@RequestMapping(Array("/sysadmin/import-sits"))
class ImportSitsController extends BaseSysadminController {

	var scheduler = Wire[Scheduler]

	@RequestMapping(method = Array(POST))
	def reindex() = {
		scheduler.scheduleNow[ImportAssignmentsJob]()
		redirectToHome
	}
}

@Controller
@RequestMapping(Array("/sysadmin/import-profiles"))
class ImportProfilesController extends BaseSysadminController with AutowiringJobServiceComponent {

	var scheduler = Wire[Scheduler]

	@RequestMapping(method = Array(POST), params = Array("members"))
	def importSpecificProfiles(@RequestParam members: String) = {
		val jobInstance = jobService.add(None, ImportMembersJob(members.split('\n')))
		Redirect(Routes.sysadmin.jobs.status(jobInstance))
	}

	@RequestMapping(method = Array(POST))
	def importProfiles(@RequestParam deptCode: String) = {
		scheduler.scheduleNow[ImportProfilesJob]("departmentCode" -> deptCode)
		redirectToHome
	}
}

@Controller
@RequestMapping(Array("/sysadmin/import-profiles/{universityId}"))
class ImportSingleProfileController extends BaseSysadminController {

	var profileService = Wire[ProfileService]

	@RequestMapping def form = "sysadmin/reindexprofile"

	@RequestMapping(method = Array(POST))
	def importProfile(@PathVariable universityId: String) = {
		val command = new ImportProfilesCommand

		val member = profileService.getMemberByUniversityIdStaleOrFresh(universityId) match {
			case Some(stu: StudentMember) => command.refresh(stu.universityId, Some(stu.userId))
			case Some(staff: StaffMember) => command.refresh(staff.universityId, Some(staff.userId))
			case Some(_) => throw new IllegalArgumentException("Tried to refresh a non-staff/student member - not implemented yet")
			case None => command.refresh(universityId, None)
		}

		// Redirect cross-context
		Redirect(Routes.profiles.profile.view(member.get))
	}
}

@Controller
@RequestMapping(Array("/sysadmin/complete-scheduled-notification"))
class CompleteScheduledNotificationsController extends BaseSysadminController {

	var scheduler = Wire[Scheduler]

	@RequestMapping
	def complete() = {
		scheduler.scheduleNow[ProcessScheduledNotificationsJob]()
		redirectToHome
	}
}