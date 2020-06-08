package uk.ac.warwick.tabula.commands.marks

import org.apache.poi.openxml4j.exceptions.InvalidFormatException
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.eventusermodel.{ReadOnlySharedStringsTable, XSSFReader}
import org.apache.poi.xssf.usermodel.XSSFComment
import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.marks.CalculateModuleMarksCommand._
import uk.ac.warwick.tabula.commands.marks.ListAssessmentComponentsCommand.StudentMarkRecord
import uk.ac.warwick.tabula.commands.marks.MarksDepartmentHomeCommand.StudentModuleMarkRecord
import uk.ac.warwick.tabula.data.model.MarkState.UnconfirmedActual
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.coursework.docconversion.AbstractXslxSheetHandler
import uk.ac.warwick.tabula.services.marks.{AssessmentComponentMarksServiceComponent, AutowiringAssessmentComponentMarksServiceComponent, AutowiringModuleRegistrationMarksServiceComponent, ModuleRegistrationMarksServiceComponent}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent, AutowiringModuleRegistrationServiceComponent, ModuleRegistrationServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.jdk.CollectionConverters._
import scala.util.Using
import scala.xml.InputSource

object CalculateModuleMarksCommand {
  sealed trait ModuleMarkCalculation { def isSuccessful: Boolean }
  object ModuleMarkCalculation {
    case class Success(mark: Option[Int], grade: Option[String], result: Option[ModuleResult]) extends ModuleMarkCalculation { override val isSuccessful: Boolean = true }
    case class Failure(message: String) extends ModuleMarkCalculation { override val isSuccessful: Boolean = false }
  }

  type SprCode = String
  class StudentModuleMarksItem {
    def this(sprCode: SprCode) {
      this()
      this.sprCode = sprCode
    }

    var sprCode: SprCode = _
    var mark: String = _ // Easier as a String to treat empty strings correctly
    var grade: String = _
    var result: String = _
    var comments: String = _
  }

  type Result = Seq[RecordedModuleRegistration]
  type Command = Appliable[Result]
    with CalculateModuleMarksRequest
    with CalculateModuleMarksLoadModuleRegistrations
    with SelfValidating
    with BindListener
    with PopulateOnForm

  def apply(sitsModuleCode: String, module: Module, academicYear: AcademicYear, occurrence: String, currentUser: CurrentUser): Command =
    new CalculateModuleMarksCommandInternal(sitsModuleCode, module, academicYear, occurrence, currentUser)
      with CalculateModuleMarksLoadModuleRegistrations
      with CalculateModuleMarksRequest
      with CalculateModuleMarksValidation
      with ModuleOccurrenceUpdateMarksPermissions
      with AutowiringAssessmentComponentMarksServiceComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringModuleRegistrationServiceComponent
      with AutowiringModuleRegistrationMarksServiceComponent
      with AutowiringTransactionalComponent
      with ComposableCommand[Result] // late-init due to CalculateModuleMarksLoadModuleRegistrations being called from permissions
      with ModuleOccurrenceDescription
      with CalculateModuleMarksSpreadsheetBindListener
      with CalculateModuleMarksPopulateOnForm
      with CalculateModuleMarksAlgorithm
}

abstract class CalculateModuleMarksCommandInternal(val sitsModuleCode: String, val module: Module, val academicYear: AcademicYear, val occurrence: String, currentUser: CurrentUser)
  extends CommandInternal[Result]
    with ModuleOccurrenceState {
  self: CalculateModuleMarksRequest
    with CalculateModuleMarksLoadModuleRegistrations
    with ModuleRegistrationMarksServiceComponent
    with TransactionalComponent =>

  val mandatoryEventName: String = "CalculateModuleMarks"

  override def applyInternal(): Result = transactional() {
    students.asScala.values.toSeq
      .map { item =>
        val moduleRegistration =
          moduleRegistrations.find(_.sprCode == item.sprCode)
            .get // We validate that this exists

        val recordedModuleRegistration: RecordedModuleRegistration =
          moduleRegistrationMarksService.getOrCreateRecordedModuleRegistration(moduleRegistration)

        recordedModuleRegistration.addMark(
          uploader = currentUser.apparentUser,
          mark = item.mark.maybeText.map(_.toInt),
          grade = item.grade.maybeText,
          result = item.result.maybeText.flatMap(c => Option(ModuleResult.fromCode(c))),
          source = RecordedModuleMarkSource.ComponentMarkCalculation,
          markState = recordedModuleRegistration.latestState.getOrElse(UnconfirmedActual),
          comments = item.comments,
        )

        moduleRegistrationMarksService.saveOrUpdate(recordedModuleRegistration)

        recordedModuleRegistration
      }
  }
}

