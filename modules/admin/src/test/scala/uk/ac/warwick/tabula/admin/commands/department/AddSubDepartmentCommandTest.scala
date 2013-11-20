package uk.ac.warwick.tabula.admin.commands.department

import uk.ac.warwick.tabula.services.ModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.TestBase
import uk.ac.warwick.tabula.Mockito
import uk.ac.warwick.tabula.Fixtures
import uk.ac.warwick.tabula.data.model.Department
import org.springframework.validation.BindException

class AddSubDepartmentCommandTest extends TestBase with Mockito {
	
	trait CommandTestSupport extends AddSubDepartmentCommandState with ModuleAndDepartmentServiceComponent {
		val moduleAndDepartmentService = mock[ModuleAndDepartmentService]
		
		moduleAndDepartmentService.getDepartmentByCode("in-pg") returns (Some(Fixtures.department("in-pg", "IT Services Postgraduate")))
		moduleAndDepartmentService.getDepartmentByCode(isNotEq("in-pg")) returns (None)
	}
	
	trait Fixture {
		val parent = Fixtures.department("in", "IT Services")
		parent.allowExtensionRequests = true
		parent.autoGroupDeregistration = false
		
		val command = new AddSubDepartmentCommandInternal(parent) with CommandTestSupport with AddSubDepartmentCommandValidation
	}
	
	@Test def init() { new Fixture {
		command.code should startWith("in-")
		command.name should startWith("IT Services ")
		command.filterRule should be(Department.AllMembersFilterRule)
	}}
	
	@Test def apply() { new Fixture {
		command.code = "in-ug"
		command.name = "IT Services Undergraduate"
		command.filterRule = Department.UndergraduateFilterRule
		
		val dept = command.applyInternal()
		dept.code should be ("in-ug")
		dept.name should be ("IT Services Undergraduate")
		dept.filterRule should be (Department.UndergraduateFilterRule)
		dept.parent should be (parent)
		dept.allowExtensionRequests should be (true)
		dept.autoGroupDeregistration should be (false)
		
		there was one(command.moduleAndDepartmentService).save(dept)
	}}
	
	@Test def validateNoErrors() { new Fixture {
		command.code = "in-ug"
		command.name = "IT Services Undergraduate"
		command.filterRule = Department.UndergraduateFilterRule
		
		val errors = new BindException(command, "command")
		command.validate(errors)
		
		errors.hasErrors should be (false)
	}}
	
	@Test def validateEmptyCode() { new Fixture {
		command.code = ""
		command.name = "IT Services Undergraduate"
		command.filterRule = Department.UndergraduateFilterRule
		
		val errors = new BindException(command, "command")
		command.validate(errors)
		
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("code")
		errors.getFieldError.getCodes should contain ("department.code.empty")
	}}
	
	@Test def validateCodeDoesntStartWithParent() { new Fixture {
		command.code = "itservices-ug"
		command.name = "IT Services Undergraduate"
		command.filterRule = Department.UndergraduateFilterRule
		
		val errors = new BindException(command, "command")
		command.validate(errors)
		
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("code")
		errors.getFieldError.getCodes should contain ("department.code.mustStartWithParent")
	}}
	
	@Test def validateCodeTooLong() { new Fixture {
		command.code = "in-an-incredibly-long-code-this-is-silly-now"
		command.name = "IT Services Undergraduate"
		command.filterRule = Department.UndergraduateFilterRule
		
		val errors = new BindException(command, "command")
		command.validate(errors)
		
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("code")
		errors.getFieldError.getCodes should contain ("department.code.tooLong")
	}}
	
	@Test def validateCodeBadFormat() { new Fixture {
		command.code = "in-UG Students"
		command.name = "IT Services Undergraduate"
		command.filterRule = Department.UndergraduateFilterRule
		
		val errors = new BindException(command, "command")
		command.validate(errors)
			
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("code")
		errors.getFieldError.getCodes should contain ("department.code.badFormat")
	}}
	
	@Test def validateExistingCode() { new Fixture {
		command.code = "in-pg"
		command.name = "IT Services Postgraduate"
		command.filterRule = Department.PostgraduateFilterRule
		
		val errors = new BindException(command, "command")
		command.validate(errors)
		
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("code")
		errors.getFieldError.getCodes should contain ("department.code.exists")
	}}
	
	@Test def validateEmptyName() { new Fixture {
		command.code = "in-ug"
		command.name = "  "
		command.filterRule = Department.UndergraduateFilterRule
		
		val errors = new BindException(command, "command")
		command.validate(errors)
		
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("department.name.empty")
	}}
	
	@Test def validateNameDoesntStartWithParent() { new Fixture {
		command.code = "in-ug"
		command.name = "ITS Undergraduates"
		command.filterRule = Department.UndergraduateFilterRule
		
		val errors = new BindException(command, "command")
		command.validate(errors)
		
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("department.name.mustStartWithParent")
	}}
	
	@Test def validateNameTooLong() { new Fixture {
		command.code = "in-ug"
		command.name = "IT Services Undergraduate Students Who Have Come To Study Computers And Programming And Things, What A Wonderful Sight This Is"
		command.filterRule = Department.UndergraduateFilterRule
		
		val errors = new BindException(command, "command")
		command.validate(errors)
		
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("name")
		errors.getFieldError.getCodes should contain ("department.name.tooLong")
	}}
	
	@Test def validateEmptyFilterRule() { new Fixture {
		command.code = "in-ug"
		command.name = "IT Services Undergraduates"
		command.filterRule = null
		
		val errors = new BindException(command, "command")
		command.validate(errors)
		
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("filterRule")
		errors.getFieldError.getCodes should contain ("department.filterRule.empty")
	}}
	
	@Test def validateLessSpecificFilterRule() { new Fixture {
		command.code = "in-ug"
		command.name = "IT Services Undergraduates"
			
		parent.filterRule = Department.UndergraduateFilterRule
		command.filterRule = Department.AllMembersFilterRule
		
		val errors = new BindException(command, "command")
		command.validate(errors)
		
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("filterRule")
		errors.getFieldError.getCodes should contain ("department.filterRule.contradictory")
	}}
	
	@Test def validateContradictoryFilterRule() { new Fixture {
		command.code = "in-ug"
		command.name = "IT Services Undergraduates"
			
		parent.filterRule = Department.UndergraduateFilterRule
		command.filterRule = Department.PostgraduateFilterRule
		
		val errors = new BindException(command, "command")
		command.validate(errors)
		
		errors.hasErrors should be (true)
		errors.getErrorCount should be (1)
		errors.getFieldError.getField should be ("filterRule")
		errors.getFieldError.getCodes should contain ("department.filterRule.contradictory")
	}}

}