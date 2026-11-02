package com.example.demo.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Đánh dấu API cần access token hợp lệ, mặc định user phải đang ACTIVE */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAuth {

    /** Cho phép user INACTIVE gọi API này */
    boolean allowInactive() default false;
}