trait CalculateModuleMarksLoadModuleRegistrations extends ModuleOccurrenceLoadModuleRegistrations {
  self: ModuleOccurrenceState
    with CalculateModuleMarksAlgorithm
    with AssessmentMembershipServiceComponent
    with AssessmentComponentMarksServiceComponent
    with ModuleRegistrationServiceComponent
    with ModuleRegistrationMarksServiceComponent =>

  lazy val studentModuleMarkRecordsAndCalculations: Seq[(StudentModuleMarkRecord, Map[AssessmentComponent, (StudentMarkRecord, Option[BigDecimal])], ModuleMarkCalculation)] =
    studentModuleMarkRecords.map { student =>
      val (cm, calculation) =
        moduleRegistrations.find(_.sprCode == student.sprCode).map { moduleRegistration =>
          val universityId = moduleRegistration.studentCourseDetails.student.universityId
          val components: Seq[(AssessmentComponent, StudentMarkRecord)] = componentMarks(universityId).toSeq

          // Also get the weighting by passing all marks in
          val marksForWeighting: Seq[(AssessmentType, String, Option[Int])] =
            components.map { case (ac, s) =>
              (ac.assessmentType, ac.sequence, s.mark)
            }

          val weightedComponents =
            components.map { case (ac, s) =>
              val weighting = ac.weightingFor(marksForWeighting)
              ac -> (s, weighting)
            }.toMap

          (weightedComponents, calculate(moduleRegistration, components))
        }.getOrElse((Map.empty[AssessmentComponent, (StudentMarkRecord, Option[BigDecimal])], ModuleMarkCalculation.Failure("No module registration found")))

      (student, cm, calculation)
    }

}

trait CalculateModuleMarksRequest {
  var students: JMap[SprCode, StudentModuleMarksItem] =
    LazyMaps.create { sprCode: SprCode => new StudentModuleMarksItem(sprCode) }
      .asJava

  // For uploading a spreadsheet
  var file: UploadedFile = new UploadedFile
}

trait CalculateModuleMarksSpreadsheetBindListener extends BindListener {
  self: CalculateModuleMarksRequest
    with TransactionalComponent =>

  final val MAX_MARKS_ROWS: Int = 5000
  final val VALID_FILE_TYPES: Seq[String] = Seq(".xlsx")

