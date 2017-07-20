package com.aceattorneyonline.master;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target(METHOD)
public @interface ChatCommandSyntax {
	
	public String name();
	public String description();
	public String arguments();

}
