package com.zmartify.iotf.tools.gateway;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;

import com.google.gson.JsonObject;
import com.ibm.iotf.client.IoTFCReSTException;
import com.zmartify.iotf.tools.api.ZmartifyAPIClient;

public class FactoryApplicationInterfaces {

	private ZmartifyAPIClient apiClient = null;
	private WatsonControl callBack = null;

	public FactoryApplicationInterfaces(ZmartifyAPIClient apiClient, WatsonControl callBack) {
		super();
		this.apiClient = apiClient;
		this.callBack = callBack;
	}
	
	private ArrayList<String> getApplicationInterfaceList() {
		ArrayList<String> list = new ArrayList<String>();
     	File [] files = callBack.getResourceFiles("api","json");
		for (int i = 0; i < files.length; i++) {
			list.add(FilenameUtils.removeExtension(files[i].getName()));
		}
		return list;
	}

	public JsonObject createApplicationInterface(String name, String description) {
		JsonObject response = null;
		try {
			response = callBack.addSchema(name,"api",description);
			String schemaId = response.get("id").getAsString();
			if (!apiClient.isApplicationInterfaceExistByName(name)) {
				System.out.println("ApplicationInterface created: " + name);
				return apiClient.addApplicationInterface(name, description, schemaId);
			} else {
				System.out.println("Application Interface already exists");
			}
		} catch (IoTFCReSTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean removeApplicationInterface(String name) {
		try {
			apiClient.deleteApplicationInterfaceByName(name);
			apiClient.deleteSchemaByName("api/" + name);
			return true;
		} catch (IoTFCReSTException e) {
			System.out.println("Error removing ApplicationInterface");
			e.printStackTrace();
			return false;
		}
	}
	
	public void createApplicationInterfaces() {
		ArrayList<String> applicationInterfaceList = getApplicationInterfaceList();
		applicationInterfaceList.forEach(name -> {
			System.out.println(name);
			removeApplicationInterface(name);
			createApplicationInterface(name, "");
		});
	}
}
