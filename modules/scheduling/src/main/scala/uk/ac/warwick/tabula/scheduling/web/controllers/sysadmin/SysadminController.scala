package uk.ac.warwick.tabula.scheduling.web.controllers.sysadmin

import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{Appliable, Command, Description, ReadOnly}
import uk.ac.warwick.tabula.data.model.{StaffMember, Department, Member, StudentMember}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.scheduling.commands.CleanupUnreferencedFilesCommand
import uk.ac.warwick.tabula.scheduling.commands.SanityCheckFilesystemCommand
import uk.ac.warwick.tabula.scheduling.commands.SyncReplicaFilesystemCommand
import uk.ac.warwick.tabula.scheduling.commands.imports.{ImportDepartmentsModulesCommand, ImportAssignmentsCommand, ImportAcademicInformationCommand, ImportProfilesCommand}
import uk.ac.warwick.tabula.scheduling.services.AssignmentImporter
import uk.ac.warwick.tabula.scheduling.services.ProfileImporter
import uk.ac.warwick.tabula.services.{ScheduledNotificationService, NotificationIndexService, AuditEventIndexService, ModuleAndDepartmentService, ProfileIndexService}
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.web.views.UrlMethodModel
import uk.ac.warwick.userlookup.UserLookupInterface
import uk.ac.warwick.tabula.DateFormats
import uk.ac.warwick.tabula.validators.WithinYears

/**
 * Screens for application sysadmins, i.e. the web development and content teams.
 *
 * @deprecated Use version in home module instead
 */

abstract class BaseSysadminController extends BaseController {
	var moduleService = Wire.auto[ModuleAndDepartmentService]
	var userLookup = Wire.auto[UserLookupInterface]
	var urlRewriter = Wire.auto[UrlMethodModel]

	def redirectToHome = {
		// Redirect cross-context
		Redirect(urlRewriter.exec(JArrayList("/sysadmin/", "/", true)).toString())
	}
}

@Controller
class HomeController extends BaseSysadminController {
	@RequestMapping(Array("/")) def home = redirectToHome
}

class ReindexAuditEventsCommand extends Command[Unit] with ReadOnly {
	PermissionCheck(Permissions.ImportSystemData)

	var indexer = Wire.auto[AuditEventIndexService]

	@WithinYears(maxPast = 20) @DateTimeFormat(pattern = DateFormats.DateTimePicker)
	var from: DateTime = _

	def applyInternal() = {
		indexer.indexFrom(from)
	}

	def describe(d: Description) = d.property("from" -> from)
}

class ReindexNotificationsCommand extends Command[Unit] with ReadOnly {
	PermissionCheck(Permissions.ImportSystemData)

	var indexer = Wire.auto[NotificationIndexService]

	@WithinYears(maxPast = 20) @DateTimeFormat(pattern = DateFormats.DateTimePicker)
	var from: DateTime = _

	def applyInternal() = {
		indexer.indexFrom(from)
	}

	def describe(d: Description) = d.property("from" -> from)
}

class ReindexProfilesCommand extends Command[Unit] with ReadOnly {
	PermissionCheck(Permissions.ImportSystemData)

	var indexer = Wire.auto[ProfileIndexService]
	var mdService = Wire.auto[ModuleAndDepartmentService]

	@WithinYears(maxPast = 20) @DateTimeFormat(pattern = DateFormats.DateTimePicker)
	var from: DateTime = _
	var deptCode: String = _

	def applyInternal() = {
		mdService.getDepartmentByCode(deptCode) match {
			case None => indexer.indexFrom(from)
			case Some(department) => indexer.indexByDateAndDepartment(from, department)
		}
	}

	def describe(d: Description) = d.property("from" -> from).property("deptCode" -> deptCode)
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
		form.apply
		redirectToHome
	}
}

@Controller
@RequestMapping(Array("/sysadmin/index/run-audit"))
class SysadminIndexAuditController extends BaseSysadminController {
	@ModelAttribute("reindexForm") def reindexForm = new ReindexAuditEventsCommand

	@RequestMapping(method = Array(POST))
	def reindex(form: ReindexAuditEventsCommand) = {
		form.apply
		redirectToHome
	}
}

@Controller
@RequestMapping(Array("/sysadmin/index/run-profiles"))
class SysadminIndexProfilesController extends BaseSysadminController {
	@ModelAttribute("reindexForm") def reindexForm = new ReindexProfilesCommand

	@RequestMapping(method = Array(POST))
	def reindex(form: ReindexProfilesCommand) = {
		form.apply
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
class ImportProfilesController extends BaseSysadminController {
	@ModelAttribute("importProfilesCommand") def importProfilesCommand = new ImportProfilesCommand

	@RequestMapping(method = Array(POST))
	def importProfiles(@ModelAttribute("importProfilesCommand") command: ImportProfilesCommand) = {
		command.apply()
		redirectToHome
	}
}

@Controller
@RequestMapping(Array("/sysadmin/import-profiles/{member}"))
class ImportSingleProfileController extends BaseSysadminController {
	@RequestMapping(method = Array(POST))
	def importProfile(@PathVariable("member") member: Member) = {
		val command = new ImportProfilesCommand

		member match {
			case stu: StudentMember => command.refresh(stu)
			case staff: StaffMember => command.refresh(staff)
			case _ => throw new IllegalArgumentException("Tried to refresh a non-staff/student member - not implemented yet")
		}

		// Redirect cross-context
		Redirect(urlRewriter.exec(JArrayList("/view/" + member.universityId, "/profiles", true)).toString())
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
