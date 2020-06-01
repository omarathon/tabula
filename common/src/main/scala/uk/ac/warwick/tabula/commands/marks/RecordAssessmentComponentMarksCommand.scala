package uk.ac.warwick.tabula.commands.marks

import org.apache.poi.openxml4j.exceptions.InvalidFormatException
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.eventusermodel.{ReadOnlySharedStringsTable, XSSFReader}
import org.apache.poi.xssf.usermodel.XSSFComment
import org.springframework.validation.{BindingResult, Errors}
import org.xml.sax.InputSource
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.RecordAssessmentComponentMarksCommand._
import uk.ac.warwick.tabula.data.model.{AssessmentComponent, RecordedAssessmentComponentStudent, UpstreamAssessmentGroup, UpstreamAssessmentGroupInfo}
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.coursework.docconversion.AbstractXslxSheetHandler
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.jdk.CollectionConverters._
import scala.util.Using

object RecordAssessmentComponentMarksCommand {
  type UniversityID = String
  class StudentMarksItem {
    def this(universityID: UniversityID) {
      this()
      this.universityID = universityID
    }

    var universityID: UniversityID = _
    var mark: String = _ // Easier as a String to treat empty strings correctly
    var grade: String = _
    var comments: String = _
  }

  type Result = Seq[RecordedAssessmentComponentStudent]
  type Command = Appliable[Result]
    with RecordAssessmentComponentMarksRequest
    with SelfValidating
    with BindListener
    with PopulateOnForm

  val AdminPermission: Permission = Permissions.Feedback.Publish

  def apply(assessmentComponent: AssessmentComponent, upstreamAssessmentGroup: UpstreamAssessmentGroup, currentUser: CurrentUser): Command =
    new RecordAssessmentComponentMarksCommandInternal(assessmentComponent, upstreamAssessmentGroup, currentUser)
      with ComposableCommand[Result]
      with RecordAssessmentComponentMarksRequest
      with RecordAssessmentComponentMarksValidation
      with RecordAssessmentComponentMarksPermissions
      with RecordAssessmentComponentMarksDescription
      with RecordAssessmentComponentMarksSpreadsheetBindListener
      with RecordAssessmentComponentMarksPopulateOnForm
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringTransactionalComponent
}

abstract class RecordAssessmentComponentMarksCommandInternal(val assessmentComponent: AssessmentComponent, val upstreamAssessmentGroup: UpstreamAssessmentGroup, currentUser: CurrentUser)
  extends CommandInternal[Result]
    with RecordAssessmentComponentMarksState {
  self: RecordAssessmentComponentMarksRequest
    with AssessmentComponentMarksServiceComponent
    with TransactionalComponent =>

  override def applyInternal(): Result = transactional() {
    students.asScala.values.toSeq
      .map { item =>
        val upstreamAssessmentGroupMember =
          upstreamAssessmentGroup.members.asScala
            .find(_.universityId == item.universityID)
            .get // We validate that this exists

        val recordedAssessmentComponentStudent: RecordedAssessmentComponentStudent =
          assessmentComponentMarksService.getOrCreateRecordedStudent(upstreamAssessmentGroupMember)

        recordedAssessmentComponentStudent.addMark(
          uploader = currentUser.apparentUser,
          mark = item.mark.maybeText.map(_.toInt),
          grade = item.grade.maybeText,
          comments = item.comments
        )

        assessmentComponentMarksService.saveOrUpdate(recordedAssessmentComponentStudent)

        recordedAssessmentComponentStudent
      }
  }
}

trait RecordAssessmentComponentMarksState {
  def assessmentComponent: AssessmentComponent
  def upstreamAssessmentGroup: UpstreamAssessmentGroup
}

trait RecordAssessmentComponentMarksRequest {
  var students: JMap[UniversityID, StudentMarksItem] =
    LazyMaps.create { id: String => new StudentMarksItem(id) }
      .asJava

  // For uploading a spreadsheet
  var file: UploadedFile = new UploadedFile
}

trait RecordAssessmentComponentMarksSpreadsheetBindListener extends BindListener {
  self: RecordAssessmentComponentMarksRequest
    with TransactionalComponent =>

  final val MAX_MARKS_ROWS: Int = 5000
  final val VALID_FILE_TYPES: Seq[String] = Seq(".xlsx")

  override def onBind(result: BindingResult): Unit = {
    val fileNames = file.fileNames map (_.toLowerCase)
    val invalidFiles = fileNames.filter(s => !VALID_FILE_TYPES.exists(s.endsWith))

    if (invalidFiles.nonEmpty) {
      if (invalidFiles.size == 1) result.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString(""), VALID_FILE_TYPES.mkString(", ")), "")
      else result.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", "), VALID_FILE_TYPES.mkString(", ")), "")
    }

    if (!result.hasErrors) {
      transactional() {
        result.pushNestedPath("file")
        file.onBind(result)
        result.popNestedPath()

        file.attached.asScala.filter(_.hasData).foreach(file => {
          try {
            Using.resource(file.asByteSource.openStream()) { stream =>
              val pkg = OPCPackage.open(stream)
              val sst = new ReadOnlySharedStringsTable(pkg)
              val reader = new XSSFReader(pkg)
              val styles = reader.getStylesTable

              val items: JList[StudentMarksItem] = JArrayList()
              val sheetHandler = new AbstractXslxSheetHandler(styles, sst, items) {
                override def newCurrentItem: StudentMarksItem = new StudentMarksItem()
                override def cell(cellReference: String, formattedValue: String, comment: XSSFComment): Unit = {
                  val col = new CellReference(cellReference).getCol
                  if (isFirstRow) {
                    columnMap(col) = formattedValue
                  } else if (columnMap.asJava.containsKey(col) && formattedValue.hasText) {
                    columnMap(col) match {
                      case "University ID" | "ID" =>
                        currentItem.universityID = formattedValue
                      case "Mark" =>
                        currentItem.mark = formattedValue
                      case "Grade" =>
                        currentItem.grade = formattedValue
                      case "Comments" =>
                        currentItem.comments = formattedValue
                      case _ => // ignore anything else
                    }
                  }
                }
              }

              val parser = sheetHandler.fetchSheetParser
              reader.getSheetsData.asScala.foreach { is =>
                Using.resource(is)(sheet => parser.parse(new InputSource(sheet)))
              }

              if (items.size() > MAX_MARKS_ROWS) {
                result.rejectValue("file", "file.tooManyRows", Array(MAX_MARKS_ROWS.toString), "")
                items.clear()
              } else {
                items.asScala.filter(_.universityID.hasText).foreach { item =>
                  students.put(item.universityID, item)
                }
              }
            }
          } catch {
            case _: InvalidFormatException => result.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", "), VALID_FILE_TYPES.mkString(", ")), "")
          }
        })
      }
    }
  }
}

