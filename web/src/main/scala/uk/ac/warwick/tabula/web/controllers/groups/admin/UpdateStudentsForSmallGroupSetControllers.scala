package uk.ac.warwick.tabula.web.controllers.groups.admin

import javax.validation.Valid
import org.joda.time.LocalDate
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{SmallGroupMembershipStyle, SmallGroupSet}
import uk.ac.warwick.tabula.groups.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.groups.GroupsController

import scala.collection.JavaConverters._

abstract class UpdateStudentsForSmallGroupSetController extends GroupsController {

  validatesSelf[SelfValidating]

  type UpdateStudentsForSmallGroupSetCommand = Appliable[SmallGroupSet]
  type FindStudentsForUserGroupCommand = Appliable[FindStudentsForUserGroupCommandResult] with PopulateOnForm with FindStudentsForUserGroupCommandState with UpdatesFindStudentsForUserGroupCommand
  type EditSmallGroupSetMembershipCommand = Appliable[EditUserGroupMembershipCommandResult] with PopulateOnForm with AddsUsersToEditUserGroupMembershipCommand with RemovesUsersFromEditUserGroupMembershipCommand with ResetsMembershipInEditUserGroupMembershipCommand

  @ModelAttribute("ManageSmallGroupsMappingParameters")
  def params: ManageSmallGroupsMappingParameters.type = ManageSmallGroupsMappingParameters

  @ModelAttribute("persistenceCommand")
  def persistenceCommand(@PathVariable module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): UpdateStudentsForSmallGroupSetCommand =
    UpdateStudentsForUserGroupCommand(mandatory(set.department), mandatory(module), mandatory(set))

  @ModelAttribute("findCommand")
  def findCommand(@PathVariable module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): FindStudentsForUserGroupCommand =
    FindStudentsForUserGroupCommand(mandatory(set.department), mandatory(module), mandatory(set))

  @ModelAttribute("editMembershipCommand")
  def editMembershipCommand(@PathVariable module: Module, @PathVariable("smallGroupSet") set: SmallGroupSet): EditSmallGroupSetMembershipCommand =
    EditUserGroupMembershipCommand(mandatory(module), mandatory(set))

  protected val newOrEdit: String

  protected val renderPath: String

