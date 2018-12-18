package uk.ac.warwick.tabula.web.views

import freemarker.template.TemplateMethodModelEx
import uk.ac.warwick.util.web.UriBuilder

import scala.util.Try

/**
	* Freemarker directive to encode a given URI (similar to encodeURI() in js)
	*
	* this wraps `UriBuilder.parse` function which is from warwick utility.
	*
	* added since TAB-6776 not exactly for parsing URI, but for parsing a complex query string happens in
	* /tabula/web/src/main/webapp/WEB-INF/freemarker/exams/grids/generate/jobProgress.ftl
	* /tabula/web/src/main/webapp/WEB-INF/freemarker/exams/grids/generate/preview.ftl
	* /tabula/web/src/main/webapp/WEB-INF/freemarker/exams/grids/generate/gridOption.ftl
	* /tabula/web/src/main/webapp/WEB-INF/freemarker/exams/grids/generate/coreRequiredModules.ftl
	* as Freemarker's `?url` would escape valid special characters (for query string) such as `&`, but not escaping characters like `[` or `]`
	*
	*/
class UriParser extends TemplateMethodModelEx {
	/**
		*
		* @param args is expected to contain 1 unencoded String.
		* @return encoded version of the passed in String.
		*/
	override def exec(args: java.util.List[_]): Object = Try {
		UriBuilder.parse(args.get(0).toString).toString
	}.toOption.getOrElse("")
}
