package pl.bpiatek.linkshortenerlinkservice.api;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UrlValidatorConstraint.class)
@Documented
public @interface ValidUrl {
    String message() default "A valid, well-formed URL is required.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
