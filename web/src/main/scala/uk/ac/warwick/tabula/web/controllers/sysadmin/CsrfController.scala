package uk.ac.warwick.tabula.web.controllers.sysadmin

import javax.validation.Valid
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{GetMapping, ModelAttribute, PostMapping, RequestMapping}
import uk.ac.warwick.tabula.commands.{ComposableCommand, CsrfEnforceCommand, CsrfEnforceCommandInternal, PopulateOnForm}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.web.Mav

@Controller
@RequestMapping(Array("/sysadmin/csrf"))
class CsrfController extends BaseSysadminController
  with AutowiringCsrfServiceComponent {

  type Command = ComposableCommand[Unit] with CsrfEnforceCommandInternal with PopulateOnForm

  @ModelAttribute("command")
  def command: Command = {
    val cmd = CsrfEnforceCommand.apply()
    cmd.populate()
    cmd
  }

  @GetMapping
  def home: Mav = Mav("sysadmin/csrf")

  @PostMapping
  def update(@Valid @ModelAttribute("command") command: Command): Mav = {
    command.apply()

    Redirect("/sysadmin/csrf")
  }
}

