<#assign spring=JspTaglibs["/WEB-INF/tld/spring.tld"]>
<#assign f=JspTaglibs["/WEB-INF/tld/spring-form.tld"]>
<#escape x as x?html>

<#assign time_remaining=durationFormatter(assignment.closeDate) />

<div>
    <h1>Authorise late submissions for ${assignment.name}</h1>
    <p>
		This assignment closes on <strong><@fmt.date date=assignment.closeDate timezone=true /> (${time_remaining})</strong>.
		To authorise an extension for a student click the "Authorise" button next to the students university ID. If you
		wish to grant an extension to a user that is not listed below, please ensure that they appear in the students
		list on the <a href="/admin/module/${module.code}/assignments/${assignment.id}/edit">assignment edit page</a>.
    </p><br/>
    <div id="extension-list">
        <table class="extensionListTable table table-striped table-bordered">
            <tr class="extension-header"><th>University ID</th><th>New submission date</th><th></th></tr>
            <!-- list existing extensions -->
			<#if existingExtensions??>
				<#list existingExtensions as extension>
					<tr id="row${extension.universityId}" class="extension-row">
						<td>${extension.universityId}</td>
						<td class="expiryDate"><@fmt.date date=extension.expiryDate at=true/></td>
						<td>
							<a class="hide new-extension btn btn-success" href="add?universityId=${extension.universityId}" data-toggle="modal" data-target="#extension-model">
								<i class="icon-ok icon-white"></i> Authorise
							</a>
							<a class="modify-extension btn btn-primary" href="edit/${extension.universityId}" data-toggle="modal" data-target="#extension-model">
								<i class="icon-edit icon-white"></i> Modify
							</a>
							<a class="revoke-extension btn btn-danger" href="delete/${extension.universityId}" data-toggle="modal" data-target="#extension-model">
								<i class="icon-remove icon-white"></i> Deauthorise
							</a>
						</td>
					</tr>
				</#list>
			</#if>
			<#if potentialExtensions??>
				<#list potentialExtensions as universityId>
					<tr id="row${universityId}" class="extension-row">
						<td>${universityId}</td>
						<td class="expiryDate"></td>
						<td>
							<a class="new-extension btn btn-success" href="add?universityId=${universityId}" data-toggle="modal" data-target="#extension-model">
								<i class="icon-ok icon-white"></i> Authorise
							</a>
							<a class="hide modify-extension btn btn-primary" href="edit/${universityId}" data-toggle="modal" data-target="#extension-model">
								<i class="icon-edit icon-white"></i> Modify
							</a>
							<a class="hide revoke-extension btn btn-danger" href="delete/${universityId}" data-toggle="modal" data-target="#extension-model">
								<i class="icon-remove icon-white"></i> Deauthorise
							</a>
						</td>
					</tr>
				</#list>
			</#if>
		</table>
		<div id="extension-model" class="modal fade"></div>
		<script type="text/javascript">
			jQuery(function($){
				// models use ajax to retrieve their contents
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
					$(this).AnyTime_noPicker().AnyTime_picker({
						format: "%e-%b-%Y %H:%i:%s",
						firstDOW: 1
					});
				});

				$('#extension-list').on('click', 'input[type=submit]', function(e){
					e.preventDefault();
					var $form = $(this).closest('form');
					$.post($form.attr('action'), $form.serialize(), function(data){
						// hide the model
						jQuery("#extension-model").modal('hide');
						$.each(data, function(){
							modifyRow(this);
						});
					});
				});

				var modifyRow = function(json){
					$row =  $("#extension-list").find("#row"+json.id);
					if(json.action === "add"){
						updateRowUI(json);
						$(".new-extension", $row).hide();
						$(".modify-extension", $row).show();
						$(".revoke-extension", $row).show();
					} else if (json.action === "edit"){
						updateRowUI(json);
					} else if (json.action === "delete"){
						$("td.expiryDate", $row).html("");
						$(".new-extension", $row).show();
						$(".modify-extension", $row).hide();
						$(".revoke-extension", $row).hide();
					}
				}

				var updateRowUI = function(json){
					for(prop in json){
						$row.find('.'+prop).html(json[prop]);
					}
				}

			});
		</script>
    </div>
</div>

</#escape>