trait RecordAssessmentComponentMarksPopulateOnForm extends PopulateOnForm {
  self: RecordAssessmentComponentMarksState
    with RecordAssessmentComponentMarksRequest
    with AssessmentComponentMarksServiceComponent
    with AssessmentMembershipServiceComponent =>

  override def populate(): Unit = {
    val info = UpstreamAssessmentGroupInfo(
      upstreamAssessmentGroup,
      assessmentMembershipService.getCurrentUpstreamAssessmentGroupMembers(upstreamAssessmentGroup.id)
    )

    ListAssessmentComponentsCommand.studentMarkRecords(info, assessmentComponentMarksService).foreach { student =>
      if (student.mark.nonEmpty || student.grade.nonEmpty) {
        val s = new StudentMarksItem(student.universityId)
        student.mark.foreach(m => s.mark = m.toString)
        student.grade.foreach(s.grade = _)

        students.put(student.universityId, s)
      }
    }
  }
}

trait RecordAssessmentComponentMarksValidation extends SelfValidating {
  self: RecordAssessmentComponentMarksState
    with RecordAssessmentComponentMarksRequest
    with AssessmentMembershipServiceComponent =>

  override def validate(errors: Errors): Unit = {
    val doGradeValidation = assessmentComponent.module.adminDepartment.assignmentGradeValidation
    students.asScala.foreach { case (universityID, item) =>
      errors.pushNestedPath(s"students[$universityID]")

      val upstreamAssessmentGroupMember = upstreamAssessmentGroup.members.asScala.find(_.universityId == universityID)

      // We allow returning marks for PWD students so we don't need to filter by "current" members here
      if (upstreamAssessmentGroupMember.isEmpty) {
        errors.reject("uniNumber.unacceptable", Array(universityID), "")
      }

      if (item.mark.hasText) {
        try {
          val asInt = item.mark.toInt
          if (asInt < 0 || asInt > 100) {
            errors.rejectValue("mark", "actualMark.range")
          } else if (doGradeValidation) {
            val validGrades = assessmentMembershipService.gradesForMark(assessmentComponent, Some(asInt), upstreamAssessmentGroupMember.flatMap(_.resitExpected).getOrElse(false))
            if (item.grade.hasText) {
              if (!validGrades.exists(_.grade == item.grade)) {
                errors.rejectValue("grade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
              }
            } else if (asInt != 0 || assessmentComponent.module.adminDepartment.assignmentGradeValidationUseDefaultForZero) {
              // This is a bit naughty, validation shouldn't modify state, but it's clearer in the preview if we show what the grade will be
              validGrades.find(_.isDefault).foreach(gb => item.grade = gb.grade)
            }

            if (!item.grade.hasText) {
              errors.rejectValue("grade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
            }
          }
        } catch {
          case _ @ (_: NumberFormatException | _: IllegalArgumentException) =>
            errors.rejectValue("mark", "actualMark.format")
        }
      } else if (doGradeValidation&& item.grade.hasText) {
        val validGrades = assessmentMembershipService.gradesForMark(assessmentComponent, None, upstreamAssessmentGroupMember.flatMap(_.resitExpected).getOrElse(false))
        if (!validGrades.exists(_.grade == item.grade)) {
          errors.rejectValue("grade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
        }
      }

      errors.popNestedPath()
    }
  }
}

trait RecordAssessmentComponentMarksPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
  self: RecordAssessmentComponentMarksState =>

  override def permissionsCheck(p: PermissionsChecking): Unit = {
    mustBeLinked(upstreamAssessmentGroup, assessmentComponent)
    p.PermissionCheck(AdminPermission, mandatory(assessmentComponent.module))
  }
}

trait RecordAssessmentComponentMarksDescription extends Describable[Result] {
  self: RecordAssessmentComponentMarksState =>

  override lazy val eventName: String = "RecordAssessmentComponentMarks"

  override def describe(d: Description): Unit =
    d.assessmentComponent(assessmentComponent)
     .upstreamAssessmentGroup(upstreamAssessmentGroup)

  override def describeResult(d: Description, result: Result): Unit =
    d.properties(
      "marks" -> result.filter(_.latestMark.nonEmpty).map { student =>
        student.universityId -> student.latestMark.get
      }.toMap,
      "grades" -> result.filter(_.latestGrade.nonEmpty).map { student =>
        student.universityId -> student.latestGrade.get
      }.toMap
    )
}
