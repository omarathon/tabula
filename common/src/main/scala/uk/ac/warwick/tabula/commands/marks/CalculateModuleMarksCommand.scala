package uk.ac.warwick.tabula.commands.marks

import freemarker.core.TemplateHTMLOutputModel
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
import uk.ac.warwick.tabula.data.model.forms.FormattedHtml
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
  sealed trait ModuleMarkCalculation {
    def isSuccessful: Boolean
    def isMultiple: Boolean
  }
  object ModuleMarkCalculation {
    case class Success(mark: Option[Int], grade: Option[String], result: Option[ModuleResult], comments: Option[String] = None) extends ModuleMarkCalculation {
      override val isSuccessful: Boolean = true
      override val isMultiple: Boolean = false
    }
    case class SuggestedModuleMarkCalculation(title: String, description: TemplateHTMLOutputModel, calculation: ModuleMarkCalculation)
    case class Multiple(reason: TemplateHTMLOutputModel, suggestions: Seq[SuggestedModuleMarkCalculation]) extends ModuleMarkCalculation {
      override val isSuccessful: Boolean = true
      override val isMultiple: Boolean = true
    }
    case class Failure(message: String) extends ModuleMarkCalculation {
      override val isSuccessful: Boolean = false
      override val isMultiple: Boolean = false
    }

    // Some reusable results (just trying to keep these together)
    object Success {
      def apply(mark: Option[Int], gradeBoundary: Option[GradeBoundary]): Success = Success(mark, gradeBoundary.map(_.grade), gradeBoundary.flatMap(_.result))
      def PassFail(gradeBoundary: GradeBoundary): Success = Success(None, Some(gradeBoundary.grade), gradeBoundary.result)
    }

    object MissingMarkAdjustment {
      val NoCalculationPossible: Success = Success(None, Some(GradeBoundary.ForceMajeureMissingComponentGrade), Some(ModuleResult.Pass), comments = Some("Missing mark adjustment - learning outcomes assessed, unable to calculate an overall module mark"))
      val AllComponentsMissing: Success = Success(None, Some(GradeBoundary.ForceMajeureMissingComponentGrade), Some(ModuleResult.Deferred), comments = Some("Missing mark adjustment - learning outcomes not assessed"))

      // Option 1) Use the remaining components to calculate a mark, with credit
      // Option 2) A force majeure pass, with credit
      // Option 3) No result, no credit
      def SomeComponentsMissing(calculation: ModuleMarkCalculation): Multiple = Multiple(
        reason = FormattedHtml(
          """
            |At least one component has been recorded as missed due to force majeure.
            |Which calculation to use depends on whether there have been sufficient other components completed to assess the learning outcomes.
            |[Guidance on the options in 2019/20 is available on the Teaching Continuity site](https://warwick.ac.uk/insite/coronavirus/staff/teaching/marksandexamboards/guidance/decisions-first-year/#credit).
            |""".stripMargin
        ),
        suggestions = Seq(
          SuggestedModuleMarkCalculation(
            title = "Option 1 (Overall Mark and Credit Awarded)",
            description = FormattedHtml("One or more assessments have been cancelled but the module learning outcomes have still been assessed. Sufficient assessment has taken place for student performance to be differentiated, so a module mark is calculated from a weighted average of the remaining components and pass/fail result is determined accordingly. Students will be awarded credit for the module where they pass."),
            calculation = calculation match {
              case s: ModuleMarkCalculation.Success => s.copy(comments = Some("Missing mark adjustment - learning outcomes assessed, weighted mark"))
              case o => o
            },
          ),
          SuggestedModuleMarkCalculation(
            title = "Option 2 (Pass/fail and Credit Awarded)",
            description = FormattedHtml("One or more assessments have been cancelled and the module learning outcomes have still been assessed. However, sufficient assessment has not taken place for student performance to be differentiated, so a module mark will not be calculated and only a pass/fail result will be determined. Students will be awarded credit for the module where they pass."),
            calculation = NoCalculationPossible,
          ),
          SuggestedModuleMarkCalculation(
            title = "Option 3 (No Result, No Credit)",
            description = FormattedHtml("All or most assessment has been cancelled and there is insufficient evidence available that the student has achieved the learning outcomes of the module, so no result can be recorded and no credit can be awarded."),
            calculation = AllComponentsMissing,
          ),
        ),
      )
    }

    object Failure {
      val NoModuleRegistration: Failure = Failure("No module registration found")
      val NoComponentMarks: Failure = Failure("There were no component marks")
      val NoMarkScheme: Failure = Failure("There is no mark scheme associated with the module registration")
      def MarksAndGradesMissingFor(sequences: Seq[String]): Failure = Failure(s"Marks and grades are missing for ${sequences.mkString(", ")}")

      object PassFail {
        val MismatchedMarkScheme: Failure = Failure("Not all components are pass/fail but module is")
        def GradesMissingFor(sequences: Seq[String]): Failure = Failure(s"Grades are missing for ${sequences.mkString(", ")}")
        val NoDefaultGrade: Failure = Failure("Unable to select a valid grade")
        def MismatchedGrades(grades: Seq[String]): Failure = Failure(s"Couldn't automatically select a result from component grades ${grades.mkString(", ")}")
      }

      def MarksMissingFor(sequences: Seq[String]): Failure = Failure(s"Marks are missing for ${sequences.mkString(", ")}")
      def WeightingsMissingFor(sequences: Seq[String]): Failure = Failure(s"Weightings are missing for ${sequences.mkString(", ")}")
      def NoGradeBoundary(grade: String): Failure = Failure(s"Unable to find grade boundary for $grade grade")
      def MismatchedIndicatorGrades(grades: Seq[String], sequences: Seq[String]): Failure = Failure(s"Mis-matched indicator grades ${grades.mkString(", ")} for ${sequences.mkString(", ")}")
      val General: Failure = Failure("Couldn't automatically calculate a module result")
    }
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
        }.getOrElse((Map.empty[AssessmentComponent, (StudentMarkRecord, Option[BigDecimal])], ModuleMarkCalculation.Failure.NoModuleRegistration))

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
                      case "Result" =>
                        currentItem.result = formattedValue
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
        case ModuleMarkCalculation.Success(mark, grade, result, comments) =>
          mark.foreach(m => s.mark = m.toString)
          grade.foreach(s.grade = _)
          result.foreach(r => s.result = r.dbValue)
          comments.foreach(s.comments = _)

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

    if (components.isEmpty) ModuleMarkCalculation.Failure.NoComponentMarks
    else if (!moduleRegistration.marksCode.hasText) ModuleMarkCalculation.Failure.NoMarkScheme
    else {
      val componentsWithMissingMarkOrGrades = components.filter { case (_, s) => s.mark.isEmpty && s.grade.isEmpty }.map(_._1).sortBy(_.sequence)

      if (componentsWithMissingMarkOrGrades.nonEmpty) ModuleMarkCalculation.Failure.MarksAndGradesMissingFor(componentsWithMissingMarkOrGrades.map(_.sequence))
      else {
        val componentsForCalculation = components.filter { case (_, s) => !s.grade.contains(GradeBoundary.ForceMajeureMissingComponentGrade) }
        if (componentsForCalculation.isEmpty && components.forall(_._2.grade.contains(GradeBoundary.ForceMajeureMissingComponentGrade))) {
          // No components have been assessed, so it is not possible to grant credit.
          ModuleMarkCalculation.MissingMarkAdjustment.AllComponentsMissing
        } else {
          lazy val calculation = {
            def validGradesForMark(m: Option[Int]) =
              assessmentMembershipService.gradesForMark(moduleRegistration, m, componentsForCalculation.map(_._2.upstreamAssessmentGroupMember.currentResitAttempt).maxOption.flatten)

            // Is this a pass/fail module?
            if (moduleRegistration.passFail) {
              // All the components must also be passFail
              if (componentsForCalculation.exists { case (c, _) => !moduleRegistration.marksCode.maybeText.contains(c.marksCode) }) {
                ModuleMarkCalculation.Failure.PassFail.MismatchedMarkScheme
              } else {
                val validGrades = validGradesForMark(None)
                def passFailResult(grade: String): ModuleMarkCalculation =
                  validGrades.find(_.grade == grade) match {
                    case None => ModuleMarkCalculation.Failure.NoGradeBoundary(grade)
                    case Some(gradeBoundary) => ModuleMarkCalculation.Success.PassFail(gradeBoundary)
                  }

                // Each component must have a grade
                val componentsWithMissingGrades = componentsForCalculation.filter(_._2.grade.isEmpty).map(_._1).sortBy(_.sequence)

                if (componentsWithMissingGrades.nonEmpty) ModuleMarkCalculation.Failure.PassFail.GradesMissingFor(componentsWithMissingGrades.map(_.sequence))
                else if (componentsForCalculation.size == 1) {
                  // If there's a single component and it's valid, just copy it over
                  val gb: Option[GradeBoundary] = componentsForCalculation.head._2.grade match {
                    case Some(existing) => validGrades.find(_.grade == existing)
                    case _ => validGrades.find(_.isDefault)
                  }

                  gb match {
                    case None => ModuleMarkCalculation.Failure.PassFail.NoDefaultGrade
                    case Some(gradeBoundary) => ModuleMarkCalculation.Success.PassFail(gradeBoundary)
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
                  ModuleMarkCalculation.Failure.PassFail.MismatchedGrades(componentsForCalculation.flatMap(_._2.grade))
                }
              }
            } else {
              // Each component must have a mark
              val componentsWithMissingMarks = componentsForCalculation.filter(_._2.mark.isEmpty).map(_._1).sortBy(_.sequence)

              if (componentsWithMissingMarks.nonEmpty)
                ModuleMarkCalculation.Failure.MarksMissingFor(componentsWithMissingMarks.map(_.sequence))
              else {
                val componentsWithMissingWeighting = components.filter(_._1.scaledWeighting.isEmpty).map(_._1).sortBy(_.sequence)

                if (componentsWithMissingWeighting.nonEmpty) ModuleMarkCalculation.Failure.WeightingsMissingFor(componentsWithMissingWeighting.map(_.sequence))
                else {
                  // If there are any indicator grades, just fail out unless all the grades are the same
                  def isIndicatorGrade(ac: AssessmentComponent, s: StudentMarkRecord): Boolean = {
                    s.grade.exists { g =>
                      assessmentMembershipService.gradesForMark(ac, s.mark, s.upstreamAssessmentGroupMember.currentResitAttempt)
                        .find(_.grade == g)
                        .exists(!_.isDefault)
                    }
                  }

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

                      lazy val markCap = assessmentMembershipService.passMark(moduleRegistration, s.upstreamAssessmentGroupMember.currentResitAttempt)

                      val cappedMark = if(s.resitExpected && !s.furtherFirstSit) {
                        markCap.map(cap => if (mark > cap) cap else mark).getOrElse(mark)
                      } else mark

                      cappedMark * (weighting / totalWeighting)
                    }.sum
                  }.setScale(0, BigDecimal.RoundingMode.HALF_UP).toInt
                  val validGrades = validGradesForMark(Some(calculatedMark))

                  val componentsWithIndicatorGrades = componentsForCalculation.filter { case (ac, s) => isIndicatorGrade(ac, s) }.sortBy(_._1.sequence)

                  if (componentsWithIndicatorGrades.size == componentsForCalculation.size && componentsForCalculation.nonEmpty && componentsForCalculation.forall(_._2.grade == componentsForCalculation.head._2.grade)) {
                    val grade = componentsForCalculation.head._2.grade.get

                    validGrades.find(_.grade == grade) match {
                      case None => ModuleMarkCalculation.Failure.NoGradeBoundary(grade)
                      case Some(gradeBoundary) => ModuleMarkCalculation.Success(Some(calculatedMark), Some(gradeBoundary))
                    }
                  } else if (componentsWithIndicatorGrades.nonEmpty) {
                    ModuleMarkCalculation.Failure.MismatchedIndicatorGrades(componentsWithIndicatorGrades.flatMap(_._2.grade), componentsWithIndicatorGrades.map(_._1.sequence))
                  } else if (componentsForCalculation.isEmpty) {
                    ModuleMarkCalculation.Failure.General
                  } else {
                    validGrades.find(_.isDefault) match {
                      case None => ModuleMarkCalculation.Success(Some(calculatedMark), None, None)
                      case Some(gradeBoundary) => ModuleMarkCalculation.Success(Some(calculatedMark), Some(gradeBoundary))
                    }
                  }
                }
              }
            }
          }

          if (componentsForCalculation.nonEmpty && components.exists(_._2.grade.contains(GradeBoundary.ForceMajeureMissingComponentGrade))) {
            // We have at least one FM component but not all, so we can't do a single calculation. We need to offer multiple options
            ModuleMarkCalculation.MissingMarkAdjustment.SomeComponentsMissing(calculation)
          } else calculation
        }
      }
    }
  }
}

