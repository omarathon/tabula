package uk.ac.warwick.tabula.commands.groups

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupEvent
import uk.ac.warwick.tabula.pdf.FreemarkerXHTMLPDFGeneratorComponent
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsCheckingMethods, PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.{AutowiredTextRendererComponent, PDFView}

import scala.collection.JavaConverters._

import DownloadRegisterAsPdfCommand._

object DownloadRegisterAsPdfCommand {
	def apply(event: SmallGroupEvent, week: Int) =
		new DownloadRegisterAsPdfCommandInternal(event, week)
			with ComposableCommand[PDFView]
			with DownloadRegisterAsPdfPermissions
			with AutowiringProfileServiceComponent
			with AutowiringSmallGroupServiceComponent
			with AutowiringUserLookupComponent
			with AutowiringTermServiceComponent
			with ReadOnly with Unaudited

	object DisplayName {
		val Name = "name"
		val Id = "id"
		val Both = "both"
	}

	object DisplayCheck {
		val Checkbox = "checkbox"
		val SignatureLine = "line"
	}
}

trait DownloadRegisterAsPdfCommandState {
	def event: SmallGroupEvent
	def week: Int

	var showPhotos = true
	var displayName = DisplayName.Name
	var displayCheck = DisplayCheck.Checkbox
}

class DownloadRegisterAsPdfCommandInternal(val event: SmallGroupEvent, val week: Int) extends CommandInternal[PDFView] with DownloadRegisterAsPdfCommandState {
	self: ProfileServiceComponent with SmallGroupServiceComponent with UserLookupComponent with TermServiceComponent =>

	lazy val occurrence = transactional() { smallGroupService.getOrCreateSmallGroupEventOccurrence(event, week) }

	lazy val members: Seq[MemberOrUser] = {
		(event.group.students.users.map { user =>
			val member = profileService.getMemberByUniversityId(user.getWarwickId)
			MemberOrUser(member, user)
		} ++ occurrence.attendance.asScala.toSeq.map { a =>
			val member = profileService.getMemberByUniversityId(a.universityId)
			val user = userLookup.getUserByWarwickUniId(a.universityId)
			MemberOrUser(member, user)
		}).distinct.sortBy { mou => (mou.lastName, mou.firstName, mou.universityId) }
	}

	lazy val eventDate = {
		val weeksForYear = termService.getAcademicWeeksForYear(event.group.groupSet.academicYear.dateInTermOne).toMap
		weeksForYear(week).getStart.withDayOfWeek(event.day.jodaDayOfWeek)
	}

	def applyInternal() = {
		new PDFView(
			s"register-week${week}.pdf",
			"/WEB-INF/freemarker/groups/attendance/register-pdf.ftl",
			Map(
				"event" -> event,
				"week" -> week,
				"formattedEventDate" -> eventDate.toString("dd/MM/yyyy"),
				"members" -> members,
				"showPhotos" -> showPhotos,
				"displayName" -> displayName,
				"displayCheck" -> displayCheck
			)
		) with FreemarkerXHTMLPDFGeneratorComponent with AutowiredTextRendererComponent
	}
}

trait DownloadRegisterAsPdfPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DownloadRegisterAsPdfCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroupEvents.Register, mandatory(event))
	}
}
