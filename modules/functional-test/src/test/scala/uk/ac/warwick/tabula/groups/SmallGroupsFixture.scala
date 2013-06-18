package uk.ac.warwick.tabula.groups

import uk.ac.warwick.tabula.{LoginDetails, BrowserTest}


class SmallGroupsFixture extends BrowserTest{

  before{
    go to (Path("/scheduling/fixtures/setup"))
  }

  def as[T](user: LoginDetails)(fn: =>T) = {
    signIn as(user) to (Path("/groups"))
    fn
  }
}
