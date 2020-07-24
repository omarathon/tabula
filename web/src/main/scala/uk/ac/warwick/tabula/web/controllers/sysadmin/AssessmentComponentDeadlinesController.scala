package uk.ac.warwick.tabula.web.controllers.sysadmin

import java.sql.{Date, ResultSet, Types}

import javax.sql.DataSource
import javax.validation.Valid
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.util.CellReference
import org.apache.poi.xssf.eventusermodel.{ReadOnlySharedStringsTable, XSSFReader}
import org.apache.poi.xssf.usermodel.XSSFComment
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.{LocalDate, YearMonth}
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.`object`.{MappingSqlQuery, SqlUpdate}
import org.springframework.jdbc.core.SqlParameter
import org.springframework.stereotype.Controller
import org.springframework.ui.ModelMap
import org.springframework.validation.{BindingResult, Errors}
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, PostMapping, RequestMapping}
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import org.xml.sax.InputSource
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, UpstreamAssessmentGroup}
import uk.ac.warwick.tabula.data.{AutowiringTransactionalComponent, TransactionalComponent}
import uk.ac.warwick.tabula.helpers.JodaConverters._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.coursework.docconversion.AbstractXslxSheetHandler
import uk.ac.warwick.tabula.services.scheduling.{AutowiringSitsDataSourceComponent, SitsDataSourceComponent}
import uk.ac.warwick.tabula.services.{AssessmentMembershipServiceComponent, AutowiringAssessmentMembershipServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.{BreadCrumb, Routes}
import uk.ac.warwick.tabula.{AcademicYear, ToString}

import scala.jdk.CollectionConverters._
import scala.util.{Try, Using}

@Controller
@Profile(Array("dev", "test", "production")) // Not on the sandbox
@RequestMapping(Array("/sysadmin/departments/{department}/assessment-component-deadlines"))
class AssessmentComponentDeadlinesController extends BaseSysadminController {

  validatesSelf[SelfValidating]

  @ModelAttribute("command")
  def command(@PathVariable department: Department): AssessmentComponentDeadlinesController.Command =
    AssessmentComponentDeadlinesController.command(department)

  @ModelAttribute("breadcrumbs")
  def breadcrumbs: Seq[BreadCrumb] = Seq(
    SysadminBreadcrumbs.Departments.Home
  )

  @RequestMapping
  def formView: String = "sysadmin/departments/assessment-component-deadlines"

  @PostMapping
  def submit(
    @Valid @ModelAttribute("command") command: AssessmentComponentDeadlinesController.Command,
    errors: Errors,
    @PathVariable department: Department,
    model: ModelMap,
  )(implicit redirectAttributes: RedirectAttributes): String =
    if (errors.hasErrors) {
      model.addAttribute("flash__error", "flash.hasErrors")
      model.addAttribute("errors", errors)
      formView
    } else {
      command.apply()
      RedirectFlashing(Routes.sysadmin.Departments.department(department), "flash__success" -> "flash.assessmentComponent.deadlinesSet")
    }
}

object AssessmentComponentDeadlinesController {
  type Result = Seq[(UpstreamAssessmentGroup, LocalDate)]
  type Command = Appliable[Result] with SelfValidating with BindListener

  val DeadlineLocalDateFormats: Seq[DateTimeFormatter] = Seq(
    DateTimeFormat.forPattern("dd-MMM-yy"),
    DateTimeFormat.forPattern("dd-MM-yyyy"),
    DateTimeFormat.forPattern("M/d/yy"),
    DateTimeFormat.forPattern("dd/MM/yyyy"),
    DateTimeFormat.forPattern("d.M.yyyy"),
  )

  val DeadlineYearMonthFormats: Seq[DateTimeFormatter] = Seq(
    DateTimeFormat.forPattern("MMM-yy"),
  )

  def parseDeadlineDate(in: String): LocalDate = in match {
    case r"""(?i)^\s*week (\d+)${weekNumber}\s*$$""" =>
      AcademicYear.now().weeks(weekNumber.toInt).firstDay

    case s if DeadlineYearMonthFormats.exists(df => Try(YearMonth.parse(s, df)).isSuccess) =>
      DeadlineYearMonthFormats.to(LazyList).map(df => Try(YearMonth.parse(s, df)))
        .find(_.isSuccess)
        .get.get.toLocalDate(1)

    case s => DeadlineLocalDateFormats.to(LazyList).map(df => Try(df.parseLocalDate(s)))
      .find(_.isSuccess)
      .map(_.get)
      .getOrElse(throw new IllegalArgumentException(s"Couldn't parse date $in"))
  }

  def CountModuleAssessmentDeadlineRowsSql(sitsSchema: String) =
    s"""
      select count(*) as row_count
      from $sitsSchema.cam_mad
      where mod_code = :module_code
        and map_code = :module_code
        and psl_code = 'Y' -- period slot code of Y (year)
        and mav_occur = :occurrence
        and mab_seq = :sequence
        and ayr_code = :academic_year_code
    """

  class CountModuleAssessmentDeadlineRowsQuery(ds: DataSource, sitsSchema: String) extends MappingSqlQuery[Int](ds, CountModuleAssessmentDeadlineRowsSql(sitsSchema)) {
    declareParameter(new SqlParameter("module_code", Types.VARCHAR))
    declareParameter(new SqlParameter("occurrence", Types.VARCHAR))
    declareParameter(new SqlParameter("sequence", Types.VARCHAR))
    declareParameter(new SqlParameter("academic_year_code", Types.VARCHAR))
    compile()

    override def mapRow(rs: ResultSet, rowNumber: Int): Int = rs.getInt("row_count")
  }

  def UpdateModuleAssessmentDeadlineSql(sitsSchema: String) =
    s"""
      update $sitsSchema.cam_mad
      set mad_ddate = :deadline
      where mod_code = :module_code
        and map_code = :module_code
        and psl_code = 'Y' -- period slot code of Y (year)
        and mav_occur = :occurrence
        and mab_seq = :sequence
        and ayr_code = :academic_year_code
    """

  class UpdateModuleAssessmentDeadlineQuery(ds: DataSource, sitsSchema: String) extends SqlUpdate(ds, UpdateModuleAssessmentDeadlineSql(sitsSchema)) {
    declareParameter(new SqlParameter("module_code", Types.VARCHAR))
    declareParameter(new SqlParameter("occurrence", Types.VARCHAR))
    declareParameter(new SqlParameter("sequence", Types.VARCHAR))
    declareParameter(new SqlParameter("academic_year_code", Types.VARCHAR))
    declareParameter(new SqlParameter("deadline", Types.DATE))
    compile()
  }

  def command(department: Department): Command =
    new AssessmentComponentDeadlinesCommandInternal(department)
      with ComposableCommand[Result]
      with AssessmentComponentDeadlinesRequest
      with AssessmentComponentDeadlinesBindListener
      with AssessmentComponentDeadlinesValidation
      with AssessmentComponentDeadlinesPermissions
      with AssessmentComponentDeadlinesDescription
      with AutowiringTransactionalComponent
      with AutowiringAssessmentMembershipServiceComponent
      with AutowiringSitsDataSourceComponent {
      var sitsSchema: String = Wire.property("${schema.sits}")
    }

  abstract class AssessmentComponentDeadlinesCommandInternal(val department: Department)
    extends CommandInternal[Result]
      with AssessmentComponentDeadlinesState  {
    self: AssessmentComponentDeadlinesRequest
      with AssessmentMembershipServiceComponent
      with SitsDataSourceComponent
      with TransactionalComponent =>

    def sitsSchema: String
    lazy val updateQuery: UpdateModuleAssessmentDeadlineQuery = new UpdateModuleAssessmentDeadlineQuery(sitsDataSource, sitsSchema)

    override def applyInternal(): Seq[(UpstreamAssessmentGroup, LocalDate)] = transactional() {
      items.asScala.flatMap { item =>
        val groups =
          assessmentMembershipService.getUpstreamAssessmentGroups(item.academicYear, item.moduleCode)
            .find(uag => uag.occurrence == item.occurrence && uag.sequence == item.sequence)

        // Update MAD record in SITS
        require(updateQuery.updateByNamedParam(JHashMap(
          "module_code" -> item.moduleCode,
          "occurrence" -> item.occurrence,
          "sequence" -> item.sequence,
          "academic_year_code" -> item.academicYear.toString,
          "deadline" -> Option(item.deadline).map(d => Date.valueOf(d.asJava)).orNull,
        )) == 1, s"Unable to update deadline for $groups")

        // Update the UpstreamAssessmentGroup records too
        groups.foreach { uag =>
          uag.deadline = Option(item.deadline)
          assessmentMembershipService.save(uag)
        }

        groups.map(_ -> item.deadline)
      }.toSeq
    }
  }

  trait AssessmentComponentDeadlinesState {
    def department: Department
  }

  class AssessmentComponentDeadlines extends ToString {
    var moduleCode: String = _
    var occurrence: String = _
    var sequence: String = _
    var academicYear: AcademicYear = AcademicYear.now() // default to current
    var deadline: LocalDate = _

    override def toStringProps: Seq[(String, Any)] = Seq(
      "moduleCode" -> moduleCode,
      "occurrence" -> occurrence,
      "sequence" -> sequence,
      "academicYear" -> academicYear,
      "deadline" -> deadline
    )
  }

  trait AssessmentComponentDeadlinesRequest {
    var file: UploadedFile = new UploadedFile
    var items: JList[AssessmentComponentDeadlines] = JArrayList()
  }

  trait AssessmentComponentDeadlinesBindListener extends BindListener {
    self: AssessmentComponentDeadlinesRequest
      with TransactionalComponent =>

    override def onBind(result: BindingResult): Unit = transactional() {
      if (file.isMissing) {
        result.rejectValue("file", "NotEmpty")
      } else {
        val fileNames = file.fileNames.map(_.toLowerCase)
        val invalidFiles = fileNames.filterNot(f => f.endsWith(".xlsm") || f.endsWith(".xlsx"))

        if (invalidFiles.nonEmpty) {
          if (invalidFiles.size == 1) result.rejectValue("file", "file.wrongtype.one", Array(invalidFiles.mkString(""), ".xlsm, .xlsx"), "")
          else result.rejectValue("file", "file.wrongtype", Array(invalidFiles.mkString(", "), ".xlsm, .xlsx"), "")
        }
      }

      if (!result.hasErrors) {
        result.pushNestedPath("file")
        file.onBind(result)
        result.popNestedPath()

        file.attached.asScala.filter(_.hasData).foreach { attachment =>
          Using.resource(attachment.asByteSource.openStream()) { stream =>
            val pkg = OPCPackage.open(stream)
            val sst = new ReadOnlySharedStringsTable(pkg)
            val reader = new XSSFReader(pkg)
            val styles = reader.getStylesTable

            val handler = new AbstractXslxSheetHandler(styles, sst, items) {
              override def newCurrentItem: AssessmentComponentDeadlines = new AssessmentComponentDeadlines()
              override def cell(cellReference: String, formattedValue: String, comment: XSSFComment): Unit = {
                val col = new CellReference(cellReference).getCol
                if (isFirstRow) {
                  columnMap(col) = formattedValue
                } else if (columnMap.asJava.containsKey(col) && formattedValue.hasText) {
                  columnMap(col).toLowerCase match {
                    case "modulecode" | "module code" | "module_code" =>
                      currentItem.moduleCode = formattedValue
                    case "sequence" =>
                      currentItem.sequence = formattedValue
                    case "occurrence" =>
                      currentItem.occurrence = formattedValue
                    case "academicyear" | "academic year" if formattedValue.matches("^\\d{4}$") =>
                      currentItem.academicYear = AcademicYear.starting(formattedValue.toInt)
                    case "academicyear" | "academic year" =>
                      currentItem.academicYear = AcademicYear.parse(formattedValue)
                    case "deadline" | "confirmed deadline" =>
                      currentItem.deadline = parseDeadlineDate(formattedValue)
                    case _ => // ignore anything else
                  }
                }
              }
            }
            val parser = handler.fetchSheetParser

            val sheets = reader.getSheetsData
            while (sheets.hasNext) {
              Using.resource(sheets.next()) { sheet =>
                parser.parse(new InputSource(sheet))
              }
            }
          }
        }
      }
    }
  }

  trait AssessmentComponentDeadlinesValidation extends SelfValidating with Logging {
    self: AssessmentComponentDeadlinesState
      with AssessmentComponentDeadlinesRequest
      with AssessmentMembershipServiceComponent
      with SitsDataSourceComponent =>

    def sitsSchema: String
    lazy val countQuery: CountModuleAssessmentDeadlineRowsQuery = new CountModuleAssessmentDeadlineRowsQuery(sitsDataSource, sitsSchema)

    override def validate(errors: Errors): Unit = {
      // For each item, get the matching assessment groups
      items.asScala.zipWithIndex.foreach { case (item, index) =>
        errors.pushNestedPath(s"items[$index]")

        if (!item.moduleCode.hasText) errors.rejectValue("moduleCode", "NotEmpty")
        if (!item.occurrence.hasText) errors.rejectValue("occurrence", "NotEmpty")
        if (!item.sequence.hasText) errors.rejectValue("sequence", "NotEmpty")
        if (item.academicYear == null) errors.rejectValue("academicYear", "NotEmpty")

        if (item.moduleCode.hasText && item.occurrence.hasText && item.sequence.hasText && item.academicYear != null) {
          val groups =
            assessmentMembershipService.getUpstreamAssessmentGroups(item.academicYear, item.moduleCode)
              .find(uag => uag.occurrence == item.occurrence && uag.sequence == item.sequence)

          if (groups.isEmpty) {
            logger.info(s"No assessment groups found for $item")
            errors.rejectValue("", "NotEmpty")
          } else if (groups.exists(_.assessmentComponent.exists(_.module.adminDepartment != department))) {
            logger.info(s"Wrong department for $item (${groups.flatMap(_.assessmentComponent.map(_.module.adminDepartment.code))})")
            errors.rejectValue("", "NotEmpty")
          } else {
            // Make sure a MAD row exists
            val rowCount = countQuery.executeByNamedParam(JHashMap(
              "module_code" -> item.moduleCode,
              "occurrence" -> item.occurrence,
              "sequence" -> item.sequence,
              "academic_year_code" -> item.academicYear.toString,
            )).get(0)

            if (rowCount != 1) {
              logger.info(s"No MAD row found for $item")
              errors.rejectValue("", "NotEmpty")
            }
          }
        }

        errors.popNestedPath()
      }
    }
  }

  trait AssessmentComponentDeadlinesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
    self: AssessmentComponentDeadlinesState =>

    override def permissionsCheck(p: PermissionsChecking): Unit = {
      mandatory(department)
      p.PermissionCheck(Permissions.ImportSystemData)
    }
  }

  trait AssessmentComponentDeadlinesDescription extends Describable[Result] {
    self: AssessmentComponentDeadlinesState =>

    override lazy val eventName: String = "AssessmentComponentDeadlines"

    override def describe(d: Description): Unit =
      d.department(department)

    override def describeResult(d: Description, result: Result): Unit =
      d.property(
        "deadlines" -> result.groupBy(_._1.moduleCode).map { case (moduleCode, results) =>
          moduleCode -> results.map { case (uag, deadline) =>
            uag.sequence -> Option(deadline).map(_.toString).getOrElse("-")
          }.toMap
        }
      )
  }
}
