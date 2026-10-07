package com.libraryms.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method to be automatically audited upon successful completion.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {

    /** Type of aggregate or entity being affected (e.g. BOOK, COPY, MEMBER, LOAN, USER, AUTHOR) */
    String entityType();

    /** Action performed (e.g. CREATE, UPDATE, DELETE, RETURN, PASSWORD_CHANGE) */
    String operation();
}
