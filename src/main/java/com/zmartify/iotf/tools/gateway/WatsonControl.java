/**
 *****************************************************************************
 * Copyright (c) 2016 IBM Corporation and other Contributors.

 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Patrizia Gufler1 - Initial Contribution
 * Sathiskumar Palaniappan - Initial Contribution
 *****************************************************************************
 */
package com.zmartify.iotf.tools.gateway;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.ibm.iotf.client.IoTFCReSTException;
import com.ibm.iotf.devicemgmt.DeviceData;
import com.ibm.iotf.devicemgmt.DeviceInfo;
import com.ibm.iotf.devicemgmt.gateway.ManagedGateway;
import com.zmartify.iotf.tools.api.ZmartifyAPIClient;
import com.zmartify.iotf.tools.gateway.factory.FactoryApplicationInterfaces;
import com.zmartify.iotf.tools.gateway.factory.FactoryDeviceTypes;
import com.zmartify.iotf.tools.gateway.factory.FactoryEventTypes;
import com.zmartify.iotf.tools.gateway.factory.FactoryPhysicalInterfaces;

/**
 * Gateways are a specialized class of devices in Watson IoT Platform which serve as access points
 * to the Watson IoT Platform for other devices. Gateway devices have additional permission when
 * compared to regular devices and can perform the following functions:
 *
 * 1. Register new devices to Watson IoT Platform
 * 2. Send and receive its own sensor data like a directly connected device,
 * 3. Send and receive data on behalf of the devices connected to it
 * 4. Run a device management agent, so that it can be managed, also manage the devices connected to it
 *
 * In this sample we demonstrate a sample home gateway that manages few attached home devices like,
 * Lights, Switchs, Elevator, Oven and OutdoorTemperature. And the following configuration is assumed,
 *
 * 1. Few devices are not manageable
 * 2. Few devices are manageable but accept only firmware
 * 3. Few devices are manageable but accept only Device actions
 * 4. Few devices are manageable and accept both firmware/device actions
 *
 * All devices publish events and few devices accept commands.
 *
 *
 */
public class WatsonControl {
    private final static String PROPERTIES_FILE_NAME = "/DMGatewaySample.properties";

    private ManagedGateway mgdGateway;
    private ZmartifyAPIClient apiClient;
    private String registrationMode;

    // Define factories
    FactoryApplicationInterfaces apiFactory;
    FactoryEventTypes evtFactory;
    FactoryDeviceTypes devFactory;
    FactoryPhysicalInterfaces phyFactory;

