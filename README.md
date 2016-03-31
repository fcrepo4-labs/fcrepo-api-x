# DCS Vagrant & Docker Images 

This document is for setting up Vagrant and Docker images. For more documentation on how to use the system once it's up, see  [User Documentation](USERDOC.md)


## Prerequisites 

The below instruction assumes you have downloaded and installed Vagrant and Virtualbox. If not, please download and install Vagrant and Virtualbox before attempting the [Quickstart](#quickstart) instructions.

* [Vagrant Download](https://www.vagrantup.com/downloads.html) (tested on version 1.8.1)
* [VirtualBox Download](https://www.virtualbox.org/wiki/Downloads) (tested on version 5.0.14 for OS X hosts, amd64; _not_ the extension pack)


## Quickstart
By default, this will launch a Virtualbox VM running Ubuntu 14.04 LTS (a "docker host VM") and will run docker 
images within this VM.  Additionally, this VM will expose ports on your local machine (e.g. for viewing with a web 
browser).  This pattern works on all platforms (Linux, Mac OS, and Windows).  Linux hosts may choose to configure 
Vagrant to launch Docker on their own machine, rather than launching a separate VM.  This is discussed in a separate 
section.

* Be default, the Virtualbox VM will expose ports `8080` and `8181` on your local machine.  If this will conflict
  with anything you have running, you may change these ports by editing `DockerHostVagrantFile` or turn off
  port forwarding entirely.  If the default port forwarding is OK, you do not need to do anything.
* By default, the Virtualbox VM creates a private network on your machine, and is accessible at the IP address 
 `10.11.8.11`.  If this poses a problem, you mat edit `DockerHostVagrantFile` to change this IP, or turn it off entirely.
  If the default VM network address of `10.11.8.11` is OK, then you do not need to do anything.
    * If you wish, you may give `10.11.8.11` a human readable hostname (like `vagrant.local` or `dockerhost`) by 
      editing the hosts file on your local machine (`/etc/hosts` on unix-like systems).  
* In the project's root directory (the one containing `Vagrantfile`), run `vagrant up`
	* You'll see lots of text fly by as it launches a VM, builds a docker image, and starts.  
	  This can take several minutes.
* In order to share files with the VM (e.g. to deposit packages, read log files, update configuration), you need to 
  mount a network share provided by the VM.  This is accessible at `\\10.11.8.11\shared`.  
* When you are finished, type `vagrant halt`.  Type `vagrant up` at any time to re-launch the docker images, with your
  data intact.

## Verification
These verification steps can be used to assure that the docker instance(s) are running and are accessible.  The
instructions assume default settings, so you will need to adjust accordingly if you, say, changed the port numbers.
Additionally, because the VM appears to your local machine as a separate networked resource, for any URL you can 
use something like `http://10.11.8.11` (or `http://dockerhost` if you gave it a name like `dockerhost` in your hosts
file) instead of `http://localhost`.

* Point your browser to [http://localhost:8181/system/console/components](http://localhost:8181/system/console/components).
	* You should see an Apache Karaf page
	* You should see a list of org.dataconservancy components with status `active`
* point your browser to Fedora at [http://localhost:8080/fcrepo/rest](http://localhost:8080/fcrepo/rest)
	* You should see an empty root container
* Drop some packages into the deposit dir(s) and watch them ingest
	* The default deposit directory is available on the network share as `\\10.11.8.11\shared\package-ingest\packages`, 
	  packages deposited here will be deposited into Fedora's root container;  
      [http://localhost:8080/fcrepo/rest](http://localhost:8080/fcrepo/rest)
	* The service is configured to wait 30 seconds before ingesting a package
	    * You can change this by going into [OSGI-> components](http://localhost:8181/system/console/components) menu 
	      of the webconsole, clicking on the wrench icon by the by PackageFileDepositWorkflow service, and editing the 
          value of the package poll interval, in milliseconds.  Default is 30000 (30 seconds).  Click 'save', and the 
          changes will be in effect immediately
	* You may wish to set up e-mail notifications (see below)
* Once a package has been processed, it will disappear from the deposit directory.  If it has failed, it will appear in 
  the package fail directory (`\\10.11.8.11\shared\package-ingest\packages`).  Otherwise, it's in Fedora!

## E-mail notification Configuration
By default, the service is configured to simply log deposits to the karaf log.  You may want to set it up to send 
e-mails once a deposit is finished.  You can do this in two ways:

### Via webconsole
This method of storing configuration is persistent within a single container instance.  The container can be started/stopped, but the configuration is persisted in that container's filesystem locally.  If the container is destroyed, a new container will not retain configuration created through the webconsole.  Also, any configuration via files (see below) will override the webconsole config if the container is re-started.

 - Navigate to [OSGi->Configuration](http://localhost:8181/system/console/configMgr)
 - Find `org.dataconservancy.packaging.ingest.camel.impl.EmailNotifications` and click on it to bring up a form.
 - Fill out all the requested values and click 'save'
 - Go to [OSGi->Components](http://localhost:8181/system/console/components) and click the stop button (black square) next to LoggerNotifications.


### Via configuration files
This method of storing configuration is persistent across containers - you can completely erase a container, create a new
one, point it to the config, and it should work as configured.  We'll use the convention `${SHARED}` to refer to the 
shared folder location (typically `\\10.11.8.11\shared`)
* Create a `${SHARED}/karaf/deploy` directory.
    * Create a file in the deploy directory named `org.dataconservancy.packaging.ingest.camel.impl.EmailNotifications.cfg`
      with the following contents: 
      <pre>
      mail.smtpHost = YOUR_SMTP_SERVER (e.g. smtp.gmail.com)
      mail.smtpUser = YOUR_EMAIL
      mail.smtpPass = YOUR_PASSWORD
      mail.from = FROM_ADDRESS
      mail.to = TO_ADDRESS
      </pre>
* Create a file in the deploy directory named `org.dataconservancy.packaging.ingest.camel.impl.LoggerNotifications.cfg` 
  with the following contents: <pre>
  service.ranking = -2
  </pre>
    * This explicitly disables/de-prioritizes the default logging notification.  Ideally, this step wouldn't be 
      necessary, but testing has revealed that the notification implementation won't be swapped out until this happens.


## Setting up new deposit locations
In order to deposit into a non-root container in Fedora (like adding collections to a project), you need to add a package
deposit workflow that monitors a directory and ingests packages into a given Fedora container.

### Via configuration files
This method of storing configuration is persistent across containers - you can completely erase a container, create a new one, point it to the config, and it should work as configured.   Again, we'll use the convention `${SHARED}` to refer 
to the shared folder location (typically `\\10.11.8.11\shared`)

 * Create a `${SHARED}/karaf/deploy` directory, if one doesn't exist already.  This is where you will put the 
   Karaf configuration
 * Create a new directory for packages to be deposited into the container.  It has to be somewhere underneath
   `${SHARED}/package-ingest` Let's call this `${SHARED}/package-ingest/myPackages` for the sake of argument.
 * Optionally, create a directory for failed packages.  Only do this if you want to keep track of where failed packages
   came from.  Otherwise, it's fine to use the same failed package directory for every deposit workflow (e.g. 
   `${SHARED}/package-ingest/failed-packages`
 * Create a text file in the `deploy` directory named 
   `org.dataconservancy.packaging.ingest.camel.impl.PackageFileDepositWorkflow-myPackages.cfg`.  
    * The part after the dash, `-myPackages.cfg` has to be unique for each workflow, and should be an informative name,
	   like `-ELOKAProject.cfg` or `-cowImagesCollection.cfg`.   
	* Populate the config file with the following content: <pre>
		deposit.location = http://CONTAINER-URI
        package.deposit.dir = /shared/package-ingest/PATH-TO-PACKAGE_DIR
        package.fail.dir = /shared/package-ingest/failed-packages
        package.poll.interval.ms = 1000
        </pre>
        where `CONTAINER-URI` is a URI of a Fedora container (e.g. from a notification e-mail), `PATH-TO-PACKAGE-DIR` 
        is the relative path to an package deposit dir.  
    * Important:  The deposit and fail dirs are filesystem paths _on the docker container_ and therefore always
	  start with `/shared/package-ingest`.
        * The `package.poll.interval.ms` is optional.  Default is 30 seconds (30000) if unspecified.

## Editing `DockerHostVagrantfile`
The `DockerHostVagrantfile` defines the characteristics of the docker host VM, including how it exposes networked
resources to the local host machine running vagrant, and how it shares folders and files.  If vagrant used on a Linux
system set up directly provision docker images on that host (instead of in the VM), this section is irrelevant to 
those setups.

### Networking
#### Private network
The docker host VM appears as a networked machine with its own IP address, in a private IP address space that cannot
be routed to the outside world.  By default, it is given a fixed IP address of `10.11.8.11`.  If this address
conflicts with the local host, it may be changed by editing the line:
<pre>
config.vm.network "private_network", ip: "10.11.8.11"
</pre>
If you want to disable the private network entirely, you may comment out this line.  Note, if this is commented out,
this entirely disables file sharing from the VM to the host.

#### Port forwarding
Completely separate from any private network settings, ports from the docker host VM can be forwarded to the local host.
Forwarding port `8080` from the VM will bind to port `8080` on the local machine, and appear as if there is a service
at port `8080` running on the local machine.  If you _already_ have a service running at that port on your local 
machine, the ports will conflict, and the VM will not be able to bind to it.  In these cases, you may wish to change the
port numbers that are exposed on the local machine.   At a minumum, change the `host` ports in:
<pre>
  config.vm.network "forwarded_port",
    guest: 8080, host: 8080
  config.vm.network "forwarded_port",
    guest: 8181, host: 8181
</pre>

### File sharing
By default, all data, configuration files, logs, etc reside on the VM, in the `/shared` directory.  If the VM is
deleted, then this data is lost.  For testing and evaluation, this is usually perfectly fine.  It is the least 
problematic setup, and easiest to use.  It is possible, however, to have the data reside on your local machine as its
primary location.  In this situation, you machine would share the files with the VM.  This section describes such 
situations.  Please ignore it if you have no desire to have your local machine as the primary storage location of
shared data files.

#### Virtualbox shared folders
This is the easiest to set up and the most uniform across platforms, but is the most problematic due to limitations in the
way Virtualbox implements shared folders.  You'll need to uncomment the `Virtualbox shared folders` section of 
the DockerHostVagrantfile

By default the following directories on your local filesystem are shared with the DCS Vagrant and Docker images
* `/shared/package-ingest`: Subdirectories of this directory are where you'll place packages for deposit and where failed
   packages will appear.
* `/shared/jetty`: This directory will contain the logs for the Fedora instance that your packages are deposited to.
* `/shared/karaf`: This directory will contain the logs for the Karaf instance that runs the Package Ingest Service.

_Shared_ means that the contents of these directories can be read from or written to by your local operating system _and_ the DCS Vagrant and Docker images.  As the virtual machines update content in these directories, you can see the updates (e.g. `tail -f /shared/karaf/log/karaf.log`).  Likewise the virtual machines can see the content you place in these shared directories (e.g. `cp my-package.tar.gz /shared/package-ingest/packages`).

If you want to use different paths other than those above, open `DockerHostVagrantfile` and edit the section:

<pre>
  # Local folders for packages and karaf config
  config.vm.synced_folder "/shared/package-ingest", "/shared/package-ingest",
     mount_options: ["dmode=777", "fmode=666"]
  config.vm.synced_folder "/shared/karaf", "/shared/karaf",
     mount_options: ["dmode=777", "fmode=666"]
  config.vm.synced_folder "/shared/jetty", "/shared/jetty",
     mount_options: ["dmode=777", "fmode=666"]
</pre>

The left and right parameters to `config.vm.synced_folder` govern where these directories reside on the local and remote (virtual machine) file systems respectively.  If you wanted to keep everything under a specific user's home directory, for example, you could replace the left parameters with `/home/esm/packageingestservice-runtime/package-dir`, `/home/esm/packageingestservice-runtime/karaf`, and `/home/esm/packageingestservice-runtime/jetty`:

<pre>
    # Local folders for packages and karaf config
  config.vm.synced_folder "/home/esm/packageingestservice-runtime/package-dir", "/shared/package-ingest",
     mount_options: ["dmode=777", "fmode=666"]
  config.vm.synced_folder "/home/esm/packageingestservice-runtime/karaf", "/shared/karaf",
     mount_options: ["dmode=777", "fmode=666"]
  config.vm.synced_folder "/home/esm/packageingestservice-runtime/jetty", "/shared/jetty",
     mount_options: ["dmode=777", "fmode=666"]
</pre>


*_You need to create these directories yourself_*

*_Do not change the right-hand parameters_*

After verifying that the services are up and running, you should be able to deposit a new package by copying it to `/home/esm/packageingestservice-runtime/package-dir/packages`, and see log files appear in `/home/esm/packageingestservice-runtime/jetty/logs` and `/home/esm/packageingestservice-runtime/karaf/log`.

#### NFS or SMB shared folders
Alternate NFS or SMB configuration of LOCAL folders shared with the VM. 
This causes your local machine to serve as an NFS or SMB server, sharing
the contents of the specified directory with the VM.  *nix and MacOS
hosts must use NFS, Windows must use SMB.  Windows hosts must have
 PowerShell 3 installed

This option should provide better performance and reliability than
Virtualbox file shares, but may have security implications as it turns your
local host into a file server.

To enable this, un-comment the following section of `DockerHostVagrantfile`
<pre>
  if Vagrant::Util::Platform.windows?
    config.vm.synced_folder "/mySharedFolder", "/shared", type: "smb"
  else
    config.vm.synced_folder "/mySharedFolder", "/shared", 
    :nfs => true,
    :mount_options => ['rw']
  end
</pre>

.. where `mySharedFolder` is the name of a local folder you are sharing with the VM.  You may need to adjust 
your firewall settings to allow NFS or SMB traffic to reach the docker host VM, which is available at the default address
of `10.11.8.11`.

