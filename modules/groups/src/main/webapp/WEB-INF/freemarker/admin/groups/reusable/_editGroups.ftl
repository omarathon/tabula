<#escape x as x?html>
	<#list command.groupNames as group>
		<@form.labelled_row "groupNames[${group_index}]" "${group_index + 1}.">
			<@f.input path="groupNames[${group_index}]" cssClass="text" />
			&nbsp;
			<button type="button" class="btn btn-danger" data-toggle="remove"><i class="icon-remove"></i></button>
		</@form.labelled_row>
	</#list>

	<#assign group_index = command.groupNames?size />

	<@form.labelled_row "groupNames[${group_index}]" "${group_index + 1}.">
		<@f.input path="groupNames[${group_index}]" cssClass="text" />
	</@form.labelled_row>

	<@form.row>
		<@form.field>
			<button type="button" class="btn" data-toggle="add" title="Add another group" disabled="disabled">
				<i class="icon-plus"></i> Add group
			</button>
		</@form.field>
	</@form.row>

	<script type="text/javascript">
		jQuery(function($) {
			$('button[data-toggle="add"]').each(function() {
				var $button = $(this);
				var $group = $button.closest('.control-group').prev('.control-group');
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

			$('button[data-toggle="add"]').on('click', function() {
				var $button = $(this);
				var $group = $button.closest('.control-group').prev('.control-group');

				var $clone = $group.clone();

				var index = parseInt(/\[(\d+)\]/.exec($clone.find('input[type="text"]').attr('name'))[1]);
				$clone.find('label').text((index + 1) + ".");

				$clone.insertBefore($group);
				$clone.find('.controls')
						.append(' &nbsp; ')
						.append('<button type="button" class="btn btn-danger" data-toggle="remove"><i class="icon-remove"></i></button>');

				var nextIndex = index + 1;
				var $name = $group.find('input[type="text"]');
				$name.attr('name', 'groupNames[' + nextIndex + ']');
				$name.attr('id', 'groupNames' + nextIndex);

				$group.find('label').attr('for', $name.attr('id')).text((nextIndex + 1) + '.');

				$name.val('').focus();
				$button.attr('disabled', 'disabled');
			});

			$(document.body).on('click', 'button[data-toggle="remove"]', function() {
				var $button = $(this);
				var $group = $button.closest('.control-group');

				$group.remove();

				// Re-order all of the groups
				var groups = $('input[name^="groupNames"]').closest('.control-group');
				groups.each(function(index) {
					var $group = $(this);

					var $name = $group.find('input[type="text"]');
					$name.attr('name', 'groupNames[' + index + ']');
					$name.attr('id', 'groupNames' + index);

					$group.find('label').attr('for', $name.attr('id')).text((index + 1) + '.');
				});
			});
		});
	</script>
</#escape>