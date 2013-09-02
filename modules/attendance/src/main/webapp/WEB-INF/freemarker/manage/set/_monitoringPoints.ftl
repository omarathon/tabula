<div class="monitoring-points">
	<@spring.bind path="command.monitoringPoints">
		<#if status.error>
			<div class="alert alert-error"><@f.errors path="command.monitoringPoints" cssClass="error"/></div>
		</#if>
	</@spring.bind>

	<#assign pointCount = 0 />
	<#assign pointFields = ["name", "defaultValue", "week"] />
	<#macro pointsInATerm term>
		<div class="striped-section">
			<h2 class="section-title">${term}</h2>
			<div class="striped-section-contents">
				<#list command.monitoringPointsByTerm[term]?sort_by("week") as point>
					<div class="item-info row-fluid point">
						<div class="span12">
							<div class="pull-right">
								<a class="btn btn-primary edit-point" href="<@url page="/manage/${command.dept.code}/sets/add/points/edit/${pointCount}?form=true"/>">Edit</a>
								<a class="btn btn-danger delete-point" title="Delete" href="<@url page="/manage/${command.dept.code}/sets/add/points/delete/${pointCount}?form=true"/>"><i class="icon-remove"></i></a>
							</div>
							${point.name} (<@fmt.singleWeekFormat point.week command.academicYear command.dept />)

							<#list pointFields as pointField>
								<@spring.bind path="command.monitoringPoints[${pointCount}].${pointField}">
									<#if status.error>
										<div class="alert alert-error"><@f.errors path="command.monitoringPoints[${pointCount}].${pointField}" cssClass="error"/></div>
									</#if>
								</@spring.bind>
							</#list>

							<input type="hidden" name="monitoringPoints[${pointCount}].name" value="${point.name}" />
							<input type="hidden" name="monitoringPoints[${pointCount}].defaultValue" value="<#if point.defaultValue>true<#else>false</#if>" />
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