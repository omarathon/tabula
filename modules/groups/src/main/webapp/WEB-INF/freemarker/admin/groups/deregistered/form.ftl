<#escape x as x?html>
	<#import "/WEB-INF/freemarker/_profile_link.ftl" as pl />
	<div id="profile-modal" class="modal fade profile-subset"></div>

	<h1>Deregistered students</h1>
	<h4><span class="muted">for</span> ${smallGroupSet.name}</h4>

	<p>Students who are allocated to groups may become deregistered from the set of small groups
	   after being allocated. Tabula does not automatically remove these students from the groups
	   as it does not know whether it is correct to do so.</p>

	<p>Check the box next to each student below to deregister them from the small group.</p>

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
						<#if checkedStudent.userId == student.userId><#assign checked = true /></#if>
					</#list>
					<tr>
						<td><input type="checkbox" id="chk-${student.userId}" name="students" value="${student.userId}" <#if checked>checked="checked"</#if>></td>
						<td><label for="chk-${student.userId}">${student.firstName}</label></td>
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
</#escape>