package uk.ac.warwick.tabula.api.web.controllers.profiles

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.data.LevelDao
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/studyLevels"))
class StudyLevelListController extends ApiController {

  @Autowired var levelDao: LevelDao = _

  @RequestMapping(method = Array(GET), produces = Array("application/json"))
  def index(): Mav = {
    Mav(new JSONView(
      Map(
        "success" -> true,
        "status" -> "ok",
        "levels" -> levelDao.getAllLevels.map(l => Map(
          "code" -> l.code,
          "shortName" -> l.shortName,
          "name" -> l.name,
        ))
      )
    ))
  }

}
