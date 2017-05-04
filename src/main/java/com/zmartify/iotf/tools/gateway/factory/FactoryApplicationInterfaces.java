package com.zmartify.iotf.tools.gateway.factory;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ibm.iotf.client.IoTFCReSTException;
import com.zmartify.iotf.tools.api.ZmartifyAPIClient;
import com.zmartify.iotf.tools.gateway.WatsonControl;
import com.zmartify.iotf.tools.gateway.ZmartifyDeviceType;

public class FactoryApplicationInterfaces {

	private ZmartifyAPIClient apiClient = null;
	private WatsonControl callBack = null;

	public FactoryApplicationInterfaces(ZmartifyAPIClient apiClient, WatsonControl callBack) {
		super();
		this.apiClient = apiClient;
		this.callBack = callBack;
	}

	public String getId(JsonObject response) {
		return response.get("id").getAsString();
	}

	public boolean createApplicationInterface(ZmartifyDeviceType dt) {
		JsonObject response = null;
		String name = dt.getDeviceType();

		try {
			if (!apiClient.isApplicationInterfaceExistByName(name)) {
				response = callBack.addSchema(name, "api", dt.getDescription());
				String schemaId = response.get("id").getAsString();
				response = apiClient.addApplicationInterface(name, dt.getDescription(), schemaId);
			} else {
				response = apiClient.getApplicationInterfaceByName(name);
			}
			String applicationInterfaceId = response.get("results").getAsJsonArray().get(0).getAsJsonObject().get("id").getAsString();
			JsonArray apiList = apiClient.getDeviceTypeApplicationInterfaces(name);
			boolean exists = false;
			for (int i = 0; i < apiList.size() && !exists; i++) {
				if (apiList.get(i).getAsJsonObject().get("name").getAsString().equals(name)) {
					// Return if interface is already attached
					exists = true;
				}
			}
			if (!exists)
				apiClient.attachApplicationInterface(name, response);

			/*
			 * Now we will add the mapping to the deviceType, first check if it exists
			 */
			try {
				response = apiClient.getMappings(dt.getDeviceType(), applicationInterfaceId);
				response = apiClient.updateMappings(dt.getDeviceType(), dt.getMAPJson(applicationInterfaceId));
				System.out.println("Mapping updated");
			} catch (IoTFCReSTException e) {
				if (e.getHttpCode() == 404) {
					// Mapping does not exist, let's add it
					response = apiClient.addMappings(dt.getDeviceType(), dt.getMAPJson(applicationInterfaceId));
					System.out.println("Mapping added");
				} else {
					// We got a real error, let's throw the error again.
					throw e;
				}
			}
			
		} catch (IoTFCReSTException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		try {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			response = apiClient.validateConfiguration(dt.getDeviceType());
			System.out.println(gson.toJson(response));

		} catch (IoTFCReSTException e) {
			System.out.println("Problems validating - "  + e.getMessage());
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}

	public boolean removeApplicationInterface(ZmartifyDeviceType dt) {
		String applicationInterfaceId = null;
		try {
			JsonArray apiList = apiClient.getDeviceTypeApplicationInterfaces(dt.getDeviceType());
			for (int i = 0; i < apiList.size(); i++) {
				JsonObject apiJson = apiList.get(i).getAsJsonObject();
				if (apiJson.get("name").getAsString().equals(dt.getDeviceType())) {
					applicationInterfaceId = apiJson.get("id").getAsString();
				}
			}
			if (applicationInterfaceId != null) {
				apiClient.removeApplicationInterface(dt.getDeviceType(), applicationInterfaceId);
			}
			apiClient.deleteApplicationInterfaceByName(dt.getDeviceType());
			apiClient.deleteSchemaByName("api/" + dt.getDeviceType());
			return true;
		} catch (IoTFCReSTException e) {
			System.out.println("Error removing ApplicationInterface");
			e.printStackTrace();
			return false;
		}
	}

	public void createApplicationInterfaces() {
		for (ZmartifyDeviceType dt : ZmartifyDeviceType.values()) {
			String name = dt.getDeviceType();
			callBack.writeResourceFile(name, dt.getAPIJson(), "api");
			// removeApplicationInterface(dt);
			if (createApplicationInterface(dt)) {
				System.out.println("Application interface created: " + name);
			}
		}
	}
}
