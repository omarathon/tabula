require 'fileutils'
require 'rexml/document'
require 'uri'
require 'open-uri'
require 'yaml'
require 'net/http'
require 'time'
require 'etc'

include REXML

def log(string)
  puts "[#{Time.now.iso8601}] #{string}"
end

class Deployment
  attr_reader :jbosses, :scheduler_jboss, :apaches

  def self.from_yaml_file(filename)
     deployer = YAML.load_file(filename)
     deployer.init
     deployer
  end

  # NOTE this doesn't run when loading from YAML
  def initialize(lockfile,jboss_instances,jboss_scheduler_instance,apache_instances,notification_emails)
     @lockfile=lockfile
     @jbosses=jboss_instances
     @scheduler_jboss=jboss_scheduler_instance
     @apaches=apache_instances 
     @notification_emails=notification_emails
     @build_status='starting'
  end
  
  def init
    log "Initialising"
    war_name = @war_name || @app
    log "WAR name: #{war_name}"
    @jbosses.each do |name, group|
       group.each do |jboss|
         jboss.group_name = name
         jboss.war_name = war_name
       end
    end  
  end
 
  def run
     if (preconditions_ok?) then
       set_running
       build_war
       deploy_war_to_scheduler
       deploy_static
       deploy_war
       unset_running
     end
  end
  
  # all jbosses in all groups, except scheduler
  def all_jbosses
    @jbosses.map{|name,group| group}.flatten
  end

  def print_status
    log ""
    puts "***********************************************************"
    status="#{@build_status}. \n   "
    unless @scheduler_jboss.nil?
      status += "scheduler: #{@scheduler_jboss.state}  #{@scheduler_jboss.deployment_state} "
    end 
    all_jbosses.each { |jboss| status += "#{jboss.name}: #{jboss.state} #{jboss.deployment_state}. " }
    status +="\n   "
    @apaches.each{ |apache| status +="#{apache.name}: #{apache.state} "}
    puts status
    puts "***********************************************************"
  end

  def preconditions_ok?
    if(File.exists?(@lockfile)) then
      log("There seems to be a deploy running already! If not, remove #{@lockfile} and try again")
      return false
    end
    
    current_user = Etc.getlogin
    unless current_user == @build_user
    	log "Current user is #{current_user} but expected #{@build_user}"
    	return false
    end 
    
    arch_dir = "/var/src/archive"
    
    unless File.exists? arch_dir and File.can_write_to? arch_dir
    	log "Can't write to #{arch_dir}"
    	return false
    end
    
    #gets apaches whose preconditions aren't ok, and checks that collection is empty.
    return false unless static_preconditions_ok?
    return false unless @apaches.reject{|a| a.preconditions_ok? }.empty?
    return false unless @scheduler_jboss.nil? or @scheduler_jboss.preconditions_ok?
    return false unless all_jbosses.reject{|j| j.preconditions_ok? }.empty?
    return true
  end
  
  def set_running
     @build_status= 'started'
     FileUtils.touch(@lockfile)
     print_status 
  end

  def build_war
     @build_status= 'building war'
     print_status
     
     arch_dir = @archive_dir || "/var/src/arch"
     checkout = "#{@checkout_base}/#{@app}"
     
     # Checkout the source
  
     
     case @repository
     when "git"
        # Git. Don't checkout afresh each time; do it in place.
        # If we need access to the previous checkout, we should just
        # store the commit ID in a file somewhere.
        # Assumes there's a cloned repo in #{checkout}; Puppet should ensure this.
        log "Performing a Git hard reset and a pull"
        system("cd #{checkout} ; git reset --hard HEAD; git pull")
     else
        # CVS.

        checkout_backup = "#{@checkout_base}/#{@app}-old"
        temp_checkout_base = "/tmp"
        temp_checkout = "#{temp_checkout_base}/#{@app}"
     
        log "Checking out source into #{temp_checkout_base}"
     
        if File.exists? temp_checkout
           log "Removing old directory from #{temp_checkout}"
           FileUtils.rm_rf temp_checkout
        end
     
        if File.exists? checkout_backup
           log "Removing old directory from #{checkout_backup}"
     	   FileUtils.rm_rf checkout_backup
        end

        # TODO find a way to do this without a system call
        system("cd #{temp_checkout_base} ; /usr/local/bin/cvs -d:ext:build@byngo:/cvsrep -Q get #{@app}")
        raise "Couldn't checkout from CVS" unless $? == 0

        log "Moving new sources into #{@checkout_base}"
     
        if File.exists? checkout
           FileUtils.mv checkout, checkout_backup
        end
     
        FileUtils.mv temp_checkout, checkout
     end
     
	 # Build the application
	 
	 log "Building app in #{checkout}"
	 
         ant = @ant_path || "/usr/local/ant/bin/ant"
         ant_target = @ant_package_target || "build-war"

	 # TODO find a way to do this without a system call
	 system("cd #{checkout} ; #{ant} -q #{ant_target}")
	 raise "Ant build failed: giving up" unless $? == 0
	 
	 built_war = "#{checkout}/dist/#{@app}.war"
	 
	 # Archive and move the newly built application
	 
	 version = Time.now.strftime "%Y%m%d-%H%M%S"
	 new_build = "#{arch_dir}/#{@app}.war.#{version}"
	 
	 FileUtils.cp built_war, new_build
	 
	 current_link = "#{arch_dir}/#{@app}.war.current"
	 lastgood_link = "#{arch_dir}/#{@app}.war.lastgood"
	 
	 if File.exists? current_link
	 	lastgood = File.readlink(current_link)
	 
	 	FileUtils.rm current_link
	 
	 	if File.exists? lastgood_link
	 		FileUtils.rm lastgood_link
	 	end
	 	
	 	FileUtils.ln_s lastgood, lastgood_link
	 end
	 
	 FileUtils.ln_s new_build, current_link
	 
	 log "New version is #{new_build}"

     @warfile = current_link
     @build_status = "built war #{@warfile}"
     print_status
  end

  def deploy_war_to_scheduler
     if @scheduler_jboss.nil?
        log "No scheduler JBoss defined, skipping this step"
     else
        # no need to monkey with apache
        @scheduler_jboss.deploy(@warfile, self)
        @build_status='deploying war'
        print_status
     end
  end

  def deploy_war
  	raise "@warfile is not set" if @warfile.nil?
    @jbosses.each do |group_name, group|
      done_first_deploy=false
      log "Deploying group '#{group_name}'"
      first_server=group[0]
      group.each do |jboss|
        if (!done_first_deploy) then
          hide_jboss(jboss)
        end
        jboss.deploy(@warfile, self)
        done_first_deploy = true
        use_this_jboss_only(first_server)
      end
      use_all_jbosses(group_name)
      log "Finished deploying group '#{group_name}'"
    end
    @build_status='deployed war'  
    print_status
  end
  
  #Redeploy a specific JBoss.
  #DOESN'T REMOVE FROM PORT BALANCING! Remove from Apache first.
  #see hide_jboss(jboss)
  def deploy_war_to (jboss)
  	build_war
    jboss = find_jboss_by_name(jboss) unless jboss.is_a? Jboss
    jboss.deploy(@warfile, self)
  end
  
  # Finds the Apache whose name matches the name of the JBoss group,
  # as this one is responsible for serving content for this group.
  def apaches_for(jboss)
    apaches = @apaches.select{|a| a.name == jboss.group_name }
    raise "No apaches found for #{jboss}" if apaches.empty?
    return apaches
  end
  
  # Like apaches_for(jboss), but just takes the name of a JBoss group.
  def apaches_by_group(group_name)
    @apaches.select{|a| a.name == group_name }
  end

  # remove from apache load balancing
  def hide_jboss(jboss)
    jboss = find_jboss_by_name(jboss) unless jboss.is_a? Jboss
    log "Hiding JBoss #{jboss.name}"
    apaches_for(jboss).each{ |a| a.remove_server(jboss.port,self)}
  end
  
  # point ALL apache requests at this jboss
  # (by ALL, we mean all Apaches that serve this JBoss' group)
  def use_this_jboss_only(jboss)
    jboss = find_jboss_by_name(jboss) unless jboss.is_a? Jboss
    log "Using only #{jboss.name}"
    apaches_for(jboss).each{|a|
      a.use_server_exclusively(jboss.port,self) 
    }
  end
  
  def use_all_jbosses(group_name=nil)
    if group_name.nil?
      # enable port balancing for all port balancer groups
      @jbosses.each do |name, group|
        apaches_by_group(name).each { |a| a.use_balancer(self) }
      end
    else
      # enable port balancing for a specific group
      apaches_by_group(group_name).each { |a| a.use_balancer(self) }
    end
  end
  
  def find_jboss_by_name(name)
	  really_all = all_jbosses + [ @scheduler_jboss ]
    jboss = really_all.select {|j| name == j.name }.first
    raise "JBoss '#{name}' not found" unless jboss
    return jboss
  end
  
  def static_preconditions_ok?
  	log "Checking preconditions for static deploy"
        if @apache_htdocs.nil?
           log "apache_htdocs not specified, assuming no static Apache content for this app"
        else 

	static_war_file = "#{@apache_htdocs}/static_war"
	temporary_dir = "#{@apache_htdocs}/static_war.new"
  	backup_dir = "#{@apache_htdocs}/static_war.last"
  	
  	if File.exists? temporary_dir then
  		log "htdocs temporary directory already exists: #{temporary_dir}"
  		return false
  	end
	
	unless File.exists? @apache_htdocs
		log "Apache htdocs does not exist: #{@apache_htdocs}"
		return false
	end
	
	if File.exists?(static_war_file) then
		unless File.can_write_to? static_war_file
			log "Can't write to #{static_war_file}"
			return false
		end
	else
		unless File.can_write_to? @apache_htdocs
			log "Can't write to #{@apache_htdocs}"
			return false
		end
	end

        end
	
	true
  end

  def deploy_static

	if @apache_htdocs.nil?
		log "Skipping deploy_static as apache_htdocs is not specified"
		return
	end

	static_war_src = "#{@checkout_base}/#{@app}/target/static_war"
	
	static_war_file = "#{@apache_htdocs}/static_war"
  	temporary_dir = "#{@apache_htdocs}/static_war.new"
  	backup_dir = "#{@apache_htdocs}/static_war.last"
  
  	# create a temporary place for the new files
  	
  	log "Creating new files in #{temporary_dir}"
  	
  	def copy_dir src, dest
  		FileUtils.mkdir dest
  		
  		Dir.foreach(src) do |file|
  			next if file.match(/^\./) or file.match(/CVS/)
  			
  			s = File.join(src, file)
  			d = File.join(dest, file)
  			
  			if File.directory? s
  				copy_dir s, d
  			else
  				FileUtils.cp(s, d)
  			end
  		end
  	end
  	
  	copy_dir static_war_src, temporary_dir
  	
  	if File.exists? backup_dir
  		log "Removing previous backup directory #{backup_dir}"
  		FileUtils.rm_r backup_dir
  	end
  	
  	# Move the new content into place
  	
  	log "Replacing static content"
  	
  	FileUtils.mv static_war_file, backup_dir
  	FileUtils.mv temporary_dir, static_war_file
  	
  	puts "Backing up current state"
  	
  	version = Time.now.strftime "%Y%m%d-%H%M%S"
  	
  	# TODO find a way to do this without a system call
  	system("cd #{@apache_htdocs} ; gtar czf /var/src/arch/static_war.tgz.#{version} static_war")
  	raise "Couldn't create archive of new static_war" unless $? == 0
  	  
  	@build_status = 'built war and deployed static'
    print_status
  end

  def unset_running
     @build_status='complete'
     File.delete(@lockfile)
     print_status
  end

