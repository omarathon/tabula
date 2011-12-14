package uk.ac.warwick.courses.services
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import scala.collection.JavaConversions._
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.InitializingBean
import org.springframework.stereotype.Service
import uk.ac.warwick.courses.data.model.Assignment
import uk.ac.warwick.courses.data.model.Feedback
import uk.ac.warwick.courses.helpers.Logging
import collection.mutable.{Seq => MSeq}
import scala.collection.mutable.ListBuffer
import java.io.Closeable

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