package com.zmartify.iotf.tools.gateway.factory;

import com.ibm.iotf.client.IoTFCReSTException;
import com.zmartify.iotf.tools.api.ZmartifyAPIClient;
import com.zmartify.iotf.tools.gateway.ZmartifyDeviceType;

public class FactoryDeviceTypes {
	
	private ZmartifyAPIClient apiClient = null;

	public FactoryDeviceTypes(ZmartifyAPIClient apiClient) {
		super();
		this.apiClient = apiClient;
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
