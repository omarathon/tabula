require 'fileutils'
require 'rexml/document'
require 'uri'
require 'open-uri'
require 'yaml'
require 'net/http'
require 'time'
require 'etc'
require 'haproxy'
require 'optparse'

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
       
       unless (has_version_number?)
       	 build_war_and_artifacts
       end

       deploy_war_to_scheduler
       deploy_build_artifacts
       deploy_war
       unset_running
     else
       exit 2
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
    
    arch_dir = @archive_dir || "/var/src/archive"
    
    unless File.exists? arch_dir and File.can_write_to? arch_dir
    	log "Can't write to #{arch_dir}"
    	return false
    end
    
    if (has_version_number?)
    	return false unless File.exists?(@warfile)
    	@artifacts.each do |artifact|
    	  return false unless File.exists?(artifact)
    	end
    end
    
    #gets apaches whose preconditions aren't ok, and checks that collection is empty.
    return false unless build_artifacts_preconditions_ok?
    return false unless @apaches.reject{|a| a.preconditions_ok? }.empty?
    return false unless @scheduler_jboss.nil? or @scheduler_jboss.preconditions_ok?
    return false unless all_jbosses.reject{|j| j.preconditions_ok? }.empty?
    return true
  end
  
  def set_branch(branch_name)
  	@branch = branch_name
  end
  
  def set_version(version_number)
  	@version_number = version_number
  	
  	arch_dir = @archive_dir || "/var/src/archive"
  	this_version_dir = "#{arch_dir}/#{@version_number}"
  	
  	war_name = @war_name || @app
  	
  	@warfile = link_file("#{this_version_dir}/#{war_name}.war", "#{war_name}.war")
  	
  	@artifacts = Array.new 
  	if !(@build_artifacts.nil?)
  	  @build_artifacts.each do |artifact_name, target|
  	    @artifacts.push(link_file("#{this_version_dir}/#{artifact_name}", artifact_name))
  	  end
  	end
  end
  
  def has_version_number?
  	!(defined?(@version_number)).nil?
  end
  
  def set_running
     @build_status= 'started'
     FileUtils.touch(@lockfile)
     print_status 
  end

  def build_war_and_artifacts
     @build_status= 'building war and artifacts'
     print_status
     
     arch_dir = @archive_dir || "/var/src/archive"
     checkout = "#{@checkout_base}/#{@app}"
     
     # Checkout the source
     # Git. Don't checkout afresh each time; do it in place.
     # If we need access to the previous checkout, we should just
     # store the commit ID in a file somewhere.
     # Assumes there's a cloned repo in #{checkout}; Puppet should ensure this.
     log "Performing a Git hard reset, clean, and a pull from branch #{@branch}"
     
     system("cd #{checkout}; git reset --hard HEAD; git clean -fdx; git pull")
     raise "Couldn't pull latest version of repository into #{checkout}" unless $? == 0
     
     system("cd #{checkout}; git checkout #{@branch} || git checkout --track origin/#{@branch}")
     raise "Couldn't checkout branch #{@branch}" unless $? == 0
     
     system("cd #{checkout}; git pull")
     raise "Couldn't pull latest version of repository into #{checkout}" unless $? == 0
     
	 # Build the application
	 
	 log "Building app in #{checkout}"
	 
	 ant = @ant_path || "/usr/local/ant/bin/ant"
     ant_target = @ant_package_target || "build-war"
	 
	 # TODO find a way to do this without a system call
	 system("cd #{checkout} ; #{ant} -q #{ant_target}")
	 raise "Ant build failed: giving up" unless $? == 0
	 
	 version = Time.now.strftime "%Y%m%d-%H%M%S"
	 
	 war_name = @war_name || @app	 
	 built_war = "#{checkout}/dist/#{war_name}.war"
	 
	 # Archive and move the newly built application
	 new_build = "#{arch_dir}/#{war_name}.war.#{version}"
	 FileUtils.cp built_war, new_build
	 
	 @warfile = link_file(new_build, "#{war_name}.war")
  	
  	 @artifacts = Array.new 
  	 if !(@build_artifacts.nil?)
  	   @build_artifacts.each do |artifact_name, target|
  	     built_artifact = "#{checkout}/dist/#{artifact_name}"
  	     
  	     # Archive and move the newly built artifact
  	     new_artifact = "#{arch_dir}/#{artifact_name}.#{version}"
  	     FileUtils.cp built_artifact, new_artifact
  	   
  	     @artifacts.push(link_file(new_artifact, artifact_name))
  	   end
  	 end
	 
     @build_status = "built war #{@warfile}; artifacts #{@artifacts}"
     print_status
  end
  
  def link_file(file, filename)
     arch_dir = @archive_dir || "/var/src/archive"
  	 current_link = "#{arch_dir}/#{filename}.current"
	 lastgood_link = "#{arch_dir}/#{filename}.lastgood"
	 
	 if File.exists? current_link
	 	lastgood = File.readlink(current_link)
	 
	 	FileUtils.rm current_link
	 
	 	if File.exists? lastgood_link
	 		FileUtils.rm lastgood_link
	 	end
	 	
	 	FileUtils.ln_s lastgood, lastgood_link
	 end
	 
	 FileUtils.ln_s file, current_link
	 
	 log "New version is #{file}"

     file
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
    apaches_for(jboss).each{ |a| a.remove_server(jboss,self)}
  end
  
  # point ALL apache requests at this jboss
  # (by ALL, we mean all Apaches that serve this JBoss' group)
  def use_this_jboss_only(jboss)
    jboss = find_jboss_by_name(jboss) unless jboss.is_a? Jboss
    log "Using only #{jboss.name}"
    apaches_for(jboss).each{|a|
      a.use_server_exclusively(jboss,self) 
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
  
  def build_artifacts_preconditions_ok?
    if @build_artifacts.nil?
      return true
    else
  	  log "Checking preconditions for deploy of build_artifacts"
  	  @build_artifacts.each do |artifact_name, target|
  	    target_temporary_dir = "#{target}.new"
  	    target_backup_dir = "#{target}.last"
  	    
  	    if File.exists? target_temporary_dir then
  		  log "#{artifact_name} temporary directory already exists: #{target_temporary_dir}"
  		  return false
  	    end
  	    
  	    target_parent_dir = File.dirname(target)
	
	    unless File.exists? target_parent_dir
		  log "#{artifact_name} target parent dir does not exist: #{target_parent_dir}"
		  return false
	    end
	
	    if File.exists?(target) then
		  unless File.can_write_to? target
		    log "Can't write to #{target}"
		    return false
	      end
	    else
		  unless File.can_write_to? target_parent_dir
		    log "Can't write to #{target_parent_dir}"
		    return false
		  end
	    end
	
	    true
  	  end
  	end
  end

  def deploy_build_artifacts
    if !@build_artifacts.nil?
  	  def unpack_tgz src, dest
  		system("mkdir #{dest} && gtar -C #{dest} --strip-components=1 -zxf #{src}")
  		raise "Couldn't unpack archive #{src}" unless $? == 0
  	  end
  	  
  	  arch_dir = @archive_dir || "/var/src/archive"
  	    
      @build_artifacts.each do |artifact_name, target|
        target_temporary_dir = "#{target}.new"
  	    target_backup_dir = "#{target}.last"
  	    
  	    # create a temporary place for the new files
  	    log "Creating new files in #{target_temporary_dir}"
  	    
  	    unpack_tgz "#{arch_dir}/#{artifact_name}.current", target_temporary_dir
  	
  	    if File.exists? target_backup_dir
  		  log "Removing previous backup directory #{target_backup_dir}"
  		  FileUtils.rm_r target_backup_dir
  	    end
  	    
  	    # Move the new content into place
  	    
  	    log "Replacing build artifact #{target}"
  	    
  	    if File.exists? target
  		  FileUtils.mv target, target_backup_dir
  	    end
  	    
  	    FileUtils.mv target_temporary_dir, target
      end
    end
  	  
  	@build_status = 'built war and deployed build artifacts'
    print_status
  end

  def unset_running
     @build_status='complete'
     File.delete(@lockfile)
     print_status
  end

end


class Jboss
  attr_accessor :name, :group_name, :state, :deployment_state, :port, :war_name, :haproxy_name

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
    test_path = @test_path
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
            http.read_timeout = 900
          	http.get(test_path)
          }
        rescue Errno::ECONNREFUSED => e
          res = class << res
            def code() "connection refused" end
          end
        end
        
        if (res.code.to_i != 200)
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
          begin
		    res = Net::HTTP.start("localhost", @port) {|http|
              http.read_timeout = 900
              http.get(url)
            }
          rescue Errno::ECONNREFUSED => e
            res = class << res
              def code() "connection refused" end
            end
          end
        
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
 
  def initialize(display_name,balancer)
     @name=display_name    
     @state='balanced'
     @port_balancer=balancer
  end
  
  def preconditions_ok?
  	@port_balancer.preconditions_ok?
  end

  def restart
    @port_balancer.reconfigure
    
    if (@port_balancer.restart_required?) then
	    command = @restart_command
	    log ("Restarting apache #{@name}: running #{command}")
	    system command
	    if ($? != 0) then
	       raise("Apache restart command failed")
	    end
	end
  end

  def remove_server(jboss, status_printer)
     @port_balancer.remove_jboss(jboss)
     restart
     @state="unbalanced: #{@port_balancer.current_jboss}"
     status_printer.print_status
  end
  def add_server(jboss, status_printer)
    @port_balancer.add_jboss(jboss)
    #restart
    @state="balanced: #{@port_balancer.current_jboss}"
    status_printer.print_status
  end

  def use_server_exclusively(jboss, status_printer)
    log "Switching Apache #{name} to only serve to jboss #{jboss.name}"
    @port_balancer.use_exclusively(jboss)
    restart
    @state="unbalanced: #{@port_balancer.current_jboss}"
    status_printer.print_status
  end

  def use_balancer(status_printer)
    @port_balancer.use_balancer
    restart
    @state="balanced: #{@port_balancer.current_jboss}"
  end

  
