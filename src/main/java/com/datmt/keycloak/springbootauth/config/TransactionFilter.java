package com.datmt.keycloak.springbootauth.config;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Component
@Order(1) 
public class TransactionFilter implements Filter {

	private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(TransactionFilter.class);


	private int MAX_REQUESTS_PER_SECOND = 10;

	private LoadingCache<String, Integer> requestCountsPerIpAddress;

	private LoadingCache<String, Integer> blockIP;

    public TransactionFilter(){
        super();

		requestCountsPerIpAddress = CacheBuilder.newBuilder().
				expireAfterWrite(5, TimeUnit.SECONDS).build(new CacheLoader<String, Integer>() {
					public Integer load(String key) {
						return 0;
					}
				});

		blockIP = CacheBuilder.newBuilder().
				expireAfterWrite(5, TimeUnit.MINUTES).build(new CacheLoader<String, Integer>() {
					public Integer load(String key) {
						return 0;
					}
				});
}

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
		LOG.info("Initializing filter :{}", this);

	}

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;
		res.setCharacterEncoding("UTF-8");
		String clientIpAddress = getClientIP(req);

		if(checkBlockIp(clientIpAddress)){
			res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			res.getWriter().write("Tào lao quá block lun nha tui báo admin rồi á");
			return;
		}

		if(isMaximumRequestsPerSecondExceeded(clientIpAddress)){
			res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
			res.getWriter().write("Too many requests");
			return;
		}
		String link = req.getRequestURI();
		String domain = req.getHeader("host");
		LOG.info("link :{}", link);
		Enumeration<String> header =  req.getHeaderNames();
		while (header.hasMoreElements()) {
			String headerName = header.nextElement();
			String headerValue = req.getHeader(headerName);
			LOG.info("header :{} ", headerName);
			LOG.info("value :{} ", headerValue);
		}

		LOG.info("Starting Transaction for req :{}", req.getRequestURI());
		if(req.getRequestURI().equals("/blogger-api-gateway/swagger-ui")){
			res.setStatus(HttpStatus.BAD_REQUEST.value());
			res.getWriter().write(" only support call blogger-api-gateway/swagger-ui/index.html");
			return;
		}

			chain.doFilter(request, response);
		res.setHeader("Access-Control-Allow-Origin", "*");
		LOG.info("Committing Transaction for req :{}", req.getRequestURI());

	}

	private boolean checkBlockIp(String clientIpAddress) {
		int count =0;
		try {
			count = blockIP.get(clientIpAddress);
			if(count > 3){
				return true;
			}
		} catch (Exception e){
			return false;
		}
		return false;
	}

	private boolean isMaximumRequestsPerSecondExceeded(String clientIpAddress){
		int requests = 0;
		int count = 0;
		try {
			requests = requestCountsPerIpAddress.get(clientIpAddress);
			if(requests > MAX_REQUESTS_PER_SECOND){
				count = blockIP.get(clientIpAddress);
				count++;
				blockIP.put(clientIpAddress, count);
				requestCountsPerIpAddress.put(clientIpAddress, requests);
				return true;
			}
		} catch (ExecutionException e) {
			requests = 0;
		}
		requests++;
		requestCountsPerIpAddress.put(clientIpAddress, requests);
		return false;
	}

	public String getClientIP(HttpServletRequest request) {
		String xfHeader = request.getHeader("X-Forwarded-For");
		if (xfHeader == null){
			return request.getRemoteAddr();
		}
		return xfHeader.split(",")[0];
	}

	@Override
	public void destroy() {
		LOG.info("Destructing filter :{}", this);
	}
}
