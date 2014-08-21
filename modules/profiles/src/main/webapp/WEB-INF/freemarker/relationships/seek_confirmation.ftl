<#escape x as x?html>

	<#assign formDestination><@routes.relationship_allocate department relationshipType /></#assign>
	<#assign mappingById=allocateStudentsToRelationshipCommand.mappingById />

	<@spring.bind path="allocateStudentsToRelationshipCommand">
		<#assign hasErrors=status.errors.allErrors?size gt 0 />
	</@spring.bind>
	
	<#assign hasRows =  allocateStudentsToRelationshipCommand.newOrChangedMapping?keys?has_content />

	<@f.form method="post" action=formDestination commandName="allocateStudentsToRelationshipCommand">
		<@f.hidden name="confirmed" value="confirmed"/>
		<h1>You have requested the following changes</h1>

		<h3 class="relationship-change-requests">
			<span class="emphasis">${allocateStudentsToRelationshipCommand.studentsWithTutorRemoved?size}</span> students: ${relationshipType.description?lower_case} removed</h3>
		<h3 class="relationship-change-requests">
			<span class="emphasis">${allocateStudentsToRelationshipCommand.studentsWithTutorChanged?size}</span> students: ${relationshipType.description?lower_case} changed</h3>
		<h3 class="relationship-change-requests">
			<span class="emphasis">${allocateStudentsToRelationshipCommand.studentsWithTutorAdded?size}</span> students: ${relationshipType.description?lower_case} added. </h3>

		<#include "_allocate_notifications_modal.ftl" />

		<#if hasRows>
			<h2>${relationshipType.description} changes</h2>

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
					<#macro hasAgent path student agent>
					<tr>
						<td>
							<@f.hidden path=path value=(student.universityId)! />
									${(student.universityId)!}
						</td>
						<td>
						${(student.fullName)!}
						</td>
						<td>
						${(agent.universityId)!}
						</td>
						<td>
						${(agent.fullName)!}
						</td>
					</tr>
					</#macro>

					<#list allocateStudentsToRelationshipCommand.newOrChangedMapping?keys as agent>
						<#assign existingStudents = mappingById[agent.universityId]![] />

						<#list existingStudents as student>
							<@hasAgent "mapping[${agent.universityId}][${student_index}]" student agent />
						</#list>
					</#list>
				</tbody>
			</table>
		</#if>

		<#if allocateStudentsToRelationshipCommand.studentsWithTutorRemoved?has_content>
			<h2>${relationshipType.description}s to be removed</h2>

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

					<#list allocateStudentsToRelationshipCommand.studentsWithTutorRemoved as student>
						<@agentRemoved student />
					</#list>
				</tbody>
			</table>

		</#if>

		<div class="submit-buttons">
			<#if allocateStudentsToRelationshipCommand.hasChanges><input type="hidden" name="confirm" value="true"></#if>
			<button type="button" class="btn btn-primary" data-toggle="modal" data-target="#notify-modal"<#if !hasRows> disabled="disabled"</#if>>Confirm</button>
			<a class="btn" href="<@routes.relationship_allocate department relationshipType />">Cancel</a>
		</div>

	</@f.form>
</#escape>
