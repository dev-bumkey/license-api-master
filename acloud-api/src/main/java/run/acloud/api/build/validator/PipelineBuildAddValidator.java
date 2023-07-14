package run.acloud.api.build.validator;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.resourceloading.PlatformResourceBundleLocator;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import run.acloud.api.build.vo.BuildAddVO;
import run.acloud.api.build.vo.BuildStepVO;
import run.acloud.commons.vo.HasUseYnVO;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

@Slf4j
@Component
@SuppressWarnings("unchecked")
public class PipelineBuildAddValidator implements Validator {

    @Override
    public boolean supports(Class<?> aClass) {
        return BuildAddVO.class.isAssignableFrom(aClass);
    }

    @Override
    public void validate(Object o, Errors errors) {
        Map<String, String> errMsgMap = new HashMap<>();
        errMsgMap.put("NotBlank", this.getMessage("org.hibernate.validator.constraints.NotBlank.message"));
        errMsgMap.put("NotNull", this.getMessage("jakarta.validation.constraints.NotNull.message"));

        // valid 체크한 field 제외
        List<String> isNotValidFieldNames = new ArrayList<>();
        isNotValidFieldNames.add("editType");
        isNotValidFieldNames.add("buildName");
        isNotValidFieldNames.add("buildSeq");

        // HasUseYnVO field 제외
        Class hasUseYnClazz = HasUseYnVO.class;
        Field[] hasUseYnFields = hasUseYnClazz.getDeclaredFields();
        for(Field fieldRow : hasUseYnFields){
            isNotValidFieldNames.add(fieldRow.getName());
        }

        BuildAddVO buildAdd = (BuildAddVO)o;

        if(StringUtils.containsAny(buildAdd.getEditType(), new String[]{"N", "U"})){
            if(StringUtils.equals(buildAdd.getEditType(), "N")){
                // 빌드명 체크
                if(StringUtils.isBlank(buildAdd.getBuildName())){
                    errors.rejectValue("buildName", "NotBlank", null, errMsgMap.get("NotBlank"));
                }

                // account seq 체크
                if(buildAdd.getAccountSeq() == null || (buildAdd.getAccountSeq() != null && buildAdd.getAccountSeq().intValue() < 1)){
                    errors.rejectValue("AccountSeq", "NotBlank", null, errMsgMap.get("NotBlank"));
                }

                // registry project id 체크
                if(buildAdd.getRegistryProjectId() == null){
                    errors.rejectValue("registryProjectId", "NotBlank", null, errMsgMap.get("NotBlank"));
                }
            }else if (StringUtils.equals(buildAdd.getEditType(), "U")){
                if(buildAdd.getBuildSeq() == null || (buildAdd.getBuildSeq() != null && buildAdd.getBuildSeq().intValue() < 1)){
                    errors.rejectValue("buildSeq", "NotNull", null, errMsgMap.get("NotNull"));
                }

            }

            this.invokeGetterMethod("", buildAdd, buildAdd, isNotValidFieldNames, errors, errMsgMap);

        }else{
            errors.rejectValue("editType", "NotBlank", null, errMsgMap.get("NotBlank"));
        }
    }