end


class Jboss
  attr_accessor :name, :group_name, :state, :deployment_state,:port,:war_name

  def initialize(instance_name,port)
    @name= instance_name
    @state= "running"
    @deployment_state= 'old'
    @port=port
  end
  
  def war_path
  	"/usr/local/jboss/server/#{@name}/deploy/#{war_name}.war"
  end
  
  def preconditions_ok? 
  	unless File.can_write_to? war_path
  	  log "Can't write to war destination: #{war_path}"
	  return false
  	end
	true
  end

  def deploy(warfile,status_printer)
    graceful_halt
    install_war(warfile)
    start
    status_printer.print_status
  end

  def install_war(warfile)
    FileUtils.cp(warfile, war_path)
    @deployment_state='new'
  end

  def graceful_halt
    keep_going = true
    tries = 5
    while (keep_going) do
      tries = tries -1
      log("Waiting for all threads on #{@name} to complete" )
      sleep(5)
      if (is_idle? || tries < 1) then 
        keep_going = false
      end
    end
    log "Finished waiting; stopping #{name} now"
    system("#{@stop_command}#{name}")
    @state='stopped'
  end

  def start
    
    instance = @name

    # test_page is for Sitebuilder compatibility - 
    # other apps should specify test_path
    test_path = @test_path || ("/sitebuilder2/render/renderPage.htm?sbrPage=%s" % @test_page)

  	retries = 6
  	
  	jboss_run = "sudo /etc/local.d/jboss_run.sh"
  	
	def check_instance_exists
		log "Verifying [#{@name}] is known by jboss..."
		system("#{jboss_run} list | grep #{@name}")
		
		raise "[#{@name}] isn't in the list of known servers!" unless $? == 0
	end  	

	def do_restart_jboss
		log "Restarting instance: #{@name}"
		system("#{jboss_run} restart #{@name}")
		log "Waiting for startup..."
	end
	
	def check_instance_exists_smf
		log "Verifying [#{@name}] is known by SMF..."
		system("svcs \"jboss:#{@name}\"")
		
		raise "[#{@name}] isn't in the list of SMF jboss: servers!" unless $? == 0
	end  	
	
	def do_restart_jboss_smf
		log "Restarting instance: #{@name}"
		system("/usr/sbin/svcadm enable \"jboss:#{@name}\"")
		log "Waiting for startup..."
	end
	
	def check_instance_exists_ubuntu
		log "Verifying [#{@name}] is known by jboss..."
		system("pgrep -lf jboss | grep \"\\-c #{@name}\"")
		
		raise "[#{@name}] isn't in the list of running servers!" unless $? == 0
	end  	

	def do_restart_jboss_ubuntu
		log "Restarting instance: #{@name}"
		worked = system("sudo nohup /usr/local/jboss/bin/run.sh -b 0.0.0.0 -c #{@name} &")
		log "Waiting for startup (system call returned #{worked})..."
	end
	
	case @mode
		when "smf"
			check_instance_exists_smf
			do_restart_jboss_smf
		when "ubuntu"
			check_instance_exists_ubuntu
			do_restart_jboss_ubuntu
		else
			check_instance_exists
			do_restart_jboss
	end
	
	while retries > 0
		sleep(15)
		
		log "Fetching test page #{test_path} (#{retries} attempts left)"
		
		begin
		  res = Net::HTTP.start("localhost", @port) {|http|
			  http.get(test_path)
        	  }
        rescue Errno::ECONNREFUSED => e
          log "Connection refused, still restarting?"
          res = nil
        end
        
		if (res.nil? or res.code.to_i != 200)
		  	log "[NOT OK] #{test_path} got #{res.code} expected 200"
		  	retries = retries - 1
		  	
		  	raise "Couldn't fetch test page!" unless retries > 0
		else
		  	log "[OK]     #{test_path} got #{res.code}"
		  	break
		end
	end
	
	log "Done. Jboss [#{@name}] has been restarted."
  
    raise "Could not verify deployment!" unless verify_deployment
    sleep(2)
    @state='running'
  end

  def verify_deployment
      ok = true
      @requests.each do |url, expected|
          res = Net::HTTP.start("localhost", @port) {|http|
            http.get(url)
          }
          if (res.code.to_i != expected)
             log "[NOT OK] #{url} got #{res.code} expected #{expected}"   
             ok = false
          else
             log "[OK]     #{url} got #{res.code}"
          end
      end
     return ok
  end


  # check whether this instance has no active requests.
  def is_idle?
    host="localhost:#{@port}"
    url = "http://#{host}/status?XML=true"
    log "Checking #{url}"
    xml = open(url){|f| f.read }
    doc= Document.new(xml)
    
    # guess at right connector
    httpconnector = doc.root.elements[3]    
    # now look for the right connector
    doc.root.elements.each do |connector|
      if (connector.attributes["name"] =~ /http/) then
        httpconnector = connector
      end
    end
    workers = httpconnector.elements["workers"].elements
    numconnections = 0;
    workers.each do |w|  
      if  ((w.attributes["stage"] =~ /S/ )&&  (w.attributes["currentUri"] !~ /\/status(.*)/ )) then
        numconnections = numconnections +1 
      end
    end
    log "Jboss instance #{name} has #{numconnections} active connections"
    return (numconnections == 0)    
  end
  