  private def summaryString(
    findStudentsCommandResult: FindStudentsForUserGroupCommandResult,
    editMembershipCommandResult: EditUserGroupMembershipCommandResult
  ): String = {
    val sitsCount = (findStudentsCommandResult.staticStudentIds.asScala
      diff editMembershipCommandResult.excludedStudentIds.asScala
      diff editMembershipCommandResult.includedStudentIds.asScala).size

    val removedCount = editMembershipCommandResult.excludedStudentIds.asScala.count(findStudentsCommandResult.staticStudentIds.asScala.contains)
    val addedCount = editMembershipCommandResult.includedStudentIds.asScala.size

    if (sitsCount == 0)
      ""
    else
      s"${sitsCount + addedCount} students on this scheme <span class='very-subtle'>($sitsCount from SITS${
        removedCount match {
          case 0 => ""
          case count => s" after $count removed manually"
        }
      }${
        addedCount match {
          case 0 => ""
          case count => s", plus $count added manually"
        }
      })</span>"
  }

  protected def render(
    set: SmallGroupSet,
    findStudentsCommandResult: FindStudentsForUserGroupCommandResult,
    editMembershipCommandResult: EditUserGroupMembershipCommandResult,
    addUsersResult: AddUsersToEditUserGroupMembershipCommandResult = AddUsersToEditUserGroupMembershipCommandResult(Seq()),
    expandFind: Boolean = false,
    expandManual: Boolean = false
  ): Mav = {
    Mav(renderPath,
      "totalResults" -> 0,
      "findCommandResult" -> findStudentsCommandResult,
      "editMembershipCommandResult" -> editMembershipCommandResult,
      "addUsersResult" -> addUsersResult,
      "summaryString" -> summaryString(findStudentsCommandResult, editMembershipCommandResult),
      "expandFind" -> expandFind,
      "expandManual" -> expandManual,
      "SITSInFlux" -> set.academicYear.isSITSInFlux(LocalDate.now),
      "returnTo" -> getReturnTo("")
    ).crumbs(Breadcrumbs.Department(set.department, set.academicYear), Breadcrumbs.ModuleForYear(set.module, set.academicYear))
  }

  @RequestMapping
  def form(
    @ModelAttribute("findCommand") findCommand: FindStudentsForUserGroupCommand,
    @ModelAttribute("editMembershipCommand") editMembershipCommand: EditSmallGroupSetMembershipCommand,
    @PathVariable("smallGroupSet") set: SmallGroupSet
  ): Mav = {
    if (set.membershipStyle != SmallGroupMembershipStyle.SitsQuery) {
      Mav("redirect:" + (newOrEdit match {
        case "new" => Routes.admin.createAddStudents(set)
        case "edit" => Routes.admin.editAddStudents(set)
      }))
    } else {
      findCommand.populate()
      editMembershipCommand.populate()
      val findStudentsCommandResult =
        if (findCommand.filterQueryString.length > 0)
          findCommand.apply()
        else
          FindStudentsForUserGroupCommandResult(JArrayList(), Seq())
      val editMembershipCommandResult = editMembershipCommand.apply()
      render(set, findStudentsCommandResult, editMembershipCommandResult)
    }
  }

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.findStudents))
  def findStudents(
    @ModelAttribute("findCommand") findCommand: FindStudentsForUserGroupCommand,
    @ModelAttribute("editMembershipCommand") editMembershipCommand: EditSmallGroupSetMembershipCommand,
    @PathVariable("smallGroupSet") set: SmallGroupSet
  ): Mav = {
    val findStudentsCommandResult = findCommand.apply()
    val editMembershipCommandResult = editMembershipCommand.apply()
    render(set, findStudentsCommandResult, editMembershipCommandResult, expandFind = true)
  }

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.manuallyAddForm))
  def manuallyAddForm(
    @ModelAttribute("findCommand") findCommand: FindStudentsForUserGroupCommand,
    @ModelAttribute("editMembershipCommand") editMembershipCommand: EditSmallGroupSetMembershipCommand,
    @PathVariable("smallGroupSet") set: SmallGroupSet
  ): Mav = {
    Mav("groups/admin/groups/manuallyaddstudents",
      "returnTo" -> getReturnTo("")
    ).crumbs(Breadcrumbs.Department(set.department, set.academicYear), Breadcrumbs.ModuleForYear(set.module, set.academicYear))
  }

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.manuallyAddSubmit))
  def manuallyAddSubmit(
    @ModelAttribute("findCommand") findCommand: FindStudentsForUserGroupCommand,
    @ModelAttribute("editMembershipCommand") editMembershipCommand: EditSmallGroupSetMembershipCommand,
    @PathVariable("smallGroupSet") set: SmallGroupSet
  ): Mav = {
    val addUsersResult = editMembershipCommand.addUsers()
    val editMembershipCommandResult = editMembershipCommand.apply()
    findCommand.update(editMembershipCommandResult)
    val findStudentsCommandResult = findCommand.apply()
    render(set, findStudentsCommandResult, editMembershipCommandResult, addUsersResult = addUsersResult, expandManual = true)
  }

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.manuallyExclude))
  def manuallyExclude(
    @ModelAttribute("findCommand") findCommand: FindStudentsForUserGroupCommand,
    @ModelAttribute("editMembershipCommand") editMembershipCommand: EditSmallGroupSetMembershipCommand,
    @PathVariable("smallGroupSet") set: SmallGroupSet
  ): Mav = {
    editMembershipCommand.removeUsers()
    val editMembershipCommandResult = editMembershipCommand.apply()
    findCommand.update(editMembershipCommandResult)
    val findStudentsCommandResult = findCommand.apply()
    render(set, findStudentsCommandResult, editMembershipCommandResult, expandManual = true)
  }

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.resetMembership))
  def resetMembership(
    @ModelAttribute("findCommand") findCommand: FindStudentsForUserGroupCommand,
    @ModelAttribute("editMembershipCommand") editMembershipCommand: EditSmallGroupSetMembershipCommand,
    @PathVariable("smallGroupSet") set: SmallGroupSet
  ): Mav = {
    editMembershipCommand.resetMembership()
    val editMembershipCommandResult = editMembershipCommand.apply()
    findCommand.update(editMembershipCommandResult)
    val findStudentsCommandResult = findCommand.apply()
    render(set, findStudentsCommandResult, editMembershipCommandResult, expandManual = true)
  }

  protected def submit(
    cmd: UpdateStudentsForSmallGroupSetCommand,
    errors: Errors,
    findCommand: FindStudentsForUserGroupCommand,
    editMembershipCommand: EditSmallGroupSetMembershipCommand,
    set: SmallGroupSet,
    route: String
  ): Mav = {
    if (errors.hasErrors) {
      val findStudentsCommandResult =
        if (findCommand.filterQueryString.length > 0)
          findCommand.apply()
        else
          FindStudentsForUserGroupCommandResult(JArrayList(), Seq())
      val editMembershipCommandResult = editMembershipCommand.apply()
      render(set, findStudentsCommandResult, editMembershipCommandResult)
    } else {
      cmd.apply()
      RedirectForce(route)
    }
  }

  @RequestMapping(method = Array(POST), params = Array("persist"))
  def save(
    @Valid @ModelAttribute("persistenceCommand") cmd: UpdateStudentsForSmallGroupSetCommand,
    errors: Errors,
    @ModelAttribute("findCommand") findCommand: FindStudentsForUserGroupCommand,
    @ModelAttribute("editMembershipCommand") editMembershipCommand: EditSmallGroupSetMembershipCommand,
    @PathVariable("smallGroupSet") set: SmallGroupSet
  ): Mav = submit(cmd, errors, findCommand, editMembershipCommand, set, Routes.admin.module(set.module, set.academicYear))

}

