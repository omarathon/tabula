<#escape x as x?html>
<section class="profile">
	<h1>${profile.fullName} (${profile.universityId})</h1>
	
	<section class="personal-details">
		<h2>Personal details</h2>
	
		<img class="photo" src="<@url page="/view/photo/${profile.universityId}.jpg" />" width="120" />
		
		<dl>
			<dt>Official name</dt>
			<dd>${profile.officialName}</dd>
			
			<dt>Preferred name</dt>
			<dd>${profile.fullName}</dd>
			
			<#if profile.gender??>
				<dt>Gender</dt>
				<dd>${profile.gender.description}</dd>
			</#if>
			
			<dt>University number</dt>
			<dd>${profile.universityId}</dd>
			
			<dt>Description</dt>
			<dd>${profile.groupName}</dd>
			
			<#if profile.studentStatus??>
				<dt>Status</dt>
				<dd>${profile.studentStatus}</dd>
			</#if>
			
			<#if profile.transferReason??>
				<dt>Transfer reason</dt>
				<dd>${profile.transferReason}</dd>
			</#if>
			
			<#if profile.feeStatus??>
				<dt>Fee status</dt>
				<dd>${profile.feeStatus}</dd>
			</#if>
			
			<dt>Nationality</dt>
			<dd>${profile.nationality?default('Unknown')}</dd>
			
			<#if profile.dateOfBirth??>
				<dt>Date of birth</dt>
				<dd><@warwick.formatDate value=profile.dateOfBirth.toDateTimeAtStartOfDay() pattern="d MMMM yyyy" /></dd>
			</#if>
			
			<#if profile.homeDepartment??>
				<dt>Home department</dt>
				<dd>${profile.homeDepartment.name}</dd>
			</#if>
			
			<#if profile.yearOfStudy??>
				<dt>Year of study</dt>
				<dd>${profile.yearOfStudy}</dd>
			</#if>
			
			<dt>2 + 2?</dt>
			<dd>??????</dd>
			
			<dt>Personal tutor</dt>
			<dd>??????</dd>
			
			<dt>Registered disability</dt>
			<dd>??????</dd>
			
			<dt>Notes on disability</dt>
			<dd>??????</dd>
			
			<dt>IT code</dt>
			<dd>${profile.userId}</dd>
			
			<dt>IT account status</dt>
			<dd>${profile.inUseFlag}</dd>
			
			<#if profile.inactivationDate??>
				<dt>IT account inactivation date</dt>
				<dd><@warwick.formatDate value=profile.inactivationDate.toDateTimeAtStartOfDay() pattern="d MMMM yyyy" /></dd>
			</#if>
			
			<#if profile.attendanceMode??>
				<dt>Mode of attendance</dt>
				<dd>${profile.attendanceMode}</dd>
			</#if>
			
			<dt>Comments</dt>
			<dd>??????</dd>
		</dl>
	</section>
	
	<section class="course-details">
		<h2>Course details</h2>
		
		<dl>
			<dt>Course title</td>
			<dd>??????</dd>
			
			<dt>Length of course</td>
			<dd>??????</dd>
			
			<dt>Course code</td>
			<dd>??????</dd>
			
			<dt>PGT dissertation title / PGR thesis title</td>
			<dd>??????</dd>
			
			<dt>3rd year project supervisor(s)</td>
			<dd>??????</dd>
			
			<dt>PGT dissertation supervisor(s) / PGR thesis supervisor(s)</td>
			<dd>??????</dd>
			
			<dt>Source of funding</td>
			<dd>??????</dd>
			
			<dt>Date of dissertation / thesis submission</td>
			<dd>??????</dd>
			
			<dt>VIVA date</td>
			<dd>??????</dd>
			
			<dt>Post-Warwick destination</td>
			<dd>??????</dd>
		</dl>
	</section>
	
	<section class="contact-details">
		<h2>Contact details</h2>
		
		<dl>
			<#if profile.email??>
				<dt>Warwick email address</dt>
				<dd>${profile.email}</dd>
			</#if>
			
			<#if profile.homeEmail??>
				<dt>Alternative email address</dt>
				<dd>${profile.homeEmail}</dd>
			</#if>
			
			<#if profile.mobileNumber??>
				<dt>Mobile phone number</dt>
				<dd>${profile.mobileNumber}</dd>
			</#if>
			
			<dt>Emergency contact</dt>
			<dd><dl>
				<dt>Name</dt>
				<dd>??????</dd>
				
				<dt>Telephone</dt>
				<dd>??????</dd>
				
				<dt>Relationship</dt>
				<dd>??????</dd>
			</dl></dd>
			
			<dt>Term-time address</dt>
			<dd>??????</dd>
			
			<dt>Home address</dt>
			<dd>??????</dd>
			
			<dt>Details last updated</dt>
			<dd>??????</dd>
		</dl>
	</section>
	
	<section class="previous-qualifications">
		<h2>Previous qualifications</h2>
		
		<dl>
			<#if profile.highestQualificationOnEntry??>
				<dt>Highest qualification on entry</dt>
				<dd>${profile.highestQualificationOnEntry}</dd>
			</#if>
			
			<#if profile.lastInstitute??>
				<dt>Previous institute</dt>
				<dd>${profile.lastInstitute}</dd>
			</#if>
			
			<#if profile.lastSchool??>
				<dt>Previous school</dt>
				<dd>${profile.lastSchool}</dd>
			</#if>
		</dl>
	</section>
</section>

<#if user.sysadmin>
	<@f.form method="post" action="${url('/view/' + profile.universityId + '/reimport')}">
		<button class="btn btn-large btn-danger" type="submit">Re-import details from ADS</button>
	</@f.form>
</#if>
</#escape>