<#escape x as x?html>

<#if createType == "blank">
	<h1>Create monitoring schemes</h1>
</#if>

<form action="" class="form-inline">
	<label>Academic year <select name="academicYear">
		<#-- <#assign academicYears = [command.academicYear.previous.toString, command.academicYear.toString, command.academicYear.next.toString] /> -->
		<#assign academicYears = [command.academicYear.toString] />
        <#list academicYears as year>
        	<option <#if command.academicYear.toString == year>selected</#if> value="${year}">${year}</option>
        </#list>
	</select></label>
</form>

<hr />

<form action="<@url page="/manage/${command.dept.code}/sets/add"/>" method="POST">
	<div class="routeAndYearPicker">
		<div class="row-fluid">
			<div class="span2">
				<h2>Students</h2>
			</div>
			<div class="span10">
				<span class="hint"><#if createType == "blank">
					Create monitoring schemes for the following students
				</#if></span>
			</div>
		</div>

		<div class="row-fluid">
			<div class="span2">
				<h3>Route and year of study</h3>
			</div>
			<div class="span10">
				<p class="collapsible"><i class="icon-fixed-width icon-chevron-right"></i> There <span class="routes-count">are no routes</span> selected</p>
				<#assign yearList = ["1","2","3","4","5","6","7","8","All"] />
				<div class="collapsible-target">
					<table class="table table-bordered table-striped table-condensed table-hover header">
						<thead>
							<tr>
								<th class="ellipsis">Route</th>
								<th colspan="9">Year of study</th>
							</tr>
							<tr class="years">
								<th></th>
								<#list yearList as year>
									<th class="year_${year}" data-year="${year}">${year}</th>
								</#list>
							</tr>
						</thead>
					</table>
					<div class="scroller">
						<table class="table table-bordered table-striped table-condensed table-hover">
							<tbody>
								<#list command.availableRoutes as route>
									<#assign availableYearsForRoute = command.availableYears[route.code]/>
									<tr>
										<td class="ellipsis" title="${route.code?upper_case} ${route.name}">
											${route.code?upper_case} ${route.name}
										</td>
										<#list yearList as year>
											<#assign checked = ""/>
											<#if command.selectedRoutesAndYearsByRouteCode(route.code)[year]>
												<#assign checked = "checked"/>
											</#if>
											<td class="year_${year}">
												<#if availableYearsForRoute[year]>
													<input ${checked} data-year="${year}" type="checkbox" name="selectedRoutesAndYears[${route.code}][${year}]" value="true" />
												<#else>
													<input data-year="${year}" type="checkbox" name="selectedRoutesAndYears[${route.code}][${year}]" value="false" disabled title="Unavailable"/>
												</#if>
											</td>
										</#list>
									</tr>
								</#list>
							</tbody>
						</table>
					</div>
				</div>

				<@spring.bind path="command.selectedRoutesAndYears">
					<#if status.error>
						<div class="alert alert-error"><@f.errors path="command.selectedRoutesAndYears" cssClass="error"/></div>
					</#if>
				</@spring.bind>
			</div>
		</div>
	</div>

	<hr />

	<div class="row-fluid">
		<div class="span3">
			<h2>Monitoring points</h2>
		</div>
		<div class="span9">
			<a href="" class="btn btn-primary"><i class="icon-plus"></i> Create new point</a>
		</div>
	</div>

	<#macro pointsInATerm term>
		<div class="striped-section">
			<h2 class="section-title">${term}</h2>
			<div class="striped-section-contents">
				<#list command.pointsByTerm[term]?sort_by("week") as point>
					<div class="item-info row-fluid point">
						<div class="span12">
							<div class="pull-right">
								<a class="btn btn-primary edit" href="<@url page="/manage/${departmentCode}/points/edit?set=${point.pointSet.id}&point=${point.id}"/>">Edit</a>
								<a class="btn btn-danger delete" href="<@url page="/manage/${departmentCode}/points/delete?set=${point.pointSet.id}&point=${point.id}"/>"><i class="icon-delete"></i></a>
							</div>
							${point.name} (<@fmt.weekRanges point />)
						</div>
					</div>
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
			<#if command.pointsByTerm[term]??>
				<@pointsInATerm term/>
			</#if>
		</#list>
	</#if>

	<input type="submit" value="Create" class="btn btn-primary"/> <a class="btn" href="<@url page="/manage/${command.dept.code}"/>">Cancel</a>

</form>


</#escape>