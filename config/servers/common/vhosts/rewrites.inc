## Populate request header X-Requested-URI with requested URI (including query string)
# If query string is empty, don't add a question mark
RewriteCond %{QUERY_STRING} ^$
RewriteRule .? - [E=REQ_URI:%{ENV:SCRIPT_URI}]
RewriteCond %{QUERY_STRING} !^$
RewriteRule .? - [E=REQ_URI:%{ENV:SCRIPT_URI}?%{QUERY_STRING}]
RequestHeader set X-Requested-URI "%{REQ_URI}e" env=!no-requested-uri
####

# Rewrite API to the API instances
RewriteCond %{REQUEST_URI} ^/api/v1/(.*)
RewriteRule (.*) http://127.0.0.1:${api:port}$1 [QSA,P,L]

# Go straight to app server
RewriteCond %{REQUEST_URI} !^/errordocuments/(.*)
RewriteRule (.*) http://127.0.0.1:${proxy:port}$1 [QSA,P,L]