<#if route.monitoringPointSets?size == 0>
	<div class="row-fluid">
		<div class="span12 create-buttons">
			<em>There are no existing sets for this route.</em>
			<a class="btn btn-primary create-set" href="<@url page="/manage/${departmentCode}/sets/add"/>"><i class="icon-plus"/> Create new set</a>
		</div>
	</div>
	<script>
    	Attendance.Manage.bindNewSetButton();
    </script>
<#elseif route.monitoringPointSets?first.year??>
	<div class="row-fluid">
		<div class="span3">
			<form action="<@url page="/manage/${departmentCode}/points"/>" class="manage-points-year form-inline">
				<select name="set-year">
					<option value="" disabled selected style="display:none;">Year of study</option>
					<#list route.monitoringPointSets?sort_by("year") as set>
						<option value="${set.year}">${set.year}</option>
					</#list>
				</select>
			</form>
		</div>
		<div class="span9 create-buttons">
			<a class="btn btn-primary create-set" href="<@url page="/manage/${departmentCode}/sets/add"/>">
				<i class="icon-plus"/> Create new set
			</a>
			<a class="btn btn-primary create-point" href="<@url page="/manage/${departmentCode}/points/add"/>">
				<i class="icon-plus"/> Create new point
			</a>
		</div>
	</div>
	<script>
		Attendance.Manage.bindChooseYear();
		Attendance.Manage.bindNewSetButton();
	</script>
<#else>
	<div class="row-fluid">
		<div class="span12 create-buttons">
			<form action="<@url page="/manage/${departmentCode}/points"/>" class="manage-points-year form-inline">
				<em>This set of points is used for all years of the route.</em>
				<a class="btn btn-primary create-point" href="<@url page="/manage/${departmentCode}/points/add"/>">
					<i class="icon-plus"/> Create new point
				</a>
			</form>
		</div>
	</div>
	<script>
    	Attendance.Manage.onChooseYear();
    </script>
</#if>