@RequestMapping(Array("/groups/admin/module/{module}/groups/new/{smallGroupSet}/sits-students"))
@Controller
class CreateSmallGroupSetAddStudentsSitsController extends UpdateStudentsForSmallGroupSetController {

  override protected val newOrEdit: String = "new"
  override protected val renderPath = "groups/admin/groups/newstudentssits"

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndEditProperties))
  def saveAndEditProperties(
    @Valid @ModelAttribute("persistenceCommand") cmd: UpdateStudentsForSmallGroupSetCommand,
    errors: Errors,
    @ModelAttribute("findCommand") findCommand: FindStudentsForUserGroupCommand,
    @ModelAttribute("editMembershipCommand") editMembershipCommand: EditSmallGroupSetMembershipCommand,
    @PathVariable("smallGroupSet") set: SmallGroupSet
  ): Mav = submit(cmd, errors, findCommand, editMembershipCommand, set, Routes.admin.create(set))

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddGroups))
  def saveAndAddGroups(
    @Valid @ModelAttribute("persistenceCommand") cmd: UpdateStudentsForSmallGroupSetCommand,
    errors: Errors,
    @ModelAttribute("findCommand") findCommand: FindStudentsForUserGroupCommand,
    @ModelAttribute("editMembershipCommand") editMembershipCommand: EditSmallGroupSetMembershipCommand,
    @PathVariable("smallGroupSet") set: SmallGroupSet
  ): Mav = submit(cmd, errors, findCommand, editMembershipCommand, set, Routes.admin.createAddGroups(set))

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAddEvents))
  def saveAndAddEvents(
    @Valid @ModelAttribute("persistenceCommand") cmd: UpdateStudentsForSmallGroupSetCommand,
    errors: Errors,
    @ModelAttribute("findCommand") findCommand: FindStudentsForUserGroupCommand,
    @ModelAttribute("editMembershipCommand") editMembershipCommand: EditSmallGroupSetMembershipCommand,
    @PathVariable("smallGroupSet") set: SmallGroupSet
  ): Mav = submit(cmd, errors, findCommand, editMembershipCommand, set, Routes.admin.createAddEvents(set))

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.createAndAllocate))
  def saveAndAllocate(
    @Valid @ModelAttribute("persistenceCommand") cmd: UpdateStudentsForSmallGroupSetCommand,
    errors: Errors,
    @ModelAttribute("findCommand") findCommand: FindStudentsForUserGroupCommand,
    @ModelAttribute("editMembershipCommand") editMembershipCommand: EditSmallGroupSetMembershipCommand,
    @PathVariable("smallGroupSet") set: SmallGroupSet
  ): Mav = submit(cmd, errors, findCommand, editMembershipCommand, set, Routes.admin.createAllocate(set))

}

