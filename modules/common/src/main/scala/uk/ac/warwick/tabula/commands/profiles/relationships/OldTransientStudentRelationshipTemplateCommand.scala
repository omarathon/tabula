package uk.ac.warwick.tabula.commands.profiles.relationships

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, Member, StudentRelationshipType}
import uk.ac.warwick.tabula.helpers.{LazyLists, LazyMaps}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.web.views.ExcelView

import scala.collection.JavaConverters._

/**
 * Generates a template spreadsheet for agent upload based on the POSTed values
 * rather than the persisted existing relationships
 */
object OldTransientStudentRelationshipTemplateCommand {
	def apply(department: Department, relationshipType: StudentRelationshipType) =
		new OldTransientStudentRelationshipTemplateCommandInternal(department, relationshipType)
			with AutowiringProfileServiceComponent
			with ComposableCommand[ExcelView]
			with OldTransientStudentRelationshipTemplatePermissions
			with OldTransientStudentRelationshipTemplateCommandState
			with ReadOnly with Unaudited
}


class OldTransientStudentRelationshipTemplateCommandInternal(val department: Department, val relationshipType: StudentRelationshipType)
	extends CommandInternal[ExcelView] with GeneratesStudentRelationshipWorkbook {

	self: OldTransientStudentRelationshipTemplateCommandState with ProfileServiceComponent =>

	override def applyInternal(): ExcelView = {
		// Transform into a list of (Member, Seq[Member]) pairs
		val existingAllocations =
			mapping.asScala.toSeq.flatMap{case(agent, students) => students.asScala.map((agent, _))}
				.groupBy(_._2)
				.toSeq
				.map{case(student, agentStudentPairs) => student -> agentStudentPairs.map(_._1)}

		val allAllocations =
			(existingAllocations ++ unallocated.asScala.map {(_, Nil)})
				.sortBy { case (student, _) => student.lastName + ", " + student.firstName}

		additionalAgents.asScala
			.flatMap { profileService.getAllMembersWithUserId(_) }
			.foreach { member =>
			if (!mapping.containsKey(member)) mapping.put(member, JArrayList())
		}

		val workbook = generateWorkbook(mapping.asScala.keys.toSeq, allAllocations, department, relationshipType)

		new ExcelView("Allocation for " + allocateSheetName(department, relationshipType) + ".xlsx", workbook)
	}

}

trait OldTransientStudentRelationshipTemplatePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: OldTransientStudentRelationshipTemplateCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(mandatory(relationshipType)), department)
	}

}

trait OldTransientStudentRelationshipTemplateCommandState {
	def department: Department
	def relationshipType: StudentRelationshipType

	// Bind variables

	var unallocated: JList[Member] = LazyLists.createWithFactory { () => null } // grower, not a shower
	var mapping: JMap[Member, JList[Member]] =
		LazyMaps.create { key: Member => JArrayList(): JList[Member] }.asJava
	var additionalAgents: JList[String] = JArrayList()
}
