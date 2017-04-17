#!/bin/sh
#You must have access to an solaris sparc system to build this agent
rm -rf kwagent
echo "Cleanup remote files in case"
ssh someuser@gate.polarhome.com -p725 "rm -rf agent;mkdir agent"
echo "Copy sources to solaris build host"
scp -P725 -C ../platforms.properties someuser@gate.polarhome.com:agent/
scp -r -P725 -C ../../kwatee_agent/src someuser@gate.polarhome.com:agent/
scp -r -P725 -C ../../kwatee_agent/solaris_sparc someuser@gate.polarhome.com:agent/
echo "Start build process"
ssh someuser@gate.polarhome.com -p725 "export PATH=/opt/csw/gnu:/opt/csw/gcc4/bin:/opt/csw/bin:/usr/ccs/bin;cd agent/solaris_sparc;gmake clean all;./kwagent --version"
echo "Download built kwagent"
scp -P725 someuser@gate.polarhome.com:agent/solaris_sparc/kwagent ./
echo "Clean up remote files"
ssh someuser@gate.polarhome.com -p725 "rm -rf agent"
