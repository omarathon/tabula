package uk.ac.warwick.tabula.profiles.commands.relationships

import uk.ac.warwick.tabula.commands.{StudentAssociationData, StudentAssociationEntityData}
import uk.ac.warwick.tabula.services.{ProfileService, ProfileServiceComponent, RelationshipService, RelationshipServiceComponent}
import uk.ac.warwick.tabula.{Mockito, TestBase}

import scala.collection.JavaConverters._

class FetchDepartmentRelationshipInformationCommandTest extends TestBase with Mockito {

	trait Fixture {
		val student1 = StudentAssociationData("1", "", "", null, null, 0)
		val student2 = StudentAssociationData("2", "", "", null, null, 0)
		val student3 = StudentAssociationData("3", "", "", null, null, 0)
		val student4 = StudentAssociationData("4", "", "", null, null, 0)
		val student5 = StudentAssociationData("5", "", "", null, null, 0)
		val student6 = StudentAssociationData("6", "", "", null, null, 0)
		val student7 = StudentAssociationData("7", "", "", null, null, 0)
		val student8 = StudentAssociationData("8", "Fred", "Jones", null, null, 0)
		val student9 = StudentAssociationData("9", "Samuel Jones", "Partington Smyth", null, null, 0)
		val student10 = StudentAssociationData("10", "", "", null, null, 0)
		val student11 = StudentAssociationData("11", "", "", null, null, 0)

		val command = new FetchDepartmentRelationshipInformationCommandInternal(null, null)
			with FetchDepartmentRelationshipInformationCommandRequest with FetchDepartmentRelationshipInformationCommandState
			with RelationshipServiceComponent with ProfileServiceComponent {

			val relationshipService = smartMock[RelationshipService]
			relationshipService.getStudentAssociationDataWithoutRelationship(null, null, Seq()) returns Seq(student8, student9, student10, student11)
			relationshipService.getStudentAssociationEntityData(null, null, Seq()) returns Seq(
				StudentAssociationEntityData("1", null, null, null, Seq()),
				StudentAssociationEntityData("2", null, null, null, Seq(student1, student2)),
				StudentAssociationEntityData("3", null, null, null, Seq(student3, student4)),
				StudentAssociationEntityData("4", null, null, null, Seq(student5, student6, student7))
			)
			val profileService = smartMock[ProfileService]
		}
	}

	@Test
	def query(): Unit = {
		new Fixture {
			command.query = "JO"
			val result = command.applyInternal()
			result.unallocated.size should be (2)
			result.unallocated.contains(student8) should be {true}
			result.unallocated.contains(student9) should be {true}
		}
		new Fixture {
			command.query = "pa sm"
			val result = command.applyInternal()
			result.unallocated.size should be (1)
			result.unallocated.contains(student9) should be {true}
		}
		new Fixture {
			command.query = "Sam Jones"
			val result = command.applyInternal()
			result.unallocated.size should be (1)
			result.unallocated.contains(student9) should be {true}
		}
	}

	@Test
	def distributeToAll(): Unit = {
		new Fixture {
			val allocated = Seq("8", "9", "10", "11")
			command.allocate.addAll(allocated.asJava)
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.DistributeToAll
			val result = command.applyInternal()
			result.unallocated.isEmpty should be {true}
			result.allocated.size should be (4)
			result.allocated.flatMap(_.students).distinct.size should be (11)
			val sizes = result.allocated.map(_.students.size)
			sizes.count(_ == 3) should be (3)
			sizes.count(_ == 2) should be (1)
			allocated.foreach(id => {
				command.additions.get(result.allocated.find(_.students.exists(_.universityId == id)).get.entityId).contains(id) should be {true}
			})
		}

		new Fixture {
			val allocated = Seq("8", "9", "10")
			command.allocate.addAll(allocated.asJava)
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.DistributeToAll
			val result = command.applyInternal()
			result.unallocated.size should be (1)
			result.allocated.size should be (4)
			result.allocated.flatMap(_.students).distinct.size should be (10)
			val sizes = result.allocated.map(_.students.size)
			sizes.count(_ == 3) should be (2)
			sizes.count(_ == 2) should be (2)
			allocated.foreach(id => {
				command.additions.get(result.allocated.find(_.students.exists(_.universityId == id)).get.entityId).contains(id) should be {true}
			})
		}
	}

