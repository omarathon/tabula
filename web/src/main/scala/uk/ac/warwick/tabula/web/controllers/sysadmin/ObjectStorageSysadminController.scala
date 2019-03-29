package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.{AutowiringFileDaoComponent, FileDaoComponent}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.services.objectstore.{AutowiringObjectStorageServiceComponent, ObjectStorageServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.sysadmin.ObjectStorageSysadminCommand.ResolvedFile

import scala.concurrent.Await
import scala.concurrent.duration.Duration

@Controller
@RequestMapping(value = Array("/sysadmin/objectstorage"))
class ObjectStorageSysadminController extends BaseSysadminController {

  @ModelAttribute("command")
  def command(): ObjectStorageSysadminCommand.Command = ObjectStorageSysadminCommand()

  @GetMapping
  def form(): Mav = Mav("sysadmin/objectstorage/form")

  @PostMapping
  def results(@ModelAttribute("command") command: ObjectStorageSysadminCommand.Command): Mav =
    Mav("sysadmin/objectstorage/results",
      "results" -> command.apply()
    )

}

@Controller
@RequestMapping(value = Array("/sysadmin/objectstorage/{id}"))
class ObjectStorageSysadminDownloadController extends BaseSysadminController {

  @ModelAttribute("command")
  def command(@PathVariable id: String): ObjectStorageSysadminDownloadCommand.Command =
    ObjectStorageSysadminDownloadCommand(id)

  @GetMapping
  def download(@ModelAttribute("command") command: ObjectStorageSysadminDownloadCommand.Command): RenderableFile =
    command.apply()

}

object ObjectStorageSysadminCommand {
  case class ResolvedFile(id: String, attachment: Option[FileAttachment])

  type Result = Seq[ResolvedFile]
  type Command = Appliable[Result] with ObjectStorageSysadminCommandRequest

  def apply(): Command =
    new ObjectStorageSysadminCommandInternal
      with ComposableCommand[Result]
      with ReadOnly with Unaudited
      with ObjectStorageSysadminCommandRequest
      with ObjectStorageSysadminCommandPermissions
      with ObjectStorageSysadminCommandFileResolver
      with AutowiringFileDaoComponent
      with AutowiringObjectStorageServiceComponent
}

abstract class ObjectStorageSysadminCommandInternal extends CommandInternal[ObjectStorageSysadminCommand.Result] {
  self: ObjectStorageSysadminCommandRequest
    with ObjectStorageSysadminCommandFileResolver
    with FileDaoComponent
    with ObjectStorageServiceComponent =>

  override protected def applyInternal(): ObjectStorageSysadminCommand.Result =
    ids.split('\n').map(_.safeTrim).filter(_.hasText).flatMap(resolve)

}

trait ObjectStorageSysadminCommandRequest {
  var ids: String = _
}

trait ObjectStorageSysadminCommandFileResolver {
  self: FileDaoComponent with ObjectStorageServiceComponent =>

  def resolve(id: String): Option[ResolvedFile] =
    fileDao.getFileById(id).map { a => ResolvedFile(id, Some(a)) }.orElse {
      if (Await.result(objectStorageService.keyExists(id), Duration.Inf)) Some(ResolvedFile(id, None))
      else None
    }
}

trait ObjectStorageSysadminCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(Permissions.ViewObjectStorage)
}

object ObjectStorageSysadminDownloadCommand {
  type Result = RenderableFile
  type Command = Appliable[Result]

  def apply(id: String): Command =
    new ObjectStorageSysadminDownloadCommandInternal(id)
      with ComposableCommand[Result]
      with ReadOnly with Unaudited
      with ObjectStorageSysadminCommandPermissions
      with ObjectStorageSysadminCommandFileResolver
      with AutowiringFileDaoComponent
      with AutowiringObjectStorageServiceComponent
}

abstract class ObjectStorageSysadminDownloadCommandInternal(id: String) extends CommandInternal[ObjectStorageSysadminDownloadCommand.Result] {
  self: ObjectStorageSysadminCommandFileResolver
    with FileDaoComponent
    with ObjectStorageServiceComponent =>

  override protected def applyInternal(): ObjectStorageSysadminDownloadCommand.Result =
    resolve(id) match {
      case Some(ResolvedFile(_, Some(attachment))) => new RenderableAttachment(attachment)
      case Some(ResolvedFile(key, _)) => Await.result(objectStorageService.renderable(key, None), Duration.Inf).getOrElse(throw new ItemNotFoundException)
      case _ => throw new ItemNotFoundException
    }

}