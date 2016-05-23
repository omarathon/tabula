<#escape x as x?html>

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

					<#if !profile.student>
						<tr>
							<th>Job title</th>
							<td>${profile.jobTitle}</td>
						</tr>
					</#if>


					<#if profile.gender??>
						<tr>
							<th>Gender</th>
							<td>${profile.gender.description}</td>
						</tr>
					</#if>

					<#if profile.dateOfBirth??>
						<tr>
							<th>Date of birth</th>
							<td><@warwick.formatDate value=profile.dateOfBirth.toDateTimeAtStartOfDay() pattern="dd/MM/yyyy" /></td>
						</tr>
					</#if>

					<#if profile.nationality??>
						<tr>
							<th>Nationality</th>
							<td><@fmt.nationality profile.nationality?default('Unknown') /></td>
						</tr>
					</#if>

					<#if features.disabilityRenderingInProfiles && (profile.disability.reportable)!false>
						<tr>
							<th>Disability</th>
							<td>
								<a class="use-popover cue-popover" id="popover-disability" data-html="true"
								   data-original-title="Disability"
								   data-content="<p><#if isSelf>You have<#else>This student has</#if> self-reported the following disability code:</p><div class='well'><h6>${profile.disability.code}</h6><small>${(profile.disability.sitsDefinition)!}</small></div>">
									${profile.disability.definition}
								</a>
							</td>
						</tr>
					</#if>

					<#if profile.termtimeAddress??>
					<tr class="address">
						<th>Term-time address</th>
						<td><@profile_macros.address profile.termtimeAddress /></td>
					</tr>
					</#if>

					<#if profile.nextOfKins?has_content>
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

					 <#if features.visaInStudentProfile && !isSelf && profile.hasTier4Visa?? && profile.casUsed??>
						<tr>
							<th>Tier 4 requirements</th>
							<td>
								<#if profile.casUsed && profile.hasTier4Visa>Yes
								<#elseif !profile.casUsed && !profile.hasTier4Visa>No
								<#else>
									<#if !profile.casUsed && profile.hasTier4Visa>
										<#assign inconsistency = "Tier 4 visa exists but no Confirmation of Acceptance for Studies" />
									<#else>
										<#assign inconsistency = "Confirmation of Acceptance for Studies exists but no tier 4 visa" />
									</#if>
									Contact the <a href="mailto:immigrationservice@warwick.ac.uk">Immigration Service</a>
									<a class="use-popover" data-content="Contact the University's Immigration Service to find out whether tier 4
									requirements apply to this student. (${inconsistency})" data-toggle="popover"><i class="faf a-question-circle"></i></a>
								</#if>
							</td>
						</tr>
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
							<td><a href="mailto:${profile.email}">${profile.email}</a></td>
						</tr>
					</#if>

					<#if profile.homeEmail??>
						<tr>
							<th>Alternative email</th>
							<td><a href="mailto:${profile.homeEmail}">${profile.homeEmail}</a></td>
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

</#escape>