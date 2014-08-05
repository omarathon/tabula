<#escape x as x?html>
	<fieldset>
		<@form.labelled_row "title" "Title">
			<@f.input path="title" />
		</@form.labelled_row>

		<@form.labelled_row "tutors" "Tutors">
			<@form.flexipicker path="tutors" placeholder="User name" list=true multiple=true />
		</@form.labelled_row>

		<@form.labelled_row path="weeks" label="Terms" fieldCssClass="controls-row">
			<@spring.bind path="weeks">
				<#assign allWeeks=(status.actualValue)![] />
			</@spring.bind>

			<#list allTermWeekRanges as term_week_range>
				<#assign weeks = term_week_range.weekRange.toWeeks />
				<#assign full = term_week_range.isFull(allWeeks) />
				<#assign partial = !full && term_week_range.isPartial(allWeeks) />

				<div class="span4">
					<@form.label checkbox=true>
						<input id="weeks${term_week_range_index}-checkbox" type="checkbox" value="true" <#if full || partial>checked="checked"</#if> data-indeterminate="<#if partial>true<#else>false</#if>" data-target="#weeks${term_week_range_index}">
						Term ${term_week_range_index+1}
					</@form.label>

					<@f.select path="weeks" id="weeks${term_week_range_index}" size="${weeks?size?c}" multiple="true" cssClass="individual-weeks span1">
						<#list weeks as week>
							<@f.option value="${week}" label="${week_index+1}" />
						</#list>
					</@f.select>
				</div>
			</#list>

			<div class="very-subtle individual-weeks span3" style="margin-top: 30px;">
				Drag to select a week range. Hold Ctrl and click to select and deselect individual weeks.
			</div>

			<div class="clearfix"></div>
			<button type="button" class="btn btn-mini" data-toggle="elements" data-target=".individual-weeks">Select individual weeks</button>
		</@form.labelled_row>

		<@form.labelled_row "day" "Day">
			<@f.select path="day" id="day">
				<@f.option value="" label=""/>
				<@f.options items=allDays itemLabel="name" itemValue="asInt" />
			</@f.select>
		</@form.labelled_row>

		<#-- The time-picker causes the entire page to become a submit button, can't work out why -->
		<@form.labelled_row "startTime" "Start time">
			<@f.input path="startTime" cssClass="time-picker startDateTime" />
			<input class="endoffset" type="hidden" data-end-offset="3600000" />
		</@form.labelled_row>

		<@form.labelled_row "endTime" "End time">
			<@f.input path="endTime" cssClass="time-picker endDateTime" />
		</@form.labelled_row>

		<@form.labelled_row "location" "Location">
			<@f.input path="location" />
		</@form.labelled_row>
	</fieldset>

	<script type="text/javascript">
		jQuery(function($) {
			$('span[data-toggle="tooltip"]').tooltip();

			$('button[data-toggle="elements"][data-target]').on('click', function() {
				var $button = $(this);
				var $target = $($button.data('target'));

				$target.show();
				$button.hide();
			});

			// Initially hide all of the elements, we may show them if they're the target of indeterminate-ness
			$('button[data-toggle="elements"][data-target]').each(function() {
				var $button = $(this);
				var $target = $($button.data('target'));
				$target.hide();
			});

			$('input[type="checkbox"][data-indeterminate]').each(function() {
				var $checkbox = $(this);
				$checkbox.prop('indeterminate', $checkbox.data('indeterminate'));

				if ($checkbox.data('target')) {
					var $target = $($checkbox.data('target'));

					if ($target.prop('multiple')) {
						// Wire a change listener on the target to manage the indeterminate nature
						$target.on('change', function() {
							var $select = $(this);
							var $options = $select.find('option');
							var $selected = $options.filter(':selected');

							if ($options.length == $selected.length) {
								// All selected
								$checkbox.attr('checked', 'checked');
								$checkbox.prop('indeterminate', false);
							} else if ($selected.length == 0) {
								// None selected
								$checkbox.removeAttr('checked');
								$checkbox.prop('indeterminate', false);
							} else {
								// Indeterminate
								$checkbox.attr('checked', 'checked');
								$checkbox.prop('indeterminate', true);
							}
						});
					}
				}
			});

			$('input[type="checkbox"][data-target]').on('change', function() {
				var $checkbox = $(this);
				var $target = $($checkbox.data('target'));
				if ($checkbox.is(':checked')) {
					$target.find('option').attr('selected', 'selected');
				} else {
					$target.find('option').removeAttr('selected');
				}
			});
		});
	</script>

	<style type="text/css">
		<#-- Hide the confusing dates in the header of the time picker -->
		.datetimepicker-hours thead i { display: none !important; }
		.datetimepicker-hours thead .switch { visibility: hidden; }
		.datetimepicker-hours thead th { height: 0px; }
		.datetimepicker-minutes thead .switch { visibility: hidden; }
	</style>
</#escape>