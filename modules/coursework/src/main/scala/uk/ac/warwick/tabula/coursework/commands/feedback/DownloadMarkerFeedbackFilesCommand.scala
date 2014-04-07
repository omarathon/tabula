package uk.ac.warwick.tabula.coursework.commands.feedback

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assignment, Module, MarkerFeedback}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.helpers.StringUtils.StringToSuperString
import uk.ac.warwick.tabula.services.fileserver._
import uk.ac.warwick.tabula.services.{FeedbackService, ZipService}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._


class DownloadMarkerFeedbackFilesCommand (val module: Module, val assignment: Assignment, val markerFeedbackId: String) extends Command[Option[RenderableFile]] with ReadOnly {

		var feedbackService = Wire[FeedbackService]

		val markerFeedback = feedbackService.getMarkerFeedbackById(markerFeedbackId).get

		notDeleted(assignment)
		mustBeLinked(assignment, module)
		PermissionCheck(Permissions.Feedback.Read, markerFeedback.feedback)

		var filename: String = _

		var zipService = Wire.auto[ZipService]

		private var fileFound: Boolean = _
		var callback: (RenderableFile) => Unit = _

		/**
		 * If filename is unset, it returns a renderable Zip of all files.
		 * If filename is set, it will return a renderable attachment if found.
		 * In either case if it's not found, None is returned.
		 */
		def applyInternal() = {
			val result: Option[RenderableFile] =
				filename match {
					case filename: String if filename.hasText => {
						markerFeedback.attachments.asScala.find(_.name == filename).map(new RenderableAttachment(_))
					}
					case _ => Some(zipped(markerFeedback))
				}

			fileFound = result.isDefined
			if (callback != null) {
				result.map { callback(_) }
			}
			result
		}

		private def zipped(markerFeedback: MarkerFeedback) = new RenderableZip(zipService.getSomeMarkerFeedbacksZip(Seq(markerFeedback)))

		override def describe(d: Description) = {
			d.property("filename", filename)
		}

		override def describeResult(d: Description) {
			d.property("fileFound", fileFound)
		}
	}