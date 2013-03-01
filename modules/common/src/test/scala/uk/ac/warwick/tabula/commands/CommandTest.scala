package uk.ac.warwick.tabula.commands
import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import uk.ac.warwick.tabula.services.MaintenanceModeEnabledException
import uk.ac.warwick.tabula.services.MaintenanceModeServiceImpl
import uk.ac.warwick.tabula.events.Log4JEventListener
import uk.ac.warwick.tabula.Fixtures


class CommandTest extends TestBase {
	
	class TestCommand extends Command[Boolean] {
		def describe(d:Description) {}
		def applyInternal = true
	}
	
	// set event name via overriding value
	case class Spell6Command() extends TestCommand {
		override lazy val eventName = "DefendCastle"
	}
	
	// event name taken from class 
	case class HealSelfSpell() extends TestCommand
	
	// event name taken from class ("Command" suffix removed)
	case class CastFlameSpellCommand() extends TestCommand with ReadOnly
	
	@Test def commandName {
		Spell6Command().eventName should be("DefendCastle")
		HealSelfSpell().eventName should be("HealSelfSpell")
		CastFlameSpellCommand().eventName should be("CastFlameSpell")
	}
	
	@Test def maintenanceModeDisabled {
		val mmService = new MaintenanceModeServiceImpl()
		mmService.disable
		
		val cmd = Spell6Command() 
		cmd.maintenanceMode = mmService
		cmd.listener = new Log4JEventListener
		
		cmd.apply() should be (true)
	}
	
	@Test(expected=classOf[MaintenanceModeEnabledException]) def maintenanceModeEnabled {
		val mmService = new MaintenanceModeServiceImpl()
		mmService.enable
		
		val cmd = Spell6Command()
		cmd.maintenanceMode = mmService
		
		cmd.apply()
		fail("expected exception")
	}
	
	@Test def maintenanceModeEnabledButReadOnly {
		val mmService = new MaintenanceModeServiceImpl()
		mmService.enable
		
		val cmd = CastFlameSpellCommand()
		cmd.maintenanceMode = mmService
		cmd.listener = new Log4JEventListener
		
		cmd.apply() should be (true)
	}
	
	@Test def description {
		val description = new DescriptionImpl
		description.properties(("yes" -> "no"), ("steve" -> Seq("tom", "jerry")))
		description.properties(Map("queen" -> "sheeba", "nine" -> 9))
		
		val department = Fixtures.department("in", "IT Services")
		val module = Fixtures.module("in101", "Introduction to Web Develoment")
		module.id = "moduleId"
		module.department = department
		
		val assignment = Fixtures.assignment("my assignment")
		assignment.id = "assignmentId"
		assignment.module = module
		
		val submission1 = Fixtures.submission("0000001")
		submission1.id = "submission1Id"
		submission1.assignment = assignment
		
		val submission2 = Fixtures.submission("0000002")
		submission2.id = "submission2Id"
		submission2.assignment = assignment
		
		val feedback = Fixtures.feedback("0000001")
		feedback.id = "feedbackId"
		feedback.assignment = assignment
				
		val workflow = Fixtures.markingWorkflow("my workflow")
		workflow.id = "workflowId"
		workflow.department = department
		
		val staff = Fixtures.staff("1010101")
		
		description.feedback(feedback)
							 .submission(submission1)
							 .studentIds(Seq("0000001", "0000002"))
							 .submissions(Seq(submission1, submission2))
							 .markingWorkflow(workflow)
							 .member(staff)
		
	  description.allProperties should be (Map(
 		  "yes" -> "no", 
 		  "steve" -> Seq("tom", "jerry"), 
 		  "queen" -> "sheeba", 
 		  "nine" -> 9,
 		  "feedback" -> "feedbackId", 
 		  "submission" -> "submission1Id", 
 		  "assignment" -> "assignmentId", 
 		  "module" -> "moduleId", 
 		  "department" -> "in", 
 		  "submissions" -> Seq("submission1Id", "submission2Id"), 
 		  "students" -> List("0000001", "0000002"), 
 		  "markingWorkflow" -> "workflowId", 
 		  "member" -> "1010101" 
	  ))
	}
	
}
