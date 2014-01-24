<section id="personal-details" class="clearfix">

	<@fmt.member_photo profile />

	<header>
		<h1><@fmt.profile_name profile /></h1>
	</header>

	<div class="data clearfix">
		<div class="col1">
			<table class="profile-or-course-info">
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
							<td><@profile_macros.address profile.termtimeAddress /></td>
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

					<#if profile.tier4VisaRequirement??>
						<tr>
							<th>Nationality requires visa</th>
							<#if profile.tier4VisaRequirement>
								<td>Yes</td>
								</tr>
								<#if profile.casUsed??>
									<tr>
										<th>CAS used to obtain visa</th>
										<#if profile.casUsed>
											<td>Yes</td>
										<#else>
											<td>No</td>
										</#if>
									</tr>
								</#if>
							<#else>
								<td>No</td>
								</tr>
							</#if>
					</#if>
				</tbody>
			</table>
		</div>

		<div class="col2">
			<table class="profile-or-course-info">
				<tbody>
					<#if profile.email??>
						<tr>
							<th>Warwick email</th>
							<td><i class="icon-envelope-alt"></i> <a href="mailto:${profile.email}">${profile.email}</a></td>
						</tr>
					</#if>

					<#if profile.homeEmail??>
						<tr>
							<th>Alternative email</th>
							<td><i class="icon-envelope-alt"></i> <a href="mailto:${profile.homeEmail}">${profile.homeEmail}</a></td>
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
							<td><@profile_macros.address profile.homeAddress /></td>
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
