<#if !results?has_content && (command.hasBeenFiltered || command.searchSingle || command.searchMulti)>
	<#if command.noPermissionIds?has_content || command.notFoundIds?has_content>
		<div class="alert alert-danger">
			<#if command.noPermissionIds?has_content>
				You do not have permission to view the following students: <#list command.noPermissionIds as universityId>${universityId}<#if universityId_has_next>, </#if></#list>
			</#if>
			<#if command.notFoundIds?has_content>
				Could not find the following students: <#list command.notFoundIds as universityId>${universityId}<#if universityId_has_next>, </#if></#list>
			</#if>
		</div>
	<#else>
		<p><em>No students found.</em></p>
	</#if>
<#elseif results?has_content>
	<#if command.noPermissionIds?has_content || command.notFoundIds?has_content>
		<div class="alert alert-danger">
			<#if command.noPermissionIds?has_content>
				You do not have permission to view the following students: <#list command.noPermissionIds as universityId>${universityId}<#if universityId_has_next>, </#if></#list>
			</#if>
			<#if command.notFoundIds?has_content>
				Could not find the following students: <#list command.notFoundIds as universityId>${universityId}<#if universityId_has_next>, </#if></#list>
			</#if>
		</div>
	</#if>
	<form action="<@routes.reports.profileExportReport department academicYear />" method="post">
		<div class="pull-right">
			<input type="submit" class="btn btn-primary generate" disabled title="Use the checkboxes to select students" value="Generate reports on selected students" />
		</div>
		<#if command.noPermissionIds?has_content>
			<div class="clearfix"></div>
			<div class="alert alert-danger">
				You do not have permission to view the following students: <#list command.noPermissionIds as universityId>${universityId}<#if universityId_has_next>, </#if></#list>
			</div>
		</#if>
		<p><@fmt.p results?size "result" /></p>
		<table class="table table-condensed table-striped students">
			<thead>
			<tr>
				<th></th>
				<th class="sortable">First name</th>
				<th class="sortable">Last name</th>
				<th class="sortable">ID</th>
				<th>Route</th>
				<th class="sortable">Supervisor</th>
			</tr>
			</thead>
			<tbody>
				<#list results as stuWithSCDs>
				<tr>
					<td><input name="students" value="${stuWithSCDs.studentData.universityId}" type="checkbox" <#if command.searchSingle || command.searchMulti >checked</#if>/></td>
					<td>${stuWithSCDs.studentData.firstName}</td>
					<td>${stuWithSCDs.studentData.lastName}</td>
					<td>${stuWithSCDs.studentData.universityId}</td>
					<td>${(stuWithSCDs.studentData.routeCode!"")?upper_case} ${stuWithSCDs.studentData.routeName!""}</td>
					<td>
						<#list stuWithSCDs.significantSCDs as scd>
							<#list scd.allRelationships as relationship>
								<#if relationship.relationshipType.toString == "StudentRelationshipType(supervisor)">
									${relationship._agentMember.fullName}<#if relationship_has_next>, </#if>
								</#if>
							</#list>
						</#list>
					</td>
				</tr>
				</#list>
			</tbody>
		</table>
	</form>
	<script>
		jQuery(function($){
			$('table.students').sortableTable();
		});
	</script>
</#if>