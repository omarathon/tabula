<#escape x as x?html>
	<#import "*/assignment_components.ftl" as components />
	<#include "assign_marker_macros.ftl" />
<div class="deptheader">
	<h1>Create a new assignment</h1>
	<h4 class="with-related"><span class="muted">for</span> <@fmt.module_name module /></h4>
</div>
<div class="fix-area">
	<#assign actionUrl><@routes.cm2.createassignmentmarkerstemplate assignment /></#assign>
	<@f.form method="post" action=actionUrl enctype="multipart/form-data" cssClass="dirty-check" commandName="assignMarkersBySpreadsheetCommand">
		<@components.assignment_wizard 'markers' assignment.module false assignment />

		<#if assignMarkersBySpreadsheetCommand.rowsWithErrors?size &gt; 0>
			<h2>Invalid rows</h2>
			<h6>The following rows had errors which prevent them being uploaded</h6>
			<table class="table table-bordered table-condensed table-striped">
				<thead>
				<tr>
					<th>Student usercode</th>
					<th>Student name</th>
					<th>Marker usercode</th>
					<th>Marker name</th>
				</tr>
				</thead>
				<tbody>
					<#list assignMarkersBySpreadsheetCommand.rowsWithErrors as row>
						<#list row.errors as error>
						<tr class="no-bottom-border">
							<#if error.rowData["Student usercode"]??><td>${error.rowData["Student usercode"]}</td><#else><td></td></#if>
							<#if error.rowData["Student name"]??><td>${error.rowData["Student name"]}</td><#else><td></td></#if>
							<#if error.rowData["Marker name"]??><td>${error.rowData["Marker name"]}</td><#else><td></td></#if>
							<#if error.rowData["Marker usercode"]??><td>${error.rowData["Marker usercode"]}</td><#else><td></td></#if>
						</tr>
						<tr class="no-top-border">
							<#if error.field == "Student usercode">
								<td colspan="2">
									<@spring.message code=error.code arguments=error.codeArgument />
								</td>
								<td colspan="2"></td>
							<#else>
								<td colspan="2"></td>
								<td colspan="2">
									<@spring.message code=error.code arguments=error.codeArgument />
								</td>
							</#if>
						</tr>
						</#list>
					</#list>
				</tbody>
			</table>
		</#if>

		<#if allocationPreview?has_content>
			<#list allocationOrder as roleName>

				<#assign allocations = mapGet(allocationPreview, roleName)![]>
				<#if allocations?has_content>
					<h2>${roleName} allocations</h2>
					<table class="table table-bordered table-condensed table-striped">
						<thead>
						<tr>
							<th>Student usercode</th>
							<th>Student name</th>
							<th>${roleName} usercode</th>
							<th>${roleName} name</th>
						</tr>
						</thead>
						<tbody>
							<#assign allocations = mapGet(allocationPreview, roleName)![]>
							<#list allocations?keys as marker>
								<#list mapGet(allocations, marker) as student>
								<tr>
									<td>${student.userId}</td>
									<td>${student.fullName}</td>
									<td>${marker.userId}</td>
									<td>${marker.fullName}</td>
								</tr>
								</#list>
							</#list>
						</tbody>
					</table>
				</#if>

				<#if mapGet(unallocatedStudents, roleName)?has_content>
					<h2>Students without a ${roleName}</h2>
					<table class="table table-bordered table-condensed table-striped">
						<thead>
							<tr>
								<th>Student usercode</th>
								<th>Student name</th>
							</tr>
						</thead>
						<tbody>
							<#list mapGet(unallocatedStudents, roleName) as student>
								<tr>
									<td>${student.userId}</td>
									<td>${student.fullName}</td>
								</tr>
							</#list>
						</tbody>
					</table>
				</#if>
			</#list>
		</#if>

		<#list assignMarkersBySpreadsheetCommand.file.attached as attached>
			<input type="hidden" name="file.attached" value="${attached.id}">
		</#list>

		<div class="fix-footer">
			<input
					type="submit"
					class="btn btn-primary"
					name="${ManageAssignmentMappingParameters.createAndAddSubmissions}"
					value="Save and continue"
			/>
			<input
					type="submit"
					class="btn btn-primary"
					name="${ManageAssignmentMappingParameters.createAndAddMarkers}"
					value="Save and exit"
			/>
		</div>
	</@f.form>
</div>
</#escape>