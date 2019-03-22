package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.FileAttachment
import uk.ac.warwick.tabula.data.{AutowiringFileDaoComponent, FileDaoComponent}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.sysadmin.ObjectStorageSysadminCommand._

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
      "attachments" -> command.apply()
    )

  @GetMapping(value = Array("/{attachment}"))
  def download(@PathVariable attachment: FileAttachment): RenderableFile =
    new RenderableAttachment(attachment)

}

object ObjectStorageSysadminCommand {
  type Result = Seq[FileAttachment]
  type Command = Appliable[Result] with ObjectStorageSysadminCommandRequest

  def apply(): Command =
    new ObjectStorageSysadminCommandInternal
      with ComposableCommand[Result]
      with ReadOnly with Unaudited
      with ObjectStorageSysadminCommandRequest
      with ObjectStorageSysadminCommandPermissions
      with AutowiringFileDaoComponent
}

abstract class ObjectStorageSysadminCommandInternal extends CommandInternal[Result] {
  self: ObjectStorageSysadminCommandRequest with FileDaoComponent =>

  override protected def applyInternal(): Result =
    ids.split('\n').map(_.safeTrim).filter(_.hasText).flatMap(fileDao.getFileById)

}

trait ObjectStorageSysadminCommandRequest {
  var ids: String = _
}

trait ObjectStorageSysadminCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  override def permissionsCheck(p: PermissionsChecking): Unit =
    p.PermissionCheck(Permissions.ViewObjectStorage)
}