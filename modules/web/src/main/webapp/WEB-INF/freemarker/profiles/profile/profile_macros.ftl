<#import "../related_students/meeting/meeting_list_macros.ftl" as meeting_macros />
<#import "*/modal_macros.ftl" as modal />
<#escape x as x?html>

<#macro address address>
	<div class="vcard">
		<#if address.line1??>
			<p class="address">
				<span class="line1">${address.line1}</span>
				<#if address.line2??><br><span class="line2">${address.line2}</span></#if>
				<#if address.line3??><br><span class="line3">${address.line3}</span></#if>
				<#if address.line4??><br><span class="line4">${address.line4}</span></#if>
				<#if address.line5??><br><span class="line5">${address.line5}</span></#if>
				<#if address.postcode??><br><span class="postcode">${address.postcode}</span></#if>
			</p>
		</#if>
		<#if address.telephone??>
			<p class="tel">${phoneNumberFormatter(address.telephone)}</p>
		</#if>
	</div>
</#macro>

<#macro relationship_section_timeline_row profile relationship>
	<tr>
		<td>
			<@fmt.relation_photo profile relationship "tinythumbnail" />
			${relationship.agentMember.fullName!relationship.relationshipType.agentRole?cap_first}
			<#if relationship.percentage?has_content>
				<span class="percentage muted">(${relationship.percentage}%)</span>
			</#if>
		</td>
		<td>
			<#if relationship.startDate??>
				<@fmt.date date=relationship.startDate includeTime=false />
			<#else>
				<em>Unknown</em>
			</#if>
		</td>
		<td>
			<#if relationship.endDate??>
				<@fmt.date date=relationship.endDate includeTime=false />
			<#else>
				<em>None</em>
			</#if>
		</td>
	</tr>
</#macro>

