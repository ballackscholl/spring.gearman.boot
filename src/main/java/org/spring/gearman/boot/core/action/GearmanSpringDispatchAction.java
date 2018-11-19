package org.spring.gearman.boot.core.action;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.gearman.client.GearmanJobResult;
import org.gearman.util.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spring.gearman.boot.config.ActionConfig;
import org.spring.gearman.boot.config.ActionConfigRegistry;
import org.spring.gearman.boot.core.ActionJobResult;
import org.spring.gearman.boot.core.action.annotation.GmOuputLog;
import org.spring.gearman.boot.core.action.annotation.Param;
import org.spring.gearman.boot.utils.JsonMessageOperation;
import org.spring.gearman.boot.utils.JsonRequestMessage;
import org.spring.gearman.boot.utils.JsonResponseMessage;
import org.spring.gearman.boot.utils.NoCatchException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class GearmanSpringDispatchAction extends GearmanAction {

	private static final Logger LOGGER = LoggerFactory.getLogger(GearmanSpringDispatchAction.class);

	// 解析请求错误错误
	private static int ERROR_CODE_PARSE_REQUEST_ERROR = 1;
	// 消息格式错误
	private static int ERROR_CODE_MESSAGE_FORMAT_WRONG = 2;
	// 处理消息的action未找到
	private static int ERROR_CODE_ACTION_NOT_FOUND = 3;
	// 无此方法
	private static int ERROR_CODE_NO_SUCH_METHOD = 4;
	// 参数转换异常
	private static int ERROR_CODE_PARAM_CONVERT_FAILED = 5;
	// 参数错误
	private static int ERROR_CODE_PARAM_ILLEGAL = 6;
	// 处理异常
	private static int ERROR_CODE_PROCESS_ERROR = 7;
	// 未知错误
	private static int ERROR_CODE_UNKNOWN_ERROR = 100;

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static List<Class> SIMPLE_CLASSES = CollectionUtils
			.arrayToList(new Class[]{Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class,
					Double.class, String.class, Date.class, BigDecimal.class, BigInteger.class, Boolean.class});

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static List<String> SIMPLE_TYPE_NAMES = CollectionUtils
			.arrayToList(new String[]{"char", "byte", "short", "int", "long", "float", "double", "boolean"});

	private ActionConfigRegistry configRegistry;

	@Override
	protected void afterInitialize() {
		configRegistry = this.getApplicationContext().getBean(ActionConfigRegistry.class);
	}


	@Override
	public GearmanJobResult execute() {
		String jsonReq = null;
		try {
			jsonReq = ByteUtils.fromUTF8Bytes(this.getRequest());
		} catch (Exception e) {
			String errorMessage = "parse message from request failed.";
			LOGGER.error(errorMessage, e);
			return generateExceptionResult(ERROR_CODE_PARSE_REQUEST_ERROR, errorMessage, null);
		}

		return dispatch(jsonReq);
	}

	public GearmanJobResult dispatch(String jsonReq) {

		JsonRequestMessage request = null;
		boolean isOutputLog = true;

		try {
			request = JsonMessageOperation.decode(jsonReq);
		} catch (Exception e) {
			String errorMessage = "decode message failed. message=" + jsonReq;
			LOGGER.warn(errorMessage);
			return generateExceptionResult(ERROR_CODE_MESSAGE_FORMAT_WRONG, errorMessage, null);
		}

		String actionName = request.getMethod();

		LOGGER.info("REQUEST---> actionName=" + actionName + ", content=" + request.getContent());

		if (actionName == null) {
			String errorMessage = "illegal message, method is null";
			LOGGER.warn(errorMessage);
			return generateExceptionResult(ERROR_CODE_MESSAGE_FORMAT_WRONG, errorMessage, null);
		}

		JsonResponseMessage resp = new JsonResponseMessage();
		ActionConfig actionConfig = configRegistry.getConfig(actionName);

		if (actionConfig == null) {
			String errorMessage = "actionConfig not found. actionName=" + actionName;
			LOGGER.warn("actionConfig not found. actionName=" + actionName);
			return generateExceptionResult(ERROR_CODE_ACTION_NOT_FOUND, errorMessage, null);
		}
		int returnCode = 0;
		String message = null;
		Object action = actionConfig.getAction();
		Method method = getTargetMethod(action, actionConfig.getMethod());
		if (method == null) {
			// 没有此方法
			String errorMessage = "no such method. method=" + actionName;
			LOGGER.warn(errorMessage);
			return generateExceptionResult(ERROR_CODE_NO_SUCH_METHOD, errorMessage, null);
		} else {


			GmOuputLog gmOuputLog = method.getAnnotation(GmOuputLog.class);
			if (gmOuputLog != null) {
				isOutputLog = gmOuputLog.output();
			}

			Object[] paramValues = null;
			try {
				paramValues = getMethodParameterValues(action.getClass(), method, request.getContent());
			} catch (Exception e) {
				String errorMessage = "convert request param to method param failed.";
				LOGGER.warn(errorMessage, e);
				return generateExceptionResult(ERROR_CODE_PARAM_CONVERT_FAILED, errorMessage, null);
			}

			@SuppressWarnings("rawtypes")
			InvokeResult invokeResult;
			try {
				invokeResult = (InvokeResult<?>) method.invoke(action, paramValues);

				if (actionConfig.isRetStream()) {

					if (invokeResult != null) {
						byte[] buffer = (byte[]) invokeResult.getData();
						if (isOutputLog) {
							LOGGER.info("<---RESPONSE buffer length:" + buffer.length);
						}
						return new ActionJobResult(buffer);
					} else {
						LOGGER.warn("isRetStream invokeResult is null");
						throw new RuntimeException("invokeResult is null");
					}
				}

				if (invokeResult != null) {
					returnCode = invokeResult.getReturnCode();
					message = invokeResult.getMessage();
					resp.setContent(invokeResult.getData());

					if (returnCode != 0) {
						LOGGER.warn("actionName=" + actionName + ", returnCode=" + returnCode + ", message=" + message);
					}
				}
			} catch (IllegalAccessException e) {
				String errorMessage = "illegal argument. actionName=" + actionName;
				LOGGER.warn(errorMessage, e);
				return generateExceptionResult(ERROR_CODE_PARAM_ILLEGAL, errorMessage, null);
			} catch (IllegalArgumentException e) {
				String errorMessage = "invocation error. actionName=" + actionName;
				LOGGER.warn(errorMessage, e);
				return generateExceptionResult(ERROR_CODE_PROCESS_ERROR, errorMessage, null);
			} catch(NoCatchException e){
				throw e;
			} catch (Exception e) {
				String errorMessage = "unknown error. actionName=" + actionName;
				LOGGER.warn(errorMessage, e);
				return generateExceptionResult(ERROR_CODE_UNKNOWN_ERROR, errorMessage, null);
			}
		}


		resp.setRet(returnCode);
		resp.setMsg(message);

		return generateResult(resp, isOutputLog);
	}

	private ActionJobResult generateExceptionResult(int ret, String msg, Object content) {
		JsonResponseMessage response = new JsonResponseMessage();
		response.setRet(ret);
		response.setMsg(msg);
		response.setContent(content);
		return generateResult(response, true);
	}

	private ActionJobResult generateResult(JsonResponseMessage response, boolean isOutputLog) {
		String result = JsonMessageOperation.encode(response);
		if (isOutputLog) {
			LOGGER.info("<---RESPONSE " + result);
		}
		return new ActionJobResult(ByteUtils.toUTF8Bytes(result));
	}

	@SuppressWarnings({ "rawtypes" })
	private Object[] getMethodParameterValues(Class actionClass, Method method, String content)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, Exception {

		Parameter[] parameters = getMethodParameters(method);

		Object[] paramValues = new Object[parameters.length];

		if (parameters.length == 0) {
			return paramValues;
		}

		// 参数类型可能有简单类型、对象类型、容器类型、Map类型，对象类型无需注解
		// 校验需要加注解的参数注解是否合法
		checkMethodParameterAnnotation(parameters);

		JSONObject jsonObject = JSONObject.parseObject(content);

		for (int i = 0; i < parameters.length; i++) {
			String paramName = parameters[i].name;
			Class paramClazz = parameters[i].paramClazz;
			Type elementType = parameters[i].elementType;
			String dateFormat = parameters[i].dateFormat;
			if (isSimpleType(paramClazz)) {
				// 简单类型
				if (jsonObject.containsKey(paramName)) {
					Object paramValue = jsonObject.get(paramName);
					if (paramValue != null) {
						paramValues[i] = getSimpleValue(paramName, paramClazz, paramValue, dateFormat);
					}else {
						paramValues[i] = getSimpleTypeNullVale(paramClazz);
					}
				}
			} else if (isCollectionType(paramClazz)) {
				// 容器类型
				JSONArray jsonArray = (JSONArray) jsonObject.get(paramName);
				if (jsonArray != null) {
					Collection collection = getCollectionValue(paramClazz, elementType, jsonArray, dateFormat);
					paramValues[i] = collection;
				}
			} else if (isMapType(paramClazz)) {
				// Map
				JSONObject paramValue = (JSONObject) jsonObject.get(paramName);
				if (paramValue != null) {
					Map map = getMapValue(paramClazz, elementType, paramValue, dateFormat);
					paramValues[i] = map;
				}
			} else {
				// 对象类型
				if (StringUtils.isEmpty(paramName)) {
					paramValues[i] = getObjectValue(paramClazz, jsonObject);
				} else {
					JSONObject paramValue = (JSONObject) jsonObject.get(paramName);
					if (paramValue != null) {
						paramValues[i] = getObjectValue(paramClazz, paramValue);
					}
				}
			}
		}
		return paramValues;

	}

	/**
	 * 参数类型可能有简单类型、对象类型、容器类型、Map类型，对象类型和Map类型无需注解
	 */
	@SuppressWarnings("rawtypes")
	private void checkMethodParameterAnnotation(Parameter[] parameters) {
		if (parameters == null || parameters.length == 0)
			return;
		if (parameters.length == 1) {
			// 如果只有一个参数，当参数类型为Map或对象类型时，无需注解
			Class paramClazz = parameters[0].paramClazz;
			if (isSimpleType(paramClazz) || isCollectionType(paramClazz)) {
				if (StringUtils.isEmpty(parameters[0].name)) {
					throw new IllegalArgumentException(
							"param must set annotation while the only one param is simple or collection type.");
				}
			}
		} else {
			// 有多个参数时，对象类型可以注解，也可不注解，其他类型必须注解
			for (int i = 0; i < parameters.length; i++) {
				Class paramClazz = parameters[i].paramClazz;
				if (isSimpleType(paramClazz) || isCollectionType(paramClazz) || isMapType(paramClazz)) {
					if (StringUtils.isEmpty(parameters[i].name)) {
						throw new IllegalArgumentException(
								"param must set annotation while there are more than one params and they are not object type.");
					}
				}
			}
		}
	}

	protected ActionJobResult defaultMethod(Object param) {
		return new ActionJobResult(false);
	}

	/**
	 * 根据属性的具体类型获取真实值
	 * 
	 * @param fieldClass
	 * @param fieldValue
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private Object getSimpleValue(String fieldName, Class fieldClass, Object fieldValue, String dateFormat) {
		// 兼容处理空字符串
		if (fieldValue instanceof String && !fieldClass.equals(String.class)) {
			if (((String) fieldValue).isEmpty())
				return null;
		}
		
		if (fieldClass.equals(String.class)) {
			return fieldValue.toString();
		} else if (fieldClass.equals(Long.class) || fieldClass.getSimpleName().equals("long")) {
			return Long.valueOf(fieldValue.toString());
		} else if (fieldClass.equals(Integer.class) || fieldClass.getSimpleName().equals("int")) {
			return Integer.valueOf(fieldValue.toString());
		} else if (fieldClass.equals(Double.class) || fieldClass.getSimpleName().equals("double")) {
			return Double.valueOf(fieldValue.toString());
		} else if (fieldClass.equals(Float.class) || fieldClass.getSimpleName().equals("float")) {
			return Float.valueOf(fieldValue.toString());
		} else if (fieldClass.equals(Boolean.class) || fieldClass.getSimpleName().equals("boolean")) {
			return new Boolean(fieldValue.toString());
		} else if (fieldClass.equals(BigDecimal.class)) {
			return new BigDecimal(fieldValue.toString());
		} else if (fieldClass.equals(BigInteger.class)) {
			return new BigInteger(fieldValue.toString());
		} else if (fieldClass.equals(Date.class)) {
			// 添加对日期格式的处理
			if (StringUtils.isEmpty(dateFormat)) {
				// 未设置格式时，按照毫秒处理
				return new Date(Long.valueOf(fieldValue.toString()));
			} else {
				// 按照指定格式处理
				SimpleDateFormat format = new SimpleDateFormat(dateFormat);
				try {
					return format.parse(fieldValue.toString());
				} catch (ParseException e) {
					throw new IllegalArgumentException("parse date format failed. paramName=" + fieldName + ", format="
							+ dateFormat + ", value=" + fieldValue);
				}
			}
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	private Object getObjectValue(Class objectClass, JSONObject jsonObject) throws Exception {

		Object targetObject = objectClass.newInstance();
		// 遍历对象所有属性，在JSON对象中找对应键值
		Set keySet = jsonObject.keySet();
		for (Object key : keySet) {
			String fieldName = (String) key;
			Field field = null;
			try {
				field = objectClass.getDeclaredField(fieldName);
			} catch (Exception e) {
				LOGGER.warn("No field named [" + fieldName + "] in class " + objectClass.getName());
			}
			if (field == null)
				continue;
			Object jsonValue = jsonObject.get(fieldName);
			if (jsonValue == null)
				continue;
			Type fieldType = field.getGenericType();
			Parameter parameter = getFieldParameter(field);
			// 根据属性类型获取属性真实值
			Object fieldValue = null;
			if (fieldType instanceof ParameterizedType) {
				// 属性类型为泛型，需要确认泛型具体类型
				ParameterizedType fieldFullType = (ParameterizedType) fieldType;
				Class fieldClass = (Class) fieldFullType.getRawType();
				if (isCollectionType(fieldClass)) {
					// 如果是容器
					JSONArray jsonArray = (JSONArray) jsonValue;
					Type elementType = fieldFullType.getActualTypeArguments()[0];
					Collection collection = getCollectionValue(fieldClass, elementType, jsonArray, parameter.dateFormat);
					fieldValue = collection;
				} else if (isMapType(fieldClass)) {
					// 如果是Map
					JSONObject jsonMapObject = (JSONObject) jsonValue;
					Type valueType = fieldFullType.getActualTypeArguments()[1];
					Map map = getMapValue(fieldClass, valueType, jsonMapObject, parameter.dateFormat);
					fieldValue = map;
				}
			} else {
				Class fieldClass = (Class) fieldType;
				if (isSimpleType(fieldClass)) {
					fieldValue = getSimpleValue(parameter.name, fieldClass, jsonValue, parameter.dateFormat);
				} else {
					JSONObject innerJsonObject = (JSONObject) jsonValue;
					Object innerObject = getObjectValue(fieldClass, innerJsonObject);
					fieldValue = innerObject;
				}
			}
			// 设置属性为可访问(必须，否则当field为private时，抛出无法访问异常)
			field.setAccessible(true);
			// 反射设置属性值
			field.set(targetObject, fieldValue);
		}
		return targetObject;

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection getCollectionValue(Class collectionType, Type elementType, JSONArray jsonArray, String dateFormat)
			throws Exception {
		Collection collection = createCollection(collectionType, null);
		if (elementType instanceof ParameterizedType) {
			// 元素为容器或Map类型
			ParameterizedType elementFullType = (ParameterizedType) elementType;
			Type[] elementArgTypes = elementFullType.getActualTypeArguments();
			// 元素类型
			Class elementClass = (Class) elementFullType.getRawType();
			if (isCollectionType(elementClass)) {
				Type innerElementType = elementArgTypes[0];
				Iterator iterator = jsonArray.iterator();
				// 遍历列表
				while (iterator.hasNext()) {
					// 按照类型创建内部元素
					JSONArray innerArray = (JSONArray) iterator.next();
					Collection elementCollection = getCollectionValue(elementClass, innerElementType, innerArray,
							dateFormat);
					// 加入到外部集合中
					collection.add(elementCollection);
				}
			} else if (isMapType(elementClass)) {
				Type valueType = elementArgTypes[1];
				Iterator iterator = jsonArray.iterator();
				// 遍历列表
				while (iterator.hasNext()) {
					// 按照类型创建内部元素
					JSONObject jsonObject = (JSONObject) iterator.next();
					Map mapElement = getMapValue(elementClass, valueType, jsonObject, dateFormat);
					collection.add(mapElement);
				}
			}
		} else {
			// 元素为简单类型或对象类型
			Class elementClass = (Class) elementType;
			if (isSimpleType(elementClass)) {
				Iterator iterator = jsonArray.iterator();
				// 遍历列表
				while (iterator.hasNext()) {
					Object value = iterator.next();
					Object realValue = getSimpleValue("", elementClass, value, dateFormat);
					// 加入到外部集合中
					collection.add(realValue);
				}
			} else {
				Iterator iterator = jsonArray.iterator();
				// 遍历列表
				while (iterator.hasNext()) {
					JSONObject jsonObject = (JSONObject) iterator.next();
					Object targetObject = getObjectValue(elementClass, jsonObject);
					// 加入到外部集合中
					collection.add(targetObject);
				}
			}
		}
		return collection;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map getMapValue(Class mapClass, Type valueType, JSONObject jsonMapObject, String dateFormat)
			throws Exception {
		Map map = createMap(mapClass);
		if (valueType instanceof ParameterizedType) {
			// value为容器或Map类型
			ParameterizedType valueFullType = (ParameterizedType) valueType;
			Class valueClass = (Class) valueFullType.getRawType();
			if (isCollectionType(valueClass)) {
				Type elementType = valueFullType.getActualTypeArguments()[0];
				Set keySet = jsonMapObject.keySet();
				for (Object key : keySet) {
					JSONArray jsonArray = (JSONArray) jsonMapObject.get(key);
					Collection collection = getCollectionValue(valueClass, elementType, jsonArray, dateFormat);
					map.put(key, collection);
				}
			} else if (isMapType(valueClass)) {
				Type innerValueType = valueFullType.getActualTypeArguments()[1];
				Set keySet = jsonMapObject.keySet();
				for (Object key : keySet) {
					JSONObject jsonObject = (JSONObject) jsonMapObject.get(key);
					Map innerMap = getMapValue(valueClass, innerValueType, jsonObject, dateFormat);
					map.put(key, innerMap);
				}
			}
		} else {
			Class valueClass = (Class) valueType;
			// value为简单或对象类型
			if (isSimpleType(valueClass)) {
				Set keySet = jsonMapObject.keySet();
				for (Object key : keySet) {
					Object object = jsonMapObject.get(key);
					Object realValue = getSimpleValue("", valueClass, object, dateFormat);
					map.put(key, realValue);
				}
			} else {
				Set keySet = jsonMapObject.keySet();
				for (Object key : keySet) {
					JSONObject jsonObject = (JSONObject) jsonMapObject.get(key);
					Object targetObject = getObjectValue(valueClass, jsonObject);
					map.put(key, targetObject);
				}
			}
		}
		return map;
	}

	private Method getTargetMethod(Object handler, String methodName) {
		if (handler == null)
			return null;
		Method[] methods = handler.getClass().getMethods();
		for (Method method : methods) {
			if (method.getName().equals(methodName))
				return method;
		}
		return null;
	}

	/**
	 * 判断是否是简单类型
	 * 
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private static boolean isSimpleType(Class clazz) {
		String simpleName = clazz.getSimpleName();
		boolean isSimpleType = SIMPLE_CLASSES.contains(clazz) || SIMPLE_TYPE_NAMES.contains(simpleName);
		return isSimpleType;
	}

	@SuppressWarnings("rawtypes")
	private static boolean isCollectionType(Class clazz) {
		return Collection.class.isAssignableFrom(clazz);
	}

	@SuppressWarnings("rawtypes")
	private static boolean isMapType(Class clazz) {
		return Map.class.isAssignableFrom(clazz);
	}

	@SuppressWarnings("rawtypes")
	private Parameter[] getMethodParameters(Method method) {
		Class[] paramClasses = method.getParameterTypes();

		Type[] paramTypes = method.getGenericParameterTypes();

		int paramNum = paramClasses.length;
		Parameter[] parameters = new Parameter[paramNum];
		Annotation[][] annotations = method.getParameterAnnotations();
		for (int i = 0; i < annotations.length; i++) {
			parameters[i] = new Parameter();
			Type paramType = paramTypes[i];
			parameters[i].paramType = paramType;
			// 设置参数类型或内部元素类型
			getTypeAndClass(parameters[i], paramType);

			Annotation[] annot = annotations[i];
			for (int j = 0; j < annot.length; j++) {
				if (Param.class.equals(annot[j].annotationType())) {
					Param param = (Param) annot[j];
					parameters[i].name = param.name();
					parameters[i].dateFormat = param.dateFormat();
				}
			}
		}
		return parameters;
	}

	private Parameter getFieldParameter(Field field) {
		Parameter parameter = new Parameter();
		Param param = field.getAnnotation(Param.class);
		if (param == null) {
			parameter.name = field.getName();
		} else {
			parameter.name = param.name();
			parameter.dateFormat = param.dateFormat();
		}
		Type fieldType = field.getGenericType();
		// 设置属性类型或内部元素类型
		getTypeAndClass(parameter, fieldType);
		return parameter;
	}

	@SuppressWarnings("rawtypes")
	private void getTypeAndClass(Parameter parameter, Type type) {
		if (type instanceof ParameterizedType) {
			ParameterizedType fullType = (ParameterizedType) type;
			Class paramClazz = (Class) fullType.getRawType();
			parameter.paramClazz = paramClazz;
			Type elementType = null;
			if (isCollectionType(paramClazz)) {
				elementType = fullType.getActualTypeArguments()[0];
			} else if (isMapType(paramClazz)) {
				elementType = fullType.getActualTypeArguments()[1];
			}
			parameter.elementType = elementType;
			if (elementType instanceof ParameterizedType) {
				parameter.elementClazz = (Class) ((ParameterizedType) elementType).getRawType();
			} else {
				parameter.elementClazz = (Class) elementType;
			}
		} else {
			parameter.paramClazz = (Class) type;
		}
	}

	static class Parameter {
		String name = "";
		@SuppressWarnings("rawtypes")
		Class elementClazz;
		@SuppressWarnings("rawtypes")
		Class paramClazz;

		Type paramType;

		Type elementType;
		// 参数格式,比如参数类型为Date时，参数格式为“yyyy-MM-dd”
		String dateFormat;
	}

	@SuppressWarnings("unchecked")
	private static <E> Collection<E> createCollection(Class<?> collectionType, Class<E> elementType) {
		if (collectionType.isInterface()) {
			if (Set.class.equals(collectionType) || Collection.class.equals(collectionType)) {
				return new LinkedHashSet<E>();
			} else if (List.class.equals(collectionType)) {
				return new LinkedList<E>();
			} else if (SortedSet.class.equals(collectionType) || NavigableSet.class.equals(collectionType)) {
				return new TreeSet<E>();
			} else {
				throw new IllegalArgumentException("Unsupported Collection interface: " + collectionType.getName());
			}
		} else {
			if (!Collection.class.isAssignableFrom(collectionType)) {
				throw new IllegalArgumentException("Unsupported Collection type: " + collectionType.getName());
			}
			try {
				return (Collection<E>) collectionType.newInstance();
			} catch (Exception ex) {
				throw new IllegalArgumentException(
						"Could not instantiate Collection type: " + collectionType.getName(), ex);
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	private static <K, V> Map<K, V> createMap(Class<?> mapType) {
		if (mapType.isInterface()) {
			if (Map.class.equals(mapType)) {
				return new LinkedHashMap<K, V>();
			} else if (SortedMap.class.equals(mapType) || NavigableMap.class.equals(mapType)) {
				return new TreeMap<K, V>();
			} else {
				throw new IllegalArgumentException("Unsupported Map interface: " + mapType.getName());
			}
		} else {
			if (!Map.class.isAssignableFrom(mapType)) {
				throw new IllegalArgumentException("Unsupported Map type: " + mapType.getName());
			}
			try {
				return (Map<K, V>) mapType.newInstance();
			} catch (Exception ex) {
				throw new IllegalArgumentException("Could not instantiate Map type: " + mapType.getName(), ex);
			}
		}
	}
	
	

    private Object getSimpleTypeNullVale(Class<?> type) {
    	switch(type.getName()) {
    		case "char": 	return new Character((char) 0x00);
    		case "byte": 	return new Byte((byte) 0x00);
    		case "short": 	return new Short((short) 0);
    		case "int": 	return new Integer(0);
    		case "long": 	return new Long(0);
    		case "float": 	return new Float(0.0);
    		case "double": 	return new Double(0.0);
    		case "boolean": return new Boolean(false);
    		default:return null;
    	}
    }





}
