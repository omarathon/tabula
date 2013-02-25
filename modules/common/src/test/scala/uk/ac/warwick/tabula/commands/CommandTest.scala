package uk.ac.warwick.tabula.commands
import uk.ac.warwick.tabula.TestBase
import org.junit.Test
import uk.ac.warwick.tabula.services.MaintenanceModeEnabledException
import uk.ac.warwick.tabula.services.MaintenanceModeServiceImpl
import uk.ac.warwick.tabula.events.Log4JEventListener


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
	
}