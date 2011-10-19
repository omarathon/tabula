package uk.ac.warwick.courses.services
import org.springframework.stereotype.Service
import scala.xml._
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.ResponseHandler
import uk.ac.warwick.courses.helpers.Logging
import org.w3c.dom.NodeList
import org.springframework.util.StringUtils._
import uk.ac.warwick.util.web.Uri

class DepartmentInfo(val name:String, val code:String, val faculty:String)
class ModuleInfo(val name:String, val code:String, val group:String)

trait DepartmentFetcher {
    def getModules(deptCode:String):Seq[ModuleInfo]
	def getDepartments:Seq[DepartmentInfo]
}


@Service
class DepartmentFetcherImpl extends DepartmentFetcher with Logging {  
  
	// TODO try dispatch.databinder.net - wraps httpclient in a scala way
    val client = new DefaultHttpClient
  
	def getDepartments:Seq[DepartmentInfo] = {
		val get = new HttpGet("http://webgroups.warwick.ac.uk/query/department/all")
		val doc = fetchXml(get)
		logger.debug("Loaded list of departments")
		val depts = parseDepts(doc)
		logger.debug("Loaded "+depts.length+" departments")
		depts
	}
    
    def getModules(deptCode:String):Seq[ModuleInfo] = {
    	val get = new HttpGet("http://webgroups.warwick.ac.uk/query/search/deptcode/" + deptCode)
    	val doc = fetchXml(get)
    	val modules = parseModules(doc)
    	logger.debug("Loaded "+modules.length+" modules for dept " + deptCode)
    	modules
    }
    
    def fetchXml(get:HttpGet) = client.execute(get, new ResponseHandler[xml.Elem]{
		  def handleResponse(response:HttpResponse) = {
		    XML.load(response.getEntity.getContent)
		  }
		})
		
	def parseModules(doc:Elem) = for (group <- (doc \\ "group") if isModule(group))
		yield new ModuleInfo(
		    (group\\"title")(0).text,
			groupNameToModuleCode(group.attributes("name").toString),
			group.attributes("name").toString
		)
    
    def groupNameToModuleCode(groupName:String) = groupName.substring(groupName.indexOf("-")+1)
    
    def isModule(group:Node) = (group\\"type") match {
	    case nodes:NodeSeq => nodes.text == "Module"
	    case _ => false
	}
    
    def parseDepts(doc:Elem) = for (dept <- (doc \\ "department") if isValidDept(dept)) 
    	yield new DepartmentInfo(
    	    (dept\\"name")(0).text,
    	    (dept\\"code")(0).text,
    	    (dept\\"faculty")(0).text
    	)
    	
    def isValidDept(dept:Node) = (dept\\"name") match {
      case nodes:NodeSeq => hasText(nodes(0).text)
      case _ => false
    }
}