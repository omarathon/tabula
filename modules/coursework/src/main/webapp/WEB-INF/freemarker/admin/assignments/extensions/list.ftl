<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#assign time_remaining=durationFormatter(assignment.closeDate) />
<#assign deptName=(module.department.name)!"this deparment" />

<div>
    <h1>Grant extensions</h1>
	<h5><span class="muted">for</span> ${assignment.name}</h5>
    <p>
		This assignment <@fmt.tense assignment.closeDate "closes" "closed" />
		on <strong><@fmt.date date=assignment.closeDate at=true timezone=true /> (${time_remaining})</strong>.
    </p>
    <div class="alert alert-info">
    	<i class="icon-envelope-alt"></i> Students will automatically be notified by email when you grant, modify or revoke an extension.
    </div>
    <div id="extension-list">
        <table class="extensionListTable table table-striped table-bordered tabula-greenLight tablesorter">
            <thead>
                <tr class="extension-header">
                    <th>First name</th>
                    <th>Last name</th>
                    <th>New deadline</th>
					<th>Feedback deadline</th>
                    <th>Extension status</th>
                    <th>
	                    Actions
	                    <a class="use-popover" id="popover-actions-help" data-html="true" data-container="body"
						   data-placement="bottom"
	                       data-original-title="Granting extensions"
	                       data-content="<p>To grant an extension for a student, click on the 'Grant' button next to the student's name.</p>
	                                     <p>If you wish to grant an extension to a student that is not listed below, please ensure that they appear in the students lists in
	                                     <a href='../edit'>assignment properties</a></p>
	                                     <p>Once an extension is granted you can change it using the 'Modify' button or withdraw it using the 'Revoke' button.</p>
	                                     <p>If a student has requested an extension, you can click on the 'Review' button to grant or reject.</p>
	                                    ">
		                   <i class="icon-question-sign"></i></a>
                    </th>
                </tr>
            </thead>

			<tbody>
				<#if RequestParameters.highlight??>
					<#assign highlightId = RequestParameters.highlight>
				<#else>
					<#assign highlightId = "">
				</#if>

				<!-- list extension requests -->
				<#if extensionRequests??>
					<#list extensionRequests as extension>
						<#if (highlightId == extension.universityId)>
							<#assign highlightClass = "highlight">
						<#else>
							<#assign highlightClass = "">
						</#if>
						<tr id="row${extension.universityId}" class="extension-row ${highlightClass}">
							<#assign student = studentNameLookup[extension.universityId]>
							<td><h6>${student.firstName}</h6></td>
							<td><h6>${student.lastName}</h6></td>
							<td class="expiryDate">
								<#if extension.expiryDate??>
									<@fmt.date date=extension.expiryDate capitalise=true shortMonth=true />
								<#else>
									<span class="muted">no change</span>
								</#if>
							</td>
							<td>
								<#if extension.expiryDate??>
									<@fmt.date date=extension.feedbackDeadline capitalise=true shortMonth=true />
								<#else>
									<span class="muted">no change</span>
								</#if>
							</td>
							<td class="status">
								<#if extension.approved>
									Granted
								<#elseif extension.rejected>
									Rejected
								<#else>
									<span class="requestedExtension">Requested <@fmt.date date=extension.requestedOn at=true /></span>
								</#if>
							</td>
							<td>
								<#if isExtensionManager>
									<#assign review_url><@routes.extensionreviewrequest assignment=assignment uniId=extension.universityId /></#assign>

									<@fmt.permission_button
										permission='Extension.ReviewRequest'
										scope=extension
										action_descr='review an extension'
										classes='review-extension btn btn-mini btn-primary'
										data_attr='data-toggle=modal data-target=#extension-model'
										href=review_url>
										<i class="icon-edit icon-white"></i> Review
									</@fmt.permission_button>
								<#else>
									<i class="icon-ban-circle use-tooltip error" title="You are not an extension manager for ${deptName}"></i>
								</#if>
							</td>
						</tr>
					</#list>
				</#if>

				<!-- list existing extensions -->
				<#if existingExtensions??>
					<#list existingExtensions as extension>
						<tr id="row${extension.universityId}" class="extension-row">
							<#assign student = studentNameLookup[extension.universityId]>
							<td><h6>${student.firstName}</h6></td>
							<td><h6>${student.lastName}</h6></td>
							<td class="expiryDate">
								<#if extension.expiryDate??>
									<@fmt.date date=extension.expiryDate capitalise=true shortMonth=true />
								<#else>
									<span class="muted">no change</span>
								</#if>
							</td>
							<td>
								<#if extension.expiryDate??>
									<@fmt.date date=extension.feedbackDeadline capitalise=true shortMonth=true />
								<#else>
									<span class="muted">no change</span>
								</#if>
							</td>
							<td class="status">
								<#if extension.approved>
									Granted
								<#elseif extension.rejected>
									Rejected
								</#if>
							</td>
							<td>
								<#assign modify_url><@routes.extensionedit assignment=assignment uniId=extension.universityId /></#assign>
								<#assign revoke_url><@routes.extensiondelete assignment=assignment uniId=extension.universityId /></#assign>

								<@fmt.permission_button
									permission='Extension.Update'
									scope=extension
									action_descr='modify an extension'
									classes='modify-extension btn btn-mini btn-primary'
									data_attr='data-toggle=modal data-target=#extension-model'
									href=modify_url>
									<i class="icon-edit icon-white"></i> Modify
								</@fmt.permission_button>

								<@fmt.permission_button
									permission='Extension.Delete'
									scope=extension
									action_descr='revoke an extension'
									classes='revoke-extension btn btn-mini btn-danger'
									data_attr='data-toggle=modal data-target=#extension-model'
									href=revoke_url>
									<i class="icon-remove icon-white"></i> Revoke
								</@fmt.permission_button>
							</td>
						</tr>
					</#list>
				</#if>
				<#if potentialExtensions??>
					<#list potentialExtensions as universityId>
						<tr id="row${universityId}" class="extension-row">
							<#assign student = studentNameLookup[universityId]>
							<td><h6>${student.firstName}</h6></td>
							<td><h6>${student.lastName}</h6></td>
							<td class="expiryDate"></td>
							<td></td>
							<td class="status"></td>
							<td>
								<#assign grant_url><@routes.extensionadd assignment=assignment uniId=universityId /></#assign>

								<@fmt.permission_button
									permission='Extension.Create'
									scope=assignment
									action_descr='grant an extension'
									classes='new-extension btn btn-mini btn-success'
									data_attr='data-toggle=modal data-target=#extension-model'
									href=grant_url>
									<i class="icon-ok icon-white"></i> Grant
								</@fmt.permission_button>
							</td>
						</tr>
					</#list>
				</#if>
	        </tbody>
		</table>
		<div id="extension-model" class="modal fade"></div>
    </div>
</div>

<script type="text/javascript">

	(function($) {

	// FIXME 'ajaxComplete' is a global event, this will fire on _any_ AJAX completion.
	// FIXME should be handled in our shared modal code.
	$('#extension-model').on('ajaxComplete', function(){
		$(this).find('details').details();
	})

	$('.extensionListTable').tablesorter({
		sortList: [[1,0]],
		headers: { 4: { sorter: false } },
		textExtraction: function(node) {
			var $el = $(node);

			return $el.text().trim();
		}
		});
	})(jQuery);

</script>

</#escape>