end

class LoadBalancer
  attr_reader :current_jboss
  
  def preconditions_ok?
    raise NotImplementedError
  end
  
  def restart_required?
    raise NotImplementedError
  end
  
  def reconfigure
    raise NotImplementedError
  end

  def remove_jboss(jboss)
    raise NotImplementedError
  end

  def add_jboss(jboss)
    raise NotImplementedError
  end

  def use_balancer
    raise NotImplementedError
  end
  
  def use_exclusively(jboss)
    raise NotImplementedError
  end
  
  private

  def initialize
  end
end

class LoadBalancedPortTracker < LoadBalancer
  attr_accessor :balanced_ports, :balancer_port, :current_port, :rewrite_map

  # nasty hack that only really works properly with 2 servers
  def initialize(balancer_port, balanced_ports)
    @balancer_port=balancer_port
    @balanced_ports=balanced_ports
    @current_jboss=balancer_port
  end
  
  def preconditions_ok?
  	puts @rewrite_map
  
  	unless File.can_write_to? @rewrite_map
  	  log "Can't write to #{@rewrite_map}"
	  return false 
  	end
	true
  end
  
  def restart_required?
  	false
  end

  def reconfigure
    command="echo \"port #{@current_jboss}\" > #{@rewrite_map}"
 
    log "Reconfiguring apache: running #{command}"
    system command
    if ($? != 0) then
       raise("Failed to write to port map file!")
    end	
  end

  def remove_jboss(jboss)
    @balanced_ports.reject!{ |p| p == jboss.port}
    raise "All ports removed from loadbalancer!" if @balanced_ports.length==0
    @current_jboss= @balanced_ports[0]
  end

  def add_jboss(jboss)
    @balanced_ports << jboss.port
    @current_jboss = @balancer_port
  end

  def use_balancer
    @current_jboss = @balancer_port
  end
  
  def use_exclusively(jboss)
    @current_jboss = jboss.port	
  end

