<#escape x as x?html>

	<#assign formDestination><@routes.profiles.relationship_allocate_old department relationshipType /></#assign>
	<#assign mappingById=allocateStudentsToRelationshipCommand.mappingById />

	<@spring.bind path="allocateStudentsToRelationshipCommand">
		<#assign hasErrors=status.errors.allErrors?size gt 0 />
	</@spring.bind>

	<#assign hasRows = allocateStudentsToRelationshipCommand.unallocated?has_content || allocateStudentsToRelationshipCommand.mapping?keys?has_content />

	<@f.form method="post" action=formDestination commandName="allocateStudentsToRelationshipCommand">
		<h1>Spreadsheet upload of ${relationshipType.description} changes</h1>
		<h4><span class="muted">for</span> ${department.name}</h4>

		<div class="submit-buttons">
			<#if hasRows>
				<input type="hidden" name="spreadsheet" value="true" />
				<button type="submit" class="btn btn-primary">Continue</button>
			</#if>

			<a class="btn" href="<@routes.profiles.relationship_allocate_old department relationshipType />">Cancel</a>
		</div>

		<#if hasErrors>
			<h2>Invalid rows</h2>
			<h6>The following rows had errors which prevent them being uploaded</h6>

			<table class="table table-bordered table-condensed table-striped">
				<thead>
					<tr>
						<th>${relationshipType.studentRole?cap_first} ID</th>
						<th>${relationshipType.studentRole?cap_first} name</th>
						<th>${relationshipType.agentRole?cap_first} ID</th>
						<th>${relationshipType.agentRole?cap_first} name</th>
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
						</tr>
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
			<h2>Valid rows</h2>

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

					<#list allocateStudentsToRelationshipCommand.mapping?keys as agent>
						<#assign existingStudents = mappingById[agent.universityId]![] />

						<#list existingStudents as student>
							<@hasAgent "mapping[${agent.universityId}][${student_index}]" student agent />
						</#list>
					</#list>
				</tbody>
			</table>
		<#elseif hasErrors>
			<h2>Valid rows</h2>

			<p class="text-error">
				<i class="icon-warning-sign"></i> There were no valid rows in the spreadsheet. Please review your spreadsheet data.
			</p>
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