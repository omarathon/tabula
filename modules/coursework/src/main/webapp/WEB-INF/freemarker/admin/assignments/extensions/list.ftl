<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#assign time_remaining=durationFormatter(assignment.closeDate) />

<div>
    <h1>Authorise late submissions for ${assignment.name}</h1>
    <p>
		This assignment <@fmt.tense assignment.closeDate "closes" "closed" /> 
		on <strong><@fmt.date date=assignment.closeDate timezone=true /> (${time_remaining})</strong>.
		To authorise an extension for a student click the "Grant" button next to the student's University ID. If you
		wish to grant an extension to a user that is not listed below, please ensure that they appear in the students
		list on the <a href="<@url page="/admin/module/${module.code}/assignments/${assignment.id}/edit" />">assignment edit page</a>.
    </p>
    <div class="alert alert-info">
    	<i class="icon-envelope"></i> Students will automatically be notified by email when you grant, modify or revoke an extension.
    </div>
    <div id="extension-list">
        <table class="extensionListTable table table-striped table-bordered">
            <tr class="extension-header">
				<th>Name</th>
				<th>Status</th>
				<th>New deadline</th>
				<th><!-- Actions column--></th>
			</tr>

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
						<td>${studentNameLookup[extension.universityId]}</td>
						<td class="status">
							<#if extension.approved>
								<span class="label label-success">Approved</span>
							<#elseif extension.rejected>
								<span class="label label-important">Rejected</span>
							<#else>
								<span class="label label-info">Awaiting approval</span><br/>
								Requested <@fmt.date date=extension.requestedOn at=true/>
							</#if>
						</td>
						<td class="expiryDate"><#if extension.expiryDate??> <@fmt.date date=extension.expiryDate at=true/></#if></td>
						<td><#if isExtensionManager>
							<a class="review-extension btn btn-mini btn-primary" href="<@routes.extensionreviewrequest assignment=assignment uniId=extension.universityId />" data-toggle="modal" data-target="#extension-model">
								<i class="icon-edit icon-white"></i> Review request
							</a>
						</#if></td>
					</tr>
				</#list>
			</#if>

            <!-- list existing extensions -->
			<#if existingExtensions??>
				<#list existingExtensions as extension>
					<tr id="row${extension.universityId}" class="extension-row">
						<td>${studentNameLookup[extension.universityId]}</td>
						<td class="status">
							<#if extension.approved>
								<span class="label label-success">Approved</span>
							<#elseif extension.rejected>
								<span class="label label-important">Rejected</span>
							</#if>
						</td>
						<td class="expiryDate"><#if extension.expiryDate??> <@fmt.date date=extension.expiryDate at=true/></#if></td>
						<td>
							<a class="hide new-extension btn btn-mini btn-success" href="<@routes.extensionadd assignment=assignment uniId=extension.universityId />" data-toggle="modal" data-target="#extension-model">
								<i class="icon-ok icon-white"></i> Grant
							</a>
							<a class="modify-extension btn btn-mini btn-primary" href="<@routes.extensionedit assignment=assignment uniId=extension.universityId />" data-toggle="modal" data-target="#extension-model">
								<i class="icon-edit icon-white"></i> Modify
							</a>
							&nbsp;
							<a class="revoke-extension btn btn-mini btn-danger" href="<@routes.extensiondelete assignment=assignment uniId=extension.universityId />" data-toggle="modal" data-target="#extension-model">
								<i class="icon-remove icon-white"></i> Revoke
							</a>
						</td>
					</tr>
				</#list>
			</#if>
			<#if potentialExtensions??>
				<#list potentialExtensions as universityId>
					<tr id="row${universityId}" class="extension-row">
						<td>${studentNameLookup[universityId]}</td>
						<td  class="status"></td>
						<td class="expiryDate"></td>
						<td>
							<a class="new-extension btn btn-mini btn-success" href="<@routes.extensionadd assignment=assignment uniId=universityId />" data-toggle="modal" data-target="#extension-model">
								<i class="icon-ok icon-white"></i> Grant
							</a>
							<a class="hide modify-extension btn btn-mini btn-primary" href="<@routes.extensionedit assignment=assignment uniId=universityId />" data-toggle="modal" data-target="#extension-model">
								<i class="icon-edit icon-white"></i> Modify
							</a>
							&nbsp;
							<a class="hide revoke-extension btn btn-mini btn-danger" href="<@routes.extensiondelete assignment=assignment uniId=universityId />" data-toggle="modal" data-target="#extension-model">
								<i class="icon-remove icon-white"></i> Revoke
							</a>
						</td>
					</tr>
				</#list>
			</#if>
		</table>
		<div id="extension-model" class="modal fade"></div>
		<script type="text/javascript">
			jQuery(function($){

				var highlightId = "${highlightId}";
				if (highlightId != "") {
					var container = $("#extension-list");
					var highlightRow = $("#row"+highlightId);
					if(highlightRow.length > 0){
						container.animate({
							scrollTop: highlightRow.offset().top - container.offset().top + container.scrollTop()
						}, 1000);
					}
				}

				// modals use ajax to retrieve their contents
				$('#extension-list').on('click', 'a[data-toggle=modal]', function(e){
					e.preventDefault();
					$this = $(this);
					target = $this.attr('data-target');
					url = $this.attr('href');
					$(target).load(url);
				});

				// any date fields returned by ajax will have datetime pickers bound to them as required
				$('#extension-list').on('focus', 'input.date-time-picker', function(e){
					e.preventDefault();
					var isPickerHidden = (typeof $('.datetimepicker').filter(':visible')[0] === "undefined") ? true : false;
					
					if(isPickerHidden) {
						$(this).datetimepicker('remove').datetimepicker({
							format: "dd-M-yyyy hh:ii:ss",
							weekStart: 1,
							minView: 'day',
							autoclose: true
						}).on('show', function(ev){
							var d = new Date(ev.date.valueOf()),
								  minutes = d.getUTCMinutes(),
									seconds = d.getUTCSeconds(),
									millis = d.getUTCMilliseconds();
									
							if (minutes > 0 || seconds > 0 || millis > 0) {
								d.setUTCMinutes(0);
								d.setUTCSeconds(0);
								d.setUTCMilliseconds(0);
								
								var DPGlobal = $.fn.datetimepicker.DPGlobal;
								$(this).val(DPGlobal.formatDate(d, DPGlobal.parseFormat("dd-M-yyyy hh:ii:ss", "standard"), "en", "standard"));
								
								$(this).datetimepicker('update');
							}
						});
					}
				});			

				$('#extension-list').on('submit', 'form', function(e){
					e.preventDefault();
					var $form = $(this);
					$.post($form.attr('action'), $form.serialize(), function(data){
						if(data.status == "error"){
							// delete any old errors
							$("span.error").remove();
							$('.error').removeClass('error');
							for(error in data.result){
								addError(error, data.result[error]);
							}
						} else {
							var action = data.action;
							$.each(data.result, function(){
								modifyRow(this, action);
							});
							// hide the model
							jQuery("#extension-model").modal('hide');
						}
					});
				});

				// set reject and approved flags
				jQuery("#extension-model").on('click', '#approveButton', function(){
					jQuery(".approveField").val("1");
					jQuery(".rejectField").val("0");
				});

				jQuery("#extension-model").on('click', '#rejectButton', function(){
					jQuery(".approveField").val("0");
					jQuery(".rejectField").val("1");
				});

				var addError = function(field, message){
					var $field = $("input[name='"+field+"']");
					$field.closest(".control-group").addClass("error");
					// insert error message
					$field.after('<span class="error help-inline">'+message+'</span>');
				};

				var modifyRow = function(results, action){
					$row =  $("#extension-list").find("#row"+results.id);
					if(action === "add"){
						updateRowUI(results);
						$(".new-extension", $row).hide();
						$(".modify-extension", $row).show();
						$(".revoke-extension", $row).show();
					} else if (action === "edit"){
						updateRowUI(results);
					} else if (action === "delete"){
						$("td.expiryDate", $row).html("");
						$("td.status", $row).html("");
						$(".new-extension", $row).show();
						$(".modify-extension", $row).hide();
						$(".revoke-extension", $row).hide();
					}
				};

				var updateRowUI = function(json){
					for(prop in json){
						$row.find('.'+prop).html(json[prop]);
					}
					$('.status').each(function(){
						if ($(this).html() === "Approved")
							$(this).html('<span class="label label-success">Approved</span>');
						else if ($(this).html() === "Rejected")
							$(this).html('<span class="label label-important">Rejected</span>');
					});
				};
			});
		</script>
    </div>
</div>

</#escape>