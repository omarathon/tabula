<#escape x as x?html>
	<#list command.groupNames as group>
		<@form.labelled_row "groupNames[${group_index}]" "${group_index + 1}.">
			<@f.input path="groupNames[${group_index}]" cssClass="text" />
		</@form.labelled_row>
	</#list>

	<#assign group_index = command.groupNames?size />

	<@form.labelled_row "groupNames[${group_index}]" "${group_index + 1}.">
		<@f.input path="groupNames[${group_index}]" cssClass="text" />

		<button type="button" class="btn" data-toggle="add" title="Add another group" disabled="disabled">
			<i class="icon-plus"></i>
		</button>
	</@form.labelled_row>

	<script type="text/javascript">
		jQuery(function($) {
			$('button[data-toggle="add"]').each(function() {
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

			$('button[data-toggle="add"]').on('click', function() {
				var $button = $(this);
				var $group = $button.closest('.control-group');

				var $clone = $group.clone();
				$clone.find('button[data-toggle="add"]').remove();

				var index = parseInt(/\[(\d+)\]/.exec($clone.find('input[type="text"]').attr('name'))[1]);
				console.log(index);
				$clone.find('label').text((index + 1) + ".");

				$clone.insertBefore($group);

				var nextIndex = index + 1;
				var $input = $group.find('input[type="text"]');
				$input.attr('name', 'groupNames[' + nextIndex + ']');
				$input.attr('id', 'groupNames' + nextIndex);
				$group.find('label').attr('for', $input.attr('id')).text((nextIndex + 1) + '.');

				$input.val('').focus();
				$button.attr('disabled', 'disabled');
			});
		});
	</script>
</#escape>