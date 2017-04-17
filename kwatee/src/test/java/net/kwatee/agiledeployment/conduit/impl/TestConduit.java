/*
 * ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.conduit.impl;

import java.io.IOException;

import net.kwatee.agiledeployment.common.VariableResolver;
import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationFailedException;
import net.kwatee.agiledeployment.common.exception.ConduitAuthenticationPromptPasswordException;
import net.kwatee.agiledeployment.conduit.Conduit;
import net.kwatee.agiledeployment.conduit.DeployCredentials;
import net.kwatee.agiledeployment.conduit.ServerInstance;
import ch.ethz.ssh2.crypto.PEMDecoder;

public class TestConduit implements Conduit {

	public static final String SECRET_PEM = "-----BEGIN RSA PRIVATE KEY-----\n" +
			"MIIEowIBAAKCAQEAxGp4nrYRanJvWTl5CROKawVWq4A051XOD7Vdmc/7oUIZnQft\n" +
			"pSzb4tQN2NBvmkKaJt/wNFdvtHJGGhE3i+jRsEKbQAogHA4h54h61gfjNIHBdrgO\n" +
			"V0NcQSZYmHDiYTQZb6n6qD3RVxntRORiA3BDb4wuq3P9PN0P6GPL7U77X+bu5BIs\n" +
			"WW6cNGoMcnqV019QLqkS7cgMGE8SOxyDyqI2h6a6X4YMlW214sDHMvxFXHrvFQ47\n" +
			"O9GwvLnNPg5Ny/ZYo/GolPTGf+aY9zcGMuGEr8pwoUKu3AHx6tVyzwT4g9/IRB86\n" +
			"/n7tJtziOd42Q6rQijzl7xpRFnfOjmg5NG4gkwIDAQABAoIBAQCPpz9o9MhwDhoa\n" +
			"K8q2NLB77X+iFzaaK0t2ebNnl5H2Mx4Al5fbwp9fmrA17txw+l08KXTJE1oDo8BZ\n" +
			"uFXj4ffa7hwWDlHFsyBg8pTEjShDctNIPP1gHUgP7jfF5mnrpesknDFTc0L0bWX3\n" +
			"iQGWejkYEHLhuwdvyE6mLLctbSK01C0ngpEh7mxhyIpNCFf0UFKQzk4n1PrzI3kI\n" +
			"69wm9LDxzNhsC2IZpkTI1t23pqTBG/5KD1510EcpTNInzBguzW/nThbsrC0lPTFG\n" +
			"L3TVlDVooTgozs4zoPh0tKD5GlmPg3sWctdBU/umfe12Nt1L9xjFtdm4bAFGUjFC\n" +
			"K4NSYaehAoGBAO6mPXBwa8WHsPi+ETMTBXthGZcSAM0j6yBpfkFL6XXmkU60JvJw\n" +
			"QhSEsMkdEv453RHNI2/Ri9hLJYG5TIc0knyMPhbgoJ0PPE2s3BIgh8ah/UtsD0G+\n" +
			"k0gnbFi5xO2/nE0cAKhGw0X5s0TbcxefxqDUSfcEDnWdvSLYV462AkMXAoGBANKy\n" +
			"Lbs5UbjUIkVYZ+gJDLxry8cGjYcpuO2WM4V/YSQQQBJi3U6LaiYKRrvYUWxaGAvH\n" +
			"TKiQlaIhBGoASmVklCt0Use328omoLssyL8FNNTdoj42uSVyzj5RHAjP3WRggurF\n" +
			"bkAPoH6i7oB8BtH49YeYpTyl7QVEim8ZyUhA/uvlAoGAZtLPk/uMI76UHIhgBr6C\n" +
			"VSeADBwgpkJ8kymA0YdwnnDqWJu8UCbYAdkuBmj1fv3VZ08YnpwNgfLWxS7ebz7S\n" +
			"gsE2G0tLSICA/gKli1Xiyk2PQgjcBfqjoKoDv7LCy6EMKhkPm3Fpv2OXvupCi4I2\n" +
			"5aKx/7EKvdhp3nKSPxXioZ8CgYA+gE6imGlOq9cSvRvtsCmQB2YmvyHis5TBHepg\n" +
			"sp2tZgirq5o8v4yYhjnfZVcaDkvNyqxJ4MISmzrE+xsWotR1Y4de6YKQoA7UF3/h\n" +
			"cnjuoGSNkgwwOLNHtyXa01a4fQy4+iIvbLNOfaTEhoY7aV/kfnqd2BbMlj1+oaad\n" +
			"AXLdpQKBgDh1F0lOPmmuDtk89KOyBWPkKV2jkOzPbfjJakJ3+LLmXIt1pBLqbe1k\n" +
			"AuFZAUxNpG6IpxR6Rxh2SzTkOpuup4e6rh/kuga8tZTP2EePtRgJvvducg0WOi3O\n" +
			"nSBLPtT65w4uCQmP+XzpGxAfW9acDeXus0JLTuzULbzvpwjVgomv\n" +
			"-----END RSA PRIVATE KEY-----";
	public static final String SECRET_PEM_ENCRYPTED = "-----BEGIN RSA PRIVATE KEY-----\n" +
			"Proc-Type: 4,ENCRYPTED\n" +
			"DEK-Info: AES-128-CBC,D79F12A4B381CDB52CBBD3B41979DB80\n\n" +
			"V38s26wsSyMp+XhPWiRAMhNuqhk9ZKAKEEA9MySO+kxO3FMjH4Wgr7ijgOPzTtne\n" +
			"+SyZlscqUzCvc4H7OrL7473U6BRxa03L6l9G6H+2LnPeD+xEniwITuxBfOU2+dz4\n" +
			"wObKB6d39o5rmn9dhUw95cfwyOlR0Rjxhttqj9aea6tbCkD8+13cAfS1XMis59Qy\n" +
			"StcxeV5Neb2V/qO9ed7W4ob3P8/ocqXrZ7FN8lk6erdrp4YzobHO+7YZSY7e1vuf\n" +
			"9AR14B838rog7jH2cd5qXh4WSqqR15nsDSLV1bLJSjvFULmoEXU8gyZ8cAYvCwuV\n" +
			"VYkSQLb2Po2fQU4yOF9elIPgyy4Wv7Ph4tWjHw3qkJnUhOBeE+n0ZiyjYleG/tW1\n" +
			"l5OGPTGYacyMr3NZ8TweACUaVXq+975PZi8OUYOCuIBeBZG3I3icNBLW+zSvQubT\n" +
			"7AE1Eltpujyb2FaVy8EyPDbhOredDR8mkKXioDuDiobrwHO2AlUrkmyr9803/EvT\n" +
			"0fRMCwneWDvQY4ZbzwM/vAXKfycdeTbgamQCHUnfGpDTmAPZH/OuJnxzsbbdal8G\n" +
			"hyYLGdT5V4LqID2fNPJX4Gzd2JNLoZ9eO0EWZbWETc/pHg3ht1I475taAvajoFE2\n" +
			"6tuGwRWiOpOfJic51wNO241I110hsnSdGlZm1lFJUpt80JnP0zCoFw17cLdeRP/A\n" +
			"XlmA/hjCO+jVcBna7GvwfPzaCoAXdCDM/G0oTPtKzSXD6NKkkaRkAQ5jXZutC9LQ\n" +
			"WQy5/9DKTalqeatALSrXE7nLjOA3dwOfmDrC1Qd6Fnbm9nh9D3MYHw55+Mx40Ztk\n" +
			"-----END RSA PRIVATE KEY-----";
	public static final String SECRET_PASSWORD = "password";

	private ServerInstance server;

	/**
	 * @param server
	 */
	public TestConduit(ServerInstance server, String rootDir) {
		this.server = server;
	}

	@Override
	public void open(String ref, DeployCredentials credentials) throws ConduitAuthenticationFailedException {
		if (!"test".equals(credentials.getLogin())) {
			throw new ConduitAuthenticationFailedException("Bad user");
		}
		if (credentials.hasPem()) {
			String pem = credentials.getPem();
			try {
				PEMDecoder.decode(pem.toCharArray(), credentials.getPassword());
			} catch (IOException e) {
				throw new ConduitAuthenticationPromptPasswordException(ref, server.getName(), credentials.getLogin(), credentials.getAccessLevel().toString(), e.getMessage());
			}
			if (!SECRET_PEM.equals(pem) && !SECRET_PEM_ENCRYPTED.equals(pem))
				throw new ConduitAuthenticationPromptPasswordException(ref, server.getName(), credentials.getLogin(), credentials.getAccessLevel().toString(), "Bad pem");
		} else {
			if (!SECRET_PASSWORD.equals(credentials.getPassword())) {
				throw new ConduitAuthenticationPromptPasswordException(ref, server.getName(), credentials.getLogin(), credentials.getAccessLevel().toString(), "Bad password");
			}
		}
	}

	@Override
	public void close() {}

	@Override
	public boolean newlyCreated() {
		return false;
	}

	@Override
	public String getLastCommandOutput() {
		return "command result";
	}

	@Override
	public void sendFile(String localFile, String remoteDir, String remoteName) throws IOException, InterruptedException {}

	@Override
	public void retrieveFile(String localDir, String remoteFile) throws IOException {}

	@Override
	public ServerInstance getServer() {
		return server;
	}

	@Override
	public String remoteDiagnostics(DeployCredentials credentials) throws ConduitAuthenticationFailedException {
		open(null, credentials);
		return "Test platform command availability (support=0 is good)\n" +
				"cd support=0\n" +
				"chmod support=0\n" +
				"mkdir support=0\n" +
				"rm support=0\n" +
				"cat support=0";
	}

	@Override
	public String getRemoteDescriptor(String deploymentName, String packageName) throws InterruptedException {
		return null;
	}

	@Override
	public void installAgent() throws InterruptedException {}

	@Override
	public String retrieveRemoteAgentVersion() throws InterruptedException {
		return null;
	}

	@Override
	public void deployPackage(String deploymentName, String packageName, String packageFile, boolean useSudo) throws InterruptedException {}

	@Override
	public void checkIntegrity(String deploymentName, String packageName, String signature, boolean useSudo) throws InterruptedException {}

	@Override
	public int executeRemoteAction(String action, String actionParams, String deploymentName, String packageName, boolean skipIntegrityCheck, boolean useSudo) throws InterruptedException {
		return Conduit.KWATEE_RESULT_OK;
	}

	@Override
	public int undeployPackage(String deploymentName, String packageName, boolean skipIntegrityCheck, boolean force, boolean useSudo) throws InterruptedException {
		return Conduit.KWATEE_RESULT_OK;
	}

	@Override
	public void updateDeploymentDescriptor(String deploymentName, String descriptorXml) throws InterruptedException {}

	@Override
	public void updatePackageDescriptor(String deploymentName, String packageName, String descriptorXml) throws InterruptedException {}

	@Override
	public void setVariableResolver(VariableResolver resolver) {}
}
