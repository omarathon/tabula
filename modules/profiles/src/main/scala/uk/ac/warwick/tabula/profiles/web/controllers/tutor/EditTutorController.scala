package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import scala.reflect.BeanProperty
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
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.profiles.commands.SearchTutorsCommand
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.data.model.StudentMember

class EditTutorCommand(val student: StudentMember) extends Command[StudentRelationship] {

	PermissionCheck(Permissions.Profiles.PersonalTutor.Update, student)
	
	@BeanProperty var studentUniId: String = student.getUniversityId
	@BeanProperty var tutorUniId: String = null
	@BeanProperty var save: String = null	

	var profileService = Wire.auto[ProfileService]
	
	def currentTutor = profileService.getPersonalTutor(student)
	
	def applyInternal: StudentRelationship = {
		profileService.saveStudentRelationship(PersonalTutor, student.studyDetails.sprCode, tutorUniId)	
	}

	override def describe(d: Description) = d.property("student ID" -> studentUniId).property("new tutor ID" -> tutorUniId)
}

@Controller
@RequestMapping(Array("/tutor/{student}/edit"))
class EditTutorController extends BaseController {
	var profileService = Wire.auto[ProfileService]
	
	@ModelAttribute("searchTutorsCommand") def searchTutorsCommand =
		restricted(new SearchTutorsCommand(user)) orNull
	
	@ModelAttribute("editTutorCommand")
	def editTutorCommand(@PathVariable("student") student: StudentMember) = new EditTutorCommand(student)
	
	// initial form display
	@RequestMapping(params=Array("!tutorUniId"))
	def editTutor(@ModelAttribute("editTutorCommand") cmd: EditTutorCommand, request: HttpServletRequest ) = {
		Mav("tutor/edit/view", 
			"studentUniId" -> cmd.student.universityId,
			"tutorToDisplay" -> cmd.currentTutor
		)
	}
	
	// now we've got a tutor id to display, but it's not time to save it yet
	@RequestMapping(params=Array("tutorUniId", "!save"))
	def displayPickedTutor(@ModelAttribute("editTutorCommand") cmd: EditTutorCommand, request: HttpServletRequest ) = {

		val pickedTutor = profileService.getMemberByUniversityId(cmd.tutorUniId)
		
		Mav("tutor/edit/view", 
			"studentUniId" -> cmd.studentUniId,
			"tutorToDisplay" -> pickedTutor,
			"pickedTutor" -> pickedTutor
		)
	}	

	@RequestMapping(params=Array("tutorUniId", "save=true"), method=Array(POST))
	def savePickedTutor(@ModelAttribute("editTutorCommand") cmd: EditTutorCommand, request: HttpServletRequest ) = {
		val student = cmd.student
		
		if (cmd.save.equals("true")) {
			val rel = cmd.apply()
		}
		else throw new IllegalStateException("form param save not set as expected")
		
		Mav("tutor/edit/view", 
			"studentUniId" -> cmd.studentUniId, 
			"tutorToDisplay" -> cmd.currentTutor
		)
	}
}
