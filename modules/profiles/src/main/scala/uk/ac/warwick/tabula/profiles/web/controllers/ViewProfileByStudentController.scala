package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.PermissionDeniedException
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.model.{ MeetingRecord, Member, StudentMember }
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.profiles.commands.SearchProfilesCommand
import uk.ac.warwick.tabula.commands.{ Appliable, ViewViewableCommand }
import uk.ac.warwick.tabula.profiles.commands.ViewMeetingRecordCommand
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.commands.Command
import scala.Some
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.web.Mav

@Controller
class ViewProfileByStudentController extends ViewProfileController {

	@ModelAttribute("viewProfileCommand")
	def viewProfileCommand(@PathVariable("member") member: Member) = member match {
		case student: StudentMember => new ViewProfileCommand(user, student)
		case _ => throw new ItemNotFoundException
	}

	@RequestMapping(Array("/view/{member}"))
	def viewProfile(
		@PathVariable("member") member: Member,
		@ModelAttribute("viewProfileCommand") profileCmd: Appliable[StudentMember],
		@RequestParam(value = "meeting", required = false) openMeetingId: String,
		@RequestParam(defaultValue = "", required = false) agentId: String) = {
		val profiledStudentMember = profileCmd.apply()
		viewProfileForCourse(profiledStudentMember.mostSignificantCourseDetails, openMeetingId, agentId, profiledStudentMember)
	}
}
