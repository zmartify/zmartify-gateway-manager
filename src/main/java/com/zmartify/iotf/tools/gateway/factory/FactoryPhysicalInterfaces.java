package com.zmartify.iotf.tools.gateway.factory;

import static com.zmartify.iotf.tools.gateway.WCConstants.EVENTID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ibm.iotf.client.IoTFCReSTException;
import com.zmartify.iotf.tools.api.ZmartifyAPIClient;
import com.zmartify.iotf.tools.gateway.ZmartifyDeviceType;

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
            if (resultArray.size() > 0) {
                // Return first object
                return resultArray.get(0).getAsJsonObject().get("id").getAsString();
            }
        }
        return null;
    }

    public void removePhyscialInterfaces() {
        /*
         * Remove physicalInterfaces, but first remove all refereed eventIds
         */
        String physicalInterfaceId = null;
        try {
            JsonObject phyJson = new JsonObject();
            JsonArray phyList = apiClient.getAllPhyscialInterfaces().get("results").getAsJsonArray();

            do {
                /*
                 * Loop through all physicalInterfaces (25 at a time)
                 */
                for (int i = 0; i < phyList.size(); i++) {
                    physicalInterfaceId = phyList.get(i).getAsJsonObject().get("id").getAsString();
                    JsonArray eventIdList = apiClient.getEventIds(physicalInterfaceId);
                    System.out.print("Removing physicalInterface: "
                            + phyList.get(i).getAsJsonObject().get("name").getAsString() + " + eventId: ");

                    /*
                     * First remove all links between physicalInterfarve and eventIds
                     */
                    for (int j = 0; j < eventIdList.size(); j++) {
                        String eventId = eventIdList.get(j).getAsJsonObject().get("eventId").getAsString();
                        System.out.print(eventId + ", ");
                        apiClient.removeEventId(physicalInterfaceId, eventId);
                    }
                    System.out.println("..completed");
                    /*
                     * Second remove the refereed physical interface
                     */
                    apiClient.deletePhysicalInterface(physicalInterfaceId);
                }
                phyJson = apiClient.getAllPhyscialInterfaces();
                phyList = phyJson.get("results").getAsJsonArray();
            } while ((phyJson.get("meta").getAsJsonObject().get("total_rows").getAsInt() > 0));
            System.out.println("<---- physicalInterfaces deleted.");

        } catch (IoTFCReSTException e) {
            System.out.println("Error removing physical interface " + e.getHttpCode() + " ::" + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean createPhysicalInterface(String eventId, String eventType, String deviceType,
            String physicalInterfaceName, String description) {

        try {
            String physicalInterfaceId = addPhysicalInterface(physicalInterfaceName, description);
            if (physicalInterfaceId != null) {
                // Let's continue, if EventId exists, we first need to remove it
                if (apiClient.isEventIdExist(physicalInterfaceId, eventId)) {
                    apiClient.removeEventId(physicalInterfaceId, eventId);
                }
                JsonArray resultArray = apiClient.getEventTypeByName(eventType).get("results").getAsJsonArray();
                if (resultArray.size() > 0) {
                    // Return first object
                    String eventTypeId = resultArray.get(0).getAsJsonObject().get("id").getAsString();
                    apiClient.attachEventId(physicalInterfaceId, eventId, eventTypeId);
                } else {
                    System.out.println("ERROR: No event type " + eventType + " exists for physical interface "
                            + physicalInterfaceName);
                }
                apiClient.addPhyscialInterfaceToDeviceType(deviceType, physicalInterfaceId);
                System.out.println("Created physical interface: " + physicalInterfaceName);
            }
        } catch (IoTFCReSTException e) {
            System.out.println("ERROR: Trying to create Physical Interface: " + physicalInterfaceName + " ("
                    + e.getHttpCode() + ") ::" + e.getMessage());
            e.printStackTrace();
        }

        return true;
    }

    public void createPhysicalInterfaces() {
        for (ZmartifyDeviceType zmDT : ZmartifyDeviceType.values()) {
            if (createPhysicalInterface(EVENTID, zmDT.getEventType(), zmDT.getDeviceType(),
                    zmDT.getPhysicalInterfaceName(), zmDT.getDescription())) {
            }
        }
        System.out.println("<---- physicalInterfaces created.");
    }

}
