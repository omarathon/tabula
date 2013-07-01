package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.util.files.imageresize.CachingImageResizer
import uk.ac.warwick.util.files.imageresize.JAIImageResizer
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.FileAttachment
import org.springframework.core.io.ClassPathResource
import java.io.File
import uk.ac.warwick.tabula.JavaImports._

class ResizesPhotoTest extends TestBase {
	
	trait Context {
		val directory = createTemporaryDirectory
		val resizer = new CachingImageResizer(new JAIImageResizer, directory);
		val command = new ResizesPhoto {
			override val imageResizer = resizer
		}
		
		val member = new StudentMember
		val photoAttachment = new FileAttachment
		photoAttachment.file = new ClassPathResource("/dijkstra.jpg").getFile
		photoAttachment.file.exists should be (true)
		photoAttachment.name = "0123456.jpg"
		member.photo = photoAttachment
		
		def fileList: Array[File] = {
			val profilesDirectory = new File(directory, "profilephoto")
			if (profilesDirectory.exists) profilesDirectory.listFiles()
			else Array()
		}
	}
	
	@Test
	def nullAttachment() {
		new Context {
			member.photo = null
			command.size = "actual"
			val renderable = command.render(Some(member))
			renderable should be (command.DefaultPhoto)
		}
	}
	
	@Test
	def thumbnail() {
		new Context {
			command.size = "thumbnail"
			val renderable = command.render(Some(member))
			val files = fileList
			files.length should be (1)
			files(0).getName should be ("0123456.jpg@170x0")
		}
	}
	
	@Test
	def actual() {
		new Context {
			command.size = "actual"
			val renderable = command.render(Some(member))
			renderable.contentLength.get should be > 0L
			val files = fileList
			files.length should be (0L)
		}
	}
	
	@Test
	def unrecognisedSize() {
		new Context {
			command.size = "winning" // should be ignored, use actual size
			val renderable = command.render(Some(member))
			renderable.contentLength.get should be > 0L
			val files = fileList
			files.length should be (0L)
		}
	}
}