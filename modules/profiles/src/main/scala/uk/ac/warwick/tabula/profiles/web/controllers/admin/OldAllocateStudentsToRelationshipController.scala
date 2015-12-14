package uk.ac.warwick.tabula.profiles.web.controllers.admin

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.profiles.relationships.{OldTransientStudentRelationshipTemplateCommand, OldAllocateStudentsToRelationshipCommand}
import uk.ac.warwick.tabula.data.model.{Department, StudentRelationshipType}
import uk.ac.warwick.tabula.commands.profiles.relationships.OldTransientStudentRelationshipTemplateCommand
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.ExcelView

import scala.collection.JavaConverters._

/**
 * Allocates students to relationships in a department.
 */
@Controller
@RequestMapping(value=Array("/department/{department}/{relationshipType}/allocate-old"))
class OldAllocateStudentsToRelationshipController extends ProfilesController {

	validatesSelf[OldAllocateStudentsToRelationshipCommand]

	@ModelAttribute
	def command(@PathVariable department: Department, @PathVariable relationshipType: StudentRelationshipType) =
		new OldAllocateStudentsToRelationshipCommand(department, relationshipType, user)

	@RequestMapping
	def showForm(cmd: OldAllocateStudentsToRelationshipCommand) = {
		cmd.populate()
		form(cmd)
	}

	@RequestMapping(method=Array(POST), params=Array("action=refresh"))
	def form(cmd: OldAllocateStudentsToRelationshipCommand) = {
		cmd.sort()
		Mav("relationships/allocate-old")
	}

	@RequestMapping(method = Array(POST), params = Array("doPreviewSpreadsheetUpload", "action!=refresh"))
	def previewFileUpload(@PathVariable("department") department: Department, @Valid cmd: OldAllocateStudentsToRelationshipCommand, errors: Errors): Mav = {
		if (errors.hasErrors && errors.getFieldErrors.asScala.exists { _.getCode == "file.wrongtype.one" }) {
			form(cmd)
		} else {
			Mav("relationships/upload_preview-old")
		}
	}

	@RequestMapping(method=Array(POST), params=Array("confirmed", "action!=refresh"))
	def submit(@Valid cmd: OldAllocateStudentsToRelationshipCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.relationships(cmd.department, cmd.relationshipType))
		}
	}

	@RequestMapping(method=Array(POST), params=Array("action!=refresh"))
	def seekConfirmation(@Valid cmd: OldAllocateStudentsToRelationshipCommand, errors: Errors): Mav = {
		if (errors.hasErrors) {
			form(cmd)
		} else {

			val changed = cmd.studentsWithTutorChanged.map { case (student, tutorInfo) => (student.universityId , tutorInfo.tutorsAfter.toSeq) }
			val added = cmd.studentsWithTutorAdded.map { case (student, tutorInfo) => (student.universityId , tutorInfo.tutorsAfter.toSeq) }

			Mav("relationships/seek_confirmation-old",
				"studentNameMap" -> cmd.studentsWithTutorChangedOrAdded.map { case (student, tutorInfo) => (student.universityId, student.fullName)},
				"changed" -> changed,
				"added" -> added
			)
		}
	}

	@ModelAttribute("templateCommand")
	def templateCommand(@PathVariable department: Department, @PathVariable relationshipType: StudentRelationshipType) =
		OldTransientStudentRelationshipTemplateCommand(department, relationshipType)

	@RequestMapping(method=Array(POST), params=Array("template", "!action"))
	def template(@ModelAttribute("templateCommand") cmd: Appliable[ExcelView]) = cmd.apply()

}