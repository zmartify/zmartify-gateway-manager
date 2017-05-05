package com.zmartify.iotf.tools.gateway.factory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ibm.iotf.client.IoTFCReSTException;
import com.zmartify.iotf.tools.api.ZmartifyAPIClient;
import com.zmartify.iotf.tools.gateway.ZmartifyDeviceType;
import static com.zmartify.iotf.tools.gateway.WCConstants.*;

public class FactoryPhysicalInterfaces {

	ZmartifyAPIClient apiClient = null;

	public FactoryPhysicalInterfaces(ZmartifyAPIClient apiClient) {
		super();
		this.apiClient = apiClient;
	}

	/**
	 * 
	 * @param name
	 * @param description
	 * 
	 * @return id as String
	 * @throws IoTFCReSTException 
	 */
	private String addPhysicalInterface(String name, String description) throws IoTFCReSTException {
		if (!apiClient.isPhysicalInterfaceExistByName(name)) {
			return apiClient.addPhysicalInterface(name, description).get("id").getAsString();
		} else {
			JsonArray resultArray = apiClient.getPhysicalInterfaceByName(name).get("results").getAsJsonArray();
			if (resultArray.size() > 0 ) {
				// Return first object
				return resultArray.get(0).getAsJsonObject().get("id").getAsString();
			}
		}
		return null;
	}

	public void removePhyscialInterfaces() {
		
	}

	public void createPhysicalInterface(String eventId, String eventType, String deviceType, String physicalInterfaceName, String description) throws IoTFCReSTException {
		JsonObject response = new JsonObject();
		
		String physicalInterfaceId = addPhysicalInterface(physicalInterfaceName, description);

		if (physicalInterfaceId != null) {
			// Let's continue, if EventId exists, we first need to remove it
			if (apiClient.isEventIdExist(physicalInterfaceId, eventId)) {
				apiClient.removeEventId(physicalInterfaceId, eventId);
			}
			JsonArray resultArray = apiClient.getEventTypeByName(eventType).get("results").getAsJsonArray();
			if (resultArray.size() > 0 ) {
				// Return first object
				String eventTypeId = resultArray.get(0).getAsJsonObject().get("id").getAsString();
				apiClient.attachEventId(physicalInterfaceId, eventId, eventTypeId);
			}
			response = apiClient.addPhyscialInterfaceToDeviceType(deviceType, physicalInterfaceId);
		}
	}

	public void createPhysicalInterfaces() {
		for (ZmartifyDeviceType zmDT : ZmartifyDeviceType.values()) {
			try {
				createPhysicalInterface(EVENTID,zmDT.getEventType() , zmDT.getDeviceType(), zmDT.getPhysicalInterfaceName(), zmDT.getDescription());
				System.out.println("Created physical interface: " + zmDT.getPhysicalInterfaceName());
			} catch (IoTFCReSTException e) {
				System.out.println("ERROR: Trying to create Physical Interface: " + zmDT.getPhysicalInterfaceName() + " ::" + e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
}
