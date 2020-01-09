package uk.ac.warwick.tabula.commands.home

import uk.ac.warwick.tabula.data.model.UserSettings
import uk.ac.warwick.tabula.services.UserSettingsService
import uk.ac.warwick.tabula.{Mockito, NoCurrentUser, TestBase}

class DismissHiddenIntroCommandTest extends TestBase with Mockito {

  val user = NoCurrentUser()
  val settings = new UserSettings("userId")

  val service: UserSettingsService = mock[UserSettingsService]

  @Test def setupWithNonExisting: Unit = {
    val cmd = new DismissHiddenIntroCommand(user, settings, "hash")
    cmd.dismiss should be(false)
  }

  @Test def setupWithExisting: Unit = {
    settings.hiddenIntros = Seq("hash")

    val cmd = new DismissHiddenIntroCommand(user, settings, "hash")
    cmd.dismiss should be(true)
  }

  @Test def dismissNonExisting: Unit = {
    val cmd = new DismissHiddenIntroCommand(user, settings, "hash")
    cmd.service = service

    cmd.dismiss = true

    cmd.applyInternal()

    settings.hiddenIntros should be(Seq("hash"))

    verify(service, times(1)).save(user, settings)
  }

  @Test def dismissExisting: Unit = {
    settings.hiddenIntros = Seq("hash", "otherhash")

    val cmd = new DismissHiddenIntroCommand(user, settings, "hash")
    cmd.service = service

    cmd.dismiss = true

    cmd.applyInternal()

    settings.hiddenIntros should be(Seq("hash", "otherhash"))

    verify(service, times(1)).save(user, settings)
  }

  @Test def undismissExisting: Unit = {
    settings.hiddenIntros = Seq("hash", "otherhash")

    val cmd = new DismissHiddenIntroCommand(user, settings, "hash")
    cmd.service = service

    cmd.dismiss = false

    cmd.applyInternal()

    settings.hiddenIntros should be(Seq("otherhash"))

    verify(service, times(1)).save(user, settings)
  }

  @Test def undismissNonExisting: Unit = {
    settings.hiddenIntros = Seq("otherhash")

    val cmd = new DismissHiddenIntroCommand(user, settings, "hash")
    cmd.service = service

    cmd.dismiss = false

    cmd.applyInternal()

    settings.hiddenIntros should be(Seq("otherhash"))

    verify(service, times(1)).save(user, settings)
  }

}