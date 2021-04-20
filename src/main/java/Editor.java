import java.io.IOException;
import java.io.PrintWriter;
import java.sql.* ;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.jstl.sql.Result;
import javax.servlet.jsp.jstl.sql.ResultSupport;
import javax.sql.DataSource;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/**
 * Servlet implementation class for Servlet: ConfigurationTest
 *
 */
public class Editor extends HttpServlet {
    /**
     * The Servlet constructor
     * 
     * @see javax.servlet.http.HttpServlet#HttpServlet()
     */
    public Editor() {}

    public void init() throws ServletException
    {
        /*  write any servlet initialization code here or remove this function */
    }
    
    public void destroy()
    {
        /*  write any servlet cleanup code here or remove this function */
    }

    /**
     * Handles HTTP GET requests
     * 
     * @see javax.servlet.http.HttpServlet#doGet(HttpServletRequest request,
     *      HttpServletResponse response)
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
    {
        // implement your GET method handling code here
        // currently we simply show the page generated by "edit.jsp"
        String username, title = null, body = null, query = null;
        String action = request.getParameter("action");
        Connection c = null;
        Statement  s = null; 
        ResultSet rs = null; 
        PreparedStatement pstmt = null;
        
        int postid;

        try {
            /* create an instance of a Connection object */
            c = DriverManager.getConnection("jdbc:mariadb://localhost:3306/CS144", "cs144", "");