end

class Apache
  attr_accessor :name, :restart_command,:state
 
  def initialize(display_name,balancer,remotehost = nil)
     @name=display_name    
     @state='balanced'
     @port_balancer=balancer
     @remotehost=remotehost
  end
  
  def preconditions_ok?
  	unless File.can_write_to? @rewrite_map
  	  log "Can't write to #{@rewrite_map}"
	  return false 
  	end
	true
  end

  def reconfigure
    if (@remotehost != nil) then 
      # note the escaped redirection \\> ; otherwise the output will go onto the local host, not
      # the remote one!
      command = "ssh #{@remotehost} echo \"port #{@port_balancer.current_port}\" \\> #{@rewrite_map}"
    else
      command="echo \"port #{@port_balancer.current_port}\" > #{@rewrite_map}"
    end 
    log "Reconfiguring apache #{@name}: running #{command}"
    system command
    if ($? != 0) then
       raise("Failed to write to port map file!")
    end	
  end

  def restart
    reconfigure
    command = @restart_command
    if (@remotehost != nil) then
      command = "ssh #{@remotehost} " + command
    end
    log ("Restarting apache #{@name}: running #{command}")
    system command
    if ($? != 0) then
       raise("Apache restart command failed")
    end 	
  end

  def remove_server(port,status_printer)
     @port_balancer.remove_port(port)
     restart
     @state="unbalanced: #{@port_balancer.current_port}"
     status_printer.print_status
  end
  def add_server(port, status_printer)
    @port_balancer.add_port(port)
    #restart
    @state="balanced: #{@port_balancer.current_port}"
    status_printer.print_status
  end

  def use_server_exclusively(port, status_printer)
    log "Switching Apache #{name} to only serve to port #{port}"
    @port_balancer.use_exclusively(port)
    restart
    @state="unbalanced: #{@port_balancer.current_port}"
    status_printer.print_status
  end

  def use_balancer(status_printer)
    @port_balancer.use_balancer
    restart
    @state="balanced: #{@port_balancer.current_port}"
  end

  
