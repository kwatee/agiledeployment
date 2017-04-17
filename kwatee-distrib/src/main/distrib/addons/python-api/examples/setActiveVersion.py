#
# This example script shows how to change a release artifact active version with the kwatee json api
#

import kwatee
import sys, os

if len(sys.argv) < 5:
	print 'Sets the active version of a artifact within the snapshot release'
	print 'Usage: python setActiveVersion.py http://[<user[:password>]@]<host>:<port>/<kwateeUrl> <environment> <artifact> <version>'
else:
	kwateeUrl = sys.argv[1]
	environmentName = sys.argv[2]
	artifactName = sys.argv[3]
	versionName = sys.argv[4]
	session = kwatee.api.Session(kwateeUrl)
	session.connect()
	session.setArtifactActiveVersion(environmentName, artifactName, versionName)
	print 'Active version updated'
	session.disconnect()
