package com.loserico.web.advice;

import com.loserico.common.lang.errors.ErrorTypes;
import com.loserico.common.lang.exception.ApplicationException;
import com.loserico.common.lang.exception.BusinessException;
import com.loserico.common.lang.exception.EntityNotFoundException;
import com.loserico.common.lang.i18n.I18N;
import com.loserico.common.lang.vo.Result;
import com.loserico.common.lang.vo.Results;
import com.loserico.validation.bean.ErrorMessage;
import com.loserico.validation.exception.GeneralValidationException;
import com.loserico.validation.exception.UniqueConstraintViolationException;
import com.loserico.validation.exception.ValidationException;
import com.loserico.validation.utils.ValidationUtils;
import com.loserico.web.exception.LocalizedException;
import com.loserico.web.utils.MessageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.loserico.common.lang.errors.ErrorTypes.BAD_REQUEST;
import static com.loserico.common.lang.errors.ErrorTypes.INTERNAL_SERVER_ERROR;
import static com.loserico.common.lang.errors.ErrorTypes.METHOD_NOT_ALLOWED;
import static com.loserico.common.lang.errors.ErrorTypes.NOT_FOUND;
import static com.loserico.common.lang.errors.ErrorTypes.VALIDATION_FAIL;
import static java.util.stream.Collectors.toList;

/**
 * 全局异常处理
 * <p>
 * Copyright: Copyright (c) 2019-10-11 15:04
 * <p>
 * Company: Sexy Uncle Inc.
 * <p>
 *
 * @author Rico Yu  ricoyu520@gmail.com
 * @version 1.0
 */
@RestControllerAdvice
@Slf4j
public class RestExceptionAdvice extends ResponseEntityExceptionHandler {
	
	private static Pattern messageTemplatePattern = Pattern.compile("\\{(.+)\\}");
	
