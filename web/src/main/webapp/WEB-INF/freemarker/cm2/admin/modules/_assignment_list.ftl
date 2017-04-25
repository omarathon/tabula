
<table class="table table-striped table-condensed table-hover table-sortable table-checkable sticky-table-headers" id="batch-add-table">
	<thead>
	<tr>
		<th class="for-check-all"><input  type="checkbox"  class="collection-check-all use-tooltip" title="Select all/none"> </th>
		<th>Assignments</th>
		<th></th>
	</tr>
	</thead>
<tbody>
<#list modules as module>
	<#if map[module.code]??>

		<#if showSubHeadings!false>
		<h6 class="module-split">
			<small><@fmt.module_name module /></small>
		</h6>
		</#if>
		<#list map[module.code] as assignment>
		<tr>
			<td><input type="checkbox" class="collection-checkbox" name="assignments" value="${assignment.id}">

			</td>
			<td>${assignment.name}
				<span class="very-subtle">${assignment.academicYear.toString}</span></td>
		</tr>
		</#list>
	</#if>
</#list>

</tbody>
</table>
