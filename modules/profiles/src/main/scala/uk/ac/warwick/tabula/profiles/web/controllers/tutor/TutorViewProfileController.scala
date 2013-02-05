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

class TutorViewProfileCommand(student: Member) extends ViewViewableCommand(Permissions.Profiles.Read, student) {
	@BeanProperty var studentUniId: String = student.getUniversityId
}

@Controller
@RequestMapping(Array("/tutor/{student}/edit"))
class TutorViewProfileController extends TutorProfilesController {
	
	@ModelAttribute("tutorSearchProfilesCommand") def tutorSearchProfilesCommand =
		restricted(new TutorSearchProfilesCommand(user)) orNull
	
	@ModelAttribute("tutorViewProfileCommand")
	def tutorViewProfileCommand(@PathVariable("student") student: Member) = new TutorViewProfileCommand(student)
	
	@RequestMapping
	def viewProfile(@ModelAttribute("tutorViewProfileCommand") cmd: TutorViewProfileCommand, request: HttpServletRequest ) = {
		val student = cmd.apply
		val studentUniId = student.universityId
		
		// "tutor" will only be defined second time round
		if (request.getParameterMap().containsKey("tutorUniId")) {
            val tutorUniId: String = request.getParameter("tutorUniId");
 			val rel = profileService.saveStudentRelationship(PersonalTutor, student.sprCode, tutorUniId)
        }
		
		Mav("tutor/tutor_view", 
			"studentUniId" -> studentUniId,
			"tutorName" -> profileService.getTutorName(student))
	}
}
