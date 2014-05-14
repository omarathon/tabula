<script>
	var weeks = ${weekRangesDumper()}
</script>

<#import "../related_students/related_students_macros.ftl" as relationships />

<div class="tabbable">

	<ol class="panes">
		<li id="sg-pane" style="display:none;" data-title="Groups">
			<section id="small-groups" class="clearfix" >
				<script type="text/javascript">
					jQuery(function($){
						$('#small-groups').load('/groups/tutor', {ts: new Date().getTime()}, function() {
							var pane = $('#sg-pane');
							var title = pane.find('h4').html();
							if (title != '' && title != undefined) {
								pane.find('.title').html(title);
								$('a.ajax-modal', '#small-groups').ajaxModalLink();
								$('#sg-pane').show();
							}
						});
					});
				</script>
			</section>
		</li>

		<li id="timetable-pane" data-title="Timetable">
			<section id="timetable-details" class="clearfix" >
				<h4>Timetable</h4>
				<div class='fullCalendar' data-viewname='agendaWeek' data-studentid='${profile.universityId}'></div>
			</section>
		</li>

		<#if  (viewerRelationshipTypes?size > 0)>
			<li id="relationships-pane" data-title="My Students">
				<section id="relationship-details" class="clearfix" >
					<h4>My students</h4>
					<ul>
						<#list viewerRelationshipTypes as relationshipType>
							<li><h5><a id="relationship-${relationshipType.urlPart}" href="<@routes.relationship_students relationshipType />">${relationshipType.studentRole?cap_first}s</a></h5></li>
						</#list>
						<#list smallGroups as smallGroup>
							<#assign _groupSet=smallGroup.groupSet />
							<#assign _module=smallGroup.groupSet.module />
							<li><a href="<@routes.smallgroup smallGroup />">
							${_module.code?upper_case} (${_module.name}) ${_groupSet.name}, ${smallGroup.name}
							</a></li>
						</#list>
					</ul>
				</section>
			</li>
		</#if>
	</ol>

</div>