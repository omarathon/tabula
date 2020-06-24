package uk.ac.warwick.tabula.commands.scheduling.imports

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.{Daoisms, SessionComponent}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.scheduling.{AssignmentImporterComponent, AutowiringAssignmentImporterComponent, MembershipMember}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, SprCode}

import scala.jdk.CollectionConverters._
import scala.util.Try

object ImportAssignmentsCommand {
  def apply(): ComposableCommandWithoutTransaction[Unit] = new ComposableCommandWithoutTransaction[Unit]
    with ImportAssignmentsCommand
    with RemovesMissingAssessmentComponentsCommand
    with RemovesMissingUpstreamAssessmentGroupsCommand
    with ImportAssignmentsAllMembers
    with ImportAssignmentsDescription
    with AutowiringAssignmentImporterComponent
    with Daoisms

  def applyAllYears(): ComposableCommandWithoutTransaction[Unit] = new ComposableCommandWithoutTransaction[Unit]
    with ImportAssignmentsAllYearsCommand
    with RemovesMissingAssessmentComponentsCommand
    with RemovesMissingUpstreamAssessmentGroupsCommand
    with ImportAssignmentsAllMembers
    with ImportAssignmentsDescription
    with AutowiringAssignmentImporterComponent
    with Daoisms {
    override lazy val eventName = "ImportAssignmentsAllYears"
  }

  def applyIndividualYear(year: AcademicYear): ComposableCommandWithoutTransaction[Unit] = new ComposableCommandWithoutTransaction[Unit]
    with ImportAssignmentsIndividualYearCommand
    with RemovesMissingAssessmentComponentsCommand
    with RemovesMissingUpstreamAssessmentGroupsCommand
    with ImportAssignmentsAllMembers
    with ImportAssignmentsDescription
    with AutowiringAssignmentImporterComponent
    with Daoisms {
    override def yearsToImport: Seq[AcademicYear] = Seq(year)

    override lazy val eventName = "ImportAssignmentsIndividualYear"
  }

  def applyForMembers(applyMembers: Seq[MembershipMember], applyYears: Seq[AcademicYear]): ComposableCommandWithoutTransaction[Unit] = new ComposableCommandWithoutTransaction[Unit]
    with ImportAssignmentsCommand
    with ImportAssignmentsSpecificMembers
    with ImportAssignmentsDescription
    with AutowiringAssignmentImporterComponent
    with Daoisms {
    override val members: Seq[MembershipMember] = applyMembers
    override val yearsToImport: Seq[AcademicYear] = applyYears
  }

  case class Result(
    assignmentsFound: Int,
    assignmentsChanged: Int,
    groupsFound: Int,
    groupsChanged: Int
  )
}

trait ImportAssignmentsMembersToImport {
  def yearsToImport: Seq[AcademicYear]

  def membersToImport(assessmentType: UpstreamAssessmentGroupMemberAssessmentType)(callback: UpstreamAssessmentRegistration => Unit): Unit

  def importEverything: Boolean
}

trait ImportAssignmentsAllMembers extends ImportAssignmentsMembersToImport {
  self: AssignmentImporterComponent =>

  override def membersToImport(assessmentType: UpstreamAssessmentGroupMemberAssessmentType)(callback: UpstreamAssessmentRegistration => Unit): Unit = {
    assignmentImporter.allMembers(assessmentType, yearsToImport)(callback)
  }

  val importEverything: Boolean = true
}

trait ImportAssignmentsSpecificMembers extends ImportAssignmentsMembersToImport {
  self: AssignmentImporterComponent =>

  val members: Seq[MembershipMember]

  override def membersToImport(assessmentType: UpstreamAssessmentGroupMemberAssessmentType)(callback: UpstreamAssessmentRegistration => Unit): Unit = {
    assignmentImporter.specificMembers(members, assessmentType, yearsToImport)(callback)
  }

  val importEverything: Boolean = false
}

