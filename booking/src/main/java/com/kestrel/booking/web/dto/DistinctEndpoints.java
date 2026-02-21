package com.kestrel.booking.web.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Class-level constraint: origin and destination of a shipment must not be the same place. */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DistinctEndpointsValidator.class)
public @interface DistinctEndpoints {

    String message() default "origin and destination must differ";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
