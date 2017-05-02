<#if isNew>
	<@bs3form.labelled_form_group path="workflowType" labelText="Workflow type">
		<@f.select path="workflowType" class="form-control" >
			<option value="" disabled selected></option>
			<#list availableWorkflows as workflowType>
				<option <#if status.value?? && (status.value!"") == workflowType.name> selected="selected"</#if>
						value="${workflowType.name}"
						data-numroles="${workflowType.roleNames?size}"
						data-roles="${workflowType.roleNames?join(",")}"
				>
					${workflowType.description}
				</option>
			</#list>
		</@f.select>
	</@bs3form.labelled_form_group>
<#else>
	<@bs3form.labelled_form_group>
		<label for="workflowType" class="control-label ">Workflow type</label>
		<select id="workflowType" name="workflowType" class="form-control" disabled="disabled">
			<option
					selected="selected"
					value="${workflow.workflowType.name}"
					data-numroles="${workflow.workflowType.roleNames?size}"
					data-roles="${workflow.workflowType.roleNames?join(",")}"
			>
				${workflow.workflowType.description}
			</option>
		</select>
		<div class="help-block">
			It is not possible to modify the marking method once a marking workflow has been created.
		</div>
	</@bs3form.labelled_form_group>
</#if>

<#assign markerHelp>You can add an individual by name or university ID.<#if !canDeleteMarkers> At least one assignment that uses this workflow has marking in progress so you can't remove markers. You can replace markers instead.</#if></#assign>

<@bs3form.labelled_form_group path="markersA" labelText="Add markers" help="${markerHelp}" cssClass="markersA">
	<@bs3form.flexipicker path="markersA" placeholder="User name" list=true multiple=true auto_multiple=false delete_existing=canDeleteMarkers />
</@bs3form.labelled_form_group>


<@bs3form.labelled_form_group path="markersB" labelText="Add markers" help="${markerHelp}" cssClass="markersB hide">
	<@bs3form.flexipicker path="markersB" placeholder="User name" list=true multiple=true auto_multiple=false delete_existing=canDeleteMarkers />
</@bs3form.labelled_form_group>

<#if !isNew>
	<@bs3form.labelled_form_group>
	<a href="<@routes.cm2.reusableWorkflowReplaceMarker department academicYear workflow />">Replace marker</a>
	</@bs3form.labelled_form_group>
</#if>

<script type="text/javascript">
	(function ($) { "use strict";

		$('select[name=workflowType]').on('change', function() {
			var $this = $(this);
			var $workflowOption = $this.find('option:selected');
			var roleNames = $workflowOption.data('roles') ? $workflowOption.data('roles').split(",") : [];
			if(roleNames.length !== 0){
				$('.form-group.markersA label').text(roleNames[0]);
				var useMarkerB = $workflowOption.data("numroles") > 1;
				if(useMarkerB) {
					$('.form-group.markersB label').text(roleNames[1]);
				}
				$('.markersB').removeClass('hide').toggle(useMarkerB);
			}

		}).trigger('change');

	})(jQuery);
</script>