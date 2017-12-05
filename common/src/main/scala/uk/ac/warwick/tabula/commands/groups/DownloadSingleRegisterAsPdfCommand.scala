package uk.ac.warwick.tabula.commands.groups

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.groups.admin._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupEvent, SmallGroupEventOccurrence}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object DownloadSingleRegisterAsPdfCommand {
	def apply(event: SmallGroupEvent, week: Int, filename: String, user: CurrentUser) =
		new DownloadSingleRegisterAsPdfCommandInternal(event, week, filename, user)
			with ComposableCommandWithoutTransaction[RenderableFile]
			with AutowiringDownloadRegistersAsPdfCommandHelper
			with DownloadSingleRegisterAsPdfPermissions
			with DownloadSingleRegisterAsPdfCommandState
			with DownloadRegistersAsPdfCommandRequest
			with GetsOccurrencesForDownloadSingleRegisterAsPdfCommand
			with Unaudited
}

class DownloadSingleRegisterAsPdfCommandInternal(val event: SmallGroupEvent, val week: Int, filename: String, user: CurrentUser)
	extends DownloadRegistersAsPdfCommandInternal(event.group.groupSet.module.adminDepartment, event.group.groupSet.academicYear, filename, user) {

	self: DownloadRegistersAsPdfHelper.Dependencies =>

}

trait DownloadSingleRegisterAsPdfCommandState extends DownloadRegistersAsPdfCommandState {

	self: SmallGroupServiceComponent =>

	def event: SmallGroupEvent
	def week: Int
}

trait DownloadSingleRegisterAsPdfPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: DownloadSingleRegisterAsPdfCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroupEvents.Register, mandatory(event))
	}
}

trait GetsOccurrencesForDownloadSingleRegisterAsPdfCommand extends GetsOccurrences {

	self: DownloadSingleRegisterAsPdfCommandState with SmallGroupServiceComponent =>

	def getOccurrences: Seq[SmallGroupEventOccurrence] = Seq(transactional(readOnly = true){
		smallGroupService.getOrCreateSmallGroupEventOccurrence(event, week).getOrElse(throw new IllegalArgumentException(
			s"Week number $week is not valid for event ${event.id}"
		))
	})
}
