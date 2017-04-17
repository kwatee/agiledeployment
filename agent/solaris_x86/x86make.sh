#!/bin/sh
#You must have access to an solaris x86 system to build this agent
rm -rf kwagent
echo "Cleanup remote files in case"
ssh osol "rm -rf agent;mkdir agent"
echo "Copy sources to solaris build host"
scp ../platforms.properties osol:agent/
scp -r ../../kwatee_agent/src osol:agent/
scp -r ../../kwatee_agent/solaris_x86 osol:agent/
echo "Start build process"
ssh osol "export PATH=/usr/gnu/bin:/usr/bin;cd agent/solaris_x86;make clean all;./kwagent --version"
echo "Download built kwagent"
scp  osol:agent/solaris_x86/kwagent ./
echo "Clean up remote files"
ssh osol "rm -rf agent"
