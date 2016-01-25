package uk.ac.warwick.tabula.scheduling.web.controllers.sysadmin

import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{Appliable, Command, Description, ReadOnly}
import uk.ac.warwick.tabula.data.model.{StaffMember, StudentMember}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.scheduling.commands.imports.{ImportAcademicInformationCommand, ImportAssignmentsCommand, ImportDepartmentsModulesCommand, ImportProfilesCommand}
import uk.ac.warwick.tabula.scheduling.commands.{CleanupUnreferencedFilesCommand, SanityCheckFilesystemCommand, SyncReplicaFilesystemCommand}
import uk.ac.warwick.tabula.scheduling.jobs.ImportMembersJob
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.elasticsearch.{NotificationIndexService, AuditEventIndexService, ElasticsearchIndexingResult, ProfileIndexService}
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.validators.WithinYears
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.UrlMethodModel
import uk.ac.warwick.userlookup.UserLookupInterface

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * Screens for application sysadmins, i.e. the web development and content teams.
 *
 * @deprecated Use version in home module instead
 */

abstract class BaseSysadminController extends BaseController {
	var moduleService = Wire[ModuleAndDepartmentService]
	var userLookup = Wire[UserLookupInterface]
	var urlRewriter = Wire[UrlMethodModel]

	def redirectToHome = {
		// Redirect cross-context
		Redirect(urlRewriter.exec(JArrayList("/sysadmin/", "/", true)).toString)
	}
}

@Controller
class HomeController extends BaseSysadminController {
	@RequestMapping(Array("/")) def home = redirectToHome
}

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

class CompleteScheduledNotificationsCommand extends Command[Unit] with ReadOnly {
	PermissionCheck(Permissions.ImportSystemData)

	def applyInternal() = {
		scheduledNotificationService.processNotifications()
	}

	def describe(d: Description) = d.property("from" -> DateTime.now)
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

	@RequestMapping(method = Array(POST))
	def importModules = {
		ImportAcademicInformationCommand().apply()
		"sysadmin/importdone"
	}

}

@Controller
@RequestMapping(Array("/sysadmin/import-department"))
class ImportDeptModulesController extends BaseSysadminController {

	@ModelAttribute("importDeptModulesCommand") def importProfilesCommand = ImportDepartmentsModulesCommand()

	@RequestMapping(method = Array(POST))
	def importModules(@ModelAttribute("importDeptModulesCommand") command: Appliable[Unit]) = {
		command.apply()
		redirectToHome
	}
}

@Controller
@RequestMapping(Array("/sysadmin/import-sits"))
class ImportSitsController extends BaseSysadminController {
	@RequestMapping(method = Array(POST))
	def reindex() = {
		val command = ImportAssignmentsCommand()
		command.apply()
		redirectToHome
	}
}

@Controller
@RequestMapping(Array("/sysadmin/import-profiles"))
class ImportProfilesController extends BaseSysadminController with AutowiringJobServiceComponent {

	@ModelAttribute("importProfilesCommand")
	def importProfilesCommand = new ImportProfilesCommand

	@RequestMapping(method = Array(POST), params = Array("members"))
	def importSpecificProfiles(@RequestParam members: String) = {
		val jobInstance = jobService.add(None, ImportMembersJob(members.split('\n')))
		Redirect(Routes.scheduling.jobs.status(jobInstance))
	}

	@RequestMapping(method = Array(POST))
	def importProfiles(@ModelAttribute("importProfilesCommand") command: ImportProfilesCommand) = {
		command.apply()
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

		profileService.getMemberByUniversityIdStaleOrFresh(universityId) match {
			case Some(stu: StudentMember) => command.refresh(stu.universityId, Some(stu.userId))
			case Some(staff: StaffMember) => command.refresh(staff.universityId, Some(staff.userId))
			case Some(_) => throw new IllegalArgumentException("Tried to refresh a non-staff/student member - not implemented yet")
			case None => command.refresh(universityId, None)
		}

		// Redirect cross-context
		Redirect(urlRewriter.exec(JArrayList("/view/" + universityId, "/profiles", true)).toString)
	}
}

@Controller
@RequestMapping(Array("/sysadmin/sync"))
class SyncFilesystemController extends BaseSysadminController {
	var fileSyncEnabled = Wire[JBoolean]("${environment.standby:false}")

	@RequestMapping
	def sync() = {
		if (!fileSyncEnabled) throw new IllegalStateException("File syncing not enabled")

		new SyncReplicaFilesystemCommand().apply()
		redirectToHome
	}
}

@Controller
@RequestMapping(Array("/sysadmin/filesystem-cleanup"))
class CleanupFilesystemController extends BaseSysadminController {
	@RequestMapping
	def cleanup() = {
		new CleanupUnreferencedFilesCommand().apply()
		redirectToHome
	}
}

@Controller
@RequestMapping(Array("/sysadmin/filesystem-sanity"))
class SanityCheckFilesystemController extends BaseSysadminController {
	@RequestMapping
	def sanityCheck() = {
		new SanityCheckFilesystemCommand().apply()
		redirectToHome
	}
}

@Controller
@RequestMapping(Array("/sysadmin/complete-scheduled-notification"))
class CompleteScheduledNotificationsController extends BaseSysadminController {
	@RequestMapping
	def complete() = {
		new CompleteScheduledNotificationsCommand().apply()
		redirectToHome
	}
}
