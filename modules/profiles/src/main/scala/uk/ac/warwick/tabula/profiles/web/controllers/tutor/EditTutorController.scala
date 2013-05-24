package uk.ac.warwick.tabula.profiles.web.controllers.tutor


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
import uk.ac.warwick.tabula.profiles.commands.TutorChangeNotifierCommand
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.helpers.Promises
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.ItemNotFoundException
import org.springframework.web.bind.annotation.RequestParam
import org.joda.time.DateTime

class EditTutorCommand(val student: StudentMember, val currentTutor: Option[Member], val remove: Option[Boolean]) extends Command[Option[StudentRelationship]] with Promises {
	
	var profileService = Wire[ProfileService]

	var tutor: Member = _

	PermissionCheck(Permissions.Profiles.PersonalTutor.Update, student)

	// throw this request out if personal tutors can't be edited in Tabula for this department
	if (!student.studyDetails.studyDepartment.canEditPersonalTutors) {
		logger.info("Denying access to EditTutorCommand since student "
				+ student.studyDetails.sprCode
				+ " has a study department "
				+ "( " + student.studyDetails.studyDepartment.name
				+ ") with a personal tutor source setting of "
				+ student.studyDetails.studyDepartment.personalTutorSource + ".")
		throw new ItemNotFoundException()
	}

	val newTutor = promise { tutor }

	val notifyCommand = new TutorChangeNotifierCommand(student, currentTutor, newTutor)

	def applyInternal = {
		if (!currentTutor.isDefined) {
			// Brand new tutor
			val newRelationship = profileService.saveStudentRelationship(PersonalTutor, student.studyDetails.sprCode, tutor.universityId)

			notifyCommand.apply()
			Some(newRelationship)
		} else if (currentTutor.get != tutor) {
			// Replacing the current tutor with a new one
			val currentRelationships = profileService.findCurrentRelationships(PersonalTutor, student.studyDetails.sprCode)
			
			// Is there an existing relationship for this tutor? 
			// Could happen if a student has two tutors, and we're trying to replace the second with the first
			currentRelationships.find(_.agent == tutor.universityId) match {
				case Some(existingRelationship) => {
					// Just return the existing relationship without any notification
						Some(existingRelationship)
				}
				case _ => {
					// Find the relationship for the current tutor, and end it
					endTutorRelationship(currentRelationships)
					
					// Save the new relationship
					val newRelationship = profileService.saveStudentRelationship(PersonalTutor, student.studyDetails.sprCode, tutor.universityId)

					notifyCommand.apply()
					Some(newRelationship)
				}
			}
		} else if (currentTutor.get == tutor && remove.getOrElse(false)) {
				val currentRelationships = profileService.findCurrentRelationships(PersonalTutor, student.studyDetails.sprCode)
				endTutorRelationship(currentRelationships)
				None
			} else {
				None
			}
	}

	def endTutorRelationship(currentRelationships: Seq[StudentRelationship]) {
		currentRelationships.find(_.agent == currentTutor.get.universityId) foreach { rel =>
			rel.endDate = DateTime.now
			profileService.saveOrUpdate(rel)
		}
	}

	override def describe(d: Description) = d.property("student ID" -> student.universityId).property("new tutor ID" -> tutor.universityId)
}

@Controller
@RequestMapping(Array("/tutor/{student}/edit"))
class EditTutorController extends BaseController {
	var profileService = Wire.auto[ProfileService]

	@ModelAttribute("editTutorCommand")
	def editTutorCommand(@PathVariable("student") student: Member, @RequestParam(value="currentTutor", required=false) currentTutor: Member,
	                     @RequestParam(value="remove", required=false) remove: Boolean) = student match {
		case student: StudentMember => new EditTutorCommand(student, Option(currentTutor), Option(remove))
		case _ => throw new ItemNotFoundException
	}

	// initial form display
	@RequestMapping(method=Array(GET))
	def editTutor(@ModelAttribute("editTutorCommand") cmd: EditTutorCommand, request: HttpServletRequest) = {
		Mav("tutor/edit/view",
			"student" -> cmd.student,
			"tutorToDisplay" -> cmd.currentTutor
		).noLayout()
	}

	@RequestMapping(method=Array(POST))
	def saveTutor(@ModelAttribute("editTutorCommand") cmd: EditTutorCommand, request: HttpServletRequest ) = {
		val rel = cmd.apply()

		Mav("tutor/edit/view",
			"student" -> cmd.student,
			"tutorToDisplay" -> cmd.currentTutor
		)
	}
}
