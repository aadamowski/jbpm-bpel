/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the JBPM BPEL PUBLIC LICENSE AGREEMENT as
 * published by JBoss Inc.; either version 1.0 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package org.jbpm.bpel.sublang.xpath;

import java.util.Iterator;

import org.jaxen.expr.BinaryExpr;
import org.jaxen.expr.Expr;
import org.jaxen.expr.FilterExpr;
import org.jaxen.expr.FunctionCallExpr;
import org.jaxen.expr.LiteralExpr;
import org.jaxen.expr.LocationPath;
import org.jaxen.expr.NumberExpr;
import org.jaxen.expr.PathExpr;
import org.jaxen.expr.UnaryExpr;
import org.jaxen.expr.VariableReferenceExpr;

/**
 * Replacement for the <code>Visitor</code> interface and
 * <code>VisitorSupport</code> class in Jaxen. These two libraries were
 * deleted from Jaxen in version 1.1 Beta12 with no direct replacement.
 * @author Alejandro Guizar
 * @version $Revision$ $Date: 2007/02/24 13:34:30 $
 */
class ExprVisitor {

  public void visit(Expr expr) {
    if (expr instanceof BinaryExpr)
      visit((BinaryExpr) expr);
    else if (expr instanceof FilterExpr)
      visit((FilterExpr) expr);
    else if (expr instanceof FunctionCallExpr)
      visit((FunctionCallExpr) expr);
    else if (expr instanceof LiteralExpr)
      visit((LiteralExpr) expr);
    else if (expr instanceof LocationPath)
      visit((LocationPath) expr);
    else if (expr instanceof NumberExpr)
      visit((NumberExpr) expr);
    else if (expr instanceof PathExpr)
      visit((PathExpr) expr);
    else if (expr instanceof UnaryExpr)
      visit((UnaryExpr) expr);
    else if (expr instanceof VariableReferenceExpr)
      visit((VariableReferenceExpr) expr);
  }

  public void visit(BinaryExpr expr) {
    visit(expr.getLHS());
    visit(expr.getRHS());
  }

  public void visit(FilterExpr expr) {
    visit(expr.getExpr());
  }

  public void visit(FunctionCallExpr expr) {
    Iterator parameterIt = expr.getParameters().iterator();
    while (parameterIt.hasNext()) {
      Expr parameter = (Expr) parameterIt.next();
      visit(parameter);
    }
  }

  public void visit(LiteralExpr expr) {
    // primary expr
  }

  public void visit(LocationPath expr) {
    // primary expr
  }

  public void visit(NumberExpr expr) {
    // primary expr
  }

  public void visit(PathExpr expr) {
    visit(expr.getFilterExpr());
    visit(expr.getLocationPath());
  }

  public void visit(UnaryExpr expr) {
    visit(expr.getExpr());
  }

  public void visit(VariableReferenceExpr expr) {
    // primary expr
  }
}
