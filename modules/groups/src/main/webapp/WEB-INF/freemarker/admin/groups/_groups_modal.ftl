<#escape x as x?html>
	<div id="groups-modal" class="modal hide fade refresh-form" tabindex="-1" role="dialog" aria-labelledby="groups-modal-label" aria-hidden="true">
		<div class="modal-header">
    	<h3 id="groups-modal-label"><#if groups?size gt 0>Edit<#else>Add</#if> groups</h3>
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




					<button type="button" class="btn" data-toggle="add" title="Add another group" disabled="disabled">
						<i class="icon-plus"></i>
					</button>
				</@form.labelled_row>
			</@spring.nestedPath>





		</div>
		<div class="modal-footer">
			<button class="btn" data-dismiss="modal" aria-hidden="true">Done</button>
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
						$controlGroup.find('input').addClass('disabled deleted');
					}
				}

				$button.on('click', function() {
					if (value !== "true" || confirm("Deleting this set will also delete any associated events and remove any allocated students. Are you sure?")) {
						$target.val(value);

						if (value === "true") {
							$controlGroup.addClass('deleted');
							$controlGroup.find('input').addClass('disabled deleted');
						} else {
							$controlGroup.removeClass('deleted');
							$controlGroup.find('input').removeClass('disabled deleted');
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

			$('#groups-modal button[data-toggle="add"]').each(function() {
				var $button = $(this);
				var $group = $button.closest('.control-group');
				var $input = $group.find('input[type="text"]');

				$input.on('paste', function() {
					setTimeout(function() {
						$input.trigger('change');
					}, 50);
				});

				$input.on('change keyup', function() {
					if ($input.val().length > 0) {
						$button.removeAttr('disabled');
					} else {
						$button.attr('disabled', 'disabled');
					}
				});
			});

			$('#groups-modal button[data-toggle="add"]').on('click', function() {
				var $button = $(this);
				var $group = $button.closest('.control-group');

				var $clone = $group.clone();
				$clone.find('button[data-toggle="add"]').remove();

				var index = parseInt(/\[(\d+)\]/.exec($clone.find('input[type="text"]').attr('name'))[1]);
				$clone.find('label').text((index + 1) + ".");

				$clone.insertBefore($group);

				var nextIndex = index + 1;
				var $input = $group.find('input[type="text"]');
				$input.attr('name', 'groups[' + nextIndex + '].name');
				$input.attr('id', 'groups' + nextIndex + '.name');
				$group.find('label').attr('for', $input.attr('id'));

				$input.val('').focus();
				$button.attr('disabled', 'disabled');
			});
		});
	</script>
</#escape>