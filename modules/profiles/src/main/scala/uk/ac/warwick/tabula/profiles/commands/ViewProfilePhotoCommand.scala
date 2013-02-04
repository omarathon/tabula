package uk.ac.warwick.tabula.profiles.commands
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.springframework.util.FileCopyUtils
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands.ApplyWithCallback
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.fileserver.RenderableAttachment
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.commands.Unaudited

class ViewProfilePhotoCommand(val member: Member) extends Command[RenderableFile] with ReadOnly with ApplyWithCallback[RenderableFile] with Unaudited {
	
	PermissionCheck(Permissions.Profiles.Read, member)
	
	private val DefaultPhoto = new DefaultPhoto
	private var fileFound: Boolean = _
	
	override def applyInternal() = {
		val attachmentOption = Option(member.photo) map { a => new Photo(a) }
		val attachment = attachmentOption match {
			case Some(photo) => photo.inputStream match {
				case null => DefaultPhoto
				case _ => photo
			}
			case None => DefaultPhoto
		}
		
		if (callback != null) callback(attachment)
		
		attachment
	}
	
	override def describe(d: Description) = d.member(member)
	override def describeResult(d: Description) { d.property("fileFound", fileFound) }

}

class Photo(attachment: FileAttachment) extends RenderableAttachment(attachment: FileAttachment) {
	override def contentType = "image/jpeg"
}

class DefaultPhoto extends RenderableFile {
	private def read() = {
		val is = getClass.getResourceAsStream("/no-photo.png")
		val os = new ByteArrayOutputStream
		
		FileCopyUtils.copy(is, os)
		os.toByteArray
	}
	
	// TODO is keeping this in memory the right thing to do? It's only 3kb
	private val NoPhoto = read()
	
	override def inputStream = new ByteArrayInputStream(NoPhoto)
	override def filename = "no-photo.png"
	override def contentType = "image/png"
	override def contentLength = Some(NoPhoto.length)
	override def file = None
}