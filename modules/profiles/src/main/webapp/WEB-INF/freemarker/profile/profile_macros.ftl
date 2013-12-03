<#import "../related_students/meeting/meeting_list_macros.ftl" as meeting_macros />
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

<#macro relationship_section studentCourseDetails relationshipType meetings>
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

	<#if ((studentCourseDetails.relationships(relationshipType))![])?size gt 0>
		<#local relationships = studentCourseDetails.relationships(relationshipType) />

		<h4>${relationshipType.agentRole?cap_first}<#if relationships?size gt 1>s</#if></h4>

		<#if relationships?size gt 0 && can.do_with_selector("Profiles.StudentRelationship.Create", profile, relationshipType) && acceptsChanges>
			<a class="add-agent-link" href="<@routes.relationship_edit_no_agent scjCode=studentCourseDetails.urlSafeId relationshipType=relationshipType />"
				data-target="#modal-change-agent"
				data-scj="${studentCourseDetails.scjCode}"
			>
			<i class="icon-plus"></i> Add another ${relationshipType.agentRole}
			</a>
		</#if>

		<div class="relationships clearfix row-fluid">
		<#list relationships as relationship>

			<div class="agent clearfix span4">
				<#if !relationship.agentMember??>
					${relationship.agentName} <span class="muted">External to Warwick</span>
					<#if can.do_with_selector("Profiles.StudentRelationship.Update", profile, relationshipType) && acceptsChanges>
						<a class="edit-agent-link" href="<@routes.relationship_edit_no_agent scjCode=studentCourseDetails.urlSafeId relationshipType=relationshipType />"
						data-target="#modal-change-agent"
						data-scj="${studentCourseDetails.scjCode}"
						>
						<i class="icon-edit"></i>
						</a>
					</#if>
				<#else>
					<#local agent = relationship.agentMember />

					<@fmt.relation_photo profile relationship "tinythumbnail" />

					<h5>
						${agent.fullName!relationshipType.agentRole?cap_first}
						<#if can.do_with_selector("Profiles.StudentRelationship.Update", profile, relationshipType) && acceptsChanges>
							<a class="edit-agent-link" href="<@routes.relationship_edit scjCode=studentCourseDetails.urlSafeId currentAgent=agent relationshipType=relationshipType />"
							data-target="#modal-change-agent"
							data-scj="${studentCourseDetails.scjCode}"
							>
							<i class="icon-edit"></i>
							</a>
						</#if>
					</h5>
					<#if agent.universityId == viewer.universityId>
						<span class="muted">(you)</span>
					<#else>
						<#if agent.email??>
							<p><i class="icon-envelope-alt"></i> <a href="mailto:${agent.email}">${agent.email}</a></p>
						</#if>
					</#if>
				</#if>
			</div>
		</#list>
		</div>
	<#else>
		<h4>${relationshipType.agentRole?cap_first}</h4>
		<p class="text-warning"><i class="icon-warning-sign"></i> No ${relationshipType.agentRole} details are recorded in Tabula for the current year.</p>

		<#if can.do_with_selector("Profiles.StudentRelationship.Update", profile, relationshipType) && acceptsChanges>
			<a class="btn edit-agent-link" href="<@routes.relationship_edit_no_agent scjCode=studentCourseDetails.urlSafeId relationshipType=relationshipType />"
					data-target="#modal-change-agent"
					data-scj="${studentCourseDetails.scjCode}"
				>
				<i class="icon-plus"></i> Add a ${relationshipType.agentRole}
			</a>
		</#if>
	</#if>

	<@meeting_macros.list studentCourseDetails meetings relationshipType />
</section>
</#macro>

</#escape>