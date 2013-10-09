<#escape x as x?html>

	<#assign formDestination><@routes.relationship_allocate department relationshipType /></#assign>
	<#assign mappingById=allocateStudentsToRelationshipCommand.mappingById />

	<@spring.bind path="allocateStudentsToRelationshipCommand">
		<#assign hasErrors=status.errors.allErrors?size gt 0 />
	</@spring.bind>
	
	<#assign hasRows = allocateStudentsToRelationshipCommand.unallocated?has_content || allocateStudentsToRelationshipCommand.mapping?keys?has_content />

	<@f.form method="post" action=formDestination commandName="allocateStudentsToRelationshipCommand">
		<h1>Preview ${relationshipType.description} changes for ${department.name}</h1>
		
		<#include "_allocate_notifications_modal.ftl" />

		<div class="submit-buttons">
			<#if hasRows><input type="hidden" name="confirm" value="true"></#if>
			<button type="button" class="btn btn-primary" data-toggle="modal" data-target="#notify-modal"<#if !hasRows> disabled="disabled"</#if>>Confirm</button>
			<a class="btn" href="<@routes.relationship_allocate department relationshipType />">Cancel</a>
		</div>
		
		<#if hasErrors>
			<h2>Invalid rows</h2>
			
			<div class="alert">
				<p>There were problems with some rows in your spreadsheet. If you confirm, these rows will be ignored.</p>
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
					<#macro errorRow error>
						<#local field=error.arguments[0] />
						<#local rowData=error.arguments[1] />
						
						<#local studentNameField>${relationshipType.studentRole?lower_case} name</#local>
						<#local agentNameField>${relationshipType.agentRole?lower_case} name</#local>
					
						<tr class="no-bottom-border">
							<td<#if field == 'student_id'> class="error"</#if>>${(rowData['student_id'])!}</td>
							<td<#if field == 'student_id'> class="error"</#if>>${(rowData[studentNameField])!}</td>
							<td<#if field == 'agent_id'> class="error"</#if>>${(rowData['agent_id'])!}</td>
							<td<#if field == 'agent_id'> class="error"</#if>>${(rowData[agentNameField])!}</td>
						</tr>
						<tr class="no-top-border">
							<#if field == 'student_id'>
								<td colspan="2" class="error"><@spring.message message=error /></td>
							<#else>
								<td colspan="2"></td>
							</#if>
							<#if field == 'agent_id'>
								<td colspan="2" class="error"><@spring.message message=error /></td>
							<#else>
								<td colspan="2"></td>
							</#if>
						</td>
					</#macro>
				
					<@spring.bind path="allocateStudentsToRelationshipCommand">
						<#list status.errors.allErrors as error>
							<#if error.arguments?size gt 0>
								<@errorRow error />
							</#if>
						</#list>
					</@spring.bind>
				</tbody>
			</table>
		</#if>
		
		<#if hasRows>
			<h2>Changes to be made</h2>
	
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
		<#else>
			<div class="alert alert-block alert-error">
				<h2>No information was found in the spreadsheet</h2>
				
				<p>In order for students to be allocated or unallocated from their ${relationshipType.agentRole}, there must be
				at least two columns in the spreadsheet. One must have a header of <strong>student_id</strong> and contain
				University card numbers for students, and the other must have a header of <strong>agent_id</strong> and contain
				University card numbers of each student's ${relationshipType.agentRole}. If the <strong>agent_id</strong> is blank,
				any existing ${relationshipType.agentRole} will be removed.</p>
			</div>
		</#if>
	</@f.form>
</#escape>