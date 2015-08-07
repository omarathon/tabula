<#escape x as x?html>

	<#assign formDestination><@routes.relationship_allocate_old department relationshipType /></#assign>
	<#assign mappingById=allocateStudentsToRelationshipCommand.mappingById />


	<@spring.bind path="allocateStudentsToRelationshipCommand">
		<#assign hasErrors=status.errors.allErrors?size gt 0 />
	</@spring.bind>

	<@f.form method="post" action=formDestination commandName="allocateStudentsToRelationshipCommand">
		<@f.hidden name="confirmed" value="confirmed"/>
		<@f.hidden path="spreadsheet"/>

	<h1>You have requested the following ${relationshipType.description?lower_case} changes</h1>

		<h3 class="relationship-change-summary">
			<span class="emphasis">${allocateStudentsToRelationshipCommand.studentsWithTutorRemoved?size}</span> students: ${relationshipType.description?lower_case} removed</h3>
		<h3 class="relationship-change-summary">
			<span class="emphasis">${allocateStudentsToRelationshipCommand.studentsWithTutorChanged?size}</span> students: ${relationshipType.description?lower_case} changed</h3>
		<h3 class="relationship-change-summary">
			<span class="emphasis">${allocateStudentsToRelationshipCommand.studentsWithTutorAdded?size}</span> students: ${relationshipType.description?lower_case} added</h3>

		<#include "_allocate_notifications_modal.ftl" />

		<!-- send all of the data in "mapping" through the form -->
		<#list allocateStudentsToRelationshipCommand.mapping?keys as agent>
			<#assign existingStudents = mappingById[agent.universityId]![] />

			<#list existingStudents as student>
				<@f.hidden path="mapping[${agent.universityId}][${student_index}]" />
			</#list>
		</#list>

		<#macro displayStudentChange studentID studentFullName agent>
		<tr>
			<td>
			${(studentID)!}
			</td>
			<td>
			${(studentFullName)!}
			</td>
			<td>
			${(agent.universityId)!}
			</td>
			<td>
			${(agent.fullName)!}
			</td>
		</tr>
		</#macro>

		<#if allocateStudentsToRelationshipCommand.studentsWithTutorAdded?size gt 0>
			<#-- display the students with tutors that have been added -->
			<h2 class="relationship-change-detail">Students newly given ${relationshipType.description?lower_case}s</h2>

			<table class="table table-bordered table-condensed table-striped">
				<thead>
				<tr>
					<th>${relationshipType.studentRole?cap_first} ID</th>
					<th>${relationshipType.studentRole?cap_first} Name</th>
					<th>${relationshipType.agentRole?cap_first} ID</th>
					<th>${relationshipType.agentRole?cap_first} Name</th>
				</tr>
				</thead>
				<tbody>

					<#-- 3 levels of lists?? really?
						first iterates through all the agents with changed mappings
						second iterates through all the students for that agent
						third iterates through studentsWithTutorAdded looking for matches
						TODO: - factor this logic out into the currently overburdened command (TAB-2609)
					-->

<#--					<#list allocateStudentsToRelationshipCommand.newOrChangedMapping?keys as agent>
						<#assign studentsForAgent = mappingById[agent.universityId]![] />

						<#list studentsForAgent as student>
							<#assign studentUniId = student.universityId></assign>
							<#list allocateStudentsToRelationshipCommand.studentsWithTutorAdded.keySet as studentForTutor>
								<#if studentUniId = studentForTutor.universityId>
									<@displayStudentChange "newOrChangedMapping[${agent.universityId}][${student_index}]" student agent />
								</#if>
							</#list>
						</#list>
					</#list>-->

					<#list added?keys as student>
						<#list added[student] as tutor>
							<@displayStudentChange student studentNameMap[student] tutor />
						</#list>
					</#list>

				</tbody>
			</table>
		</#if>

		<#if allocateStudentsToRelationshipCommand.studentsWithTutorChanged?size gt 0>
		<!-- display the students with tutors that have been changed -->
		<h2 class="relationship-change-detail">Students with changed ${relationshipType.description?lower_case}s</h2>

		<table class="table table-bordered table-condensed table-striped">
			<thead>
			<tr>
				<th>${relationshipType.studentRole?cap_first} ID</th>
				<th>${relationshipType.studentRole?cap_first} Name</th>
				<th>${relationshipType.agentRole?cap_first} ID</th>
				<th>${relationshipType.agentRole?cap_first} Name</th>
			</tr>
			</thead>
			<tbody>
				<#list changed?keys as student>
					<#list changed[student] as tutor>
						<@displayStudentChange student studentNameMap[student] tutor />
					</#list>
				</#list>
			</tbody>
		</table>
		</#if>

		<!-- display the students with tutors that have been removed -->
		<#if allocateStudentsToRelationshipCommand.studentsWithTutorRemoved?has_content>
			<h2 class="relationship-change-detail">${relationshipType.description?lower_case?cap_first}s to be removed</h2>

			<table class="table table-bordered table-condensed table-striped">
				<thead>
				<tr>
					<th>${relationshipType.studentRole?cap_first} ID</th>
					<th>${relationshipType.studentRole?cap_first} Name</th>
				</tr>
				</thead>
				<tbody>
					<#macro agentRemoved student>
						<tr>
							<td>
								${(student.universityId)!}
							</td><td>
								${(student.fullName)!}
							</td>
						</tr>
					</#macro>

					<#list allocateStudentsToRelationshipCommand.studentsWithTutorRemoved?keys as student>
							<@agentRemoved student />
					</#list>
				</tbody>
			</table>

		</#if>

		<div class="submit-buttons">
			<#if allocateStudentsToRelationshipCommand.hasChanges>
				<input type="hidden" name="confirm" value="true">
			</#if>
			<button type="button" class="btn btn-primary" data-toggle="modal" data-target="#notify-modal"
				<#if !allocateStudentsToRelationshipCommand.hasChanges> disabled="disabled"
				</#if>
			>Confirm</button>
			<a class="btn" href="<@routes.relationship_allocate department relationshipType />">Cancel</a>
		</div>

	</@f.form>
</#escape>
