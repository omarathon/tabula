package uk.ac.warwick.tabula.web.controllers.sysadmin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.sysadmin._
import uk.ac.warwick.tabula.data.model.OriginalityReport
import uk.ac.warwick.tabula.web.Mav


@Controller
@RequestMapping(value = Array("/sysadmin/tca/check-in-progress"))
class CheckTCAInProgressController extends BaseSysadminController {

  @ModelAttribute("checkTCAInProgressCommand") def checkTCAInProgressCommand = CheckTCAInProgressCommand()

  @RequestMapping(method = Array(GET, HEAD))
  def form(): Mav = Mav("sysadmin/tca/check-in-progress")

  @RequestMapping(method = Array(POST))
  def check(@ModelAttribute("checkTCAInProgressCommand") cmd: Appliable[Seq[OriginalityReport]]): Mav =
    Mav("sysadmin/tca/check-in-progress", "result" -> cmd.apply())

}
