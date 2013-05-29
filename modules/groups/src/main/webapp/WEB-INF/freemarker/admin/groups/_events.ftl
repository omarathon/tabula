<#escape x as x?html>
	<@spring.bind path="academicYear">
		<#assign academicYear=status.actualValue />
	</@spring.bind>
	
	<#macro button group_index event_index extra_classes="">
		<button type="button" data-target="#group${group_index}-event${event_index}-modal" class="btn ${extra_classes}" data-toggle="modal">
			<#nested/>
		</button>
	</#macro>
	<#macro modal group_index event_index>								
		<div id="group${group_index}-event${event_index}-modal" class="modal hide fade refresh-form" tabindex="-1" role="dialog" aria-labelledby="group${group_index}-event${event_index}-modal-label" aria-hidden="true">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
	    	<h3 id="group${group_index}-event${event_index}-modal-label"><#nested/></h3>
			</div>	
			<div class="modal-body">				
				<@form.labelled_row "tutors" "Tutors">
					<div id="group${group_index}-event${event_index}-tutor-list">
						<@form.userpicker path="tutors" list=true multiple=true spanClass="span5" />
					</div>
					<script>
						jQuery('#group${group_index}-event${event_index}-tutor-list').on('click', function(e){
							e.preventDefault();
							var name = jQuery(this).data('expression');
							var newButton = jQuery('<div><input type="text" class="text" name="'+name+'" /></div>');
							jQuery('#group${group_index}-event${event_index}-tutor-list button').before(newButton);
							return false;
						});
					</script>
				</@form.labelled_row>
				
				<@form.labelled_row path="weeks" label="Terms" fieldCssClass="controls-row">
					<@spring.bind path="weeks">
						<#local allWeeks=status.actualValue />
					</@spring.bind>
				
					<#list allTermWeekRanges as term_week_range>
						<#local weeks = term_week_range.weekRange.toWeeks />
						<#local full = term_week_range.isFull(allWeeks) />
						<#local partial = !full && term_week_range.isPartial(allWeeks) />
						
						<div class="span3">
							<@form.label checkbox=true>
								<input id="group${group_index}-event${event_index}-weeks${term_week_range_index}-checkbox" type="checkbox" value="true" <#if full || partial>checked="checked"</#if> data-indeterminate="<#if partial>true<#else>false</#if>" data-target="#group${group_index}-event${event_index}-weeks${term_week_range_index}">
								Term ${term_week_range_index+1}
							</@form.label>
						
							<@f.select path="weeks" id="group${group_index}-event${event_index}-weeks${term_week_range_index}" size="${weeks?size?c}" multiple="true" cssClass="individual-weeks span9">
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
					<#--
						<button type="button" class="btn btn-mini" data-toggle="elements" data-target=".individual-dates">Use specific first/last dates</button>
					-->
				</@form.labelled_row>
				
				<@form.labelled_row "day" "Day">
					<@f.select path="day" id="group${group_index}-event${event_index}-day">
						<@f.option value="" label=""/>
						<@f.options items=allDays itemLabel="name" itemValue="asInt" />
					</@f.select>
				</@form.labelled_row>
				
				<#-- The time-picker causes the entire page to become a submit button, can't work out why -->
				<@form.labelled_row "startTime" "Start time">
					<@f.input path="startTime" <#--cssClass="time-picker"--> />
				</@form.labelled_row>
				
				<@form.labelled_row "endTime" "End time">
					<@f.input path="endTime" <#--cssClass="time-picker"--> />
				</@form.labelled_row>
				
				<@form.labelled_row "location" "Location">
					<@f.input path="location" />
				</@form.labelled_row>
			</div>
			<div class="modal-footer">
				<button class="btn btn-primary" data-dismiss="modal" aria-hidden="true">Save</button>
			</div>
		</div>
	</#macro>

	<#list groups?chunk(2) as row>
		<div class="row-fluid">
			<#list row as group>
				<#assign groupIndex=(row_index * 2) + group_index />
				<@spring.nestedPath path="groups[${groupIndex}]">
					<@spring.bind path="delete">
						<#assign deleteGroup=status.actualValue />
					</@spring.bind>
				
					<div class="span6 group<#if deleteGroup> deleted</#if>">
						<h4 class="name">
							${group.name!""}
							<small><@fmt.p (group.students.includeUsers?size)!0 "student" "students" /></small>
						</h4>
					
						<ul class="events unstyled">
							<#list group.events as event>
								<@spring.nestedPath path="events[${event_index}]">
									<li>
										<@f.hidden path="delete" id="group${groupIndex}_event${event_index}_delete" />
									
										<#-- TODO display tutors -->
									
										<#-- TODO this should be a formatter, the current formatter expects a full fat event -->
										<#if event.weekRanges?size gt 0 && event.day??>
											<#noescape>${weekRangesFormatter(event.weekRanges, event.day, academicYear, module.department)}</#noescape>,
										<#elseif event.weekRanges?size gt 0>
											[no day of week selected]
										<#else>
											[no dates selected]
										</#if>
										
										${(event.day.shortName)!"[no day selected]"} 
										<#if event.startTime??><@fmt.time event.startTime /><#else>[no start time]</#if> 
										- 
										<#if event.endTime??><@fmt.time event.endTime /><#else>[no end time]</#if>,
										${event.location!"[no location]"}
										
										<#if !deleteGroup>
											<@button groupIndex event_index "btn-mini btn-info">
												Edit
											</@button>
											<@modal groupIndex event_index>
												Edit event for ${group.name!""}
											</@modal>
											
											<button type="button" class="btn btn-danger btn-mini" data-toggle="delete" data-value="true" data-target="#group${groupIndex}_event${event_index}_delete">
												<i class="icon-remove"></i>
											</button>
											<button type="button" class="btn btn-info btn-mini" data-toggle="delete" data-value="false" data-target="#group${groupIndex}_event${event_index}_delete">
												<i class="icon-undo"></i>
											</button>
										</#if>
									</li>
								</@spring.nestedPath>
							</#list>
							
							<#if !deleteGroup>
								<@spring.nestedPath path="events[${group.events?size}]">
									<li>
										<@button groupIndex group.events?size>
											Add event
										</@button>
										
										<@modal groupIndex group.events?size>
											Add event for ${group.name!""}
										</@modal>
									</li>
								</@spring.nestedPath>
							</#if>
						</ul>
					</div>
				</@spring.nestedPath>
			</#list>
		</div>
	</#list>
	
	<script type="text/javascript">
		jQuery(function($) {
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
			
			$('.group button[data-toggle="delete"]').each(function() {
				var $button = $(this);
				var $li = $button.closest('li');
				var $target = $($button.data('target'));
				var value = "" + $button.data('value');
				
				if ($target.val() === value) { 
					$button.hide();
					
					if (value === "true") {
						$li.addClass('deleted');
					} 
				}
				
				$button.on('click', function() {
					$target.val(value);
					
					if (value === "true") {
						$li.addClass('deleted');
					} else {
						$li.removeClass('deleted');
					}
					
					$button.hide();
					$li.find('button[data-toggle="delete"]').filter(function() {
						var $otherButton = $(this);
						var otherValue = "" + $otherButton.data('value');
						
						return otherValue != value && $otherButton.data('target') == $button.data('target');
					}).show();
				});
			});
		});
	</script>
</#escape>