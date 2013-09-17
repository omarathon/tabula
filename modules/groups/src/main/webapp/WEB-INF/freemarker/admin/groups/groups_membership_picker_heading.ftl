<#escape x as x?html>

		<#list command.upstreamGroups as item>
			<@f.hidden path="upstreamGroups[${item_index}]" cssClass="upstreamGroups" />
		</#list>

		<@spring.bind path="members">
			<#assign membersGroup=status.actualValue />
		</@spring.bind>

		<#assign includeIcon><span class="use-tooltip" title="Added manually" data-placement="right"><i class="icon-hand-up"></i></span><span class="hide">Added</span></#assign>
		<#assign pendingDeletionIcon><span class="use-tooltip" title="Deleted manual addition" data-placement="right"><i class="icon-remove"></i></span><span class="hide">Pending deletion</span></#assign>
		<#assign excludeIcon><span class="use-tooltip" title="Removed manually, overriding SITS" data-placement="right"><i class="icon-ban-circle"></i></span><span class="hide">Removed</span></#assign>
		<#assign sitsIcon><span class="use-tooltip" title="Automatically linked from SITS" data-placement="right"><i class="icon-list-alt"></i></span><span class="hide">SITS</span></#assign>

		<#assign membershipInfo = command.membershipInfo />
		<#assign hasMembers = membershipInfo.totalCount gt 0 />

		<#macro what_is_this>
			<#local popoverText>
				<p>You can link to one or more assessment components in SITS and the list of students will be updated automatically from there.
				If you are not using SITS you can manually add students by ITS usercode or university number.</p>

				<p>It is also possible to tweak the list even when using SITS data, but this is only to be used
				when necessary and you still need to ensure that the upstream SITS data gets fixed.</p>
			</#local>

			<a href="#"
			   title="What's this?"
			   class="use-popover"
			   data-title="Students"
			   data-trigger="hover"
	   		   data-html="true"
			   data-content="${popoverText}"
			   ><i class="icon-question-sign"></i></a>
		</#macro>

				<#-- enumerate current state -->
				<p>
				<#if linkedUpstreamAssessmentGroups?has_content>

					<span class="uneditable-value enrolledCount">
						${membershipInfo.totalCount} enrolled
						<#if membershipInfo.excludeCount gt 0 || membershipInfo.includeCount gt 0>
							<span class="muted">(${membershipInfo.sitsCount} from SITS<#if membershipInfo.usedExcludeCount gt 0> after ${membershipInfo.usedExcludeCount} removed manually</#if><#if membershipInfo.usedIncludeCount gt 0>, plus ${membershipInfo.usedIncludeCount} added manually</#if>)</span>
						<#else>
							<span class="muted">from SITS</span>
						</#if>
					<@what_is_this /></span>
				<#elseif hasMembers>
					<span class="uneditable-value enrolledCount">${membershipInfo.includeCount} manually enrolled
					<@what_is_this /></span>
				<#else>
					<span class="uneditable-value enrolledCount">No students enrolled
					<@what_is_this /></span>

				</#if>
				</p>


</#escape>