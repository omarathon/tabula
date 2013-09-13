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

		renderable should be (DefaultPhoto)
	}

	@Test
	def photoWithNoStream() {
		val member = new StudentMember()
		//member.photo = mock[FileAttachment] // photo.dataStream should return null
		// can't use a mock because verifying mocked GeneratedIds doesn't work properly
		val p = new FileAttachment(){
			var dataStreamCallCount = 0
			override def dataStream = {
				dataStreamCallCount += 1
				null
			}
		}
		member.photo =p
		val command = new ViewProfilePhotoCommand(member)
		val renderable = command.applyInternal()

		renderable should be (DefaultPhoto)
		p.dataStreamCallCount should be(1)
	}

}
