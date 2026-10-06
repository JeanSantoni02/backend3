package com.bank.xyz.clientes.validacion;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RutValido.Validador.class)
public @interface RutValido {

    String message() default "el RUT no es valido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validador implements ConstraintValidator<RutValido, String> {

        @Override
        public boolean isValid(String rut, ConstraintValidatorContext contexto) {
            return rut == null || Rut.esValido(rut);
        }
    }
}
