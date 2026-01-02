package com.smf.securewebservices.cors;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.AllowedCrossDomainsHandler.AllowedCrossDomainsChecker;

import jakarta.enterprise.context.Dependent;
import jakarta.servlet.http.HttpServletRequest;
/**
 * 
 * @author androettop
 * TODO: make configurable
 */
@Dependent
public class AllowCrossDomains extends AllowedCrossDomainsChecker {
    private static final Logger log = Logger.getLogger(AllowCrossDomains.class);
    @Override
    public boolean isAllowedOrigin(HttpServletRequest request, String origin) {
        log.debug("Origin " + origin);
        log.debug("Request information: " + request.getRequestURL() + "-" + request.getQueryString());
        return true;
    }
}
