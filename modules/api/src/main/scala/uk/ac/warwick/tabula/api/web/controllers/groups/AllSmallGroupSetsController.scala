package uk.ac.warwick.tabula.api.web.controllers.groups

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.{AssessmentMembershipInfoToJsonConverter, SmallGroupEventToJsonConverter, SmallGroupSetToJsonConverter, SmallGroupToJsonConverter}
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel
import uk.ac.warwick.tabula.groups.web.views.GroupsViewModel.{ViewGroup, ViewSet}
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

import scala.collection.JavaConverters._

@Controller
@RequestMapping(Array("/v1/groups"))
class AllSmallGroupSetsController extends ApiController
	with ListAllSmallGroupSetsApi
	with SmallGroupSetToJsonConverter
	with SmallGroupToJsonConverter
	with SmallGroupEventToJsonConverter
	with AssessmentMembershipInfoToJsonConverter
	with AutowiringSmallGroupServiceComponent

trait ListAllSmallGroupSetsApi {
	self: ApiController with SmallGroupSetToJsonConverter with SmallGroupServiceComponent =>

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def list(@RequestParam(required = false) academicYear: AcademicYear): Mav = {
		val year = Option(academicYear).getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))

		val sets = smallGroupService.getAllSmallGroupSets(year)
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"academicYear" -> year.toString,
			"groups" -> sets.map { set => jsonSmallGroupSetObject(ViewSet(set, ViewGroup.fromGroups(set.groups.asScala.sorted), GroupsViewModel.Tutor)) }
		)))
	}

}