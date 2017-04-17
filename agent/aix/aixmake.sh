#!/bin/sh
#You must have access to an aix system to build this agent
rm -rf kwagent
echo "Cleanup remote files in case"
ssh someuser@gate.polarhome.com -p775 "rm -rf agent;mkdir agent"
echo "Copy sources to aix build host"
scp -P775 ../platforms.properties someuser@gate.polarhome.com:agent/
scp -r -P775 ../../kwatee_agent/src someuser@gate.polarhome.com:agent/
scp -r -P775 ../../kwatee_agent/aix someuser@gate.polarhome.com:agent/
echo "Start build process"
ssh someuser@gate.polarhome.com -p775 "cd agent/aix;gmake clean all;./kwagent --version"
echo "Download built kwagent"
scp -P775 someuser@gate.polarhome.com:agent/aix/kwagent ./
echo "Clean up remote files"
ssh someuser@gate.polarhome.com -p775 "rm -rf agent"
