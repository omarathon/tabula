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

<#if user.staff>
	<#include "search/form.ftl" />

	<hr class="full-width" />
</#if>

<article class="profile">
	<section id="personal-details" class="clearfix">
		<div class="photo">
			<img src="<@routes.photo profile />" />
		</div>

		<header>
			<h1><@fmt.profile_name profile /></h1>
			<h5><@fmt.profile_description profile /></h5>
		</header>

		<div class="data clearfix">
			<div class="col1">
				<table class="profile-info">
					<tbody>
						<tr>
							<th>Official name</th>
							<td>${profile.officialName}</td>
						</tr>

						<tr>
							<th>Preferred name</th>
							<td>${profile.fullName}</td>
						</tr>

						<#if profile.gender??>
							<tr>
								<th>Gender</th>
								<td>${profile.gender.description}</td>
							</tr>
						</#if>

						<#if profile.nationality??>
							<tr>
								<th>Nationality</th>
								<td><@fmt.nationality profile.nationality?default('Unknown') /></td>
							</tr>
						</#if>

						<#if profile.dateOfBirth??>
							<tr>
								<th>Date of birth</th>
								<td><@warwick.formatDate value=profile.dateOfBirth.toDateTimeAtStartOfDay() pattern="dd/MM/yyyy" /></td>
							</tr>
						</#if>

						<#if profile.student && profile.termtimeAddress??>
							<tr class="address">
								<th>Term-time address</th>
								<td><@address profile.termtimeAddress /></td>
							</tr>
						</#if>

						<#if profile.student && profile.nextOfKins?? && profile.nextOfKins?size gt 0>
							<tr>
								<th>Emergency contacts</th>
								<td>
									<#list profile.nextOfKins as kin>
										<div>
											<#if kin.firstName?? && kin.lastName??>${kin.fullName}</#if>
											<#if kin.relationship??>(${kin.relationship})</#if>
										</div>
									</#list>
								</td>
							</tr>
						</#if>
					</tbody>
				</table>

				<br class="clearfix">
			</div>

			<div class="col2">
				<table class="profile-info">
					<tbody>
						<#if profile.email??>
							<tr>
								<th>Warwick email</th>
								<td><i class="icon-envelope"></i> <a href="mailto:${profile.email}">${profile.email}</a></td>
							</tr>
						</#if>

						<#if profile.homeEmail??>
							<tr>
								<th>Alternative email</th>
								<td><i class="icon-envelope"></i> <a href="mailto:${profile.homeEmail}">${profile.homeEmail}</a></td>
							</tr>
						</#if>

						<#if profile.phoneNumber??>
							<tr>
								<th>Phone number</th>
								<td>${phoneNumberFormatter(profile.phoneNumber)}</td>
							</tr>
						</#if>

						<#if profile.mobileNumber??>
							<tr>
								<th>Mobile phone</th>
								<td>${phoneNumberFormatter(profile.mobileNumber)}</td>
							</tr>
						</#if>

						<#if profile.universityId??>
							<tr>
								<th>University number</th>
								<td>${profile.universityId}</td>
							</tr>
						</#if>

						<#if profile.userId??>
							<tr>
								<th>IT code</th>
								<td>${profile.userId}</td>
							</tr>
						</#if>

						<#if profile.student && profile.homeAddress??>
							<tr class="address">
								<th>Home address</th>
								<td><@address profile.homeAddress /></td>
							</tr>
						</#if>
					</tbody>
				</table>
			</div>
		</div>

		<#if isSelf>
			<div style="margin-top: 12px;"><span class="use-tooltip" data-placement="bottom" title="Your profile is only visible to you, and to staff who have permission to see student records.">Who can see this information?</span></div>
		</#if>
	</section>

	<#if profile.student>
		<div class="tabbable">
    		<span id="layout-tools" class="pull-right"><i class="icon-folder-close" title="Switch to tabbed layout"></i> <i class="icon-th-large" title="Switch to gadget layout"></i> <i class="icon-ok" title="Save layout settings"></i></span>
    		<div class="row-fluid">
    			<ul class="nav nav-tabs" id="profile-section-tabs">
    				<li class="active"><a href="#course-pane" data-toggle="tab" data-title="Course details">Course details <i class="icon-move" title="Click and drag to move"></i> <i class="icon-resize-small" title="Collapse"></i></a></li>
    				<li><a href="#supervision-pane" data-toggle="tab" data-title="Supervision">Supervision <i class="icon-move" title="Click and drag to move"></i> <i class="icon-resize-small" title="Collapse"></i></a></li>
    				<li><a href="#pd-pane" data-toggle="tab" data-title="Personal development">Personal development <i class="icon-move" title="Click and drag to move"></i> <i class="icon-resize-small" title="Collapse"></i></a></li>
    			</ul>
    		</div>

    		<div class="tab-content">
    			<div class="tab-pane active" id="course-pane">
    				<#include "_course_details.ftl" />
				</div>

				<div class="tab-pane" id="supervision-pane">
					<#include "_supervision.ftl" />
				</div>

				<div class="tab-pane" id="pd-pane">
					<#include "_personal_development.ftl" />
				</div>
			</div>
		</div>
	</#if>
</article>

<#if user.sysadmin>
	<div class="alert alert-info sysadmin-only-content" style="margin-top: 2em;">
		<button type="button" class="close" data-dismiss="alert">&times;</button>

		<h4>Sysadmin-only actions</h4>

		<p>This is only shown to Tabula system administrators. Click the &times; button to see the page as a non-administrator sees it.</p>

		<@f.form method="post" action="${url('/sysadmin/import-profiles/' + profile.universityId, '/scheduling')}">
			<button class="btn btn-large" type="submit">Re-import details from Membership, SITS and about a billion other systems</button>
		</@f.form>
	</div>
</#if>
</#escape>