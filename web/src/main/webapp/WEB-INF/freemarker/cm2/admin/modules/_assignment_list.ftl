<table class="table table-striped table-condensed table-hover table-checkable copy-assignments">
	<thead>
	<tr>
		<th class="for-check-all"><input  type="checkbox"  checked="checked" class="collection-check-all use-tooltip" title="Select all/none"> </th>
		<th>Assignments</th>
		<th></th>
	</tr>
	</thead>
	<tbody>
		<#list modules as module>
			<#if map[module.code]??>
				<#if showSubHeadings!false>
					<tr>
						<td></td>
						<td>
							<@fmt.module_name module />
						</td>
					</tr>
				</#if>
				<#list map[module.code] as assignment>
					<tr>
						<td><input type="checkbox" class="collection-checkbox" name="assignments" value="${assignment.id}"></td>
						<td>${assignment.name}<span class="very-subtle">${assignment.academicYear.toString}</span></td>
					</tr>
				</#list>
			</#if>
		</#list>
	</tbody>
</table>

