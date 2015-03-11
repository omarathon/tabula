package uk.ac.warwick.tabula.api.web.controllers.index

import javax.servlet.http.HttpServletResponse

import com.fasterxml.jackson.annotation.JsonAutoDetect
import org.apache.lucene.queryparser.classic.ParseException
import org.apache.lucene.search.{SortField, Sort, Query}
import org.springframework.http.{HttpStatus, MediaType}
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{RequestBody, ModelAttribute, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.api.commands.JsonApiRequest
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{AuditEvent, Member}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.{JSONView, JSONErrorView}

import scala.beans.BeanProperty
import scala.collection.JavaConverters._

import IndexController._

trait IndexServiceComponent[A] {
	def indexService: AbstractIndexService[A]
}

object IndexController {
	type SearchIndexCommand = Appliable[RichSearchResults] with SearchLuceneIndexState with SelfValidating
}

abstract class IndexController[A] extends ApiController with IndexServiceComponent[A]
	with SearchLuceneIndexApi {
	def indexService: AbstractIndexService[A]
}

// GET - Search the index
trait SearchLuceneIndexApi {
	self: ApiController with IndexServiceComponent[_] =>

	@ModelAttribute("searchCommand")
	def command(): SearchIndexCommand =
		SearchLuceneIndexCommand(indexService)

	@RequestMapping(method = Array(GET, POST), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
	def search(@RequestBody request: SearchIndexRequest[SearchLuceneIndexState], @ModelAttribute("searchCommand") command: SearchIndexCommand, errors: Errors)(implicit response: HttpServletResponse) = {
		request.copyTo(command, errors)
		command.validate(errors)

		if (errors.hasErrors) {
			response.setStatus(HttpStatus.BAD_REQUEST.value())

			Mav(new JSONErrorView(errors, Map("success" -> false, "status" -> HttpStatus.BAD_REQUEST.value())))
		} else {
			val result = command.apply()
			Mav(new JSONView(Map("success" -> true) ++ toJson(request, result)))
		}
	}

	def toJson(request: SearchIndexRequest[SearchLuceneIndexState], result: RichSearchResults) = Map(
		"queryString" -> request.queryString,
		"sort" -> request.sort,
		"max" -> request.max,
		"offset" -> request.offset,
		"results" -> result.transform { doc =>
			Some(doc.asScala.map { field =>
				field.name() -> doc.getValues(field.name()).headOption.orNull
			}.toMap)
		}
	)

}

@Controller
@RequestMapping(Array("/v1/index/audit"))
class AuditEventIndexController extends IndexController[AuditEvent] {
	val indexService = Wire[AuditEventIndexService]
}

@Controller
@RequestMapping(Array("/v1/index/notification"))
class NotificationIndexController extends IndexController[RecipientNotification] {
	val indexService = Wire[NotificationIndexServiceImpl]
}

@Controller
@RequestMapping(Array("/v1/index/profile"))
class ProfileIndexController extends IndexController[Member] {
	val indexService = Wire[ProfileIndexService]
}

object SearchLuceneIndexCommand {
	def apply(indexService: AbstractIndexService[_]): SearchIndexCommand =
		new SearchLuceneIndexCommandInternal(indexService)
			with ComposableCommand[RichSearchResults]
			with SearchLuceneIndexValidation
			with SearchLuceneIndexPermissions
			with Unaudited with ReadOnly
}

class SearchLuceneIndexCommandInternal(val indexService: AbstractIndexService[_]) extends CommandInternal[RichSearchResults] with SearchLuceneIndexState {
	override protected def applyInternal(): RichSearchResults = {
		println(query)

		if (max.nonEmpty) {
			indexService.search(query, max.get, sort.orNull, offset.getOrElse(0))
		} else {
			indexService.search(query, sort.orNull)
		}
	}
}

trait SearchLuceneIndexState {
	def indexService: AbstractIndexService[_]

	var query: Query = _
	var sort: Option[Sort] = None
	var max: Option[Int] = None

	// For normal searches, use offset
	var offset: Option[Int] = None
}

trait SearchLuceneIndexValidation extends SelfValidating {
	self: SearchLuceneIndexState =>

	override def validate(errors: Errors) {
		if (query == null) {
			errors.rejectValue("query", "NotEmpty")
		}
	}

}

trait SearchLuceneIndexPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.ViewAuditLog)
	}

}

@JsonAutoDetect
class SearchIndexRequest[A <: SearchLuceneIndexState] extends JsonApiRequest[A] {
	@BeanProperty var queryString: String = _
	@BeanProperty var sort: JList[JMap[String, _]] = JArrayList()
	@BeanProperty var max: Int = -1
	@BeanProperty var offset: Int = 0

	override def copyTo(state: A, errors: Errors) {
		queryString.maybeText.foreach { queryString =>
			try {
				state.query = state.indexService.parser.parse(queryString)
			} catch {
				case e: ParseException => errors.rejectValue("query", "typeMismatch")
			}
		}

		if (max >= 0) {
			state.max = Some(max)
		}

		if (offset > 0) {
			state.offset = Some(offset)
		}

		if (sort.asScala.nonEmpty) {
			state.sort = Some(new Sort(sort.asScala.map { _.asScala }.flatMap { sortField =>
				sortField.get("field") match {
					case Some(field: String) =>
						sortField.get("type") match {
							case Some(fieldType: String) if SortField.Type.values().map { _.name() }.contains(fieldType) =>
								val reverse = sortField.get("reverse").collect { case r: Boolean => r }.getOrElse(false)

								Some(new SortField(field, SortField.Type.valueOf(fieldType), reverse))
							case _ =>
								errors.rejectValue("sort", "typeMismatch")
								None
						}
					case _ =>
						errors.rejectValue("sort", "typeMismatch")
						None
				}
			}.toArray: _*))
		}
	}
}