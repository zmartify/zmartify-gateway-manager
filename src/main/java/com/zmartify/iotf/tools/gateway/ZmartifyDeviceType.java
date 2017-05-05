package com.zmartify.iotf.tools.gateway;

import static com.zmartify.iotf.tools.gateway.WCConstants.EVENTID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public enum ZmartifyDeviceType {
    ENERGYMETERWATT("energyMeterWatt", "DecimalType", "Number", "%s", "state", "energyWatt", "Energy Meter in Watts"),
    ENERGYMETERKVAH("energyMeterKVAH", "DecimalType", "Number", "%s", "state", "energyKVAH", "Energy Meter in KVAH"),
    ENERGYMETERVOLT("energyMeterVolt", "DecimalType", "Number", "%s", "state", "energyVolt", "Energy Meter in Volt"),
    ENERGYMETERAMP("energyMeterAmp", "DecimalType", "Number", "%s", "state", "energyAmp", "Energy Meter in Amp"),
    ENERGYMETER("energyMeter", "DecimalType", "Number", "%s", "state", "energy", "Energy Meter"),
    ALARM("alarm", "OnOffType", "Switch", "%s", "state", "alarm", "Alarm"),
    BATTERYLEVEL("batteryLevel", "PercentType", "Dimmer", "%s", "state", "battery", "Battery level"),
    BUTTON("button", "OnOffType", "Switch", "%s", "state", "button", "Button"),
    CONTACTSENSOR("contactSensor", "OpenClosedType", "Contact", "%s", "state", "contactSensor", "Contact Sensor"),
    LOCK("lock", "DecimalType", "Number", "%s", "state", "lock", "Lock"),
    NOTIFICATIONMESSAGE("notificationMessage", "DecimalType", "Number", "%s", "state", "notificationState",
            "Notification Message"),
    NOTIFICATIONSTATE("notificationState", "StringType", "String", "%s", "state", "notificationMessage",
            "Notification State"),
    RELATIVEHUMIDITYMEASUREMENT("relativeHumidityMeasurement", "DecimalType", "Number", "%s", "state", "humidity",
            "Relative Humidity Measurement"),
    SIGNALSTRENGTH("signalStrength", "DecimalType", "Number", "%s", "state", "rssi", "Signal Strength"),
    SWITCH("switch", "OnOffType", "Switch", "%s", "state", "switch", "Switch"),
    SWITCHLEVEL("switchLevel", "PercentType", "Dimmer", "%s", "state", "switchLevel", "Switch Level"),
    THERMOSTATHEATINGSETPOINT("thermostatHeatingSetpoint", "DecimalType", "Number", "%s", "state", "setpoint",
            "Thermostat Heating Setpoint"),
    THERMOSTATMODE("thermostatMode", "DecimalType", "Number", "%s", "state", "themostatMode", "Thermostat Mode"),
    THERMOSTATOPERATINGSTATE("thermostatOperatingState", "DecimalType", "Number", "%s", "state",
            "thermostatOperatingState", "Thermostat Operating State"),
    THERMOSTATSETPOINT("thermostatSetpoint", "DecimalType", "Number", "%s", "state", "setpoint", "Thermostat Setpoint"),
    VALVE("valve", "OpenClosedType", "Contact", "%s", "state", "valve", "Valve"),
    RELAYSWITCH("relaySwitch", "OnOffType", "Switch", "%s", "state", "switch", "Relay Switch"),
    SLEEPSENSOR("sleepSensor", "OpenClosedType", "Contact", "%s", "state", "sleeping", "Sleep Sensor"),
    TAMPERALERT("tamperAlert", "OpenClosedType", "Contact", "%s", "state", "tamper", "Tamper Alert"),
    TEMPERATURE("temperature", "DecimalType", "Number", "%s", "state", "temperature", "Temperature Measurement"),
    FLOORTEMPERATURE("floorTemperature", "DecimalType", "Number", "%s", "state", "floorTemperature",
            "Floor temperature measurement"),
    AIRTEMPERATURE("airTemperature", "DecimalType", "Number", "%s", "state", "airTemperature",
            "Air temperature measurement"),
    OUTLET("outlet", "OnOffType", "Switch", "%s", "state", "switch", "Outlet");

    private String deviceType;
    private String eventProperty;
    private String eventType;
    private String required;
    private String eventId;
    private String applicationInterfaceProperty;
    private String description;

    private JsonObject mapBuilder = new JsonObject();
    private JsonObject apiBuilder = new JsonObject();

    private JsonObject mapProperties = new JsonObject();
    private JsonObject mapSubProperties = new JsonObject();

    private JsonObject apiTopProperties = new JsonObject();
    private JsonObject apiProperties = new JsonObject();
    private JsonObject apiSubProperties = new JsonObject();

    private JsonArray apiSubRequired = new JsonArray();
    private JsonArray apiTopRequired = new JsonArray();

    private ZmartifyDeviceType(String deviceType, String eventProperty, String eventType, String required,
            String eventId, String applicationInterfaceProperty, String description) {
        this.deviceType = deviceType;
        this.eventType = eventType;
        this.eventProperty = eventProperty;
        this.required = required;
        this.eventId = eventId;
        this.applicationInterfaceProperty = applicationInterfaceProperty;
        this.description = description;
        buildDefaultMapping();
    }

    private JsonObject buildProperty(String propertyType, Object propertyDefault) {
        JsonObject property = new JsonObject();
        property.addProperty("type", propertyType);
        if (propertyDefault != null) {
            if (propertyDefault instanceof String) {
                property.addProperty("default", (String) propertyDefault);
            } else if (propertyDefault instanceof Number) {
                property.addProperty("default", (Number) propertyDefault);
            } else if (propertyDefault instanceof Boolean) {
                property.addProperty("default", (Boolean) propertyDefault);
            }
        }
        return property;
    }

    private void apiPropertyBuilder(String propertyName, String subProperty, String propertyType,
            Object propertyDefault) {
        if (subProperty == null) {
            apiTopProperties.add(propertyName, buildProperty(propertyType, propertyDefault));
            apiTopRequired.add(propertyName);
        } else {
            apiSubProperties.add(subProperty, buildProperty(propertyType, propertyDefault));
            apiSubRequired.add(subProperty);
        }
    }

    private void mapPropertyBuilder(String propertyName, String subProperty, String formula) {
        mapSubProperties.addProperty(propertyName + ((subProperty != null) ? "." + subProperty : ""),
                String.format(formula, "$event.d." + eventProperty, "$event.d." + eventProperty));
    }

    private void addProperty(String propertyName, String subProperty, String propertyType, Object propertyDefault,
            String formula) {
        mapPropertyBuilder(propertyName, subProperty, formula);
        apiPropertyBuilder(propertyName, subProperty, propertyType, propertyDefault);
    }

    private void buildDefaultMapping() {
        mapBuilder.addProperty("applicationInterfaceId", "NOT SET");
        apiBuilder.addProperty("type", "object");
        addProperty("eventCount", null, "number", -1, "$state.eventCount+1");
        switch (applicationInterfaceProperty) {
            case "temperature":
            case "floorTemperature":
            case "airTemperature":
            case "thermostatSetPoint":
                addProperty(applicationInterfaceProperty, "C", "number", 0, "%s");
                addProperty(applicationInterfaceProperty, "F", "number", 0, "%s * 1.8 + 32");
                addProperty(applicationInterfaceProperty, "isLow", "boolean", false,
                        "%s < $state." + applicationInterfaceProperty + ".lowest");
                addProperty(applicationInterfaceProperty, "isHigh", "boolean", false,
                        "%s > $state." + applicationInterfaceProperty + ".highest");
                addProperty(applicationInterfaceProperty, "lowest", "number", 100,
                        "(%s < $state." + applicationInterfaceProperty + ".lowest) ? %s : $state."
                                + applicationInterfaceProperty + ".lowest");
                addProperty(applicationInterfaceProperty, "highest", "number", 0,
                        "(%s < $state." + applicationInterfaceProperty + ".highest) ? %s : $state."
                                + applicationInterfaceProperty + ".highest");
                break;
            default:
                switch (eventType) {
                    case "Number":
                    case "Dimmer":
                        addProperty(applicationInterfaceProperty, null, "number", 0, "%s");
                        break;
                    case "Switch":
                    case "Contact":
                        addProperty(applicationInterfaceProperty, null, "string", "", "%s");
                        break;
                    case "DateTime":
                        addProperty(applicationInterfaceProperty, null, "string", "", "%s");
                        break;
                    case "Player":
                    case "Rollershutter":
                        addProperty(applicationInterfaceProperty, null, "string", "", "%s");
                        break;
                    case "HSBType":
                    case "String":
                        addProperty(applicationInterfaceProperty, null, "string", "", "%s");
                        break;
                    default:
                        System.out.println("Missing eventProperty " + eventType + " - " + deviceType);
                        // do nothing
                        break;
                }
        }
        mapBuilder.addProperty("notificationStrategy", "on-state-change");
        mapProperties.add(EVENTID, mapSubProperties);
        mapBuilder.add("propertyMappings", mapProperties);
        if (apiSubProperties.entrySet().size() > 0) {
            apiProperties.add("required", apiSubRequired);
            apiProperties.addProperty("type", "object");
            apiProperties.add("properties", apiSubProperties);
            apiProperties.add("required", apiSubRequired);
            apiTopProperties.add(applicationInterfaceProperty, apiProperties);
            apiTopRequired.add(applicationInterfaceProperty);
        }
        apiBuilder.add("properties", apiTopProperties);
        apiBuilder.add("required", apiTopRequired);
    }

    public JsonObject getMAPJson(String applicationInterfaceId) {
        mapBuilder.addProperty("applicationInterfaceId", applicationInterfaceId);
        return mapBuilder;
    }

    public JsonObject getAPIJson() {
        return apiBuilder;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getEventProperty() {
        return eventProperty;
    }

    public String getEventType() {
        return eventType;
    }

    public String getRequired() {
        return required;
    }

    public String getEventId() {
        return eventId;
    }

    public String getApplicationInterfaceProperty() {
        return applicationInterfaceProperty;
    }

    public String getDescription() {
        return description;
    }

    public String getPhysicalInterfaceName() {
        return new StringBuilder("OH2").append("-").append(deviceType).append("-").append("state").toString();
    }
}
