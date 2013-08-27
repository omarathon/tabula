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
import uk.ac.warwick.tabula.services.fileserver.{CachePolicy, RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model.StudentRelationship
import uk.ac.warwick.spring.Wire
import org.joda.time.{Days, Hours, DateTime}
import uk.ac.warwick.util.files.imageresize.ImageResizer.FileType
import uk.ac.warwick.util.files.FileReference
import java.io.File
import java.io.FileInputStream
import uk.ac.warwick.util.files.imageresize.FileExposingImageResizer
import uk.ac.warwick.util.files.impl.AbstractFileReference
import uk.ac.warwick.util.files.FileData
import uk.ac.warwick.util.files.FileStore.UsingOutput

trait ResizesPhoto {
	var imageResizer = Wire[FileExposingImageResizer]

	val DefaultModified = new DateTime(0)
	
	val THUMBNAIL_SIZE = "thumbnail"
	val TINYTHUMBNAIL_SIZE = "tinythumbnail"
	val ACTUAL_SIZE = "actual"
	
	var size: String = ACTUAL_SIZE
	
	def render(member: Option[Member]): RenderableFile = 
		member map { m =>
			val photo = new Photo(m.photo)
			photo.inputStream match {
				case null => resized(DefaultPhoto, DefaultModified)
				case _ => resized(photo, m.lastUpdatedDate)
			}
		} getOrElse {
			resized(DefaultPhoto, DefaultModified)
		}	
	
	
	def resized(renderable: RenderableFile, lastModified: DateTime): RenderableFile = size match {
		case THUMBNAIL_SIZE => resized(renderable, lastModified, 170)
		case TINYTHUMBNAIL_SIZE => resized(renderable, lastModified, 40)
		case _ => renderable
	}
	
	def resized(renderable: RenderableFile, lastModified: DateTime, maxWidth: Int) = {
		val ref: FileReference = new RenderableFileReference(renderable, "profilephoto")
		val file = imageResizer.getResized(ref, lastModified, maxWidth, 0, FileType.jpg)
		new ResizedPhoto(file)
	}
}

/** Hacky adapter around RenderableFile to implement enough of FileReference for the resizer to accept */
class RenderableFileReference(ref: RenderableFile, prefix: String) extends AbstractFileReference {
	override def getData = new FileData {
		override def delete(): Boolean = ???
		override def getInputStream() = ref.inputStream
		override def getInputStreamSource() = ???
		override def getRealFile() = ???
		override def getRealPath() = ???
		override def isExists(): Boolean = ???
		override def isFileBacked(): Boolean = ???
		override def length(): Long = ???
		override def overwrite(x$1: String) = ??? 
		override def overwrite(x$1: Array[Byte]) = ???
		override def overwrite(x$1: java.io.File) = ???
		override def overwrite(x$1: UsingOutput) = ???
	}
	
	override def isLocal = true
	override def copyTo(ref: FileReference) = ???
	override def renameTo(ref: FileReference) = ???
	override def getHash = null
	override def getPath = s"/${prefix}/${ref.filename}"
	override def unlink() { ??? }
}

class ViewProfilePhotoCommand(val member: Member) extends Command[RenderableFile] with ReadOnly with ApplyWithCallback[RenderableFile] with Unaudited with ResizesPhoto {

	PermissionCheck(Permissions.Profiles.Read.Core, mandatory(member))

	override def applyInternal() = {
		val renderable = render(Option(member))

		if (callback != null) callback(renderable)
		renderable
	}

	override def describe(d: Description) = d.member(member)

}

class ViewStudentRelationshipPhotoCommand(val member: Member, val relationship: StudentRelationship) extends Command[RenderableFile] with ReadOnly with ApplyWithCallback[RenderableFile] with Unaudited  with ResizesPhoto {
	
	PermissionCheck(Permissions.Profiles.StudentRelationship.Read(relationship.relationshipType), member)

	override def applyInternal() = {
		val attachment = render(relationship.agentMember)

		if (callback != null) callback(attachment)

		attachment
	}

	override def describe(d: Description) = d.member(member).property("relationship" -> relationship)

}

trait PhotoCachePolicy { self: RenderableFile =>
	// Let browsers cache photos for a couple of hours
	override def cachePolicy = CachePolicy(expires = Some(Hours.TWO))
}

class ResizedPhoto(f: File) extends RenderableFile with PhotoCachePolicy {
	override def contentType = "image/jpeg"
	override def inputStream = new FileInputStream(f)
	override def filename = f.getName
	override def contentLength = Some(f.length)
	override def file = Some(f)
}

class Photo(attachment: FileAttachment) extends RenderableAttachment(attachment: FileAttachment) with PhotoCachePolicy {
	override def contentType = "image/jpeg"
}

object DefaultPhoto extends RenderableFile {
	private def read() = {
		val is = getClass.getResourceAsStream("/no-photo.jpg")
		val os = new ByteArrayOutputStream

		FileCopyUtils.copy(is, os)
		os.toByteArray
	}

	// TODO is keeping this in memory the right thing to do? It's only 3kb
	private val NoPhoto = read()

	override def inputStream = new ByteArrayInputStream(NoPhoto)
	override def filename = "no-photo.jpg"
	override def contentType = "image/jpg"
	override def contentLength = Some(NoPhoto.length)
	override def file = None

	override def cachePolicy = CachePolicy(expires = Some(Days.ONE))
}