trait ImportAssignmentsCommand extends CommandInternal[Unit] with RequiresPermissionsChecking with Logging with SessionComponent
  with AssignmentImporterComponent with ImportAssignmentsMembersToImport with RemovesMissingAssessmentComponentsCommand with RemovesMissingUpstreamAssessmentGroupsCommand {
  def permissionsCheck(p: PermissionsChecking): Unit = {
    p.PermissionCheck(Permissions.ImportSystemData)
  }

  var assessmentMembershipService: AssessmentMembershipService = Wire[AssessmentMembershipService]
  var moduleAndDepartmentService: ModuleAndDepartmentService = Wire[ModuleAndDepartmentService]

  val ImportGroupSize = 100

  var modifiedAssignments: Set[Assignment] = Set.empty

  override def yearsToImport: Seq[AcademicYear] = AcademicYear.allCurrent() :+ AcademicYear.now().next

  def applyInternal(): Unit = {
    benchmark("ImportAssessment") {
      if (importEverything) {
        doAssignments()
        logger.debug("Imported AssessmentComponents. Importing assessment groups...")
        doGroups()
        doGroupMembers()
        logger.debug("Imported assessment groups. Importing grade boundaries...")
        doGradeBoundaries()
        logger.debug("Imported grade boundaries. Importing variable assessment weighting rules...")
        doVariableAssessmentWeightingRules()
        logger.debug("Imported variable assessment weighting rules. Importing exam schedule...")
        doExamSchedule()

        logger.debug("Removing blank feedback for students who have deregistered...")
        removeBlankFeedbackForDeregisteredStudents()
      } else {
        logger.debug("Importing assessment group members...")
        doGroupMembers()
      }
    }
  }

  def doAssignments(): Unit = {
    benchmark("Process Assessment components") {
      val existingAssessmentComponents = transactional(readOnly = true) {
        assessmentMembershipService.getAllAssessmentComponents(yearsToImport)
      }

      val assessmentComponents = logSize {
        assignmentImporter.getAllAssessmentComponents(yearsToImport)
          .filter(ac => Module.stripCats(ac.moduleCode).nonEmpty) // Ignore any duff data
      }

      val modules = transactional(readOnly = true) {
        moduleAndDepartmentService.getModulesByCodes(assessmentComponents.map(_.moduleCodeBasic).distinct)
          .groupBy(_.code).view.mapValues(_.head)
      }
      assessmentComponents.grouped(ImportGroupSize).foreach(assignments => {
        benchmark("Save assessment component related assignments") {
          transactional() {
            assignments.foreach(assignment => {
              if (assignment.name == null) {
                // Some SITS data is bad, but try to carry on.
                assignment.name = "Assessment Component"
              }

              modules.get(assignment.moduleCodeBasic.toLowerCase).foreach(module => assignment.module = module)
              assessmentMembershipService.save(assignment)
            })
          }
        }
      })

      // find any existing AssessmentComponents that no longer appear in SITS
      existingAssessmentComponents.foreach { existing =>
        transactional() {
          if (!assessmentComponents.exists(upstream => upstream.sameKey(existing))) {
            session.refresh(existing)
            removeAssessmentComponent(existing)
          }
        }
      }
    }
  }

  def doGroups(): Unit = {
    benchmark("Import assessment groups") {
      val existingUpstreamAssessmentGroupsForThatYear = transactional(readOnly = true) {
        assessmentMembershipService.getUpstreamAssessmentGroups(yearsToImport)
      }
      val upstreamAssessmentGroupsFromSITS = assignmentImporter.getAllAssessmentGroups(yearsToImport)

      // Split into chunks so we commit transactions periodically.
      for (groups <- logSize(upstreamAssessmentGroupsFromSITS).grouped(ImportGroupSize)) {
        saveGroups(groups)
        transactional() {
          session.flush()
          groups foreach session.evict
        }
      }

      existingUpstreamAssessmentGroupsForThatYear.foreach { existing =>
        if (!upstreamAssessmentGroupsFromSITS.exists(upstream => upstream.isEquivalentTo(existing))) {
          removeUpstreamAssessmentGroup(existing)
        }
      }
    }
  }

  /**
   * This calls the importer method that iterates over ALL module registrations.
   * The results are ordered such that it can hold a list of items until it
   * detects one that belongs to a different group, at which point it saves
   * what it's got and starts a new list. This way we don't have to load many
   * things into memory at once.
   */
  def doGroupMembers(): Unit = {
    benchmark("Import all group members") {
      def doGroupMembersForAssessmentType(assessmentType: UpstreamAssessmentGroupMemberAssessmentType): Set[UpstreamAssessmentGroup] = {
        var notEmptyGroups = Set[UpstreamAssessmentGroup]()
        var registrations = List[UpstreamAssessmentRegistration]()
        var count = 0
        benchmark(s"Process $assessmentType group members") {
          membersToImport(assessmentType) { r =>
            if (registrations.nonEmpty && r.differentGroup(registrations.head, assessmentType)) {
              // This element r is for a new group, so save this group and start afresh

              transactional() {
                notEmptyGroups = notEmptyGroups ++ save(registrations, assessmentType)
              }
              registrations = Nil
            }
            registrations = registrations :+ r

            count += 1
            if (count % 1000 == 0) {
              logger.info(s"Processed $count $assessmentType group members")
            }
          }
        }

        // TAB-1265 Don't forget the very last bunch.
        if (registrations.nonEmpty) {
          benchmark(s"Save final $assessmentType registrations") {
            transactional() {
              notEmptyGroups = notEmptyGroups ++ save(registrations, assessmentType)
            }
          }
        }

        logger.info(s"Imported $count $assessmentType group members")

        notEmptyGroups
      }

      val notEmptyGroupIdsByAssessmentType: Map[UpstreamAssessmentGroupMemberAssessmentType, Set[UpstreamAssessmentGroup]] =
        UpstreamAssessmentGroupMemberAssessmentType.values.map { assessmentType =>
          assessmentType -> doGroupMembersForAssessmentType(assessmentType).filter(uag => yearsToImport.contains(uag.academicYear))
        }.toMap

      if (importEverything) {
        // We need to clear out groups we didn't see for each type; e.g. where a resit used to be against a sequence but not anymore
        benchmark("Clearing out unseen groups per assessment type") {
          UpstreamAssessmentGroupMemberAssessmentType.values.foreach { assessmentType =>
            val seenIds = notEmptyGroupIdsByAssessmentType(assessmentType).map(_.id)

            notEmptyGroupIdsByAssessmentType.view.filterKeys(_ != assessmentType)
              .values.flatten
              .filterNot(uag => seenIds.contains(uag.id))
              .foreach { uag =>
                transactional() {
                  assessmentMembershipService.replaceMembers(uag, Seq.empty, assessmentType)
                }
              }
          }
        }

        val notEmptyGroupIds: Set[String] = notEmptyGroupIdsByAssessmentType.values.flatten.toSet.map((g: UpstreamAssessmentGroup) => g.id)

        // empty unseen groups - this is done in transactional batches

        val groupsToEmpty = transactional(readOnly = true) {
          assessmentMembershipService.getUpstreamAssessmentGroupsNotIn(
            ids = notEmptyGroupIds.filter(_.hasText).toSeq,
            academicYears = yearsToImport
          )
        }

        logger.info("Emptying members for unseen groups")
        val numEmptied = transactional() {
          assessmentMembershipService.emptyMembers(groupsToEmpty)
        }
        logger.info(s"Emptied $numEmptied users from ${groupsToEmpty.size} unseen groups")
      }
    }
  }

  def doExamSchedule(): Unit = {
    benchmark("Import exam schedule") {
      transactional() { // Do it all in one big tx
        val existing = assessmentMembershipService.allScheduledExams(assignmentImporter.publishedExamProfiles(yearsToImport))

        val seenIds =
          assignmentImporter.getAllScheduledExams(yearsToImport)
            .filter(ac => Module.stripCats(ac.moduleCode).nonEmpty) // Ignore any duff data
            .map { schedule =>
              val updated =
                assessmentMembershipService.findScheduledExamBySlotSequence(schedule.examProfileCode, schedule.slotId, schedule.sequence, schedule.locationSequence)
                  .map(_.copyFrom(schedule))
                  .getOrElse(schedule)

              // Make sure any transient instances are saved before we try and add students to them
              assessmentMembershipService.save(updated)

              val existingStudents = updated.students.asScala.toSeq
              val students =
                assignmentImporter.getScheduledExamStudents(updated)
                  .map { student =>
                    if (!student.universityId.hasText && student.sprCode.hasText) {
                      // Just guess, duff data
                      student.universityId = student.sprCode.split('/').head
                    }

                    student
                  }
                  .filter(_.universityId.nonEmpty)

              existingStudents.filterNot(s => students.exists(_.universityId == s.universityId))
                .foreach(updated.students.remove)

              students.foreach { s =>
                existingStudents.find(e => s.universityId == e.universityId) match {
                  case Some(existing) if existing.sameDataAs(s) => // do nothing
                  case Some(existing) => existing.copyFrom(s)
                  case _ =>
                    s.schedule = updated
                    updated.students.add(s)
                }
              }

              assessmentMembershipService.save(updated)

              updated.id
            }

        // Delete any ids that we haven't seen
        existing.filterNot(e => seenIds.contains(e.id)).foreach(assessmentMembershipService.delete)
      }
    }
  }

  /**
   * This sequence of ModuleRegistrations represents the members of an assessment
   * group, so save them (and reconcile it with any existing members we have in the
   * database).
   * The students in a group does NOT vary by sequence, so the membership should be set on ALL the groups.
   * Then set the properties of members of each group by sequence.
   */
  def save(registrations: Seq[UpstreamAssessmentRegistration], assessmentType: UpstreamAssessmentGroupMemberAssessmentType): Seq[UpstreamAssessmentGroup] = {
    registrations.headOption.map { head =>
      var hasChanged: Boolean = false

      // Get all the Assessment Components we have in the DB, even if marked as not in use, as we might have previous years groups to populate
      val allAssessmentComponentsForAssessmentGroup = assessmentMembershipService.getAssessmentComponents(head.moduleCode, inUseOnly = false)
        .filter(_.assessmentGroup == head.assessmentGroup)

      val assessmentComponents = assessmentType match {
        case UpstreamAssessmentGroupMemberAssessmentType.OriginalAssessment =>
          allAssessmentComponentsForAssessmentGroup

        case UpstreamAssessmentGroupMemberAssessmentType.Reassessment =>
          // Reassessment group membership varies by sequence so we should receive smaller batches with only registrations for this sequence
          allAssessmentComponentsForAssessmentGroup.filter(_.sequence == head.sequence)
      }

      val assessmentGroups = head.toUpstreamAssessmentGroups(assessmentComponents.map(_.sequence).distinct)
        .map { assessmentGroup =>
          assessmentMembershipService.getUpstreamAssessmentGroup(assessmentGroup)
            .map { group =>
              if (importEverything) {
                val existingUniversityIdAndResitSequences: Set[(String, Option[String])] = group.members.asScala.map(m => (m.universityId, m.resitSequence)).toSet
                val newUniversityIdAndResitSequences: Set[(String, Option[String])] = registrations.map(r => (r.universityId, r.resitSequence.maybeText)).toSet

                if (existingUniversityIdAndResitSequences != newUniversityIdAndResitSequences) {
                  hasChanged = true
                  assessmentMembershipService.replaceMembers(group, registrations, assessmentType)
                } else group
              } else group
            }
            .getOrElse(assessmentGroup)
        }

      // Now sort out properties
      val hasSequence = registrations.filter(r => r.sequence != null)
      assessmentGroups.foreach { group =>
        // Find the registrations for this exact group (including sequence)
        val theseRegistrations = hasSequence.filter(_.toExactUpstreamAssessmentGroup.isEquivalentTo(group))
        if (theseRegistrations.nonEmpty) {
          val registrationsByStudent = theseRegistrations.groupBy(r => (r.sprCode, r.resitSequence))

          // Where there are multiple values for each of the properties (seat number, mark, and grade) we need to flatten them to a single value.
          // Where there is ambiguity, set the value to None
          val propertiesMap: Map[(String, String), UpstreamAssessmentGroupMemberProperties] = registrationsByStudent.map { case ((sprCode, resitSequence), studentRegistrations) =>
            (sprCode, resitSequence) -> {
              if (studentRegistrations.size == 1) {
                new UpstreamAssessmentGroupMemberProperties {
                  position = Try(studentRegistrations.head.seatNumber.toInt).toOption
                  actualMark = Try(studentRegistrations.head.actualMark.toInt).toOption
                  actualGrade = studentRegistrations.head.actualGrade.maybeText
                  agreedMark = Try(studentRegistrations.head.agreedMark.toInt).toOption
                  agreedGrade = studentRegistrations.head.agreedGrade.maybeText
                  currentResitAttempt = Try(studentRegistrations.head.currentResitAttempt.toInt).toOption
                }
              } else {
                def validInts(strings: Seq[String]): Seq[Int] = strings.filter(s => Try(s.toInt).isSuccess).map(_.toInt)

                def validStrings(strings: Seq[String]): Seq[String] = strings.filter(s => s.maybeText.isDefined)

                def resolveDuplicates[A](props: Seq[A], description: String): Option[A] = {
                  if (props.distinct.size > 1) {
                    logger.debug("Found multiple %ss (%s) for %s for Assessment Group %s. %s will be null".format(
                      description,
                      props.mkString(", "),
                      sprCode,
                      group.toString,
                      description.capitalize
                    ))
                    None
                  } else {
                    props.headOption
                  }
                }

                new UpstreamAssessmentGroupMemberProperties {
                  position = resolveDuplicates(validInts(studentRegistrations.map(_.seatNumber)), "seat number")
                  actualMark = resolveDuplicates(validInts(studentRegistrations.map(_.actualMark)), "actual mark")
                  actualGrade = resolveDuplicates(validStrings(studentRegistrations.map(_.actualGrade)), "actual grade")
                  agreedMark = resolveDuplicates(validInts(studentRegistrations.map(_.agreedMark)), "agreed mark")
                  agreedGrade = resolveDuplicates(validStrings(studentRegistrations.map(_.agreedGrade)), "agreed grade")
                  currentResitAttempt = resolveDuplicates(validInts(studentRegistrations.map(_.currentResitAttempt)), "current attempt number")
                }
              }
            }
          }

          propertiesMap.foreach { case ((sprCode, resitSequence), properties) =>
            group.members.asScala.find(m => m.universityId == SprCode.getUniversityId(sprCode) && m.resitSequence.orNull == resitSequence).foreach { member =>
              val memberHasChanged = (
                member.position != properties.position ||
                member.actualMark != properties.actualMark ||
                member.actualGrade != properties.actualGrade ||
                member.agreedMark != properties.agreedMark ||
                member.agreedGrade != properties.agreedGrade ||
                member.currentResitAttempt != properties.currentResitAttempt
              )

              if (memberHasChanged) {
                hasChanged = true

                member.position = properties.position
                member.actualMark = properties.actualMark
                member.actualGrade = properties.actualGrade
                member.agreedMark = properties.agreedMark
                member.agreedGrade = properties.agreedGrade
                member.currentResitAttempt = properties.currentResitAttempt
                assessmentMembershipService.save(member)
              }
            }
          }
        }
      }

      if (hasChanged) {
        modifiedAssignments = modifiedAssignments ++ assessmentComponents.flatMap(_.linkedAssignments)
      }

      assessmentGroups
    }.getOrElse(Seq())
  }


  def saveGroups(groups: Seq[UpstreamAssessmentGroup]): Unit = transactional() {
    logger.debug("Importing " + groups.size + " assessment groups")
    benchmark("Import " + groups.size + " groups") {
      for (group <- groups) {
        assessmentMembershipService.save(group)
      }
    }
  }

  def doGradeBoundaries(): Unit = {
    transactional() {
      val sitsBoundaries = assignmentImporter.getAllGradeBoundaries

      val marksAndProcessAndAttempt = sitsBoundaries.map(gb => (gb.marksCode, gb.process, gb.attempt)).distinct

      val fmBoundary = marksAndProcessAndAttempt.flatMap { case (marksCode, process, attempt) =>
        if (sitsBoundaries.exists(gb => gb.marksCode == marksCode && gb.grade == GradeBoundary.ForceMajeureMissingComponentGrade && gb.process == process && gb.attempt == attempt)) None
        else Some(GradeBoundary(marksCode, process, attempt, 1000, GradeBoundary.ForceMajeureMissingComponentGrade, None, None, "S", None)) // No suggested result for FM, all results are allowed
      }

      val wBoundary = marksAndProcessAndAttempt.flatMap { case (marksCode, process, attempt) =>
        if (sitsBoundaries.exists(gb => gb.marksCode == marksCode && gb.grade == GradeBoundary.WithdrawnGrade && gb.process == process)) {
          Seq.empty
        } else if (sitsBoundaries.exists(gb => gb.marksCode == marksCode && gb.process == process && gb.minimumMark.nonEmpty && gb.isDefault)) {
          val passMark =
            sitsBoundaries.find(gb => gb.marksCode == marksCode && gb.process == process && gb.minimumMark.nonEmpty && gb.isDefault && gb.result.contains(ModuleResult.Pass))
              .flatMap(_.minimumMark)
              .minOption
              .getOrElse(ProgressionService.DefaultPassMark)

          Seq(
            GradeBoundary(marksCode, process, attempt, 999, GradeBoundary.WithdrawnGrade, Some(passMark), Some(100), "S", Some(ModuleResult.Pass)),
            GradeBoundary(marksCode, process, attempt, 999, GradeBoundary.WithdrawnGrade, Some(0), Some(passMark - 1), "S", Some(ModuleResult.Fail))
          )
        } else {
          Seq(GradeBoundary(marksCode, process, attempt, 999, GradeBoundary.WithdrawnGrade, None, None, "S", Some(ModuleResult.Fail)))
        }
      }

      // Inject the FM and W grades if they don't exist already
      val allBoundaries = sitsBoundaries ++ fmBoundary ++ wBoundary

      allBoundaries.groupBy(_.marksCode).keys.foreach(assessmentMembershipService.deleteGradeBoundaries)
      for (gradeBoundary <- logSize(allBoundaries)) {
        assessmentMembershipService.save(gradeBoundary)
      }
    }
  }

  def doVariableAssessmentWeightingRules(): Unit = transactional() {
    val existing = assessmentMembershipService.allVariableAssessmentWeightingRules
    val upstream = assignmentImporter.getAllVariableAssessmentWeightingRules

    val additions = upstream.filterNot(rule => existing.exists(_.matchesKey(rule)))
    val deletions = existing.filterNot(rule => upstream.exists(_.matchesKey(rule)))
    val modifications = existing.flatMap { rule =>
      upstream.find(_.matchesKey(rule)).map { r =>
        rule.copyFrom(r)
        rule
      }
    }

    (additions ++ modifications).foreach(assessmentMembershipService.save)
    deletions.foreach(assessmentMembershipService.delete)
  }

  def removeBlankFeedbackForDeregisteredStudents(): Seq[Feedback] =
    modifiedAssignments.toSeq
      .flatMap { assignment =>
        transactional() {
          // Find students who are assigned to a marker but are not a member of the assignment
          val memberUsercodes = assessmentMembershipService.determineMembershipUsers(assignment).map(_.getUserId)
          val removedMembers = assignment.allFeedback.map(_.usercode).toSet.diff(memberUsercodes.toSet)

          val removedFeedback = assignment.allFeedback
            .filter(f => removedMembers.contains(f.usercode))
            .collect {
              case feedback if feedback.hasBeenModified || assignment.findSubmission(feedback.usercode).exists(_.submitted) =>
                logger.debug(s"${feedback.usercode} is no longer a member of assignment ${assignment.id} but has submission or feedback")

                null
              case feedback =>
                logger.info(s"Removing feedback for ${feedback.usercode} from assignment ${assignment.id}")

                assignment.feedbacks.remove(feedback)
                feedback.assignment = null
                session.delete(feedback)

                feedback
            }
            .filter(_ != null)

          removedFeedback
        }
      }
}


