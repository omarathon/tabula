package uk.ac.warwick.tabula.commands

import java.io.{File, InputStream}

import com.google.common.base.Optional
import com.google.common.io.ByteSource
import org.springframework.validation.BindingResult
import org.springframework.web.multipart.MultipartFile
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.RequestInfo
import uk.ac.warwick.tabula.data.FileDao
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{MaintenanceModeEnabledException, MaintenanceModeService}
import uk.ac.warwick.tabula.system.{BindListener, NoBind}
import uk.ac.warwick.util.virusscan.{VirusScanResult, VirusScanService}

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Encapsulates initially-uploaded MultipartFiles with a reference to
  * FileAttachments to hold on to the ID of a file on subsequent submissions,
  * so that you don't have to re-upload a file again.
  *
  * When an HTML5 multiple file upload control is used, Spring can bind this
  * to an array or collection of MultipartFile objects. This is why UploadedFile
  * has a collection of MultipartFiles, rather than using a collection of UploadedFile
  * objects.
  *
  * Works well with the filewidget macro in forms.ftl. Remember to set the
  * multipart/form-data encoding type on your form.
  */
class UploadedFile extends BindListener with Logging {
  var fileDao: FileDao = Wire[FileDao]
  var maintenanceMode: MaintenanceModeService = Wire[MaintenanceModeService]
  var virusScanService: VirusScanService = Wire[VirusScanService]

  @NoBind var rejectVirusUploads: Boolean = Wire[String]("${uploads.virusscan.rejectOnVirus:false}").toBoolean
  @NoBind var rejectVirusUploadsOnError: Boolean = Wire[String]("${uploads.virusscan.rejectOnError:false}").toBoolean
  @NoBind var virusScanMaxSize: Long = Wire[String]("${uploads.virusscan.maxSize:2147483648}").toLong

  @NoBind var disallowedFilenames: List[String] = commaSeparated(Wire[String]("${uploads.disallowedFilenames}"))
  @NoBind var disallowedPrefixes: List[String] = commaSeparated(Wire[String]("${uploads.disallowedPrefixes}"))

  // files bound from an upload request, prior to being persisted by `onBind`.
  var upload: JList[MultipartFile] = JArrayList()

  // files that have been persisted - can be represented in forms by ID
  var attached: JList[FileAttachment] = JArrayList()

  def uploadedFileNames: Seq[String] = upload.asScala.toSeq.map(_.getOriginalFilename).filterNot(_ == "")

  def attachedFileNames: Seq[String] = attached.asScala.toSeq.map(_.getName)

  def fileNames: Seq[String] = uploadedFileNames ++ attachedFileNames

  def isMissing: Boolean = !isExists

  def isExists: Boolean = hasUploads || hasAttachments

  def size: Int =
    if (hasAttachments) attached.size
    else if (hasUploads) permittedUploads.size
    else 0

  def attachedOrEmpty: JList[FileAttachment] = Option(attached).getOrElse(JArrayList())

  def uploadOrEmpty: JList[MultipartFile] = permittedUploads

  def hasAttachments: Boolean = attached != null && !attached.isEmpty

  def hasUploads: Boolean = !permittedUploads.isEmpty

  /** Uploads excluding those that are empty or have bad names. */
  def permittedUploads: JList[MultipartFile] = {
    upload.asScala.filterNot { s =>
      s.isEmpty ||
        (disallowedFilenames contains s.getOriginalFilename) ||
        (disallowedPrefixes exists s.getOriginalFilename.startsWith)
    }.asJava
  }

  def isUploaded: Boolean = hasUploads

  def individualFileSizes: Seq[(String, Long)] =
    upload.asScala.toSeq.map(u => (u.getOriginalFilename, u.getSize)) ++
      attached.asScala.map(a => (a.getName, a.length.getOrElse(0L)))

  /**
    * Performs persistence of uploads such as converting MultipartFiles
    * into FileAttachments saves to the database. When first saved, these
    * FileAttachments are marked as "temporary" until persisted by whatever
    * command needs them. This method will throw an exception
    */
  override def onBind(result: BindingResult): Unit = {
    if (maintenanceMode.enabled) {
      throw new MaintenanceModeEnabledException(maintenanceMode.until, maintenanceMode.message)
    }

    val bindResult = for (item <- attached.asScala) yield {
      if (item != null && !item.temporary) {
        result.reject("binding.reSubmission")
        false
      } else true
    }

    // Early exit if we've failed binding
    if (!bindResult.contains(false)) {
      if (hasUploads) {
        // Virus scan
        permittedUploads.asScala.zipWithIndex.foreach { case (item, index) =>
          val byteSource = MultipartFileByteSource(item)
          if (byteSource.size() > virusScanMaxSize) {
            logger.warn(s"Not virus scanning ${item.getOriginalFilename}${RequestInfo.fromThread.map { info => s" by ${info.user.fullName} (${info.user.userId})" }.getOrElse("")}: size exceeds max ${byteSource.size()} > $virusScanMaxSize")
          } else {
            val virusScanResult = Await.result(virusScanService.scan(byteSource).toScala, Duration.Inf)
            virusScanResult.getStatus match {
              case VirusScanResult.Status.clean =>
              // Do nothing

              case VirusScanResult.Status.virus =>
                logger.warn(s"Virus uploaded${RequestInfo.fromThread.map { info => s" by ${info.user.fullName} (${info.user.userId})" }.getOrElse("")}: ${item.getOriginalFilename} (${virusScanResult.getVirus.get})")

                if (rejectVirusUploads) {
                  result.rejectValue(s"upload[$index]", "file.virus", Array(item.getOriginalFilename, virusScanResult.getVirus.get()), s"The submitted file ${item.getOriginalFilename} contains a virus ${virusScanResult.getVirus.get()}.")
                }
              case VirusScanResult.Status.error =>
                logger.warn(s"Error calling virus scan service for ${item.getOriginalFilename} (${virusScanResult.getError.get})")

                if (rejectVirusUploadsOnError) {
                  result.rejectValue(s"upload[$index]", "file.virus", Array(item.getOriginalFilename, virusScanResult.getError.get()), s"There was an error calling the virus scan service for ${item.getOriginalFilename}: ${virusScanResult.getError.get()}.")
                }
            }
          }
        }

        // convert MultipartFiles into FileAttachments
        transactional() {
          val newAttachments = for (item <- permittedUploads.asScala) yield {
            val a = new FileAttachment
            a.name = new File(item.getOriginalFilename).getName
            a.uploadedData = MultipartFileByteSource(item)
            RequestInfo.fromThread.foreach { info => a.uploadedBy = info.user.userId }
            fileDao.saveTemporary(a)
            a
          }
          // remove converted files from upload to avoid duplicates
          upload.clear()
          attached.addAll(newAttachments.asJava)
        }
      } else {
        // sometimes we manually add FileAttachments with uploaded data to persist
        for (item <- attached.asScala if item.uploadedData != null)
          fileDao.saveTemporary(item)
      }
    }
  }

  private def commaSeparated(csv: String) =
    if (csv == null) Nil
    else csv.split(",").toList
}

case class MultipartFileByteSource(file: MultipartFile) extends ByteSource {
  override def openStream(): InputStream = file.getInputStream
  override lazy val sizeIfKnown: Optional[JLong] = Optional.of(file.getSize)
  override lazy val isEmpty: Boolean = file.isEmpty
}