	@Override
	@ResponseBody
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
	                                                    HttpStatus status, WebRequest request) {
		logger.error("Rest API ERROR happen", ex);
		return super.handleTypeMismatch(ex, headers, status, request);
	}
	
	/**
	 * 表单提交数据校验错误, 或者提交的数据转换成目标数据类型时候出错
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers, HttpStatus status,
	                                                     WebRequest request) {
		logger.error("Rest API ERROR happen", ex);
		headers.add("Content-Type", "application/json");
		ErrorMessage errorMessage = ValidationUtils.getErrorMessage(ex.getBindingResult());
		
		Result result = Results.status(BAD_REQUEST.code(), errorMessage.getErrors()).build();
		return new ResponseEntity(result, headers, HttpStatus.OK);
	}
	
	/**
	 * 处理验证相关的异常
	 * 目前只处理了BindException
	 *
	 * @param e
	 * @return
	 */
	@ExceptionHandler(ValidationException.class)
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	protected ResponseEntity<Object> handleValidationException(ValidationException e) {
		Throwable cause = e.getCause();
		if (cause != null && cause instanceof BindException) {
			BindException bindException = (BindException) cause;
			BindingResult bindingResult = bindException.getBindingResult();
			ErrorMessage errorMessage = ValidationUtils.getErrorMessage(bindingResult);
			List<String[]> msgs = errorMessage.getErrors();
			
			Result result = Results.status(VALIDATION_FAIL.code(), msgs).build();
			return new ResponseEntity(result, HttpStatus.OK);
		}
		Result result = Results.status(VALIDATION_FAIL.code(), e.getMessage()).build();
		return new ResponseEntity(result, HttpStatus.OK);
	}
	
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
	                                                              HttpHeaders headers, HttpStatus status, WebRequest request) {
		logger.error("Rest API ERROR happen", ex);
		headers.add("Content-Type", "application/json");
		Result result = Results.status(BAD_REQUEST).build();
		return new ResponseEntity(result, headers, HttpStatus.OK);
	}
	
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
	                                                              HttpHeaders headers, HttpStatus status, WebRequest request) {
		logger.error("Rest API ERROR happen", ex);
		ErrorMessage errorMessage = ValidationUtils.getErrorMessage(ex.getBindingResult());
		List<String[]> msgs = errorMessage.getErrors()
				.stream()
				.map((errArray) -> {
					Matcher matcher = messageTemplatePattern.matcher(errArray[1]);
					if (matcher.matches()) {
						errArray[1] = MessageHelper.getMessage(matcher.group(1));
					}
					return errArray;
				})
				.collect(toList());
		Result result = Results.status(VALIDATION_FAIL.code(), msgs).build();
		return new ResponseEntity(result, headers, HttpStatus.OK);
	}
	
	/**
	 * 手工验证不通过时抛出
	 *
	 * @param e
	 * @return ResponseEntity<Object>
	 */
	@ExceptionHandler(GeneralValidationException.class)
	@ResponseStatus(value = HttpStatus.OK)
	@ResponseBody
	protected ResponseEntity<Object> handleMethodArgumentNotValid(GeneralValidationException e) {
		logger.error("Rest API ERROR happen", e);
		ErrorMessage errorMessage = e.getErrorMessage();
		List<String[]> msgs = errorMessage.getErrors()
				.stream()
				.map((errArray) -> {
					Matcher matcher = messageTemplatePattern.matcher(errArray[1]);
					if (matcher.matches()) {
						errArray[1] = MessageHelper.getMessage(matcher.group(1));
					}
					return errArray;
				})
				.collect(toList());
		Result result = Results.status(VALIDATION_FAIL.code(), msgs).build();
		return new ResponseEntity(result, HttpStatus.OK);
	}
	
	@ExceptionHandler(UniqueConstraintViolationException.class)
	@ResponseStatus(value = HttpStatus.OK)
	public ResponseEntity<Object> handleUniqueConstraintViolationException(UniqueConstraintViolationException e) {
		logger.error("", e);
		Result result = Results.status(INTERNAL_SERVER_ERROR).build();
		return new ResponseEntity(result, HttpStatus.OK);
	}
	
	@ExceptionHandler(EntityNotFoundException.class)
	@ResponseStatus(value = HttpStatus.OK)
	public ResponseEntity<Object> handleEntityNotFoundException(EntityNotFoundException e) {
		logger.error("", e);
		Result result = Results.status(NOT_FOUND.code(), e.getMessage()).build();
		return new ResponseEntity(result, HttpStatus.OK);
	}
	
	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatus status,
	                                                                     WebRequest request) {
		headers.add("Content-Type", "application/json");
		Result result = Results.status(METHOD_NOT_ALLOWED).build();
		return new ResponseEntity(result, headers, HttpStatus.METHOD_NOT_ALLOWED);
	}
	
	/**
	 * 通用业务异常处理
	 *
	 * @param e
	 * @return
	 */
	@ExceptionHandler(BusinessException.class)
	@ResponseStatus(value = HttpStatus.OK)
	public ResponseEntity<Object> handleBusinessException(BusinessException e) {
		logger.error("", e);
		Result result = Results.status(e.getCode(), I18N.i18nMessage(e)).build();
		return new ResponseEntity(result, HttpStatus.OK);
	}
	
	@ExceptionHandler(LocalizedException.class)
	@ResponseStatus(value = HttpStatus.OK)
	public ResponseEntity<Object> handleLocalizedException(LocalizedException e) {
		logger.error("", e);
		Result result = Results.status(e.getStatusCode(), e.getLocalizedMessage()).build();
		return new ResponseEntity(result, HttpStatus.OK);
	}
	
	@ExceptionHandler(ApplicationException.class)
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody
	public Result handleApplicationException(ApplicationException e) {
		logger.error("Rest API ERROR happen", e);
		return Results.status(e.getCode(), e.getMessage()).build();
	}
	
	@ExceptionHandler(Throwable.class)
	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody
	public ResponseEntity<?> handleThrowable(Throwable e) {
		logger.error("Rest API ERROR happen", e);
		Result result = Results.status(ErrorTypes.INTERNAL_SERVER_ERROR).build();
		return new ResponseEntity(result, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
