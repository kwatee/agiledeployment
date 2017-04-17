AGENT_VERSION=`cat platforms.properties | grep -e 'version=.*' | sed 's/version=//'`
REPO:=./repo/$(AGENT_VERSION)

local_agents:
	@echo "\nBuilding macos\n=============="
	@pushd macos;sh -c "make all";popd
	@echo "\nBuilding linux_64\n================="
	@pushd linux_64;sh -c "make all";popd
	@echo "\nBuilding linux_x86\n=================="
	@pushd linux_x86;sh -c "make all";popd
	@echo "\nBuilding windows\n================"
	@pushd windows;sh -c "make all";popd
	@echo "\nBuilding win64\n=============="
	@pushd win64;sh -c "make all";popd
	@echo "\nBuilding solaris_x86\n===================="
	@pushd solaris_x86;sh x86make.sh;popd

remote_agents:
	@echo "\nBuilding aix\n============"
	@pushd aix;sh aixmake.sh;popd
	@echo "\nBuilding solaris_sparc\n======================"
	@pushd solaris_sparc;sh sparcmake.sh;popd

deploy:
	@echo "\nDeploy platforms\n================"
	@echo "Version: " $(AGENT_VERSION)
	@cp platforms.properties $(REPO)/
	@cp macos/kwagent $(REPO)/macosx_x86/ && echo "macosx_x86"
	@cp linux_64/kwagent $(REPO)/linux_64/ && echo "linux_64"
	@cp linux_x86/kwagent $(REPO)/linux_x86/ && echo "linux_x86"
	@cp windows/kwagent.exe $(REPO)/win32/ && echo "win32"
	@cp win64/kwagent64.exe $(REPO)/win64/ && echo "win64"
	@cp solaris_x86/kwagent $(REPO)/solaris_x86/ && echo "solaris_x86"
	@cp aix/kwagent $(REPO)/aix/ && echo "aix"
	@cp solaris_sparc/kwagent $(REPO)/solaris_sparc/ && echo "solaris_sparc"

clean:
	@pushd macos;sh -c "make clean";popd
	@pushd linux_64;sh -c "make clean";popd
	@pushd linux_x86;sh -c "make clean";popd
	@pushd windows;sh -c "make clean";popd
	@pushd win64;sh -c "make clean";popd
	@pushd aix;sh -c "make clean";popd
	@pushd solaris_sparc;sh -c "make clean";popd
	@pushd solaris_x86;sh -c "make clean";popd

local: clean local_agents deploy

all: clean local_agents remote_agents
