<#escape x as x?html>
	<@spring.bind path="academicYear">
		<#assign academicYear=status.actualValue />
	</@spring.bind>
	
	<#macro button group_index event_index extra_classes="">
		<button type="button" data-target="#group${group_index}-event${event_index}-modal" class="btn ${extra_classes}" data-toggle="modal">
			<#nested/>
		</button>
	</#macro>


	<div class="striped-section-contents">
		<#list groups as group>
			<@spring.nestedPath path="groups[${group_index}]">
				<@spring.bind path="delete">
					<#assign deleteGroup=status.actualValue />
				</@spring.bind>
			
				<div class="item-info<#if deleteGroup> deleted</#if>">
					<div class="row-fluid">
						<div class="span10 groupDetail">
							<h3 class="name inline-block">
								${group.name!""}
								<#if !newRecord>
									<small><@fmt.p (group.group.students.includeUsers?size)!0 "student" "students" /></small>
								</#if>
							</h3>
							<#assign unlimited = !((smallGroupSet.defaultMaxGroupSizeEnabled)!false) />

							<span class="groupSizeUnlimited groupSizeDetails" <#if !unlimited>style="display:none;"</#if>>
								Unlimited group size
							</span>
							<span class="groupSizeLimited groupSizeDetails" <#if unlimited>style="display:none;"</#if>>
								Maximum group size: <@f.input path="maxGroupSize" type="number" min="0" cssClass="input-small" />
							</span>
						</div>
						<div class="span2">
							<#if !deleteGroup>
								<@spring.nestedPath path="events[${group.events?size}]">
									<@button group_index=group_index event_index=group.events?size extra_classes="pull-right">
										Add event
									</@button>
								</@spring.nestedPath>
							<#else>
								<div><span class="label label-important pull-right" data-toggle="tooltip" title="This group, any events and any allocation will be deleted when you click Save">Marked for deletion</span></div>
							</#if>
						</div>
					</div>

					<div class="row-fluid">
						<div class="span12">
							<ul class="events unstyled">
								<#list group.events as event>
									<#if !((event.isEmpty())!false) || event_has_next>
										<@spring.nestedPath path="events[${event_index}]">
											<li>
												<@f.hidden path="delete" id="group${group_index}_event${event_index}_delete" />
											
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
												<#if event.endTime??><@fmt.time event.endTime /><#else>[no end time]</#if><#if event.location?has_content>,</#if>
												${event.location!"[no location]"}
												
												<#if !deleteGroup>
													<div class="buttons pull-right">
														<@button group_index event_index "btn-mini btn-info">
															Edit
														</@button>
													
														<button type="button" class="btn btn-danger btn-mini" data-toggle="delete" data-value="true" data-target="#group${group_index}_event${event_index}_delete">
															<i class="icon-remove"></i>
														</button>
														<button type="button" class="btn btn-info btn-mini" data-toggle="delete" data-value="false" data-target="#group${group_index}_event${event_index}_delete">
															<i class="icon-undo"></i>
														</button>
													</div>
												</#if>
											</li>
										</@spring.nestedPath>
									</#if>
								</#list>
							</ul>
						</div>
					</div>
				</div>
			</@spring.nestedPath>
		</#list>
	</div>
	
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
			
			$('.events button[data-toggle="delete"]').each(function() {
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
	
	<style type="text/css">
		<#-- Hide the confusing dates in the header of the time picker -->
		.datetimepicker-hours thead i { display: none !important; }
		.datetimepicker-hours thead .switch { visibility: hidden; }
		.datetimepicker-hours thead th { height: 0px; }
		.datetimepicker-minutes thead .switch { visibility: hidden; }
		
		.item-info .events li { line-height: 30px; padding: 0 3px; }
		.item-info .events li button { margin-top: 0; }
		.item-info .events li:hover { background: #dddddd; }
	</style>
</#escape>
