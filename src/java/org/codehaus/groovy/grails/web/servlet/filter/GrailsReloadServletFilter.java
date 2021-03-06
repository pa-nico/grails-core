/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.web.servlet.filter;

import grails.util.GrailsUtil;
import groovy.lang.Writable;
import groovy.text.Template;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.commons.spring.GrailsApplicationContext;
import org.codehaus.groovy.grails.commons.spring.GrailsRuntimeConfigurator;
import org.codehaus.groovy.grails.web.errors.GrailsWrappedRuntimeException;
import org.codehaus.groovy.grails.web.pages.GSPResponseWriter;
import org.codehaus.groovy.grails.web.pages.GroovyPagesTemplateEngine;
import org.codehaus.groovy.grails.web.servlet.DefaultGrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes;
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Copies resources from the source on content change and manages reloading if necessary.
 *
 * @author Graeme Rocher
 */
public class GrailsReloadServletFilter extends OncePerRequestFilter {

    public static final Log LOG = LogFactory.getLog(GrailsReloadServletFilter.class);

    private GrailsApplicationContext context;
    private WebApplicationContext parent;
    private GrailsApplicationAttributes appAttributes;

    @Override
    protected void initFilterBean() throws ServletException {
        super.initFilterBean();
        appAttributes = new DefaultGrailsApplicationAttributes(getServletContext());
        context = (GrailsApplicationContext)appAttributes.getApplicationContext();
        parent = (WebApplicationContext)getServletContext().getAttribute(GrailsApplicationAttributes.PARENT_APPLICATION_CONTEXT);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Executing Grails reload filter...");
        }

        try {
            GrailsApplication application = (GrailsApplication)context.getBean(GrailsApplication.APPLICATION_ID);
            if (!application.isInitialised()) {
                application.rebuild();
                GrailsRuntimeConfigurator config = new GrailsRuntimeConfigurator(application, parent);
                config.reconfigure(context, getServletContext(), true);
            }
        }
        catch (Exception e) {
            GrailsUtil.deepSanitize(e);
            LOG.error("Error occured reloading application: " + e.getMessage(),e);

            httpServletResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            GroovyPagesTemplateEngine engine = appAttributes.getPagesTemplateEngine();

            Template t = engine.createTemplate(GrailsApplicationAttributes.PATH_TO_VIEWS + "/error.gsp");

            GrailsWrappedRuntimeException wrapped = new GrailsWrappedRuntimeException(getServletContext(), e);
            Map model = new HashMap();
            model.put("exception", wrapped);

            Writable w = t.make(model);
            Writer out = createResponseWriter(httpServletResponse);
            w.writeTo(out);

            return;
        }
        filterChain.doFilter(httpServletRequest,httpServletResponse);
    }

    protected Writer createResponseWriter(HttpServletResponse response) {
        PrintWriter out = GSPResponseWriter.getInstance(response);
        GrailsWebRequest webRequest =  (GrailsWebRequest) RequestContextHolder.currentRequestAttributes();
        webRequest.setOut(out);
        return out;
    }
}