trait ImportAssignmentsDescription extends Describable[Unit] {
  def describe(d: Description): Unit = {}
}

trait ImportAssignmentsYearCommand extends ImportAssignmentsCommand {
  def process(year: AcademicYear): Unit = {
    benchmark(s"ImportAssignmentsCommand for $year") {
      doAssignments()
      doGroups()
      doGroupMembers()
      doExamSchedule()
      logger.info(s"ImportAssignmentsCommand for $year completed")
    }
  }

}

trait ImportAssignmentsAllYearsCommand extends ImportAssignmentsYearCommand {

  var yearZero: Int = Wire.property("${tabula.yearZero:2000}").toInt
  private var currentYear: AcademicYear = _

  override def yearsToImport: Seq[AcademicYear] = Seq(currentYear)

  override def applyInternal(): Unit = {
    val next = AcademicYear.now().next.startYear
    for (year <- yearZero until next) {
      currentYear = AcademicYear.starting(year)
      process(currentYear)
    }
  }
}

trait ImportAssignmentsIndividualYearCommand extends ImportAssignmentsYearCommand {
  override def applyInternal(): Unit = {
    process(yearsToImport.head)
  }
}

trait RemovesMissingAssessmentComponents {
  def removeAssessmentComponent(assessmentComponent: AssessmentComponent): Unit
}

trait RemovesMissingAssessmentComponentsCommand extends RemovesMissingAssessmentComponents {
  override def removeAssessmentComponent(assessmentComponent: AssessmentComponent): Unit = new RemoveMissingAssessmentComponentCommand(assessmentComponent).apply()
}

trait RemovesMissingUpstreamAssessmentGroups {
  def removeUpstreamAssessmentGroup(upstreamAssessmentGroup: UpstreamAssessmentGroup): Unit
}

trait RemovesMissingUpstreamAssessmentGroupsCommand extends RemovesMissingUpstreamAssessmentGroups {
  override def removeUpstreamAssessmentGroup(upstreamAssessmentGroup: UpstreamAssessmentGroup): Unit = new RemoveMissingUpstreamAssessmentGroupCommand(upstreamAssessmentGroup).apply()
}

