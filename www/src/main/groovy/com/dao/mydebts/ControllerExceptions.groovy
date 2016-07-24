package com.dao.mydebts

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus

import javax.persistence.EntityExistsException
import javax.persistence.EntityNotFoundException

/**
 * Controller advice on various types of exceptions. As of now, applies to both
 * {@link DebtsController} and {@link AuditController}
 *
 * @author Oleg Chernovskiy
 */
@ControllerAdvice
@ResponseBody
class ControllerExceptions {

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFoundException(EntityNotFoundException ex) {
        return ex.getLocalizedMessage();
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public String handleViolationException(DataIntegrityViolationException ex) {
        return ex.getRootCause().getLocalizedMessage();
    }

    @ExceptionHandler(InvalidObjectException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public String handleInvalidObject(InvalidObjectException ex) {
        return ex.getLocalizedMessage();
    }

    @ExceptionHandler(EntityExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleExistsFoundException(EntityExistsException ex) {
        return ex.getLocalizedMessage();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleAppException(Exception ex) {
        return ex.getMessage();
    }

}
