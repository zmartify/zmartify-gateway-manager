package com.zmartify.iotf.tools.gateway.factory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ibm.iotf.client.IoTFCReSTException;
import com.zmartify.iotf.tools.api.ZmartifyAPIClient;
import com.zmartify.iotf.tools.gateway.ZmartifyDeviceType;

public class FactoryDeviceTypes {
	
	private ZmartifyAPIClient apiClient = null;

	public FactoryDeviceTypes(ZmartifyAPIClient apiClient) {
		super();
		this.apiClient = apiClient;
	}
	
	public void removeDeviceTypes() {
		try {
			JsonArray devList = apiClient.getAllDeviceTypes().get("results").getAsJsonArray();
			for (int i = 0; i < devList.size(); i++) {
				String deviceType = devList.get(i).getAsJsonObject().get("id").getAsString();
				String classId = devList.get(i).getAsJsonObject().get("classId").getAsString();
				if (classId.equals("Device")) {
					JsonArray mapList = apiClient.getMappings(deviceType);
					for (int j = 0; j < mapList.size(); j++) {
						String applicationInterfaceId = mapList.get(j).getAsJsonObject().get("applicationInterfaceId")
								.getAsString();
						apiClient.deleteMappings(deviceType, applicationInterfaceId);
					}
					if (apiClient.deleteDeviceType(deviceType)) {
						System.out.println("Device type deleted : " + deviceType);
					}
				}
			}
		} catch (IoTFCReSTException e) {
			// TODO Auto-generated catch block
			System.out.println("Error removing device types: " + e.getHttpCode() + " ::" + e.getMessage());
			e.printStackTrace();
		}
	}

	public void createDeviceTypes() {
		for (ZmartifyDeviceType zmDT : ZmartifyDeviceType.values()) {
			try {
				if (!apiClient.isDeviceTypeExist(zmDT.getDeviceType())) {
					apiClient.addDeviceType(zmDT.getDeviceType(), zmDT.getDescription(), null, null); 
					System.out.println("Device Type created       : " + zmDT.getDeviceType());
				} else {
					System.out.println("Device Type already exists: " + zmDT.getDeviceType());
				}
			} catch (IoTFCReSTException e) {
				System.out.println("ERROR: Device Type already exists" + e.getMessage());
				e.printStackTrace();
			}
		}
	}
}
