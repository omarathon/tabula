package uk.ac.warwick.tabula.api.web.controllers.profiles

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.{Command, ReadOnly, Unaudited, ViewViewableCommand}
import uk.ac.warwick.tabula.data.model.{Module, StudentRelationshipType}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.services.RelationshipService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/relationships/agents/{studentRelationshipType}"))
class RelationshipAgentsController extends ApiController {

  @Autowired var relationshipsService: RelationshipService = _

  @ModelAttribute("getCommand")
  def getCommand(@PathVariable studentRelationshipType: StudentRelationshipType): ViewRelationshipAgentsCommand[StudentRelationshipType] =
    new ViewRelationshipAgentsCommand(Permissions.Profiles.StudentRelationship.Read(studentRelationshipType), PermissionsTarget.Global, mandatory(studentRelationshipType))


  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def index(@ModelAttribute("getCommand") cmd: ViewRelationshipAgentsCommand[StudentRelationshipType]): Mav = {
    Mav(new JSONView(
      Map(
        "success" -> true,
        "status" -> "ok",
        "agents" -> relationshipsService.listCurrentRelationshipsGlobally(cmd.apply()).map(sr => Map(
          "firstName" -> sr(1),
          "lastName" -> sr(2),
          "universityId" -> sr(0)
        ))
      )
    ))
  }

  class ViewRelationshipAgentsCommand[A <: PermissionsTarget](val permission: Permission, val target: PermissionsTarget, val value: A) extends Command[A] with ReadOnly with Unaudited {
    PermissionCheck(permission, target)

    override def applyInternal(): A = value
  }

}