  override def onBind(result: BindingResult): Unit = {
    val fileNames = file.fileNames.map(_.toLowerCase)
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

              val items: JList[StudentModuleMarksItem] = JArrayList()
              val sheetHandler = new AbstractXslxSheetHandler(styles, sst, items) {
                override def newCurrentItem: StudentModuleMarksItem = new StudentModuleMarksItem()
                override def cell(cellReference: String, formattedValue: String, comment: XSSFComment): Unit = {
                  val col = new CellReference(cellReference).getCol
                  if (isFirstRow) {
                    columnMap(col) = formattedValue
                  } else if (columnMap.asJava.containsKey(col) && formattedValue.hasText) {
                    columnMap(col) match {
                      // Support the old format in the hope that SPR and SCJ codes match
                      case "SPR Code" | "SPR" | "SCJ Code" | "SCJ" =>
                        currentItem.sprCode = formattedValue
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
                items.asScala.filter(_.sprCode.hasText).foreach { item =>
                  students.put(item.sprCode, item)
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

trait CalculateModuleMarksPopulateOnForm extends PopulateOnForm {
  self: ModuleOccurrenceState
    with CalculateModuleMarksRequest
    with CalculateModuleMarksAlgorithm
    with CalculateModuleMarksLoadModuleRegistrations
    with ModuleRegistrationMarksServiceComponent
    with ModuleRegistrationServiceComponent
    with AssessmentMembershipServiceComponent
    with AssessmentComponentMarksServiceComponent =>

  override def populate(): Unit = {
    studentModuleMarkRecordsAndCalculations.foreach { case (student, _, calculation) =>
      val s = new StudentModuleMarksItem(student.sprCode)
      student.mark.foreach(m => s.mark = m.toString)
      student.grade.foreach(s.grade = _)
      student.result.foreach(r => s.result = r.dbValue)

      // If we can make a calculation we always use it, but make it clear that we have done
      calculation match {
        case ModuleMarkCalculation.Success(mark, grade, result) =>
          mark.foreach(m => s.mark = m.toString)
          grade.foreach(s.grade = _)
          result.foreach(r => s.result = r.dbValue)

        case _ => // Do nothing
      }

      students.put(student.sprCode, s)
    }
  }
}

trait CalculateModuleMarksAlgorithm {
  self: AssessmentMembershipServiceComponent =>

  def calculate(moduleRegistration: ModuleRegistration, components: Seq[(AssessmentComponent, StudentMarkRecord)]): ModuleMarkCalculation = {
    require(components.isEmpty || components.forall { case (ac, s) =>
      ac.moduleCode == components.head._1.moduleCode &&
      s.universityId == components.head._2.universityId
    })

    if (components.isEmpty) ModuleMarkCalculation.Failure("There were no component marks")
    else if (!moduleRegistration.marksCode.hasText) ModuleMarkCalculation.Failure("There is no mark scheme associated with the module registration")
    else {
      val componentsWithMissingMarkOrGrades = components.filter { case (_, s) => s.mark.isEmpty && s.grade.isEmpty }.map(_._1).sortBy(_.sequence)

      if (componentsWithMissingMarkOrGrades.nonEmpty) ModuleMarkCalculation.Failure(s"Marks and grades are missing for ${componentsWithMissingMarkOrGrades.map(_.sequence).mkString(", ")}")
      else {
        val componentsForCalculation = components.filter { case (_, s) => !s.grade.contains(GradeBoundary.ForceMajeureMissingComponentGrade) && !s.grade.contains(GradeBoundary.MitigatingCircumstancesGrade) }
        if (componentsForCalculation.isEmpty && components.forall(_._2.grade.contains(GradeBoundary.ForceMajeureMissingComponentGrade))) ModuleMarkCalculation.Success(None, Some(GradeBoundary.ForceMajeureMissingComponentGrade), None)
        else {
          def validGradesForMark(m: Option[Int]) =
            assessmentMembershipService.gradesForMark(moduleRegistration, m, componentsForCalculation.exists(_._2.resitExpected))

          // Is this a pass/fail module?
          if (moduleRegistration.passFail) {
            // All the components must also be passFail
            if (componentsForCalculation.exists { case (c, _) => !moduleRegistration.marksCode.maybeText.contains(c.marksCode) }) {
              ModuleMarkCalculation.Failure("Not all components are pass/fail but module is")
            } else {
              val validGrades = validGradesForMark(None)
              def passFailResult(grade: String): ModuleMarkCalculation =
                validGrades.find(_.grade == grade) match {
                  case None => ModuleMarkCalculation.Failure(s"Unable to find grade boundary for $grade grade")
                  case Some(gradeBoundary) => ModuleMarkCalculation.Success(None, Some(gradeBoundary.grade), gradeBoundary.result)
                }

              // Each component must have a grade
              val componentsWithMissingGrades = componentsForCalculation.filter(_._2.grade.isEmpty).map(_._1).sortBy(_.sequence)

              if (componentsWithMissingGrades.nonEmpty) ModuleMarkCalculation.Failure(s"Grades are missing for ${componentsWithMissingGrades.map(_.sequence).mkString(", ")}")
              else if (componentsForCalculation.size == 1) {
                // If there's a single component and it's valid, just copy it over
                val gb: Option[GradeBoundary] = componentsForCalculation.head._2.grade match {
                  case Some(existing) => validGrades.find(_.grade == existing)
                  case _ => validGrades.find(_.isDefault)
                }

                gb match {
                  case None => ModuleMarkCalculation.Failure("Unable to select a valid grade")
                  case Some(gradeBoundary) => ModuleMarkCalculation.Success(None, Some(gradeBoundary.grade), gradeBoundary.result)
                }
              } else if (componentsForCalculation.exists(_._2.grade.contains("F"))) {
                // Any fail grades in lead to a fail grade out
                passFailResult("F")
              } else if (componentsForCalculation.exists(_._2.grade.contains("R"))) {
                // Any resit grades in to a resit grade out
                passFailResult("R")
              } else if (componentsForCalculation.forall(_._2.grade.contains("P"))) {
                // All passes in leads to a pass out
                passFailResult("P")
              } else {
                // ¯\_(ツ)_/¯
                ModuleMarkCalculation.Failure(s"Couldn't automatically select a result from component grades ${componentsForCalculation.flatMap(_._2.grade).mkString(", ")}")
              }
            }
          } else {
            // Each component must have a mark
            val componentsWithMissingMarks = componentsForCalculation.filter(_._2.mark.isEmpty).map(_._1).sortBy(_.sequence)

            if (componentsWithMissingMarks.nonEmpty) ModuleMarkCalculation.Failure(s"Marks are missing for ${componentsWithMissingMarks.map(_.sequence).mkString(", ")}")
            else if (componentsForCalculation.size == 1) {
              // If there's a single component and it's valid, just copy it over
              val validGrades = validGradesForMark(componentsForCalculation.head._2.mark)

              val gb: Option[GradeBoundary] = componentsForCalculation.head._2.grade match {
                case Some(existing) => validGrades.find(_.grade == existing)
                case _ => validGrades.find(_.isDefault)
              }

              gb match {
                case None => ModuleMarkCalculation.Success(componentsForCalculation.head._2.mark, None, None)
                case Some(gradeBoundary) => ModuleMarkCalculation.Success(componentsForCalculation.head._2.mark, Some(gradeBoundary.grade), gradeBoundary.result)
              }
            } else {
              val componentsWithMissingWeighting = components.filter(_._1.scaledWeighting.isEmpty).map(_._1).sortBy(_.sequence)

              if (componentsWithMissingWeighting.nonEmpty) ModuleMarkCalculation.Failure(s"Weightings are missing for ${componentsWithMissingWeighting.map(_.sequence).mkString(", ")}")
              else {
                // If there are any indicator grades, just fail out unless all the grades are the same
                def isIndicatorGrade(ac: AssessmentComponent, s: StudentMarkRecord): Boolean =
                  s.grade.exists(g => assessmentMembershipService.gradesForMark(ac, s.mark, s.resitExpected).find(_.grade == g).exists(!_.isDefault))

                // We know that all weightings are defined and all marks are defined at this point
                val calculatedMark = {
                  // We need to consider all components for marks for weighting, including FM marks
                  val marksForWeighting: Seq[(AssessmentType, String, Option[Int])] =
                    components.map { case (ac, s) =>
                      (ac.assessmentType, ac.sequence, s.mark)
                    }

                  val totalWeighting: BigDecimal = componentsForCalculation.map(_._1.weightingFor(marksForWeighting).get).sum

                  componentsForCalculation.map { case (ac, s) =>
                    val mark = s.mark.get
                    val weighting = ac.weightingFor(marksForWeighting).get

                    mark * (weighting / totalWeighting)
                  }.sum
                }.setScale(0, BigDecimal.RoundingMode.HALF_UP).toInt
                val validGrades = validGradesForMark(Some(calculatedMark))

                val componentsWithIndicatorGrades = componentsForCalculation.filter { case (ac, s) => isIndicatorGrade(ac, s) }.sortBy(_._1.sequence)

                if (componentsWithIndicatorGrades.size == componentsForCalculation.size && componentsForCalculation.forall(_._2.grade == componentsForCalculation.head._2.grade)) {
                  val grade = componentsForCalculation.head._2.grade.get

                  validGrades.find(_.grade == grade) match {
                    case None => ModuleMarkCalculation.Failure(s"Unable to find grade boundary for $grade grade")
                    case Some(gradeBoundary) => ModuleMarkCalculation.Success(Some(calculatedMark), Some(gradeBoundary.grade), gradeBoundary.result)
                  }
                } else if (componentsWithIndicatorGrades.nonEmpty) {
                  ModuleMarkCalculation.Failure(s"Mis-matched indicator grades ${componentsWithIndicatorGrades.map(_._2.grade.get).mkString(", ")} for ${componentsWithIndicatorGrades.map(_._1.sequence).mkString(", ")}")
                } else {
                  validGrades.find(_.isDefault) match {
                    case None => ModuleMarkCalculation.Success(Some(calculatedMark), None, None)
                    case Some(gradeBoundary) => ModuleMarkCalculation.Success(Some(calculatedMark), Some(gradeBoundary.grade), gradeBoundary.result)
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

trait CalculateModuleMarksValidation extends SelfValidating {
  self: ModuleOccurrenceState
    with CalculateModuleMarksRequest
    with ModuleOccurrenceLoadModuleRegistrations
    with AssessmentMembershipServiceComponent =>

  override def validate(errors: Errors): Unit = {
    val doGradeValidation = module.adminDepartment.assignmentGradeValidation
    students.asScala.foreach { case (sprCode, item) =>
      errors.pushNestedPath(s"students[$sprCode]")

      // Check that there's a module registration for the student
      val moduleRegistration = moduleRegistrations.find(_.sprCode == sprCode)

      // We allow returning marks for PWD students so we don't need to filter by "current" members here
      if (moduleRegistration.isEmpty) {
        errors.reject("uniNumber.unacceptable", Array(sprCode), "")
      }

      val isResitting = moduleRegistration.exists { modReg =>
        val universityId = modReg.studentCourseDetails.student.universityId
        componentMarks(universityId).exists(_._2.resitExpected)
      }

      if (item.mark.hasText) {
        try {
          val asInt = item.mark.toInt
          if (asInt < 0 || asInt > 100) {
            errors.rejectValue("mark", "actualMark.range")
          } else if (doGradeValidation) {
            val validGrades = moduleRegistration.map(modReg => assessmentMembershipService.gradesForMark(modReg, Some(asInt), isResitting)).getOrElse(Seq.empty)
            if (item.grade.hasText) {
              if (!validGrades.exists(_.grade == item.grade)) {
                errors.rejectValue("grade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
              } else {
                validGrades.find(_.grade == item.grade).foreach { gb =>
                  if (!item.result.hasText) {
                    item.result = gb.result.map(_.dbValue).orNull
                  } else if (gb.result.exists(_.dbValue != item.result)) {
                    errors.rejectValue("result", "result.invalidSITS", Array(gb.result.get.dbValue), "")
                  }
                }
              }
            } else if (asInt != 0 || module.adminDepartment.assignmentGradeValidationUseDefaultForZero) {
              // This is a bit naughty, validation shouldn't modify state, but it's clearer in the preview if we show what the grade will be
              validGrades.find(_.isDefault).foreach { gb =>
                item.grade = gb.grade

                if (!item.result.hasText) {
                  item.result = gb.result.map(_.dbValue).orNull
                } else if (gb.result.exists(_.dbValue != item.result)) {
                  errors.rejectValue("result", "result.invalidSITS", Array(gb.result.get.dbValue), "")
                }
              }
            }

            if (!item.grade.hasText) {
              errors.rejectValue("grade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
            }
          }
        } catch {
          case _ @ (_: NumberFormatException | _: IllegalArgumentException) =>
            errors.rejectValue("mark", "actualMark.format")
        }
      } else if (doGradeValidation && item.grade.hasText) {
        val validGrades = moduleRegistration.map(modReg => assessmentMembershipService.gradesForMark(modReg, None, isResitting)).getOrElse(Seq.empty)
        if (!validGrades.exists(_.grade == item.grade)) {
          errors.rejectValue("grade", "actualGrade.invalidSITS", Array(validGrades.map(_.grade).mkString(", ")), "")
        } else {
          validGrades.find(_.grade == item.grade).foreach { gb =>
            if (!item.result.hasText) {
              item.result = gb.result.map(_.dbValue).orNull
            } else if (gb.result.exists(_.dbValue != item.result)) {
              errors.rejectValue("result", "result.invalidSITS", Array(gb.result.get.dbValue), "")
            }
          }
        }
      }

      errors.popNestedPath()
    }
  }
}
