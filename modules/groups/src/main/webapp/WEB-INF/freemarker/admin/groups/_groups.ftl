<#escape x as x?html>

	<button type="button" data-target="#groups-modal" class="btn" data-toggle="modal">
		Add<#if groups?size gt 0>/edit</#if> groups
	</button>

	<div id="groups-modal" class="modal hide fade refresh-form" tabindex="-1" role="dialog" aria-labelledby="groups-modal-label" aria-hidden="true">
		<div class="modal-header">
			<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
    	<h3 id="groups-modal-label">Add<#if groups?size gt 0>/edit</#if> groups</h3>
		</div>	
		<div class="modal-body">
			<#list groups as group>
				<@spring.nestedPath path="groups[${group_index}]">
					<@form.labelled_row "name" "${group_index + 1}.">
						<@f.input path="name" cssClass="text" />
					</@form.labelled_row>
				</@spring.nestedPath>
			</#list>
			
			<@spring.nestedPath path="groups[${groups?size}]">
				<@form.labelled_row "name" "Group name">
					<@f.input path="name" cssClass="text" />
				</@form.labelled_row>
			</@spring.nestedPath>
		</div>
		<div class="modal-footer">
			<button class="btn btn-primary" data-dismiss="modal" aria-hidden="true">Save</button>
		</div>
	</div>

</#escape>