package net.kwatee.agiledeployment.client;

import java.io.File;
import java.io.IOException;

public class ArtifactTest {

	public static void main(String argv[]) throws IOException {
		File f = new File("/Users/kwatee/dbedit.xml");
		if (f.exists()) {
			KwateeAPI session = KwateeAPI.createInstance("http://admin:password@localhost:8082/kwatee");
			session.uploadArtifactPackage("qwe", "v1", f, false);
			session.close();
		}
	}
}
