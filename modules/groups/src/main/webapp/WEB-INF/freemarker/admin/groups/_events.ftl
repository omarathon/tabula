<#escape x as x?html>
	<@spring.bind path="academicYear">
		<#assign academicYear=status.actualValue />
	</@spring.bind>
	
	<#macro button group_index event_index extra_classes="">
		<button type="button" data-target="#group${group_index}-event${event_index}-modal" class="btn ${extra_classes}" data-toggle="modal">
			<#nested/>
		</button>
	</#macro>
	<#macro modal group group_index event_index>								
		<div id="group${group_index}-event${event_index}-modal" class="modal hide fade refresh-form" tabindex="-1" role="dialog" aria-labelledby="group${group_index}-event${event_index}-modal-label" aria-hidden="true">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
	    	<h3 id="group${group_index}-event${event_index}-modal-label"><#nested/></h3>
			</div>	
			<div class="modal-body">
				<div class="control-group">
					<label class="control-label">Group name</label>
					<div class="controls">
						<span class="uneditable-input">${group.name!""}</span>
					</div>
				</div>
				
				<@form.labelled_row "tutors" "Tutors">
					<div id="group${group_index}-event${event_index}-tutor-list">
						<@form.userpicker path="tutors" list=true multiple=true />
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
				
				<#-- TODO Terms -->
				
				<@form.labelled_row "day" "Day">
					<@f.select path="day" id="day">
						<@f.options items=allDays itemLabel="name" itemValue="asInt" />
					</@f.select>
				</@form.labelled_row>
				
				<@form.labelled_row "startTime" "Start time">
					<@f.input path="startTime" cssClass="time-picker" />
				</@form.labelled_row>
				
				<@form.labelled_row "endTime" "End time">
					<@f.input path="endTime" cssClass="time-picker" />
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
					<div class="span6 group">
						<h4 class="name">
							${group.name!""}
							<small><@fmt.p (group.students.includeUsers?size)!0 "student" "students" /></small>
						</h4>
					
						<ul class="events unstyled">
							<#list group.events as event>
								<@spring.nestedPath path="events[${event_index}]">
									<li>
										<#-- TODO display tutors -->
									
										<#-- TODO this should be a formatter, the current formatter expects a full fat event -->
										<#noescape>${weekRangesFormatter(event.weekRanges, event.day, academicYear, module.department)}</#noescape>,
										
										${event.day.shortName} <@fmt.time event.startTime /> - <@fmt.time event.endTime />,
										${event.location}
										
										<@button groupIndex event_index "btn-mini btn-info">
											Edit
										</@button>
										<@modal group groupIndex event_index>
											Edit event
										</@modal>
									</li>
								</@spring.nestedPath>
							</#list>
							
							<@spring.nestedPath path="events[${group.events?size}]">
								<li>
									<@button groupIndex group.events?size>
										Add event
									</@button>
									
									<@modal group groupIndex group.events?size>
										Add event
									</@modal>
								</li>
							</@spring.nestedPath>
						</ul>
					</div>
				</@spring.nestedPath>
			</#list>
		</div>
	</#list>
</#escape>