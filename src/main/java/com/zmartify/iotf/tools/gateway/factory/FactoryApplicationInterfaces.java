package com.zmartify.iotf.tools.gateway.factory;

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
        String applicationInterfaceId;

        try {
            if (!apiClient.isApplicationInterfaceExistByName(name)) {
                response = callBack.addSchema(name, "api", dt.getDescription());
                String schemaId = response.get("id").getAsString();
                response = apiClient.addApplicationInterface(name, dt.getDescription(), schemaId);
            } else {
                response = apiClient.getApplicationInterfaceByName(name).get("results").getAsJsonArray().get(0)
                        .getAsJsonObject();
            }
            applicationInterfaceId = response.get("id").getAsString();

            JsonArray apiList = apiClient.getDeviceTypeApplicationInterfaces(name);
            boolean exists = false;
            for (int i = 0; i < apiList.size() && !exists; i++) {
                if (apiList.get(i).getAsJsonObject().get("name").getAsString().equals(name)) {
                    // Return if interface is already attached
                    exists = true;
                }
            }
            if (!exists) {
                apiClient.attachApplicationInterface(name, response);
            }

            /*
             * Now we will add the mapping to the deviceType, first check if it exists
             */
            try {
                response = apiClient.getMappings(dt.getDeviceType(), applicationInterfaceId).getAsJsonObject();
                response = apiClient.updateMappings(dt.getDeviceType(), dt.getMAPJson(applicationInterfaceId));
            } catch (IoTFCReSTException e) {
                if (e.getHttpCode() == 404) {
                    // Mapping does not exist, let's add it
                    response = apiClient.addMappings(dt.getDeviceType(), dt.getMAPJson(applicationInterfaceId));
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
            if (response.get("failures").getAsJsonArray().size() == 0) {
                System.out.print("Configuration OK!...");
            } else {
                System.out.println(gson.toJson(response));
            }

        } catch (IoTFCReSTException e) {
            System.out.println("Problems validating - " + e.getMessage());
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;
    }

    public boolean removeApplicationInterface(String applicationInterfaceId) {
        try {
            // apiClient.removeApplicationInterface(deviceType, applicationInterfaceId);
            apiClient.deleteApplicationInterface(applicationInterfaceId);
            // apiClient.deleteSchemaByName("api/" + deviceType);
            return true;
        } catch (IoTFCReSTException e) {
            System.out.println("Error removing ApplicationInterface" + e.getHttpCode() + " ::" + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public void createApplicationInterfaces() {
        for (ZmartifyDeviceType dt : ZmartifyDeviceType.values()) {
            callBack.writeResourceFile(dt.getDeviceType(), dt.getAPIJson(), "api");
            if (createApplicationInterface(dt)) {
                System.out.println("application interface created: " + dt.getDeviceType());
            }
        }
        System.out.println("<---- applicationInterfaces created.");
    }

    public void removeApplicationInterfaces() {
        JsonArray apiList;
        try {
            do {
                // 25 at a time
                apiList = apiClient.getAllApplicationInterfaces().get("results").getAsJsonArray();
                for (int i = 0; i < apiList.size(); i++) {
                    removeApplicationInterface(apiList.get(i).getAsJsonObject().get("id").getAsString());
                }
            } while (apiList.size() >= 25);
        } catch (IoTFCReSTException e) {
            System.out.println("Error removing Application interfaces " + e.getHttpCode() + " ::" + e.getMessage());
            e.printStackTrace();
        }

    }

    public void removeSchemas() {
        JsonArray schemaList = null;
        String schemaId = null;
        String schemaName = null;
        try {
            do {
                // 25 at a time
                schemaList = apiClient.getAllSchemas().get("results").getAsJsonArray();
                for (int i = 0; i < schemaList.size(); i++) {
                    schemaId = schemaList.get(i).getAsJsonObject().get("id").getAsString();
                    schemaName = schemaList.get(i).getAsJsonObject().get("name").getAsString();
                    if (apiClient.deleteSchema(schemaId)) {
                        System.out.println("Schema deleted  : " + schemaName);
                    }
                }
            } while (schemaList.size() >= 25);
        } catch (IoTFCReSTException e) {
            System.out
                    .println("ERROR: deleting schema" + schemaName + " (" + e.getHttpCode() + ") ::" + e.getMessage());
            e.printStackTrace();
        }

    }
}