trait CalculateModuleMarksValidation extends SelfValidating {
  self: ModuleOccurrenceState
    with CalculateModuleMarksRequest
    with CalculateModuleMarksLoadModuleRegistrations
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

      val currentResitAttempt = moduleRegistration.flatMap { modReg =>
        val universityId = modReg.studentCourseDetails.student.universityId
        componentMarks(universityId).map(_._2.upstreamAssessmentGroupMember.currentResitAttempt).maxOption.flatten
      }

      if (item.mark.hasText) {
        if (item.grade.maybeText.contains(GradeBoundary.ForceMajeureMissingComponentGrade)) {
          errors.rejectValue("mark", "actualMark.notEmpty.forceMajeure")
        }

        try {
          val asInt = item.mark.toInt
          if (asInt < 0 || asInt > 100) {
            errors.rejectValue("mark", "actualMark.range")
          } else if (doGradeValidation) {
            val validGrades = moduleRegistration.map(modReg => assessmentMembershipService.gradesForMark(modReg, Some(asInt), currentResitAttempt)).getOrElse(Seq.empty)
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
        val validGrades = moduleRegistration.map(modReg => assessmentMembershipService.gradesForMark(modReg, None, currentResitAttempt)).getOrElse(Seq.empty)
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

      // TAB-8428 if the mark, grade or result differ from the current mark, grade or result AND they differ from the calculated
      // mark, grade and result, comment becomes mandatory
      studentModuleMarkRecordsAndCalculations.find(_._1.sprCode == item.sprCode).foreach { case (currentModuleMarkRecord, _, calculation) =>
        def matchesCalculation(calculation: ModuleMarkCalculation.Success): Boolean =
          item.mark.maybeText.exists(m => m != calculation.mark.map(_.toString).getOrElse("") && m != currentModuleMarkRecord.mark.map(_.toString).getOrElse("")) ||
          item.grade.maybeText.exists(g => g != calculation.grade.getOrElse("") && g != currentModuleMarkRecord.grade.getOrElse("")) ||
          item.result.maybeText.exists(r => r != calculation.result.map(_.dbValue).getOrElse("") && r != currentModuleMarkRecord.result.map(_.dbValue).getOrElse(""))

        lazy val doesntMatchCalculation = calculation match {
          case c: ModuleMarkCalculation.Success => matchesCalculation(c)
          case m: ModuleMarkCalculation.Multiple => m.suggestions.map(_.calculation).collect { case c: ModuleMarkCalculation.Success => c }.exists(matchesCalculation)
          case _ => false // If the calculation was a failure, allow it through
        }

        if (!item.comments.hasText && doesntMatchCalculation) {
          errors.rejectValue("comments", "moduleMarkCalculation.mismatch.noComment")
        }
      }

      errors.popNestedPath()
    }
  }
}

trait ClearRecordedModuleMarksState {
  def currentUser: CurrentUser
}

/**
 * A mixin trait for component mark operations which mutate actual or agreed component marks, and therefore
 * mean the module mark/grade needs clearing for recalculation.
 */
trait ClearRecordedModuleMarks {
  self: ClearRecordedModuleMarksState
    with ModuleRegistrationMarksServiceComponent
    with ModuleRegistrationServiceComponent =>

