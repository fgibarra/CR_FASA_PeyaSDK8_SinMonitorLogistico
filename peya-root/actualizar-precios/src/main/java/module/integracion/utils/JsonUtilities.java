package module.integracion.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class JsonUtilities extends cl.ahumada.esb.utils.json.JSonUtilities {

	
	private static JsonUtilities instance;
	
	public static JsonUtilities getInstance() {
		if (instance == null)
			instance = new JsonUtilities();
		return instance;
	}
	
	public String toJsonString(Class<?> clazz, Object data) {
		return toJsonString(clazz, data, false);
	}
	
	public String toJsonString(Class<?> clazz, Object data, Boolean unwrapRoot) {
		StringBuffer sb = null;
		if (unwrapRoot)
			sb = new StringBuffer(String.format("{\n"));
		else
			sb = new StringBuffer(String.format("%s {\n", clazz.getSimpleName()));
		
		Method[] methods = clazz.getDeclaredMethods();
		for (Method met : methods) {
			String name = met.getName();
			int parametersCount = met.getParameterCount();
			if (name.startsWith("get") && parametersCount == 0) {
				Class<?> returnType = met.getReturnType();
				
				Object valor = null;
				try {
					valor = met.invoke(data, (Object[])null);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
				if (valor != null) {
					if (returnType.isArray()) {
						Class<?> arrayType = returnType.getComponentType();
						sb.append(String.format("\t\"%s\": [\n", name.substring(3)));
						if (arrayType.isPrimitive()) {
							String vat = arrayType.getSimpleName();
							if (vat.equalsIgnoreCase("char")) {
								sb.append(dumpPrimitive(arrayType, (char[])valor));
							} else if (vat.equalsIgnoreCase("byte")){
								sb.append(Arrays.toString((byte[])valor));
							} else if (vat.equalsIgnoreCase("short")){
								sb.append(Arrays.toString((short[])valor));
							} else if (vat.equalsIgnoreCase("int")){
								sb.append(Arrays.toString((int[])valor));
							} else if (vat.equalsIgnoreCase("long")){
								sb.append(Arrays.toString((long[])valor));
							} else if (vat.equalsIgnoreCase("float")){
								sb.append(Arrays.toString((float[])valor));
							} else if (vat.equalsIgnoreCase("double")){
								sb.append(Arrays.toString((double[])valor));
							} else if (vat.equalsIgnoreCase("boolean")){
								sb.append(Arrays.toString((boolean[])valor));
							}
						} else {
							// es una clase
							if (arrayType == String.class ||
								arrayType == Integer.class || 
								arrayType == Long.class || 
								arrayType == Float.class || 
								arrayType == Short.class || 
								arrayType == Double.class ||
								arrayType == Boolean.class) {
								
								sb.append(dumpPrimitive(arrayType, (Object[])valor));
							} else {
								// una clase
								for (Object elemento : (Object[])valor) {
									sb.append(String.format("\t%s,\n", toJsonString(arrayType, elemento, true)));
								}
								sb.setLength(sb.lastIndexOf(","));
							}
						} 
						sb.append("\n],\n");
					} else {
						if (returnType == String.class) {
							sb.append(String.format("\t\"%s\": \"%s\",\n", name.substring(3), (String)valor));
						} else if (
							returnType == Integer.class || 
							returnType == Long.class || 
							returnType == Float.class || 
							returnType == Short.class || 
							returnType == Double.class ||
							returnType == Boolean.class)  {
							
							sb.append(String.format("\t\"%s\": %s,\n", name.substring(3), valor.toString()));
							
						} else {
							// una clase
							sb.append(String.format("\"%s\": %s,\n", name.substring(3), toJsonString(returnType, valor, true)));
						}
					}
				}
			}
		}
		int ultimaComa = sb.lastIndexOf(",");
		if (ultimaComa > 0) {
			sb.setLength(ultimaComa);
		}
			
		sb.append("\n}");
		return sb.toString();
	}

	private String dumpPrimitive(Class<?> arrayType, Object[] valores) {
		boolean conCremilla = false;
		if (arrayType == String.class)
			conCremilla = true;
		StringBuffer sb = new StringBuffer();
		for (Object obj : valores) {
			sb.append(String.format("%s%s%s, ", conCremilla?"\"":"",obj,conCremilla?"\"":""));
		}
		if (sb.length() > 0)
			sb.setLength(sb.lastIndexOf(","));
		return sb.toString();
	}
	private String dumpPrimitive(Class<?> arrayType, char[] valores) {
		StringBuffer sb = new StringBuffer();
		for (Object obj : valores) {
			sb.append(String.format("'%s', ",obj));
		}
		if (sb.length() > 0)
			sb.setLength(sb.lastIndexOf(","));
		return sb.toString();
	}
}