@RequestMapping(Array("/groups/admin/module/{module}/groups/edit/{smallGroupSet}/sits-students"))
@Controller
class EditSmallGroupSetAddStudentsSitsController extends UpdateStudentsForSmallGroupSetController {

  override protected val newOrEdit: String = "edit"
  override protected val renderPath = "groups/admin/groups/editstudentssits"

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndEditProperties))
  def saveAndEditProperties(
    @Valid @ModelAttribute("persistenceCommand") cmd: UpdateStudentsForSmallGroupSetCommand,
    errors: Errors,
    @ModelAttribute("findCommand") findCommand: FindStudentsForUserGroupCommand,
    @ModelAttribute("editMembershipCommand") editMembershipCommand: EditSmallGroupSetMembershipCommand,
    @PathVariable("smallGroupSet") set: SmallGroupSet
  ): Mav = submit(cmd, errors, findCommand, editMembershipCommand, set, Routes.admin.edit(set))

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddGroups))
  def saveAndAddGroups(
    @Valid @ModelAttribute("persistenceCommand") cmd: UpdateStudentsForSmallGroupSetCommand,
    errors: Errors,
    @ModelAttribute("findCommand") findCommand: FindStudentsForUserGroupCommand,
    @ModelAttribute("editMembershipCommand") editMembershipCommand: EditSmallGroupSetMembershipCommand,
    @PathVariable("smallGroupSet") set: SmallGroupSet
  ): Mav = submit(cmd, errors, findCommand, editMembershipCommand, set, Routes.admin.editAddGroups(set))

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAddEvents))
  def saveAndAddEvents(
    @Valid @ModelAttribute("persistenceCommand") cmd: UpdateStudentsForSmallGroupSetCommand,
    errors: Errors,
    @ModelAttribute("findCommand") findCommand: FindStudentsForUserGroupCommand,
    @ModelAttribute("editMembershipCommand") editMembershipCommand: EditSmallGroupSetMembershipCommand,
    @PathVariable("smallGroupSet") set: SmallGroupSet
  ): Mav = submit(cmd, errors, findCommand, editMembershipCommand, set, Routes.admin.editAddEvents(set))

  @RequestMapping(method = Array(POST), params = Array(ManageSmallGroupsMappingParameters.editAndAllocate))
  def saveAndAllocate(
    @Valid @ModelAttribute("persistenceCommand") cmd: UpdateStudentsForSmallGroupSetCommand,
    errors: Errors,
    @ModelAttribute("findCommand") findCommand: FindStudentsForUserGroupCommand,
    @ModelAttribute("editMembershipCommand") editMembershipCommand: EditSmallGroupSetMembershipCommand,
    @PathVariable("smallGroupSet") set: SmallGroupSet
  ): Mav = submit(cmd, errors, findCommand, editMembershipCommand, set, Routes.admin.editAllocate(set))

}