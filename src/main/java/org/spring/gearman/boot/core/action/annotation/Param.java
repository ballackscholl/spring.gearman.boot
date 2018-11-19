package org.spring.gearman.boot.core.action.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Param {
	/**
	 * 参数名，不可为空
	 * 
	 * @return
	 */
	String name();

	/**
	 * 参数中Date类型数据的格式，默认为空串，代表毫秒
	 * 
	 * @return
	 */
	String dateFormat() default "";
}