<#macro relationship_section studentCourseDetails relationshipType>
<section id="relationship-${relationshipType.id}" class="relationship-section clearfix">

	<#if (RequestParameters.relationshipType!) == relationshipType.id>
		<#if RequestParameters.action??>
			<#if RequestParameters.action?? && RequestParameters.action == "agentremoved" || RequestParameters.action == "agentchanged" || RequestParameters.action == "agenterror">
				<#if RequestParameters.action = "agenterror"><#assign alertClass="alert-danger"><#else><#assign alertClass="alert-success"></#if>
				<div id="agentsMessage" class="alert ${alertClass}">
					<button type="button" class="close" data-dismiss="alert">&times;</button>
					<p>
						<#if RequestParameters.action = "agenterror">
							There was a problem when attempting to update this personal tutor. No tutor was specified
						<#elseif RequestParameters.action = "agentremoved">
							<strong>${agent.fullName}</strong> is no longer ${profile.firstName}'s ${relationshipType.agentRole}.
						<#else>
							<strong>${agent.fullName}</strong> is now ${profile.firstName}'s ${relationshipType.agentRole}.
						</#if>
					</p>
				</div>
			</#if>
		</#if>
	</#if>

	<#local acceptsChanges = (studentCourseDetails.sprCode)?? && (studentCourseDetails.department)?? && !relationshipType.readOnly(studentCourseDetails.department) />
	<#local relationships = (studentCourseYearDetails.relationships(relationshipType))![] />
	<#local allRelationships = studentCourseDetails.allRelationshipsOfType(relationshipType) />

	<div id="${relationshipType.urlPart}-timeline" class="modal hide fade">
		<@modal.header>
			<h2>Changes to ${relationshipType.agentRole}</h2>
		</@modal.header>
		<@modal.body>
		<#if !allRelationships?has_content>
			<em>No ${relationshipType.agentRole} details found for this course</em>
		<#else>
			<table class="table table-hover table-condensed sb-no-wrapper-table-popout">
				<thead>
					<tr>
						<th></th>
						<th>Start date</th>
						<th>End date</th>
					</tr>
				</thead>
				<tbody>
					<#list allRelationships as rel>
						<#if rel.current>
							<@relationship_section_timeline_row profile rel />
						</#if>
					</#list>
					<#list allRelationships as rel>
						<#if !rel.current>
							<@relationship_section_timeline_row profile rel />
						</#if>
					</#list>
				</tbody>
			</table>
		</#if>
		</@modal.body>
	</div>

	<#if can.do_with_selector("Profiles.StudentRelationship.Manage", studentCourseDetails, relationshipType)>
		<div class="btn-group pull-right">
			<a class="btn dropdown-toggle" data-toggle="dropdown" href="#">
				Actions&hellip;
				<span class="caret"></span>
			</a>
			<ul class="dropdown-menu">
				<#if acceptsChanges>
					<#if relationships?size gt 0>
						<li>
							<a class="add-agent-link" href="<@routes.profiles.relationship_edit_no_agent scjCode=studentCourseDetails.urlSafeId relationshipType=relationshipType />"
							   data-target="#modal-change-agent"
							   data-scj="${studentCourseDetails.scjCode}"
							>
								Add another ${relationshipType.agentRole}
							</a>
						</li>
					<#else>
						<li>
							<a class="edit-agent-link" href="<@routes.profiles.relationship_edit_no_agent scjCode=studentCourseDetails.urlSafeId relationshipType=relationshipType />"
							   data-target="#modal-change-agent"
							   data-scj="${studentCourseDetails.scjCode}"
							>
								Add a ${relationshipType.agentRole}
							</a>
						</li>
					</#if>
				<#else>
				</#if>
				<li>
					<a href="#" data-target="#${relationshipType.urlPart}-timeline" data-toggle="modal">View timeline</a>
				</li>
			</ul>
		</div>
	<#else>
		<a href="#" class="btn pull-right" data-target="#${relationshipType.urlPart}-timeline" data-toggle="modal">View timeline</a>
	</#if>

	<h4>${relationshipType.agentRole?cap_first}<#if relationships?size gt 1 && !relationshipType.agentRole?ends_with("s")>s</#if></h4>

	<#if relationships?size gt 0>
		<div class="relationships clearfix row-fluid">
		<#list relationships as relationship>
			<div class="agent clearfix span4">
				<#if !relationship.agentMember??>
					${relationship.agentName}
					<#if relationship.percentage?has_content>
						<span class="percentage muted">(${relationship.percentage}%)</span>
					</#if>
					<span class="muted">External to Warwick</span>
					<#if can.do_with_selector("Profiles.StudentRelationship.Manage", studentCourseDetails, relationshipType) && acceptsChanges>
						<a class="edit-agent-link" href="<@routes.profiles.relationship_edit_no_agent scjCode=studentCourseDetails.urlSafeId relationshipType=relationshipType />"
							data-target="#modal-change-agent"
							data-scj="${studentCourseDetails.scjCode}"
						>
							Edit
						</a>
					</#if>
				<#else>
					<#local agent = relationship.agentMember />

					<@fmt.relation_photo profile relationship "tinythumbnail" />

					<h5>
						${agent.fullName!relationshipType.agentRole?cap_first}
						<#if relationship.percentage?has_content>
							<span class="percentage muted">(${relationship.percentage}%)</span>
						</#if>
						<#if can.do_with_selector("Profiles.StudentRelationship.Manage", studentCourseDetails, relationshipType) && acceptsChanges>
							<a class="edit-agent-link" href="<@routes.profiles.relationship_edit scjCode=studentCourseDetails.urlSafeId currentAgent=agent relationshipType=relationshipType />"
								data-target="#modal-change-agent"
								data-scj="${studentCourseDetails.scjCode}"
							>
								Edit
							</a>
						</#if>
					</h5>
					<#if agent.universityId == viewerUser.universityId!>
						<span class="muted">(you)</span>
					<#else>
						<#if agent.email??>
							<p><a href="mailto:${agent.email}">${agent.email}</a> <br/>
								<#local ajaxTargetUrl><@routes.profiles.peoplesearchData agent /></#local>
								<span class="pull-left ajaxPeoplesearchContents">
									<script>
										jQuery(function($) {
											$.getJSON('${ajaxTargetUrl}', function(data) {
												var extension = '';
												var room = '';
												if (data.extensionNumberWithExternal) {
													extension = 'Phone: ' + data.extensionNumberWithExternal + '<br/>'
												}
												if (data.room) {
													room = 'Room: ' + data.room
												}
												$('.ajaxPeoplesearchContents').html(extension + room);
											});
										});
									</script>
    							</span>
							</p>
						</#if>
					</#if>
				</#if>
			</div>
		</#list>
		</div>
	<#else>
		<p class="text-warning"> No ${relationshipType.agentRole} details are recorded in Tabula for the current year.</p>
	</#if>

	<hr />

	<#local ajaxRoute>
		<#if openMeetingId?has_content>
			<@routes.profiles.listMeetingsTargetted relationshipType studentCourseDetails.urlSafeId studentCourseYearDetails.academicYear openMeetingId/>
		<#else>
			<@routes.profiles.listMeetings relationshipType studentCourseDetails.urlSafeId studentCourseYearDetails.academicYear/>
		</#if>
	</#local>
	<#local ajaxTarget>meetings-target-${relationshipType.urlPart}-${studentCourseDetails.urlSafeId}-${studentCourseYearDetails.academicYear.startYear?c}</#local>
	<div id="${ajaxTarget}" class="meetings-target"><i class="fa fa-spinner fa-spin"></i><em> Loading meetings&hellip;</em></div>
	<script>
		jQuery(function($){
			$.get('${ajaxRoute}', function(data){
				$('#${ajaxTarget}').empty().html(data);
			});
		});
	</script>
