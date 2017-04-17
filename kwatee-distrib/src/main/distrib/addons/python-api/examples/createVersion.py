#
# This example script shows how to create an artifact version using the kwatee json api
#

import kwatee
import sys, os

if len(sys.argv) < 4:
	print 'Creates a new version in an existing artifact'
	print 'Usage: python createVersion.py http://[<user[:password>]@]<host>:<port>/<kwateeUrl> <artifact> <version> [<description>]'
else:
	print len(sys.argv)
	kwateeUrl = sys.argv[1]
	artifactName = sys.argv[2]
	versionName = sys.argv[3]
	properties = None if len(sys.argv) == 4 else {'description':sys.argv[4]}
	session = kwatee.api.Session(kwateeUrl)
	session.connect()
	session.createVersion(artifactName, versionName, properties)
	print 'Version created'
	session.disconnect()
