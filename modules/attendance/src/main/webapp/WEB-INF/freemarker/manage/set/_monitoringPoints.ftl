<div class="monitoring-points">
	<#assign pointCount = 0 />
	<#macro pointsInATerm term>
		<div class="striped-section">
			<h2 class="section-title">${term}</h2>
			<div class="striped-section-contents">
				<#list command.monitoringPointsByTerm[term]?sort_by("week") as point>
					<div class="item-info row-fluid point">
						<div class="span12">
							<div class="pull-right">
								<a class="btn btn-primary edit-point" href="<@url page="/manage/${command.dept.code}/sets/add/points/edit/${point_index}?form=true"/>">Edit</a>
								<a class="btn btn-danger delete-point" title="Delete" href="<@url page="/manage/${command.dept.code}/sets/add/points/delete/${point_index}?form=true"/>"><i class="icon-remove"></i></a>
							</div>
							${point.name} (<@fmt.singleWeekFormat point.week command.academicYear command.dept />)
							<input type="hidden" name="monitoringPoints[${pointCount}].name" value="${point.name}" />
							<input type="hidden" name="monitoringPoints[${pointCount}].defaultValue" value="<#if point.defaultValue>true<#else>false></#if>" />
							<input type="hidden" name="monitoringPoints[${pointCount}].week" value="${point.week}" />
						</div>
					</div>
					<#assign pointCount = pointCount + 1 />
				</#list>
			</div>
		</div>
	</#macro>

	<#if command.monitoringPoints?size == 0>
		<div class="row-fluid">
			<div class="span12">
				<em>No points exist for this scheme</em>
			</div>
		</div>
	<#else>
		<#list ["Autumn", "Christmas vacation", "Spring", "Easter vacation", "Summer", "Summer vacation"] as term>
			<#if command.monitoringPointsByTerm[term]??>
				<@pointsInATerm term/>
			</#if>
		</#list>
	</#if>
</div>