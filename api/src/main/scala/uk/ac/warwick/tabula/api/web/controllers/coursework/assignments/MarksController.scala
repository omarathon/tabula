package uk.ac.warwick.tabula.api.web.controllers.coursework.assignments

import com.fasterxml.jackson.annotation.JsonAutoDetect
import javax.validation.Valid
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.util.StringUtils
import org.springframework.validation.{BindingResult, Errors}
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestBody, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.api.commands.JsonApiRequest
import uk.ac.warwick.tabula.api.commands.coursework.ApiAddMarksCommand
import uk.ac.warwick.tabula.api.web.controllers.coursework.assignments.MarksController.AddMarksCommand
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.cm2.feedback.GenerateGradesFromMarkCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Feedback, MarkPoint, MarkerFeedback}
import uk.ac.warwick.tabula.helpers.{FoundUser, LazyLists}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringUserLookupComponent, FeedbackServiceComponent, GeneratesGradesFromMarks}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.UserOrderingByIds._
import uk.ac.warwick.tabula.services.cm2.docconversion.MarksExtractorComponent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}
import uk.ac.warwick.userlookup.User

import scala.beans.BeanProperty
import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.Try

object MarksController {
  type AddMarksCommand = Appliable[Seq[Feedback]] with ApiAddMarksState with ApiAddMarksValidator
}

trait ApiAddMarksState extends Object with FeedbackServiceComponent {
  self: AssessmentMembershipServiceComponent =>

  def assignment: Assignment

  def gradeGenerator: GeneratesGradesFromMarks

  def submitter: CurrentUser

  def existingMarks: Seq[FeedbackItem] = assessmentMembershipService.determineMembershipUsers(assignment).map(student => {
    val feedback = feedbackService.getStudentFeedback(assignment, student.getUserId)
    val feedbackItem = new FeedbackItem
    feedbackItem.id = Option(student.getWarwickId).getOrElse(student.getUserId)
    feedbackItem.mark = feedback.flatMap(_.actualMark).map(_.toString).getOrElse("")
    feedbackItem.grade = feedback.flatMap(_.actualGrade).getOrElse("")
    feedbackItem.feedback = feedback.flatMap(_.fieldValue("feedbackText")).getOrElse("")

    feedbackItem
  }).sortBy(_.user(assignment))

  def updatedReleasedFeedback: Seq[Feedback] = students.asScala.filter(_.isModified).flatMap(_.currentFeedback(assignment)).filter(_.released)

  // bindable
  var students: JList[FeedbackItem] = LazyLists.create[FeedbackItem]()
}

// based on AddMarksCommandBindListener from MarkerAddMarksCOmmand.scala, adjusted for FeedbackItem
// TODO: check the errorCodes in calls to rejectValue
trait ApiAddMarksValidator extends SelfValidating {
  self: ApiAddMarksState with MarksExtractorComponent =>

  def isModified(feedbackItem: FeedbackItem): Boolean

  def canMark(feedbackItem: FeedbackItem): Boolean

  override def validate(errors: Errors): Unit = {
    val usersSoFar: mutable.Set[User] = mutable.Set()
    students.asScala.zipWithIndex.foreach { case (student, i) =>
      errors.pushNestedPath("students[" + i + "]")
      validateFeedbackItem(student, errors)
      student.isModified = isModified(student)
      errors.popNestedPath()
    }

    def validateFeedbackItem(item: FeedbackItem, errors: Errors): Unit = {

      def rejectValue(field: String, errorCode: String, errorArgs: Array[AnyRef] = null, defaultMessage: String = ""): Unit = {
        item.isValid = false
        errors.rejectValue(field, errorCode, errorArgs, defaultMessage)
      }

      // validate id is present, relates to a user and isn't a dupe
      if (!item.id.hasText) {
        rejectValue("id", "NotEmpty")
      } else {
        item.user(assignment) match {
          case Some(FoundUser(u)) if usersSoFar.contains(u) => rejectValue("id", "id.duplicate.mark")
          case Some(FoundUser(u)) =>
            usersSoFar.add(u)
            if (!canMark(item)) rejectValue("id", "id.wrong.marker")
          case None => rejectValue("id", "id.userNotFound", Array(item.id))
        }
      }

      // validate mark (must be int between 0 and 100)
      if (item.mark.hasText) {
        try {
          val asInt = item.mark.toInt
          if (asInt < 0 || asInt > 100) {
            rejectValue("mark", "mark.range")
          }
          if (assignment.useMarkPoints && MarkPoint.forMark(asInt).isEmpty) {
            rejectValue("mark", "mark.markPoint")
          }
        } catch {
          case _@(_: NumberFormatException | _: IllegalArgumentException) => rejectValue("mark", "actualMark.format")
        }
      } else if (assignment.module.adminDepartment.assignmentGradeValidation && item.grade.hasText) {
        rejectValue("grade", "actualMark.validateGrade.adjustedGrade")
      }

      // validate grade is department setting is true
      item.user(assignment).flatMap(u => Option(u.getWarwickId)).foreach(uniId => {
        if (item.isValid && item.grade.hasText && assignment.module.adminDepartment.assignmentGradeValidation) {
          val validGrades = gradeGenerator.applyForMarks(Map(uniId -> item.mark.toInt))(uniId)
          if (validGrades.nonEmpty && !validGrades.exists(_.grade == item.grade)) {
            rejectValue("grade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")))
          }
        }
      })

