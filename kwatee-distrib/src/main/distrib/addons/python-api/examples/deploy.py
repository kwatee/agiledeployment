#
# This example script shows how to initiate a deploy operation with the kwatee json api
# The same structure can be used to undeploy, start and stop
#

import kwatee
import sys, time

if len(sys.argv) < 4:
	print 'Deploys release'
	print 'Usage: python deploy.py http://[<user[:password>]@]<host>:<port>/<kwateeUrl> environmentName releaseName [artifactName [serverName]]'
else:
	kwateeUrl = sys.argv[1]
	environmentName = sys.argv[2]
	releaseName = sys.argv[3]
	artifactName = sys.argv[4] if len(sys.argv) > 4 else None
	serverName = sys.argv[5] if len(sys.argv) > 5 else None
	session = kwatee.api.Session(kwateeUrl)
	session.connect()
	ref = session.manage(environmentName, releaseName, artifactName, serverName, 'deploy')
	while !session.isOperationFinished(ref):
		time.sleep(0.5)
	print session.getDeploymentProgress(ref)
	session.clearDeployment(ref)
	session.disconnect()
