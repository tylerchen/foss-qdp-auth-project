/*******************************************************************************
 * Copyright (c) Aug 28, 2017 @author <a href="mailto:iffiff1@gmail.com">Tyler Chen</a>.
 * All rights reserved.
 *
 * Contributors:
 *     <a href="mailto:iffiff1@gmail.com">Tyler Chen</a> - initial API and implementation
 ******************************************************************************/
package com.foreveross.common.shiro;

import com.foreveross.common.ResultBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.servlet.AdviceFilter;
import org.iff.infra.util.Assert;
import org.iff.infra.util.Exceptions;
import org.iff.infra.util.FCS;
import org.iff.infra.util.HttpHelper;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Shiro ZUUL验证过滤器
 *
 * @author <a href="mailto:iffiff1@gmail.com">Tyler Chen</a>
 * @since Aug 11, 2016
 */
public class ShiroZuulAccessControlFilter extends AdviceFilter implements OnceValidAdvice {

    private static final org.iff.infra.util.Logger.Log Logger = org.iff.infra.util.Logger.get("FOSS.SHIRO");

    public boolean preHandle(ServletRequest servletRequest, ServletResponse servletResponse) throws Exception {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String url = StringUtils.removeStart(request.getRequestURI(), request.getContextPath());
        //是否为OnceValidAdvice。
        boolean isOnceValidAdvice = Boolean.TRUE.equals(request.getAttribute(OnceValidAdvice.REQUEST_MARK));

        String ip = HttpHelper.getRemoteIpAddr(request);
        /**
         * 验证： 如果Header[zuul]采用了约定的加密方式（把客户端的所有IP进行md5(md5(ip).reverse())拼接），服务端拿到客户端的IP也进行相同的加密方式，最后对比是否包含加密段即可，【非严谨验证方式】。
         */
        String zuul = request.getHeader("zuul");

        if (StringUtils.isBlank(zuul)) {
            return false;
        }

        try {//开启shiro鉴权
            Subject subject = SecurityUtils.getSubject();
            subject.login(new JWTToken(zuul));
            //Shiro鉴权不通过，如果要终止后续的验证，需要自行返回错误信息并抛出异常
            Assert.state(ShiroHelper.isPermitted(subject, url));
            Logger.debug(FCS.get("Shiro zuul auth success, ip: {0}", ip));
            return true;
        } catch (Exception e) {
            ShiroHelper.retrun401(request, response, ResultBean.error().setBody("Unauthorized"));
            if (isOnceValidAdvice) {
                Exceptions.runtime("Shiro not permit, end OnceValidAdvice chain.", "FOSS-SHIRO-0100");
            }
        }
        return false;
    }

    protected void cleanup(ServletRequest request, ServletResponse response, Exception existing)
            throws ServletException, IOException {
        super.cleanup(request, response, existing);
    }

}