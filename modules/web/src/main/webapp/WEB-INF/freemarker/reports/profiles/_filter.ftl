<#if !results?has_content && (command.hasBeenFiltered || command.searchSingle || command.searchMulti)>
	<#if command.noPermissionIds?has_content>
		<div class="alert alert-error">
			You do not have permission to view the following students: <#list command.noPermissionIds as universityId>${universityId}<#if universityId_has_next>, </#if></#list>
		</div>
	<#else>
		<p><em>No students found.</em></p>
	</#if>
<#elseif results?has_content>
	<form action="<@routes.reports.profileExportReport department academicYear />" method="post">
		<div class="pull-right">
			<input type="submit" class="btn btn-primary generate" disabled title="Use the checkboxes to select students" value="Generate reports on selected students" />
		</div>
		<#if command.noPermissionIds?has_content>
			<div class="clearfix"></div>
			<div class="alert alert-error">
				You do not have permission to view the following students: <#list command.noPermissionIds as universityId>${universityId}<#if universityId_has_next>, </#if></#list>
			</div>
		</#if>
		<p><@fmt.p results?size "result" /></p>
		<table class="table table-condensed table-bordered table-striped students">
			<thead>
			<tr>
				<th></th>
				<th class="sortable">First name</th>
				<th class="sortable">Last name</th>
				<th class="sortable">ID</th>
				<th>Route</th>
			</tr>
			</thead>
			<tbody>
				<#list results as student>
				<tr>
					<td><input name="students" value="${student.universityId}" type="checkbox" <#if command.searchSingle || command.searchMulti >checked</#if>/></td>
					<td>${student.firstName}</td>
					<td>${student.lastName}</td>
					<td>${student.universityId}</td>
					<td>${student.routeCode?upper_case} ${student.routeName}</td>
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