            if (action.equals("open")) {
                username = request.getParameter("username");
                postid = Integer.parseInt(request.getParameter("postid"));
                title = request.getParameter("title");
                body = request.getParameter("body");
                query = "SELECT * FROM Posts WHERE username = ? and postid = ?;";
                pstmt = c.prepareStatement(query);

                if (username == null) {
                    response.setStatus(404);
                    return;
                }


                if (postid == 0) {
                    if (title == null) {
                        title = "";
                    }
                    if(body == null) {
                        body = "";
                    }
                    response.setStatus(200);
                } else if (postid > 0) {
                    if (title == null || body == null) {
                        pstmt.setString(1, username);
                        pstmt.setInt(2, postid);
                        rs = pstmt.executeQuery();
                        if (!rs.next()) {
                            response.setStatus(404);
                        } else {
                            title = rs.getString("title");
                            body = rs.getString("body");
                            response.setStatus(200);
                        }
                    } else {
                        response.setStatus(200);
                    }
                }

                request.setAttribute("path", request.getRequestURL().toString());
                request.setAttribute("title", title);
                request.setAttribute("body", body);
                request.setAttribute("username", username);
                request.setAttribute("postid", postid);
                request.getRequestDispatcher("/edit.jsp").forward(request, response);
            } else if (action.equals("save")) {
                request.getRequestDispatcher("/edit.jsp").forward(request, response);       
            } else if (action.equals("delete")) {
                doPost(request, response);
            } else if (action.equals("preview")) {
                response.setStatus(200);
                request.getRequestDispatcher("/preview.jsp").forward(request, response);
            } else if (action.equals("list")) {
                username = request.getParameter("username");
                request.setAttribute("username", username);
                pstmt = c.prepareStatement("SELECT * FROM Posts WHERE username = ?");
                pstmt.setString(1, username);
                rs = pstmt.executeQuery();
                Result result = ResultSupport.toResult(rs);
                request.setAttribute("result", result);
                request.getRequestDispatcher("/list.jsp").forward(request, response);
            } else {
                response.setStatus(404);
            }
        } catch (SQLException ex){
            System.out.println("SQLException caught");
            System.out.println("---");
            while ( ex != null ) {
                System.out.println("Message   : " + ex.getMessage());
                System.out.println("SQLState  : " + ex.getSQLState());
                System.out.println("ErrorCode : " + ex.getErrorCode());
                System.out.println("---");
                ex = ex.getNextException();
            }
        } finally {
            try { rs.close(); } catch (Exception e) { /* ignored */ }
            try { s.close(); } catch (Exception e) { /* ignored */ }
            try { c.close(); } catch (Exception e) { /* ignored */ }
            try { pstmt.close(); } catch (Exception e) { /* ignored */ }
        }
        
    }
    
    /**
     * Handles HTTP POST requests
     * 
     * @see javax.servlet.http.HttpServlet#doPost(HttpServletRequest request,
     *      HttpServletResponse response)
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
    {
        // implement your POST method handling code here
        // currently we simply show the page generated by "edit.jsp"
        String action = request.getParameter("action");
        String username = "", title = "", body = "";
        Connection c = null;
        Statement  s = null;
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        int postid;
        Date d = new Date();

        try {
            /* create an instance of a Connection object */
            c = DriverManager.getConnection("jdbc:mariadb://localhost:3306/CS144", "cs144", "");

            if (action.equals("save")) {

                postid = Integer.parseInt(request.getParameter("postid"));
                username = request.getParameter("username");
                title = request.getParameter("title");
                body = request.getParameter("body");
                request.setAttribute("username", username);
                request.setAttribute("postid", postid);

                if (postid == 0) {
                    pstmt = c.prepareStatement("SELECT IFNULL(MAX(postid), 0) AS MAX FROM Posts WHERE username = ?");
                    pstmt.setString(1, username);
                    rs = pstmt.executeQuery();
                    if (rs.next()) {
                        postid = rs.getInt("MAX") + 1;
                    }

                    pstmt = c.prepareStatement("INSERT INTO Posts (username, postid, title, body, created) VALUES (?, ?, ?, ?, ?)");
                    pstmt.setString(1, username);
                    pstmt.setInt(2, postid);
                    pstmt.setString(3, title);
                    pstmt.setString(4, body);
                    pstmt.setTimestamp(5, new Timestamp(d.getTime()));
                    pstmt.executeUpdate();
                    response.setStatus(200);
                } else if (postid > 0) {
                    pstmt = c.prepareStatement("SELECT * FROM Posts WHERE (username, postid) = (?, ?)");
                    pstmt.setString(1, username);
                    pstmt.setInt(2, postid);
                    rs = pstmt.executeQuery();
                    if (!rs.next()) {
                        // (username, postid) row does not exist
                        request.setAttribute("Warning", "No Exist");
                        response.setStatus(404);
                    } else {
                        Timestamp ts;
                        request.setAttribute("Warning", new java.sql.Date(d.getTime()));
                        username = rs.getString("username");

                        postid = rs.getInt("postid");
                        pstmt = c.prepareStatement("UPDATE Posts SET title = ?, body = ?, modified = ? WHERE (username, postid) = (?, ?)");
                        pstmt.setString(1, title);
                        pstmt.setString(2, body);
                        pstmt.setTimestamp(3, new Timestamp(d.getTime()));
                        pstmt.setString(4, username);
                        pstmt.setInt(5, postid);
                        pstmt.executeUpdate();
                        response.setStatus(200);
                    }
                }
                response.sendRedirect(request.getRequestURL() + "?action=list&username=" + username);
            } else if (action.equals("preview")) {
                postid = Integer.parseInt(request.getParameter("postid"));
                username = request.getParameter("username");
                title = request.getParameter("title");
                body = request.getParameter("body");
                Parser parser = Parser.builder().build();
                HtmlRenderer renderer = HtmlRenderer.builder().build();
                String html = renderer.render(parser.parse(body));
                request.setAttribute("username", username);
                request.setAttribute("postid", postid);
                request.setAttribute("title", title);
                request.setAttribute("body", html);
                request.setAttribute("path", request.getRequestURL().toString());
                request.getRequestDispatcher("/preview.jsp").forward(request, response);
            } else if (action.equals("delete")) {
                postid = Integer.parseInt(request.getParameter("postid"));
                username = request.getParameter("username");
                pstmt = c.prepareStatement("DELETE FROM Posts WHERE (username, postid) = (?, ?)");
                pstmt.setString(1, username);
                pstmt.setInt(2, postid);
                pstmt.executeUpdate();
                request.setAttribute("path", request.getRequestURL().toString());
                request.setAttribute("username", username);
                response.setStatus(200);
                request.getRequestDispatcher("/delete.jsp").forward(request, response);
            } else {
                response.setStatus(404);
            }
            //request.getRequestDispatcher("/edit.jsp").forward(request, response);
        } catch (SQLException ex){
            System.out.println("SQLException caught");
            System.out.println("---");
            while ( ex != null ) {
                System.out.println("Message   : " + ex.getMessage());
                System.out.println("SQLState  : " + ex.getSQLState());
                System.out.println("ErrorCode : " + ex.getErrorCode());
                System.out.println("---");
                ex = ex.getNextException();
            }
        } finally {
            try { rs.close(); } catch (Exception e) { /* ignored */ }
            try { s.close(); } catch (Exception e) { /* ignored */ }
            try { c.close(); } catch (Exception e) { /* ignored */ }
            try { pstmt.close(); } catch (Exception e) { /* ignored */ }
        }
    }
}