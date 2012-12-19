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
			<p class="tel">${address.telephone}</p>
		</#if>
	</div>
</#macro>

<#include "search/form.ftl" />

<hr class="full-width" />

<section class="profile">
	<section class="personal-details clearfix">
		<div class="photo">
			<img src="<@routes.photo profile />" />
		</div>
		
		<header>
			<h1><@fmt.profile_name profile /></h1>
			<h5><@fmt.profile_description profile /></h5>
		</header>
		
		<div class="col1 clearfix">
			<table role="presentation" class="profile-info">
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
					
					<tr>
						<th>Nationality</th>
						<td><@fmt.nationality profile.nationality?default('Unknown') /></td>
					</tr>
					
					<#if profile.dateOfBirth??>
						<tr>
							<th>Date of birth</th>
							<td><@warwick.formatDate value=profile.dateOfBirth.toDateTimeAtStartOfDay() pattern="dd/MM/yyyy" /></td>
						</tr>
					</#if>
					
					<#if profile.termtimeAddress??>
						<tr class="address">
							<th>Term-time address</th>
							<td><@address profile.termtimeAddress /></td>
						</tr>
					</#if>
					
					<#if profile.nextOfKins?size gt 0>
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
		</div>
		
		<div class="col2 clearfix">
			<table role="presentation" class="profile-info">
				<tbody>
					<#if profile.email??>
						<tr>
							<th>Warwick email</th>
							<td>${profile.email}</td>
						</tr>
					</#if>
					
					<#if profile.homeEmail??>
						<tr>
							<th>Alternative email</th>
							<td>${profile.homeEmail}</td>
						</tr>
					</#if>
					
					<#if profile.mobileNumber??>
						<tr>
							<th>Mobile phone</th>
							<td>${profile.mobileNumber}</td>
						</tr>
					</#if>
					
					<tr>
						<th>University number</th>
						<td>${profile.universityId}</td>
					</tr>
					
					<tr>
						<th>IT code</th>
						<td>${profile.userId}</td>
					</tr>
					
					<#if profile.homeAddress??>
						<tr class="address">
							<th>Home address</th>
							<td><@address profile.homeAddress /></td>
						</tr>
					</#if>
				</tbody>
			</table>
		</div>
	</section>
</section>

<#if user.sysadmin>
	<div class="alert alert-info" style="margin-top: 2em;">
		<button type="button" class="close" data-dismiss="alert">&times;</button>
		
		<h4>Sysadmin-only actions</h4>
		
		<p>This is only shown to Tabula system administrators. Click the &times; button to see the page as a non-administrator sees it.</p>
	
		<@f.form method="post" action="${url('/view/' + profile.universityId + '/reimport')}">
			<button class="btn btn-large" type="submit">Re-import details from ADS</button>
		</@f.form>
	</div>
</#if>
</#escape>