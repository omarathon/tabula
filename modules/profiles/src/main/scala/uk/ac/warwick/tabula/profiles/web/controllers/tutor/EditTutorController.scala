package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import scala.beans.BeanProperty

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

import javax.servlet.http.HttpServletRequest
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.RelationshipType.PersonalTutor
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.helpers.Promises
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.profiles.commands.SearchTutorsCommand
import uk.ac.warwick.tabula.profiles.commands.TutorChangeNotifierCommand
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.helpers.Promises
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.ItemNotFoundException

class EditTutorCommand(val student: StudentMember) extends Command[Option[StudentRelationship]] with Promises {

	var tutor: Member = _
	
	PermissionCheck(Permissions.Profiles.PersonalTutor.Update, student)

	val newTutor = promise { tutor }
	
	//var storeTutor: Boolean = false
	
	var profileService = Wire.auto[ProfileService]
	
	def currentTutor = profileService.getPersonalTutor(student)
	
	val notifyCommand = new TutorChangeNotifierCommand(student, currentTutor, newTutor)

	def applyInternal = {
		if (!currentTutor.isDefined || !currentTutor.get.equals(tutor)) {
			// it's a real change

			val newRelationship = profileService.saveStudentRelationship(PersonalTutor, student.studyDetails.sprCode, tutor.universityId)

			notifyCommand.apply()
			Some(newRelationship)
		} else {
			None
		}
	}

	override def describe(d: Description) = d.property("student ID" -> student.universityId).property("new tutor ID" -> tutor.universityId)
}

@Controller
@RequestMapping(Array("/tutor/{student}/edit"))
class EditTutorController extends BaseController {
	var profileService = Wire.auto[ProfileService]
	
	@ModelAttribute("searchTutorsCommand") def searchTutorsCommand =
		restricted(new SearchTutorsCommand(user)).orNull

	@ModelAttribute("editTutorCommand")
	def editTutorCommand(@PathVariable("student") student: Member) = student match {
		case student: StudentMember => new EditTutorCommand(student)
		case _ => throw new ItemNotFoundException
	}
	
	// initial form display
	@RequestMapping(params = Array("!tutor"))
	def editTutor(@ModelAttribute("editTutorCommand") cmd: EditTutorCommand, request: HttpServletRequest) = {
		Mav("tutor/edit/view",
			"student" -> cmd.student,
			"tutorToDisplay" -> cmd.currentTutor,
			"displayOptionToSave" -> false)
	}

	// now we've got a tutor id to display, but it's not time to save it yet
	@RequestMapping(params = Array("tutor", "!storeTutor"))
	def displayPickedTutor(@ModelAttribute("editTutorCommand") cmd: EditTutorCommand, request: HttpServletRequest) = {

		Mav("tutor/edit/view",
			"student" -> cmd.student,
			"tutorToDisplay" -> cmd.tutor,
			"displayOptionToSave" -> (!cmd.currentTutor.isDefined || !cmd.currentTutor.get.equals(cmd.tutor)))	}

	@RequestMapping(params=Array("tutor", "storeTutor"), method=Array(POST))
	def savePickedTutor(@ModelAttribute("editTutorCommand") cmd: EditTutorCommand, request: HttpServletRequest ) = {
		val rel = cmd.apply()
		
		Mav("tutor/edit/view", 
			"student" -> cmd.student, 
			"tutorToDisplay" -> cmd.currentTutor,
			"displayOptionToSave" -> false
		)
	}
}