    /**
     * This method creates a ManagedGateway instance by passing the required
     * properties and connects the Gateway to the Watson IoT Platform by calling
     * the connect function.
     *
     * After the successful connection to the Watson IoT Platform, the Gateway
     * can perform the following operations, 1. Publish events for itself and on
     * behalf of devices connected behind the Gateway 2. Subscribe to commands
     * for itself and on behalf of devices behind the Gateway 3. Send a manage
     * request so that it can participate in the Device Management activities
     */
    private void init(String propertiesFile) throws Exception {

        /**
         * Load device properties
         */
        Properties deviceProps = new Properties();
        try {
            deviceProps.load(WatsonControl.class.getResourceAsStream(propertiesFile));
        } catch (IOException e1) {
            System.err.println("Not able to read the properties file, exiting..");
            System.exit(-1);
        }

        /**
         * Let us create the DeviceData object with the DeviceInfo object
         */
        DeviceInfo deviceInfo = new DeviceInfo.Builder()
                .serialNumber(trimedValue(deviceProps.getProperty("DeviceInfo.serialNumber")))
                .manufacturer(trimedValue(deviceProps.getProperty("DeviceInfo.manufacturer")))
                .model(trimedValue(deviceProps.getProperty("DeviceInfo.model")))
                .deviceClass(trimedValue(deviceProps.getProperty("DeviceInfo.deviceClass")))
                .description(trimedValue(deviceProps.getProperty("DeviceInfo.description")))
                .fwVersion(trimedValue(deviceProps.getProperty("DeviceInfo.swVersion")))
                .hwVersion(trimedValue(deviceProps.getProperty("DeviceInfo.hwVersion")))
                .descriptiveLocation(trimedValue(deviceProps.getProperty("DeviceInfo.descriptiveLocation"))).build();

        DeviceData deviceData = new DeviceData.Builder().deviceInfo(deviceInfo).build();

        this.registrationMode = deviceProps.getProperty("Registration-Mode");
        if (!(this.registrationMode.equalsIgnoreCase(RegistrationMode.MANUAL.getRegistrationMode())
                || this.registrationMode.equalsIgnoreCase(RegistrationMode.AUTOMATIC.toString()))) {
            throw new Exception("RegistrationMode not valid");
        }

        mgdGateway = new ManagedGateway(deviceProps, deviceData);

        // Connect to Watson IoT Platform
        mgdGateway.connect();

        // We need to create APIclint to register the device type of Arduino Uno
        // device, if its not registered already
        Properties options = new Properties();
        options.put("Organization-ID", deviceProps.getProperty("Organization-ID"));
        options.put("id", "app" + (Math.random() * 10000));
        options.put("Authentication-Method", "apikey");
        options.put("API-Key", deviceProps.getProperty("API-Key"));
        options.put("Authentication-Token", deviceProps.getProperty("API-Token"));

        apiClient = new ZmartifyAPIClient(options);

        // add command callback for these devices or gateway
        addCommandCallback();

        // can also not be managed
        mgdGateway.sendGatewayManageRequest(0, true, true);

        // Initialize a firmware handler that handles the firmware update for the Gateway and
        // attached devices that supports firmware actions
        GatewayFirmwareHandlerSample fwHandler = new GatewayFirmwareHandlerSample();
        fwHandler.setGateway(mgdGateway);

        // Initialize a device action handler that handles the reboot or reset request for the Gateway and
        // attached devices
        GatewayActionHandlerSample actionHandler = new GatewayActionHandlerSample();
        actionHandler.setGateway(mgdGateway);

        // Create a threadpool that can handle the firmware/device action requests from the Watson IoT Platform
        // in bulk, for example, if a user wants to reboot all the devices connected to the gateway in one go,
        // gateway should be able to handle the load if there are 1000 or more devices connected to it.
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10, 10, 60, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        // Allow core threads to timeout as the firmware/device actions may not be called very frequently
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        fwHandler.setExecutor(threadPoolExecutor);
        actionHandler.setExecutor(threadPoolExecutor);

        // Add the firmware and device action handler to Gateway
        mgdGateway.addFirmwareHandler(fwHandler);
        mgdGateway.addDeviceActionHandler(actionHandler);

        initFactories();
    }

    private void initFactories() {
        apiFactory = new FactoryApplicationInterfaces(apiClient, this);
        evtFactory = new FactoryEventTypes(apiClient, this);
        devFactory = new FactoryDeviceTypes(apiClient);
        phyFactory = new FactoryPhysicalInterfaces(apiClient);
    }

    private String trimedValue(String value) {
        if (value == null || value == "") {
            return "";
        } else {
            return value.trim();
        }
    }

