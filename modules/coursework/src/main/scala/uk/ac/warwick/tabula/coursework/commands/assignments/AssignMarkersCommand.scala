package uk.ac.warwick.tabula.coursework.commands.assignments

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.commands.{Description, Command}
import uk.ac.warwick.tabula.data.model.{UserGroup, Module, Assignment}
import uk.ac.warwick.tabula.data.Daoisms
import uk.ac.warwick.tabula.data.Transactions._
import reflect.BeanProperty
import uk.ac.warwick.tabula.helpers.ArrayList
import java.util.HashMap
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.permissions.Permissions


class AssignMarkersCommand(module: Module, assignment:Assignment) extends Command[Assignment] with Daoisms{

	PermissionCheck(Permissions.Assignment.Update, assignment)

	@transient
	var assignmentService = Wire[AssignmentService]("assignmentService")

	@BeanProperty var firstMarkerStudents: JList[String] = _
	@BeanProperty var secondMarkerStudents: JList[String] = _
	@BeanProperty var firstMarkers: JList[String] = _
	@BeanProperty var secondMarkers: JList[String] = _
	@BeanProperty var markerMapping: JMap[String, JList[String]] = _

	def onBind() {
		firstMarkers = assignment.markingWorkflow.firstMarkers.members
		secondMarkers = assignment.markingWorkflow.secondMarkers.members

		def retrieveMarkers(markerDef:Seq[String]):JMap[String, JList[String]] = {
			val resultMap = new HashMap[String, JList[String]]()
			markerDef.foreach{marker =>
				val students:JList[String] = assignment.markerMap.toMap.get(marker) match {
					case Some(userGroup:UserGroup) => userGroup.includeUsers
					case None => ArrayList()
				}
				resultMap.put(marker, students)
			}
			resultMap
		}

		val members = assignmentService.determineMembershipUsers(assignment).map(_.getUserId)

		val firstMarkerMap = retrieveMarkers(firstMarkers)
		val secondMarkerMap = retrieveMarkers(secondMarkers)
		markerMapping = firstMarkerMap ++ secondMarkerMap.toList

		firstMarkerStudents = members.toList filterNot firstMarkerMap.values.flatten.toList.contains
		secondMarkerStudents = members.toList filterNot secondMarkerMap.values.flatten.toList.contains
	}

	def applyInternal() = {
		transactional(){
			assignment.markerMap = new HashMap[String, UserGroup]()
			if(Option(markerMapping).isDefined){
				markerMapping.foreach{case(marker, studentList) =>
					val group = new UserGroup()
					group.includeUsers = studentList
					session.saveOrUpdate(group)
					assignment.markerMap.put(marker, group)
				}
			}
			session.saveOrUpdate(assignment)
			assignment
		}
	}

	def describe(d: Description) {
		d.assignment(assignment)
	}

}
