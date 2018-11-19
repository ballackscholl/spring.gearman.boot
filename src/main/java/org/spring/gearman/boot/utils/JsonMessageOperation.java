package org.spring.gearman.boot.utils;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.fastjson.JSONObject;

public class JsonMessageOperation {
	
	public static JsonRequestMessage decode(String code) {
		
		JsonRequestMessage message = new JsonRequestMessage();

		JSONObject json = JSONObject.parseObject(code);
		if (json.containsKey("method")) {
			message.setMethod(json.getString("method"));
		}

		if (json.containsKey("content")) {
			message.setContent(json.getString("content"));
		}

		return message;
	}
	
	public static String encode(JsonResponseMessage message) {
		
		JSONObject jsonObject = new JSONObject();

		String msg = message.getMsg();
		Integer ret = message.getRet();
		Object content = message.getContent();

		Map<String, Object> map = new HashMap<String, Object>();
		map.put("ret", ret);
		map.put("msg", msg);
		if (content != null) {
			map.put("content", content);
		}
		
		jsonObject.putAll(map);
		return jsonObject.toJSONString();
	}


}
