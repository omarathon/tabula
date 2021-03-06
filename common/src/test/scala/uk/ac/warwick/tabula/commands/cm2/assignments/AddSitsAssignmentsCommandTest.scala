package uk.ac.warwick.tabula.commands.cm2.assignments

import org.joda.time.{DateTime, LocalDate}
import org.springframework.beans.MutablePropertyValues
import org.springframework.validation.BindException
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.{CustomDataBinder, NoAutoGrownNestedPaths}

import scala.jdk.CollectionConverters._

class AddSitsAssignmentsCommandTest extends TestBase with Mockito {

  trait Fixture {
    val thisDepartment: Department = Fixtures.department(code = "ls", name = "Life Sciences")
    val module1: Module = Fixtures.module(code = "ls101")
    val module2: Module = Fixtures.module(code = "ls102")
    val module3: Module = Fixtures.module(code = "ls103")

    val upstream1: AssessmentComponent = Fixtures.assessmentComponent(module = module1, number = 1)
    val upstream2: AssessmentComponent = Fixtures.assessmentComponent(module = module2, number = 2)
    val upstream3: AssessmentComponent = Fixtures.assessmentComponent(module = module3, number = 3)
    val assessmentGroup1: UpstreamAssessmentGroup = Fixtures.assessmentGroup(upstream1)
    val assessmentGroup3: UpstreamAssessmentGroup = Fixtures.assessmentGroup(upstream3)

    thisDepartment.modules.add(module1)
    thisDepartment.modules.add(module2)
    thisDepartment.modules.add(module3)

    val thisModuleAndDepartmentService: ModuleAndDepartmentService = smartMock[ModuleAndDepartmentService]
    val thisAssignmentService: AssessmentService = smartMock[AssessmentService]
    val thisAssignmentMembershipService: AssessmentMembershipService = smartMock[AssessmentMembershipService]

    thisModuleAndDepartmentService.getModuleByCode(module1.code) returns Option(module1)
    thisModuleAndDepartmentService.getModuleByCode(module2.code) returns Option(module2)
    thisModuleAndDepartmentService.getModuleByCode(module3.code) returns Option(module3)

    thisAssignmentService.getAssignmentByNameYearModule(any[String], any[AcademicYear], any[Module]) returns Seq()
  }

  @Test def validate(): Unit = new Fixture {
    withUser("cuscav") {
      val validator = new AddSitsAssignmentsValidation with AddSitsAssignmentsCommandState
        with ModuleAndDepartmentServiceComponent with AssessmentServiceComponent {
        val academicYear = AcademicYear(2016)
        val department: Department = thisDepartment
        val user: CurrentUser = currentUser
        val moduleAndDepartmentService: ModuleAndDepartmentService = thisModuleAndDepartmentService
        val assessmentService: AssessmentService = thisAssignmentService
      }

      validator.sitsAssignmentItems = Seq(
        item(upstream1, include = true, optionsId = "A"),
        item(upstream2, include = false, optionsId = null),
        item(upstream3, include = true, "A", openEnded = true)
      ).asJava
      validator.optionsMap = Map(
        "A" -> new SharedAssignmentPropertiesForm
      ).asJava

      val errors = new BindException(validator, "command")
      validator.validate(errors)
      errors.hasErrors should be (false)
    }
  }

  @Test def applyCommand(): Unit = {
    new Fixture {
      withUser("cuscav") {
        val cmd = new AddSitsAssignmentsCommandInternal(thisDepartment, AcademicYear(2016), currentUser) with AddSitsAssignmentsCommandState
          with ModuleAndDepartmentServiceComponent with AssessmentServiceComponent with AssessmentMembershipServiceComponent {
          val moduleAndDepartmentService: ModuleAndDepartmentService = thisModuleAndDepartmentService
          val assessmentService: AssessmentService = thisAssignmentService
          val assessmentMembershipService: AssessmentMembershipService = thisAssignmentMembershipService
        }

        cmd.sitsAssignmentItems = Seq(
          item(upstream1, include = true, optionsId = "A"),
          item(upstream2, include = false, optionsId = null),
          item(upstream3, include = true, "A", openEnded = true)
        ).asJava
        cmd.optionsMap = Map(
          "A" -> new SharedAssignmentPropertiesForm
        ).asJava

        val result = cmd.applyInternal()

        result.exists(_.module == module1) should be (true)
        val module1result = result.find(_.module == module1).get
        module1result.name should be("Assignment 1")
        //check the default fields were added.
        withClue("Expecting attachment field.") {
          module1result.attachmentField should be(Symbol("defined"))
        }
        withClue("Expecting comment field.") {
          module1result.commentField should be(Symbol("defined"))
        }
        withClue("Expected not open ended") {
          assert(module1result.openEnded === false)
        }

        result.exists(_.module == module3) should be (true)
        val module3result = result.find(_.module == module3).get
        module3result.name should be("Assignment 3")
        //check the default fields were added.
        withClue("Expecting attachment field.") {
          module3result.attachmentField should be(Symbol("defined"))
        }
        withClue("Expecting comment field.") {
          module3result.commentField should be(Symbol("defined"))
        }
        withClue("Expected open ended") {
          assert(module3result.openEnded === true)
        }
      }
    }
  }

  @Test def optionsMapBinding(): Unit = {
    new Fixture {
      val cmd = new AddSitsAssignmentsCommandInternal(null, AcademicYear(2016), null) with AddSitsAssignmentsCommandState
        with ModuleAndDepartmentServiceComponent with AssessmentServiceComponent with AssessmentMembershipServiceComponent {
        val moduleAndDepartmentService: ModuleAndDepartmentService = thisModuleAndDepartmentService
        val assessmentService: AssessmentService = thisAssignmentService
        val assessmentMembershipService: AssessmentMembershipService = thisAssignmentMembershipService
      }
      val binder = new CustomDataBinder(cmd, "cmd") with NoAutoGrownNestedPaths
      val pvs = new MutablePropertyValues()

      pvs.add("optionsMap[A].allowExtensions", true)
      binder.bind(pvs)

      cmd.optionsMap.get("A").allowExtensions.booleanValue should be(true)
    }
  }

  private def item(assignment: AssessmentComponent, include: Boolean, optionsId: String, openEnded: Boolean = false) = {
    val item = new SitsAssignmentItem(include, "A", assignment)
    item.optionsId = optionsId
    item.openDate = new LocalDate(2016, 9, 1)
    item.closeDate = new DateTime(2016, 11, 1, 12, 0)
    item.openEnded = openEnded
    item
  }


}
