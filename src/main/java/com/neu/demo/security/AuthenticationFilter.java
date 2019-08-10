//package com.neu.demo.security;
//
//
//import com.nimbusds.jose.JOSEException;
//import org.elasticsearch.common.Strings;
//import org.springframework.core.annotation.Order;
//
//import javax.servlet.*;
//import javax.servlet.annotation.WebFilter;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.io.OutputStreamWriter;
//import java.io.PrintWriter;
//import java.text.ParseException;
//
///**
// * @author lyupingdu
// * @date 2019-07-29.
// */
//@Order(1)
//@WebFilter(urlPatterns = "/authenticate")
//public class AuthenticationFilter implements Filter {
//
//    @Override
//    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
//        String auth = ((HttpServletRequest) request).getHeader("Authorization");
//        if (Strings.isNullOrEmpty(auth) || !auth.startsWith("Bearer ")) {
//            setErrorMsg((HttpServletResponse)response);
//        } else {
//            int startIndex = "Bearer ".length();
//            String token = auth.substring(startIndex);
//            try {
//                if (AuthenticateService.verify(token)) {
//                    chain.doFilter(request, response);
//                } else {
//                    setErrorMsg((HttpServletResponse)response);
//                }
//            } catch (ParseException | JOSEException e) {
//                e.printStackTrace();
//            }
//
//        }
//    }
//
//    private void setErrorMsg(HttpServletResponse response) {
//        PrintWriter writer;
//        OutputStreamWriter osw = null;
//        try {
//            osw = new OutputStreamWriter(response.getOutputStream(),
//                    "UTF-8");
//        } catch (IOException e) {
//            System.out.println(e.getMessage());
//        }
//        writer = new PrintWriter(osw, true);
//        writer.write("Authentication Fail");
//        response.setStatus(403);
//        writer.flush();
//        writer.close();
//    }
//}
//
