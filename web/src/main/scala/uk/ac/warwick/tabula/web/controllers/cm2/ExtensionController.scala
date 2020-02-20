package uk.ac.warwick.tabula.web.controllers.cm2

import com.fasterxml.jackson.databind.ObjectMapper
import javax.validation.Valid
import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.{BindingResult, Errors}
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, _}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments.extensions.{EditExtensionCommand, _}
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.forms.{Extension, ExtensionState}
import uk.ac.warwick.tabula.data.model.{Assignment, Department, StudentMember}
import uk.ac.warwick.tabula.helpers.DateBuilder
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.fileserver.{RenderableAttachment, RenderableFile}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.userlookup.User

trait ExtensionServices {
  var json: ObjectMapper = Wire[ObjectMapper]
  var userLookup: UserLookupService = Wire[UserLookupService]
  var relationshipService: RelationshipService = Wire[RelationshipService]
  var profileService: ProfileService = Wire[ProfileService]

  class ExtensionMap(extension: Extension) {
    def asMap: Map[String, String] = {

      def convertDateToString(date: Option[DateTime]) = date.map(DateBuilder.format(_)).getOrElse("")

      def convertDateToMillis(date: Option[DateTime]) = date.map(_.getMillis.toString).orNull

      Map(
        "id" -> extension.universityId.getOrElse(""),
        "usercode" -> extension.usercode,
        "status" -> extension.state.description,
        "requestedExpiryDate" -> convertDateToString(extension.requestedExpiryDate),
        "expiryDate" -> convertDateToString(extension.expiryDate),
        "expiryDateMillis" -> convertDateToMillis(extension.expiryDate),
        "extensionDuration" -> extension.duration.toString,
        "requestedExtraExtensionDuration" -> extension.requestedExtraDuration.toString,
        "reviewerComments" -> extension.reviewerComments
      )
    }
  }

  import scala.language.implicitConversions

  implicit def asMap(e: Extension): ExtensionMap = new ExtensionMap(e)
}

abstract class AbstractFilterExtensionsController extends CourseworkController
  with AcademicYearScopedController with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent {

  type FilterExtensionsCommand = Appliable[FilterExtensionResults] with FilterExtensionsState

  @ModelAttribute("filterExtensionsCommand")
  def filterCommand(@ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear], user: CurrentUser) =
    FilterExtensionsCommand(activeAcademicYear.getOrElse(AcademicYear.now()), user)

  @RequestMapping(params = Array("!ajax"), headers = Array("!X-Requested-With"))
  def viewForm(@ModelAttribute("filterExtensionsCommand") cmd: FilterExtensionsCommand): Mav = {
    val results = cmd.apply()
    Mav("cm2/admin/extensions/list",
      "academicYear" -> cmd.academicYear,
      "command" -> cmd,
      "results" -> results)
      .secondCrumbs(academicYearBreadcrumbs(cmd.academicYear)(Routes.admin.extensions.apply): _*)
  }

  @RequestMapping
  def listFilterResults(@ModelAttribute("filterExtensionsCommand") cmd: FilterExtensionsCommand): Mav = {
    val results = cmd.apply()
    Mav("cm2/admin/extensions/_filter_results",
      "academicYear" -> cmd.academicYear,
      "command" -> cmd,
      "results" -> results
    ).noLayout()
  }
}

@Controller
@RequestMapping(Array("/coursework/admin/extensions"))
class FilterExtensionsController extends AbstractFilterExtensionsController {

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear: Option[AcademicYear] =
    retrieveActiveAcademicYear(None)

}

@Controller
@RequestMapping(Array("/coursework/admin/extensions/{academicYear:\\d{4}}"))
class FilterExtensionsForYearController extends AbstractFilterExtensionsController {

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
    retrieveActiveAcademicYear(Option(academicYear))

}

@Controller
@RequestMapping(Array("/coursework/admin/extensions/{extension}"))
class ExtensionController extends CourseworkController {

  type ExtensionsDetailCommand = Appliable[ExtensionDetail] with ViewExtensionState
  type ModifyExtensionCommand = Appliable[Extension] with ModifyExtensionState

  validatesSelf[SelfValidating]

  @ModelAttribute("extensionDetailCommand")
  def detailCommand(@PathVariable extension: Extension) = ViewExtensionCommand(mandatory(extension))

