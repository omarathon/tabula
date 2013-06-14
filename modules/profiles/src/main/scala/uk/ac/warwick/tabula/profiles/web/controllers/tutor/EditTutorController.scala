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
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.ItemNotFoundException
import org.springframework.web.bind.annotation.RequestParam
import org.joda.time.DateTime
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.web.controllers.BaseController
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.services.RelationshipService

class EditTutorCommand(val studentCourseDetails: StudentCourseDetails, val currentTutor: Option[Member], val remove: Boolean) extends Command[Option[StudentRelationship]] with Promises {

	var relationshipService = Wire[RelationshipService]

	var tutor: Member = _

	PermissionCheck(Permissions.Profiles.PersonalTutor.Update, studentCourseDetails.student)

	// throw this request out if personal tutors can't be edited in Tabula for this department
	if (!studentCourseDetails.department.canEditPersonalTutors) {
		logger.info("Denying access to EditTutorCommand since student "
				+ studentCourseDetails.sprCode
				+ " has a study department "
				+ "( " + studentCourseDetails.department.name
				+ ") with a personal tutor source setting of "
				+ studentCourseDetails.department.personalTutorSource + ".")
		throw new ItemNotFoundException()
	}

	val newTutor = promise { tutor }

	val notifyCommand = new TutorChangeNotifierCommand(studentCourseDetails.student, currentTutor, newTutor)

	def applyInternal = {
		if (!currentTutor.isDefined) {
			// Brand new tutor
			val newRelationship = relationshipService.saveStudentRelationship(PersonalTutor, studentCourseDetails.sprCode, tutor.universityId)

			notifyCommand.apply()
			Some(newRelationship)
		} else if (currentTutor.get != tutor) {
			// Replacing the current tutor with a new one
			val currentRelationships = relationshipService.findCurrentRelationships(PersonalTutor, studentCourseDetails.sprCode)

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
					val newRelationship = relationshipService.saveStudentRelationship(PersonalTutor, studentCourseDetails.sprCode, tutor.universityId)

					notifyCommand.apply()
					Some(newRelationship)
				}
			}
		} else if (currentTutor.get == tutor && remove) {
				val currentRelationships = relationshipService.findCurrentRelationships(PersonalTutor, studentCourseDetails.sprCode)
				endTutorRelationship(currentRelationships)
				None
			} else {
				None
			}
	}

	def endTutorRelationship(currentRelationships: Seq[StudentRelationship]) {
		currentRelationships.find(_.agent == currentTutor.get.universityId) foreach { rel =>
			rel.endDate = DateTime.now
			relationshipService.saveOrUpdate(rel)
		}
	}

	override def describe(d: Description) = d.property("student SPR code" -> studentCourseDetails.sprCode).property("new tutor ID" -> tutor.universityId)
}

@Controller
@RequestMapping(Array("/tutor/{studentCourseDetails}"))
class EditTutorController extends BaseController {
	var profileService = Wire.auto[ProfileService]

	@ModelAttribute("editTutorCommand")
	def editTutorCommand(
			@PathVariable("studentCourseDetails") studentCourseDetails: StudentCourseDetails,
			@RequestParam(value="currentTutor", required=false) currentTutor: Member,
			@RequestParam(value="remove", required=false) remove: Boolean
			) =
		new EditTutorCommand(studentCourseDetails, Option(currentTutor), Option(remove).getOrElse(false))

	// initial form display
	@RequestMapping(value = Array("/edit","/add"),method=Array(GET))
	def editTutor(@ModelAttribute("editTutorCommand") cmd: EditTutorCommand, request: HttpServletRequest) = {
		Mav("tutor/edit/view",
			"studentCourseDetails" -> cmd.studentCourseDetails,
			"tutorToDisplay" -> cmd.currentTutor
		).noLayout()
	}


	@RequestMapping(value = Array("/edit", "/add"), method=Array(POST))
	def saveTutor(@ModelAttribute("editTutorCommand") cmd: EditTutorCommand, request: HttpServletRequest ) = {
		val rel = cmd.apply()

		Mav("tutor/edit/view",
			"student" -> cmd.studentCourseDetails.student,
			"tutorToDisplay" -> cmd.currentTutor
		)
	}
}
