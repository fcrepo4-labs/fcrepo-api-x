# API-X development

This repository contains development and proof-of-concept code for API-X.  API-X may
be demonstrated in action by running Vagrant using the included vagrantfile.  This will
launch a VM into which several docker containers will be run, representing API-X and various
backend services (e.g. fedora itself, message routing, API-X extensions, etc)

## API-X POC/demo services
A description of API-X services can be found in [poc-services/README.md](poc-services/README.md)

## Prerequisites 

Building the code requires Maven 3.x, and Java 8.

Running API-X in Vagrant for demonstration/development purposes requires Vagrant and Virtualbox.  Linux users can avoid
installing Virtualbox if they use their own local installed Docker engine to run containers.
* [Vagrant Download](https://www.vagrantup.com/downloads.html) (tested on version 1.8.1)
* [VirtualBox Download](https://www.virtualbox.org/wiki/Downloads) (tested on version 5.0.14 for OS X hosts, amd64; _not_ the extension pack)

## Quickstart
By default, this will launch a Virtualbox VM running Ubuntu 14.04 LTS (a "docker host VM") and will run docker 
images within this VM.  Additionally, this VM will expose ports on your local machine (e.g. for viewing with a web 
browser).  This pattern works on all platforms (Linux, Mac OS, and Windows).  Linux hosts may choose to configure 
Vagrant to launch Docker on their own machine, rather than launching a separate VM.  This is discussed in a separate 
section.

### TL;DR
* `mvn install`
* `vagrant up`
* try `vagrant up` again if something fails (e.g. faileed download)

### Starting/Stopping
* By default, the Virtualbox VM creates a private network on your machine, and is accessible at the IP address 
 `10.11.8.11`.  If this poses a problem, you may edit `DockerHostVagrantFile` to change this IP, or turn it off entirely.
  If the default VM network address of `10.11.8.11` is OK, then you do not need to do anything.
    * If you wish, you may give `10.11.8.11` a human readable hostname (like `vagrant.local` or `dockerhost`) by 
      editing the hosts file on your local machine (`/etc/hosts` on unix-like systems).  
* In the project's root directory (the one containing `Vagrantfile`), run `mvn install`, then vagrant up`
	* You'll see lots of text fly by as it launches a VM, builds a docker image, and starts.  
	  This can take several minutes.  Subsequent runs will be much faster as once the docker images
	  have been built, there is no reason to re-build them unless changed (e.g. by a developer)
* Optionally, in order to share files with the VM (e.g. read log files, update configuration), you may wish to
  mount a network share provided by the VM.  This is accessible at `\\10.11.8.11\shared`.  This can
  be convenient for inspecting data files of various services.
* When you are finished, type `vagrant halt`.  Type `vagrant up` at any time to re-launch the docker images, with your
  data intact.
* Since docker images are built from scratch if they don't already, exist, and downloading content can always
  fail due to network effects, you may end up with one or more failed image buildds.  If this happens, just run
  `vagrant up` again.  It will attempt to build any images that have not successfully resulted in running containers

## Verification
These verification steps can be used to assure that the docker instance(s) are running and are accessible.  The
instructions assume default settings, so you will need to adjust accordingly if you, say, changed the port numbers.
Additionally, because the VM appears to your local machine as a separate networked resource, for any URL you can 
use something like `http://10.11.8.11` (or `http://dockerhost` if you gave it a name like `dockerhost` in your hosts
file) instead of `http://localhost`.

* Point your browser to [http://10.11.8.11:8080/rest](http://10.11.8.11:8080/rest).
	* You should see Fedora, with an empty root container
	* Create an object through Fedora's UI
* point your browser to Fuseki at [http://10.11.8.11:3030/fcrepo](http://10.11.8.11:3030/fcrepo)
	* You should see some triples!

## Working with Vagrant and Docker
This section gives some useful commands for interacting with the api-x docker containers via Vagrant at the
command line.  Note:  the Vagrant docker provider is a little quirky, and doesn't seem to work entirely as advertised
all the time.

All Vagrant commands assume you are in the root of the fcrepo-api-x reposiory working directory.

### See the status of containers or host vm
Run `vagrant status` to just get the status of all docker containers
Run `vagrant global-status` to get the status of docker containers and the host VM _and their identities_.
The identity of the docker host (available from global-status) is particularly important if you want to ssh into it


<pre>
$ vagrant status
$ vagrant global-status
id       name                       provider   state   directory
--------------------------------------------------------------------------------------------
34ff311  dockerhost                 virtualbox running /home/me/fcrepo-api-x
6161192  fcrepo                     docker     running /home/me/fcrepo-api-x
f58adfb  route-indexing-triplestore docker     running /home/me/fcrepo-api-x
5394c08  fuseki                     docker     running /home/me/fcrepo-api-x
</pre>

### Start and stop docker containers, or the VM
* To start all containers (building images from scratch if necessary), `vagrant up`
   * This is an idempotent operation, call it as many times as necessary.
* To stop a container `vagrant halt <name>`
* To completely remove a container (including the image) `vagrant destroy <name>`
* To stop the docker host vm `vagrant halt <id>`, where the id is the hexadecimal id from `vagrant global-status`
* To delete all data from the docker host vm, `vagrant destroy <id>`
* To destroy the entire world:
  * Launch the VirtualBox console `virtualbox`, click on the host vm, stop it, then remove it (say 'yes' to delete files)
  * `rm -rf .vagrant`
  * `rm -rf ~/.vagrant.d/*`

### SSH into the VM
Use `vagrant global-status` to get dockerhost's id, then
`vagrant ssh 34ff311` (use the id given).

This will bring you into the virtual machine.  All docker images that persist stuff to disk will write into some
subdirectory of `/shared`.  From within the VM, you can use regular docker commands to manually interact with the containers,
creat new ones, etc.  For example:
<pre>
$ vagrant ssh 34ff311
vagrant@vagrant-ubuntu-trusty-64:~$ docker images
REPOSITORY                            TAG                 IMAGE ID            CREATED             VIRTUAL SIZE
apix-poc/service-fuseki               latest              88d1aaaa89b8        18 minutes ago      138.9 MB
apix-poc/service-fcrepo               latest              5331be6f8825        38 minutes ago      154.4 MB
apix-poc/route-indexing-triplestore   latest              871c218df2bc        44 minutes ago      133.1 MB
apix-poc/karaf                        latest              9ac45354c674        45 minutes ago      144.1 MB
apix-poc/java                         latest              e4da5f99e763        About an hour ago   109 MB
alpine                                3.3                 9a686d8dd34c        4 weeks ago         4.798 MB
</pre>

### View the log output of the service
Do `vagrant docker-logs <name>`


## Networking
### Private network
The docker host VM appears as a networked machine with its own IP address, in a private IP address space that cannot
be routed to the outside world.  By default, it is given a fixed IP address of `10.11.8.11`.  If this address
conflicts with the local host, it may be changed by editing the line:
<pre>
config.vm.network "private_network", ip: "10.11.8.11"
</pre>
If you want to disable the private network entirely, you may comment out this line.  Note, if this is commented out,
this entirely disables file sharing from the VM to the host.

### Port forwarding
Completely separate from any private network settings, ports from the docker host VM can be forwarded to the local host.
This is disabled by default.  To enable, edit `DockerhostVagrantfile` to add the desired ports:
<pre>
  config.vm.network "forwarded_port",
    guest: 8080, host: 8080
  config.vm.network "forwarded_port",
    guest: 3030, host: 3030
</pre>

### File sharing
By default, all data, configuration files, logs, etc reside on the VM, in the `/shared` directory.  If the VM is
deleted, then this data is lost.  For testing and evaluation, this is usually perfectly fine.  It is the least 
problematic setup, and easiest to use.  It is possible, however, to have the data reside on your local machine as its
primary location.  In this situation, you machine would share the files with the VM.  This section describes such 
situations.  Please ignore it if you have no desire to have your local machine as the primary storage location of
shared data files.

### Virtualbox shared folders
This is the easiest to set up and the most uniform across platforms, but is the most problematic due to limitations in the
way Virtualbox implements shared folders.  For example, memory mapped files (`mmap()`) always fail.  As Modeshape/infinispan
utilized memory mapped Files, Fedora cannot be run within virtualbox shared folders.


To share specific directories, open `DockerHostVagrantfile` and add something like:

<pre>
  # Local folders for packages and karaf config
  config.vm.synced_folder "/path/to/my/fcrepo-data", "/shared/fcrepo-data",
     mount_options: ["dmode=777", "fmode=666"]
</pre>


*_You need to create any local directories yourself_*

*_Do not change the right-hand parameters_*


### NFS or SMB shared folders
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

