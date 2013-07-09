package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.{Mockito, ItemNotFoundException, TestBase}
import uk.ac.warwick.tabula.data.model.{FileAttachment, StudentMember, Member}

class ViewProfilePhotoCommandTest extends TestBase with Mockito {

	@Test(expected=classOf[ItemNotFoundException])
	def memberDoesNotExist() {
		new ViewProfilePhotoCommand(null: Member)
	}

	@Test
	def memberWithNoPhoto() {
		val member = new StudentMember()
		member.photo = null
		val command = new ViewProfilePhotoCommand(member)
		val renderable = command.applyInternal()

		renderable should be (command.DefaultPhoto)
	}

	@Test
	def photoWithNoStream() {
		val member = new StudentMember()
		member.photo = mock[FileAttachment] // photo.dataStream should return null
		val command = new ViewProfilePhotoCommand(member)
		val renderable = command.applyInternal()

		renderable should be (command.DefaultPhoto)
		there was one(member.photo).dataStream
	}

}
