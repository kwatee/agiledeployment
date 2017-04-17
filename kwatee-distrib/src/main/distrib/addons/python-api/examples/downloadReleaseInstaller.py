#
# This example script shows how to export download a release installer with the kwatee json api
#

import kwatee
import sys, os

if len(sys.argv) < 5:
	print 'Downloads the artifacts for the selected release'
	print 'Usage: python exportReleaseDeployment.py http://[<user[:password>]@]<host>:<port>/<kwateeUrl> <environment> <release> <path>'
else:
	kwateeUrl = sys.argv[1]
	environmentName = sys.argv[2]
	releaseName = sys.argv[3]
	path = os.path.abspath(sys.argv[4])
	session = kwatee.api.Session(kwateeUrl)
	session.connect()
	session.downloadInstaller(environmentName, releaseName, path)
	print 'Installer downloaded at ' + path
	session.disconnect()
