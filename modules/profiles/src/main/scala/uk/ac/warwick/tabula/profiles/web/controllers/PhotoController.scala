package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.profiles.commands.ViewProfilePhotoCommand
import uk.ac.warwick.tabula.services.fileserver.FileServer
import org.springframework.web.bind.annotation.RequestMethod
import uk.ac.warwick.tabula.profiles.commands.ViewStudentRelationshipPhotoCommand
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

@Controller
@RequestMapping(value = Array("/view/photo/{member}.jpg"))
class PhotoController extends ProfilesController {

	var fileServer = Wire.auto[FileServer]

	@ModelAttribute("viewProfilePhotoCommand") def command(@PathVariable("member") member: Member) = new ViewProfilePhotoCommand(member)

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getPhoto(@ModelAttribute("viewProfilePhotoCommand") command: ViewProfilePhotoCommand)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		// specify callback so that audit logging happens around file serving
		command.apply { (renderable) => fileServer.stream(renderable) }
	}

}

@Controller
@RequestMapping(value = Array("/view/photo/{sprCode}/{relationshipType}/{agent}.jpg"))
class StudentRelationshipPhotoController extends ProfilesController {

	var fileServer = Wire[FileServer]

	@ModelAttribute("viewStudentRelationshipPhotoCommand")
	def command(
		@PathVariable("sprCode") sprCode: String,
		@PathVariable("relationshipType") relationshipType: StudentRelationshipType,
		@PathVariable("agent") agent: String) = {
			val relationships = relationshipService.findCurrentRelationships(relationshipType, sprCode)
			val relationship = relationships.find(_.agent == agent) getOrElse(throw new ItemNotFoundException)

			var cmd = profileService.getStudentBySprCode(sprCode) match {
				case Some(student: Member) => {
					new ViewStudentRelationshipPhotoCommand(student, relationship)
				}
				case _ => {
					throw new IllegalStateException("Failed to resolve SPR code " + sprCode + " to a student")
					null
				}
			}
	}

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD))
	def getPhoto(@ModelAttribute("viewStudentRelationshipPhotoCommand") command: ViewStudentRelationshipPhotoCommand)(implicit request: HttpServletRequest, response: HttpServletResponse): Unit = {
		// specify callback so that audit logging happens around file serving
		command.apply { (renderable) => fileServer.stream(renderable) }
	}

}