  @ModelAttribute("modifyExtensionCommand")
  def modifyCommand(@PathVariable extension: Extension) = ModifyExtensionCommand(mandatory(extension), mandatory(user))

  @RequestMapping(method = Array(GET), path = Array("detail"))
  def detail(
    @ModelAttribute("extensionDetailCommand") detailCommand: ExtensionsDetailCommand,
    @ModelAttribute("modifyExtensionCommand") updateCommand: ModifyExtensionCommand,
    errors: Errors
  ): Mav = {
    val detail = detailCommand.apply()
    Mav("cm2/admin/extensions/detail",
      "detail" -> detail,
      "modifyExtensionCommand" -> updateCommand,
      "states" -> ExtensionState
    ).noLayout()
  }

  @RequestMapping(method = Array(POST), path = Array("update"))
  def update(
    @ModelAttribute("extensionDetailCommand") detailCommand: ExtensionsDetailCommand,
    @Valid @ModelAttribute("modifyExtensionCommand") updateCommand: ModifyExtensionCommand,
    result: BindingResult,
    errors: Errors
  ): Mav = {
    if (errors.hasErrors) {
      detail(detailCommand, updateCommand, errors)
    } else {
      updateCommand.apply()
      Mav(new JSONView(Map(
        "success" -> true,
        "redirect" -> Routes.admin.extensions(updateCommand.extension.assignment.academicYear)
      )))
    }
  }
}

@Controller
@RequestMapping(Array("/coursework/admin/extensions/{extension}/supporting-file/{filename}"))
class DownloadExtensionAttachmentController extends CourseworkController {

  type DownloadAttachmentCommand = Appliable[Option[RenderableAttachment]] with DownloadExtensionAttachmentState

  @ModelAttribute("downloadAttachmentCommand")
  def attachmentCommand(@PathVariable extension: Extension, @PathVariable filename: String): DownloadAttachmentCommand =
    DownloadExtensionAttachmentCommand(mandatory(extension), mandatory(filename))

  @RequestMapping(method = Array(GET))
  def supportingFile(
    @ModelAttribute("downloadAttachmentCommand") attachmentCommand: DownloadAttachmentCommand,
    @PathVariable filename: String
  ): RenderableFile = {
    attachmentCommand.apply()
      .getOrElse(throw new ItemNotFoundException)
  }
}

@Controller
@RequestMapping(Array("/coursework/admin/assignments/{assignment}/extensions"))
class ListExtensionsForAssignmentController extends CourseworkController {
  @ModelAttribute("listCommand")
  def listCommand(@PathVariable assignment: Assignment): ListExtensionsForAssignmentCommand.Command =
    ListExtensionsForAssignmentCommand(assignment, user)

  @RequestMapping
  def listExtensions(@ModelAttribute("listCommand") cmd: ListExtensionsForAssignmentCommand.Command, @RequestParam(value = "extension", required = false) extension: String): Mav =
    Mav("cm2/admin/extensions/assignmentSummary",
      "extensionToOpen" -> extension,
      "extensionGraphs" -> cmd.apply(),
      "module" -> cmd.assignment.module,
      "assignment" -> cmd.assignment,
      "maxDaysToDisplayAsProgressBar" -> Extension.MaxDaysToDisplayAsProgressBar)
      .crumbsList(Breadcrumbs.assignment(cmd.assignment))

}

@Controller
@RequestMapping(Array("/coursework/admin/assignments/{assignment}/extensions/{student}"))
class EditExtensionController extends CourseworkController with ExtensionServices {

  type ExtensionsDetailCommand = Appliable[DisplayExtensionDetail] with DisplayExtensionState
  type EditExtensionCommand = Appliable[Extension] with EditExtensionCommandState

  validatesSelf[SelfValidating]

  @ModelAttribute("extensionDetailCommand")
  def detailCommand(@PathVariable assignment: Assignment, @PathVariable student: User) =
    DisplayExtensionCommand(mandatory(student), mandatory(assignment))

  @ModelAttribute("editExtensionCommand")
  def editCommand(@PathVariable assignment: Assignment, @PathVariable student: User) =
    EditExtensionCommand(assignment, student, user)

