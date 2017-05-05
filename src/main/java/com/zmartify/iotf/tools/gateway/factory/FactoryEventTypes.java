package com.zmartify.iotf.tools.gateway.factory;

import java.io.File;
import java.util.ArrayList;

import org.apache.commons.io.FilenameUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ibm.iotf.client.IoTFCReSTException;
import com.zmartify.iotf.tools.api.ZmartifyAPIClient;
import com.zmartify.iotf.tools.gateway.WatsonControl;

public class FactoryEventTypes {

    private ZmartifyAPIClient apiClient = null;
    private WatsonControl callBack = null;

    public FactoryEventTypes(ZmartifyAPIClient apiClient, WatsonControl callBack) {
        super();
        this.apiClient = apiClient;
        this.callBack = callBack;
    }

    private ArrayList<String> getEventTypeList() {
        ArrayList<String> list = new ArrayList<String>();
        File[] files = callBack.getResourceFiles("evt", "json");
        for (int i = 0; i < files.length; i++) {
            list.add(FilenameUtils.removeExtension(files[i].getName()));
        }
        return list;
    }

    public JsonObject createEventType(String eventName, String description) {
        JsonObject response = null;
        try {
            response = callBack.addSchema(eventName, "evt", description);
            String schemaId = response.get("id").getAsString();
            if (!apiClient.isEventTypeExistByName(eventName)) {
                return apiClient.addEventType(eventName, schemaId);
            } else {
                System.out.println("EventType already exists");
            }
        } catch (IoTFCReSTException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public boolean removeEventType(String name) {
        try {
            apiClient.deleteEventTypeByName(name);
            apiClient.deleteSchemaByName("evt/" + name);
            return true;
        } catch (IoTFCReSTException e) {
            System.out.println("Error removing EventType " + e.getHttpCode() + " ::" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void createEventTypes() {
        ArrayList<String> eventTypeList = getEventTypeList();
        eventTypeList.forEach(name -> {
            System.out.println("Event Type     : " + name);
            createEventType(name, "");
        });
        System.out.println("<---- eventTypes created.");
    }

    public void removeEventTypes() {
        String eventTypeName = null;
        try {
            /*
             * Loop through all eventTypes and delete them one by one
             */
            JsonArray evtList = apiClient.getEventTypes().get("results").getAsJsonArray();
            for (int i = 0; i < evtList.size(); i++) {
                eventTypeName = evtList.get(i).getAsJsonObject().get("name").getAsString();
                if (apiClient.deleteEventType(evtList.get(i).getAsJsonObject().get("id").getAsString())) {
                    System.out.println("EventType deleted    : " + eventTypeName);
                }
            }

        } catch (IoTFCReSTException e) {
            System.out.println(
                    "Error removing EventType " + eventTypeName + " (" + e.getHttpCode() + ") ::" + e.getMessage());
            e.printStackTrace();
            callBack.abortProgram(0);
        }
    }

}
