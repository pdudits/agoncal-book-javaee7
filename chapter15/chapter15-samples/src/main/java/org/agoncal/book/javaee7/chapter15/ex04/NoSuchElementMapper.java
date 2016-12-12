/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.agoncal.book.javaee7.chapter15.ex04;

import java.util.NoSuchElementException;
import javax.ejb.EJBException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 *
 * @author patrik
 */
@Provider
public class NoSuchElementMapper implements ExceptionMapper<EJBException>{

    @Context
    UriInfo uri;
    
    @Override
    public Response toResponse(EJBException exception) {
        if (exception.getCause() instanceof NoSuchElementException) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(exception.getCause().getMessage()+" at "+uri.getPath())
                .build();
        } else {
            return null;
        }
    }
    
}
