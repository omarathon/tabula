package uk.ac.warwick.tabula.commands.attendance.manage

trait SetsFindPointsResultOnCommandState {

  self: FindPointsResultCommandState =>

  def setFindPointsResult(result: FindPointsResult): Unit = {
    findPointsResult = result
  }

}

trait FindPointsResultCommandState {
  var findPointsResult: FindPointsResult = _
}