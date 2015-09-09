package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.commands.profiles.DefaultPhoto
import uk.ac.warwick.tabula.{Mockito, ItemNotFoundException, TestBase}
import uk.ac.warwick.tabula.data.model.{FileAttachment, StudentMember, Member}
import scala.language.reflectiveCalls

class ViewProfilePhotoCommandTest extends TestBase with Mockito {

	@Test(expected=classOf[ItemNotFoundException])
	def memberDoesNotExist() {
		new ViewProfilePhotoCommand(null: Member)
	}

	@Test
	def memberPhoto() {
		val member = new StudentMember()
		member.universityId = "1170836"
		val command = new ViewProfilePhotoCommand(member) {}
		val mav = command.applyInternal()
		mav.viewName should be ("redirect:https://$%7Bphotos.host%7D/$%7Bphotos.applicationId%7D/photo/a4ad3c80fd4bb5681b53c29f702abe8c/1170836")
	}

}