    private void invokeGetterMethod(String parentNestedPath, BuildAddVO rootObj, Object object, List<String> isNotValidFieldNames, Errors errors, Map<String, String> errMsgMap){
        if(StringUtils.isNotBlank(parentNestedPath)){
            parentNestedPath += ".";
        }

        Class clazz = object.getClass();
        Field[] fields = clazz.getDeclaredFields();
        Map<String, Object> annotationMap;
        boolean isContinue = true;

        if(StringUtils.equals("BuildStepVO", clazz.getSimpleName())) {
            BuildStepVO buildStep = (BuildStepVO) object;
            // 수정일 경우 useYn에 따라 유효성 체크를 결정
            if (StringUtils.equals("U", rootObj.getEditType())) {
                if (buildStep.getBuildStepSeq() == null) {
                    isContinue = false;
                    errors.rejectValue(String.format("%s%s", parentNestedPath, "buildStepSeq"), "NotNull", null, errMsgMap.get("NotNull"));
                } else {
                    if (buildStep.isUseFlag()) {
                        if (StringUtils.equals("Y", buildStep.getUseYn())) {
                            if (buildStep.getBuildStepSeq().intValue() < 1) {
                                errors.rejectValue(String.format("%s%s", parentNestedPath, "buildStepSeq"), "NotNull", null, "이미 생성된 ");
                            }
                        }
                    } else {
                        isContinue = false;
                    }
                }
            }else{
                if (!buildStep.isUseFlag()) {
                    isContinue = false;
                }
            }
        }


        try {
            if(isContinue){
                /**
                 * CODE_DOWN 단계에서 commonType이 PRIVATE라면 userId, password 필수 체크
                 */
                boolean isPrivateOfDownStep = false;
                // commonType
                Optional<Field> commonTypeField = Arrays.stream(fields).filter(f -> (StringUtils.equalsIgnoreCase("commonType", f.getName()) && !f.getType().isEnum())).findFirst();
                if(commonTypeField.isPresent()){
                    Method getter = ReflectionUtils.findMethod(clazz, String.format("get%s", StringUtils.capitalize(commonTypeField.get().getName())));
                    Object returnObject = getter.invoke(object, (Object[])null);
                    // commonType 값이 PRIVATE이면 flag 처리
                    if(returnObject != null && StringUtils.equals("PRIVATE", (String)returnObject)){
                        isPrivateOfDownStep = true;
                    }
                }

                for(Field fieldRow : fields){
                    if(!isNotValidFieldNames.contains(fieldRow.getName())){

                        // validation annotation 속성 셋팅
                        annotationMap = new HashMap();
                        Annotation[] fieldAnttn = fieldRow.getAnnotations();
                        if(fieldAnttn != null && fieldAnttn.length > 0){
                            for(Annotation annttnRow : fieldAnttn){
                                if(StringUtils.equals("jakarta.validation.constraints.NotNull", annttnRow.annotationType().getName())){
                                    Map<String, Object> annotationDetailMap = new HashMap();

                                    Method method = ReflectionUtils.findMethod(annttnRow.annotationType(), "message");
                                    annotationDetailMap.put("message", method.invoke(annttnRow, (Object[])null));

                                    annotationMap.put("NotNull", annotationDetailMap);
                                }else if(StringUtils.equals("org.hibernate.validator.constraints.NotBlank", annttnRow.annotationType().getName())){
                                    Map<String, Object> annotationDetailMap = new HashMap();

                                    Method method = ReflectionUtils.findMethod(annttnRow.annotationType(), "message");
                                    annotationDetailMap.put("message", method.invoke(annttnRow, (Object[])null));

                                    annotationMap.put("NotBlank", annotationDetailMap);
                                }else if(StringUtils.equals("jakarta.validation.constraints.Size", annttnRow.annotationType().getName())){
                                    Map<String, Object> annotationDetailMap = new HashMap();

                                    Method method = ReflectionUtils.findMethod(annttnRow.annotationType(), "min");
                                    annotationDetailMap.put("min", method.invoke(annttnRow, (Object[])null));

                                    method = ReflectionUtils.findMethod(annttnRow.annotationType(), "max");
                                    annotationDetailMap.put("max", method.invoke(annttnRow, (Object[])null));

                                    method = ReflectionUtils.findMethod(annttnRow.annotationType(), "message");
                                    annotationDetailMap.put("message", method.invoke(annttnRow, (Object[])null));

                                    annotationMap.put("Size", annotationDetailMap);
                                }else if(StringUtils.equals("jakarta.validation.constraints.Min", annttnRow.annotationType().getName())){
                                    Map<String, Object> annotationDetailMap = new HashMap();

                                    Method method = ReflectionUtils.findMethod(annttnRow.annotationType(), "value");
                                    annotationDetailMap.put("value", method.invoke(annttnRow, (Object[])null));

                                    method = ReflectionUtils.findMethod(annttnRow.annotationType(), "message");
                                    annotationDetailMap.put("message", method.invoke(annttnRow, (Object[])null));

                                    annotationMap.put("Min", annotationDetailMap);
                                }
                            }
                        }

                        /**
                         * DOWN 단계에서 commonType이 PRIVATE라면 userId, password 필수 체크하도록 셋팅
                         */
                        if(isPrivateOfDownStep){
                            String[] isPrivateOfDownStepCheckField = null;
                            if (StringUtils.equals("U", rootObj.getEditType())) {
                                isPrivateOfDownStepCheckField = new String[]{"userId"};
                            }else{
                                isPrivateOfDownStepCheckField = new String[]{"userId", "password"};
                            }

                            if(StringUtils.containsAny(fieldRow.getName(), isPrivateOfDownStepCheckField)){
                                Map<String, Object> annotationDetailMap = new HashMap();
                                annotationDetailMap.put("message", errMsgMap.get("NotBlank"));

                                annotationMap.put("NotBlank", annotationDetailMap);
                            }
                        }

                        // find getter method
                        Method getter = ReflectionUtils.findMethod(clazz, String.format("get%s", StringUtils.capitalize(fieldRow.getName())));

                        if(getter != null){

                            // get getter method return type;
                            Class getterReturnClazz = getter.getReturnType();

                            /**
                             * return type에 맞게 처리
                             */
                            // cocktail package class인 경우
                            if(StringUtils.startsWith(getterReturnClazz.getName(), "run.acloud.api.build")){
                                if(!annotationMap.isEmpty()){
                                    Object returnObject = getter.invoke(object, (Object[])null);
                                    if(returnObject != null){
                                        if(!getterReturnClazz.isEnum()){
                                            this.invokeGetterMethod(String.format("%s%s", parentNestedPath, fieldRow.getName()), rootObj, returnObject, isNotValidFieldNames, errors, errMsgMap);
                                        }
                                    }else{
                                        this.setErrorRejectValue(parentNestedPath, fieldRow, getterReturnClazz.getSimpleName(), returnObject, errors, annotationMap, errMsgMap);
                                    }
                                }
                            }
                            // list인 경우
                            else if("List".equalsIgnoreCase(getterReturnClazz.getSimpleName())){
//                            Type getterGenericReturnClazz = getter.getGenericReturnType();
//                            log.debug(getterGenericReturnClazz.getTypeName());
                                List<Object> returnObjects = (List<Object>) getter.invoke(object, (Object[])null);
                                this.setErrorRejectValue(parentNestedPath, fieldRow, returnObjects, errors, annotationMap, errMsgMap);

                                if(returnObjects != null){
                                    int objCnt = 0;
                                    for(Object returnObject : returnObjects){
                                        if(!returnObject.getClass().isPrimitive()
                                                && StringUtils.startsWith(returnObject.getClass().getName(), "run.acloud.api.build")){
                                            this.invokeGetterMethod(String.format("%s%s[%d]", parentNestedPath, fieldRow.getName(), objCnt), rootObj, returnObject, isNotValidFieldNames, errors, errMsgMap);
                                        }
                                        objCnt++;
                                    }
                                }
                            }
                            // 그 이외
                            else{
                                Object returnObject = getter.invoke(object, (Object[])null);

                                this.setErrorRejectValue(parentNestedPath, fieldRow, getterReturnClazz.getSimpleName(), returnObject, errors, annotationMap, errMsgMap);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("유효성 검사 중 오류가 발생하였습니다.", e);
        }
    }

    private void setErrorRejectValue(String parentNestedPath, Field field, String typeName, Object returnObject, Errors errors, Map<String, Object> annotationMap, Map<String, String> errMsgMap){
        boolean isError;
        String errMsg;

        for(Map.Entry<String, Object> entry : annotationMap.entrySet()){
            isError = false;
            errMsg = "";

            if(StringUtils.equals("NotNull", entry.getKey())){
                if(returnObject == null){
                    isError = true;
                    errMsg = this.getMessage(((Map<String, Object>)entry.getValue()).get("message").toString());
                }
            }else if(StringUtils.equals("NotBlank", entry.getKey()) && StringUtils.equals("String", typeName)){
                if(StringUtils.isBlank((String)returnObject)){
                    isError = true;
                    errMsg = this.getMessage(((Map<String, Object>)entry.getValue()).get("message").toString());
                }
            }else if(StringUtils.equals("Min", entry.getKey()) && StringUtils.equals("Integer", typeName)){
                if(returnObject != null){
                    if(((Integer)returnObject).intValue() < Integer.parseInt(((Map<String, Object>)entry.getValue()).get("value").toString())){
                        isError = true;
                        Map<String, Object> valueMap = (Map<String, Object>)entry.getValue();
                        errMsg = StringUtils.replaceEach(this.getMessage(valueMap.get("message").toString()), new String[]{"{value}"}, new String[]{String.valueOf(((Long)valueMap.get("value")).intValue())});
                    }
                }
            }else if(StringUtils.equals("Size", entry.getKey())){
                if(returnObject != null) {
                    if( (StringUtils.isBlank((String) returnObject) || StringUtils.length((String) returnObject) < Integer.parseInt(((Map<String, Object>) entry.getValue()).get("min").toString()))
                            || (StringUtils.isBlank((String) returnObject) || StringUtils.length((String) returnObject) > Integer.parseInt(((Map<String, Object>) entry.getValue()).get("max").toString()))
                            ) {
                        isError = true;
                        Map<String, Object> valueMap = (Map<String, Object>) entry.getValue();
                        int maxValue = ((Integer) valueMap.get("max")).intValue();
                        int minValue = ((Integer) valueMap.get("min")).intValue();
                        errMsg = StringUtils.replaceEach(this.getMessage(valueMap.get("message").toString()), new String[]{"{min}", "{max}"}, new String[]{String.valueOf(minValue), String.valueOf(maxValue)});
                    }
                }
            }

            if(isError){
                errors.rejectValue(String.format("%s%s", parentNestedPath, field.getName()), entry.getKey(), null, errMsg);
            }
        }
    }

    private void setErrorRejectValue(String parentNestedPath, Field field, List<Object> returnObjects, Errors errors, Map<String, Object> annotationMap, Map<String, String> errMsgMap){
        boolean isError;
        String errMsg;

        for(Map.Entry<String, Object> entry : annotationMap.entrySet()){
            isError = false;
            errMsg = "";

            if(StringUtils.equals("NotNull", entry.getKey())){
                if(returnObjects == null){
                    isError = true;
                    errMsg = this.getMessage(((Map<String, Object>)entry.getValue()).get("message").toString());
                }
            }else if(StringUtils.equals("Size", entry.getKey())){
                if(returnObjects != null
                        && (returnObjects.isEmpty() || returnObjects.size() < Integer.parseInt(((Map<String, Object>)entry.getValue()).get("min").toString()))){
                    isError = true;
                    Map<String, Object> valueMap = (Map<String, Object>)entry.getValue();
                    int maxValue = ((Integer)valueMap.get("max")).intValue();
                    int minValue = ((Integer)valueMap.get("min")).intValue();
                    errMsg = StringUtils.replaceEach(this.getMessage(valueMap.get("message").toString()), new String[]{"{min}", "{max}"}, new String[]{String.valueOf(minValue), String.valueOf(maxValue)});
                }
            }

            if(isError){
                errors.rejectValue(String.format("%s%s", parentNestedPath, field.getName()), entry.getKey(), null, errMsg);
            }
        }
    }

    public String getMessage(String key) {
        String message = key;
        key = StringUtils.replaceEach(key, new String[]{"{", "}"}, new String[]{"", ""});

        PlatformResourceBundleLocator bundleLocator = new PlatformResourceBundleLocator("org.hibernate.validator.ValidationMessages");
        ResourceBundle resourceBundle = bundleLocator.getResourceBundle(Locale.getDefault());

        try {
            message = resourceBundle.getString(key);
        }catch(MissingResourceException e) {
            log.error("getMessage Error", e);
        }

        return message;
    }
}
