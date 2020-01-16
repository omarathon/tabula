package uk.ac.warwick.tabula.web.controllers.cm2

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.{CourseworkHomepageCommand, CourseworkMarkerHomepageCommand}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, BaseController}

@Controller
@RequestMapping(Array("/coursework"))
class HomeController extends CourseworkController with AutowiringUserSettingsServiceComponent {

  hideDeletedItems

  @ModelAttribute("command")
  def command(user: CurrentUser): CourseworkHomepageCommand.Command = {
    CourseworkHomepageCommand(user)
  }

  @RequestMapping
  def home(@ModelAttribute("command") command: CourseworkHomepageCommand.Command): Mav = {
    val info = command.apply()

    Mav("cm2/home/view",
      "homeDepartment" -> info.homeDepartment,
      "studentInformation" -> info.studentInformation,
      "markingAcademicYears" -> info.markingAcademicYears,
      "activeAcademicYear" -> userSettingsService.getByUserId(user.apparentId).flatMap(_.activeAcademicYear).orElse(info.markingAcademicYears.lastOption).getOrElse(AcademicYear.now()),
      "adminInformation" -> info.adminInformation,
      "embedded" -> false
    )
  }

}

abstract class AbstractMarkerHomeController extends CourseworkController
  with AcademicYearScopedController with AutowiringMaintenanceModeServiceComponent with AutowiringUserSettingsServiceComponent {

  hideDeletedItems

  @ModelAttribute("command")
  def command(@ModelAttribute("activeAcademicYear") academicYear: Option[AcademicYear]): CourseworkMarkerHomepageCommand.Command =
    CourseworkMarkerHomepageCommand(user, academicYear.getOrElse(AcademicYear.now()))

  @RequestMapping
  def markerHome(
    @ModelAttribute("command") command: CourseworkMarkerHomepageCommand.Command,
    @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear],
  ): Mav = {
    val academicYear = activeAcademicYear.getOrElse(AcademicYear.now())

    Mav("cm2/home/_marker",
      "academicYear" -> academicYear,
      "markerInformation" -> command.apply(),
      "embedded" -> ajax
    ).noLayoutIf(ajax).secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.marker.forYear(year)): _*)
  }

}

@Controller
@RequestMapping(Array("/coursework/marker"))
class MarkerHomeController extends AbstractMarkerHomeController {

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear: Option[AcademicYear] = retrieveActiveAcademicYear(None)

}

@Controller
@RequestMapping(Array("/coursework/marker/{academicYear}"))
class MarkerHomeForYearController extends AbstractMarkerHomeController {

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
    retrieveActiveAcademicYear(Option(academicYear))

}

@Controller
@RequestMapping(Array("/coursework/{academicYear:\\d{4}}", "/coursework/admin", "/coursework/admin/department", "/coursework/submission", "/coursework/module/**"))
class HomeRewritesController extends BaseController {

  @RequestMapping
  def rewriteToHome: Mav = Redirect(Routes.home)

}