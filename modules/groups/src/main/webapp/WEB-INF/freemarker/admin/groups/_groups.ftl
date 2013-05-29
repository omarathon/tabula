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
					<@f.hidden path="delete" id="group${group_index}_delete" />
				
					<@form.labelled_row "name" "${group_index + 1}.">
						<@f.input path="name" cssClass="text" />
						
						<button type="button" class="btn btn-danger" data-toggle="delete" data-value="true" data-target="#group${group_index}_delete" title="Remove this group, deleting any events and removing any allocated students">
							<i class="icon-remove"></i>
						</button>
						<button type="button" class="btn btn-info" data-toggle="delete" data-value="false" data-target="#group${group_index}_delete" title="Undo deletion of this group">
							<i class="icon-undo"></i>
						</button>
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

	<script type="text/javascript">
		jQuery(function($) {
			$('#groups-modal button[data-toggle="delete"][title]').tooltip({ placement: 'bottom' });
			
			$('#groups-modal button[data-toggle="delete"]').each(function() {
				var $button = $(this);
				var $controlGroup = $button.closest('.control-group');
				var $target = $($button.data('target'));
				var value = "" + $button.data('value');
				
				if ($target.val() === value) { 
					$button.hide();
					
					if (value === "true") {
						$controlGroup.addClass('deleted');
						$controlGroup.find('input').attr('disabled', 'disabled');
					} 
				}
				
				$button.on('click', function() {
					if (value !== "true" || confirm("Deleting this set will also delete any associated events and remove any allocated students. Are you sure?")) {
						$target.val(value);
						
						if (value === "true") {
							$controlGroup.addClass('deleted');
							$controlGroup.find('input').attr('disabled', 'disabled');
						} else {
							$controlGroup.removeClass('deleted');
							$controlGroup.find('input').removeAttr('disabled');
						}
						
						$button.hide();
						$controlGroup.find('button[data-toggle="delete"]').filter(function() {
							var $otherButton = $(this);
							var otherValue = "" + $otherButton.data('value');
							
							return otherValue != value && $otherButton.data('target') == $button.data('target');
						}).show();
					}
				});
			});
		});
	</script>
</#escape>