package uk.ac.warwick.tabula.api.web.controllers.profiles

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.data.ModeOfAttendanceDao
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/modesOfAttendance"))
class ModeOfAttendanceListController extends ApiController {

  @Autowired var modeOfAttendanceDao: ModeOfAttendanceDao = _

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def index(): Mav = {
    Mav(new JSONView(
      Map(
        "success" -> true,
        "status" -> "ok",
        "modes" -> modeOfAttendanceDao.getAll.map(m => Map(
          "code" -> m.code,
          "shortName" -> m.shortName,
          "fullName" -> m.fullName,
        ))
      )
    ))
  }

}
