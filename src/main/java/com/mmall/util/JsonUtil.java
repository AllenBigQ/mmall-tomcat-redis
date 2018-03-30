package com.mmall.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;
import java.text.SimpleDateFormat;

/**
 * Created by Allen
 */
@Slf4j
public class JsonUtil {
    //Jackson提供的ObjectMapper类是Jackson库的主要类
    //它提供一些功能可以使Java对象转换成json字符串
    private static ObjectMapper objectMapper = new ObjectMapper();
    //初始化 objectMapper
    static {
        //对象的所有字段全部列入 影响序列化的行为
        objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.ALWAYS);

        //取消默认转换timestamps形式 (默认是true,这里设置为false)
        objectMapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);

        //忽略空Bean转json的错误
        objectMapper.configure(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS, false);

        //所有的日期格式都统一为以下的样式，yyyy-MM-dd HH:mm:ss
        objectMapper.setDateFormat(new SimpleDateFormat(DateTimeUtil.STANDARD_FORMAT));

        //设置反序列化的属性
        //忽略在json字符串中存在，但是在java对象中不存在对应属性的情况，防止错误
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    //把对象转换成json字符串
    //list<User>集合也是可以转换的
    public static <T> String obj2String(T obj) {
        if (obj == null) {
            return null;
        }
        try {
            //如果obj是字符串就返回，否则转换成字符串
            return obj instanceof String ? (String) obj : objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Parse object to String error", e);
            return null;
        }
    }
    //返回一个已经格式化好的字符串，pretty
    public static <T> String obj2StringPretty(T obj) {
        if (obj == null) {
            return null;
        }
        try {
            return obj instanceof String ? (String) obj : objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("Parse object to String error", e);
            return null;
        }
    }
    //字符串转换成对象
    //<T>代表把此方法声明为泛型方法，第二个 T 代表返回值类型
    // 第三个Class<T>与List<String>是一样的 限制Class的类型
    //把一个字符串转换成一个List<User>需要一个List 一个User所以这个方法不适用
    //如果转换的话会变成LinkedHashMap(Jackson默认)，但getId()是取不出来的，因为没有这个方法
    public static <T> T string2Obj(String str, Class<T> clazz) {
        if (StringUtils.isEmpty(str) || clazz == null) {
            return null;
        }
        try {
            //如果这个class是String类型就直接返回，否则就转换并返回
            return clazz.equals(String.class) ? (T) str : objectMapper.readValue(str, clazz);
        } catch (Exception e) {
            log.warn("Parse String to Object error", e);
            return null;
        }
    }
    //对于复杂类型比如集合 可以使用下面两个方法
    //TypeReference一定要进入jackson的包
    //可以将字符串转换成集合形式List<User> Map<User,Category>
    public static <T> T string2Obj(String str, TypeReference<T> tTypeReference) {
        if (StringUtils.isEmpty(str) || tTypeReference == null) {
            return null;
        }
        try {
            return (T) (tTypeReference.getType().equals(String.class) ? str : objectMapper.readValue(str, tTypeReference));
            //使用说明
            /*
            * List<User> userListObj = JsonUtil.string2Obj(userListStr,new TypeReference<List<User>>(){} );
            * TypeReference是一个接口，但不需要实现，这样就可以了
            * */
        } catch (Exception e) {
            log.warn("Parse String to Object error", e);
            return null;
        }
    }
    public static <T> T string2Obj(String str, Class<T> collectionClass, Class<?>... elementClasses) {
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);
        try {
            return objectMapper.readValue(str, javaType);
            //使用说明
            /*
            * List<User> userListObj=JsonUtil.string2Obj(userListStr,List.class,User.class)
            * 放入字符串，集合的类型，集合中元素的类型;
            * */
        } catch (Exception e) {
            log.warn("Parse String to Object error", e);
            return null;
        }
    }

}