</section>
</#macro>

<#macro timetable_ical_modal profile>
<div class="modal hide fade" id="timetable-ical-modal">
	<@modal.header>
		<h2>Subscribe to your timetable</h2>
	</@modal.header>
	<@modal.body>
		<#if isSelf>
			<div class="alert alert-info">
				<p>Tabula provides your timetable as a calendar feed with a "private address". Private Addresses are designed for your use only. They don't require any further authentication to get information from your timetable, so they're useful for getting your timetable into another calendar or application, or your mobile phone.</p>
				<p>If you accidentally share the address with others, you can change the address by clicking the button below. All of the existing clients using this private address will break, and you will have to give them the new private address.</p>
				<form class="form-inline double-submit-protection" method="POST" action="<@routes.profiles.timetable_ical_regenerate />">
					<div class="submit-buttons">
						<button type="submit" class="btn btn-primary">Generate a new private address</button>
					</div>
				</form>
			</div>
		</#if>

		<p>You can <a href="<@routes.profiles.timetable_ical profile />">click this link</a> to subscribe to your timetable in your default calendar application.</p>

		<p>You can also copy the link and paste it into an external application, e.g. Google Calendar:</p>

		<p><a href="<@routes.profiles.timetable_ical profile />"><@routes.profiles.timetable_ical profile false /></a></p>
	</@modal.body>
</div>
</#macro>

<#macro timetable_placeholder profile view_name="agendaWeek" show_view_switcher=false renderdate=.now>
	<div
		class='fullCalendar'
		data-viewname='${view_name}'
		data-studentid='${profile.universityId}'
		data-showviewswitcher="${show_view_switcher?string("true", "false")}"
		data-renderdate="${renderdate?long?c}"
	></div>
</#macro>

<#macro timetablePane profile>
	<li id="timetable-pane" data-title="Timetable">
		<section id="timetable-details" class="clearfix" >
			<div class="pull-right">
				<a class="timetable-fullscreen" href="<@routes.profiles.timetable profile />">View full screen</a>
			</div>

			<h4>
				Timetable
				<#if profile.timetableHash?has_content && can.do("Profiles.Read.TimetablePrivateFeed", profile)>
					<a href="<@routes.profiles.timetable_ical profile />" title="Subscribe to timetable">
						Subscribe
					</a>
				</#if>
			</h4>
			<@timetable_placeholder profile />
		</section>
	</li>
	<#if profile.timetableHash?has_content>
		<@timetable_ical_modal profile />
	</#if>
</#macro>


</#escape>