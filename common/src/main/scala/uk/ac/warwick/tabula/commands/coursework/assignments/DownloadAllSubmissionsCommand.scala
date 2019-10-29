package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.{Description, _}
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.ZipService
import uk.ac.warwick.tabula.services.fileserver.RenderableFile

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class DownloadAllSubmissionsCommand(
  val module: Module,
  val assignment: Assignment,
  val filename: String)
  extends Command[RenderableFile] with ReadOnly {

  mustBeLinked(assignment, module)
  PermissionCheck(Permissions.Submission.Read, assignment)

  var zipService: ZipService = Wire.auto[ZipService]

  override def applyInternal(): RenderableFile = Await.result(zipService.getAllSubmissionsZip(assignment), Duration.Inf)

  override def describe(d: Description): Unit =
    d.assignment(assignment)
     .submissions(assignment.submissions.asScala.toSeq)

}
