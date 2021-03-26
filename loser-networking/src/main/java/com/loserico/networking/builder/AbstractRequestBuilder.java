package com.loserico.networking.builder;

import com.loserico.common.lang.bean.UrlParts;
import com.loserico.common.lang.transformer.Transformers;
import com.loserico.common.lang.utils.DateUtils;
import com.loserico.common.lang.utils.RegexUtils;
import com.loserico.common.lang.utils.StringUtils;
import com.loserico.json.jackson.JacksonUtils;
import com.loserico.networking.constants.HttpHeaders;
import com.loserico.networking.enums.HttpMethod;
import com.loserico.networking.enums.Scheme;
import com.loserico.networking.exception.HttpRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import static com.loserico.common.lang.utils.Assert.notNull;
import static com.loserico.networking.enums.Scheme.HTTP;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * 一个完整的URL包含的各部分如下:<p>
 * [protocol:][//host[:port]][path][?query][#fragment]<p>
 *
 * <pre>
 *                                                         | request ----------------------------------------------- |
 *                                                         | path ------------------- |                              |
 *         | authorization | | domain -------------- |     | directory ---- || file - | | query ---------------- |   |
 *         |               | |                       |     |                ||        | |                        |   |
 * https://username:password@www.subdomain.example.com:1234/folder/subfolder/index.html?search=products&sort=false#top
 * |       |        |        |   |         |       |   |   |       |         |     |    |      |        |    |     |
 * |       username |        |   |         |       |   |   folder  folder    |     |    |      value    |    value |
 * protocol         password |   |         |       |   port                  |     |    parameter       parameter  |
 *                           |   |         |       1st-level-domain          |     file-extension                  fragment
 *                           |   |         2nd-level-domain                  filename
 *                           |   3rd-level-domain
 *                           4th-level-domain
 *
 * </pre>
 *
 * <p>
 * Copyright: Copyright (c) 2021-03-22 16:57
 * <p>
 * Company: Information & Data Security Solutions Co., Ltd.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
@Slf4j
public abstract class AbstractRequestBuilder {
	
	protected static PoolingHttpClientConnectionManager connectionManager;
	
	static {
		if (connectionManager == null) {
			connectionManager = new PoolingHttpClientConnectionManager();
			connectionManager.setMaxTotal(50);// 整个连接池最大连接数
			connectionManager.setDefaultMaxPerRoute(5);// 每路由最大连接数, 默认值是2
		}
	}
	
	
	public static final Charset UTF8 = StandardCharsets.UTF_8;
	
	/**
	 * 完整的URL, 比如 http://192.168.100.101:9200/rico/_mapping
	 */
	protected String url;
	
	/**
	 * URL的协议部分, 默认 http
	 */
	protected Scheme scheme = HTTP;
	
	/**
	 * URL的端口部分, 默认 80
	 */
	protected int port = 80;
	
	/**
	 * URL的主机名部分, 如 www.163.com, 192.168.100.101
	 */
	protected String host;
	
	/**
	 * 请求的path部分, 如: /rico/_mapping<p>
	 * 应该以/开头
	 */
	protected String path;
	
	protected Map<String, Object> headers = new HashMap<>(12);
	
	protected Class responseType;
	
	protected HttpMethod method = HttpMethod.GET;
	
	protected MultiMap params = new MultiValueMap();
	
	/**
	 * HTTP请求报错时回调函数
	 */
	protected Consumer<Exception> errorCallback;
	
	/**
	 * 通过连接池获取HttpClient
	 *
	 * @return CloseableHttpClient
	 */
	protected CloseableHttpClient getHttpClient() {
		return HttpClients.custom().setConnectionManager(connectionManager).build();
	}
	
	/**
	 * 指定完整的URL, 如: http://192.168.100.101:9200/rico/_mapping<p>
	 * 如果设置了URL, 就不需要设置scheme, host, port, path<p>
	 * 但是请求参数还是可以设置的, 如果URL带参数部分, 又另外设置了参数, 那么取两者合集
	 *
	 * @param url
	 * @return AbstractRequestBuilder
	 */
	public AbstractRequestBuilder url(String url) {
		this.url = url;
		return this;
	}
	
	/**
	 * 指定 URL的协议部分, 如 http<p>
	 * 如果指定了完整的url就不需要指定scheme, 设置了也无效
	 *
	 * @param scheme
	 * @return AbstractRequestBuilder
	 */
	public AbstractRequestBuilder scheme(Scheme scheme) {
		this.scheme = scheme;
		return this;
	}
	
	/**
	 * URL的主机名部分, 如 www.163.com, 192.168.100.101<p>
	 * 如果指定了完整的url就不需要指定host, 设置了也无效<p>
	 * 不需要尾部的/
	 *
	 * @param host
	 * @return AbstractRequestBuilder
	 */
	public AbstractRequestBuilder host(String host) {
		/*
		 * 如果主机名部分带/结尾, 那么去掉尾部的/
		 */
		if (host != null && host.lastIndexOf("/") == host.length() - 1) {
			host = host.substring(0, host.length() - 1);
		}
		this.host = host;
		return this;
	}
	
	/**
	 * 设置端口号<p>
	 * 如果指定了完整的url就不需要指定port, 设置了也无效
	 *
	 * @param port
	 * @return
	 */
	public AbstractRequestBuilder port(int port) {
		this.port = port;
		return this;
	}
	
	/**
	 * 一个完整URL的path部分, 以/开头<p>
	 * 如http://192.168.100.101:9200/rico/_mapping 的 /rico/_mapping<p>
	 * 如果指定了完整的url就不需要指定path, 设置了也无效
	 *
	 * @param path
	 * @return
	 */
	public AbstractRequestBuilder path(String path) {
		//如果没有以/开头, 自动给他加上/
		if (path != null && path.indexOf("/") != 0) {
			path = '/' + path;
		}
		this.path = path;
		return this;
	}
	
	/**
	 * 设置 HTTP 请求方法
	 *
	 * @param method
	 * @return AbstractRequestBuilder
	 */
	public AbstractRequestBuilder method(HttpMethod method) {
		this.method = method;
		return this;
	}
	
	protected AbstractRequestBuilder addHeader(String headerName, Object headerValue) {
		notNull(headerName, "Header name cannot be null");
		notNull(headerValue, "Header value cannot be null");
		headers.put(headerName, headerValue);
		return this;
	}
	
	/**
	 * 添加请求参数
	 *
	 * @param paramName
	 * @param paramValue
	 * @return AbstractRequestBuilder
	 */
	protected AbstractRequestBuilder addParam(String paramName, Object paramValue) {
		notNull(paramName, "paramName cannot be null!");
		params.put(paramName, paramValue);
		return this;
	}
	
	/**
	 * Basic Authentication
	 * 设置请求头: Authorization, 值为: "Basic XXXX" 形式
	 *
	 * @param username
	 * @param password
	 */
	protected AbstractRequestBuilder basicAuth(String username, String password) {
		notNull(username, "Username must not be null");
		notNull(password, "Password must not be null");
		
		CharsetEncoder encoder = UTF8.newEncoder();
		if (!encoder.canEncode(username) || !encoder.canEncode(password)) {
			throw new IllegalArgumentException(
					"Username or password contains characters that cannot be encoded to " + UTF8.displayName());
		}
		
		String credentialsString = username + ":" + password;
		byte[] encodedBytes = Base64.getEncoder().encode(credentialsString.getBytes(UTF8));
		String encodedCredentials = new String(encodedBytes, UTF8);
		headers.put(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials);
		
		return this;
	}
	
	/**
	 * Bearer Token Authentication
	 * 设置请求头: Authorization, 值为: "Bearer XXXX" 形式
	 *
	 * @param token
	 */
	protected AbstractRequestBuilder bearerAuth(String token) {
		notNull(token, "token must not be null");
		headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + token);
		return this;
	}
	
	protected AbstractRequestBuilder onError(Consumer<Exception> errorCallback) {
		this.errorCallback = errorCallback;
		return this;
	}
	
	/**
	 * 将Map中的参数名/值对转成HTTPClient的NameValuePair
	 *
	 * @param params
	 * @return List<NameValuePair>
	 */
	protected List<NameValuePair> toNameValuePairs(Map<String, Object> params) {
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		for (Map.Entry<String, Object> param : params.entrySet()) {
			//如果一个参数有多个值, 把这多个值用,连接
			List valueList = (ArrayList) param.getValue();
			pairs.add(new BasicNameValuePair(param.getKey(), StringUtils.joinWith(",", valueList)));
		}
		
		return pairs;
	}
	
	/**
	 * 包装一个URL的各个部分, 以对象的形式返回
	 *
	 * @return UrlParts
	 */
	protected UrlParts urlParts() {
		if (isNotBlank(url)) {
			return RegexUtils.teardown(url);
		}
		
		UrlParts urlParts = new UrlParts();
		urlParts.setScheme(scheme == null ? null : scheme.name().toLowerCase());
		urlParts.setHost(host);
		urlParts.setPort(port);
		urlParts.setPath(path);
		
		return urlParts;
	}
	
	/**
	 * 构造HttpRequest对象, 同时设置请求头
	 *
	 * @param builder
	 * @return HttpRequest
	 */
	protected HttpUriRequest buildHttpRequest(URIBuilder builder) {
		URI uri = null;
		try {
			uri = builder.build();
		} catch (URISyntaxException e) {
			log.error("", e);
			throw new IllegalArgumentException(e);
		}
		
		HttpRequestBase request = null;
		switch (method) {
			case GET:
				request = new HttpGet(uri);
				break;
			case POST:
				request = new HttpPost(uri);
				break;
			case PUT:
				request = new HttpPut(uri);
				break;
			case DELETE:
				request = new HttpDelete(uri);
				break;
			case HEAD:
				request = new HttpHead(uri);
				break;
			case OPTIONS:
				request = new HttpOptions(uri);
				break;
			case TRACE:
				request = new HttpTrace(uri);
				break;
		}
		
		addHeader(request);
		return request;
	}
	
	/**
	 * 通过HttpClient发送请求
	 *
	 * @return T
	 */
	public <T> T request() {
		UrlParts urlParts = urlParts();
		
		URIBuilder builder = new URIBuilder();
		builder.setScheme(urlParts.getScheme());
		builder.setHost(urlParts.getHost());
		builder.setPort(urlParts.getPort());
		builder.setPath(urlParts.getPath());
		
		/*
		 * 把从URL里面解析出来的参数和显式设置的参数合并
		 */
		MultiMap multiMap = urlParts.paramMap();
		params.putAll(multiMap);
		
		List<NameValuePair> pairs = toNameValuePairs(params);
		builder.setParameters(pairs);
		
		try {
			/*
			 * 根据请求方法创建HttpGet, HttpPost等对象
			 * 同时设置请求头
			 */
			HttpUriRequest httpRequest = buildHttpRequest(builder);
			
			/*
			 * 钩子方法, 提供子类去实现
			 */
			if (httpRequest instanceof HttpPost || httpRequest instanceof HttpPut) {
				//只有POST, PUT方法, 并且是发送JSON数据才需要执行
				if (this instanceof JsonRequestBuilder) {
					addBody((HttpEntityEnclosingRequestBase) httpRequest);
				} else if (this instanceof FormRequestBuilder) {
					//表单提交时设置表单数据
					addFormData((HttpEntityEnclosingRequestBase) httpRequest);
				}
			}
			
			CloseableHttpClient httpClient = getHttpClient();
			CloseableHttpResponse response = httpClient.execute(httpRequest);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				String result = EntityUtils.toString(entity, "UTF-8");
				response.close();
				
				if (responseType != null) {
					if (isBlank(result)) {
						return null;
					}
					return (T) JacksonUtils.toObject(result, responseType);
				}
				return (T) result;
			}
		} catch (IOException e) {
			log.error("", e);
			throw new HttpRequestException(e);
		}
		
		return null;
	}
	
	/**
	 * 为HTTP POST请求添加请求体
	 *
	 * @param request
	 */
	protected void addBody(HttpEntityEnclosingRequestBase request) {
	}
	
	/**
	 * 表单提交时设置表单对象
	 *
	 * @param request
	 */
	protected void addFormData(HttpEntityEnclosingRequestBase request) {
	}
	
	private void addHeader(HttpRequestBase request) {
		for (Entry<String, Object> entry : headers.entrySet()) {
			Object value = entry.getValue();
			if (value instanceof Date) {
				request.addHeader(entry.getKey(), DateUtils.formatToRfc((Date) value));
				continue;
			}
			if (value instanceof LocalDateTime) {
				request.addHeader(entry.getKey(), DateUtils.formatToRfc((LocalDateTime) value));
				continue;
			}
			request.addHeader(entry.getKey(), Transformers.convert(value, String.class));
		}
	}
}