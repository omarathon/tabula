package uk.ac.warwick.tabula.profiles.web.controllers

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestMethod}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.profiles.{ViewProfilePhotoCommand, ViewStudentRelationshipPhotoCommand}
import uk.ac.warwick.tabula.data.model.{Member, StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.services.fileserver.FileServer
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(value = Array("/view/photo/{member}.jpg"))
class PhotoController extends ProfilesController {

	var fileServer = Wire.auto[FileServer]

	@ModelAttribute("viewProfilePhotoCommand") def command(@PathVariable member: Member) =
		ViewProfilePhotoCommand(member)

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD), produces = Array(MediaType.IMAGE_JPEG_VALUE))
	def getPhoto(@ModelAttribute("viewProfilePhotoCommand") command: ViewProfilePhotoCommand)
		(implicit request: HttpServletRequest, response: HttpServletResponse): Mav = {
		command.apply()
	}

}

@Controller
@RequestMapping(value = Array("/view/photo/{member}/{relationshipType}/{agent}.jpg"))
class StudentRelationshipPhotoController extends ProfilesController {

	var fileServer = Wire[FileServer]

	@ModelAttribute("viewStudentRelationshipPhotoCommand")
	def command(
		@PathVariable member: Member,
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable agent: String
	) = {
		mandatory(member) match {
			case student: StudentMember =>
				val relationships =
					relationshipService.getAllPastAndPresentRelationships(student)
						.filter { rel => rel.relationshipType == mandatory(relationshipType) && rel.agent == agent }

				val relationship = relationships.find { _.isCurrent }.orElse(relationships.headOption)

				new ViewStudentRelationshipPhotoCommand(student, relationship.getOrElse { throw new ItemNotFoundException })
			case _ =>
				throw new ItemNotFoundException
		}
	}

	@RequestMapping(method = Array(RequestMethod.GET, RequestMethod.HEAD), produces = Array(MediaType.IMAGE_JPEG_VALUE))
	def getPhoto(@ModelAttribute("viewStudentRelationshipPhotoCommand") command: ViewStudentRelationshipPhotoCommand)
		(implicit request: HttpServletRequest, response: HttpServletResponse): Mav = {
		command.apply()
	}

}