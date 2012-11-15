package uk.ac.warwick.tabula.commands
import uk.ac.warwick.tabula.TestBase
import org.junit.Test


class CommandTest extends TestBase {
	
	class TestCommand extends Command[Unit] {
		def describe(d:Description) {}
		def work {}
	}
	
	// set event name via overriding value
	case class Spell6Command() extends TestCommand {
		override lazy val eventName = "DefendCastle"
	}
	
	// event name taken from class 
	case class HealSelfSpell() extends TestCommand
	
	// event name taken from class ("Command" suffix removed)
	case class CastFlameSpellCommand() extends TestCommand
	
	@Test def commandName {
		Spell6Command().eventName should be("DefendCastle")
		HealSelfSpell().eventName should be("HealSelfSpell")
		CastFlameSpellCommand().eventName should be("CastFlameSpell")
	}
	
}