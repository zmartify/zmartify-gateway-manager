package com.zmartify.iotf.tools.gateway;

public enum ZmartifyDeviceType {
	ENERGYMETERWATT("energyMeterWatt","DecimalType","Number","%s","state","energyWatt","Energy Meter in Watts"),
	ENERGYMETERKVAH("energyMeterKVAH","DecimalType","Number","%s","state","energyKVAH","Energy Meter in KVAH"),
	ENERGYMETERVOLT("energyMeterVolt","DecimalType","Number","%s","state","energyVolt","Energy Meter in Volt"),
	ENERGYMETERAMP("energyMeterAmp","DecimalType","Number","%s","state","energyAmp","Energy Meter in Amp"),
	ENERGYMETER("energyMeter","DecimalType","Number","%s","state","energy","Energy Meter"),
	ALARM("alarm","OnOffType","Number","%s","state","alarm","Alarm"),
	BATTERYALARM("batteryAlarm","OnOffType","Number","%s","state","alarm","Battery alarm"),
	BATTERYLEVEL("batteryLevel","PercentType","Number","%s","state","battery","Battery level"),
	BUTTON("button","OnOffType","Switch","%s","state","button","Button"),
	CONTACTSENSOR("contactSensor","OpenClosedType","Contact","%s","state","contactSensor","Contact Sensor"),
	LOCK("lock","DecimalType","Number","%s","state","lock","Lock"),
	NOTIFICATIONMESSAGE("notificationMessage","DecimalType","Number","%s","state","notificationState","Notification Message"),
	NOTIFICATIONSTATE("notificationState","StringType","String","%s","state","notificationMessage","Notification State"),
	RELATIVEHUMIDITYMEASUREMENT("relativeHumidityMeasurement","DecimalType","Number","%s","state","humidity","Relative Humidity Measurement"),
	SIGNALSTRENGTH("signalStrength","DecimalType","Number","%s","state","rssi","Signal Strength"),
	SWITCH("switch","OnOffType","Switch","%s","state","switch","Switch"),
	SWITCHLEVEL("switchLevel","PercentType","Dimmer","%s","state","switchLevel","Switch Level"),
	THERMOSTATHEATINGSETPOINT("thermostatHeatingSetpoint","DecimalType","Number","%s","state","setpoint","Thermostat Heating Setpoint"),
	THERMOSTATMODE("thermostatMode","DecimalType","Number","%s","state","themostatMode","Thermostat Mode"),
	THERMOSTATOPERATINGSTATE("thermostatOperatingState","DecimalType","Number","%s","state","thermostatOperatingState","Thermostat Operating State"),
	THERMOSTATSETPOINT("thermostatSetpoint","DecimalType","Number","%s","state","setpoint","Thermostat Setpoint"),
	VALVE("valve","OpenClosedType","Contact","%s","state","valve","Valve"),
	RELAYSWITCH("relaySwitch","OnOffType","Switch","%s","state","switch","Relay Switch"),
	SLEEPSENSOR("sleepSensor","OpenClosedType","Contact","%s","state","sleeping","Sleep Sensor"),
	TAMPERALERT("tamperAlert","OpenClosedType","Contact","%s","state","tamper","Tamper Alert"),
	TEMPERATURE("Temperature","DecimalType","Number","%s","state","temperature","Temperature Measurement"),
	FLOORTEMPERATURE("FloorTemperature","DecimalType","Number","%s","state","floorTemperature","Floor temperature measurement"),
	AIRTEMPERATURE("airTemperature","DecimalType","Number","%s","state","airTemperature","Air temperature measurement"),
	OUTLET("outlet","OnOffType","Switch","%s","state","switch","Outlet");
	
	private String deviceType;
	private String eventProperty;
	private String eventType;
	private String required;
	private String eventId;
	private String applicationInterfaceProperty;
	private String description;
	
	private ZmartifyDeviceType(String deviceType, String eventProperty, String eventType, String required, String eventId,
			String applicationInterfaceProperty, String description) {
		this.deviceType = deviceType;
		this.eventType = eventType;
		this.eventProperty = eventProperty;
		this.required = required;
		this.eventId = eventId;
		this.applicationInterfaceProperty = applicationInterfaceProperty;
		this.description = description;
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


