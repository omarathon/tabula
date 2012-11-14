## Populate request header X-Requested-URI with requested URI (including query string)
# If query string is empty, don't add a question mark
RewriteCond %{QUERY_STRING} ^$
RewriteRule .? - [E=REQ_URI:%{ENV:SCRIPT_URI}]
RewriteCond %{QUERY_STRING} !^$
RewriteRule .? - [E=REQ_URI:%{ENV:SCRIPT_URI}?%{QUERY_STRING}]
RequestHeader set X-Requested-URI "%{REQ_URI}e" env=!no-requested-uri
####

RewriteRule ^/favicon\.ico - [G,L]
RewriteRule \.php$ [G,L]

# static content suffixes
RewriteRule ^(/static/.*?)\.[0-9]{10,16}$ $1$2 [QSA]

# Add trailing slash because otherwise ensadness occurs
RewriteRule ^/courses$ /courses/ [QSA,L,R=301]

# Rewrite everything that isn't in a module to the home module
RewriteCond %{REQUEST_URI} !^/courses.* [OR]
RewriteCond %{REQUEST_URI} !^/static.*
RewriteRule (.*) /home$1 [QSA]

# Else, go straight to Jboss
RewriteRule (.*) http://127.0.0.1:${proxy:port}$1 [QSA,P,L]