    private void writeJsonFile(String fileName, JsonObject json) {
        FileWriter writer;
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            writer = new FileWriter(fileName);
            writer.write(gson.toJson(json));
            writer.close();
        } catch (IOException e) {
            System.out.println("Error writing file");
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void writeResourceFile(String fileName, JsonObject json, String resourceType) {
        writeJsonFile(WatsonControl.class.getResource("/" + resourceType).getPath() + "/" + fileName + ".json", json);
    }

    private void disconnect() {
        // Disconnect cleanly
        mgdGateway.disconnect();
    }

    public void abortProgram(int exitCode) {
        disconnect();
        System.exit(exitCode);
    }

    public JsonObject addSchema(String schemaName, String schemaType, String description) {
        String name = schemaType + "/" + schemaName;
        String schemaFileName = WatsonControl.class.getResource("/" + schemaType + "/" + schemaName + ".json")
                .getPath();
        try {

            if (apiClient.isSchemaExistByName(name)) {
                return apiClient.updateSchemaByName(name, schemaFileName);
            } else {
                return apiClient.addSchema(name, schemaFileName, description);
            }
        } catch (IoTFCReSTException e) {
            System.out.println("Error adding schema " + name + " (" + e.getHttpCode() + ") ::" + e.getMessage());
            System.out.println("Let's take a break");
            // TODO Auto-generated catch block
            e.printStackTrace();
            // return empty JsonObject and continue
            return new JsonObject();
        }

    }

    public class MyFileFilter implements FilenameFilter {

        private String extension = ".*";

        public MyFileFilter(String extension) {
            this.extension = extension;
        }

        @Override
        public boolean accept(File directory, String fileName) {
            if (fileName.endsWith(extension)) {
                return true;
            }
            return false;
        }
    }

    public File[] getResourceFiles(String subdir, String ext) {
        File dir = new File(WatsonControl.class.getResource("/" + subdir).getPath());
        FilenameFilter filter = new MyFileFilter("." + ext);
        return dir.listFiles(filter);
    }

    /**
     * When the GatewayClient connects, it automatically subscribes to any commands for this Gateway.
     * But to subscribe to commands for the devices connected to the Gateway, one needs to use the
     * subscribeToDeviceCommands() method from the GatewayClient class.
     *
     * To receive and process the commands for the attached devices, the Gateway sample does the following,
     * 1. Adds a command callback method
     * 2. Subscribes to commands for the attached device
     *
     * The callback method processCommand() is invoked by the GatewayClient when it receives any command
     * for the attached devices from Watson IoT Platform. The Gateway CommandCallback defines a BlockingQueue
     * to store and process the commands (in separate thread) for smooth handling of MQTT publish message.
     */
    private void addCommandCallback() {
        System.out.println("<-- Subscribing to commands for all the devices..");
        GatewayCommandCallback callback = new GatewayCommandCallback(this.mgdGateway);
        mgdGateway.setGatewayCallback(callback);

        try {
            Thread t = new Thread(callback);
            t.start();
        } catch (Exception | Error e) {
            e.printStackTrace();
        }

    }

    private enum RegistrationMode {
        MANUAL("manual"),
        AUTOMATIC("automatic");

        private String registrationMode;

        RegistrationMode(String registrationMode) {
            this.registrationMode = registrationMode;
        }

        public String getRegistrationMode() {
            return this.registrationMode;
        }
    }

    public void cleanConfiguration() {
        System.out.println("We are cleaning up the system");
        devFactory.removeDeviceTypes();
        phyFactory.removePhyscialInterfaces();
        evtFactory.removeEventTypes();
        apiFactory.removeApplicationInterfaces();
        apiFactory.removeSchemas();
    }

    public void createConfiguration() {
        devFactory.createDeviceTypes();
        evtFactory.createEventTypes();
        phyFactory.createPhysicalInterfaces();
        apiFactory.createApplicationInterfaces();
    }

    public void deployConfiguration() {
        for (ZmartifyDeviceType zh : ZmartifyDeviceType.values()) {
            JsonObject response;
            try {
                response = apiClient.deployConfiguration(zh.getDeviceType());
                System.out.println(response.toString());
            } catch (IoTFCReSTException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {

        System.out.println("Starting the Managed Gateway...");

        WatsonControl app = new WatsonControl();
        try {
            // Gson gson = new GsonBuilder().setPrettyPrinting().create();

            app.init(PROPERTIES_FILE_NAME);

            // System.out.println("App initialized" + gson.toJson(ZmartifyDeviceType.AIRTEMPERATURE.getMAPJson()));

            // app.cleanConfiguration();
            // System.out.println("Completed cleaning - and we will start to build");
            // app.createConfiguration();

            app.deployConfiguration();

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.err.flush();
        } finally {
            app.disconnect();
        }

        System.out.println(" Exiting...");
        System.exit(0);
    }

}
