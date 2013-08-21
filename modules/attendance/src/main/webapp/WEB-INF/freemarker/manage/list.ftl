<h1>Manage monitoring points</h1>

<form action="<@url page="/manage/${department.code}/years"/>" class="manage-points form-inline">
	<div class="row-fluid">
		<div class="span12">
			<select name="route" class="input-xxlarge">
				<option value="" disabled selected style="display:none;">Route</option>
				<#list department.routes?sort_by("code") as route>
					<option value="${route.code}">${route.code?upper_case} ${route.name}</option>
				</#list>
			</select>
		</div>
	</div>
</form>

<div class="academicyears-target"></div>

<div class="sets-target"></div>

<div class="points-target"></div>

<div id="modal" class="modal hide fade" style="display:none;"></div>