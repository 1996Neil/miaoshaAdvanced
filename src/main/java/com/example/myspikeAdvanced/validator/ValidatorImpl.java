package com.example.myspikeAdvanced.validator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

/**
 * @author wangzhe
 * @version 1.0
 * @ClassName ValidatorImpl
 * @create 2021-08-05 12:58
 * @description springBean初始化完成之后会回调这个ValidatorImpl里的afterPropertiesSet方法
 */
@Component
public class ValidatorImpl implements InitializingBean {

    /**
     * javax原生校验器
     **/
    private Validator validator;

    public ValidatorResult validate(Object bean) {
        ValidatorResult result = new ValidatorResult();
        //如果对应的bean里面有违背定义的就会被返回到这里集合来
        Set<ConstraintViolation<Object>> constraintViolationSet = validator.validate(bean);
        if (constraintViolationSet.size() > 0) {
            //有错误
            result.setHasErrors(true);
            //foreach集合里错误信息和错误字段名,并给它添加到ValidatorResult的map中
            constraintViolationSet.forEach(constraintViolation -> {
                String errMsg = constraintViolation.getMessage();
                String propertyName = constraintViolation.getPropertyPath().toString();
                result.getErrorMsgMap().put(propertyName, errMsg);
            });
        }
        return result;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        //将hibernate validator通过工厂的初始化方法使其实例化
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

}