  def clearRecordedModuleMarksFor(recordedAssessmentComponentStudent: RecordedAssessmentComponentStudent): Option[RecordedModuleRegistration] = {
    // There might be multiple module registrations here, for different SPR codes. Just blat them all
    moduleRegistrationService.getByModuleOccurrence(recordedAssessmentComponentStudent.moduleCode, recordedAssessmentComponentStudent.academicYear, recordedAssessmentComponentStudent.occurrence)
      .filter(_.studentCourseDetails.student.universityId == recordedAssessmentComponentStudent.universityId)
      .filter { moduleRegistration =>
        val existingRecordedModuleRegistration =
          moduleRegistrationMarksService.getRecordedModuleRegistration(moduleRegistration)

        moduleRegistration.firstDefinedMark.nonEmpty ||
        moduleRegistration.firstDefinedGrade.nonEmpty ||
        Option(moduleRegistration.moduleResult).nonEmpty ||
        existingRecordedModuleRegistration.exists { recordedModuleRegistration =>
          recordedModuleRegistration.latestMark.nonEmpty ||
          recordedModuleRegistration.latestGrade.nonEmpty ||
          recordedModuleRegistration.latestResult.nonEmpty
        }
      }
      .map(moduleRegistrationMarksService.getOrCreateRecordedModuleRegistration)
      .map { recordedModuleRegistration =>
        recordedModuleRegistration.addMark(
          uploader = currentUser.apparentUser,
          mark = None,
          grade = None,
          result = None,
          source = RecordedModuleMarkSource.ComponentMarkChange,
          markState = recordedModuleRegistration.latestState.getOrElse(MarkState.UnconfirmedActual),
          comments = "Module mark calculation reset by component mark change",
        )

        moduleRegistrationMarksService.saveOrUpdate(recordedModuleRegistration)

        recordedModuleRegistration
      }
      .headOption
  }
}