      // If a row has no mark or grade, we will quietly ignore it
      if (!item.mark.hasText && !item.grade.hasText && !item.feedback.hasText) {
        item.isValid = false
      }
    }
  }
}

@Controller
@RequestMapping(Array("/v1/module/{module}/assignments/{assignment}/marks"))
class MarksController extends ApiController {
  /**
   *
   * @param assignment
   * @param currentUser
   * @return
   */
  @ModelAttribute("command")
  def command(@PathVariable assignment: Assignment, currentUser: CurrentUser) =
    ApiAddMarksCommand(mandatory(assignment), currentUser, GenerateGradesFromMarkCommand(assignment))

  /**
   *
   * @return
   */
  @RequestMapping(
    method = Array(POST),
    consumes = Array(MediaType.APPLICATION_JSON_VALUE),
    produces = Array("application/json")
  )
  def addMarks(@RequestBody request: AddMarksRequest, @Valid @ModelAttribute("command") cmd: AddMarksCommand, errors: Errors): Mav = {
    request.copyTo(cmd, errors)
    globalValidator.validate(cmd, errors)
    cmd.validate(errors)

    if (errors.hasErrors) {
      Mav(new JSONErrorView(errors))
    } else {
      cmd.apply()

      Mav(new JSONView(Map(
        "success" -> true,
        "status" -> "ok"
      )))
    }
  }
}

class AddMarksRequest extends JsonApiRequest[AddMarksCommand] {
  @BeanProperty var students: JList[FeedbackItem] = _

  override def copyTo(state: AddMarksCommand, errors: Errors) = {
    state.students = students;
  }
}

// a lot of overlap with MarkItem from MarksExtractor.scala; maybe turn user/currentFeedback/currentMarkerFeedback into a trait?
class FeedbackItem extends AutowiringUserLookupComponent {
  @BeanProperty var id: String = _
  @BeanProperty var mark: String = _
  @BeanProperty var grade: String = _
  @BeanProperty var feedback: String = _
  var isValid = true
  var isModified = false

  def user(assignment: Assignment): Option[User] = id.maybeText.map(userLookup.getUserByWarwickUniId).filter(u => u.isFoundUser && !u.isLoginDisabled)
    .orElse(id.maybeText.map(userLookup.getUserByWarwickUniIdUncached(_, skipMemberLookup = true)).filter(u => u.isFoundUser && !u.isLoginDisabled))
    .orElse(id.maybeText.map(userLookup.getUserByUserId).filter(u => u.isFoundUser && !u.isLoginDisabled))
    .orElse({
      val anonId = id.maybeText.flatMap { asStr => Try(asStr.toInt).toOption }
      anonId.flatMap(id => assignment.allFeedback.find(_.anonymousId.contains(id)).map(f => userLookup.getUserByUserId(f.usercode)))
    })

  def currentFeedback(assignment: Assignment): Option[Feedback] = for {
    u <- user(assignment)
    f <- assignment.allFeedback.find(_.usercode == u.getUserId)
  } yield f

  def currentMarkerFeedback(assignment: Assignment, marker: User): Option[MarkerFeedback] = for {
    f <- currentFeedback(assignment)
    cmf <- f.markerFeedback.asScala.find(mf => marker == mf.marker && f.outstandingStages.asScala.contains(mf.stage))
  } yield cmf

  // true if none of the fields of this FeedbackItem differ from the values found in the marker feedback
  def unchanged(mf: MarkerFeedback): Boolean = {
    val markUnchanged = if(StringUtils.hasText(mark)) mf.mark.contains(mark.toInt) else mf.mark.isEmpty
    val gradeUnchanged = if(StringUtils.hasText(grade)) mf.grade.contains(grade) else mf.grade.isEmpty
    val feedbackUnchanged = if(StringUtils.hasText(feedback)) mf.fieldValue("feedbackText").contains(feedback) else mf.fieldValue("feedbackText").isEmpty

    markUnchanged && gradeUnchanged && feedbackUnchanged
  }

}
