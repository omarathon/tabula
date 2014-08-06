<#escape x as x?html>
	<fieldset>
		<@form.labelled_row "title" "Title">
			<@f.input path="title" />
		</@form.labelled_row>

		<@form.labelled_row "tutors" "Tutors">
			<@form.flexipicker path="tutors" placeholder="User name" list=true multiple=true auto_multiple=false />
		</@form.labelled_row>

		<@form.row path="weeks">
			<@form.label>
				Running in these weeks
				<@form.label checkbox=true clazz="pull-right">
					<input type="checkbox" id="show-vacations" value="true">
					Show vacations
				</@form.label>
			</@form.label>

			<@form.field>
				<table class="table table-striped table-bordered week-selector">
					<thead>
						<tr>
							<#assign colspan=0 />
							<#list allTerms as namedTerm>
								<#if (namedTerm.weekRange.maxWeek - namedTerm.weekRange.minWeek) gt colspan>
									<#assign colspan=(namedTerm.weekRange.maxWeek - namedTerm.weekRange.minWeek) />
								</#if>
							</#list>
							<th colspan="${colspan + 2}" style="text-align: center;">
								Weeks
								<#assign helpText>
									<p>Select the weeks that this small group event will run in by clicking on each week. Click on the name of the term or vacation to select all weeks in that term or vacation.</p>
								</#assign>
								<a href="#"
								   class="use-introductory<#if showIntro("sgt-week-selector", "anywhere")> auto</#if>"
								   data-title="Selecting weeks for a small group event"
								   data-trigger="click"
								   data-placement="bottom"
								   data-html="true"
								   data-hash="${introHash("sgt-week-selector", "anywhere")}"
								   data-content="${helpText}"><i class="icon-question-sign icon-fixed-width"></i></a>
							</th>
						</tr>
					</thead>
					<tbody>
						<#list allTerms as namedTerm>
							<#assign is_vacation = !(namedTerm.term.termType?has_content) />
							<tr<#if is_vacation> class="vacation"</#if>>
								<th>${namedTerm.name}<#if !is_vacation> term</#if></th>
								<#list namedTerm.weekRange.minWeek..namedTerm.weekRange.maxWeek as weekNumber>
									<td
										class="use-tooltip"
										title="<@fmt.singleWeekFormat weekNumber smallGroupSet.academicYear module.department />"
										data-html="true"
										data-container="body">
										<@f.checkbox path="weeks" value="${weekNumber}" />
										<span class="week-number"><@fmt.singleWeekFormat weekNumber smallGroupSet.academicYear module.department true /></span>
									</td>
								</#list>
							</tr>
						</#list>
					</tbody>
				</table>
			</@form.field>
		</@form.row>

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
			<@f.hidden path="locationId" />
			<@f.input path="location" />
		</@form.labelled_row>
	</fieldset>

	<script type="text/javascript">
		jQuery(function($) {
			$('table.week-selector').each(function() {
				var $table = $(this);

				var updateCell = function($cell, value) {
					var $icon = $cell.find('i');
					if (value) {
						$icon.addClass('icon-ok');
						$cell.addClass('checked');
					} else {
						$icon.removeClass('icon-ok');
						$cell.removeClass('checked');
					}
				};

				$table.find('input[type="checkbox"]').each(function() {
					var $checkbox = $(this);
					var $cell = $checkbox.closest('td');

					$checkbox.hide();

					var $icon = $('<i />').addClass('icon-fixed-width');
					$checkbox.after($icon);

					updateCell($cell, $checkbox.is(':checked'));

					$cell.on('click', function() {
						$checkbox.prop('checked', !$checkbox.prop('checked'));
						updateCell($cell, $checkbox.is(':checked'));
					});
				});
				$table.find('tbody tr th').each(function() {
					var $header = $(this);
					var $cells = $header.closest('tr').find('td');
					$header.on('click', function() {
						var allChecked = $cells.find('input[type="checkbox"]:not(:checked)').length == 0;
						if (allChecked) {
							$cells.each(function() {
								var $cell = $(this);
								$cell.find('input[type="checkbox"]').prop('checked', false);
								updateCell($cell, false);
							});
						} else {
							$cells.each(function() {
								var $cell = $(this);
								$cell.find('input[type="checkbox"]').prop('checked', true);
								updateCell($cell, true);
							});
						}
					});
				});

				$('#show-vacations').each(function() {
					var $checkbox = $(this);

					if ($table.find('tr.vacation td.checked').length) {
						$checkbox.prop('checked', true);
					}

					var updateDisplay = function() {
						if ($checkbox.is(':checked')) {
							$table.find('tr.vacation').show();
						} else {
							$table.find('tr.vacation').hide();
						}
					};
					updateDisplay();

					$checkbox.on('change', updateDisplay);
				});
			});

			$('#location')
				.on('change', function() {
					var $this = $(this);
					if ($this.data('lid') === undefined || $this.data('lid').length === 0)
						return;

					$('#locationId').val($this.data('lid'));
					$this.data('lid','');
				})
				.locationPicker();
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