end

class HAProxySocketBalancer < LoadBalancer
  def initialize(balancer_port, balanced_group, balanced_servers, haproxy_socket)
  	@balanced_group = balanced_group
    @balanced_servers = balanced_servers
    @current_jboss = @balanced_servers
    @haproxy = HAProxy::SocketReader.new(haproxy_socket)
  end
  
  def preconditions_ok?
	# Check that we can find the backend
	backend_found = @haproxy.backends.any? do |backend| 
		backend[:pxname] == @balanced_group
	end
	
	unless backend_found
		log "Could not find backend! " + @balanced_group
		return false
	end
	
	# Check that all servers are UP, and their names match @balanced_servers
	all_servers = @haproxy.servers.select do |server|
		server[:pxname] == @balanced_group
	end
	
	all_servers_up = all_servers.all? do |server|
		# Just warn if server isn't UP for the moment
		if server[:status] != "UP" then
			log("WARNING: " + server[:svname] + " is " + server[:status])
		end
		
		server[:status] == "UP"
	end
	
	unless all_servers_up
		return false
	end
	
	all_server_names = all_servers.collect do |server|
		server[:svname]
	end
	
	if all_server_names.length != @balanced_servers.length then
		# Just a warning at the moment
		log("WARNING: There are " + (all_server_names.length) + " servers in this group (" + @balanced_group + ") but we are only managing " + (@balanced_servers.length))
	end
	
	all_servers_found = @balanced_servers.all? do |server_name|
		all_server_names.include?(server_name)
	end
	
	unless all_servers_found
		log("Servers found: [" + all_server_names.join(",") + "]; servers expected: [" + @balanced_servers.join(",") + "]")
		return false
	end
  
	true
  end
  
  def restart_required?
  	false
  end

  def reconfigure
    # Probably a better way to do this...
    
    # Enable all servers in @current_jboss
    @current_jboss.each do |name|
    	@haproxy.enable_server(@balanced_group, name)
    end
    
    # Disable all servers not in @current_jboss
    servers_to_disable = @balanced_servers.reject{ |server| @current_jboss.include?(server) }
    
    servers_to_disable.each do |name|
    	@haproxy.disable_server(@balanced_group, name)
    end
  end

  def remove_jboss(jboss)
    @current_jboss.reject!{ |p| p == jboss.haproxy_name }
    raise "All jbosses removed from loadbalancer!" if @current_jboss.length==0
  end

  def add_jboss(jboss)
    @current_jboss << [jboss.haproxy_name]
  end

  def use_balancer
    @current_jboss = @balanced_servers
  end
  
  def use_exclusively(jboss)
    @current_jboss = [jboss.haproxy_name]
  end

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

