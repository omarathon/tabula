package uk.ac.warwick.tabula.api.web.controllers.profiles

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.services.AutowiringProfileServiceComponent

@Controller
@RequestMapping(Array("/v1/usercodeSearch"))
class UsercodeSearchController extends ApiController with AutowiringProfileServiceComponent {

	final override def onPreRequest {
		session.enableFilter(Member.ActiveOnlyFilter)
		session.enableFilter(Member.FreshOnlyFilter)
	}

}