  @RequestMapping(Array("detail"))
  def detail(
    @PathVariable student: User,
    @ModelAttribute("extensionDetailCommand") detailCommand: ExtensionsDetailCommand,
    @ModelAttribute("editExtensionCommand") updateCommand: EditExtensionCommand,
    errors: Errors
  ): Mav = {
    val detail = detailCommand.apply()
    val studentMember = profileService.getMemberByUser(student)
    val studentContext = studentMember match {
      case Some(s: StudentMember) =>
        val relationships = relationshipService.allStudentRelationshipTypes.map { relationshipType =>
          (relationshipType.description, relationshipService.findCurrentRelationships(relationshipType, s))
        }.toMap.filter({ case (_, relations) => relations.nonEmpty })
        Map(
          "relationships" -> relationships,
          "course" -> s.mostSignificantCourseDetails
        )
      case _ => Map.empty
    }

    Mav("cm2/admin/extensions/assignmentdetail",
      "usercode" -> student.getUserId,
      "universityId" -> student.getWarwickId,
      "student" -> studentMember,
      "studentContext" -> studentContext,
      "detail" -> detail,
      "modifyExtensionCommand" -> updateCommand,
      "states" -> ExtensionState
    ).noLayout()
  }

  @RequestMapping(method = Array(POST), path = Array("detail"))
  def update(
    @PathVariable assignment: Assignment,
    @PathVariable student: User,
    @ModelAttribute("extensionDetailCommand") detailCommand: ExtensionsDetailCommand,
    @Valid @ModelAttribute("editExtensionCommand") updateCommand: EditExtensionCommand,
    result: BindingResult,
    errors: Errors
  ): Mav = {
    if (errors.hasErrors) {
      detail(student, detailCommand, updateCommand, errors)
    } else {
      updateCommand.apply()
      Mav(new JSONView(Map(
        "redirect" -> Routes.admin.assignment.extensions(assignment),
        "success" -> true
      )))
    }
  }

  // view an extension (or request)
  @RequestMapping
  def editExtension(
    @ModelAttribute("editExtensionCommand") cmd: EditExtensionCommand,
    errors: Errors
  ): Mav = {
    val student = cmd.extension.universityId.flatMap(uid => profileService.getMemberByUniversityId(uid))
    val studentContext = student match {
      case Some(student: StudentMember) =>
        val relationships = relationshipService.allStudentRelationshipTypes.map { relationshipType =>
          (relationshipType.description, relationshipService.findCurrentRelationships(relationshipType, student))
        }.toMap.filter({ case (_, relations) => relations.nonEmpty })
        Map(
          "relationships" -> relationships,
          "course" -> student.mostSignificantCourseDetails
        )
      case _ => Map.empty
    }

    val model = Mav("cm2/admin/extensions/detail",
      "command" -> cmd,
      "module" -> cmd.extension.assignment.module,
      "assignment" -> cmd.extension.assignment,
      "student" -> student,
      "studentContext" -> studentContext,
      "userFullName" -> userLookup.getUserByUserId(cmd.extension.usercode).getFullName
    ).noLayout()

    model
  }

  @RequestMapping(method = Array(POST))
  @ResponseBody
  def persistExtension(
    @Valid @ModelAttribute("editExtensionCommand") cmd: EditExtensionCommand,
    result: BindingResult,
    errors: Errors
  ): Mav = {
    if (errors.hasErrors) {
      editExtension(cmd, errors)
    } else {
      val extensionJson = JsonHelper.toJson(cmd.apply().asMap)
      Mav("ajax_success", "data" -> extensionJson).noLayout()
    }
  }
}

@Controller
@RequestMapping(Array("/coursework/admin/department/{department}/manage/extensions"))
class RedirectExtensionManagementController extends CourseworkController with AcademicYearScopedController
  with AutowiringUserSettingsServiceComponent with AutowiringMaintenanceModeServiceComponent {

  @ModelAttribute("activeAcademicYear")
  override def activeAcademicYear: Option[AcademicYear] =
    retrieveActiveAcademicYear(None)

  @RequestMapping
  def redirect(@PathVariable department: Department, @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]): Mav =
    Redirect(s"${Routes.admin.extensions(activeAcademicYear.getOrElse(AcademicYear.now()))}?departments=${mandatory(department).code}")
}

@Controller
@RequestMapping(Array("/coursework/admin/department/{department}/{academicYear:\\d{4}}/manage/extensions"))
class RedirectExtensionManagementForYearController extends CourseworkController {
  @RequestMapping def redirect(@PathVariable department: Department, @PathVariable academicYear: AcademicYear): Mav =
    Redirect(s"${Routes.admin.extensions(academicYear)}?departments=${mandatory(department).code}")
}
