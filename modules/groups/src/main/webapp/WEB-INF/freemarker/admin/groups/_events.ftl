<#escape x as x?html>
	<#import "*/group_components.ftl" as components />
	<#assign academicYear=smallGroupSet.academicYear />

	<div class="striped-section-contents">
		<#list groups as group>
			<@spring.nestedPath path="groups[${group.id}]">
				<div class="item-info">
					<div class="row-fluid">
						<div class="span10 groupDetail">
							<h3 class="name inline-block">
								${group.name!""}
								<small><@fmt.p (group.students.size)!0 "student" "students" /></small>
							</h3>
						</div>
						<div class="span2">
							<#if is_edit>
								<#assign addEventUrl><@routes.editseteventsnewevent group /></#assign>
							<#else>
								<#assign addEventUrl><@routes.createseteventsnewevent group /></#assign>
							</#if>
							<a class="btn pull-right" href="${addEventUrl}">Add event</a>
						</div>
					</div>

					<div class="row-fluid">
						<div class="span12">
							<ul class="events unstyled">
								<#list mapGet(command.groups, group).events as event>
									<@spring.nestedPath path="events[${event_index}]">
										<li>
											<@f.hidden path="delete" id="group${group_index}_event${event_index}_delete" />

											<@components.eventShortDetails event.event />

											<#assign popoverContent><@components.eventDetails event.event /></#assign>
											<a class="use-popover"
											   data-html="true"
											   data-content="${popoverContent}"><i class="icon-question-sign"></i></a>

											<div class="buttons pull-right">
												<#if is_edit>
													<#assign editEventUrl><@routes.editseteventseditevent event.event /></#assign>
												<#else>
													<#assign editEventUrl><@routes.createseteventseditevent event.event /></#assign>
												</#if>

												<a class="btn btn-mini btn-info" href="${editEventUrl}">Edit</a>

												<button type="button" class="btn btn-danger btn-mini" data-toggle="delete" data-value="true" data-target="#group${group_index}_event${event_index}_delete">
													<i class="icon-remove"></i>
												</button>
												<button type="button" class="btn btn-info btn-mini" data-toggle="delete" data-value="false" data-target="#group${group_index}_event${event_index}_delete">
													<i class="icon-undo"></i>
												</button>
											</div>
										</li>
									</@spring.nestedPath>
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
		.item-info .events li { line-height: 30px; padding: 0 3px; }
		.item-info .events li button { margin-top: 0; }
		.item-info .events li:hover { background: #dddddd; }
	</style>
</#escape>
