package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.sysadmin.CheckMitCircsEnabledCommand
import uk.ac.warwick.tabula.services.AutowiringRelationshipServiceComponent
import uk.ac.warwick.tabula.web.Mav


@Controller
@RequestMapping(value = Array("/sysadmin/mitcircs/check"))
class CheckMitCircsEnabledController extends BaseSysadminController with AutowiringRelationshipServiceComponent {

  @ModelAttribute("checkMitCircsEnabledCommand") def checkMitCircsEnabledCommand: CheckMitCircsEnabledCommand.Command  = CheckMitCircsEnabledCommand()
  
  @RequestMapping(method = Array(GET, HEAD))
  def form(@ModelAttribute("checkMitCircsEnabledCommand") checkMitCircsEnabledCommand: CheckMitCircsEnabledCommand.Command): Mav =
    Mav("sysadmin/mitcircs_enabled", "unableToSubmit" -> checkMitCircsEnabledCommand.apply())

}