opts = OptionParser.new do |opts|
  opts.banner    = "Usage:  deploy.rb [options] {run | status | haenable | preconditions | deploy_build_artifacts | build_war | usage}"
  opts.separator   "        deploy.rb [options] {redeploy | hahide | haexclusive} jboss_instance_name"
  
  opts.separator ""
  
  opts.separator "        run:                         Deploy the application, static and all configuration"
  opts.separator "        status:                      Show the current deploy status"
  opts.separator "        hahide:                      Remove this jboss from port balancing"
  opts.separator "        haexclusive:                 Make this jboss handle all requests"
  opts.separator "        haenable:                    Reset so all jbosses are handling requests"
  opts.separator "        redeploy:                    Redeploy this jboss (you should use hahide first!!)"
  opts.separator "        preconditions:               Check preconditions for deploying"
  opts.separator "        deploy_build_artifacts:      Redeploy any non-war artifacts from the build"
  opts.separator "        build_war:                   Build the war file for the application"
  opts.separator "        usage:                       Show this message"

  opts.separator ""
  opts.separator "Options:"
  opts.separator ""

  opts.on("--version=DIR","Provide a directory containing all of the artifacts instead of building from source") do |dir|
    deployer.set_version dir
  end
  
  opts.on("--branch=NAME","The branch from the repository to build from, defaults to the branch in the configuration file") do |name|
    deployer.set_branch name
  end
  
  opts.on_tail("-?", "--help", "Show this message") do
    puts opts
    exit
  end

  opts
end.parse!

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
  if deployer.preconditions_ok? then
  	exit 0
  elsif
  	exit 2
  end
elsif ARGV[0] == "deploy_build_artifacts"
  deployer.deploy_build_artifacts
elsif ARGV[0] == "build_war"
  deployer.build_war
elsif ARGV[0] == "usage"
  puts opts
  exit
else
  puts opts
  exit 10
end