	@Test
	def distributeToSelected(): Unit = {
		new Fixture {
			val allocated = Seq("8", "9", "10", "11")
			command.allocate.addAll(allocated.asJava)
			command.entities.addAll(Seq("1", "2", "3").asJava)
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.DistributeToSelected
			val result = command.applyInternal()
			result.unallocated.size should be (0)
			result.allocated.size should be (4)
			result.allocated.flatMap(_.students).distinct.size should be (11)
			val sizes = result.allocated.map(_.students.size)
			sizes.count(_ == 3) should be (3)
			sizes.count(_ == 2) should be (1)
			allocated.foreach(id => {
				command.additions.get(result.allocated.find(_.students.exists(_.universityId == id)).get.entityId).contains(id) should be {true}
			})
		}

		new Fixture {
			val allocated = Seq("8", "9", "10")
			command.allocate.addAll(allocated.asJava)
			command.entities.addAll(Seq("1", "2", "3", "4").asJava)
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.DistributeToSelected
			val result = command.applyInternal()
			result.unallocated.size should be (1)
			result.allocated.size should be (4)
			result.allocated.flatMap(_.students).distinct.size should be (10)
			val sizes = result.allocated.map(_.students.size)
			sizes.count(_ == 3) should be (2)
			sizes.count(_ == 2) should be (2)
			allocated.foreach(id => {
				command.additions.get(result.allocated.find(_.students.exists(_.universityId == id)).get.entityId).contains(id) should be {true}
			})
		}

		new Fixture {
			val allocated = Seq("8", "9", "10")
			command.allocate.addAll(allocated.asJava)
			command.entities.addAll(Seq("3", "4").asJava)
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.DistributeToSelected
			val result = command.applyInternal()
			result.unallocated.size should be (1)
			result.allocated.size should be (4)
			result.allocated.flatMap(_.students).distinct.size should be (10)
			result.allocated.find(_.entityId == "1").get.students.size should be (0)
			result.allocated.find(_.entityId == "2").get.students.size should be (2)
			result.allocated.find(_.entityId == "3").get.students.size should be (4)
			result.allocated.find(_.entityId == "4").get.students.size should be (4)
			allocated.foreach(id => {
				command.additions.get(result.allocated.find(_.students.exists(_.universityId == id)).get.entityId).contains(id) should be {true}
			})
		}
	}

	@Test
	def removeSingle(): Unit = {
		new Fixture {
			command.studentToRemove = "1"
			command.entityToRemoveFrom = "2"
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.RemoveSingle
			val result = command.applyInternal()
			result.unallocated.size should be (5)
			result.allocated.size should be (4)
			result.allocated.flatMap(_.students).distinct.size should be (6)
			result.allocated.find(_.entityId == "2").get.students.size should be (1)
			command.removals.get("2").contains("1") should be {true}
		}
	}

	@Test
	def removeFromAll(): Unit = {
		new Fixture {
			command.entities.addAll(Seq("3", "4").asJava)
			command.action = FetchDepartmentRelationshipInformationCommand.Actions.RemoveFromAll
			val result = command.applyInternal()
			result.unallocated.size should be (9)
			result.allocated.size should be (4)
			result.allocated.flatMap(_.students).distinct.size should be (2)
			result.allocated.find(_.entityId == "3").get.students.size should be (0)
			result.allocated.find(_.entityId == "4").get.students.size should be (0)
			command.removals.get("3").contains("3") should be {true}
			command.removals.get("3").contains("4") should be {true}
			command.removals.get("4").contains("5") should be {true}
			command.removals.get("4").contains("6") should be {true}
			command.removals.get("4").contains("7") should be {true}
		}
	}

}
