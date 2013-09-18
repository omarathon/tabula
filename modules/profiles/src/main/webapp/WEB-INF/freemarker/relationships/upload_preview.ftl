<#escape x as x?html>

	<#assign formDestination><@routes.relationship_allocate department relationshipType /></#assign>
	<#assign mappingById=allocateStudentsToRelationshipCommand.mappingById />

	<@spring.bind path="allocateStudentsToRelationshipCommand">
		<#assign hasErrors=status.errors.allErrors?size gt 0 />
	</@spring.bind>

	<@f.form method="post" action=formDestination commandName="allocateStudentsToRelationshipCommand">
		<h1>Preview ${relationshipType.description} changes for ${department.name}</h1>
		
		<#include "_allocate_notifications_modal.ftl" />

		<div class="submit-buttons">
			<input type="hidden" name="confirm" value="true">
			<button type="button" class="btn btn-primary" data-toggle="modal" data-target="#notify-modal">Confirm</button>
			<a class="btn" href="<@routes.relationship_allocate department relationshipType />">Cancel</a>
		</div>

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
				<#macro noAgent path student>
					<tr class="error">
						<td>
							<@f.hidden path=path value=(student.universityId)! />
							${(student.universityId)!}
						</td>
						<td>
							${(student.fullName)!}
						</td>
						<td>
							
						</td>
						<td>
							
						</td>
					</tr>
				</#macro>
				
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
				
				<#list allocateStudentsToRelationshipCommand.unallocated as student>
					<@noAgent "unallocated[${student_index}]" student />
				</#list>
				
				<#list allocateStudentsToRelationshipCommand.mapping?keys as agent>
					<#assign existingStudents = mappingById[agent.universityId]![] />
					
					<#list existingStudents as student>
						<@hasAgent "mapping[${agent.universityId}][${student_index}]" student agent />
					</#list>
				</#list>
			</tbody>
		</table>
	</@f.form>
</#escape>