package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.{Fixtures, TestBase}
import uk.ac.warwick.tabula.events.{EventHandling, SLF4JEventListener}
import uk.ac.warwick.tabula.services._


class CommandTest extends TestBase {

  @Test def commandName(): Unit = {
    Spell6Command().eventName should be("DefendCastle")
    HealSelfSpell().eventName should be("HealSelfSpell")
    CastFlameSpellCommand().eventName should be("CastFlameSpell")
  }

  @Test def maintenanceModeDisabled(): Unit = {
    val mmService = new MaintenanceModeServiceImpl()
    mmService.disable

    val cmd = Spell6Command()
    cmd.listener = new SLF4JEventListener
    cmd.maintenanceModeService = mmService

    cmd.apply() should be(true)
  }

  @Test(expected = classOf[MaintenanceModeEnabledException]) def maintenanceModeEnabled(): Unit = {
    EventHandling.enabled = true

    val mmService = new MaintenanceModeServiceImpl()
    mmService.enable

    val cmd = Spell6Command()
    cmd.maintenanceModeService = mmService

    cmd.apply()
    fail("expected exception")
  }

  @Test def maintenanceModeEnabledButReadOnly(): Unit = {
    val mmService = new MaintenanceModeServiceImpl()
    mmService.enable

    val cmd = CastFlameSpellCommand()
    cmd.listener = new SLF4JEventListener
    cmd.maintenanceModeService = mmService

    cmd.apply() should be(true)
  }

  @Test def description(): Unit = {
    val description = new DescriptionImpl
    description.properties("yes" -> "no", "steve" -> Seq("tom", "jerry"))
    description.properties(Map("queen" -> "sheeba", "nine" -> 9))

    val department = Fixtures.department("in", "IT Services")
    val module = Fixtures.module("in101", "Introduction to Web Develoment")
    module.id = "moduleId"
    module.adminDepartment = department

    val assignment = Fixtures.assignment("my assignment")
    assignment.id = "assignmentId"
    assignment.module = module

    val submission1 = Fixtures.submission("0000001")
    submission1.id = "submission1Id"
    submission1.assignment = assignment

    val submission2 = Fixtures.submission("0000002")
    submission2.id = "submission2Id"
    submission2.assignment = assignment

    val feedback = Fixtures.assignmentFeedback("0000001")
    feedback.id = "feedbackId"
    feedback.assignment = assignment

    val staff = Fixtures.staff("1010101")

    description.feedback(feedback)
      .submission(submission1)
      .studentIds(Seq("0000001", "0000002"))
      .submissions(Seq(submission1, submission2))
      .member(staff)

    description.allProperties should be(Map(
      "yes" -> "no",
      "steve" -> Seq("tom", "jerry"),
      "queen" -> "sheeba",
      "nine" -> 9,
      "feedback" -> "feedbackId",
      "submission" -> "submission1Id",
      "assignment" -> "assignmentId",
      "module" -> "moduleId",
      "moduleCode" -> "in101",
      "department" -> "in",
      "submissions" -> Seq("submission1Id", "submission2Id"),
      "students" -> List("0000001", "0000002"),
      "studentUsercodes" -> List("cuspxp", "cuspxp"),
      "member" -> "1010101"
    ))
  }

}

// these commands need to not be nested inside "CommandTest", otherwise the name-munging code gets
// confused by the fact that "Command" appears in the outer class name.
class TestCommand extends Command[Boolean] {
  def describe(d: Description): Unit = {}

  def applyInternal() = true
}

// set event name via overriding value
case class Spell6Command() extends TestCommand {
  override lazy val eventName = "DefendCastle"
}

// event name taken from class
case class HealSelfSpell() extends TestCommand

// event name taken from class ("Command" suffix removed)
case class CastFlameSpellCommand() extends TestCommand with ReadOnly
