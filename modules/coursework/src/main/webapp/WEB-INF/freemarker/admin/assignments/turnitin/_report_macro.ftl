<#macro originalityReport attachment>
	<#local r=attachment.originalityReport />
	<span id="tool-tip-${attachment.id}" class="similarity-${r.similarity} similarity-tooltip">${r.overlap}% similarity</span>
	<div id="tip-content-${attachment.id}" class="hide">
		<p>${attachment.name} <img src="<@url resource="/static/images/icons/turnitin-16.png"/>"></p>
		<p class="similarity-subcategories-tooltip">
			Web: ${r.webOverlap}%<br>
			Student papers: ${r.studentOverlap}%<br>
			Publications: ${r.publicationOverlap}%
		</p>
		<p>

			<#if !features.turnitinLTI && !r.turnitinId?has_content>
				<a target="turnitin-viewer" href="<@url page='/coursework/admin/module/${assignment.module.code}/assignments/${assignment.id}/turnitin-report/${attachment.id}'/>">View full report</a>
			<#elseif features.turnitinLTI && r.turnitinId?has_content><a target="turnitin-viewer" href="<@url page='/coursework/admin/module/${assignment.module.code}/assignments/${assignment.id}/turnitin-lti-report/${attachment.id}'/>">View full report</a>
			<#else>This report is no longer available in Tabula. If you need access to the full report please contact the <a href="mailto:webteam@warwick.ac.uk">ITS Web Team</a>
			</#if>

		</p>
	</div>
	<script type="text/javascript">
		jQuery(function($){
			$("#tool-tip-${attachment.id}").popover({
				placement: 'right',
				html: true,
				content: function(){return $('#tip-content-${attachment.id}').html();},
				title: 'Turnitin report summary'
			});
		});
	</script>
</#macro>