end

class LoadBalancedPortTracker
  attr_reader :current_port

  # nasty hack that only really works properly with 2 servers
  def initialize(balancer_port, balanced_ports)
    @balancer_port= balancer_port
    @balanced_ports= balanced_ports
    @current_port=balancer_port
  end

  def remove_port(port)
    @balanced_ports.reject!{ |p| p == port}
    raise "All ports removed from loadbalancer!" if @balanced_ports.length==0
    @current_port= @balanced_ports[0]
  end

  def add_port(port)
    @balanced_ports << port
    @current_port= @balancer_port
  end

  def use_balancer
    @current_port = @balancer_port
  end
  def use_exclusively(port)
    @current_port = port	
  end

end


def print_usage
  puts "  Usage: deploy.rb {run | status | haenable}"
  puts "         deploy.rb {hahide | haexclusive | redeploy} jboss_instance_name"
  puts ""
  puts "   hahide:        remove this jboss from port balancing"
  puts "   haexclusive:   make this jboss handle all requests"
  puts "   haenable:      reset so all jbosses are handling requests"
  puts "   redeploy:      redeploy this jboss (you should use hahide first!!)"
  puts "   preconditions: check preconditions for deploying"
  puts "   deploy_static: redeploy existing static_war"
end


class File
  # Test whether the file is writable OR can be created
  def File.can_write_to?(filename)
    if File.exist? filename then
      File.writable? filename
    else
      File.writable?(File.dirname(filename))
    end 	
  end
end


deployer = Deployment.from_yaml_file('deployer.yaml')

# "run" was previously the default and only script behaviour
if ARGV[0] == "run"
  deployer.run
elsif ARGV[0] == "status"
  deployer.print_status
elsif ARGV[0] == "redeploy"
  deployer.deploy_war_to ARGV[1]
elsif ARGV[0] == "hahide"
  deployer.hide_jboss ARGV[1]
elsif ARGV[0] == "haexclusive"
  deployer.use_this_jboss_only ARGV[1]
elsif ARGV[0] == "haenable"
  deployer.use_all_jbosses
elsif ARGV[0] == "preconditions"
  puts deployer.preconditions_ok?
elsif ARGV[0] == "deploy_static"
  deployer.deploy_static
elsif ARGV[0] == "build_war"
  deployer.build_war
elsif ARGV[0] =~ /^(usage|--help|-\?)$/
  print_usage
else
  puts "You're doing it wrong!\n\n"
  print_usage
end