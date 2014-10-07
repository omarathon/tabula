<#escape x as x?html>
	<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />

	<h1>Deregistered students</h1>
	<h4><span class="muted">for</span> ${smallGroupSet.name}</h4>

	<div id="profile-modal" class="modal fade profile-subset"></div>

	<p>Students who are allocated to groups may become deregistered from the set of small
	   groups at a later date (for example if they change modules).</p>

	<p>Check the box next to each student below to remove them from the groups.</p>

	<div class="fix-area">
		<#assign submitUrl><@routes.deregisteredStudents smallGroupSet /></#assign>
		<@f.form method="post" action="${submitUrl}" commandName="command">
			<table class="table table-bordered table-striped table-condensed table-hover table-checkable">
				<thead>
					<tr>
						<th class="for-check-all" style="width: 20px; padding-right: 0;"></th>
						<th>First name</th>
						<th>Last name</th>
						<th>University ID</th>
						<th>Group</th>
					</tr>
				</thead>
				<tbody><#list students as studentDetails>
					<#assign student = studentDetails.student />
					<#assign group = studentDetails.group />
					<#assign checked = false />
					<#list command.students as checkedStudent>
						<#if checkedStudent.userId == student.usercode><#assign checked = true /></#if>
					</#list>
					<tr>
						<td><input type="checkbox" id="chk-${student.usercode}" name="students" value="${student.usercode}" <#if checked>checked="checked"</#if>></td>
						<td><label for="chk-${student.usercode}">${student.firstName}</label></td>
						<td>${student.lastName}</td>
						<td>${student.universityId} <@pl.profile_link student.universityId /></td>
						<td>${group.name}</td>
					</tr>
				</#list></tbody>
			</table>

			<div class="submit-buttons fix-footer">
				<input type="submit" class="btn btn-primary" value="Remove deregistered students">
				<a href="<@routes.depthome module=smallGroupSet.module academicYear=smallGroupSet.academicYear/>" class="btn">Cancel</a>
			</div>
		</@f.form>
	</div>

	<script type="text/javascript">
		jQuery(function($) {
			<#-- dynamically attach check-all checkbox -->
			$('.for-check-all').append($('<input />', { type: 'checkbox', 'class': 'check-all use-tooltip', title: 'Select all/none', checked: 'checked' }));
			$('.check-all').tooltip();
			$('.table-checkable').on('click', 'th .check-all', function(e) {
				var $table = $(this).closest('table');
				var checkStatus = this.checked;
				$table.find('td input:checkbox').prop('checked', checkStatus);
			});

			<#-- make table rows clickable -->
			$('.table-checkable').on('click', 'tr', function(e) {
				if ($(e.target).is(':not(input:checkbox)') && $(e.target).closest('a').length == 0) {
					e.preventDefault();
					var $chk = $(this).find('input:checkbox');
					if ($chk.length) {
						$chk.prop('checked', !$chk.prop('checked'));
					}
				}
			});
		});
	</script>
</#escape>