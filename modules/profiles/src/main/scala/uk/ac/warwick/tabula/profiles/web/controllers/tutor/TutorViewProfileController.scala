package uk.ac.warwick.tabula.profiles.web.controllers.tutor
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.profiles.commands.tutor.TutorSearchProfilesCommand
import uk.ac.warwick.tabula.commands.ViewViewableCommand
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.spring.Wire
import scala.reflect.BeanProperty
import javax.servlet.http.HttpServletRequest
import uk.ac.warwick.tabula.data.model.PersonalTutor

class TutorViewProfileCommand(val student: Member) extends ViewViewableCommand(Permissions.Profiles.Read, student) {
	@BeanProperty var studentUniId: String = student.getUniversityId
	@BeanProperty var tutorUniId: String = null

	var profileService = Wire.auto[ProfileService]
	
	def currentTutor = profileService.getPersonalTutor(student)
	
	def currentTutorForDisplay = currentTutor match {
		case None => ""
		case Some(mem) => profileService.getNameAndNumber(mem)
	}
}

@Controller
@RequestMapping(Array("/tutor/{student}/edit"))
class TutorViewProfileController extends TutorProfilesController {
	
	@ModelAttribute("tutorSearchProfilesCommand") def tutorSearchProfilesCommand =
		restricted(new TutorSearchProfilesCommand(user)) orNull
	
	@ModelAttribute("tutorViewProfileCommand")
	def tutorViewProfileCommand(@PathVariable("student") student: Member) = new TutorViewProfileCommand(student)
	
	// initial form display
	@RequestMapping(params=Array("!tutorUniId"))
	def editTutor(@ModelAttribute("tutorViewProfileCommand") cmd: TutorViewProfileCommand, request: HttpServletRequest ) = {
		Mav("tutor/tutor_view", 
			"studentUniId" -> cmd.student.universityId,
			"tutorToDisplay" -> cmd.currentTutorForDisplay
		)
	}
	
	// now we've got a tutor id to display, but it's not time to save it yet
	@RequestMapping(params=Array("tutorUniId", "!save"))
	def displayPickedTutor(@ModelAttribute("tutorViewProfileCommand") cmd: TutorViewProfileCommand, request: HttpServletRequest ) = {

		val pickedTutor = profileService.getMemberByUniversityId(cmd.tutorUniId)
		
		Mav("tutor/tutor_view", 
			"studentUniId" -> cmd.studentUniId,
			"tutorToDisplay" -> profileService.getNameAndNumber(pickedTutor.getOrElse(throw new IllegalStateException("Can't find member object for new tutor"))),
			"pickedTutor" -> pickedTutor
		)
	}	

	@RequestMapping(params=Array("tutorUniId", "save"))
	def savePickedTutor(@ModelAttribute("tutorViewProfileCommand") cmd: TutorViewProfileCommand, request: HttpServletRequest ) = {
		val student = cmd.student
		val sprCode = student.sprCode
		
		val rel = profileService.saveStudentRelationship(PersonalTutor, cmd.student.sprCode, cmd.tutorUniId)

		Mav("tutor/tutor_view", 
			"studentUniId" -> cmd.studentUniId, 
			"tutorToDisplay" -> cmd.currentTutorForDisplay
		)
	}
}
