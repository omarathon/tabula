#!/bin/bash
set -e

read -p "Enter the fully qualified domain name of your local machine (e.g. localdev.warwick.ac.uk): " domain

if ! [[ $domain =~ .*\.warwick\.ac.\uk$ ]]
then
   echo "Domain must end with .warwick.ac.uk for SSO to work."
   exit 1
fi

read -p "Enter your email address (for Let's Encrypt to issue a certificate): " email

read -p "Enter your SSO provder ID (leave blank for urn:$domain:tabula:service): " sso_provider_id
if [ "x${sso_provider_id}" = "x" ]; then
    export sso_provider_id=urn:$domain:tabula:service
fi

rsa_key_size=4096
script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
data_path="${script_dir}/data/certbot"
nginx_data_path="${script_dir}/data/nginx"
nginx_certbot_data_path="${script_dir}/data/nginx-certbot"
tomcat_data_path="${script_dir}/data/tomcat"
staging=0 # Set to 1 if you're testing your setup to avoid hitting request limits

if [ -d "$data_path" ]; then
  read -p "Existing data found for $domain. THIS WILL OVERWRITE ALL YOUR CURRENT CONFIGURATION (but persistent data such as database and object storage will remain). Do you want to continue? (y/N) " decision
  if [ "$decision" != "Y" ] && [ "$decision" != "y" ]; then
    exit
  fi
fi

if [ ! -e "$data_path/conf/options-ssl-nginx.conf" ] || [ ! -e "$data_path/conf/ssl-dhparams.pem" ]; then
  echo "### Downloading recommended TLS parameters ..."
  mkdir -p "$data_path/conf"
  curl -s https://raw.githubusercontent.com/certbot/certbot/master/certbot-nginx/certbot_nginx/options-ssl-nginx.conf > "$data_path/conf/options-ssl-nginx.conf"
  curl -s https://raw.githubusercontent.com/certbot/certbot/master/certbot/ssl-dhparams.pem > "$data_path/conf/ssl-dhparams.pem"
  echo
fi

echo "### Creating dummy certificate for $domain ..."
path="/etc/letsencrypt/live/$domain"
mkdir -p "$data_path/conf/live/$domain"
sudo docker-compose -f docker-compose-certbot.yml run --rm --entrypoint "\
  openssl req -x509 -nodes -newkey rsa:1024 -days 1\
    -keyout '$path/privkey.pem' \
    -out '$path/fullchain.pem' \
    -subj '/CN=$domain'" certbot
echo

echo "### Writing nginx config ..."
mkdir -p $nginx_data_path
cat <<EOF > $nginx_data_path/app.conf
server {
    listen 80;
    server_name $domain;
    server_tokens off;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://\$host\$request_uri;
    }
}

server {
    listen 443 ssl http2;
    server_name $domain;
    server_tokens off;

    ssl_certificate /etc/letsencrypt/live/$domain/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/$domain/privkey.pem;
    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;

    client_max_body_size 10G;

    location / {
				proxy_pass http://tabula-tomcat:8080;
				proxy_set_header Host \$host;
				proxy_set_header X-Real-IP \$remote_addr;
				proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
				proxy_set_header X-Forwarded-Proto \$scheme;
				proxy_set_header X-Requested-Uri "\${scheme}://\${host}\${request_uri}";
				proxy_http_version 1.1;
				proxy_connect_timeout 300;
				proxy_send_timeout 300;
				proxy_read_timeout 300;
				send_timeout 300;
    }
}
EOF

mkdir -p $nginx_certbot_data_path
cat <<EOF > $nginx_certbot_data_path/app.conf
server {
    listen 80;
    server_name $domain;
    server_tokens off;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
}
EOF

echo "### Starting nginx ..."
sudo docker-compose -f docker-compose-certbot.yml up --force-recreate -d nginx-certbot
echo

echo "### Deleting dummy certificate for $domain ..."
sudo docker-compose -f docker-compose-certbot.yml run --rm --entrypoint "\
  rm -Rf /etc/letsencrypt/live/$domain && \
  rm -Rf /etc/letsencrypt/archive/$domain && \
  rm -Rf /etc/letsencrypt/renewal/$domain.conf" certbot
echo


echo "### Requesting Let's Encrypt certificate for $domain ..."
domain_args="-d $domain"

# Select appropriate email arg
case "$email" in
  "") email_arg="--register-unsafely-without-email" ;;
  *) email_arg="--email $email" ;;
esac

# Enable staging mode if needed
if [ $staging != "0" ]; then staging_arg="--staging"; fi

sudo docker-compose -f docker-compose-certbot.yml run --rm --entrypoint "\
  certbot certonly --webroot -w /var/www/certbot \
    $staging_arg \
    $email_arg \
    $domain_args \
    --rsa-key-size $rsa_key_size \
    --agree-tos \
    --non-interactive \
    --force-renewal" certbot
