package uk.ac.warwick.courses.services
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import scala.collection.JavaConversions.asScalaBuffer
import org.hibernate.annotations.AccessType
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Service
import javax.persistence.Entity
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Feedback
import uk.ac.warwick.courses.helpers.Logging
import org.apache.commons.io.input.BoundedInputStream

/**
 * FIXME this could generate a corrupt file if two requests tried to generate the same zip simultaneously
 * 		 or if a feedback upload invalidates a zip while one is being generated/downloaded
 */
@Service
class ZipService extends InitializingBean with ZipCreator with Logging {
	@Value("${filesystem.zip.dir}") var zipDir:File =_ 
	@Value("${filesystem.create.missing}") var createMissingDirectories:Boolean =_
	
	val idSplitSize = 4
	
	logger.info("Creating ZipService")
	
	override def afterPropertiesSet {
		if (!zipDir.exists) {
			if (createMissingDirectories) {
				zipDir.mkdirs
			} else {
				throw new IllegalStateException("zip dir '%s' does not exist" format zipDir)
			}
		}
	}
	
	def partition(id:String): String = id.replace("-","").grouped(idSplitSize).mkString("/")
	
	def resolvePath(feedback:Feedback): String = "feedback/" + partition(feedback.id)
	def resolvePathForFeedback(assignment:Assignment): String = "all-feedback/" + partition(assignment.id)
	
	def invalidateFeedbackZip(assignment:Assignment) = invalidate(resolvePathForFeedback(assignment))
	
	def getFeedbackZip(feedback:Feedback): File = 
		getZip( resolvePath(feedback), getFeedbackZipItems(feedback))
	
	private def getFeedbackZipItems(feedback:Feedback): Seq[ZipItem] =
		feedback.attachments.map { (attachment) => 
			new ZipFileItem(feedback.universityId+" - "+attachment.name, attachment.dataStream)	
		}
	
	/**
	 * A zip of feedback with a folder for each student.
	 */
	def getAllFeedbackZips(assignment:Assignment): File = {
		getZip( resolvePathForFeedback(assignment),
			assignment.feedbacks.map { (feedback) => 
				new ZipFolderItem( feedback.universityId, getFeedbackZipItems(feedback) )
			}
		)
	}
	
}

/**
 * InputStream to read a single zip entry from a parent input stream.
 */
class ZipEntryInputStream(val zip:InputStream, val entry:ZipEntry)
		extends BoundedInputStream(zip, entry.getSize) {
	setPropagateClose(false)
}

object Zips {
	
   def each(zip:ZipInputStream)(fn: (ZipEntry)=>Unit):Unit = map(zip)(fn)
   
   /**
    * Provides an iterator for ZipEntry items which will be closed when you're done with them.
    * The object returned from the function is converted to a list to guarantee that it's evaluated before closing.
    */
   def iterator[T](zip:ZipInputStream)(fn: (Iterator[ZipEntry])=>Iterator[T]): List[T] = ensureClose(zip) {
	   fn( Iterator.continually{zip.getNextEntry}.takeWhile{_ != null} ).toList
   }
   
   def map[T](zip:ZipInputStream)(fn: (ZipEntry)=>T): Seq[T] = ensureClose(zip) {
	  Iterator.continually{zip.getNextEntry}.takeWhile{_ != null}.map { (item) =>
	 	  val t = fn(item)
	 	  zip.closeEntry
	 	  t
	  }.toList // use toList to evaluate items now, before we actually close the stream
   }

   def ensureClose[T](c:Closeable)(fn: =>T): T = try { fn } finally { c.close }
   
}