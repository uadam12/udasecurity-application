package com.udacity.catpoint.service;

import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.AlarmStatus;
import com.udacity.catpoint.data.ArmingStatus;
import com.udacity.catpoint.data.SecurityRepository;
import com.udacity.catpoint.data.Sensor;
import com.udacity.catpoint.imageService.ImageService;
import org.jetbrains.annotations.NotNull;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

    private final ImageService imageService;
    private final SecurityRepository securityRepository;
    private final Set<StatusListener> statusListeners = new HashSet<>();
    private boolean imageContainsCat;

    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus No description
     */
    public void setArmingStatus(@NotNull ArmingStatus armingStatus) {
        if (imageContainsCat && armedHome(armingStatus))
            setAlarmStatus(AlarmStatus.ALARM);

        if (systemDisarmed(armingStatus))
            setAlarmStatus(AlarmStatus.NO_ALARM);
        else getSensors().forEach(sensor -> sensor.setActive(false));

        securityRepository.setArmingStatus(armingStatus);
        statusListeners.forEach(StatusListener::sensorStatusChanged);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }

    private boolean systemDisarmed(ArmingStatus armingStatus) {
        return armingStatus == ArmingStatus.DISARMED;
    }
    private boolean armedHome(ArmingStatus armingStatus) {
        return armingStatus == ArmingStatus.ARMED_HOME;
    }
    private boolean armedHome() {
        return armedHome(getArmingStatus());
    }
    private boolean armedAway() {
        return getArmingStatus() == ArmingStatus.ARMED_AWAY;
    }
    private boolean armed() {
        return armedHome() || armedAway();
    }
    // --------------------------------------------------------------------------

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status No description
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    private boolean pendingAlarm() {
        return getAlarmStatus() == AlarmStatus.PENDING_ALARM;
    }
    // --------------------------------------------------------------------------

    /**
     * Send an image to the SecurityService for processing. The securityService will use it provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage No description
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(@NotNull Boolean cat) {
        imageContainsCat = cat;

        if(cat && armedHome())
            setAlarmStatus(AlarmStatus.ALARM);
        else if(!cat && noActiveSensor())
            setAlarmStatus(AlarmStatus.NO_ALARM);

        statusListeners.forEach(sl -> sl.catDetected(cat));
    }
    // --------------------------------------------------------------------------

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor No description
     * @param active No description
     */
    public void changeSensorActivationStatus(@NotNull Sensor sensor, Boolean active) {
        if (!(sensor.getActive() && !active)) {
            if (!sensor.getActive() && active) {
                if (armed()) switch (getAlarmStatus()) {
                    case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
                    case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
                }
            } else if (!(!sensor.getActive() && !active) && pendingAlarm())
                setAlarmStatus(AlarmStatus.ALARM);
        }

        sensor.setActive(active);
        securityRepository.updateSensor(sensor);

        if (noActiveSensor() && pendingAlarm())
            setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(@NotNull Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    private boolean noActiveSensor() {
        return getSensors().
                stream().
                noneMatch(Sensor::getActive);
    }
    // --------------------------------------------------------------------------

    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener No description
     */
    public void addStatusListener(@NotNull StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

}
