/*
 ${kwatee_copyright}
 */

package net.kwatee.agiledeployment.example;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class HelloServlet
 */
public class MyTomcatWebapp extends HttpServlet {

    /**
	 * 
	 */
	private static final long serialVersionUID = 7209746463252620247L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		response.setContentType("text/plain");
		String value = getServletConfig().getInitParameter("greeting");
		if (value == null)
			value = "Hi";
		out.println(value);
		out.close();
	}

    public String getServletInfo() {
        return "A simple servlet";
    }

}
