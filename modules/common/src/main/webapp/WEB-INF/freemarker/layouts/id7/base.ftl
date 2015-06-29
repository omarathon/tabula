<#assign tiles=JspTaglibs["/WEB-INF/tld/tiles-jsp.tld"]>
<!DOCTYPE html>
<html lang="en-GB" class="no-js">
<head>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge">
	<meta name="viewport" content="width=device-width, initial-scale=1">

	<!-- Include any favicons here -->
	<meta name="theme-color" content="#5b3069">
	<!-- Use the brand colour of the site -->

	<title>Your page title</title>

	<!-- Lato web font -->
	<link href="//fonts.googleapis.com/css?family=Lato:300,400,700,300italic,400italic,700italic&amp;subset=latin,latin-ext"
		  rel="stylesheet" type="text/css">

	<!-- ID7 -->
	<@stylesheet "/static/css/id7.css" />
	<@stylesheet "/static/css/id7-fixes.css" />

	<!-- Default styling. You will probably want to replace with your own site.css -->
	<@stylesheet "/static/css/id7-theme.css" />

	<@script "/static/js/id7/id7-bundle.js" />

	<!-- HTML5 shim for IE8 support of HTML5 elements -->

	<!--[if lt IE 9]>
	<@script "/static/js/id7/vendor/html5shiv-3.7.2.min.js" />
	<![endif]-->
</head>
<body>
<div class="id7-left-border"></div>
<div class="id7-fixed-width-container">
	<a class="sr-only sr-only-focusable" href="#main">Skip to main content</a>

	<header class="id7-page-header">
		<div class="id7-utility-masthead">
			<nav class="id7-utility-bar">
				<ul>
					<#if IS_SSO_PROTECTED!true>
						<#if user?? && user.loggedIn>
							<li><a href="/settings">Settings</a></li>
							<li><a href="http://warwick.ac.uk/tabula/manual/" target="_blank">Manual</a></li>
							<li><a href="http://warwick.ac.uk/tabula/whatsnew/" target="_blank">What's new?</a></li>
							<li>
								<a href="<@sso.logoutlink target="${component.rootUrl!rootUrl}" />" data-toggle="id7:account-popover" data-loginlink="<@sso.loginlink />" data-name="${user.fullName}">Sign out</a>
							</li>
						<#else>
							<li>
								<a href="<@sso.loginlink />">Sign in</a>
							</li>
						</#if>
					</#if>
				</ul>
			</nav>

			<div class="id7-masthead">
				<div class="id7-masthead-contents">
					<div class="clearfix">
						<div class="id7-logo-column">
							<!-- Don't include the logo row on non-branded sites -->
							<div class="id7-logo-row">
								<div class="id7-logo">
									<a href="http://warwick.ac.uk" title="Warwick homepage">
										<img src="<@url resource="/static/images/shim.gif" />" alt="Warwick">
									</a>
								</div>
								<nav class="id7-site-links">
									<ul>
										<li><a href="http://warwick.ac.uk/study">Study</a></li>
										<li><a href="http://warwick.ac.uk/research">Research</a></li>
										<li><a href="http://warwick.ac.uk/business">Business</a></li>
										<li><a href="http://warwick.ac.uk/alumni">Alumni</a></li>
										<li><a href="http://warwick.ac.uk/news">News</a></li>
										<li><a href="http://warwick.ac.uk/about">About</a></li>
									</ul>
								</nav>
							</div>
						</div>
						<div class="id7-search-column">
							<div class="id7-search">
								<form action="http://search.warwick.ac.uk/website" role="search">
									<input type="hidden" name="source" value="http://warwick.ac.uk/"> <!-- Replace with the current page URL -->
									<div class="form-group">
										<label class="sr-only" for="id7-search-box">Search</label>
										<div class="id7-search-box-container">
											<input type="search" class="form-control input-lg" id="id7-search-box" name="q" placeholder="Search Warwick" data-suggest="go">
											<i class="fa fa-search fa-2x"></i>
										</div>
									</div>
								</form>
							</div>
						</div>
					</div>

					<div class="id7-header-text clearfix">
						<h1>
							<!-- Parent site link often excluded -->
							<span class="id7-parent-site-link"><a href="#">Parent site name</a></span>
							<span class="id7-current-site-link"><a href="#">Current site name</a></span>
						</h1>
					</div>
				</div>
			</div>
		</div>

		<!-- Docs master nav -->
		<div class="id7-navigation">
			<!-- Include the navigation component here if used -->
		</div>
	</header>

	<!-- Page content of course! -->
	<main class="id7-main-content-area" id="main">
		<header class="id7-main-content-header">
			<div class="id7-horizontal-divider">
				<svg xmlns="http://www.w3.org/2000/svg" x="0" y="0" version="1.1" width="1130" height="40" viewBox="0, 0, 1130, 41">
					<path d="m 0,0.5 1030.48, 0 22.8,40 16.96,-31.4 16.96,31.4 22.8,-40 20,0" class="divider" stroke="#383838" fill="none" />
				</svg>
			</div>

			<div class="id7-page-title">
				<h1>Your page title</h1>
			</div>
		</header>

		<div class="id7-main-content">
			<@tiles.insertAttribute name="body" />
		</div>
	</main>

	<footer class="id7-page-footer id7-footer-coloured"> <!-- one of id7-footer-coloured or id7-footer-divider -->
		<div class="id7-site-footer">
			<!-- Only included when footer class="id7-footer-divider"
			  <div class="id7-horizontal-divider">
			  <svg xmlns="http://www.w3.org/2000/svg" x="0" y="0" version="1.1" width="1130" height="40" viewBox="0, 0, 1130, 41">
				<path d="m 0,0.5 1030.48, 0 22.8,40 16.96,-31.4 16.96,31.4 22.8,-40 20,0" class="divider" stroke="#383838" fill="none" />
			  </svg>
			  </div>
			-->

			Your site footer content
		</div>
		<div class="id7-app-footer">
			<!-- Only include the id7-logo-bleed for footer class="id7-footer-coloured" -->
			<div class="id7-logo-bleed"></div>

			<div class="id7-footer-utility">
				<ul>
					<li>Powered by <a href="http://warwick.ac.uk/sitebuilder">App name</a></li>
					<li><a href="http://warwick.ac.uk/copyright">© MMXV</a></li>
					<li><a href="http://warwick.ac.uk/terms">Terms</a></li>
					<li><a href="http://warwick.ac.uk/privacy">Privacy</a></li>
					<li><a href="http://warwick.ac.uk/cookies">Cookies</a></li>
					<li><a href="http://warwick.ac.uk/accessibility">Accessibility</a></li>
				</ul>
			</div>
		</div>
	</footer>
</div>
<div class="id7-right-border"></div>
</body>
</html>