<h1>Manage monitoring points</h1>

<form action="" class="form-inline">
	<div class="row-fluid">
		<div class="span12">
			<select name="route" class="input-xxlarge">
				<option value="" disabled selected style="display:none;">Route</option>
				<#list department.routes?sort_by("code") as route>
					<option value="${route.code}">${route.code?upper_case} ${route.name}</option>
				</#list>
			</select>
			<!--<select name="year"  class="input-small">
				<option value="" selected style="display:none;">Year</option>
				<#list 1..8 as year>
					<option value="${year}">${year}</option>
				</#list>
			</select>
			<input type="submit" class="btn btn-primary" value="Show">-->
		</div>
	</div>
</form>