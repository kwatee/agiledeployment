#!/bin/sh
mvn release:clean release:prepare -DautoVersionSubmodules=true
#mvn release:perform

