<#escape x as x?html>
	<#if !point.pointType?? || point.pointType.dbValue != "smallGroup">
		<div class="alert alert-error">
			Specified monitoring point is not a Teaching event point
		</div>
	<#else>

		<p>
			Monitoring point '${point.name}'
			requires attendance at <strong><@fmt.p point.smallGroupEventQuantity 'event' /></strong>
			for <strong>
				<#if point.smallGroupEventModules?size == 0>
					any module
				<#else>
					<#list point.smallGroupEventModules as module>${module.code?upper_case}<#if module_has_next>, </#if></#list>
				</#if>
			</strong>
			during <strong>
				<#if point.pointType.dbValue == 'date'>
					<@fmt.interval point.startDate point.endDate />
				<#else>
					<@fmt.monitoringPointWeeksFormat
						point.startWeek
						point.endWeek
						point.scheme.academicYear
						point.scheme.department
					/>
				</#if>
			</strong>
		</p>

		<details>
			<summary>
				<span class="legend">Course: ${result.course.name}</span>
			</summary>

			<table>
				<tbody>
					<tr>
						<th>Route:</th>
						<td>${result.course.route}</td>
					</tr>
					<tr>
						<th>Department:</th>
						<td>${result.course.department}</td>
					</tr>
					<tr>
						<th>Status on Route:</th>
						<td>${result.course.status}</td>
					</tr>
					<tr>
						<th>Attendance:</th>
						<td>${result.course.attendance}</td>
					</tr>
					<tr>
						<th>UG/PG:</th>
						<td>${result.course.courseType}</td>
					</tr>
					<tr>
						<th>Year of study:</th>
						<td>${result.course.yearOfStudy}</td>
					</tr>
				</tbody>
			</table>
		</details>

		<details>
			<summary>
				<span class="legend">Modules</span>
			</summary>
		</details>

		<details open>
			<summary>
				<span class="legend">Project groups, Seminars</span>
			</summary>
		</details>

	</#if>
</#escape>