echo

echo "### Stopping nginx ..."
sudo docker-compose -f docker-compose-certbot.yml down

mkdir -p $tomcat_data_path/lib
echo "### Writing Tabula properties file"
cat <<EOF > $tomcat_data_path/lib/tabula.properties
spring.profiles.active=sandbox
scheduling.enabled=true
environment.production=false
environment.nonproduction=true
environment.standby=false

toplevel.url=https://$domain

permissions.admin.group=in-tabula-local-dev-sysadmins
permissions.masquerade.group=in-tabula-local-dev-masqueraders

# Set these after running init.sh if you have sandbox credentials
turnitin.aid=
turnitin.said=
turnitin.key=

TurnitinLti.aid=
TurnitinLti.key=
TurnitinLti.url=

elasticsearch.cluster.nodes=tabula-elasticsearch:9200
elasticsearch.cluster.name=tabula
elasticsearch.index.prefix=tabula

activemq.broker=tcp://tabula-activemq:61616

base.data.dir=/usr/local/tomcat/temp
filesystem.create.missing=true

objectstore.provider=swift
objectstore.container=tabula
objectstore.encryptionKey=cZRYXN05wYqqypMEuJSpnWDV9ynnXIiCNecqeLdmg04=
objectstore.swift.endpoint=http://tabula-objectstorage:8080/auth/v2.0
objectstore.swift.username=swift
objectstore.swift.password=fingertips

# Add mailtrap credentials in here if you have them. DON'T ENABLE features.emailStudents unless you have mailtrap creds set
# mail.smtp.host=smtp.mailtrap.io
# mail.smtp.port=2525
# mail.smtp.user=
# mail.smtp.password=
# mail.smtp.auth=true
#
# features.emailStudents=true

# Set this to true if you have mywarwick.* credentials and you're testing that
features.notificationListeners.mywarwick=false
mywarwick.instances.0.baseUrl=https://my.invalid
mywarwick.instances.0.providerId=
mywarwick.instances.0.userName=
mywarwick.instances.0.password=

# Replace with a real registered application ID for Photos.Warwick
photos.applicationId=
photos.preSharedKey=

celcat.fetcher.ib.username=
celcat.fetcher.ib.password=

EOF

cat <<EOF > $tomcat_data_path/lib/memcached.properties
memcached.servers=tabula-memcached:11211
memcached.maxReconnectDelay=5
memcached.opTimeout=250
memcached.hashAlgorithm=KETAMA

EOF

echo "### Writing Tabula SSO config file"

openssl genrsa -out /tmp/private_key.pem 2048 &>/dev/null
openssl rsa -pubout -in /tmp/private_key.pem -out /tmp/public_key.pem &>/dev/null

trustedapps_public_key=$(cat /tmp/public_key.pem | tail -n +2 | head -n -1 | tr -d '\n')
trustedapps_private_key=$(cat /tmp/private_key.pem | tail -n +2 | head -n -1 | tr -d '\n')

rm /tmp/public_key.pem /tmp/private_key.pem

cat <<EOF > $tomcat_data_path/lib/tabula-sso-config.xml
<?xml version="1.0" encoding="UTF-8"?>
<config>
  <httpbasic><allow>true</allow></httpbasic>
  <mode>new</mode>

  <cluster>
   <enabled>true</enabled>
   <datasource>java:comp/env/jdbc/TabulaSSODS</datasource>
  </cluster>

  <origin>
    <login>
      <location>https://websignon.warwick.ac.uk/origin/hs</location>
    </login>
    <logout>
      <location>https://websignon.warwick.ac.uk/origin/logout</location>
    </logout>
    <attributeauthority>
      <location>https://websignon.warwick.ac.uk/origin/aa</location>
    </attributeauthority>
  </origin>

  <shire>
    <filteruserkey>SSO_USER</filteruserkey>
    <uri-header>x-requested-uri</uri-header>
    <location>https://$domain/sso/acs</location>
    <sscookie>
      <name>SSO-SSC-Tabula</name>
      <path>/</path>
      <domain>$domain</domain>
      <secure>true</secure>
    </sscookie>
    <providerid>$sso_provider_id</providerid>
  </shire>

  <logout>
    <location>https://$domain/sso/logout</location>
  </logout>

  <oauth>
    <enabled>true</enabled>
    <!-- Location of the OAuth service -->
    <service>
      <location>https://websignon.warwick.ac.uk/origin/oauth/service</location>
    </service>
  </oauth>

  <credentials>
    <certificate>file:/etc/letsencrypt/live/$domain/fullchain.pem</certificate>
    <key>file:/etc/letsencrypt/live/$domain/privkey.pem</key>
  </credentials>

  <trustedapps>
      <!-- Credentials for this application -->
      <publickey>$trustedapps_public_key</publickey>
      <privatekey>$trustedapps_private_key</privatekey>
  </trustedapps>
</config>

EOF