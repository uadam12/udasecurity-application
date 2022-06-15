package com.udacity.catpoint.service;

import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.*;
import com.udacity.catpoint.imageService.ImageService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    // Mock the required dependencies
    @Mock private ImageService imageService;
    @Mock private SecurityRepository securityRepository;
    @Mock private Sensor sensor;

    // Private members
    private SecurityService securityService;

    private Set<Sensor> getInactiveSensors() {
        return Set.of(
                mock(Sensor.class),
                mock(Sensor.class),
                mock(Sensor.class)
        );
    }

    @BeforeEach
    public void init() {
        securityService = new SecurityService(securityRepository, imageService);
    }

    // #1
    @ParameterizedTest
    @EnumSource(names = {"ARMED_HOME", "ARMED_AWAY"}, value = ArmingStatus.class)
    @DisplayName("If alarm is armed and a sensor becomes activated, put the system into pending alarm status")
    public void changeAlarmStatus_alarmArmedAndSensorActivated_alarmStatusPending(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(sensor.getActive()).thenReturn(false);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // #2
    @ParameterizedTest
    @EnumSource(names = {"ARMED_HOME", "ARMED_AWAY"}, value = ArmingStatus.class)
    @DisplayName("If alarm is armed and a sensor becomes activated and the system is already pending alarm, set alarm status to alarm.")
    public void changeAlarmStatus_alarmAlreadyPendingAndSensorActivated_alarmStatusAlarm(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(sensor.getActive()).thenReturn(false);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    // #3
    @Test
    @DisplayName("If pending alarm and all sensors are inactive, return to no alarm state")
    public void changeAlarmStatus_alarmPendingAndAllSensorsInactive_changeToNoAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(getInactiveSensors());
        when(sensor.getActive()).thenReturn(true);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    // #4
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("If alarm is active, change in sensor state should not affect the alarm state")
    public void changeAlarmState_alarmActivateAndSensorStateChange_stateNotAffected(boolean status) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        when(sensor.getActive()).thenReturn(status);

        securityService.changeSensorActivationStatus(sensor, !status);

        verify(securityRepository, never()).setAlarmStatus(any());
    }
    // #5
    @Test
    @DisplayName("If a sensor is activated while already active and the system is in pending state, change it to alarm state")
    public void changeAlarmState_systemActivatedWhileAlreadyActiveAndAlarmPending_changeToAlarmState() {
        when(sensor.getActive()).thenReturn(true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(Set.of(sensor));

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    // #6
    @Test
    @DisplayName("If a sensor is deactivated while already inactive, make no changes to the alarm state")
    public void changeAlarmState_sensorDeactivateWithInactive_noChangeToAlarmState() {
        when(sensor.getActive()).thenReturn(false);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }
    // #7
    @Test
    @DisplayName("If the camera image contains a cat while the system is armed-home, put the system into alarm status")
    public void changeAlarmState_imageContainingCatDetectedAndSystemArmed_changeToAlarmStatus() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);

        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }
    // #8
    @Test
    @DisplayName("If the camera image does not contain a cat, change the status to no alarm as long as the sensors are not active")
    public void changeAlarmState_noCatImageIdentifiedAndSensorsAreInActive_changeToAlarmStatus() {
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);
        when(securityRepository.getSensors()).thenReturn(getInactiveSensors());

        securityService.processImage(mock(BufferedImage.class));

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    // #9
    @Test
    @DisplayName("If the system is disarmed, set the status to no alarm")
    public void changeAlarmStatus_systemDisArmed_changeToAlarmStatus() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }
    // #10
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    @DisplayName("If the system is armed, reset all sensors to inactive")
    public void updateSensors_systemArmed_deactivateAllSensors(ArmingStatus armingStatus) {
        when(securityRepository.getSensors()).thenReturn(getInactiveSensors());

        securityService.setArmingStatus(armingStatus);

        for(Sensor sensor: securityService.getSensors())
            Assertions.assertFalse(sensor.getActive());
    }

    // #11
    @Test
    @DisplayName("If the system is armed-home while the camera shows a cat, set the alarm status to alarm")
    public void changeAlarmStatus_systemArmedHomeAndCatDetected_changeToAlarmStatus() {
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    //Coverage tests
    @Test
    @DisplayName("Can add and remove status listener")
    void addAndRemoverStatusListener() {
        StatusListener statusListener = Mockito.mock(StatusListener.class);

        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
    }

    @Test
    @DisplayName("Can add and remove a sensor")
    void addAndRemoveSensor() {
        securityService.addSensor(sensor);
        securityService.removeSensor(sensor);

        verify(securityRepository).addSensor(sensor);
        verify(securityRepository).removeSensor(sensor);
    }
}